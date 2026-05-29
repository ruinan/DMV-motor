"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  Clock,
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
  const [remainingSec, setRemainingSec] = useState<number | null>(null);
  const savedTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const submittingRef = useRef(false);
  const autoSubmittedRef = useRef(false);

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

  // Submit the attempt — manual on the last question, or auto when the timer
  // hits zero. Hoisted as a callback so the countdown effect can fire it.
  // The server records a timeout as ended_by_timeout if the limit truly elapsed.
  const submitNow = useCallback(async () => {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    setErrorMsg(null);
    try {
      const res = await apiFetch<SubmitResponse>(
        `/api/v1/mock-exams/attempts/${attemptId}/submit`,
        { method: "POST" },
      );
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
      submittingRef.current = false;
    }
  }, [attemptId, queryClient, t]);

  // Server-anchored countdown: deadline = started_at + time_limit. Re-derived
  // (not restarted) on refresh. At zero we auto-submit.
  const deadlineMs =
    attempt.data && attempt.data.status === "in_progress"
      ? new Date(attempt.data.started_at).getTime() +
        attempt.data.time_limit_seconds * 1000
      : null;
  useEffect(() => {
    if (deadlineMs == null || result || terminated) return;
    const tick = () => {
      const rem = Math.max(0, Math.round((deadlineMs - Date.now()) / 1000));
      setRemainingSec(rem);
      if (rem <= 0 && !autoSubmittedRef.current) {
        autoSubmittedRef.current = true;
        void submitNow();
      }
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [deadlineMs, result, terminated, submitNow]);

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

  // Cold re-open of an already-finished attempt: refresh after submitting, or
  // clicking it in the mock history. The live result/terminated flags are gone,
  // but the fetched status is terminal — show a read-only result view (score +
  // cached AI plan + review CTAs) instead of dropping back into answering mode.
  if (attempt.data && attempt.data.status !== "in_progress") {
    return (
      <FinishedView
        t={t}
        attemptId={attemptId}
        status={attempt.data.status}
        scorePercent={attempt.data.score_percent}
        correctCount={attempt.data.correct_count}
        wrongCount={attempt.data.wrong_count}
        onNewMock={() => router.push(`/${lang}/mock`)}
        onMistakes={() => router.push(`/${lang}/mistakes`)}
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
        <AiReviewPlanBlock t={t} attemptId={attemptId} />
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
      // Time ran out mid-answer: the server finalized the attempt as a timeout.
      // Refetch so the view transitions to the read-only result.
      if (err instanceof ApiError && err.code === "MOCK_EXPIRED") {
        queryClient.invalidateQueries({ queryKey: ["mock-attempt", attemptId] });
        return;
      }
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
      {/* Countdown — turns red in the final minute. */}
      {remainingSec !== null && (
        <div
          className={`flex items-center justify-center gap-1.5 text-sm font-semibold tabular-nums ${
            remainingSec <= 60 ? "text-destructive" : "text-muted-foreground"
          }`}
        >
          <Clock className="size-4" />
          {formatMMSS(remainingSec)}
        </div>
      )}

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
            onClick={() => void submitNow()}
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

      <AiReviewPlanBlock t={t} attemptId={attemptId} />

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
 * Read-only view for a finished attempt opened cold (refresh after submit, or
 * from mock history). Shows the persisted score when available + the cached AI
 * review plan + review CTAs. Detailed per-question review lives in Mistakes —
 * wrong answers were already folded into the mistake list, so the primary CTA
 * points there.
 */
function FinishedView({
  t,
  attemptId,
  status,
  scorePercent,
  correctCount,
  wrongCount,
  onNewMock,
  onMistakes,
  onBack,
}: {
  t: Dictionary["mock"];
  attemptId: string;
  status: string;
  scorePercent: number;
  correctCount: number;
  wrongCount: number;
  onNewMock: () => void;
  onMistakes: () => void;
  onBack: () => void;
}) {
  // Exited attempts are never scored (sentinel -1); submitted / failed carry a
  // real score. Failure status is always a fail regardless of the raw number.
  const scored = scorePercent >= 0;
  const passed = scored && status === "submitted" && scorePercent >= PASS_THRESHOLD;

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {t.resultTitle}
        </h1>
      </header>

      {scored ? (
        <section className="rounded-xl border bg-card p-8 text-center shadow-sm">
          <p className="text-sm uppercase tracking-wider text-muted-foreground">
            {t.scorePercent}
          </p>
          <p className="mt-2 text-6xl font-bold tabular-nums text-foreground">
            {scorePercent}%
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
                {correctCount}
              </p>
            </div>
            <div className="rounded-lg border border-border bg-background p-3">
              <p className="text-muted-foreground">{t.wrongCount}</p>
              <p className="mt-1 text-2xl font-semibold tabular-nums text-destructive">
                {wrongCount}
              </p>
            </div>
          </div>
        </section>
      ) : (
        <section className="rounded-xl border border-dashed border-border bg-card p-6 text-center text-sm text-muted-foreground shadow-sm">
          {t.exitedSummary}
        </section>
      )}

      {scored && <AiReviewPlanBlock t={t} attemptId={attemptId} />}

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onBack}>
          {t.backToDashboard}
        </Button>
        <Button variant="outline" onClick={onMistakes}>
          {t.reviewMistakes}
        </Button>
        <Button onClick={onNewMock}>{t.tryAgain}</Button>
      </div>
    </div>
  );
}

/**
 * Post-exam AI review plan. Generation is automatic — a background job kicks
 * off when the mock completes — so this block just reflects the job's state:
 * a spinner while it's running, the plan once ready, a neutral note if AI is
 * off or the job didn't produce one. The user never triggers it (no free-text,
 * no button), consistent with the locked-payload AI contract.
 */
function AiReviewPlanBlock({
  t,
  attemptId,
}: {
  t: Dictionary["mock"];
  attemptId: string;
}) {
  const ai = useAiReviewPlan(attemptId);

  if (ai.state === "ready") {
    return (
      <section className="rounded-xl border border-primary/30 bg-primary/5 p-6 shadow-sm">
        <h2 className="mb-2 flex items-center gap-1.5 text-sm font-semibold uppercase tracking-wider text-primary">
          <Sparkles className="size-4" />
          {t.aiPlanHeading}
        </h2>
        <p className="whitespace-pre-line text-sm leading-relaxed text-foreground">
          {ai.plan}
        </p>
      </section>
    );
  }

  if (ai.state === "loading" || ai.state === "pending") {
    return (
      <section className="flex items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-card p-6 text-sm text-muted-foreground shadow-sm">
        <Loader2 className="size-4 animate-spin" />
        {t.aiPlanLoading}
      </section>
    );
  }

  // stalled (job didn't finish) or unavailable (AI off) — neutral, no action.
  return (
    <section className="rounded-xl border border-dashed border-border bg-card p-6 text-center text-sm text-muted-foreground shadow-sm">
      {ai.state === "unavailable" ? t.aiPlanUnavailable : t.aiPlanStalled}
    </section>
  );
}

function formatMMSS(totalSeconds: number): string {
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
