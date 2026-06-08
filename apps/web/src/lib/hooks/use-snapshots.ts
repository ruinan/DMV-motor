import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

/** A saved progress snapshot (paid remote backup) for the current exam. */
export type Snapshot = {
  id: string;
  readiness_score: number;
  completion_score: number;
  mock_total_attempts: number;
  mock_best_score_percent: number | null;
  mock_recent3_avg_percent: number | null;
  practice_total_sessions: number;
  practice_accuracy_percent: number;
  active_mistakes_count: number;
  created_at: string;
};

/**
 * Past progress snapshots for the user's current exam (newest first). Listing is
 * open to any signed-in user — a downgraded account keeps the history it
 * recorded; only creating a new snapshot needs an active pass.
 */
export function useSnapshots() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["snapshots"],
    queryFn: async () => {
      const data = await apiFetch<{ snapshots: Snapshot[] }>(
        "/api/v1/backup/snapshots",
      );
      return data.snapshots;
    },
    enabled: !!user,
    staleTime: 30_000,
  });
}
