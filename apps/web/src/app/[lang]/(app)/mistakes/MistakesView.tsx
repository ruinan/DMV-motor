"use client";

import { useState } from "react";
import { Bookmark, ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useMistakes, type MistakeItem } from "@/lib/hooks/use-mistakes";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
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

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.pageSize)) : 1;

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
    <li className="flex items-center gap-4 rounded-xl border bg-card p-4 shadow-sm">
      <div className="flex-1 min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
            {topicName}
          </span>
          <span className="rounded-full border border-border bg-background px-2.5 py-0.5 text-xs text-muted-foreground">
            {sourceLabel}
          </span>
        </div>
        <p className="mt-2 truncate text-sm text-foreground">
          #{item.question_id}
        </p>
        <p className="mt-0.5 text-xs text-muted-foreground">
          {wrongLabel} · {t.lastWrong}: {lastWrong}
        </p>
      </div>
      <ArrowRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
    </li>
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
