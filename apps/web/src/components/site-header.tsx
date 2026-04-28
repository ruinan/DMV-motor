import Link from "next/link";
import { LanguageToggle } from "@/components/language-toggle";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["site"];
  lang: Locale;
};

export function SiteHeader({ t, lang }: Props) {
  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/40 bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
        <Link
          href={`/${lang}`}
          className="text-xl font-bold text-primary tracking-tight"
        >
          {t.brand}
        </Link>
        <div className="flex items-center gap-3">
          <Link
            href={`/${lang}/login`}
            className="px-3 py-2 text-sm font-medium text-primary transition-colors hover:text-primary/80"
          >
            {t.signIn}
          </Link>
          <LanguageToggle currentLang={lang} ariaLabel={t.switchLanguage} />
        </div>
      </div>
    </header>
  );
}
