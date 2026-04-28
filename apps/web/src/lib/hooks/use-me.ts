import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type MeResponse = {
  user_id: string;
  email: string;
  language: string;
  access: {
    state: "free_trial" | "active" | "expired" | string;
    has_active_pass: boolean;
    expires_at: string | null;
    mock_remaining: number;
  };
  learning: {
    has_in_progress_practice: boolean;
    has_in_progress_review: boolean;
  };
};

export function useMe() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["me"],
    queryFn: () => apiFetch<MeResponse>("/api/v1/me"),
    enabled: !!user,
    staleTime: 30_000,
  });
}
