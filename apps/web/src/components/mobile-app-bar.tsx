import Link from "next/link";
import { LanguageSelect } from "@/components/language-select";
import { ExamSwitcher } from "@/components/exam-switcher";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

/**
 * Mobile-only top app bar shown on logged-in surfaces. Mirrors the desktop
 * sidebar header (brand + language selector on top, exam switcher on its own
 * full-width row below so neither control crowds the other). Sign-out lives on
 * the /me page on mobile to keep this bar lean.
 */
export function MobileAppBar({ t, lang }: Props) {
  return (
    <header className="sticky top-0 z-40 flex flex-col gap-1.5 border-b border-border bg-card px-4 py-2.5 md:hidden">
      <div className="flex items-center justify-between gap-2">
        <Link
          href={`/${lang}/dashboard`}
          className="truncate text-lg font-bold leading-tight tracking-tight text-primary"
        >
          {t.nav.appBrand}
        </Link>
        <LanguageSelect currentLang={lang} ariaLabel={t.site.switchLanguage} />
      </div>
      <ExamSwitcher
        lang={lang}
        variant="plain"
        switchLabel={t.nav.switchExam}
        confirm={{
          title: t.nav.switchExamConfirmTitle,
          body: t.nav.switchExamConfirmBody,
          yes: t.nav.switchExamConfirmYes,
          cancel: t.nav.switchExamConfirmCancel,
        }}
      />
    </header>
  );
}
