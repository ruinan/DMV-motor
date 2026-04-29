"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Choice = { key: string; text: string };

type Question = {
  question_id: string;
  variant_id: string;
  stem: string;
  choices: Choice[];
};

type TaskQuestionsResponse = {
  review_task_id: string;
  task_type: string;
  topic_id: string;
  questions: Question[];
};

type AnswerResponse = {
  question_id: string;
  is_correct: boolean;
  correct_choice_key: string;
  explanation: string;
  task_progress: { answered_count: number; target_count: number };
};

type Feedback = {
  questionId: string;
  picked: string;
  isCorrect: boolean;
  correctKey: string;
  explanation: string;
};

type Props = {
  t: Dictionary["review"];
  lang: Locale;
  taskId: string;
};

export function ReviewTaskRunner({ t, lang, taskId }: Props) {
  const router = useRouter();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["review-task", taskId, lang],
    queryFn: () =>
      apiFetch<TaskQuestionsResponse>(
        `/api/v1/review/tasks/${taskId}/questions?language=${lang}`,
      ),
    enabled: !!user,
    staleTime: 30_000,
  });

  const [index, setIndex] = useState(0);
  const [picked, setPicked] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [answeredCount, setAnsweredCount] = useState(0);
  const [completed, setCompleted] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Reset transient state when the task or current question changes.
  // React 19 pattern: adjust state during render by comparing against a tracked key.
  const questionKey = `${taskId}:${index}`;
  const [trackedKey, setTrackedKey] = useState(questionKey);
  if (trackedKey !== questionKey) {
    setTrackedKey(questionKey);
    setPicked(null);
    setFeedback(null);
    setSubmitError(null);
  }

  if (!user) return null;

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t.loading}</p>;
  }

  if (error) {
    const noPass = error instanceof ApiError && error.code === "ACCESS_DENIED";
    return (
      <div className="flex flex-col gap-4">
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {noPass ? t.passRequired : t.errorGeneric}
        </div>
        <Link
          href={`/${lang}/review`}
          className="text-sm text-muted-foreground hover:text-foreground hover:underline"
        >
          ← {t.backToPack}
        </Link>
      </div>
    );
  }

  if (!data) return null;

  const total = data.questions.length;
  const question = data.questions[index];

  if (completed || index >= total) {
    return (
      <div className="rounded-xl border bg-card p-8 text-center shadow-sm">
        <CheckCircle2 className="mx-auto mb-3 size-12 text-primary" />
        <h2 className="text-2xl font-semibold">{t.completedTitle}</h2>
        <p className="mt-2 text-muted-foreground">{t.completedBody}</p>
        <div className="mt-6 flex justify-center gap-3">
          <Button onClick={() => router.push(`/${lang}/review`)}>
            {t.backToPack}
          </Button>
        </div>
      </div>
    );
  }

  async function submit() {
    if (!picked || submitting || !question) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const res = await apiFetch<AnswerResponse>(
        `/api/v1/review/tasks/${taskId}/answers`,
        {
          method: "POST",
          body: JSON.stringify({
            question_id: question.question_id,
            variant_id: question.variant_id,
            selected_choice_key: picked,
            language: lang,
          }),
        },
      );
      setFeedback({
        questionId: res.question_id,
        picked,
        isCorrect: res.is_correct,
        correctKey: res.correct_choice_key,
        explanation: res.explanation,
      });
      setAnsweredCount(res.task_progress.answered_count);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : t.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  }

  async function next() {
    if (index + 1 >= total) {
      // Final question — call complete
      setCompleting(true);
      try {
        await apiFetch(`/api/v1/review/tasks/${taskId}/complete`, {
          method: "POST",
        });
        // Invalidate pack so list reflects task completion on return
        queryClient.invalidateQueries({ queryKey: ["review-pack"] });
      } catch {
        // Even on error we treat the task as locally complete; backend may
        // already have flipped status during the final answer.
      } finally {
        setCompleting(false);
        setCompleted(true);
      }
      return;
    }
    setIndex((i) => i + 1);
  }

  const showFeedback = !!feedback;
  const correctKey = feedback?.correctKey ?? null;
  const pickedKey = feedback?.picked ?? picked;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6">
      {/* Progress */}
      <div>
        <div className="mb-2 flex items-baseline justify-between text-sm">
          <span className="text-muted-foreground">
            {t.taskProgress}
          </span>
          <span className="font-medium tabular-nums">
            {Math.max(answeredCount, index)} / {total}
          </span>
        </div>
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-primary transition-all duration-500"
            style={{
              width: `${total > 0 ? Math.round((Math.max(answeredCount, index) / total) * 100) : 0}%`,
            }}
          />
        </div>
      </div>

      {/* Question */}
      <div className="rounded-xl border bg-card p-6 shadow-sm md:p-8">
        <p className="text-base leading-relaxed sm:text-lg">{question.stem}</p>

        <ul className="mt-6 flex flex-col gap-3">
          {question.choices.map((c) => {
            const selected = pickedKey === c.key;
            const isCorrect = showFeedback && correctKey === c.key;
            const isWrongPick = showFeedback && selected && correctKey !== c.key;

            const stateClass = showFeedback
              ? isCorrect
                ? "border-primary bg-primary/10 text-foreground"
                : isWrongPick
                  ? "border-destructive bg-destructive/10 text-foreground"
                  : "border-border bg-background text-muted-foreground"
              : selected
                ? "border-primary bg-primary/10 text-foreground"
                : "border-border bg-background hover:border-primary/50 hover:bg-muted";

            return (
              <li key={c.key}>
                <button
                  type="button"
                  disabled={showFeedback || submitting}
                  onClick={() => setPicked(c.key)}
                  className={`flex w-full items-start gap-3 rounded-lg border-2 px-4 py-3 text-left transition-colors disabled:cursor-default ${stateClass}`}
                >
                  <span className="mt-0.5 inline-flex size-7 shrink-0 items-center justify-center rounded-full border border-border bg-background text-sm font-semibold">
                    {c.key}
                  </span>
                  <span className="flex-1 text-sm sm:text-base">{c.text}</span>
                  {showFeedback && isCorrect && (
                    <CheckCircle2 className="size-5 shrink-0 text-primary" />
                  )}
                  {showFeedback && isWrongPick && (
                    <XCircle className="size-5 shrink-0 text-destructive" />
                  )}
                </button>
              </li>
            );
          })}
        </ul>

        {showFeedback && feedback && (
          <div
            className={`mt-6 rounded-lg border p-4 text-sm ${
              feedback.isCorrect
                ? "border-primary/40 bg-primary/5 text-foreground"
                : "border-destructive/40 bg-destructive/5 text-foreground"
            }`}
          >
            <p className="font-semibold">
              {feedback.isCorrect ? t.correct : t.incorrect}
            </p>
            {feedback.explanation && (
              <p className="mt-2 leading-relaxed text-muted-foreground">
                <span className="font-medium text-foreground">
                  {t.explanation}:{" "}
                </span>
                {feedback.explanation}
              </p>
            )}
          </div>
        )}

        {submitError && (
          <p className="mt-4 text-sm text-destructive">{submitError}</p>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between gap-3">
        <Link
          href={`/${lang}/review`}
          className="text-sm text-muted-foreground hover:text-foreground hover:underline"
        >
          ← {t.exit}
        </Link>
        {!showFeedback ? (
          <Button
            onClick={submit}
            disabled={!picked || submitting}
            size="lg"
          >
            {submitting ? t.submitting : t.submitAnswer}
          </Button>
        ) : (
          <Button onClick={next} disabled={completing} size="lg">
            {completing
              ? t.finishing
              : index + 1 >= total
                ? t.finishTask
                : t.nextQuestion}
          </Button>
        )}
      </div>
    </div>
  );
}
