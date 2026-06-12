"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Car, Bike, Loader2, ArrowRight } from "lucide-react";
import { useExams } from "@/lib/hooks/use-exams";
import { useSetExam } from "@/lib/hooks/use-set-exam";
import type { Locale } from "@/lib/dictionaries";

type Labels = {
  title: string;
  subtitle: string;
  select: string;
  loading: string;
  error: string;
  empty: string;
};

/**
 * First-run exam chooser, shown full-screen (no app chrome) before the user
 * reaches the dashboard. The (app) shell's gate redirects here whenever the
 * user has no current exam; picking one persists it and sends them to the
 * dashboard. After this they switch anytime from the sidebar.
 */
export function OnboardingExamChoice({
  lang,
  labels,
}: {
  lang: Locale;
  labels: Labels;
}) {
  const exams = useExams(lang);
  const setExam = useSetExam();
  const router = useRouter();
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  async function choose(id: string) {
    if (submitting) return;
    setSubmitting(id);
    setFailed(false);
    try {
      await setExam(id);
      router.replace(`/${lang}/dashboard`);
    } catch {
      setFailed(true);
      setSubmitting(null);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-muted/40 p-6">
      <div className="w-full max-w-2xl text-center">
        <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
          {labels.title}
        </h1>
        <p className="mx-auto mt-3 max-w-md text-muted-foreground">
          {labels.subtitle}
        </p>

        {exams.isLoading ? (
          <div className="mt-10 flex items-center justify-center gap-2 text-muted-foreground">
            <Loader2 className="size-5 animate-spin" />
            {labels.loading}
          </div>
        ) : exams.error ? (
          <p className="mt-10 text-destructive">{labels.error}</p>
        ) : !exams.data || exams.data.length === 0 ? (
          <p className="mt-10 text-muted-foreground">{labels.empty}</p>
        ) : (
          <div className="mt-10 grid grid-cols-1 gap-4 sm:grid-cols-2">
            {exams.data.map((exam) => {
              // Per-exam theme color (theme.css [data-exam]) — amber for
              // motorcycle, blue for Class C / car. The /start page has no
              // data-exam scope (both exams share one screen), so set each card's
              // accent explicitly instead of inheriting a single shared --primary
              // (which rendered BOTH cards blue). Mirrors the free-practice picker.
              const isMoto = exam.license_class.startsWith("M");
              const Icon = isMoto ? Bike : Car;
              const solid = isMoto ? "#b45309" : "#1b5e9b";
              const rgb = isMoto ? "180, 83, 9" : "27, 94, 155";
              const isSubmitting = submitting === exam.id;
              return (
                <button
                  key={exam.id}
                  type="button"
                  onClick={() => choose(exam.id)}
                  disabled={!!submitting}
                  style={{ borderColor: `rgba(${rgb}, 0.4)` }}
                  className="group flex flex-col items-start gap-4 rounded-2xl border-2 bg-card p-6 text-left shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <span
                    className="flex size-12 items-center justify-center rounded-full"
                    style={{ backgroundColor: `rgba(${rgb}, 0.12)`, color: solid }}
                  >
                    <Icon className="size-6" />
                  </span>
                  <span className="text-lg font-semibold" style={{ color: solid }}>
                    {exam.name}
                  </span>
                  <span
                    className="inline-flex items-center gap-1.5 text-sm font-medium"
                    style={{ color: solid }}
                  >
                    {labels.select}
                    {isSubmitting ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : (
                      <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
                    )}
                  </span>
                </button>
              );
            })}
          </div>
        )}

        {failed && <p className="mt-6 text-sm text-destructive">{labels.error}</p>}
      </div>
    </div>
  );
}
