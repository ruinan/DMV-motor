"use client";

import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { AiExplainBlock } from "@/components/ai-explain-block";
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
  // True while the attempt detail is (re)fetching — show a spinner rather than
  // a misleading empty state. The review data itself is served from cache (no
  // AI call); AI is only invoked when the learner taps a deep-dive.
  loading?: boolean;
};

/**
 * Post-exam per-question review: each answered question marked right/wrong with
 * its explanation and an "Ask AI why" button on wrong answers. The section is
 * always rendered (heading + list / spinner / empty note) so it never silently
 * disappears — the old version returned null when there were no items, which
 * looked like the review was missing entirely.
 */
export function MockReview({
  t,
  lang,
  questions,
  answers,
  isLoggedIn,
  loading = false,
}: Props) {
  const qById = new Map(questions.map((q) => [q.question_id, q]));
  const items = answers
    .map((a) => ({ a, q: qById.get(a.question_id) }))
    .filter((x): x is { a: MockSavedAnswer; q: MockAttemptQuestion } => !!x.q);

  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold tracking-tight text-foreground">
        {t.reviewTitle}
      </h2>
      {items.length > 0 ? (
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
      ) : loading ? (
        <div className="flex items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-card p-8 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          {t.loading ?? "Loading…"}
        </div>
      ) : (
        <p className="rounded-xl border border-dashed border-border bg-card p-6 text-center text-sm text-muted-foreground">
          {t.reviewEmpty}
        </p>
      )}
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
              className={`flex items-center gap-3 rounded-lg border-2 px-3 py-2 text-sm ${tone}`}
            >
              <span className="inline-flex size-6 shrink-0 items-center justify-center rounded-full border border-border bg-background text-xs font-semibold">
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
        <AiExplainBlock
          questionId={a.question_id}
          variantId={q.variant_id}
          selectedChoiceKey={a.selected_choice_key}
          language={lang}
          t={t}
          isLoggedIn={isLoggedIn}
        />
      )}
    </article>
  );
}
