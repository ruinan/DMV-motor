"use client";

import Link from "next/link";
import {
  AlertCircle,
  ArrowRight,
  BookOpenCheck,
  ClipboardList,
  History,
  Lock,
  PlayCircle,
  RefreshCw,
  Sparkles,
  Target,
  Timer,
  type LucideIcon,
} from "lucide-react";
import { useMe } from "@/lib/hooks/use-me";
import { useMistakesCount } from "@/lib/hooks/use-mistakes-count";
import { useReadiness } from "@/lib/hooks/use-readiness";
import {
  useReviewPack,
  type ReviewPack,
  type ReviewTaskSummary,
} from "@/lib/hooks/use-review-pack";
import { useSummary, type SummaryResponse } from "@/lib/hooks/use-summary";
import { useTopicNameMap } from "@/lib/hooks/use-topics";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function Dashboard({ t, lang }: Props) {
  const topicMap = useTopicNameMap(lang);
  const me = useMe();
  const summary = useSummary();
  const mistakesCount = useMistakesCount();
  const isPaid = me.data?.access.has_active_pass ?? false;
  const readiness = useReadiness(isPaid);
  const reviewPack = useReviewPack();

  const readinessScore = isPaid
    ? (readiness.data?.readiness_score ?? summary.data?.readiness_score ?? null)
    : null;
  const isReady =
    readiness.data?.is_ready_candidate ??
    summary.data?.is_ready_candidate ??
    false;

  return (
    <div className="flex flex-col gap-6">
      <Header t={t} />

      <GlobalSignals
        t={t}
        completionScore={summary.data?.completion_score ?? null}
        completionLoading={summary.isLoading}
        completionError={!!summary.error}
        weakTopicCount={summary.data?.weak_topics.length ?? 0}
        mistakesCount={mistakesCount.data ?? 0}
        mistakesLoading={mistakesCount.isLoading}
        mistakesError={!!mistakesCount.error}
        accessLoading={me.isLoading}
        isPaid={isPaid}
        readinessScore={readinessScore}
        readinessLoading={isPaid && (readiness.isLoading || summary.isLoading)}
        readinessError={!!readiness.error}
        isReady={isReady}
        lang={lang}
      />

      <section className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <LatestPracticePanel t={t} lang={lang} me={me} />
        <NextActionPanel
          t={t}
          lang={lang}
          action={summary.data?.next_action ?? null}
          loading={summary.isLoading}
          error={!!summary.error}
        />
      </section>

      <ReviewPackPanel
        t={t}
        lang={lang}
        data={reviewPack.data}
        loading={reviewPack.isLoading}
        error={!!reviewPack.error}
        noPass={reviewPack.noPass}
        topicMap={topicMap}
      />

      <HistoryPanels t={t} />
    </div>
  );
}

function Header({ t }: { t: Dictionary }) {
  return (
    <header>
      <h1 className="text-3xl font-bold tracking-tight text-foreground">
        {t.dashboard.studyHubTitle}
      </h1>
      <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
        {t.dashboard.studyHubSubtitle}
      </p>
    </header>
  );
}

function GlobalSignals({
  t,
  completionScore,
  completionLoading,
  completionError,
  weakTopicCount,
  mistakesCount,
  mistakesLoading,
  mistakesError,
  accessLoading,
  isPaid,
  readinessScore,
  readinessLoading,
  readinessError,
  isReady,
  lang,
}: {
  t: Dictionary;
  completionScore: number | null;
  completionLoading: boolean;
  completionError: boolean;
  weakTopicCount: number;
  mistakesCount: number;
  mistakesLoading: boolean;
  mistakesError: boolean;
  accessLoading: boolean;
  isPaid: boolean;
  readinessScore: number | null;
  readinessLoading: boolean;
  readinessError: boolean;
  isReady: boolean;
  lang: Locale;
}) {
  return (
    <section className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <SignalCard
        icon={BookOpenCheck}
        title={t.dashboard.knowledgeCoverageTitle}
        value={formatPercent(completionScore, completionLoading)}
        hint={
          completionError
            ? t.dashboard.signalUnavailable
            : t.dashboard.knowledgeCoverageHint
        }
        progress={completionLoading || completionError ? null : completionScore}
        progressTone="primary"
        meta={[
          {
            label: t.dashboard.needsReview,
            value: completionLoading ? t.dashboard.loadingShort : weakTopicCount,
          },
          {
            label: t.dashboard.activeMistakes,
            value: mistakesLoading
              ? t.dashboard.loadingShort
              : mistakesError
                ? t.dashboard.unavailableShort
                : mistakesCount,
          },
        ]}
      />

      {accessLoading ? (
        <SignalCard
          icon={Sparkles}
          title={t.dashboard.readinessTitle}
          value={t.dashboard.loadingShort}
          hint={t.dashboard.readinessHint}
          progress={null}
          progressTone="primary"
          meta={[
            { label: t.dashboard.mockPassed, value: t.dashboard.loadingShort },
            { label: t.dashboard.passThreshold, value: "85%" },
          ]}
        />
      ) : isPaid ? (
        <SignalCard
          icon={Sparkles}
          title={t.dashboard.readinessTitle}
          value={formatPercent(readinessScore, readinessLoading)}
          hint={
            readinessError
              ? t.dashboard.signalUnavailable
              : t.dashboard.readinessHint
          }
          progress={readinessLoading || readinessError ? null : readinessScore}
          progressTone={scoreTone(readinessScore)}
          badge={
            readinessScore === null || readinessLoading || readinessError
              ? undefined
              : isReady
                ? t.dashboard.readinessReady
                : t.dashboard.readinessNotReady
          }
          badgeTone={isReady ? "success" : "warning"}
          meta={[
            {
              label: t.dashboard.mockPassed,
              value: readinessLoading
                ? t.dashboard.loadingShort
                : isReady
                  ? t.dashboard.yes
                  : t.dashboard.notYet,
            },
            {
              label: t.dashboard.passThreshold,
              value: "85%",
            },
          ]}
        />
      ) : (
        <section className="rounded-md border border-border bg-card p-5">
          <div className="flex items-start gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground">
              <Lock className="size-5" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-base font-semibold text-foreground">
                  {t.dashboard.readinessTitle}
                </h2>
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                  {t.dashboard.locked}
                </span>
              </div>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                {t.dashboard.readinessLocked}
              </p>
              <Link
                href={`/${lang}/me#subscription`}
                className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-primary underline-offset-4 hover:underline"
              >
                {t.dashboard.readinessLockedCta}
                <ArrowRight className="size-3.5" />
              </Link>
            </div>
          </div>
        </section>
      )}
    </section>
  );
}

function SignalCard({
  icon: Icon,
  title,
  value,
  hint,
  progress,
  progressTone,
  meta,
  badge,
  badgeTone,
}: {
  icon: LucideIcon;
  title: string;
  value: string;
  hint: string;
  progress: number | null;
  progressTone: "primary" | "success" | "warning" | "destructive";
  meta: { label: string; value: string | number }[];
  badge?: string;
  badgeTone?: "success" | "warning";
}) {
  const width =
    typeof progress === "number"
      ? `${Math.max(0, Math.min(100, Math.round(progress)))}%`
      : "0%";
  const barClass =
    progressTone === "success"
      ? "bg-success"
      : progressTone === "warning"
        ? "bg-warning"
        : progressTone === "destructive"
          ? "bg-destructive"
          : "bg-primary";
  const badgeClass =
    badgeTone === "success"
      ? "bg-success/10 text-success"
      : "bg-warning/15 text-warning-foreground";

  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
            <Icon className="size-5" />
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-base font-semibold text-foreground">
                {title}
              </h2>
              {badge && (
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-semibold ${badgeClass}`}
                >
                  {badge}
                </span>
              )}
            </div>
            <p className="mt-1 text-sm text-muted-foreground">{hint}</p>
          </div>
        </div>
        <span className="shrink-0 text-3xl font-semibold tabular-nums text-foreground">
          {value}
        </span>
      </div>

      <div className="mt-5 h-2 overflow-hidden rounded-full bg-muted">
        <div
          className={`h-full rounded-full transition-all duration-700 ${barClass}`}
          style={{ width }}
        />
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        {meta.map((item) => (
          <div
            key={item.label}
            className="rounded-md border border-border/70 bg-background px-3 py-2"
          >
            <p className="text-xs text-muted-foreground">{item.label}</p>
            <p className="mt-0.5 text-sm font-semibold tabular-nums text-foreground">
              {item.value}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

function LatestPracticePanel({
  t,
  lang,
  me,
}: {
  t: Dictionary;
  lang: Locale;
  me: ReturnType<typeof useMe>;
}) {
  const hasInProgress = me.data?.learning.has_in_progress_practice ?? false;

  return (
    <section className="rounded-md border border-border bg-card p-5 lg:col-span-1">
      <SectionHeading
        icon={PlayCircle}
        title={t.dashboard.latestPracticeTitle}
        label={t.dashboard.currentLabel}
      />

      {me.isLoading ? (
        <p className="mt-4 text-sm text-muted-foreground">
          {t.dashboard.loading}
        </p>
      ) : hasInProgress ? (
        <div className="mt-4 flex flex-col gap-4">
          <div>
            <p className="font-semibold text-foreground">
              {t.dashboard.latestPracticeInProgressTitle}
            </p>
            <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
              {t.dashboard.latestPracticeInProgressBody}
            </p>
          </div>
          <BackendNeededInline
            prefix={t.dashboard.backendNeededPrefix}
            text={t.dashboard.latestPracticeBackendNeeded}
          />
          <Link
            href={`/${lang}/practice`}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
          >
            {t.dashboard.openPractice}
            <ArrowRight className="size-4" />
          </Link>
        </div>
      ) : (
        <div className="mt-4 flex flex-col gap-4">
          <div>
            <p className="font-semibold text-foreground">
              {t.dashboard.latestPracticeEmptyTitle}
            </p>
            <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
              {t.dashboard.latestPracticeEmptyBody}
            </p>
          </div>
          <Link
            href={`/${lang}/practice`}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
          >
            {t.dashboard.startPractice}
            <ArrowRight className="size-4" />
          </Link>
        </div>
      )}
    </section>
  );
}

function NextActionPanel({
  t,
  lang,
  action,
  loading,
  error,
}: {
  t: Dictionary;
  lang: Locale;
  action: SummaryResponse["next_action"];
  loading: boolean;
  error: boolean;
}) {
  const primary = getNextActionPrimary(t, lang, action);

  return (
    <section className="rounded-md border border-border bg-card p-5 lg:col-span-2">
      <SectionHeading
        icon={Sparkles}
        title={t.dashboard.nextActionTitle}
        label={t.dashboard.recommendedLabel}
      />

      {loading ? (
        <p className="mt-4 text-sm text-muted-foreground">
          {t.dashboard.loading}
        </p>
      ) : error ? (
        <ErrorState text={t.dashboard.signalUnavailable} />
      ) : (
        <div className="mt-4 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="max-w-2xl">
            <p className="font-semibold text-foreground">
              {action && action.type !== "none"
                ? action.label
                : t.dashboard.noNextAction}
            </p>
            <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
              {action?.type === "review"
                ? t.dashboard.nextActionReviewAbsorbed
                : t.dashboard.nextActionHint}
            </p>
          </div>
          <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
            <Link
              href={primary.href}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
            >
              {primary.label}
              <ArrowRight className="size-4" />
            </Link>
          </div>
        </div>
      )}
    </section>
  );
}

function ReviewPackPanel({
  t,
  lang,
  data,
  loading,
  error,
  noPass,
  topicMap,
}: {
  t: Dictionary;
  lang: Locale;
  data: ReviewPack | undefined;
  loading: boolean;
  error: boolean;
  noPass: boolean;
  topicMap: Map<string, string>;
}) {
  const tasks = data?.tasks ?? [];
  const activeTasks = tasks.filter((task) => task.status !== "completed");
  const completedTasks = tasks.filter((task) => task.status === "completed");
  const pct =
    data && data.target_question_count > 0
      ? Math.round(
          (data.completed_question_count / data.target_question_count) * 100,
        )
      : 0;

  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <SectionHeading
          icon={ClipboardList}
          title={t.dashboard.reviewPackTitle}
          label={t.dashboard.secondaryLabel}
        />
        <span className="inline-flex h-9 items-center rounded-md border border-border bg-muted px-3 text-sm font-medium text-muted-foreground">
          {t.dashboard.reviewPackOpenSecondary}
        </span>
      </div>

      {loading ? (
        <p className="mt-4 text-sm text-muted-foreground">
          {t.dashboard.loading}
        </p>
      ) : noPass ? (
        <div className="mt-4 rounded-md border border-dashed border-border bg-muted/20 p-4">
          <div className="flex items-start gap-3">
            <Lock className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
            <div>
              <p className="font-semibold text-foreground">
                {t.dashboard.reviewPackLockedTitle}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {t.dashboard.reviewPackLockedBody}
              </p>
            </div>
          </div>
        </div>
      ) : error || !data ? (
        <ErrorState text={t.dashboard.errorGeneric} />
      ) : (
        <div className="mt-4 flex flex-col gap-4">
          <div>
            <div className="mb-2 flex items-center justify-between text-sm">
              <span className="font-semibold text-foreground">
                {t.dashboard.questionsDone
                  .replace("{done}", String(data.completed_question_count))
                  .replace("{total}", String(data.target_question_count))}
              </span>
              <span className="text-muted-foreground tabular-nums">{pct}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-all duration-700"
                style={{ width: `${pct}%` }}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <MiniStat
              label={t.dashboard.activeReviewTasks}
              value={activeTasks.length}
            />
            <MiniStat
              label={t.dashboard.completedTodayTitle}
              value={completedTasks.length}
            />
            <MiniStat
              label={t.dashboard.reviewPackProgressLabel}
              value={`${pct}%`}
            />
            <MiniStat
              label={t.dashboard.reviewPackStatusLabel}
              value={data.status}
            />
          </div>

          {activeTasks.length === 0 ? (
            <div className="rounded-md border border-dashed border-border bg-muted/20 p-4 text-sm text-muted-foreground">
              {t.dashboard.noActiveTasks}
            </div>
          ) : (
            <ul className="grid grid-cols-1 gap-3 lg:grid-cols-2">
              {activeTasks.slice(0, 4).map((task) => (
                <li key={task.review_task_id}>
                  <ReviewTaskRow
                    t={t}
                    lang={lang}
                    task={task}
                    topicName={
                      topicMap.get(task.topic_id) ?? `Topic ${task.topic_id}`
                    }
                  />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}

function ReviewTaskRow({
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
    <article className="rounded-md border border-border/70 bg-background p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate font-semibold text-foreground">{topicName}</p>
          <p className="mt-1 inline-flex items-center gap-1.5 text-sm text-muted-foreground">
            <TypeIcon className="size-3.5" />
            {typeLabel}
          </p>
        </div>
        <span
          className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${priorityClass}`}
        >
          {priorityLabel}
        </span>
      </div>

      <div className="mt-4 flex items-center justify-between gap-3 border-t border-border pt-3">
        <span className="text-sm text-muted-foreground tabular-nums">
          {task.completed_question_count}/{task.target_question_count}
        </span>
        <Link
          href={`/${lang}/practice`}
          className="inline-flex h-9 items-center justify-center gap-1.5 rounded-md border border-border bg-card px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          {ctaLabel}
          <CtaIcon className="size-3.5" />
        </Link>
      </div>
    </article>
  );
}

function HistoryPanels({ t }: { t: Dictionary }) {
  return (
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-lg font-semibold text-foreground">
          {t.dashboard.historyTitle}
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {t.dashboard.historySubtitle}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <HistoryBackendNeededCard
          t={t}
          icon={History}
          title={t.dashboard.practiceHistoryTitle}
          label={t.dashboard.latestTen}
          body={t.dashboard.practiceHistoryBackendNeeded}
        />
        <HistoryBackendNeededCard
          t={t}
          icon={Timer}
          title={t.dashboard.mockHistoryTitle}
          label={t.dashboard.latestTen}
          body={t.dashboard.mockHistoryBackendNeeded}
        />
      </div>
    </section>
  );
}

function HistoryBackendNeededCard({
  t,
  icon: Icon,
  title,
  label,
  body,
}: {
  t: Dictionary;
  icon: LucideIcon;
  title: string;
  label: string;
  body: string;
}) {
  return (
    <section className="rounded-md border border-border bg-card p-5">
      <SectionHeading icon={Icon} title={title} label={label} />
      <div className="mt-4 rounded-md border border-dashed border-border bg-muted/20 p-4">
        <div className="flex items-start gap-3">
          <AlertCircle className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
          <div>
            <p className="font-semibold text-foreground">
              {t.dashboard.backendNeededTitle}
            </p>
            <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
              {body}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}

function SectionHeading({
  icon: Icon,
  title,
  label,
}: {
  icon: LucideIcon;
  title: string;
  label?: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
        <Icon className="size-4" />
      </div>
      <div className="min-w-0">
        <h2 className="truncate text-base font-semibold text-foreground">
          {title}
        </h2>
        {label && (
          <p className="text-xs font-medium uppercase text-muted-foreground">
            {label}
          </p>
        )}
      </div>
    </div>
  );
}

function MiniStat({
  label,
  value,
}: {
  label: string;
  value: string | number;
}) {
  return (
    <div className="rounded-md border border-border/70 bg-background px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-0.5 truncate text-sm font-semibold tabular-nums text-foreground">
        {value}
      </p>
    </div>
  );
}

function BackendNeededInline({
  prefix,
  text,
}: {
  prefix: string;
  text: string;
}) {
  return (
    <div className="rounded-md border border-dashed border-border bg-muted/20 p-3 text-sm text-muted-foreground">
      <span className="font-medium text-foreground">{prefix} </span>
      {text}
    </div>
  );
}

function ErrorState({ text }: { text: string }) {
  return (
    <div className="mt-4 rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
      {text}
    </div>
  );
}

function getNextActionPrimary(
  t: Dictionary,
  lang: Locale,
  action: SummaryResponse["next_action"],
): { href: string; label: string } {
  if (action?.type === "mock_exam") {
    return { href: `/${lang}/mock`, label: t.dashboard.ctaMockButton };
  }

  return { href: `/${lang}/practice`, label: t.dashboard.ctaPracticeButton };
}

function formatPercent(value: number | null, loading: boolean): string {
  if (loading) return "...";
  if (typeof value !== "number") return "--";
  return `${Math.round(value)}%`;
}

function scoreTone(
  value: number | null,
): "success" | "warning" | "destructive" | "primary" {
  if (typeof value !== "number") return "primary";
  if (value >= 85) return "success";
  if (value >= 60) return "warning";
  return "destructive";
}
