"use client";

import Link from "next/link";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, History, Sparkles, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import { useMe } from "@/lib/hooks/use-me";
import { useAiExplain } from "@/lib/hooks/use-ai-explain";
import type { Dictionary, Locale } from "@/lib/dictionaries";
import { AttemptHistory } from "./AttemptHistory";

type Choice = { key: string; text: string };

type Question = {
  question_id: string;
  variant_id: string;
  stem: string;
  choices: Choice[];
};

type StartResponse = {
  session_id: string;
  entry_type: "free_trial" | "full";
  status: string;
  language: string;
  next_question: Question;
};

type SessionStatus = {
  session_id: string;
  status: string;
  answered_count: number;
  total_count: number;
};

type AnswerResponse = {
  question_id: string;
  is_correct: boolean;
  correct_choice_key: string;
  explanation: string;
  progress: { answered_count: number };
};

type Phase =
  | { kind: "idle" }
  | { kind: "starting" }
  | {
      kind: "answering";
      sessionId: string;
      question: Question;
      answeredCount: number;
      totalCount: number;
      picked: string | null;
      submitting: boolean;
    }
  | {
      kind: "feedback";
      sessionId: string;
      question: Question;
      picked: string;
      result: AnswerResponse;
      totalCount: number;
    }
  | { kind: "completed"; sessionId: string; reason: "finished" | "exited" }
  | { kind: "error"; message: string };

type Props = {
  t: Dictionary["practice"];
  lang: Locale;
};

export function PracticeFlow({ t, lang }: Props) {
  // Drive isLoggedIn off Firebase auth state, not /me fetch state — otherwise
  // the anonymous "Sign in or register" UI flashes for the half-second between
  // Firebase resolving the user and /me returning the profile.
  const { user } = useAuth();
  const me = useMe();
  const ai = useAiExplain();
  const queryClient = useQueryClient();
  const [phase, setPhase] = useState<Phase>({ kind: "idle" });
  const [historyOpen, setHistoryOpen] = useState(false);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);

  // Invalidate any Study Hub data that practice activity touches — answers
  // shift active mistakes / accuracy, starting/exiting sessions changes
  // history rows and the in_progress_practice payload on /me. Without this
  // the dashboard shows stale zeros for up to staleTime (60s).
  function invalidateStudyHub() {
    queryClient.invalidateQueries({ queryKey: ["me"] });
    queryClient.invalidateQueries({ queryKey: ["practice-stats"] });
    queryClient.invalidateQueries({ queryKey: ["practice-history"] });
    queryClient.invalidateQueries({ queryKey: ["topic-mastery"] });
    queryClient.invalidateQueries({ queryKey: ["mistakes"] });
    queryClient.invalidateQueries({ queryKey: ["mistakes-count"] });
  }

  // Resolve the session id whenever the phase carries one — used by the
  // history toggle / view. Only "answering" / "feedback" / "completed"
  // phases hold a session id.
  const activeSessionId =
    phase.kind === "answering" ||
    phase.kind === "feedback" ||
    phase.kind === "completed"
      ? phase.sessionId
      : null;

  const isLoggedIn = !!user;
  const hasPass = me.data?.access.has_active_pass ?? false;
  const entryType: "free_trial" | "full" = hasPass ? "full" : "free_trial";

  // -------------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------------

  async function start() {
    setPhase({ kind: "starting" });
    try {
      const startRes = await apiFetch<StartResponse>(
        "/api/v1/practice/sessions",
        {
          method: "POST",
          body: JSON.stringify({ entry_type: entryType, language: lang }),
        },
      );
      const status = await apiFetch<SessionStatus>(
        `/api/v1/practice/sessions/${startRes.session_id}`,
      );
      setPhase({
        kind: "answering",
        sessionId: startRes.session_id,
        question: startRes.next_question,
        answeredCount: status.answered_count,
        totalCount: status.total_count,
        picked: null,
        submitting: false,
      });
      invalidateStudyHub();
    } catch (err) {
      setPhase({ kind: "error", message: errorMessage(err, t) });
    }
  }

  // Resume an existing in-progress session — invoked from the idle screen when
  // /me reports a half-finished session for this user. Re-uses the session_id
  // and pulls the next unanswered question + current progress.
  async function resume(sessionId: string) {
    setPhase({ kind: "starting" });
    try {
      const question = await apiFetch<Question>(
        `/api/v1/practice/sessions/${sessionId}/next-question`,
      );
      const status = await apiFetch<SessionStatus>(
        `/api/v1/practice/sessions/${sessionId}`,
      );
      setPhase({
        kind: "answering",
        sessionId,
        question,
        answeredCount: status.answered_count,
        totalCount: status.total_count,
        picked: null,
        submitting: false,
      });
    } catch (err) {
      setPhase({ kind: "error", message: errorMessage(err, t) });
    }
  }

  async function submit() {
    if (phase.kind !== "answering" || !phase.picked || phase.submitting) return;
    setPhase({ ...phase, submitting: true });
    try {
      const res = await apiFetch<AnswerResponse>(
        `/api/v1/practice/sessions/${phase.sessionId}/answers`,
        {
          method: "POST",
          body: JSON.stringify({
            question_id: phase.question.question_id,
            variant_id: phase.question.variant_id,
            selected_choice_key: phase.picked,
          }),
        },
      );
      setPhase({
        kind: "feedback",
        sessionId: phase.sessionId,
        question: phase.question,
        picked: phase.picked,
        result: res,
        totalCount: phase.totalCount,
      });
      // Each submitted answer can shift active mistakes / accuracy stats.
      invalidateStudyHub();
    } catch (err) {
      setPhase({ kind: "error", message: errorMessage(err, t) });
    }
  }

  async function next() {
    if (phase.kind !== "feedback") return;
    const sessionId = phase.sessionId;
    const totalCount = phase.totalCount;
    const answeredCount = phase.result.progress.answered_count;
    // Each new question owns its own AI explanation lifecycle — drop the
    // previous result so the AI button doesn't carry over stale text.
    ai.reset();
    setPhase({ kind: "starting" });
    try {
      const q = await apiFetch<Question & { progress?: { answered_count: number } }>(
        `/api/v1/practice/sessions/${sessionId}/next-question`,
      );
      setPhase({
        kind: "answering",
        sessionId,
        question: { question_id: q.question_id, variant_id: q.variant_id, stem: q.stem, choices: q.choices },
        answeredCount,
        totalCount,
        picked: null,
        submitting: false,
      });
    } catch (err) {
      // Backend returns SESSION_COMPLETED (404) when there are no more questions
      if (err instanceof ApiError && err.code === "SESSION_COMPLETED") {
        await complete(sessionId, "finished");
        return;
      }
      setPhase({ kind: "error", message: errorMessage(err, t) });
    }
  }

  async function complete(sessionId: string, reason: "finished" | "exited") {
    try {
      await apiFetch(`/api/v1/practice/sessions/${sessionId}/complete`, {
        method: "POST",
      });
    } catch {
      // Even if complete fails, the user has effectively ended the session.
    }
    setPhase({ kind: "completed", sessionId, reason });
    invalidateStudyHub();
  }

  async function confirmExit() {
    if (phase.kind !== "answering" && phase.kind !== "feedback") return;
    setExitConfirmOpen(false);
    // Exit does NOT call /complete — leaves the session in_progress so the
    // user can resume from Study Hub or /practice next visit. Backend
    // /complete is reserved for finishing all questions.
    setPhase({ kind: "completed", sessionId: phase.sessionId, reason: "exited" });
    invalidateStudyHub();
  }

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------

  // History overlay — shown over any phase that has a session id. We don't
  // unmount the phase state, so closing returns the user to where they
  // were (mid-question pick is preserved).
  if (historyOpen && activeSessionId) {
    return (
      <Container>
        <AttemptHistory
          sessionId={activeSessionId}
          lang={lang}
          t={t}
          onBack={() => setHistoryOpen(false)}
        />
      </Container>
    );
  }

  if (phase.kind === "idle") {
    // Subtitle changes by auth state so a signed-in user doesn't see the
    // "sign in to unlock" copy that's only meaningful to anonymous visitors.
    const subtitle = !isLoggedIn
      ? t.subtitle
      : hasPass
        ? t.subtitlePaid
        : t.subtitleSignedInNoPass;
    return (
      <Container>
        <Header t={t} subtitle={subtitle} />
        {isLoggedIn ? (
          // Signed-in user: if an in-progress session exists, lead with Resume
          // and offer "Start fresh" as secondary. Otherwise the standard Start
          // CTA. Helper text links to /me#subscription for no-pass users so
          // the "you need a pass" copy isn't a dead end.
          <div className="flex flex-col items-center gap-3">
            {me.data?.learning.in_progress_practice ? (
              <>
                <Button
                  size="lg"
                  onClick={() =>
                    resume(me.data!.learning.in_progress_practice!.session_id)
                  }
                >
                  {t.resumeCta
                    .replace(
                      "{answered}",
                      String(me.data.learning.in_progress_practice.answered_count),
                    )
                    .replace(
                      "{total}",
                      String(me.data.learning.in_progress_practice.total_count),
                    )}
                </Button>
                <button
                  type="button"
                  onClick={start}
                  className="text-xs font-medium text-muted-foreground underline-offset-4 hover:underline"
                >
                  {t.startFresh}
                </button>
              </>
            ) : (
              <Button size="lg" onClick={start} disabled={me.isLoading}>
                {entryType === "full" ? t.startFull : t.startFreeTrial}
              </Button>
            )}
            {hasPass ? (
              <p className="text-xs text-muted-foreground">{t.startFull}</p>
            ) : (
              <p className="text-xs text-muted-foreground">
                <Link
                  href={`/${lang}/me#subscription`}
                  className="font-medium text-primary underline-offset-4 hover:underline"
                >
                  {t.errorPassRequired}
                </Link>
              </p>
            )}
          </div>
        ) : (
          // Anonymous: lead with the free-trial set since docs/development
          // /api-contract.md §4 explicitly opens it to all visitors. Sign-in
          // sits below as a secondary path for users who want full coverage
          // + saved progress.
          <div className="mx-auto flex w-full max-w-md flex-col items-center gap-5">
            {/* Primary — Free trial */}
            <div className="w-full rounded-xl border border-primary/30 bg-primary/5 p-6 text-center shadow-sm">
              <p className="mb-1 text-xs font-medium uppercase tracking-wider text-primary">
                {t.freeTrialBadge}
              </p>
              <h2 className="mb-1 text-lg font-semibold text-foreground">
                {t.freeTrialHeading}
              </h2>
              <p className="mb-4 text-sm text-muted-foreground">
                {t.freeTrialBody}
              </p>
              <Button
                size="lg"
                onClick={start}
                disabled={me.isLoading}
                className="w-full"
              >
                {t.startFreeTrial}
              </Button>
            </div>

            {/* Secondary — Sign in / register */}
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>{t.signInPrompt}</span>
              <Link
                href={`/${lang}/login`}
                className="font-semibold text-primary underline-offset-4 hover:underline"
              >
                {t.signInCta}
              </Link>
            </div>
          </div>
        )}
        {/* Back-home only makes sense for anonymous visitors who came from
            the marketing landing page. Signed-in users navigate via the
            sidebar / mobile tab bar that PracticeShell renders around us. */}
        {!isLoggedIn && <BackLink t={t} lang={lang} />}
      </Container>
    );
  }

  if (phase.kind === "starting") {
    return (
      <Container>
        <p className="text-muted-foreground">{t.starting}</p>
      </Container>
    );
  }

  if (phase.kind === "error") {
    return (
      <Container>
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {phase.message}
        </div>
        <Button variant="outline" onClick={() => setPhase({ kind: "idle" })}>
          {t.backHome}
        </Button>
      </Container>
    );
  }

  if (phase.kind === "completed") {
    const isExited = phase.reason === "exited";
    return (
      <Container>
        <div className="rounded-xl border bg-card p-8 text-center shadow-sm">
          <CheckCircle2 className="mx-auto mb-3 size-12 text-primary" />
          <h2 className="text-2xl font-semibold">
            {isExited ? t.exitedTitle : t.completedTitle}
          </h2>
          <p className="mt-2 text-muted-foreground">
            {isExited ? t.exitedBody : t.completedBody}
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Button onClick={() => setHistoryOpen(true)} size="lg">
              <History className="size-4" />
              {t.reviewHistoryFromCompleted}
            </Button>
            <Link
              href={isLoggedIn ? `/${lang}/dashboard` : `/${lang}`}
              className="inline-flex items-center justify-center rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-muted"
            >
              {isLoggedIn ? t.backToDashboard : t.backHome}
            </Link>
          </div>
        </div>
      </Container>
    );
  }

  // answering / feedback share most layout
  const question = phase.question;
  const totalCount = phase.totalCount;
  const answeredCount =
    phase.kind === "feedback"
      ? phase.result.progress.answered_count
      : phase.answeredCount;

  const isFeedback = phase.kind === "feedback";
  const correctKey = isFeedback ? phase.result.correct_choice_key : null;
  const pickedKey =
    phase.kind === "answering" ? phase.picked : phase.kind === "feedback" ? phase.picked : null;

  return (
    <Container>
      <div className="flex items-end justify-between gap-3">
        <div className="flex-1">
          <ProgressBar
            t={t}
            answered={answeredCount}
            total={totalCount}
          />
        </div>
        {answeredCount > 0 && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setHistoryOpen(true)}
            className="text-muted-foreground hover:text-foreground"
          >
            <History className="size-4" />
            {t.reviewHistory}
          </Button>
        )}
      </div>

      <div className="rounded-xl border border-border/70 bg-card px-5 py-6 shadow-sm md:px-8 md:py-8">
        <div className="mx-auto max-w-xl text-center">
          <p className="text-xs font-semibold uppercase tracking-wider text-primary">
            {t.questionOf
              .replace("{current}", String(Math.min(answeredCount + 1, totalCount)))
              .replace("{total}", String(totalCount))}
          </p>
          <h2 className="mt-2 text-xl font-semibold leading-8 text-foreground sm:text-2xl sm:leading-9">
            {question.stem}
          </h2>
        </div>

        <ul className="mx-auto mt-7 flex max-w-xl flex-col gap-3">
          {question.choices.map((c) => {
            const selected = pickedKey === c.key;
            const isCorrect = isFeedback && correctKey === c.key;
            const isWrongPick = isFeedback && selected && correctKey !== c.key;

            const stateClass = isFeedback
              ? isCorrect
                ? "border-success bg-success/10 text-foreground shadow-sm"
                : isWrongPick
                  ? "border-destructive bg-destructive/10 text-foreground shadow-sm"
                  : "border-border bg-background text-muted-foreground"
              : selected
                ? "border-primary bg-primary/10 text-foreground ring-1 ring-primary/20"
                : "border-border bg-background hover:border-primary/60 hover:bg-primary/5 hover:shadow-sm";

            return (
              <li key={c.key}>
                <button
                  type="button"
                  disabled={isFeedback || phase.kind === "answering" && phase.submitting}
                  onClick={() => {
                    if (phase.kind !== "answering" || phase.submitting) return;
                    setPhase({ ...phase, picked: c.key });
                  }}
                  className={`flex w-full items-start gap-3 rounded-xl border-2 px-4 py-4 text-left transition-all disabled:cursor-default ${stateClass}`}
                >
                  <span className="mt-0.5 inline-flex size-8 shrink-0 items-center justify-center rounded-full border border-border bg-background text-sm font-bold text-foreground">
                    {c.key}
                  </span>
                  <span className="flex-1 pt-1 text-sm leading-6 sm:text-base">
                    {c.text}
                  </span>
                  {isFeedback && isCorrect && (
                    <CheckCircle2 className="mt-1 size-5 shrink-0 text-success" />
                  )}
                  {isFeedback && isWrongPick && (
                    <XCircle className="mt-1 size-5 shrink-0 text-destructive" />
                  )}
                </button>
              </li>
            );
          })}
        </ul>

        {isFeedback && (
          <div
            className={`mx-auto mt-6 max-w-xl rounded-xl border p-4 text-sm shadow-sm ${
              phase.result.is_correct
                ? "border-success/40 bg-success/5 text-foreground"
                : "border-destructive/40 bg-destructive/5 text-foreground"
            }`}
          >
            <p className="text-base font-semibold">
              {phase.result.is_correct ? t.correct : t.incorrect}
            </p>
            {phase.result.explanation && (
              <p className="mt-2 leading-relaxed text-muted-foreground">
                <span className="font-medium text-foreground">{t.explanation}: </span>
                {phase.result.explanation}
              </p>
            )}

            {/* AI deep-dive — only offered on wrong answers (the static
                explanation is enough when the user got it right). Anonymous
                visitors see a disabled button with a sign-in nudge; signed-in
                users get the DeepSeek response inline. */}
            {!phase.result.is_correct && (
              <AiExplainBlock
                t={t}
                isLoggedIn={isLoggedIn}
                ai={ai}
                onAsk={() =>
                  ai.explain({
                    question_id: phase.question.question_id,
                    variant_id: phase.question.variant_id,
                    selected_choice_key: phase.picked,
                    language: lang,
                  })
                }
              />
            )}
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-3">
        <Button variant="ghost" onClick={() => setExitConfirmOpen(true)}>
          {t.exit}
        </Button>
        {phase.kind === "answering" && (
          <Button
            onClick={submit}
            disabled={!phase.picked || phase.submitting}
            size="lg"
          >
            {phase.submitting ? t.submitting : t.submitAnswer}
          </Button>
        )}
        {phase.kind === "feedback" && (
          <Button onClick={next} size="lg">
            {t.nextQuestion}
          </Button>
        )}
      </div>

      {exitConfirmOpen && (
        <ExitConfirmDialog
          t={t}
          onCancel={() => setExitConfirmOpen(false)}
          onConfirm={confirmExit}
        />
      )}
    </Container>
  );
}

function ExitConfirmDialog({
  t,
  onCancel,
  onConfirm,
}: {
  t: Dictionary["practice"];
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="exit-confirm-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h3
          id="exit-confirm-title"
          className="text-lg font-semibold text-foreground"
        >
          {t.exitConfirmTitle}
        </h3>
        <p className="mt-2 text-sm text-muted-foreground">
          {t.exitConfirmBody}
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel}>
            {t.exitConfirmCancel}
          </Button>
          <Button variant="destructive" onClick={onConfirm}>
            {t.exitConfirmYes}
          </Button>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Subcomponents
// ---------------------------------------------------------------------------

function Container({ children }: { children: React.ReactNode }) {
  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-4 py-8 sm:py-12">
      {children}
    </main>
  );
}

function Header({
  t,
  subtitle,
}: {
  t: Dictionary["practice"];
  subtitle?: string;
}) {
  return (
    <header className="text-center">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
        {t.title}
      </h1>
      <p className="mt-2 text-muted-foreground">{subtitle ?? t.subtitle}</p>
    </header>
  );
}

function ProgressBar({
  t,
  answered,
  total,
}: {
  t: Dictionary["practice"];
  answered: number;
  total: number;
}) {
  const pct = total > 0 ? Math.min(100, Math.round((answered / total) * 100)) : 0;
  return (
    <div>
      <div className="mb-2 flex items-baseline justify-between text-sm">
        <span className="text-muted-foreground">{t.answered}</span>
        <span className="font-medium tabular-nums">
          {answered} / {total}
        </span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div
          className="h-full rounded-full bg-primary transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function AiExplainBlock({
  t,
  isLoggedIn,
  ai,
  onAsk,
}: {
  t: Dictionary["practice"];
  isLoggedIn: boolean;
  ai: ReturnType<typeof useAiExplain>;
  onAsk: () => void;
}) {
  if (ai.state.kind === "ok") {
    return (
      <div className="mt-4 border-t border-border/60 pt-4">
        <p className="mb-1 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-primary">
          <Sparkles className="size-3.5" />
          {t.aiExplainHeading}
          {ai.state.cached && (
            <span className="font-normal text-muted-foreground normal-case tracking-normal">
              {t.aiExplainCached}
            </span>
          )}
        </p>
        <p className="leading-relaxed text-foreground">{ai.state.text}</p>
      </div>
    );
  }

  if (ai.state.kind === "error") {
    // Rate-limit (429 RATE_LIMITED) gets its own friendlier copy; everything
    // else falls through to the generic message.
    const msg =
      ai.state.code === "RATE_LIMITED" ? t.aiExplainCooldown : t.aiExplainError;
    return (
      <div className="mt-4 border-t border-border/60 pt-4">
        <p className="text-xs text-destructive">{msg}</p>
      </div>
    );
  }

  return (
    <div className="mt-4 border-t border-border/60 pt-4">
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={onAsk}
        disabled={!isLoggedIn || ai.state.kind === "loading"}
        className="gap-1.5"
      >
        <Sparkles className="size-4" />
        {ai.state.kind === "loading" ? t.aiExplainLoading : t.aiExplainButton}
      </Button>
      {!isLoggedIn && (
        <p className="mt-2 text-xs text-muted-foreground">
          {t.aiExplainAuthRequired}
        </p>
      )}
    </div>
  );
}

function BackLink({ t, lang }: { t: Dictionary["practice"]; lang: Locale }) {
  return (
    <div className="text-center">
      <Link
        href={`/${lang}`}
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground hover:underline"
      >
        <ArrowLeft className="size-4" />
        {t.backHome}
      </Link>
    </div>
  );
}

function errorMessage(err: unknown, t: Dictionary["practice"]): string {
  if (err instanceof ApiError) {
    if (err.code === "UNAUTHORIZED") return t.errorAuthRequired;
    if (err.code === "ACCESS_DENIED") return t.errorPassRequired;
    return err.message;
  }
  return t.errorGeneric;
}
