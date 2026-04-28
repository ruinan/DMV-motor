import { useQuery } from "@tanstack/react-query";
import { firebaseAuth } from "@/lib/firebase";
import { useAuth } from "@/lib/auth-context";

/**
 * Active-mistakes count from the paginated /mistakes endpoint's `meta.total`.
 * apiFetch only returns `data`, so we make the fetch inline here to read the
 * envelope's `meta` field. Page size 1 minimises payload — we only want the
 * total, not the items.
 */
export function useMistakesCount() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mistakes-count"],
    queryFn: async (): Promise<number> => {
      const token = await firebaseAuth.currentUser?.getIdToken();
      const res = await fetch("/api/v1/mistakes?page=1&page_size=1", {
        headers: {
          Accept: "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      const json = await res.json().catch(() => null);
      if (!res.ok || !json?.success) {
        throw new Error(json?.error?.message ?? `HTTP ${res.status}`);
      }
      return Number(json.meta?.total ?? 0);
    },
    enabled: !!user,
    staleTime: 60_000,
  });
}
