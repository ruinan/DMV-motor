import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type SummaryResponse = {
  completion_score: number;
  weak_topics: { topic_id: string; label: string }[];
  next_action: {
    type: "review" | "mock" | "practice" | "none" | string;
    label: string;
  } | null;
  // Paid-only — undefined for free-trial users
  readiness_score?: number;
  is_ready_candidate?: boolean;
};

export function useSummary() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["summary"],
    queryFn: () => apiFetch<SummaryResponse>("/api/v1/summary"),
    enabled: !!user,
    staleTime: 60_000,
  });
}
