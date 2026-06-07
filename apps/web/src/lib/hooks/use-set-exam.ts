import { useQueryClient } from "@tanstack/react-query";
import { useRouter, usePathname } from "next/navigation";
import { apiFetch } from "@/lib/api-client";

/**
 * Returns a function that switches the user's current exam (PUT /api/v1/me/exam),
 * re-scopes every query, and lands the user on the Study Hub for the newly
 * selected exam.
 *
 * Why navigate, not just invalidate: switching exam re-scopes practice, questions,
 * topics, the mastery donut, recommendations, history and mistakes — a plain
 * refetch handles the data. But a page that's mid-session for the OLD exam (e.g.
 * /practice still showing an M1 question, or a per-question review) keeps that in
 * *local component state*, which a refetch can't clear. So we navigate to
 * /dashboard: the old page unmounts (its stale state is gone) and the dashboard
 * reflects the new exam's own in-progress session (Resume card) — or its empty
 * state if there's nothing yet. Switching back restores the other exam's session
 * the same way (the backend keeps each exam's in-progress session independently).
 *
 * Keep the activity context (B29/B31): switching from a "tracked" surface stays
 * there and re-scopes in place — settings (/me), practice (/practice), and the
 * study hub (/dashboard). Practice keeps local session state, so PracticeShell
 * keys PracticeFlow by exam id to remount it cleanly on the switch. Only the mock
 * surface (and anything else) isn't tracked → land on the study hub.
 */
export function useSetExam() {
  const queryClient = useQueryClient();
  const router = useRouter();
  const pathname = usePathname();
  return async function setExam(examId: string) {
    await apiFetch("/api/v1/me/exam", {
      method: "PUT",
      body: JSON.stringify({ exam_id: examId }),
    });
    await queryClient.invalidateQueries();
    if (/\/(me|practice|dashboard)(\/|$)/.test(pathname)) return;
    const lang = pathname.split("/")[1] || "en";
    router.push(`/${lang}/dashboard`);
  };
}
