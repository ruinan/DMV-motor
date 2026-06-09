"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  ArrowRight,
  CheckCircle2,
  Flame,
  Loader2,
  Target,
  Zap,
} from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useEngagement } from "@/lib/hooks/use-engagement";
import { useRecommendations } from "@/lib/hooks/use-recommendations";
import { useMe } from "@/lib/hooks/use-me";
import type { Dictionary, Locale } from "@/lib/dictionaries";

/**
 * Engagement strip at the top of the Study Hub (D1): keep-coming-back nudges for
 * EVERY signed-in user (free or paid, not paywalled) — current streak, today's
 * goal progress, and the single next-best action. The next-step CTA starts a
 * topic-scoped practice session (same path as Mistakes "Practice these") and
 * drops the user straight into /practice, which auto-resumes it.
 */
export function EngagementStrip({ t, lang }: { t: Dictionary; lang: Locale }) {
  const s = t.studyHub;
  const engagement = useEngagement();
  const recs = useRecommendations(lang, 1);
  const me = useMe();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState<string | null>(null);

  const streak = engagement.data?.current_streak_days ?? 0;
  const answeredToday = engagement.data?.answered_today ?? 0;
  const dailyGoal = engagement.data?.daily_goal ?? 10;
  const goalPct =
    dailyGoal > 0 ? Math.min(100, Math.round((answeredToday / dailyGoal) * 100)) : 0;
  const goalMet = dailyGoal > 0 && answeredToday >= dailyGoal;
  const topRec = recs.data?.[0] ?? null;

  async function practiceNextStep() {
    if (starting || !topRec) return;
    setStarting(true);
    setStartError(null);
    const entryType = me.data?.access.has_active_pass ? "full" : "free_trial";
    try {
      await apiFetch("/api/v1/practice/sessions", {
        method: "POST",
        body: JSON.stringify({
          entry_type: entryType,
          language: lang,
          topic_filter: topRec.topic_filter.map(Number),
        }),
      });
      // PracticeFlow auto-resumes the most-recent in-progress session on mount.
      queryClient.invalidateQueries({ queryKey: ["me"] });
      router.push(`/${lang}/practice`);
    } catch (err) {
      setStartError(err instanceof ApiError ? err.message : t.me.errorGeneric);
      setStarting(false);
    }
  }

  const reasonText =
    topRec?.reason_code === "active_mistakes"
      ? s.nextStepReasonActiveMistakes
      : topRec?.reason_code === "uncovered_key_topic"
        ? s.nextStepReasonUncoveredKeyTopic
        : "";

  // gap-px over a border-toned background paints hairline dividers between the
  // three white cells without per-cell border math.
  return (
    <section className="grid grid-cols-1 gap-px overflow-hidden rounded-xl border border-border/40 bg-border/40 shadow-sm sm:grid-cols-3">
      {/* Streak */}
      <div className="flex items-center gap-3 bg-card p-5">
        <span className="inline-flex size-11 shrink-0 items-center justify-center rounded-full bg-orange-500/10 text-orange-500">
          <Flame className="size-6" aria-hidden />
        </span>
        <div>
          <p className="text-2xl font-bold tabular-nums text-foreground">
            {streak > 0 ? streak : "—"}
            {streak > 0 && (
              <span className="ml-1 text-sm font-medium text-muted-foreground">
                {streak === 1 ? s.streakUnitOne : s.streakUnit}
              </span>
            )}
          </p>
          <p className="text-xs text-muted-foreground">
            {streak > 0 ? s.streakTitle : s.streakZero}
          </p>
          {streak > 0 && answeredToday === 0 && (
            <p className="text-[11px] text-orange-500">{s.streakKeepGoing}</p>
          )}
        </div>
      </div>

      {/* Daily goal */}
      <div className="flex flex-col justify-center gap-2 bg-card p-5">
        <div className="flex items-center justify-between">
          <span className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
            <Target className="size-4 text-primary" aria-hidden />
            {s.dailyGoalTitle}
          </span>
          {goalMet && <CheckCircle2 className="size-4 text-success" aria-hidden />}
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-primary transition-all duration-500"
            style={{ width: `${goalPct}%` }}
          />
        </div>
        <p className="text-xs text-muted-foreground">
          {goalMet
            ? s.dailyGoalMet
            : s.dailyGoalProgress
                .replace("{done}", String(answeredToday))
                .replace("{goal}", String(dailyGoal))}
        </p>
      </div>

      {/* Next best step */}
      <div className="flex flex-col justify-center gap-2 bg-card p-5">
        <span className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
          <Zap className="size-4 text-primary" aria-hidden />
          {s.nextStepTitle}
        </span>
        {topRec ? (
          <>
            <p className="truncate text-sm font-medium text-foreground">
              {topRec.label}
            </p>
            {reasonText && (
              <p className="text-xs text-muted-foreground">{reasonText}</p>
            )}
            <button
              type="button"
              onClick={practiceNextStep}
              disabled={starting}
              className="mt-1 inline-flex w-fit items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-shadow hover:shadow-md disabled:opacity-60"
            >
              {starting ? (
                <Loader2 className="size-3.5 animate-spin" aria-hidden />
              ) : (
                <ArrowRight className="size-3.5" aria-hidden />
              )}
              {starting ? s.nextStepStarting : s.nextStepCta}
            </button>
            {startError && (
              <p className="text-xs text-destructive">{startError}</p>
            )}
          </>
        ) : (
          <p className="text-xs text-muted-foreground">{s.nextStepEmpty}</p>
        )}
      </div>
    </section>
  );
}
