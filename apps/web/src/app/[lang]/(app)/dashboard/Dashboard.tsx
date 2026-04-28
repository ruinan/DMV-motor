"use client";

import Link from "next/link";
import {
  TrendingUp,
  BadgeCheck,
  AlertCircle,
  ArrowRight,
  PlayCircle,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { useMe } from "@/lib/hooks/use-me";
import { useSummary, type SummaryResponse } from "@/lib/hooks/use-summary";
import { useMistakesCount } from "@/lib/hooks/use-mistakes-count";
import { ReadinessRing } from "@/components/readiness-ring";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function Dashboard({ t, lang }: Props) {
  const { user } = useAuth();
  const me = useMe();
  const summary = useSummary();
  const mistakes = useMistakesCount();

  const isPaid = me.data?.access.has_active_pass ?? false;

  return (
    <div className="flex flex-col gap-8">
      {/* Header */}
      <header>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.dashboard.welcomeBack}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {user?.email ?? me.data?.email ?? ""}
        </p>
      </header>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left column (2/3 on lg+) */}
        <div className="flex flex-col gap-6 lg:col-span-2">
          <ReadinessHero t={t} summary={summary.data} loading={summary.isLoading} isPaid={isPaid} />
          <MetricRow
            t={t}
            lang={lang}
            completion={summary.data?.completion_score}
            completionLoading={summary.isLoading}
            mockRemaining={me.data?.access.mock_remaining}
            mockTotal={5} // pass-bundle quota; placeholder until /me exposes total
            mockLoading={me.isLoading}
            mistakesCount={mistakes.data}
            mistakesLoading={mistakes.isLoading}
          />
        </div>

        {/* Right column (1/3 on lg+) */}
        <div className="flex flex-col gap-6">
          <NextActionCard
            t={t}
            lang={lang}
            action={summary.data?.next_action ?? null}
          />
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Readiness hero — ring + weak-topic chips
// ---------------------------------------------------------------------------
function ReadinessHero({
  t,
  summary,
  loading,
  isPaid,
}: {
  t: Dictionary;
  summary: SummaryResponse | undefined;
  loading: boolean;
  isPaid: boolean;
}) {
  const score = isPaid ? (summary?.readiness_score ?? null) : null;
  const weakTopics = summary?.weak_topics ?? [];

  return (
    <section className="flex flex-col items-center gap-6 rounded-xl border border-border/30 bg-card p-6 shadow-sm md:flex-row md:p-8">
      <ReadinessRing
        percent={loading ? null : score}
        label={t.dashboard.readinessTitle.split(" ")[0]}
        lockedLabel={isPaid ? "—" : t.dashboard.readinessLocked}
      />
      <div className="flex-1 text-center md:text-left">
        <h2 className="text-2xl font-semibold text-foreground">
          {t.dashboard.readinessTitle}
        </h2>
        <p className="mt-1 text-base text-muted-foreground">
          {isPaid ? t.dashboard.readinessHint : t.dashboard.readinessLocked}
        </p>

        <div className="mt-5">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            {t.dashboard.needsReview}
          </p>
          {weakTopics.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              {loading ? "…" : t.dashboard.noWeakTopics}
            </p>
          ) : (
            <div className="flex flex-wrap justify-center gap-2 md:justify-start">
              {weakTopics.map((topic) => (
                <span
                  key={topic.topic_id}
                  className="rounded-full bg-primary/10 px-3 py-1 text-sm text-primary"
                >
                  {topic.label}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

// ---------------------------------------------------------------------------
// Metric row — completion / mock quota / active mistakes
// ---------------------------------------------------------------------------
function MetricRow({
  t,
  lang,
  completion,
  completionLoading,
  mockRemaining,
  mockTotal,
  mockLoading,
  mistakesCount,
  mistakesLoading,
}: {
  t: Dictionary;
  lang: Locale;
  completion: number | undefined;
  completionLoading: boolean;
  mockRemaining: number | undefined;
  mockTotal: number;
  mockLoading: boolean;
  mistakesCount: number | undefined;
  mistakesLoading: boolean;
}) {
  return (
    <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {/* Completion */}
      <div className="flex flex-col rounded-xl border border-border/30 bg-card p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <span className="text-sm text-muted-foreground">
            {t.dashboard.completion}
          </span>
          <TrendingUp className="size-4 text-secondary-foreground/60" />
        </div>
        <div className="mt-auto">
          <span className="text-2xl font-semibold tabular-nums text-foreground">
            {completionLoading ? "…" : `${completion ?? 0}%`}
          </span>
          <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-primary transition-all duration-700"
              style={{ width: `${completion ?? 0}%` }}
            />
          </div>
        </div>
      </div>

      {/* Mock attempts remaining */}
      <div className="flex flex-col rounded-xl border border-border/30 bg-card p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <span className="text-sm text-muted-foreground">
            {t.dashboard.mockPassed}
          </span>
          <BadgeCheck className="size-4 text-secondary-foreground/60" />
        </div>
        <div className="mt-auto">
          <span className="text-2xl font-semibold tabular-nums text-foreground">
            {mockLoading ? "…" : (mockRemaining ?? 0)}{" "}
            <span className="text-sm font-normal text-muted-foreground">
              {t.dashboard.of} {mockTotal}
            </span>
          </span>
        </div>
      </div>

      {/* Active mistakes */}
      <Link
        href={`/${lang}/mistakes`}
        className="group flex flex-col rounded-xl border border-border/30 bg-card p-5 shadow-sm transition-colors hover:border-primary/30"
      >
        <div className="mb-3 flex items-center justify-between">
          <span className="text-sm text-muted-foreground">
            {t.dashboard.activeMistakes}
          </span>
          <AlertCircle className="size-4 text-destructive" />
        </div>
        <div className="mt-auto flex items-end justify-between">
          <span className="text-2xl font-semibold tabular-nums text-foreground">
            {mistakesLoading ? "…" : (mistakesCount ?? 0)}
          </span>
          <span className="flex items-center gap-1 text-sm font-medium text-primary group-hover:underline">
            {t.dashboard.review}
            <ArrowRight className="size-3.5" />
          </span>
        </div>
      </Link>
    </section>
  );
}

// ---------------------------------------------------------------------------
// Next action CTA — driven by /summary's next_action.type
// ---------------------------------------------------------------------------
function NextActionCard({
  t,
  lang,
  action,
}: {
  t: Dictionary;
  lang: Locale;
  action: SummaryResponse["next_action"];
}) {
  const fallback = (
    <div className="flex flex-col items-center rounded-xl bg-primary p-6 text-center text-primary-foreground shadow-sm">
      <PlayCircle className="mb-3 size-10 opacity-90" />
      <h3 className="text-xl font-semibold">{t.dashboard.ctaTitle}</h3>
      <p className="mt-1 text-sm opacity-90">{t.dashboard.noNextAction}</p>
    </div>
  );

  if (!action || action.type === "none") return fallback;

  const { href, button } = (() => {
    switch (action.type) {
      case "review":
        return { href: `/${lang}/review`, button: t.dashboard.ctaButton };
      case "mock":
        return { href: `/${lang}/mock`, button: t.dashboard.ctaMockButton };
      case "practice":
      default:
        return { href: `/${lang}/practice`, button: t.dashboard.ctaPracticeButton };
    }
  })();

  return (
    <div className="flex flex-col items-center rounded-xl bg-primary p-6 text-center text-primary-foreground shadow-sm">
      <PlayCircle className="mb-3 size-10 opacity-90" />
      <h3 className="text-xl font-semibold">{t.dashboard.ctaTitle}</h3>
      <p className="mt-1 mb-5 text-sm opacity-90">{action.label}</p>
      <Link
        href={href}
        className="inline-flex w-full items-center justify-center rounded-md bg-primary-foreground px-4 py-2.5 text-sm font-semibold text-primary transition-opacity hover:opacity-90"
      >
        {button}
      </Link>
    </div>
  );
}
