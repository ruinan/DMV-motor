import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
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

// /readiness returns 403 ACCESS_DENIED for free-trial users — the global
// QueryClient retry policy in query-provider.tsx already skips 401/403,
// so this hook can rely on the default retry behaviour.
export function useReadiness(enabled: boolean) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["readiness"],
    queryFn: () => apiFetch<ReadinessResponse>("/api/v1/readiness"),
    enabled: !!user && enabled,
    staleTime: 60_000,
  });
}
