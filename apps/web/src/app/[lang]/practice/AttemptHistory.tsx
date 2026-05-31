"use client";

import { CheckCircle2, ChevronLeft, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAttempts, type AttemptItem as AttemptRow } from "@/lib/hooks/use-attempts";
import { AiExplainBlock } from "@/components/ai-explain-block";
import { useAuth } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  sessionId: string;
  lang: Locale;
  t: Dictionary["practice"];
  onBack: () => void;
};

export function AttemptHistory({ sessionId, lang, t, onBack }: Props) {
  const { data, isLoading, error } = useAttempts(sessionId, lang);
  const { user } = useAuth();
  const isLoggedIn = !!user;

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
            <li key={`${a.question_id}-${idx}`}>
              <AttemptHistoryItem
                t={t}
                lang={lang}
                index={idx}
                attempt={a}
                isLoggedIn={isLoggedIn}
              />
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

// One row + its own AI lifecycle. Per-item useAiExplain hook isolates state so
// clicking AI on attempt #3 doesn't replace the explanation on attempt #1.
function AttemptHistoryItem({
  t,
  lang,
  index,
  attempt,
  isLoggedIn,
}: {
  t: Dictionary["practice"];
  lang: Locale;
  index: number;
  attempt: AttemptRow;
  isLoggedIn: boolean;
}) {
  return (
    <article className="rounded-xl border bg-card p-5 shadow-sm">
      <header className="mb-3 flex items-center gap-2 text-sm">
        <span className="inline-flex size-6 items-center justify-center rounded-full bg-muted text-xs font-semibold tabular-nums">
          {index + 1}
        </span>
        {attempt.is_correct ? (
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

      <p className="text-base leading-relaxed">{attempt.stem}</p>

      <ul className="mt-4 flex flex-col gap-2">
        {attempt.choices.map((c) => {
          const isCorrect = c.key === attempt.correct_choice_key;
          const wasPicked = c.key === attempt.selected_choice_key;
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
          {attempt.selected_choice_key}
        </span>
        <span>
          <span className="font-medium text-foreground">
            {t.reviewHistoryCorrect}:
          </span>{" "}
          {attempt.correct_choice_key}
        </span>
      </div>

      {attempt.explanation && (
        <p className="mt-3 rounded-md bg-muted/50 p-3 text-sm leading-relaxed text-muted-foreground">
          <span className="font-medium text-foreground">
            {t.explanation}:
          </span>{" "}
          {attempt.explanation}
        </p>
      )}

      {/* AI deep-dive on wrong answers only. Click-to-reveal + "深入分析"
          layering, history persisted client-side — all handled by the shared
          AiExplainBlock (enhance1). */}
      {!attempt.is_correct && (
        <AiExplainBlock
          questionId={attempt.question_id}
          variantId={attempt.variant_id}
          selectedChoiceKey={attempt.selected_choice_key}
          language={lang}
          t={t}
          isLoggedIn={isLoggedIn}
        />
      )}
    </article>
  );
}
