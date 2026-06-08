import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";

/**
 * Whether Stripe billing is wired on the backend (a secret key is configured).
 * Public endpoint. The subscription catalog uses it to choose real hosted
 * Checkout vs the dev grant fallback. Defaults to disabled while loading.
 */
export function useBillingConfig() {
  return useQuery({
    queryKey: ["billing-config"],
    queryFn: () => apiFetch<{ enabled: boolean }>("/api/v1/billing/config"),
    staleTime: 5 * 60_000,
  });
}
