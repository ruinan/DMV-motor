"use client";

import { CheckCircle2 } from "lucide-react";
import type { Dictionary, Locale } from "@/lib/dictionaries";
import type { TopicMastery } from "@/lib/hooks/use-topic-mastery";

/**
 * Per-topic mastery progress (bug2). Makes the "stuck on a topic" state legible:
 * for every topic the user has touched, a bar shows how close they are to the
 * mastery gate — the same gate that auto-clears that topic's accumulated
 * mistakes in the practice flow. Not-yet-mastered topics surface first (closest
 * to the line on top), mastered ones drop to the bottom with a check.
 */
export function TopicProgressSection({
  t,
  lang,
  topics,
}: {
  t: Dictionary["studyHub"];
  lang: Locale;
  topics: TopicMastery[];
}) {
  const touched = topics.filter((x) => x.mastery_progress.attempted > 0);
  if (touched.length === 0) return null;

  const sorted = [...touched].sort((a, b) => {
    if (a.is_mastered !== b.is_mastered) return a.is_mastered ? 1 : -1;
    return b.mastery_progress.progress_percent - a.mastery_progress.progress_percent;
  });

  return (
    <section className="rounded-xl border border-border/40 bg-card p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-foreground">{t.topicProgressTitle}</h2>
      <p className="mt-1 text-sm text-muted-foreground">{t.topicProgressSubtitle}</p>
      <ul className="mt-5 flex flex-col gap-5">
        {sorted.map((topic) => {
          const p = topic.mastery_progress;
          const name = lang === "zh" ? topic.name_zh : topic.name_en;
          const detail = topic.is_mastered
            ? t.topicProgressMastered
            : t.topicProgressDetail
                .replace("{acc}", String(p.accuracy_percent))
                .replace("{accT}", String(p.accuracy_threshold))
                .replace("{recent}", String(p.recent_correct))
                .replace("{recentT}", String(p.recent_correct_threshold));
          return (
            <li key={topic.topic_id}>
              <div className="mb-1.5 flex items-center justify-between gap-3">
                <span className="flex items-center gap-1.5 text-sm font-medium text-foreground">
                  {topic.is_mastered && (
                    <CheckCircle2 className="size-4 shrink-0 text-success" aria-hidden />
                  )}
                  {name}
                </span>
                <span className="shrink-0 text-xs font-semibold tabular-nums text-muted-foreground">
                  {p.progress_percent}%
                </span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    topic.is_mastered ? "bg-success" : "bg-primary"
                  }`}
                  style={{ width: `${p.progress_percent}%` }}
                />
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
