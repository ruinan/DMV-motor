import Link from "next/link";
import { LanguageToggle } from "@/components/language-toggle";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

/**
 * Mobile-only top app bar shown on logged-in surfaces. Mirrors the desktop
 * sidebar header (brand + language toggle). Sign-out lives on the
 * /me page on mobile to keep this bar lean.
 */
export function MobileAppBar({ t, lang }: Props) {
  return (
    <header className="sticky top-0 z-40 flex h-16 items-center justify-between border-b border-border bg-card px-6 md:hidden">
      <Link
        href={`/${lang}/dashboard`}
        className="text-lg font-bold tracking-tight text-primary"
      >
        {t.nav.appBrand}
      </Link>
      <LanguageToggle currentLang={lang} ariaLabel={t.site.switchLanguage} />
    </header>
  );
}
