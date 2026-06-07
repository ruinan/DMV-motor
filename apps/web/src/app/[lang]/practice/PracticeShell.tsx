"use client";

import { useAuth } from "@/lib/auth-context";
import { useMe } from "@/lib/hooks/use-me";
import { AppSidebar } from "@/components/app-sidebar";
import { MobileAppBar } from "@/components/mobile-app-bar";
import { MobileTabBar } from "@/components/mobile-tab-bar";
import type { Dictionary, Locale } from "@/lib/dictionaries";
import { PracticeFlow } from "./PracticeFlow";

type Props = {
  t: Dictionary;
  lang: Locale;
};

/**
 * Shell wrapper for /practice. The route lives outside the (app) group so
 * anonymous visitors can use the free-trial set per docs/development
 * /api-contract.md §4 (sec audit #4). For *signed-in* users we still want
 * the dashboard chrome (sidebar / mobile bars) so they can switch sessions
 * without leaving practice — same URL, different chrome by auth state.
 *
 * We deliberately do NOT use RequireAuth here: anonymous must keep working.
 */
export function PracticeShell({ t, lang }: Props) {
  const { user, loading } = useAuth();
  const me = useMe();

  // Anonymous/free practice must not block on Firebase rehydration. If auth
  // later resolves to a user, the signed-in chrome replaces this shell.
  if (loading || !user) {
    return <PracticeFlow t={t.practice} lang={lang} />;
  }

  // Signed in: same chrome as the rest of the (app) surfaces, including the
  // per-exam accent theme (theme.css [data-exam=…]) — /practice lives outside
  // the (app) group so it must set data-exam itself, or it stays the default
  // (car) blue even when the learner is studying the amber motorcycle exam.
  return (
    <div
      className="min-h-screen bg-background"
      data-exam={me.data?.current_exam?.license_class}
    >
      <AppSidebar t={t} lang={lang} />
      <MobileAppBar t={t} lang={lang} />
      <main className="md:pl-64">
        <div className="mx-auto w-full max-w-7xl px-4 pb-24 pt-6 md:px-8 md:pb-12 md:pt-8">
          {/* Key by exam: switching exam while on /practice stays here (B29), so
              remount PracticeFlow to drop the old exam's in-flight session state
              and auto-resume the new exam's instead. */}
          <PracticeFlow
            key={me.data?.current_exam?.id ?? "none"}
            t={t.practice}
            lang={lang}
          />
        </div>
      </main>
      <MobileTabBar t={t} lang={lang} />
    </div>
  );
}
