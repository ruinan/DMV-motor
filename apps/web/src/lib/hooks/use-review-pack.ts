import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useMe } from "@/lib/hooks/use-me";

export type ReviewTaskSummary = {
  review_task_id: string;
  topic_id: string;
  type: "key_topic" | "persistent" | "mixed" | string;
  status: "pending" | "in_progress" | "completed" | string;
  // Backend (ReviewController.priorityTier) collapses the raw count to one
  // of three display tiers, so the union is exact — no `| string` fallback.
  priority: "high" | "medium" | "low";
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

/**
 * /review/pack only resolves for users with an active pass — anonymous /
 * free-trial / expired callers get 403 ACCESS_DENIED. We pre-flight the
 * pass status via /me and skip the call entirely when it would 403, so
 * the browser doesn't log a "Failed to load resource: 403" line for an
 * outcome we already know. UI surfaces a free-trial fallback either way.
 */
export function useReviewPack() {
  const me = useMe();
  const hasPass = me.data?.access.has_active_pass ?? false;

  const pack = useQuery({
    queryKey: ["review-pack"],
    queryFn: () => apiFetch<ReviewPack>("/api/v1/review/pack"),
    enabled: !!me.data && hasPass,
    staleTime: 30_000,
  });

  return {
    data: pack.data,
    isLoading: me.isLoading || (hasPass && pack.isLoading),
    error: pack.error,
    noPass: !!me.data && !hasPass,
  };
}
