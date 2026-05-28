"use client";

import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/api-client";

export type AiReviewPlanState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "ok"; text: string; cached: boolean }
  | { kind: "error"; message: string; code?: string };

type AiReviewPlanResponse = {
  plan: string;
  cached: boolean;
};

/**
 * Post-exam AI review plan for a completed mock attempt. One plan per attempt
 * is cached server-side, so re-clicking returns instantly with cached:true.
 */
export function useAiReviewPlan() {
  const [state, setState] = useState<AiReviewPlanState>({ kind: "idle" });

  async function generate(mockAttemptId: string, language: string) {
    setState({ kind: "loading" });
    try {
      const res = await apiFetch<AiReviewPlanResponse>("/api/v1/ai/review-plan", {
        method: "POST",
        body: JSON.stringify({ mock_attempt_id: mockAttemptId, language }),
      });
      setState({ kind: "ok", text: res.plan, cached: res.cached });
    } catch (err) {
      if (err instanceof ApiError) {
        setState({ kind: "error", message: err.message, code: err.code });
      } else {
        setState({ kind: "error", message: "Network error" });
      }
    }
  }

  return { state, generate };
}
