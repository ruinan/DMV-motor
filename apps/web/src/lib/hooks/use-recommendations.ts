import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

/** A "what to reinforce next" recommendation (deterministic, exam-scoped). */
export type Recommendation = {
  topic_id: string;
  label: string;
  reason_code: "active_mistakes" | "uncovered_key_topic" | string;
  mistake_count: number;
  topic_filter: string[];
};

/**
 * Proactive next-best-action for the Study Hub. Ranked, exam-scoped topics with
 * a structured reason + a topic_filter the client hands straight to start-practice.
 */
export function useRecommendations(lang: "en" | "zh", limit = 1) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["recommendations", lang, limit],
    queryFn: async () => {
      const data = await apiFetch<{ recommendations: Recommendation[] }>(
        `/api/v1/ai/recommendations?language=${lang}&limit=${limit}`,
      );
      return data.recommendations;
    },
    enabled: !!user,
    staleTime: 60_000,
  });
}
