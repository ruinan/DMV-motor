import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type PracticeSessionHistoryItem = {
  session_id: string;
  entry_type: "free_trial" | "full" | string;
  language: string;
  status: "completed" | "in_progress" | "abandoned" | string;
  started_at: string;
  completed_at: string;
  answered_count: number;
  correct_count: number;
  accuracy_percent: number;
};

export type PracticeHistoryResponse = {
  sessions: PracticeSessionHistoryItem[];
  total_in_db: number;
};

export type PracticeStatsResponse = {
  total_sessions: number;
  total_questions_answered: number;
  total_correct: number;
  overall_accuracy_percent: number;
  active_mistakes_count: number;
  active_mistakes_topic_count: number;
};

export function usePracticeHistory(limit = 10) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["practice-history", limit],
    queryFn: () =>
      apiFetch<PracticeHistoryResponse>(
        `/api/v1/practice/sessions/history?limit=${limit}`,
      ),
    enabled: !!user,
    staleTime: 30_000,
  });
}

export function usePracticeStats() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["practice-stats"],
    queryFn: () =>
      apiFetch<PracticeStatsResponse>("/api/v1/practice/sessions/stats"),
    enabled: !!user,
    staleTime: 30_000,
  });
}
