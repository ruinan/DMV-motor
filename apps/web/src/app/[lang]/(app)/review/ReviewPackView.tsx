"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, ListChecks, ChevronRight } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
import type { Dictionary, Locale } from "@/lib/dictionaries";

export type ReviewTaskSummary = {
  review_task_id: string;
  topic_id: string;
  type: "key_topic" | "persistent" | "mixed" | string;
  status: "pending" | "in_progress" | "completed" | string;
  priority: "high" | "medium" | "low" | string;
  target_question_count: number;
  completed_question_count: number;
};

export type ReviewPack = {
  review_pack_id: string;
  status: string;
  target_question_count: number;
  completed_question_count: number;
  tasks: ReviewTaskSummary[];
};

type Props = {
  t: Dictionary["review"];
  lang: Locale;
};

export function ReviewPackView({ t, lang }: Props) {
  const { user } = useAuth();
  const topicMap = useTopicNameMap(lang);

  const { data, isLoading, error } = useQuery({
    queryKey: ["review-pack"],
    queryFn: () => apiFetch<ReviewPack>("/api/v1/review/pack"),
    enabled: !!user,
    staleTime: 30_000,
  });

  const noPass =
    error instanceof ApiError && error.code === "ACCESS_DENIED";

  const packPct =
    data && data.target_question_count > 0
      ? Math.round(
          (data.completed_question_count / data.target_question_count) * 100,
        )
      : 0;

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

      {error && noPass && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {t.passRequired}
        </div>
      )}

      {error && !noPass && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {t.errorGeneric}
        </div>
      )}

      {data && (
        <>
          {/* Pack progress */}
          <section className="rounded-xl border bg-card p-6 shadow-sm">
            <div className="mb-3 flex items-baseline justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                {t.packProgress}
              </h2>
              <span className="text-sm tabular-nums">
                {data.completed_question_count} / {data.target_question_count}
              </span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-all duration-700"
                style={{ width: `${packPct}%` }}
              />
            </div>
          </section>

          {/* Task list */}
          {data.tasks.length === 0 ? (
            <div className="rounded-xl border border-dashed bg-card p-10 text-center">
              <ListChecks className="mx-auto mb-3 size-10 text-muted-foreground" />
              <p className="text-sm text-muted-foreground">{t.noTasks}</p>
            </div>
          ) : (
            <ul className="flex flex-col gap-3">
              {data.tasks.map((task) => (
                <TaskRow
                  key={task.review_task_id}
                  t={t}
                  lang={lang}
                  task={task}
                  topicName={
                    topicMap.get(task.topic_id) ?? `Topic ${task.topic_id}`
                  }
                />
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}

function TaskRow({
  t,
  lang,
  task,
  topicName,
}: {
  t: Dictionary["review"];
  lang: Locale;
  task: ReviewTaskSummary;
  topicName: string;
}) {
  const priorityLabel =
    task.priority === "high"
      ? t.priorityHigh
      : task.priority === "low"
        ? t.priorityLow
        : t.priorityMedium;

  const priorityClass =
    task.priority === "high"
      ? "bg-destructive/10 text-destructive"
      : task.priority === "low"
        ? "bg-muted text-muted-foreground"
        : "bg-amber-500/10 text-amber-700 dark:text-amber-400";

  const typeLabel =
    task.type === "key_topic"
      ? t.typeKeyTopic
      : task.type === "persistent"
        ? t.typePersistent
        : t.typeMixed;

  const statusLabel =
    task.status === "completed"
      ? t.statusCompleted
      : task.status === "in_progress"
        ? t.statusInProgress
        : t.statusPending;

  const cta =
    task.status === "completed"
      ? t.reviewTask
      : task.status === "in_progress"
        ? t.continueTask
        : t.startTask;

  const isDone = task.status === "completed";

  return (
    <li>
      <Link
        href={`/${lang}/review/${task.review_task_id}`}
        className="group flex items-center gap-4 rounded-xl border bg-card p-4 shadow-sm transition-colors hover:border-primary/40"
      >
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
              {topicName}
            </span>
            <span
              className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${priorityClass}`}
            >
              {t.priority}: {priorityLabel}
            </span>
            <span className="rounded-full border border-border bg-background px-2.5 py-0.5 text-xs text-muted-foreground">
              {typeLabel}
            </span>
            <span className="text-xs text-muted-foreground">{statusLabel}</span>
          </div>
          <p className="mt-2 text-xs text-muted-foreground">
            {t.taskProgress}: {task.completed_question_count} /{" "}
            {task.target_question_count}
          </p>
        </div>

        <div className="flex items-center gap-2 text-sm font-medium text-primary group-hover:underline">
          {isDone && <CheckCircle2 className="size-4 text-primary" />}
          <span>{cta}</span>
          <ChevronRight className="size-4" />
        </div>
      </Link>
    </li>
  );
}
