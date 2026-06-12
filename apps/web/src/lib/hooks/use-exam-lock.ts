"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useEntitlements } from "@/lib/hooks/use-entitlements";
import { useSetExam } from "@/lib/hooks/use-set-exam";
import type { Locale } from "@/lib/dictionaries";

/**
 * Shared "locked / open exam" behaviour for every exam-choice surface (the
 * sidebar switcher AND the settings picker), so the rule lives in ONE place.
 * They drifted once: the dropdown locked un-opened exams but the settings picker
 * let you switch into them freely — exactly the kind of bug a shared hook avoids.
 *
 * Locked = an exam that is neither the current one nor covered by an active pass
 * (from /exams/entitlements). Tapping a locked exam opens the free-trial /
 * subscribe sheet instead of switching straight in.
 */
export function useExamLock(lang: Locale) {
  const router = useRouter();
  const setExam = useSetExam();
  const entitlements = useEntitlements();
  // The exam whose "open" sheet (free trial / subscribe) is showing, if any.
  const [openExamId, setOpenExamId] = useState<string | null>(null);

  const subscribed = new Set(
    (entitlements.data ?? []).filter((e) => e.subscribed).map((e) => e.exam_id),
  );

  function isLocked(examId: string, currentId: string | null) {
    if (examId === currentId) return false;
    // Until entitlements load, don't flash a lock on an owned exam.
    if (!entitlements.data) return false;
    return !subscribed.has(examId);
  }

  // Free trial: re-scope to the exam (no payment) and drop into practice.
  async function openFree(examId: string) {
    setOpenExamId(null);
    await setExam(examId);
    router.push(`/${lang}/practice`);
  }

  // Subscribe: the settings catalog handles per-exam checkout / redeem.
  function openSubscribe() {
    setOpenExamId(null);
    router.push(`/${lang}/me#subscription`);
  }

  return {
    isLocked,
    openExamId,
    requestOpen: (examId: string) => setOpenExamId(examId),
    closeOpen: () => setOpenExamId(null),
    openFree,
    openSubscribe,
  };
}
