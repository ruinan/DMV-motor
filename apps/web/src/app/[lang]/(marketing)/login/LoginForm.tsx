"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["auth"];
  lang: Locale;
};

export function LoginForm({ t, lang }: Props) {
  const router = useRouter();
  const { signIn } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await signIn(email, password);
      router.push(`/${lang}/me`);
    } catch {
      setError(t.error);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex flex-1 items-center justify-center px-6 py-16">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm rounded-xl border bg-card p-8 shadow-sm"
      >
        <h1 className="mb-6 text-2xl font-semibold">{t.signInTitle}</h1>

        <label
          htmlFor="email"
          className="mb-2 block text-sm font-medium"
        >
          {t.email}
        </label>
        <input
          id="email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
          className="mb-4 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/50"
        />

        <label
          htmlFor="password"
          className="mb-2 block text-sm font-medium"
        >
          {t.password}
        </label>
        <input
          id="password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          className="mb-6 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/50"
        />

        {error && (
          <p className="mb-4 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}

        <Button type="submit" disabled={submitting} className="w-full">
          {submitting ? t.loading : t.submit}
        </Button>
      </form>
    </main>
  );
}
