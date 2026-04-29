import { useQuery } from "@tanstack/react-query";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type ReadinessGate =
  | "MOCK_SCORE_NOT_STABLE"
  | "KEY_COVERAGE_INCOMPLETE"
  | "HIGH_RISK_REVIEW_LOW"
  | "PERSISTENT_WEAK_POINT";

export type ReadinessResponse = {
  readiness_score: number;
  is_ready_candidate: boolean;
  missing_gates: ReadinessGate[];
};

export const ALL_READINESS_GATES: ReadinessGate[] = [
  "MOCK_SCORE_NOT_STABLE",
  "KEY_COVERAGE_INCOMPLETE",
  "HIGH_RISK_REVIEW_LOW",
  "PERSISTENT_WEAK_POINT",
];

export function useReadiness(enabled: boolean) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["readiness"],
    queryFn: () => apiFetch<ReadinessResponse>("/api/v1/readiness"),
    enabled: !!user && enabled,
    staleTime: 60_000,
    retry: (failureCount, error) => {
      // /readiness returns 403 ACCESS_DENIED for free-trial users — don't retry that.
      if (error instanceof ApiError && error.code === "ACCESS_DENIED") return false;
      return failureCount < 2;
    },
  });
}
