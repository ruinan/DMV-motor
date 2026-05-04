import { useQuery } from "@tanstack/react-query";
import { apiFetchEnvelope } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

/**
 * Active-mistakes count from the paginated /mistakes endpoint's `meta.total`.
 * Page size 1 minimises payload — we only want the total, not the items.
 */
export function useMistakesCount() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mistakes-count"],
    queryFn: async (): Promise<number> => {
      const env = await apiFetchEnvelope<{ items: unknown[] }>(
        "/api/v1/mistakes?page=1&page_size=1",
      );
      const meta = (env.meta ?? {}) as { total?: number | string };
      return Number(meta.total ?? 0);
    },
    enabled: !!user,
    staleTime: 60_000,
  });
}
