"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, ChevronLeft, ChevronRight, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Choice = { key: string; text: string };

type Question = {
  question_id: string;
  variant_id: string;
  stem: string;
  choices: Choice[];
};

type StoredStart = {
  questions: Question[];
  language: string;
  remaining_after_start: number;
};

type SubmitResponse = {
  mock_attempt_id: string;
  status: string;
  score_percent: number;
  correct_count: number;
  wrong_count: number;
  weak_topics: { topic_id: string; label: string }[];
  next_action: { type: string; label: string };
};

type SaveStatus = "idle" | "saving" | "saved" | "error";

const STORAGE_PREFIX = "dmv:mock-attempt:";
const PASS_THRESHOLD = 85;

// useSyncExternalStore plumbing — sessionStorage is read once on mount and
// never mutated externally during the exam, so subscribe is a no-op.
const subscribeNoop = () => () => {};
const getServerSnapshot = (): string | null => null;
const readStoredRaw =
  (attemptId: string) => (): string | null => {
    if (typeof window === "undefined") return null;
    return window.sessionStorage.getItem(STORAGE_PREFIX + attemptId);
  };

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

  const raw = useSyncExternalStore(
    subscribeNoop,
    useMemo(() => readStoredRaw(attemptId), [attemptId]),
    getServerSnapshot,
  );
  const stored = useMemo<StoredStart | null>(() => {
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredStart;
    } catch {
      return null;
    }
  }, [raw]);

  const [index, setIndex] = useState(0);
  const [picks, setPicks] = useState<Map<string, string>>(new Map());
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const [submitting, setSubmitting] = useState(false);
  const [exiting, setExiting] = useState(false);
  const [result, setResult] = useState<SubmitResponse | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const savedTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (savedTimer.current) clearTimeout(savedTimer.current);
    };
  }, []);

  if (!user) return null;

  if (!stored && !result) {
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
        onTryAgain={() => router.push(`/${lang}/mock`)}
        onBack={() => router.push(`/${lang}/dashboard`)}
      />
    );
  }

  const questions = stored!.questions;
  const total = questions.length;
  const question = questions[index];
  const pickedKey = picks.get(question.question_id) ?? null;
  const answeredCount = picks.size;
  const isLast = index + 1 >= total;

  async function pick(choiceKey: string) {
    if (saveStatus === "saving") return;
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
      await apiFetch(`/api/v1/mock-exams/attempts/${attemptId}/answers`, {
        method: "POST",
        body: JSON.stringify({
          question_id: question.question_id,
          variant_id: question.variant_id,
          selected_choice_key: choiceKey,
        }),
      });
      setSaveStatus("saved");
      savedTimer.current = setTimeout(() => setSaveStatus("idle"), 1500);
    } catch (err) {
      setSaveStatus("error");
      setErrorMsg(
        err instanceof ApiError ? err.message : t.errorGeneric,
      );
    }
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
      window.sessionStorage.removeItem(STORAGE_PREFIX + attemptId);
      // Mock outcome shifts mistakes / readiness — invalidate so other pages refetch
      queryClient.invalidateQueries({ queryKey: ["mock-access"] });
      queryClient.invalidateQueries({ queryKey: ["mistakes"] });
      queryClient.invalidateQueries({ queryKey: ["mistakes-count"] });
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      setResult(res);
    } catch (err) {
      setErrorMsg(err instanceof ApiError ? err.message : t.errorGeneric);
      setSubmitting(false);
    }
  }

  async function onExit() {
    if (exiting) return;
    if (!window.confirm(t.exitConfirm)) return;
    setExiting(true);
    setErrorMsg(null);
    try {
      await apiFetch(`/api/v1/mock-exams/attempts/${attemptId}/exit`, {
        method: "POST",
      });
      window.sessionStorage.removeItem(STORAGE_PREFIX + attemptId);
      queryClient.invalidateQueries({ queryKey: ["mock-access"] });
      router.push(`/${lang}/mock`);
    } catch (err) {
      setErrorMsg(err instanceof ApiError ? err.message : t.errorGeneric);
      setExiting(false);
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
            const cls = selected
              ? "border-primary bg-primary/10 text-foreground"
              : "border-border bg-background hover:border-primary/50 hover:bg-muted";
            return (
              <li key={c.key}>
                <button
                  type="button"
                  disabled={submitting || exiting}
                  onClick={() => pick(c.key)}
                  className={`flex w-full items-start gap-3 rounded-lg border-2 px-4 py-3 text-left transition-colors disabled:cursor-default disabled:opacity-60 ${cls}`}
                >
                  <span className="mt-0.5 inline-flex size-7 shrink-0 items-center justify-center rounded-full border border-border bg-background text-sm font-semibold">
                    {c.key}
                  </span>
                  <span className="flex-1 text-sm sm:text-base">{c.text}</span>
                </button>
              </li>
            );
          })}
        </ul>

        {errorMsg && (
          <p className="mt-4 text-sm text-destructive">{errorMsg}</p>
        )}
      </div>

      {/* Nav row */}
      <div className="flex items-center justify-between gap-3">
        <Button
          variant="outline"
          onClick={() => setIndex((i) => Math.max(0, i - 1))}
          disabled={index === 0 || submitting || exiting}
        >
          <ChevronLeft className="size-4" />
          {t.previous}
        </Button>

        {!isLast ? (
          <Button
            onClick={() => setIndex((i) => Math.min(total - 1, i + 1))}
            disabled={submitting || exiting}
          >
            {t.next}
            <ChevronRight className="size-4" />
          </Button>
        ) : (
          <Button
            onClick={onSubmit}
            disabled={submitting || exiting}
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
          onClick={onExit}
          disabled={submitting || exiting}
          className="text-sm text-muted-foreground hover:text-destructive hover:underline disabled:opacity-60"
        >
          {exiting ? t.exiting : t.exit}
        </button>
      </div>
    </div>
  );
}

function ResultView({
  t,
  lang,
  result,
  topicMap,
  onTryAgain,
  onBack,
}: {
  t: Dictionary["mock"];
  lang: Locale;
  result: SubmitResponse;
  topicMap: Map<string, string>;
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

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onBack}>
          {t.backToDashboard}
        </Button>
        <Button onClick={onTryAgain}>{t.tryAgain}</Button>
      </div>
    </div>
  );
}
