"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";

export type MistakeReviewChoice = { key: string; text: string };

export type MistakeReview = {
  question_id: string;
  variant_id: string;
  stem: string;
  choices: MistakeReviewChoice[];
  correct_choice_key: string;
  explanation: string;
};

/**
 * Lazily fetches the full review detail (question + correct answer +
 * explanation) for one of the user's mistakes. Only runs when `enabled` (the
 * row is expanded), so collapsed rows cost nothing. Answers are served by a
 * gated endpoint that checks the question is the caller's active mistake.
 */
export function useMistakeReview(
  questionId: string,
  language: string,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["mistake-review", questionId, language],
    queryFn: () =>
      apiFetch<MistakeReview>(
        `/api/v1/mistakes/${questionId}/review?language=${language}`,
      ),
    enabled,
    staleTime: 60_000,
  });
}
