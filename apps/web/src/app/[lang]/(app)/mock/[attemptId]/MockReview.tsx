"use client";

import { CheckCircle2, Sparkles, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAiExplain } from "@/lib/hooks/use-ai-explain";
import type {
  MockAttemptQuestion,
  MockSavedAnswer,
} from "@/lib/hooks/use-mock-attempt";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["mock"];
  lang: Locale;
  questions: MockAttemptQuestion[];
  answers: MockSavedAnswer[];
  isLoggedIn: boolean;
};

/**
 * Post-exam per-question review: each answered question marked right/wrong with
 * its explanation and an "Ask AI why" button on wrong answers. Mirrors the
 * practice attempt review; the AI payload is locked to structured fields.
 */
export function MockReview({ t, lang, questions, answers, isLoggedIn }: Props) {
  const qById = new Map(questions.map((q) => [q.question_id, q]));
  const items = answers
    .map((a) => ({ a, q: qById.get(a.question_id) }))
    .filter((x): x is { a: MockSavedAnswer; q: MockAttemptQuestion } => !!x.q);

  if (items.length === 0) return null;

  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold tracking-tight text-foreground">
        {t.reviewTitle}
      </h2>
      <ol className="flex flex-col gap-4">
        {items.map(({ a, q }, idx) => (
          <li key={a.question_id}>
            <MockReviewItem
              t={t}
              lang={lang}
              index={idx}
              q={q}
              a={a}
              isLoggedIn={isLoggedIn}
            />
          </li>
        ))}
      </ol>
    </section>
  );
}

function MockReviewItem({
  t,
  lang,
  index,
  q,
  a,
  isLoggedIn,
}: {
  t: Dictionary["mock"];
  lang: Locale;
  index: number;
  q: MockAttemptQuestion;
  a: MockSavedAnswer;
  isLoggedIn: boolean;
}) {
  const ai = useAiExplain();
  return (
    <article className="rounded-xl border bg-card p-5 shadow-sm">
      <header className="mb-3 flex items-center gap-2 text-sm">
        <span className="inline-flex size-6 items-center justify-center rounded-full bg-muted text-xs font-semibold tabular-nums">
          {index + 1}
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

      <p className="text-base leading-relaxed">{q.stem}</p>

      <ul className="mt-4 flex flex-col gap-2">
        {q.choices.map((c) => {
          const isCorrect = c.key === a.correct_choice_key;
          const wrongPick = c.key === a.selected_choice_key && !isCorrect;
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
              {wrongPick && <XCircle className="size-4 shrink-0 text-destructive" />}
            </li>
          );
        })}
      </ul>

      {a.explanation && (
        <p className="mt-3 rounded-md bg-muted/50 p-3 text-sm leading-relaxed text-muted-foreground">
          <span className="font-medium text-foreground">{t.explanation}:</span>{" "}
          {a.explanation}
        </p>
      )}

      {!a.is_correct && (
        <div className="mt-3 border-t border-border/60 pt-3">
          {ai.state.kind === "ok" ? (
            <>
              <p className="mb-1 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-primary">
                <Sparkles className="size-3.5" />
                {t.aiExplainHeading}
                {ai.state.cached && (
                  <span className="font-normal normal-case tracking-normal text-muted-foreground">
                    {t.aiExplainCached}
                  </span>
                )}
              </p>
              <p className="text-sm leading-relaxed text-foreground">
                {ai.state.text}
              </p>
            </>
          ) : ai.state.kind === "error" ? (
            <p className="text-xs text-destructive">
              {ai.state.code === "RATE_LIMITED"
                ? t.aiExplainCooldown
                : ai.state.code === "AI_UNAVAILABLE"
                  ? t.aiExplainUnavailable
                  : t.aiExplainError}
            </p>
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  ai.explain({
                    question_id: a.question_id,
                    variant_id: q.variant_id,
                    selected_choice_key: a.selected_choice_key,
                    language: lang,
                  })
                }
                disabled={!isLoggedIn || ai.state.kind === "loading"}
                className="gap-1.5"
              >
                <Sparkles className="size-4" />
                {ai.state.kind === "loading"
                  ? t.aiExplainLoading
                  : t.aiExplainButton}
              </Button>
              {!isLoggedIn && (
                <p className="mt-2 text-xs text-muted-foreground">
                  {t.aiExplainAuthRequired}
                </p>
              )}
            </>
          )}
        </div>
      )}
    </article>
  );
}
