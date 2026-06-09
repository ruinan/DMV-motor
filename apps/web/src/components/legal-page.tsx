import type { LegalDoc } from "@/content/legal";

/**
 * Renders a long-form legal document (Privacy Policy / Terms). Server component
 * — pure presentation of the locale-resolved {@link LegalDoc}. Wrapped by the
 * marketing layout's header/footer chrome.
 */
export function LegalPage({ doc }: { doc: LegalDoc }) {
  return (
    <main className="mx-auto w-full max-w-3xl px-6 py-12 sm:py-16">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">{doc.title}</h1>
      <p className="mt-2 text-sm text-muted-foreground">{doc.updated}</p>

      <div className="mt-4 rounded-lg border border-amber-400/50 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:bg-amber-950/30 dark:text-amber-200">
        {doc.reviewNote}
      </div>

      {doc.intro.map((p, i) => (
        <p key={i} className="mt-6 leading-relaxed text-muted-foreground">
          {p}
        </p>
      ))}

      <div className="mt-8 flex flex-col gap-8">
        {doc.sections.map((s) => (
          <section key={s.heading}>
            <h2 className="text-xl font-semibold text-foreground">{s.heading}</h2>
            <div className="mt-3 flex flex-col gap-3">
              {s.paragraphs.map((p, i) => (
                <p key={i} className="leading-relaxed text-muted-foreground">
                  {p}
                </p>
              ))}
            </div>
          </section>
        ))}
      </div>
    </main>
  );
}
