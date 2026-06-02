import { useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";

/**
 * Returns a function that switches the user's current exam (PUT /api/v1/me/exam)
 * and then invalidates every query — switching exam re-scopes practice,
 * questions, topics, the mastery donut, recommendations, history and mistakes,
 * so the whole authed surface must refetch under the new exam.
 */
export function useSetExam() {
  const queryClient = useQueryClient();
  return async function setExam(examId: string) {
    await apiFetch("/api/v1/me/exam", {
      method: "PUT",
      body: JSON.stringify({ exam_id: examId }),
    });
    await queryClient.invalidateQueries();
  };
}
