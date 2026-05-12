"use client";

import Link from "next/link";
import {
  ArrowRight,
  CheckCircle2,
  RefreshCw,
  Sparkles,
  Target,
} from "lucide-react";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
import {
  useReviewPack,
  type ReviewTaskSummary,
} from "@/lib/hooks/use-review-pack";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function Dashboard({ t, lang }: Props) {
  const topicMap = useTopicNameMap(lang);

  const { data, isLoading, error, noPass } = useReviewPack();

  if (noPass) return <NoPassFallback t={t} lang={lang} />;

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Header t={t} />
        <p className="text-sm text-muted-foreground">{t.dashboard.loading}</p>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex flex-col gap-6">
        <Header t={t} />
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {t.dashboard.errorGeneric}
        </div>
      </div>
    );
  }

  const activeTasks = data.tasks.filter((task) => task.status !== "completed");
  const completedTasks = data.tasks.filter((task) => task.status === "completed");
  const pct =
    data.target_question_count > 0
      ? Math.round(
          (data.completed_question_count / data.target_question_count) * 100,
        )
      : 0;

  return (
    <div className="flex flex-col gap-8">
      <Header t={t} />

      {/* Progress bar */}
      <section>
        <div className="mb-2 flex items-center justify-between text-sm">
          <span className="font-semibold text-primary">
            {t.dashboard.questionsDone
              .replace("{done}", String(data.completed_question_count))
              .replace("{total}", String(data.target_question_count))}
          </span>
          <span className="text-muted-foreground tabular-nums">{pct}%</span>
        </div>
        <div className="h-3 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-primary transition-all duration-700 ease-out"
            style={{ width: `${pct}%` }}
          />
        </div>
      </section>

      {/* Active tasks grid */}
      {activeTasks.length === 0 ? (
        <section className="rounded-xl border border-dashed border-border bg-card p-10 text-center">
          <p className="text-sm text-muted-foreground">
            {t.dashboard.noActiveTasks}
          </p>
        </section>
      ) : (
        <section className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {activeTasks.map((task) => (
            <ActiveTaskCard
              key={task.review_task_id}
              t={t}
              lang={lang}
              task={task}
              topicName={
                topicMap.get(task.topic_id) ?? `Topic ${task.topic_id}`
              }
            />
          ))}
        </section>
      )}

      {/* Completed today */}
      {completedTasks.length > 0 && (
        <section>
          <h2 className="mb-4 text-xl font-semibold text-foreground">
            {t.dashboard.completedTodayTitle}
          </h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {completedTasks.map((task) => (
              <CompletedTaskCard
                key={task.review_task_id}
                t={t}
                lang={lang}
                task={task}
                topicName={
                  topicMap.get(task.topic_id) ?? `Topic ${task.topic_id}`
                }
              />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function Header({ t }: { t: Dictionary }) {
  return (
    <header>
      <h1 className="text-3xl font-bold tracking-tight text-foreground">
        {t.dashboard.packTitle}
      </h1>
      <p className="mt-1 text-base text-muted-foreground">
        {t.dashboard.packSubtitle}
      </p>
    </header>
  );
}

function ActiveTaskCard({
  t,
  lang,
  task,
  topicName,
}: {
  t: Dictionary;
  lang: Locale;
  task: ReviewTaskSummary;
  topicName: string;
}) {
  const isInProgress = task.status === "in_progress";
  const ctaLabel = isInProgress ? t.dashboard.ctaResume : t.dashboard.ctaStart;
  const CtaIcon = isInProgress ? RefreshCw : ArrowRight;

  const typeLabel =
    task.type === "key_topic"
      ? t.dashboard.weakSpotDrill
      : task.type === "persistent"
        ? t.dashboard.sameTopicRetry
        : t.dashboard.mixedPractice;

  const TypeIcon = task.type === "persistent" ? RefreshCw : Target;

  const priorityLabel =
    task.priority === "high"
      ? t.dashboard.highPriority
      : task.priority === "low"
        ? t.dashboard.lowPriority
        : t.dashboard.mediumPriority;

  const priorityClass =
    task.priority === "high"
      ? "bg-destructive/10 text-destructive"
      : task.priority === "low"
        ? "bg-muted text-muted-foreground"
        : "bg-warning/15 text-warning-foreground";

  return (
    <article className="flex flex-col rounded-xl border border-border/40 bg-card p-6 shadow-sm transition-shadow hover:shadow-md">
      {/* Header row: topic + type label / priority badge */}
      <div className="mb-5 flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1.5">
          <span className="inline-flex w-fit items-center rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary">
            {topicName}
          </span>
          <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
            <TypeIcon className="size-3.5" />
            {typeLabel}
          </span>
        </div>
        <span
          className={`shrink-0 rounded-full px-3 py-1 text-xs font-bold ${priorityClass}`}
        >
          {priorityLabel}
        </span>
      </div>

      {/* Footer: task progress + CTA */}
      <div className="mt-auto border-t border-border pt-4">
        <div className="mb-3 flex items-center justify-between text-sm">
          <span className="text-muted-foreground">{t.review.taskProgress}</span>
          <span className="font-semibold text-foreground tabular-nums">
            {task.completed_question_count}/{task.target_question_count}
          </span>
        </div>
        <Link
          href={`/${lang}/review/${task.review_task_id}`}
          className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-primary text-base font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md active:scale-[0.98]"
        >
          {ctaLabel}
          <CtaIcon className="size-4" />
        </Link>
      </div>
    </article>
  );
}

function CompletedTaskCard({
  t,
  lang,
  task,
  topicName,
}: {
  t: Dictionary;
  lang: Locale;
  task: ReviewTaskSummary;
  topicName: string;
}) {
  return (
    <article className="flex items-center justify-between gap-4 rounded-xl border border-border/30 bg-muted/30 p-5">
      <div className="flex min-w-0 items-center gap-3">
        <CheckCircle2 className="size-5 shrink-0 text-success" />
        <div className="min-w-0">
          <p className="truncate text-base font-semibold text-foreground">
            {topicName}
          </p>
          <p className="text-sm text-muted-foreground tabular-nums">
            {task.completed_question_count}/{task.target_question_count}{" "}
            {t.review.statusCompleted.toLowerCase()}
          </p>
        </div>
      </div>
      <Link
        href={`/${lang}/review/${task.review_task_id}`}
        className="inline-flex h-11 shrink-0 items-center justify-center rounded-xl border border-border bg-card px-4 text-sm font-medium text-foreground transition-colors hover:bg-background"
      >
        {t.review.reviewTask}
      </Link>
    </article>
  );
}

function NoPassFallback({ t, lang }: { t: Dictionary; lang: Locale }) {
  return (
    <div className="flex flex-col gap-8">
      <Header t={t} />
      <section className="flex flex-col items-center rounded-xl border border-border/40 bg-card p-10 text-center shadow-sm">
        <Sparkles className="mb-4 size-10 text-primary" />
        <h2 className="text-2xl font-semibold text-foreground">
          {t.dashboard.noPassTitle}
        </h2>
        <p className="mt-2 max-w-md text-sm text-muted-foreground">
          {t.dashboard.noPassBody}
        </p>
        <Link
          href={`/${lang}/practice`}
          className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-primary px-6 text-base font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md"
        >
          {t.dashboard.noPassCta}
          <ArrowRight className="size-4" />
        </Link>
      </section>
    </div>
  );
}
