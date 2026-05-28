"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  Loader2,
  Sparkles,
  XCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
import { useMockAttempt } from "@/lib/hooks/use-mock-attempt";
import { useAiReviewPlan } from "@/lib/hooks/use-ai-review-plan";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type SubmitResponse = {
  mock_attempt_id: string;
  status: string;
  score_percent: number;
  correct_count: number;
  wrong_count: number;
  weak_topics: { topic_id: string; label: string }[];
  next_action: { type: string; label: string };
};

type SaveAnswerResponse = {
  saved: boolean;
  answered_count: number;
  is_correct: boolean;
  correct_choice_key: string;
  wrong_count: number;
  max_allowed_wrong: number;
  should_terminate: boolean;
};

type QuestionFeedback = {
  isCorrect: boolean;
  correctKey: string;
};

type SaveStatus = "idle" | "saving" | "saved" | "error";

const PASS_THRESHOLD = 85;

type Props = {
  t: Dictionary["mock"];
  lang: Locale;
  attemptId: string;
};

export function MockExam({ t, lang, attemptId }: Props) {
  const router = useRouter();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const topicMap = useTopicNameMap(lang);

  // Source of truth: backend attempt detail. Refresh / new tab / cross-device
  // all hydrate from this; no sessionStorage dependency. Saved picks are
  // pre-populated from the persisted answers so the user lands back on the
  // same question with their previous answers intact.
  const attempt = useMockAttempt(attemptId, lang);

  const [index, setIndex] = useState(0);
  const [picks, setPicks] = useState<Map<string, string>>(new Map());
  // Per-question correctness verdict (server-computed at save time). Drives
  // the inline "right/wrong" UI and gates the Next button.
  const [feedback, setFeedback] = useState<Map<string, QuestionFeedback>>(
    new Map(),
  );
  // True when the wrong-answer cap was exceeded — the attempt has been
  // finalized as ended_by_failure on the backend, no further answers are
  // accepted, and the user lands on a failure result view.
  const [terminated, setTerminated] = useState(false);
  // Tracks which attemptId we've already initialised picks for. React 19's
  // "set state during render in response to changed props" pattern; using a
  // ref here would trip the no-mutate-refs-in-render rule.
  const [initialisedFor, setInitialisedFor] = useState<string | null>(null);
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const [submitting, setSubmitting] = useState(false);
  const [exiting, setExiting] = useState(false);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [result, setResult] = useState<SubmitResponse | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const savedTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (savedTimer.current) clearTimeout(savedTimer.current);
    };
  }, []);

  // Native browser warning when the user tries to close the tab or hit
  // back/forward mid-exam. Only armed while the exam is live (not after
  // submit / terminate). Per spec: the in-app "Exit exam" button uses our
  // own dialog; the tab-close / URL-change path uses Chrome's prompt.
  const examLive = !result && !terminated;
  useEffect(() => {
    if (!examLive) return;
    function onBeforeUnload(e: BeforeUnloadEvent) {
      e.preventDefault();
      e.returnValue = "";
    }
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [examLive]);

  // Seed picks from server-persisted saved_answers on first successful fetch.
  if (attempt.data && initialisedFor !== attempt.data.mock_attempt_id) {
    setInitialisedFor(attempt.data.mock_attempt_id);
    const seeded = new Map<string, string>();
    for (const a of attempt.data.saved_answers) {
      seeded.set(a.question_id, a.selected_choice_key);
    }
    setPicks(seeded);
  }

  if (!user) return null;

  // Loading state — fetching attempt detail. Single spinner, consistent with
  // the practice page transitions.
  if (attempt.isLoading && !attempt.data) {
    return (
      <div className="mx-auto flex w-full max-w-xl flex-col items-center justify-center gap-4 py-16">
        <Loader2 className="size-10 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">{t.loading ?? "Loading…"}</p>
      </div>
    );
  }

  if (attempt.error || (!attempt.data && !result)) {
    return (
      <div className="mx-auto flex w-full max-w-xl flex-col gap-4">
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {t.errorGeneric}
        </div>
        <Link
          href={`/${lang}/mock`}
          className="text-sm text-muted-foreground hover:text-foreground hover:underline"
        >
          ← {t.backToDashboard}
        </Link>
      </div>
    );
  }

  if (result) {
    return (
      <ResultView
        t={t}
        lang={lang}
        result={result}
        topicMap={topicMap}
        attemptId={attemptId}
        onTryAgain={() => router.push(`/${lang}/mock`)}
        onBack={() => router.push(`/${lang}/dashboard`)}
      />
    );
  }

  // Wrong-answer cap exceeded — server has finalized the attempt. Show a
  // simple failure summary; the user can either go back or start a new mock.
  if (terminated) {
    const verdicts = Array.from(feedback.values());
    const wrong = verdicts.filter((f) => !f.isCorrect).length;
    const answered = verdicts.length;
    return (
      <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
        <div className="rounded-xl border border-destructive/40 bg-destructive/5 p-6 text-center shadow-sm">
          <AlertCircle className="mx-auto mb-3 size-12 text-destructive" />
          <h2 className="text-2xl font-bold text-foreground">
            {t.terminatedTitle}
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">
            {t.terminatedBody
              .replace("{answered}", String(answered))
              .replace("{wrong}", String(wrong))}
          </p>
        </div>
        <AiReviewPlanBlock t={t} lang={lang} attemptId={attemptId} />
        <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Button onClick={() => router.push(`/${lang}/mock`)}>
            {t.terminatedTryAgain}
          </Button>
          <Button variant="outline" onClick={() => router.push(`/${lang}/dashboard`)}>
            {t.backToDashboard}
          </Button>
        </div>
      </div>
    );
  }

  // Once we're past the loading + error guards above, attempt.data is set
  // (the result-only branch below also returns early if there's no data).
  const questions = attempt.data?.questions ?? [];
  const total = questions.length;
  const question = questions[index];
  const pickedKey = question ? picks.get(question.question_id) ?? null : null;
  const answeredCount = picks.size;
  const isLast = index + 1 >= total;

  // Result view (post-submit) is rendered before this block lives, but we
  // still need to gate on having a current question for the answering UI.
  if (!result && !question) {
    return (
      <div className="mx-auto flex w-full max-w-xl flex-col items-center justify-center gap-4 py-16">
        <Loader2 className="size-10 animate-spin text-primary" />
      </div>
    );
  }

  async function pick(choiceKey: string) {
    if (saveStatus === "saving") return;
    // Once feedback is shown for this question, locking the choice. No
    // changing your mind mid-question per the exam UX rules.
    if (feedback.has(question.question_id)) return;

    setPicks((prev) => {
      const next = new Map(prev);
      next.set(question.question_id, choiceKey);
      return next;
    });
    setErrorMsg(null);
    setSaveStatus("saving");
    if (savedTimer.current) {
      clearTimeout(savedTimer.current);
      savedTimer.current = null;
    }
    try {
      const res = await apiFetch<SaveAnswerResponse>(
        `/api/v1/mock-exams/attempts/${attemptId}/answers`,
        {
          method: "POST",
          body: JSON.stringify({
            question_id: question.question_id,
            variant_id: question.variant_id,
            selected_choice_key: choiceKey,
          }),
        },
      );
      // Capture the per-question verdict so the UI can paint correctness +
      // unlock the Next button.
      setFeedback((prev) => {
        const next = new Map(prev);
        next.set(question.question_id, {
          isCorrect: res.is_correct,
          correctKey: res.correct_choice_key,
        });
        return next;
      });
      setSaveStatus("saved");
      savedTimer.current = setTimeout(() => setSaveStatus("idle"), 1500);
      if (res.should_terminate) {
        setTerminated(true);
      }
    } catch (err) {
      setSaveStatus("error");
      setErrorMsg(err instanceof ApiError ? err.message : t.errorGeneric);
      // Roll back the pick so the user can try again with a different choice.
      setPicks((prev) => {
        const next = new Map(prev);
        next.delete(question.question_id);
        return next;
      });
    }
  }

  function goNext() {
    if (index + 1 < total) setIndex((i) => i + 1);
  }

  async function onSubmit() {
    if (submitting) return;
    setSubmitting(true);
    setErrorMsg(null);
    try {
      const res = await apiFetch<SubmitResponse>(
        `/api/v1/mock-exams/attempts/${attemptId}/submit`,
        { method: "POST" },
      );
      // Mock outcome shifts mistakes / readiness — invalidate so other pages refetch
      queryClient.invalidateQueries({ queryKey: ["mock-access"] });
      queryClient.invalidateQueries({ queryKey: ["mistakes"] });
      queryClient.invalidateQueries({ queryKey: ["mistakes-count"] });
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["mock-history"] });
      queryClient.invalidateQueries({ queryKey: ["mock-stats"] });
      setResult(res);
    } catch (err) {
      setErrorMsg(err instanceof ApiError ? err.message : t.errorGeneric);
      setSubmitting(false);
    }
  }

  async function confirmExit() {
    if (exiting) return;
    setExiting(true);
    setErrorMsg(null);
    try {
      await apiFetch(`/api/v1/mock-exams/attempts/${attemptId}/exit`, {
        method: "POST",
      });
      queryClient.invalidateQueries({ queryKey: ["mock-access"] });
      queryClient.invalidateQueries({ queryKey: ["mock-history"] });
      router.push(`/${lang}/mock`);
    } catch (err) {
      setErrorMsg(err instanceof ApiError ? err.message : t.errorGeneric);
      setExiting(false);
      setExitConfirmOpen(false);
    }
  }

  const progressPct = total > 0 ? Math.round((answeredCount / total) * 100) : 0;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6">
      {/* Progress header */}
      <div>
        <div className="mb-2 flex items-baseline justify-between text-sm">
          <span className="text-muted-foreground">
            {t.questionOf
              .replace("{current}", String(index + 1))
              .replace("{total}", String(total))}
          </span>
          <span className="font-medium tabular-nums text-muted-foreground">
            {t.answeredOf
              .replace("{answered}", String(answeredCount))
              .replace("{total}", String(total))}
          </span>
        </div>
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-primary transition-all duration-500"
            style={{ width: `${progressPct}%` }}
          />
        </div>
      </div>

      {/* Save status */}
      <div className="flex h-5 items-center justify-end text-xs text-muted-foreground">
        {saveStatus === "saving" && (
          <span className="flex items-center gap-1">
            <Loader2 className="size-3 animate-spin" />
            {t.saving}
          </span>
        )}
        {saveStatus === "saved" && (
          <span className="flex items-center gap-1 text-primary">
            <CheckCircle2 className="size-3" />
            {t.saved}
          </span>
        )}
      </div>

      {/* Question card */}
      <div className="rounded-xl border bg-card p-6 shadow-sm md:p-8">
        <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {t.questionLabel.replace("{n}", String(index + 1))}
        </p>
        <p className="mt-2 text-base leading-relaxed sm:text-lg">
          {question.stem}
        </p>

        <ul className="mt-6 flex flex-col gap-3">
          {question.choices.map((c) => {
            const selected = pickedKey === c.key;
            const fb = feedback.get(question.question_id);
            const showVerdict = !!fb;
            const isCorrect = showVerdict && c.key === fb.correctKey;
            const isWrongPick = showVerdict && selected && c.key !== fb.correctKey;

            const cls = showVerdict
              ? isCorrect
                ? "border-success bg-success/10 text-foreground"
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
                  disabled={submitting || exiting || showVerdict}
                  onClick={() => pick(c.key)}
                  className={`flex w-full items-start gap-3 rounded-lg border-2 px-4 py-3 text-left transition-colors disabled:cursor-default ${cls}`}
                >
                  <span className="mt-0.5 inline-flex size-7 shrink-0 items-center justify-center rounded-full border border-border bg-background text-sm font-semibold">
                    {c.key}
                  </span>
                  <span className="flex-1 text-sm sm:text-base">{c.text}</span>
                  {showVerdict && isCorrect && (
                    <CheckCircle2 className="size-5 shrink-0 text-success" />
                  )}
                  {showVerdict && isWrongPick && (
                    <XCircle className="size-5 shrink-0 text-destructive" />
                  )}
                </button>
              </li>
            );
          })}
        </ul>

        {errorMsg && (
          <p className="mt-4 text-sm text-destructive">{errorMsg}</p>
        )}
      </div>

      {/* Nav row — no Previous button: linear exam flow, can't revisit
          earlier questions. Next is gated by per-question feedback (must
          answer first); on the last question, Next becomes Submit. */}
      <div className="flex items-center justify-end gap-3">
        {!isLast ? (
          <Button
            onClick={goNext}
            disabled={!feedback.has(question.question_id) || submitting || exiting}
          >
            {t.next}
            <ChevronRight className="size-4" />
          </Button>
        ) : (
          <Button
            onClick={onSubmit}
            disabled={!feedback.has(question.question_id) || submitting || exiting}
            size="lg"
          >
            {submitting ? t.submitting : t.submitExam}
          </Button>
        )}
      </div>

      {/* Exit row */}
      <div className="flex justify-center">
        <button
          type="button"
          onClick={() => setExitConfirmOpen(true)}
          disabled={submitting || exiting}
          className="text-sm text-muted-foreground hover:text-destructive hover:underline disabled:opacity-60"
        >
          {exiting ? t.exiting : t.exit}
        </button>
      </div>

      <ConfirmDialog
        open={exitConfirmOpen}
        title={t.exitConfirmTitle}
        body={t.exitConfirmBody}
        confirmLabel={t.exitConfirmYes}
        cancelLabel={t.exitConfirmCancel}
        variant="destructive"
        busy={exiting}
        busyLabel={t.exiting}
        onConfirm={confirmExit}
        onCancel={() => setExitConfirmOpen(false)}
      />
    </div>
  );
}

function ResultView({
  t,
  lang,
  result,
  topicMap,
  attemptId,
  onTryAgain,
  onBack,
}: {
  t: Dictionary["mock"];
  lang: Locale;
  result: SubmitResponse;
  topicMap: Map<string, string>;
  attemptId: string;
  onTryAgain: () => void;
  onBack: () => void;
}) {
  const passed = result.score_percent >= PASS_THRESHOLD;
  void lang; // reserved for future locale-aware formatting

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {t.resultTitle}
        </h1>
      </header>

      <section className="rounded-xl border bg-card p-8 text-center shadow-sm">
        <p className="text-sm uppercase tracking-wider text-muted-foreground">
          {t.scorePercent}
        </p>
        <p className="mt-2 text-6xl font-bold tabular-nums text-foreground">
          {result.score_percent}%
        </p>
        <span
          className={`mt-4 inline-block rounded-full px-3 py-1 text-sm font-medium ${
            passed
              ? "bg-primary/10 text-primary"
              : "bg-destructive/10 text-destructive"
          }`}
        >
          {passed ? t.passed : t.failed}
        </span>

        <div className="mt-6 grid grid-cols-2 gap-4 text-sm">
          <div className="rounded-lg border border-border bg-background p-3">
            <p className="text-muted-foreground">{t.correctCount}</p>
            <p className="mt-1 text-2xl font-semibold tabular-nums text-primary">
              {result.correct_count}
            </p>
          </div>
          <div className="rounded-lg border border-border bg-background p-3">
            <p className="text-muted-foreground">{t.wrongCount}</p>
            <p className="mt-1 text-2xl font-semibold tabular-nums text-destructive">
              {result.wrong_count}
            </p>
          </div>
        </div>
      </section>

      <section className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          {t.weakTopics}
        </h2>
        {result.weak_topics.length === 0 ? (
          <p className="mt-3 text-sm text-muted-foreground">
            {t.noWeakTopics}
          </p>
        ) : (
          <ul className="mt-3 flex flex-wrap gap-2">
            {result.weak_topics.map((wt) => (
              <li
                key={wt.topic_id}
                className="rounded-full bg-destructive/10 px-3 py-1 text-xs font-medium text-destructive"
              >
                {topicMap.get(wt.topic_id) ?? wt.label}
              </li>
            ))}
          </ul>
        )}
      </section>

      <AiReviewPlanBlock t={t} lang={lang} attemptId={attemptId} />

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onBack}>
          {t.backToDashboard}
        </Button>
        <Button onClick={onTryAgain}>{t.tryAgain}</Button>
      </div>
    </div>
  );
}

/**
 * Post-exam AI review plan — click-to-generate, cached server-side per
 * attempt. Available on both the submitted result and the auto-terminated
 * failure screen. The AI is reached only via this button (no free-text),
 * consistent with the explanation-button locked-payload contract.
 */
function AiReviewPlanBlock({
  t,
  lang,
  attemptId,
}: {
  t: Dictionary["mock"];
  lang: Locale;
  attemptId: string;
}) {
  const ai = useAiReviewPlan();

  if (ai.state.kind === "ok") {
    return (
      <section className="rounded-xl border border-primary/30 bg-primary/5 p-6 shadow-sm">
        <h2 className="mb-2 flex items-center gap-1.5 text-sm font-semibold uppercase tracking-wider text-primary">
          <Sparkles className="size-4" />
          {t.aiPlanHeading}
          {ai.state.cached && (
            <span className="font-normal normal-case tracking-normal text-muted-foreground">
              {t.aiPlanCached}
            </span>
          )}
        </h2>
        <p className="whitespace-pre-line text-sm leading-relaxed text-foreground">
          {ai.state.text}
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-xl border border-dashed border-border bg-card p-6 text-center shadow-sm">
      <Sparkles className="mx-auto mb-2 size-6 text-primary" />
      <p className="mb-4 text-sm text-muted-foreground">{t.aiPlanPrompt}</p>
      <Button
        onClick={() => ai.generate(attemptId, lang)}
        disabled={ai.state.kind === "loading"}
        className="gap-1.5"
      >
        {ai.state.kind === "loading" ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            {t.aiPlanLoading}
          </>
        ) : (
          t.aiPlanButton
        )}
      </Button>
      {ai.state.kind === "error" && (
        <p className="mt-3 text-xs text-destructive">
          {ai.state.code === "AI_UNAVAILABLE"
            ? t.aiPlanUnavailable
            : t.aiPlanError}
        </p>
      )}
    </section>
  );
}
