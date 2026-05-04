import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";

export type AttemptItem = {
  question_id: string;
  variant_id: string;
  topic_id: string;
  language: string;
  stem: string;
  choices: { key: string; text: string }[];
  correct_choice_key: string;
  selected_choice_key: string;
  explanation: string;
  is_correct: boolean;
  submitted_at: string;
};

/**
 * Read-only history of submitted answers in a practice session. Used by
 * the review-history view inside PracticeFlow so users can revisit past
 * questions (the next-question pool excludes already-answered items, so
 * this is the documented re-read path).
 *
 * Anonymous + free-trial sessions: open to whoever holds the session id
 * (matches PracticeService.requireSession). Owned sessions: must match
 * the authenticated user; full sessions also need an active pass.
 */
export function useAttempts(sessionId: string | null, language: string) {
  return useQuery({
    queryKey: ["practice-attempts", sessionId, language],
    enabled: !!sessionId,
    staleTime: 0,
    queryFn: () =>
      apiFetch<{ items: AttemptItem[] }>(
        `/api/v1/practice/sessions/${sessionId}/attempts?language=${encodeURIComponent(language)}`,
      ),
  });
}
