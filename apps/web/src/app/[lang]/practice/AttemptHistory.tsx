"use client";

import { CheckCircle2, ChevronLeft, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAttempts } from "@/lib/hooks/use-attempts";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  sessionId: string;
  lang: Locale;
  t: Dictionary["practice"];
  onBack: () => void;
};

export function AttemptHistory({ sessionId, lang, t, onBack }: Props) {
  const { data, isLoading, error } = useAttempts(sessionId, lang);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-xl font-semibold tracking-tight sm:text-2xl">
          {t.reviewHistoryTitle}
        </h2>
        <Button variant="ghost" size="sm" onClick={onBack}>
          <ChevronLeft className="size-4" />
          {t.reviewHistoryBack}
        </Button>
      </div>

      {isLoading && (
        <p className="text-sm text-muted-foreground">{t.loading}</p>
      )}

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
          {t.errorGeneric}
        </div>
      )}

      {data && data.items.length === 0 && (
        <p className="text-sm text-muted-foreground">{t.reviewHistoryEmpty}</p>
      )}

      {data && data.items.length > 0 && (
        <ol className="flex flex-col gap-4">
          {data.items.map((a, idx) => (
            <li key={a.question_id}>
              <article className="rounded-xl border bg-card p-5 shadow-sm">
                <header className="mb-3 flex items-center gap-2 text-sm">
                  <span className="inline-flex size-6 items-center justify-center rounded-full bg-muted text-xs font-semibold tabular-nums">
                    {idx + 1}
                  </span>
                  {a.is_correct ? (
                    <span className="inline-flex items-center gap-1 text-primary">
                      <CheckCircle2 className="size-4" />
                      {t.correct}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-destructive">
                      <XCircle className="size-4" />
                      {t.incorrect}
                    </span>
                  )}
                </header>

                <p className="text-base leading-relaxed">{a.stem}</p>

                <ul className="mt-4 flex flex-col gap-2">
                  {a.choices.map((c) => {
                    const isCorrect = c.key === a.correct_choice_key;
                    const wasPicked = c.key === a.selected_choice_key;
                    const wrongPick = wasPicked && !isCorrect;

                    const tone = isCorrect
                      ? "border-primary bg-primary/10 text-foreground"
                      : wrongPick
                        ? "border-destructive bg-destructive/10 text-foreground"
                        : "border-border bg-background text-muted-foreground";

                    return (
                      <li
                        key={c.key}
                        className={`flex items-start gap-3 rounded-lg border-2 px-3 py-2 text-sm ${tone}`}
                      >
                        <span className="mt-0.5 inline-flex size-6 shrink-0 items-center justify-center rounded-full border border-border bg-background text-xs font-semibold">
                          {c.key}
                        </span>
                        <span className="flex-1">{c.text}</span>
                        {isCorrect && (
                          <CheckCircle2 className="size-4 shrink-0 text-primary" />
                        )}
                        {wrongPick && (
                          <XCircle className="size-4 shrink-0 text-destructive" />
                        )}
                      </li>
                    );
                  })}
                </ul>

                <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
                  <span>
                    <span className="font-medium text-foreground">
                      {t.reviewHistoryYourPick}:
                    </span>{" "}
                    {a.selected_choice_key}
                  </span>
                  <span>
                    <span className="font-medium text-foreground">
                      {t.reviewHistoryCorrect}:
                    </span>{" "}
                    {a.correct_choice_key}
                  </span>
                </div>

                {a.explanation && (
                  <p className="mt-3 rounded-md bg-muted/50 p-3 text-sm leading-relaxed text-muted-foreground">
                    <span className="font-medium text-foreground">
                      {t.explanation}:
                    </span>{" "}
                    {a.explanation}
                  </p>
                )}
              </article>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
