"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type AiReviewPlanView =
  | { state: "loading" }
  | { state: "pending" } // job running — keep polling
  | { state: "stalled" } // gave up polling; job likely failed
  | { state: "unavailable" } // AI turned off
  | { state: "ready"; plan: string };

type Resp = { status: "ready" | "pending" | "unavailable" | string; plan: string };

// Stop polling after ~30s (10 × 3s). A plan normally lands within a few
// seconds; if it hasn't by then the background job probably failed.
const MAX_POLLS = 10;

/**
 * Reads the auto-generated AI review plan for a completed mock attempt. The
 * plan is produced by a background job when the mock finishes — the client
 * never triggers it, it only polls here until the plan is ready.
 */
export function useAiReviewPlan(
  attemptId: string | null,
  language: string,
): AiReviewPlanView {
  const { user } = useAuth();
  const [polls, setPolls] = useState(0);
  const q = useQuery({
    // language is part of the key so switching locale refetches (the plan is
    // cached + generated per language; a new language lazily generates).
    queryKey: ["ai-review-plan", attemptId, language],
    queryFn: () => {
      setPolls((n) => n + 1);
      return apiFetch<Resp>(
        `/api/v1/ai/review-plan?mock_attempt_id=${attemptId}&language=${language}`,
      );
    },
    enabled: !!user && !!attemptId,
    refetchInterval: (query) => {
      const d = query.state.data;
      if (!d || d.status !== "pending") return false;
      return query.state.dataUpdateCount >= MAX_POLLS ? false : 3000;
    },
  });

  if (q.isLoading || (!q.data && q.isFetching)) return { state: "loading" };
  if (q.data?.status === "ready") return { state: "ready", plan: q.data.plan };
  if (q.data?.status === "unavailable") return { state: "unavailable" };
  if (q.data?.status === "pending") {
    return polls >= MAX_POLLS ? { state: "stalled" } : { state: "pending" };
  }
  return { state: "loading" };
}
