"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, ChevronDown, PlayCircle, Sparkles } from "lucide-react";
import { AttemptHistory } from "../../practice/AttemptHistory";
import { useMe } from "@/lib/hooks/use-me";
import { useReadiness } from "@/lib/hooks/use-readiness";
import { useTopicMastery } from "@/lib/hooks/use-topic-mastery";
import {
  usePracticeHistory,
  usePracticeStats,
  type PracticeSessionHistoryItem,
} from "@/lib/hooks/use-practice-history";
import {
  useMockHistory,
  useMockStats,
  type MockAttemptHistoryItem,
} from "@/lib/hooks/use-mock-history";
import { ReadinessRing } from "@/components/readiness-ring";
import { CoverageDonut } from "@/components/study-hub/CoverageDonut";
import { Sparkline } from "@/components/study-hub/Sparkline";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function Dashboard({ t, lang }: Props) {
  const me = useMe();
  const mastery = useTopicMastery();
  const practiceStats = usePracticeStats();
  // History is a review convenience, not an archive: keep the last 3 practices
  // and the last 3 mocks (mocks cost a pass). Everything older still feeds
  // readiness + mastery aggregates, just not these strips.
  const practiceHistory = usePracticeHistory(3);
  const mockStats = useMockStats();
  const mockHistory = useMockHistory(3);

  const isPaid = me.data?.access.has_active_pass ?? false;
  const readiness = useReadiness(isPaid);

  const inProgress = me.data?.learning.in_progress_practice;
  const masteredCount = mastery.data?.summary.mastered_sub_topics ?? 0;
  const totalSubTopics = mastery.data?.summary.total_sub_topics ?? 16;
  // "Touched" = any sub-topic with at least one attempt. Updates after a
  // single practice; mastered moves slower (needs the 4-of-last-4 + 80%
  // threshold). Both are exposed in the donut subtitle.
  const coveredCount =
    mastery.data?.topics.reduce(
      (sum, t) =>
        sum + t.sub_topics.filter((s) => s.attempted_count > 0).length,
      0,
    ) ?? 0;

  return (
    <div className="flex flex-col gap-10">
      <header>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.studyHub.title}
        </h1>
        <p className="mt-1 text-base text-muted-foreground">
          {t.studyHub.subtitle}
        </p>
      </header>

      {/* Hero: two gauges side by side */}
      <section className="grid grid-cols-1 gap-6 rounded-xl border border-border/40 bg-card p-6 shadow-sm md:grid-cols-2 md:gap-12 md:p-8">
        <div className="flex flex-col items-center gap-3">
          <CoverageDonut
            mastered={masteredCount}
            covered={coveredCount}
            total={totalSubTopics}
            label={t.studyHub.coverageTitle}
            masteredLabel={t.studyHub.coverageMastered}
            coveredLabel={t.studyHub.coverageTouched}
          />
        </div>
        <div className="flex flex-col items-center gap-3">
          <ReadinessRing
            percent={isPaid ? readiness.data?.readiness_score : null}
            label={t.studyHub.readinessLabel}
            lockedLabel={t.studyHub.readinessLocked}
          />
          <p className="text-center text-sm font-semibold text-foreground">
            {t.studyHub.readinessTitle}
          </p>
          {!isPaid && (
            <Link
              href={`/${lang}/me#subscription`}
              className="text-xs font-medium text-primary underline-offset-4 hover:underline"
            >
              {t.studyHub.readinessLockedCta}
            </Link>
          )}
        </div>
      </section>

      {/* Section 1: Resume CTA or Start CTA */}
      <ResumeOrStartCard t={t} lang={lang} inProgress={inProgress ?? null} stats={practiceStats.data} />

      {/* Section 2: Practice history */}
      <PracticeHistorySection
        t={t}
        lang={lang}
        sessions={practiceHistory.data?.sessions ?? []}
        totalInDb={practiceHistory.data?.total_in_db ?? 0}
        stats={practiceStats.data}
      />

      {/* Section 3: Mock exam history — only completed attempts (submitted,
          ended_by_failure, ended_by_exit). In-progress is hidden until the
          user finishes / fails / explicitly exits the attempt. */}
      <MockHistorySection
        t={t}
        lang={lang}
        attempts={(mockHistory.data?.attempts ?? []).filter(
          (a) => a.status !== "in_progress",
        )}
        totalInDb={mockHistory.data?.total_in_db ?? 0}
        stats={mockStats.data}
      />
    </div>
  );
}

// ============================================================
// Section 1
// ============================================================

function ResumeOrStartCard({
  t,
  lang,
  inProgress,
  stats,
}: {
  t: Dictionary;
  lang: Locale;
  inProgress: NonNullable<ReturnType<typeof useMe>["data"]>["learning"]["in_progress_practice"];
  stats:
    | {
        active_mistakes_count: number;
        active_mistakes_topic_count: number;
      }
    | undefined;
}) {
  if (inProgress) {
    return (
      <section className="rounded-xl border border-primary/30 bg-primary/5 p-6 shadow-sm md:p-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between md:gap-8">
          <div className="flex items-start gap-4">
            <PlayCircle className="size-10 shrink-0 text-primary" />
            <div>
              <h2 className="text-xl font-bold text-foreground">
                {t.studyHub.resumeTitle}
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {t.studyHub.resumeBody
                  .replace("{answered}", String(inProgress.answered_count))
                  .replace("{total}", String(inProgress.total_count))}
              </p>
            </div>
          </div>
          <Link
            href={`/${lang}/practice`}
            className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl bg-primary px-6 text-base font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md"
          >
            {t.studyHub.resumeCta}
            <ArrowRight className="size-4" />
          </Link>
        </div>
      </section>
    );
  }

  const mistakes = stats?.active_mistakes_count ?? 0;
  const mistakeTopics = stats?.active_mistakes_topic_count ?? 0;
  const subtitle =
    mistakes > 0
      ? t.studyHub.startBodyWithMistakes
          .replace("{n}", String(mistakes))
          .replace("{t}", String(mistakeTopics))
      : t.studyHub.startBodyFresh;

  return (
    <section className="rounded-xl border border-border/40 bg-card p-6 shadow-sm md:p-8">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between md:gap-8">
        <div className="flex items-start gap-4">
          <Sparkles className="size-10 shrink-0 text-primary" />
          <div>
            <h2 className="text-xl font-bold text-foreground">
              {t.studyHub.startTitle}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>
          </div>
        </div>
        <Link
          href={`/${lang}/practice`}
          className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl bg-primary px-6 text-base font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md"
        >
          {t.studyHub.startCta}
          <ArrowRight className="size-4" />
        </Link>
      </div>
    </section>
  );
}

// ============================================================
// Section 2
// ============================================================

function PracticeHistorySection({
  t,
  lang,
  sessions,
  totalInDb,
  stats,
}: {
  t: Dictionary;
  lang: Locale;
  sessions: PracticeSessionHistoryItem[];
  totalInDb: number;
  stats:
    | {
        total_sessions: number;
        active_mistakes_count: number;
        active_mistakes_topic_count: number;
      }
    | undefined;
}) {
  return (
    <section>
      <h2 className="mb-4 text-xl font-semibold text-foreground">
        {t.studyHub.practiceHistoryTitle}
      </h2>
      {sessions.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          {t.studyHub.practiceHistoryEmpty}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {sessions.map((s) => (
            <SessionCard key={s.session_id} t={t} lang={lang} session={s} />
          ))}
        </div>
      )}
      <p className="mt-4 text-sm text-muted-foreground">
        {t.studyHub.practiceAggregate
          .replace("{total}", String(stats?.total_sessions ?? totalInDb))
          .replace("{mistakes}", String(stats?.active_mistakes_count ?? 0))
          .replace("{topics}", String(stats?.active_mistakes_topic_count ?? 0))}
      </p>
    </section>
  );
}

// A history card. Tap to expand an inline per-question review of that session
// (reuses the same AttemptHistory component the practice page uses). When
// expanded it spans the full row so the review isn't cramped into a grid cell.
function SessionCard({
  t,
  lang,
  session,
}: {
  t: Dictionary;
  lang: Locale;
  session: PracticeSessionHistoryItem;
}) {
  const [expanded, setExpanded] = useState(false);
  const dateLabel = formatRelative(session.completed_at || session.started_at);
  const accuracyTone =
    session.accuracy_percent >= 85
      ? "text-success"
      : session.accuracy_percent >= 70
        ? "text-amber-600"
        : "text-destructive";
  return (
    <article
      className={`rounded-xl border border-border/40 bg-card text-sm shadow-sm ${
        expanded ? "sm:col-span-2 lg:col-span-3" : ""
      }`}
    >
      <button
        type="button"
        onClick={() => setExpanded((e) => !e)}
        aria-expanded={expanded}
        className="flex w-full flex-col gap-2 p-4 text-left transition-colors hover:bg-muted/40"
      >
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-muted-foreground">
            {dateLabel}
          </span>
          <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
            {session.entry_type === "full"
              ? t.studyHub.entryFull
              : t.studyHub.entryFreeTrial}
          </span>
        </div>
        <div className="flex items-end justify-between">
          <div>
            <p className={`text-2xl font-bold tabular-nums ${accuracyTone}`}>
              {session.accuracy_percent}%
            </p>
            <p className="text-xs text-muted-foreground">
              {session.correct_count} / {session.answered_count}{" "}
              {t.studyHub.correct}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {session.status === "in_progress" && (
              <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                {t.studyHub.inProgressBadge}
              </span>
            )}
            <ChevronDown
              className={`size-4 text-muted-foreground transition-transform ${
                expanded ? "rotate-180" : ""
              }`}
              aria-hidden
            />
          </div>
        </div>
      </button>
      {expanded && (
        <div className="border-t px-4 py-4">
          <AttemptHistory
            sessionId={session.session_id}
            lang={lang}
            t={t.practice}
            onBack={() => setExpanded(false)}
          />
        </div>
      )}
    </article>
  );
}

// ============================================================
// Section 3
// ============================================================

function MockHistorySection({
  t,
  lang,
  attempts,
  stats,
}: {
  t: Dictionary;
  lang: Locale;
  attempts: MockAttemptHistoryItem[];
  totalInDb: number;
  stats:
    | {
        total_attempts: number;
        recent_3_avg_score_percent: number;
        best_score_percent: number;
        latest_score_percent: number;
      }
    | undefined;
}) {
  // Reverse for sparkline (oldest → newest) — backend returns newest first.
  const sparklineValues = attempts
    .filter((a) => a.status === "submitted" && a.score_percent >= 0)
    .map((a) => a.score_percent)
    .reverse();

  const recent3Avg = stats?.recent_3_avg_score_percent;
  const best = stats?.best_score_percent;

  return (
    <section>
      <div className="mb-4 flex items-end justify-between">
        <h2 className="text-xl font-semibold text-foreground">
          {t.studyHub.mockHistoryTitle}
        </h2>
        {sparklineValues.length > 0 && (
          <Sparkline values={sparklineValues} width={140} height={36} />
        )}
      </div>
      {attempts.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          {t.studyHub.mockHistoryEmpty}
        </div>
      ) : (
        <div className="flex flex-wrap gap-2">
          {attempts.map((a) => (
            <MockBadge key={a.attempt_id} attempt={a} lang={lang} />
          ))}
        </div>
      )}
      <p className="mt-4 text-sm text-muted-foreground">
        {t.studyHub.mockAggregate
          .replace("{total}", String(stats?.total_attempts ?? 0))
          .replace(
            "{recentAvg}",
            recent3Avg != null && recent3Avg >= 0 ? `${recent3Avg}%` : "—",
          )
          .replace(
            "{best}",
            best != null && best >= 0 ? `${best}%` : "—",
          )}
      </p>
    </section>
  );
}

function MockBadge({
  attempt,
  lang,
}: {
  attempt: MockAttemptHistoryItem;
  lang: Locale;
}) {
  const tone =
    attempt.score_percent < 0
      ? "border-border bg-muted text-muted-foreground"
      : attempt.score_percent >= 85
        ? "border-success/40 bg-success/10 text-success"
        : attempt.score_percent >= 70
          ? "border-amber-500/40 bg-amber-500/10 text-amber-700"
          : "border-destructive/40 bg-destructive/10 text-destructive";
  const label = attempt.score_percent < 0 ? "—" : `${attempt.score_percent}%`;
  const date = formatRelative(attempt.submitted_at || attempt.started_at);
  // Tap a past mock to reopen it in review mode (sidebar stays; ?review=1) —
  // score + AI plan + per-question review.
  return (
    <Link
      href={`/${lang}/mock/${attempt.attempt_id}?review=1`}
      className={`flex flex-col items-center gap-1 rounded-lg border px-3 py-2 transition-shadow hover:shadow-sm ${tone}`}
      title={`${date} · ${attempt.status}`}
    >
      <span className="text-base font-bold tabular-nums">{label}</span>
      <span className="text-[10px] text-muted-foreground">{date}</span>
    </Link>
  );
}

// ============================================================
// Utils
// ============================================================

function formatRelative(iso: string): string {
  if (!iso) return "—";
  const date = new Date(iso);
  const diff = Date.now() - date.getTime();
  const day = 24 * 60 * 60 * 1000;
  if (diff < 60 * 1000) return "now";
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}m`;
  if (diff < day) return `${Math.floor(diff / (60 * 60 * 1000))}h`;
  if (diff < 30 * day) return `${Math.floor(diff / day)}d`;
  return date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}
