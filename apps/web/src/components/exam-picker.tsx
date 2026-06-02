"use client";

import { useState } from "react";
import { CheckCircle2, Loader2 } from "lucide-react";
import { ApiError } from "@/lib/api-client";
import { useExams } from "@/lib/hooks/use-exams";
import { useMe } from "@/lib/hooks/use-me";
import { useSetExam } from "@/lib/hooks/use-set-exam";
import type { Locale } from "@/lib/dictionaries";

type Labels = {
  loading: string;
  errorGeneric: string;
  empty: string;
};

/**
 * Lets the user pick which exam (state × license) they're preparing for and
 * persists it via PUT /api/v1/me/exam. Switching exam re-scopes practice,
 * topics, the mastery donut, recommendations and mock — so on success we
 * invalidate everything and let the surfaces refetch under the new exam.
 *
 * Reused by the settings switcher and the dashboard onboarding card.
 */
export function ExamPicker({
  lang,
  labels,
  onPicked,
}: {
  lang: Locale;
  labels: Labels;
  onPicked?: () => void;
}) {
  const exams = useExams(lang);
  const me = useMe();
  const setExam = useSetExam();
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [errMsg, setErrMsg] = useState<string | null>(null);

  const currentId = me.data?.current_exam?.id ?? null;

  async function pick(examId: string) {
    if (examId === currentId || submitting) return;
    setSubmitting(examId);
    setErrMsg(null);
    try {
      await setExam(examId);
      onPicked?.();
    } catch (e) {
      setErrMsg(e instanceof ApiError ? e.message : labels.errorGeneric);
    } finally {
      setSubmitting(null);
    }
  }

  if (exams.isLoading || me.isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" />
        {labels.loading}
      </div>
    );
  }

  if (exams.error) {
    return <p className="text-sm text-destructive">{labels.errorGeneric}</p>;
  }

  if (!exams.data || exams.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{labels.empty}</p>;
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        {exams.data.map((exam) => {
          const active = exam.id === currentId;
          const isSubmitting = submitting === exam.id;
          return (
            <button
              key={exam.id}
              type="button"
              onClick={() => pick(exam.id)}
              disabled={!!submitting}
              aria-pressed={active}
              className={`inline-flex h-10 items-center gap-2 rounded-lg border-2 px-4 text-sm font-medium transition-colors ${
                active
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border bg-background text-foreground hover:bg-muted"
              } disabled:cursor-not-allowed disabled:opacity-60`}
            >
              {active && <CheckCircle2 className="size-4" />}
              {exam.name}
              {isSubmitting && <Loader2 className="size-4 animate-spin" />}
            </button>
          );
        })}
      </div>
      {errMsg && <p className="text-sm text-destructive">{errMsg}</p>}
    </div>
  );
}
