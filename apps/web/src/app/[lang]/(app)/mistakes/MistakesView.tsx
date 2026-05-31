"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  Bookmark,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Target,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { AiExplainBlock } from "@/components/ai-explain-block";
import { useMistakes, type MistakeItem } from "@/lib/hooks/use-mistakes";
import { useMe } from "@/lib/hooks/use-me";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
import { useMistakeReview } from "@/lib/hooks/use-mistake-review";
import { useAuth } from "@/lib/auth-context";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["mistakes"];
  lang: Locale;
};

const PAGE_SIZE = 20;

export function MistakesView({ t, lang }: Props) {
  const [page, setPage] = useState(1);
  const { data, isLoading, error } = useMistakes(page, PAGE_SIZE);
  const topicMap = useTopicNameMap(lang);
  const me = useMe();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState<string | null>(null);

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.pageSize)) : 1;

  // Distinct topic ids across the loaded mistakes, capped at 8 (server caps
  // too). Drives the "Practice these" CTA — starts a topic-scoped practice
  // session, then /practice auto-resumes into it.
  const mistakeTopicIds = data
    ? Array.from(new Set(data.items.map((m) => Number(m.topic_id)))).slice(0, 8)
    : [];

  async function practiceThese() {
    if (starting || mistakeTopicIds.length === 0) return;
    setStarting(true);
    setStartError(null);
    const entryType = me.data?.access.has_active_pass ? "full" : "free_trial";
    try {
      await apiFetch("/api/v1/practice/sessions", {
        method: "POST",
        body: JSON.stringify({
          entry_type: entryType,
          language: lang,
          topic_filter: mistakeTopicIds,
        }),
      });
      // PracticeFlow auto-resumes the most-recent in-progress session on mount,
      // so just refresh /me and navigate.
      queryClient.invalidateQueries({ queryKey: ["me"] });
      router.push(`/${lang}/practice`);
    } catch (err) {
      setStartError(err instanceof ApiError ? err.message : t.errorGeneric);
      setStarting(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.title}
        </h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          {t.subtitle}
        </p>
      </header>

      {data && data.items.length > 0 && (
        <div className="flex flex-col gap-2 rounded-xl border border-primary/30 bg-primary/5 p-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">
            {t.practiceTheseHint.replace("{n}", String(mistakeTopicIds.length))}
          </p>
          <Button onClick={practiceThese} disabled={starting} className="gap-1.5">
            <Target className="size-4" />
            {starting ? t.practiceTheseStarting : t.practiceThese}
          </Button>
        </div>
      )}
      {startError && (
        <p className="text-sm text-destructive">{startError}</p>
      )}

      {isLoading && (
        <p className="text-sm text-muted-foreground">{t.loading}</p>
      )}

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {t.errorGeneric}
        </div>
      )}

      {data && data.items.length === 0 && (
        <div className="rounded-xl border border-dashed bg-card p-10 text-center">
          <Bookmark className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">{t.empty}</p>
        </div>
      )}

      {data && data.items.length > 0 && (
        <>
          <ul className="flex flex-col gap-3">
            {data.items.map((m) => (
              <MistakeRow
                key={m.mistake_id}
                t={t}
                lang={lang}
                item={m}
                topicName={topicMap.get(m.topic_id) ?? `Topic ${m.topic_id}`}
              />
            ))}
          </ul>

          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t pt-4">
              <Button
                variant="outline"
                size="sm"
                disabled={page <= 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
              >
                <ChevronLeft className="size-4" /> {t.previous}
              </Button>
              <span className="text-sm tabular-nums text-muted-foreground">
                {t.page} {page} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                {t.next} <ChevronRight className="size-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function MistakeRow({
  t,
  lang,
  item,
  topicName,
}: {
  t: Dictionary["mistakes"];
  lang: Locale;
  item: MistakeItem;
  topicName: string;
}) {
  const [expanded, setExpanded] = useState(false);

  const wrongLabel =
    item.wrong_count === 1
      ? t.wrongCountOne
      : t.wrongCount.replace("{count}", String(item.wrong_count));

  const lastWrong = formatDate(item.last_wrong_at, lang);

  const sourceLabel =
    item.source === "review"
      ? t.fromReview
      : item.source === "mock"
        ? t.fromMock
        : t.fromPractice;

  return (
    <li className="overflow-hidden rounded-xl border bg-card shadow-sm">
      <button
        type="button"
        onClick={() => setExpanded((e) => !e)}
        aria-expanded={expanded}
        className="flex w-full items-center gap-4 p-4 text-left transition-colors hover:bg-muted/40"
      >
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
              {topicName}
            </span>
            <span className="rounded-full border border-border bg-background px-2.5 py-0.5 text-xs text-muted-foreground">
              {sourceLabel}
            </span>
          </div>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {wrongLabel} · {t.lastWrong}: {lastWrong}
          </p>
        </div>
        <ChevronDown
          className={`size-4 shrink-0 text-muted-foreground transition-transform ${
            expanded ? "rotate-180" : ""
          }`}
          aria-hidden
        />
      </button>
      {expanded && (
        <MistakeReviewPanel t={t} lang={lang} questionId={item.question_id} />
      )}
    </li>
  );
}

// Lazily-loaded review for one mistake: the question, the correct answer
// highlighted, the explanation, and an AI deep-dive button. Mirrors the
// practice attempt review. The AI payload is locked to structured fields
// (no free text); the backend enforces caps + cooldown.
function MistakeReviewPanel({
  t,
  lang,
  questionId,
}: {
  t: Dictionary["mistakes"];
  lang: Locale;
  questionId: string;
}) {
  const { data, isLoading, error } = useMistakeReview(questionId, lang, true);
  const { user } = useAuth();
  const isLoggedIn = !!user;

  if (isLoading) {
    return (
      <div className="border-t px-4 py-3 text-sm text-muted-foreground">
        {t.loading}
      </div>
    );
  }
  if (error || !data) {
    return (
      <div className="border-t px-4 py-3 text-sm text-destructive">
        {t.errorGeneric}
      </div>
    );
  }

  return (
    <div className="border-t px-4 py-4">
      <p className="text-base leading-relaxed text-foreground">{data.stem}</p>

      <ul className="mt-4 flex flex-col gap-2">
        {data.choices.map((c) => {
          const isCorrect = c.key === data.correct_choice_key;
          const tone = isCorrect
            ? "border-primary bg-primary/10 text-foreground"
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
            </li>
          );
        })}
      </ul>

      {data.explanation && (
        <p className="mt-3 rounded-md bg-muted/50 p-3 text-sm leading-relaxed text-muted-foreground">
          <span className="font-medium text-foreground">{t.explanation}:</span>{" "}
          {data.explanation}
        </p>
      )}

      <AiExplainBlock
        questionId={data.question_id}
        variantId={data.variant_id}
        language={lang}
        t={t}
        isLoggedIn={isLoggedIn}
      />
    </div>
  );
}

function formatDate(iso: string, lang: Locale): string {
  try {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleDateString(lang === "zh" ? "zh-CN" : "en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  } catch {
    return iso;
  }
}
