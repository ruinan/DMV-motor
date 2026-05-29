"use client";

import { usePathname, useSearchParams } from "next/navigation";
import { AppSidebar } from "@/components/app-sidebar";
import { MobileTabBar } from "@/components/mobile-tab-bar";
import { MobileAppBar } from "@/components/mobile-app-bar";
import type { Dictionary, Locale } from "@/lib/dictionaries";
import type { ReactNode } from "react";

type Props = {
  t: Dictionary;
  lang: Locale;
  children: ReactNode;
};

/**
 * Wraps the (app) shell so we can selectively drop chrome on focus surfaces.
 * A mock-exam attempt page (`/<lang>/mock/<attemptId>`) renders full-bleed
 * while the exam is being taken — no sidebar, no top bar, no bottom tabs — so
 * the user concentrates on the questions. Opening a finished attempt in review
 * mode (`?review=1`, e.g. from the Study Hub) keeps the normal chrome so it
 * reads like the rest of the app. Everything else renders the normal chrome.
 */
export function AppChrome({ t, lang, children }: Props) {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const isReview = searchParams.get("review") === "1";
  // Match /<lang>/mock/<anything-not-just-mock> — i.e. an attempt page, not
  // the mock landing. Path normaliser tolerates trailing slash. Review mode
  // keeps the chrome.
  const stripped = pathname.replace(/\/$/, "");
  const inExamAttempt =
    new RegExp(`^/${lang}/mock/[^/]+`).test(stripped) && !isReview;

  if (inExamAttempt) {
    return (
      <div className="min-h-screen bg-background">
        <main>
          <div className="mx-auto w-full max-w-3xl px-4 py-6 md:px-8 md:py-10">
            {children}
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
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
