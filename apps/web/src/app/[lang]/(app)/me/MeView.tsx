"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type MeResponse = {
  user_id: string;
  email: string;
  language: string;
  access: {
    state: string;
    has_active_pass: boolean;
    expires_at: string | null;
    mock_remaining: number;
  };
  learning: {
    has_in_progress_practice: boolean;
    has_in_progress_review: boolean;
  };
};

type Props = {
  t: Dictionary["me"];
  lang: Locale;
};

export function MeView({ t, lang }: Props) {
  const router = useRouter();
  const { user, loading: authLoading, signOut } = useAuth();

  // Once Firebase has settled and there's no user, send them to /login
  useEffect(() => {
    if (!authLoading && !user) {
      router.replace(`/${lang}/login`);
    }
  }, [authLoading, user, router, lang]);

  const { data, isLoading, error } = useQuery({
    queryKey: ["me"],
    queryFn: () => apiFetch<MeResponse>("/api/v1/me"),
    enabled: !!user,
  });

  if (authLoading || (!user && !authLoading)) {
    return (
      <main className="flex flex-1 items-center justify-center">
        <p className="text-muted-foreground">{t.loading}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-16">
      <div className="mb-8 flex items-center justify-between">
        <h1 className="text-3xl font-semibold">{t.title}</h1>
        <Button variant="outline" onClick={() => signOut()}>
          Sign out
        </Button>
      </div>

      {isLoading && (
        <p className="text-muted-foreground">{t.loading}</p>
      )}

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error instanceof Error ? error.message : "Request failed"}
        </div>
      )}

      {data && (
        <dl className="grid grid-cols-1 gap-4 rounded-xl border bg-card p-6 shadow-sm sm:grid-cols-2">
          <Field label={t.userId} value={data.user_id} />
          <Field label={t.email} value={data.email} />
          <Field label={t.language} value={data.language} />
          <Field label={t.accessState} value={data.access.state} />
        </dl>
      )}
    </main>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 font-mono text-sm">{value}</dd>
    </div>
  );
}
