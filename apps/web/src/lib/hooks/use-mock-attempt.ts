import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";

export type MockAttemptChoice = { key: string; text: string };

export type MockAttemptQuestion = {
  question_id: string;
  variant_id: string;
  stem: string;
  choices: MockAttemptChoice[];
};

export type MockSavedAnswer = {
  question_id: string;
  selected_choice_key: string;
};

export type MockAttemptDetail = {
  mock_attempt_id: string;
  mock_exam_id: string;
  status: "in_progress" | "submitted" | "ended_by_exit" | "expired" | string;
  language: string;
  questions: MockAttemptQuestion[];
  saved_answers: MockSavedAnswer[];
};

/**
 * Fetches a mock attempt's full state (questions in the requested language +
 * the user's already-saved picks). Lets MockExam survive a refresh / new tab
 * without depending on sessionStorage.
 */
export function useMockAttempt(attemptId: string | null, language: string) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["mock-attempt", attemptId, language],
    queryFn: () =>
      apiFetch<MockAttemptDetail>(
        `/api/v1/mock-exams/attempts/${attemptId}?language=${language}`,
      ),
    enabled: !!user && !!attemptId,
    staleTime: 30_000,
  });
}
