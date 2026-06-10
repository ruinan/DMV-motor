"use client";

import { useEffect, useState } from "react";
import { usePathname, useSearchParams, useRouter } from "next/navigation";
import { Loader2, RefreshCw } from "lucide-react";
import { AppSidebar } from "@/components/app-sidebar";
import { MobileTabBar } from "@/components/mobile-tab-bar";
import { MobileAppBar } from "@/components/mobile-app-bar";
import { useMe } from "@/lib/hooks/use-me";
import type { Dictionary, Locale } from "@/lib/dictionaries";
import type { ReactNode } from "react";

type Props = {
  t: Dictionary;
  lang: Locale;
  children: ReactNode;
};

/**
 * Wraps the (app) shell. Two responsibilities:
 *
 * 1. Onboarding gate — a logged-in user with no current exam is sent to the
 *    full-screen chooser at `/<lang>/start` before any app surface renders, so
 *    "which test am I studying?" is always answered first. `/start` itself
 *    renders full-bleed (no chrome); once an exam is set the user switches
 *    anytime from the sidebar.
 *
 * 2. Focus mode — a mock-exam attempt page (`/<lang>/mock/<attemptId>`) renders
 *    full-bleed (no sidebar/top bar/tabs) so the user concentrates on the
 *    questions. Opening a finished attempt in review mode (`?review=1`) keeps
 *    the normal chrome. Everything else gets the normal chrome.
 */
export function AppChrome({ t, lang, children }: Props) {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const router = useRouter();
  const me = useMe();

  const isReview = searchParams.get("review") === "1";
  const stripped = pathname.replace(/\/$/, "");
  const onStart = stripped === `/${lang}/start`;
  const inExamAttempt =
    new RegExp(`^/${lang}/mock/[^/]+`).test(stripped) && !isReview;

  const loadingMe = me.isLoading;
  const hasExam = !!me.data?.current_exam;
  // Drives the per-exam accent theme (see theme.css [data-exam=…]) so the shell
  // always reflects which exam the learner is studying.
  const examClass = me.data?.current_exam?.license_class;

  // Onboarding gate: force exam selection before any app surface, and bounce
  // an already-onboarded user off /start.
  useEffect(() => {
    if (loadingMe) return;
    if (!hasExam && !onStart) {
      router.replace(`/${lang}/start`);
    } else if (hasExam && onStart) {
      router.replace(`/${lang}/dashboard`);
    }
  }, [loadingMe, hasExam, onStart, lang, router]);

  // A spinner that never resolves looks identical to a crash. Cloud Run can
  // cold-start the backend for ~10s; past that, surface a "waking up / retry"
  // card instead of spinning forever so the user knows it isn't frozen.
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    if (!loadingMe) {
      setSlow(false);
      return;
    }
    const id = setTimeout(() => setSlow(true), 10_000);
    return () => clearTimeout(id);
  }, [loadingMe]);

  // The onboarding screen is full-bleed (its own layout).
  if (onStart) {
    return <>{children}</>;
  }

  // While we don't yet know the exam, or while the gate redirects an
  // exam-less user to /start, show a spinner instead of flashing the page.
  if (loadingMe || !hasExam) {
    if (slow && loadingMe) {
      return (
        <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
          <div className="w-full max-w-sm rounded-xl border border-border/40 bg-card p-6 text-center shadow-sm">
            <h2 className="text-lg font-semibold text-foreground">
              {t.auth.stuckTitle}
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">{t.auth.slowBody}</p>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="mt-6 inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-primary text-sm font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md"
            >
              <RefreshCw className="size-4" />
              {t.auth.refresh}
            </button>
          </div>
        </div>
      );
    }
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden />
      </div>
    );
  }

  if (inExamAttempt) {
    return (
      <div className="min-h-screen bg-background" data-exam={examClass}>
        <main>
          <div className="mx-auto w-full max-w-3xl px-4 py-6 md:px-8 md:py-10">
            {children}
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background" data-exam={examClass}>
      <AppSidebar t={t} lang={lang} />
      <MobileAppBar t={t} lang={lang} />
      <main className="md:pl-64">
        <div className="mx-auto w-full max-w-7xl px-4 pb-24 pt-6 md:px-8 md:pb-12 md:pt-8">
          {children}
        </div>
      </main>
      <MobileTabBar t={t} lang={lang} />
    </div>
  );
}
