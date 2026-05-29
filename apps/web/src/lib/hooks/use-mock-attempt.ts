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
  // Review detail — populated once the attempt is finished. During the live
  // exam correct_choice_key/is_correct are present (already shown post-answer)
  // but explanation is blank (the "why" stays hidden until the exam ends).
  correct_choice_key: string;
  is_correct: boolean;
  explanation: string;
};

export type MockAttemptDetail = {
  mock_attempt_id: string;
  mock_exam_id: string;
  status:
    | "in_progress"
    | "submitted"
    | "ended_by_failure"
    | "ended_by_exit"
    | "expired"
    | string;
  language: string;
  questions: MockAttemptQuestion[];
  saved_answers: MockSavedAnswer[];
  // Score summary — only populated once the attempt is finished. In-progress
  // attempts return the -1 / 0 sentinels (mirrors /attempts/history).
  score_percent: number;
  correct_count: number;
  wrong_count: number;
  // Timer: the countdown is anchored to started_at + time_limit_seconds so a
  // refresh resumes the same clock. time_used_seconds is -1 until finished.
  time_limit_seconds: number;
  started_at: string;
  time_used_seconds: number;
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
