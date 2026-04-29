"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Timer } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type AccessResponse = {
  allowed: boolean;
  mock_remaining: number;
  reason: string;
};

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

const STORAGE_PREFIX = "dmv:mock-attempt:";

type Props = {
  t: Dictionary["mock"];
  lang: Locale;
};

export function MockLanding({ t, lang }: Props) {
  const router = useRouter();
  const { user } = useAuth();
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: access, isLoading } = useQuery({
    queryKey: ["mock-access"],
    queryFn: () => apiFetch<AccessResponse>("/api/v1/mock-exams/access"),
    enabled: !!user,
    staleTime: 30_000,
  });

  async function start() {
    setStarting(true);
    setError(null);
    try {
      const res = await apiFetch<StartResponse>("/api/v1/mock-exams/attempts", {
        method: "POST",
        body: JSON.stringify({ language: lang }),
      });
      // Stash the question payload so the [attemptId] page has data without
      // needing a backend GET endpoint. Cleared on submit/exit.
      window.sessionStorage.setItem(
        STORAGE_PREFIX + res.mock_attempt_id,
        JSON.stringify({
          questions: res.questions,
          language: lang,
          remaining_after_start: res.mock_remaining_after_start,
        }),
      );
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

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.title}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t.subtitle}</p>
      </header>

      <section className="flex items-center gap-4 rounded-xl border bg-card p-6 shadow-sm">
        <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
          <Timer className="size-6" />
        </div>
        <div className="flex-1">
          <p className="text-sm text-muted-foreground">{t.remaining}</p>
          <p className="text-2xl font-semibold tabular-nums">
            {isLoading ? "…" : (access?.mock_remaining ?? 0)}
          </p>
        </div>
      </section>

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {access && !access.allowed && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {access.reason || t.noAttemptsLeft}
        </div>
      )}

      <Button
        size="lg"
        disabled={
          starting || isLoading || (!!access && !access.allowed)
        }
        onClick={start}
      >
        {starting ? t.starting : t.startExam}
      </Button>
    </div>
  );
}
