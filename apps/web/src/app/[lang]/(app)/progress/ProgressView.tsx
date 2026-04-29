"use client";

import Link from "next/link";
import { CheckCircle2, Circle, Lock, ArrowRight, Sparkles } from "lucide-react";
import { useMe } from "@/lib/hooks/use-me";
import { useSummary, type SummaryResponse } from "@/lib/hooks/use-summary";
import {
  useReadiness,
  ALL_READINESS_GATES,
  type ReadinessGate,
  type ReadinessResponse,
} from "@/lib/hooks/use-readiness";
import { ReadinessRing } from "@/components/readiness-ring";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function ProgressView({ t, lang }: Props) {
  const me = useMe();
  const summary = useSummary();
  const isPaid = me.data?.access.has_active_pass ?? false;
  const readiness = useReadiness(isPaid);

  return (
    <div className="flex flex-col gap-8">
      <header>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.progress.title}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {t.progress.subtitle}
        </p>
      </header>

      <ScoresPanel
        t={t}
        summary={summary.data}
        readiness={readiness.data}
        loading={summary.isLoading || me.isLoading}
        isPaid={isPaid}
      />

      <GatesPanel
        t={t}
        readiness={readiness.data}
        loading={readiness.isLoading}
        isPaid={isPaid}
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <WeakTopicsPanel
            t={t}
            lang={lang}
            weakTopics={summary.data?.weak_topics ?? []}
            loading={summary.isLoading}
          />
        </div>
        <NextActionPanel
          t={t}
          lang={lang}
          action={summary.data?.next_action ?? null}
        />
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
function ScoresPanel({
  t,
  summary,
  readiness,
  loading,
  isPaid,
}: {
  t: Dictionary;
  summary: SummaryResponse | undefined;
  readiness: ReadinessResponse | undefined;
  loading: boolean;
  isPaid: boolean;
}) {
  const completion = summary?.completion_score;
  // Prefer the canonical /readiness number when present (paid); fall back to
  // /summary's readiness_score (also paid) so we don't blink while readiness
  // is still loading.
  const readinessScore = isPaid
    ? (readiness?.readiness_score ?? summary?.readiness_score ?? null)
    : null;
  const isReady = readiness?.is_ready_candidate ?? summary?.is_ready_candidate ?? false;

  return (
    <section className="grid grid-cols-1 gap-4 md:grid-cols-2">
      {/* Completion (always visible) */}
      <div className="flex items-center gap-6 rounded-xl border border-border/30 bg-card p-6 shadow-sm">
        <ReadinessRing
          percent={loading ? null : (completion ?? 0)}
          label={t.progress.completionTitle}
        />
        <div className="flex-1">
          <h2 className="text-lg font-semibold text-foreground">
            {t.progress.completionTitle}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {t.progress.completionHint}
          </p>
        </div>
      </div>

      {/* Readiness (paid only) */}
      <div className="flex items-center gap-6 rounded-xl border border-border/30 bg-card p-6 shadow-sm">
        <ReadinessRing
          percent={isPaid ? (loading ? null : readinessScore) : null}
          label={t.progress.readinessTitle}
          lockedLabel={t.dashboard.readinessLocked}
        />
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-foreground">
              {t.progress.readinessTitle}
            </h2>
            {isPaid && readinessScore !== null && (
              <span
                className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                  isReady
                    ? "bg-green-500/15 text-green-700 dark:text-green-300"
                    : "bg-amber-500/15 text-amber-700 dark:text-amber-300"
                }`}
              >
                {isReady ? t.progress.readyBadge : t.progress.notReadyBadge}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {isPaid ? t.progress.readinessHint : t.progress.readinessLockedBody}
          </p>
        </div>
      </div>
    </section>
  );
}

// ---------------------------------------------------------------------------
function GatesPanel({
  t,
  readiness,
  loading,
  isPaid,
}: {
  t: Dictionary;
  readiness: ReadinessResponse | undefined;
  loading: boolean;
  isPaid: boolean;
}) {
  if (!isPaid) {
    return (
      <section className="rounded-xl border border-dashed border-border bg-muted/20 p-8 text-center">
        <Lock className="mx-auto mb-3 size-8 text-muted-foreground" />
        <h2 className="text-lg font-semibold text-foreground">
          {t.progress.readinessLockedTitle}
        </h2>
        <p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">
          {t.progress.readinessLockedBody}
        </p>
      </section>
    );
  }

  const missing = new Set<string>(readiness?.missing_gates ?? []);

  return (
    <section className="rounded-xl border border-border/30 bg-card p-6 shadow-sm">
      <header className="mb-4">
        <h2 className="text-lg font-semibold text-foreground">
          {t.progress.gatesTitle}
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {t.progress.gatesSubtitle}
        </p>
      </header>
      <ul className="grid grid-cols-1 gap-3 md:grid-cols-2">
        {ALL_READINESS_GATES.map((gateId) => {
          const passed = !loading && !missing.has(gateId);
          const copy = t.progress.gates[gateId as ReadinessGate];
          return (
            <li
              key={gateId}
              className={`flex items-start gap-3 rounded-lg border p-4 ${
                loading
                  ? "border-border/50 bg-muted/20"
                  : passed
                    ? "border-green-500/30 bg-green-500/5"
                    : "border-amber-500/30 bg-amber-500/5"
              }`}
            >
              {loading ? (
                <Circle className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              ) : passed ? (
                <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-green-600 dark:text-green-400" />
              ) : (
                <Circle className="mt-0.5 size-5 shrink-0 text-amber-600 dark:text-amber-400" />
              )}
              <div className="flex-1">
                <div className="flex items-baseline justify-between gap-2">
                  <h3 className="font-medium text-foreground">{copy.label}</h3>
                  {!loading && (
                    <span
                      className={`text-xs font-semibold ${
                        passed
                          ? "text-green-700 dark:text-green-300"
                          : "text-amber-700 dark:text-amber-300"
                      }`}
                    >
                      {passed ? t.progress.gatePassed : t.progress.gateOpen}
                    </span>
                  )}
                </div>
                <p className="mt-1 text-sm text-muted-foreground">{copy.body}</p>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

// ---------------------------------------------------------------------------
function WeakTopicsPanel({
  t,
  lang,
  weakTopics,
  loading,
}: {
  t: Dictionary;
  lang: Locale;
  weakTopics: { topic_id: string; label: string }[];
  loading: boolean;
}) {
  return (
    <section className="rounded-xl border border-border/30 bg-card p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-foreground">
        {t.progress.weakTopicsTitle}
      </h2>
      {loading ? (
        <p className="mt-3 text-sm text-muted-foreground">{t.progress.loading}</p>
      ) : weakTopics.length === 0 ? (
        <p className="mt-3 text-sm text-muted-foreground">
          {t.progress.noWeakTopics}
        </p>
      ) : (
        <ul className="mt-4 flex flex-col gap-2">
          {weakTopics.map((topic) => (
            <li
              key={topic.topic_id}
              className="flex items-center justify-between gap-3 rounded-lg border border-border/30 bg-background px-4 py-3"
            >
              <span className="font-medium text-foreground">{topic.label}</span>
              <Link
                href={`/${lang}/practice`}
                className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline"
              >
                {t.progress.practiceTopic}
                <ArrowRight className="size-3.5" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// ---------------------------------------------------------------------------
function NextActionPanel({
  t,
  lang,
  action,
}: {
  t: Dictionary;
  lang: Locale;
  action: SummaryResponse["next_action"];
}) {
  const fallback = (
    <section className="flex flex-col items-center rounded-xl border border-border/30 bg-card p-6 text-center shadow-sm">
      <Sparkles className="mb-3 size-8 text-primary" />
      <h2 className="text-lg font-semibold text-foreground">
        {t.progress.nextActionTitle}
      </h2>
      <p className="mt-2 text-sm text-muted-foreground">
        {t.progress.noNextAction}
      </p>
    </section>
  );

  if (!action || action.type === "none") return fallback;

  const { href, button } = (() => {
    switch (action.type) {
      case "review":
        return { href: `/${lang}/review`, button: t.progress.ctaReview };
      case "mock_exam":
        return { href: `/${lang}/mock`, button: t.progress.ctaMock };
      case "practice":
      default:
        return { href: `/${lang}/practice`, button: t.progress.ctaPractice };
    }
  })();

  return (
    <section className="flex flex-col rounded-xl bg-primary p-6 text-primary-foreground shadow-sm">
      <Sparkles className="mb-3 size-8 opacity-90" />
      <h2 className="text-lg font-semibold">{t.progress.nextActionTitle}</h2>
      <p className="mt-2 mb-5 text-sm opacity-90">{action.label}</p>
      <Link
        href={href}
        className="mt-auto inline-flex items-center justify-center rounded-md bg-primary-foreground px-4 py-2.5 text-sm font-semibold text-primary transition-opacity hover:opacity-90"
      >
        {button}
      </Link>
    </section>
  );
}
