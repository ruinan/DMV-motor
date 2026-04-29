import { useQuery } from "@tanstack/react-query";
import { firebaseAuth } from "@/lib/firebase";
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

/**
 * Paginated /mistakes — we have to do the fetch inline (rather than use
 * apiFetch) so we can read `meta.{page,page_size,total}` from the envelope.
 */
export function useMistakes(page: number, pageSize = 20) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mistakes", page, pageSize],
    enabled: !!user,
    staleTime: 30_000,
    queryFn: async (): Promise<MistakesPage> => {
      const token = await firebaseAuth.currentUser?.getIdToken();
      const res = await fetch(
        `/api/v1/mistakes?page=${page}&page_size=${pageSize}`,
        {
          headers: {
            Accept: "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        },
      );
      const json = await res.json().catch(() => null);
      if (!res.ok || !json?.success) {
        throw new Error(json?.error?.message ?? `HTTP ${res.status}`);
      }
      return {
        items: (json.data?.items ?? []) as MistakeItem[],
        page: Number(json.meta?.page ?? page),
        pageSize: Number(json.meta?.page_size ?? pageSize),
        total: Number(json.meta?.total ?? 0),
      };
    },
  });
}
