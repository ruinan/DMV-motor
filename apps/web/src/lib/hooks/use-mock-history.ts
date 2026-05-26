import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type MockAttemptHistoryItem = {
  attempt_id: string;
  mock_exam_id: string;
  mock_exam_code: string;
  status: "submitted" | "in_progress" | "ended_by_exit" | "expired" | string;
  /** -1 sentinel = null (not yet scored / exited / in-progress) */
  score_percent: number;
  correct_count: number;
  answered_count: number;
  started_at: string;
  submitted_at: string;
};

export type MockHistoryResponse = {
  attempts: MockAttemptHistoryItem[];
  total_in_db: number;
};

export type MockStatsResponse = {
  total_attempts: number;
  submitted_count: number;
  exited_count: number;
  /** -1 sentinel = no submitted attempts yet */
  recent_3_avg_score_percent: number;
  best_score_percent: number;
  latest_score_percent: number;
};

export function useMockHistory(limit = 10) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mock-history", limit],
    queryFn: () =>
      apiFetch<MockHistoryResponse>(
        `/api/v1/mock-exams/attempts/history?limit=${limit}`,
      ),
    enabled: !!user,
    staleTime: 30_000,
  });
}

export function useMockStats() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mock-stats"],
    queryFn: () => apiFetch<MockStatsResponse>("/api/v1/mock-exams/attempts/stats"),
    enabled: !!user,
    staleTime: 30_000,
  });
}
