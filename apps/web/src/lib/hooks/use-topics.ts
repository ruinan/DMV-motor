import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";

export type Topic = {
  id: string;
  parent_topic_id: string | null;
  code: string;
  name_en: string;
  name_zh: string;
  is_key_topic: boolean;
  risk_level: string;
  sort_order: number;
};

export function useTopics() {
  return useQuery({
    queryKey: ["topics"],
    queryFn: async () => {
      const data = await apiFetch<{ items: Topic[] }>("/api/v1/topics");
      return data.items;
    },
    staleTime: 5 * 60_000, // topics rarely change; cache aggressively
  });
}

/**
 * Build a map from topic id → localised name for fast lookup in lists.
 */
export function useTopicNameMap(lang: "en" | "zh"): Map<string, string> {
  const { data } = useTopics();
  const map = new Map<string, string>();
  if (data) {
    for (const t of data) {
      map.set(t.id, lang === "zh" ? t.name_zh : t.name_en);
    }
  }
  return map;
}
