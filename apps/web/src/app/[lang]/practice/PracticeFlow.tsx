"use client";

import Link from "next/link";
import { useState } from "react";
import { CheckCircle2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useMe } from "@/lib/hooks/use-me";
import type { Dictionary, Locale } from "@/lib/dictionaries";

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
  | { kind: "completed" }
  | { kind: "error"; message: string };

type Props = {
  t: Dictionary["practice"];
  lang: Locale;
};

export function PracticeFlow({ t, lang }: Props) {
  const me = useMe();
  const [phase, setPhase] = useState<Phase>({ kind: "idle" });

  const isLoggedIn = !!me.data;
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
    } catch (err) {
      setPhase({ kind: "error", message: errorMessage(err, t) });
    }
  }

  async function next() {
    if (phase.kind !== "feedback") return;
    const sessionId = phase.sessionId;
    const totalCount = phase.totalCount;
    const answeredCount = phase.result.progress.answered_count;
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
        await complete(sessionId);
        return;
      }
      setPhase({ kind: "error", message: errorMessage(err, t) });
    }
  }

  async function complete(sessionId: string) {
    try {
      await apiFetch(`/api/v1/practice/sessions/${sessionId}/complete`, {
        method: "POST",
      });
    } catch {
      // Even if complete fails, the user has effectively finished the pool.
    }
    setPhase({ kind: "completed" });
  }

  async function finish() {
    if (phase.kind !== "answering" && phase.kind !== "feedback") return;
    await complete(phase.sessionId);
  }

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------

  if (phase.kind === "idle") {
    return (
      <Container>
        <Header t={t} />
        <div className="flex flex-col items-center gap-3">
          <Button size="lg" onClick={start} disabled={me.isLoading}>
            {entryType === "full" ? t.startFull : t.startFreeTrial}
          </Button>
          <p className="text-xs text-muted-foreground">
            {isLoggedIn
              ? hasPass
                ? `${t.startFull}`
                : t.errorPassRequired
              : t.errorAuthRequired}
          </p>
        </div>
        <BackLink t={t} lang={lang} />
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
    return (
      <Container>
        <div className="rounded-xl border bg-card p-8 text-center shadow-sm">
          <CheckCircle2 className="mx-auto mb-3 size-12 text-primary" />
          <h2 className="text-2xl font-semibold">{t.completedTitle}</h2>
          <p className="mt-2 text-muted-foreground">{t.completedBody}</p>
          <div className="mt-6 flex justify-center gap-3">
            <Link
              href={isLoggedIn ? `/${lang}/dashboard` : `/${lang}`}
              className="rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-muted"
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
      <ProgressBar
        t={t}
        answered={answeredCount}
        total={totalCount}
      />

      <div className="rounded-xl border bg-card p-6 shadow-sm md:p-8">
        <p className="text-base leading-relaxed sm:text-lg">{question.stem}</p>

        <ul className="mt-6 flex flex-col gap-3">
          {question.choices.map((c) => {
            const selected = pickedKey === c.key;
            const isCorrect = isFeedback && correctKey === c.key;
            const isWrongPick = isFeedback && selected && correctKey !== c.key;

            const stateClass = isFeedback
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
                  disabled={isFeedback || phase.kind === "answering" && phase.submitting}
                  onClick={() => {
                    if (phase.kind !== "answering" || phase.submitting) return;
                    setPhase({ ...phase, picked: c.key });
                  }}
                  className={`flex w-full items-start gap-3 rounded-lg border-2 px-4 py-3 text-left transition-colors disabled:cursor-default ${stateClass}`}
                >
                  <span className="mt-0.5 inline-flex size-7 shrink-0 items-center justify-center rounded-full border border-border bg-background text-sm font-semibold">
                    {c.key}
                  </span>
                  <span className="flex-1 text-sm sm:text-base">{c.text}</span>
                  {isFeedback && isCorrect && (
                    <CheckCircle2 className="size-5 shrink-0 text-primary" />
                  )}
                  {isFeedback && isWrongPick && (
                    <XCircle className="size-5 shrink-0 text-destructive" />
                  )}
                </button>
              </li>
            );
          })}
        </ul>

        {isFeedback && (
          <div
            className={`mt-6 rounded-lg border p-4 text-sm ${
              phase.result.is_correct
                ? "border-primary/40 bg-primary/5 text-foreground"
                : "border-destructive/40 bg-destructive/5 text-foreground"
            }`}
          >
            <p className="font-semibold">
              {phase.result.is_correct ? t.correct : t.incorrect}
            </p>
            {phase.result.explanation && (
              <p className="mt-2 leading-relaxed text-muted-foreground">
                <span className="font-medium text-foreground">{t.explanation}: </span>
                {phase.result.explanation}
              </p>
            )}
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-3">
        <Button variant="ghost" onClick={finish}>
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
    </Container>
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

function Header({ t }: { t: Dictionary["practice"] }) {
  return (
    <header className="text-center">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
        {t.title}
      </h1>
      <p className="mt-2 text-muted-foreground">{t.subtitle}</p>
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

function BackLink({ t, lang }: { t: Dictionary["practice"]; lang: Locale }) {
  return (
    <div className="text-center">
      <Link
        href={`/${lang}`}
        className="text-sm text-muted-foreground hover:text-foreground hover:underline"
      >
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
