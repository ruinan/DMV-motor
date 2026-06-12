"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api-client";
import { useEntitlements } from "@/lib/hooks/use-entitlements";
import { useSetExam } from "@/lib/hooks/use-set-exam";
import type { Locale } from "@/lib/dictionaries";

/** Per-exam state, in increasing order of access. */
export type ExamStatus = "locked" | "free" | "paid";

/**
 * Shared exam-state rule for every exam-choice surface (the sidebar switcher AND
 * the settings picker), so it lives in ONE place. They drifted once: the dropdown
 * locked un-opened exams but the settings picker let you switch in freely —
 * exactly the kind of bug a shared hook avoids.
 *
 * Three states (server-derived; see ExamController.entitlements):
 *   - paid:   holds an active pass → full access.
 *   - free:   no pass, but the user has opened it (practiced it, or it's their
 *             current exam) → free-trial access, switchable, NOT locked.
 *   - locked: never opened → tapping opens the free-trial / subscribe sheet
 *             instead of switching straight in.
 * Paid is server-authoritative (gates real features); free/locked is only a UX
 * gate (free-trial practice is open to everyone anyway), so deriving "opened"
 * from activity is safe.
 */
export function useExamLock(lang: Locale) {
  const router = useRouter();
  const setExam = useSetExam();
  const entitlements = useEntitlements();
  // The exam whose "open" sheet (free trial / subscribe) is showing, if any.
  const [openExamId, setOpenExamId] = useState<string | null>(null);

  const byId = new Map((entitlements.data ?? []).map((e) => [e.exam_id, e]));

  function examStatus(examId: string, currentId: string | null): ExamStatus {
    const ent = byId.get(examId);
    if (ent?.subscribed) return "paid";
    // The current exam is always at least "opened"; otherwise read the server's
    // opened signal. Until entitlements load, treat as free (never flash a lock
    // on an exam we might own).
    if (examId === currentId || !entitlements.data) return "free";
    return ent?.opened ? "free" : "locked";
  }

  function isLocked(examId: string, currentId: string | null) {
    return examStatus(examId, currentId) === "locked";
  }

  // Free trial: persistently mark the exam as opened (so it stays Free even if
  // the user never practices), re-scope to it, and drop into practice. The
  // open-free call grants nothing beyond the free tier.
  async function openFree(examId: string) {
    setOpenExamId(null);
    await apiFetch(`/api/v1/exams/${examId}/open-free`, { method: "POST" });
    await setExam(examId); // re-scopes + invalidates all (incl. entitlements)
    router.push(`/${lang}/practice`);
  }

  // Subscribe: the settings catalog handles per-exam checkout / redeem.
  function openSubscribe() {
    setOpenExamId(null);
    router.push(`/${lang}/me#subscription`);
  }

  return {
    examStatus,
    isLocked,
    openExamId,
    requestOpen: (examId: string) => setOpenExamId(examId),
    closeOpen: () => setOpenExamId(null),
    openFree,
    openSubscribe,
  };
}
