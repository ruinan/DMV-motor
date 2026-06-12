import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

/** Per-exam state for the signed-in user. */
export type Entitlement = {
  exam_id: string;
  subscribed: boolean;
  /** True once the user has any practice activity for this exam — drives the
   *  "Free" (opened) vs "Locked" (never opened) distinction in the switcher. */
  has_activity: boolean;
};

/**
 * Drives the settings subscription catalog (Subscribe / Unsubscribe per exam).
 * Authed-only endpoint: one entry per active exam, {@code subscribed} true when
 * the user holds an active pass that unlocks it (V32 per-exam model). Pair with
 * {@link useExams} for the localized names.
 */
export function useEntitlements() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["entitlements"],
    queryFn: async () => {
      const data = await apiFetch<{ entitlements: Entitlement[] }>(
        "/api/v1/exams/entitlements",
      );
      return data.entitlements;
    },
    enabled: !!user,
    staleTime: 30_000,
  });
}
