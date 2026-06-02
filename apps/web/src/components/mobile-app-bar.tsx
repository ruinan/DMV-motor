import Link from "next/link";
import { LanguageToggle } from "@/components/language-toggle";
import { ExamSwitcher } from "@/components/exam-switcher";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

/**
 * Mobile-only top app bar shown on logged-in surfaces. Mirrors the desktop
 * sidebar header (brand + exam switcher + language toggle). Sign-out lives on
 * the /me page on mobile to keep this bar lean.
 */
export function MobileAppBar({ t, lang }: Props) {
  return (
    <header className="sticky top-0 z-40 flex h-16 items-center justify-between border-b border-border bg-card px-6 md:hidden">
      <div className="flex min-w-0 flex-col">
        <Link
          href={`/${lang}/dashboard`}
          className="text-lg font-bold leading-tight tracking-tight text-primary"
        >
          {t.nav.appBrand}
        </Link>
        <ExamSwitcher lang={lang} variant="plain" switchLabel={t.nav.switchExam} />
      </div>
      <LanguageToggle currentLang={lang} ariaLabel={t.site.switchLanguage} />
    </header>
  );
}
