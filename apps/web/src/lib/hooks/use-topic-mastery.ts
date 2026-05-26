import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type SubTopicMastery = {
  sub_topic_id: string;
  code: string;
  name_en: string;
  name_zh: string;
  is_mastered: boolean;
  attempted_count: number;
  correct_count: number;
  bank_size: number;
};

export type TopicMastery = {
  topic_id: string;
  code: string;
  name_en: string;
  name_zh: string;
  is_mastered: boolean;
  sub_topics: SubTopicMastery[];
};

export type TopicMasteryResponse = {
  topics: TopicMastery[];
  summary: {
    total_sub_topics: number;
    mastered_sub_topics: number;
  };
};

export function useTopicMastery() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["topic-mastery"],
    queryFn: () => apiFetch<TopicMasteryResponse>("/api/v1/topics/mastery"),
    enabled: !!user,
    staleTime: 60_000,
  });
}
