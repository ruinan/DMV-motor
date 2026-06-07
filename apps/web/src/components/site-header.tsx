"use client";

import Link from "next/link";
import { LanguageSelect } from "@/components/language-select";
import { useAuth } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["site"];
  lang: Locale;
};

/**
 * Marketing header. Auth-aware: a signed-in visitor who lands on the marketing
 * pages sees "Go to study hub" (→ dashboard) instead of "Sign in", so the header
 * never claims they're logged out while the app treats them as logged in (which
 * was confusing — sign-in shown, yet "free practice" dropped them into the
 * signed-in UI). While auth is still resolving we render neither CTA to avoid a
 * flash of the wrong one.
 */
export function SiteHeader({ t, lang }: Props) {
  const { user, loading } = useAuth();

  return (
    <header className="sticky top-0 z-40 w-full bg-card shadow-sm">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
        {/* Header brand + actions use a fixed neutral dark — independent of the
            landing's blue/amber accent that shifts with the hero carousel, so the
            chrome never clashes with whichever slide is showing. */}
        <Link
          href={`/${lang}`}
          className="text-xl font-bold tracking-tight text-foreground"
        >
          {t.brand}
        </Link>
        <div className="flex items-center gap-3">
          {loading ? (
            <span className="h-9 w-24" aria-hidden />
          ) : user ? (
            <Link
              href={`/${lang}/dashboard`}
              className="rounded-md bg-foreground px-3 py-2 text-sm font-medium text-background transition-colors hover:bg-foreground/90"
            >
              {t.goToDashboard}
            </Link>
          ) : (
            <Link
              href={`/${lang}/login`}
              className="px-3 py-2 text-sm font-medium text-foreground transition-colors hover:text-foreground/70"
            >
              {t.signIn}
            </Link>
          )}
          <LanguageSelect currentLang={lang} ariaLabel={t.switchLanguage} />
        </div>
      </div>
    </header>
  );
}
