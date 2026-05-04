import { useQuery } from "@tanstack/react-query";
import { apiFetchEnvelope } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type MistakeItem = {
  mistake_id: string;
  question_id: string;
  topic_id: string;
  wrong_count: number;
  last_wrong_at: string;
  source: "practice" | "review" | "mock" | string;
};

export type MistakesPage = {
  items: MistakeItem[];
  page: number;
  pageSize: number;
  total: number;
};

type MistakesMeta = {
  page?: number | string;
  page_size?: number | string;
  total?: number | string;
};

export function useMistakes(page: number, pageSize = 20) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mistakes", page, pageSize],
    enabled: !!user,
    staleTime: 30_000,
    queryFn: async (): Promise<MistakesPage> => {
      const env = await apiFetchEnvelope<{ items: MistakeItem[] }>(
        `/api/v1/mistakes?page=${page}&page_size=${pageSize}`,
      );
      const meta = (env.meta ?? {}) as MistakesMeta;
      return {
        items: env.data?.items ?? [],
        page: Number(meta.page ?? page),
        pageSize: Number(meta.page_size ?? pageSize),
        total: Number(meta.total ?? 0),
      };
    },
  });
}
