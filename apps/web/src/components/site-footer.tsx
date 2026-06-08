import type { Dictionary } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["footer"];
};

export function SiteFooter({ t }: Props) {
  // about/privacy/terms hrefs are placeholders — pages don't exist yet.
  // Switch to /<lang>/about etc. once those routes are added.
  return (
    <footer className="w-full border-t border-border bg-card py-8 shadow-[0_-2px_8px_-2px_rgba(0,0,0,0.06)]">
      <div className="mx-auto flex w-full max-w-7xl flex-col items-center gap-3 px-6 text-sm text-muted-foreground sm:flex-row sm:justify-between">
        <p>{t.copyright}</p>
        <nav className="flex items-center gap-6">
          <a href="#" className="transition-colors hover:text-foreground">
            {t.about}
          </a>
          <a href="#" className="transition-colors hover:text-foreground">
            {t.privacy}
          </a>
          <a href="#" className="transition-colors hover:text-foreground">
            {t.terms}
          </a>
        </nav>
      </div>
    </footer>
  );
}
