import Link from "next/link";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["footer"];
  lang: Locale;
};

export function SiteFooter({ t, lang }: Props) {
  return (
    <footer className="w-full border-t border-border bg-card py-8 shadow-[0_-2px_8px_-2px_rgba(0,0,0,0.06)]">
      <div className="mx-auto flex w-full max-w-7xl flex-col items-center gap-3 px-6 text-sm text-muted-foreground sm:flex-row sm:justify-between">
        <p>{t.copyright}</p>
        <nav className="flex items-center gap-6">
          {/* "About" page doesn't exist yet — link home for now. */}
          <Link href={`/${lang}`} className="transition-colors hover:text-foreground">
            {t.about}
          </Link>
          <Link
            href={`/${lang}/privacy`}
            className="transition-colors hover:text-foreground"
          >
            {t.privacy}
          </Link>
          <Link
            href={`/${lang}/terms`}
            className="transition-colors hover:text-foreground"
          >
            {t.terms}
          </Link>
        </nav>
      </div>
    </footer>
  );
}
