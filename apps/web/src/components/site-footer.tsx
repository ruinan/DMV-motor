import type { Dictionary } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["footer"];
};

export function SiteFooter({ t }: Props) {
  // about/privacy/terms hrefs are placeholders — pages don't exist yet.
  // Switch to /<lang>/about etc. once those routes are added.
  return (
    <footer className="w-full border-t border-border/40 bg-background py-8">
      <div className="mx-auto flex w-full max-w-7xl flex-col items-center gap-3 px-6 text-sm text-muted-foreground sm:flex-row sm:justify-between">
        <p>{t.copyright}</p>
        <nav className="flex items-center gap-6">
          <a href="#" className="transition-colors hover:text-primary">
            {t.about}
          </a>
          <a href="#" className="transition-colors hover:text-primary">
            {t.privacy}
          </a>
          <a href="#" className="transition-colors hover:text-primary">
            {t.terms}
          </a>
        </nav>
      </div>
    </footer>
  );
}
