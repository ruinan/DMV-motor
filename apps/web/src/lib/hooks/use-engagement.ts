import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type Engagement = {
  current_streak_days: number;
  answered_today: number;
  daily_goal: number;
};

/**
 * Streak + daily-goal for the Study Hub engagement strip. Sends the client's
 * UTC offset (minutes to ADD to UTC, i.e. -getTimezoneOffset()) so the backend
 * buckets "today" / consecutive days by the user's clock, not server UTC. The
 * offset is read in the query fn (not render) to stay clear of React's purity
 * rule; it's stable enough that a static query key is fine.
 */
export function useEngagement() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["engagement"],
    queryFn: () => {
      const tz = -new Date().getTimezoneOffset();
      return apiFetch<Engagement>(`/api/v1/engagement?tz_offset_minutes=${tz}`);
    },
    enabled: !!user,
    staleTime: 60_000,
  });
}
