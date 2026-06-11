"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Lock, Timer } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useMe } from "@/lib/hooks/use-me";
import { ExamIndicator } from "@/components/exam-indicator";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Choice = { key: string; text: string };

type Question = {
  question_id: string;
  variant_id: string;
  stem: string;
  choices: Choice[];
};

type StartResponse = {
  mock_attempt_id: string;
  status: string;
  mock_remaining_after_start: number;
  questions: Question[];
};

type Props = {
  t: Dictionary["mock"];
  lang: Locale;
};

export function MockLanding({ t, lang }: Props) {
  const router = useRouter();
  const me = useMe();
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Derive access straight from /me (already fetched app-wide) instead of a
  // second mock-access call — fewer backend round-trips, one source of truth.
  const hasPass = me.data?.access.has_active_pass ?? false;
  const remaining = me.data?.access.mock_remaining ?? 0;
  const canStart = hasPass && remaining > 0;

  async function start() {
    setStarting(true);
    setError(null);
    try {
      const res = await apiFetch<StartResponse>("/api/v1/mock-exams/attempts", {
        method: "POST",
        body: JSON.stringify({ language: lang }),
      });
      // No sessionStorage write — the /mock/[attemptId] page fetches its own
      // state from GET /attempts/{id}, which is refresh-resilient and works
      // cross-tab / cross-device.
      router.push(`/${lang}/mock/${res.mock_attempt_id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === "UNAUTHORIZED") {
          setError(t.errorAuthRequired);
        } else if (err.code === "ACCESS_DENIED") {
          setError(t.errorPassRequired);
        } else {
          setError(err.message);
        }
      } else {
        setError(t.errorGeneric);
      }
      setStarting(false);
    }
  }

  const header = (
    <header className="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.title}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t.subtitle}</p>
      </div>
      <ExamIndicator lang={lang} />
    </header>
  );

  // No active pass → don't surface a "0 attempts" counter or the raw backend
  // "no active pass" reason. Guide the user to subscribe / redeem instead.
  if (!me.isLoading && !hasPass) {
    return (
      <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
        {header}
        <section className="flex flex-col items-center gap-4 rounded-xl border bg-card p-8 text-center shadow-sm">
          <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            <Lock className="size-6" />
          </div>
          <p className="max-w-sm text-sm text-muted-foreground">
            {t.subscribeBody}
          </p>
          <Button size="lg" onClick={() => router.push(`/${lang}/me#subscription`)}>
            {t.subscribeCta}
          </Button>
        </section>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
      {header}

      <section className="flex items-center gap-4 rounded-xl border bg-card p-6 shadow-sm">
        <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
          <Timer className="size-6" />
        </div>
        <div className="flex-1">
          <p className="text-sm text-muted-foreground">{t.remaining}</p>
          <p className="text-2xl font-semibold tabular-nums">
            {me.isLoading ? "…" : remaining}
          </p>
        </div>
      </section>

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {/* Has a pass but used every attempt — distinct from "not subscribed". */}
      {!me.isLoading && !canStart && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {t.noAttemptsLeft}
        </div>
      )}

      <Button
        size="lg"
        disabled={starting || me.isLoading || !canStart}
        onClick={start}
      >
        {starting ? t.starting : t.startExam}
      </Button>
    </div>
  );
}
