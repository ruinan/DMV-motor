import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

/** The single latest server-computed backup for the current exam. */
export type Backup = {
  id: string;
  readiness_score: number;
  completion_score: number;
  mock_total_attempts: number;
  mock_best_score_percent: number | null;
  mock_recent3_avg_percent: number | null;
  practice_total_sessions: number;
  practice_accuracy_percent: number;
  active_mistakes_count: number;
  updated_at: string;
};

export type LatestBackup = { has_backup: boolean; backup: Backup | null };

/**
 * The user's single latest backup for their current exam (owner-only read,
 * allowed even after a downgrade). This is what a cache-wiped or new-platform
 * client downloads to rehydrate.
 */
export function useBackup() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["backup"],
    queryFn: () => apiFetch<LatestBackup>("/api/v1/backup/latest"),
    enabled: !!user,
    staleTime: 30_000,
  });
}

/**
 * Best-effort background sync for paid users. The server is the source of truth
 * and no-ops when nothing changed, so this is cheap to call after a session /
 * mock. Swallows all errors — backup is imperceptible and must never disrupt the
 * user (e.g. a free user gets 403; that's fine to ignore).
 */
export async function syncBackup(): Promise<void> {
  try {
    await apiFetch("/api/v1/backup/sync", { method: "POST" });
  } catch {
    // best-effort
  }
}
