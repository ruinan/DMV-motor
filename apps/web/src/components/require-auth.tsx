"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, RefreshCw } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

const STUCK_MS = 8_000;

type Props = {
  lang: Locale;
  t: Dictionary["auth"];
  children: ReactNode;
};

/**
 * Client guard for everything in the (app) route group. Wraps the *entire*
 * app shell (sidebar, top bar, content) so that while Firebase is hydrating
 * we render a focused full-screen spinner instead of a half-painted
 * dashboard that flashes for a frame before redirecting to /login.
 *
 * Three states:
 *   - loading + not stuck → centered spinner
 *   - loading >= STUCK_MS → failure card (refresh / go to sign in)
 *   - resolved + no user  → null (about to redirect)
 *   - resolved + user     → render children
 */
export function RequireAuth({ lang, t, children }: Props) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [stuck, setStuck] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.replace(`/${lang}/login`);
    }
  }, [loading, user, router, lang]);

  useEffect(() => {
    if (!loading) return;
    const id = setTimeout(() => setStuck(true), STUCK_MS);
    return () => clearTimeout(id);
  }, [loading]);

  if (stuck) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
        <div className="w-full max-w-sm rounded-xl border border-border/40 bg-card p-6 text-center shadow-sm">
          <h2 className="text-lg font-semibold text-foreground">
            {t.stuckTitle}
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">{t.stuckBody}</p>
          <div className="mt-6 flex flex-col gap-2">
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-primary text-sm font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md"
            >
              <RefreshCw className="size-4" />
              {t.refresh}
            </button>
            <Link
              href={`/${lang}/login`}
              className="inline-flex h-11 w-full items-center justify-center rounded-xl border border-border text-sm text-foreground transition-colors hover:bg-muted"
            >
              {t.goToSignIn}
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div
        role="status"
        aria-live="polite"
        className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background p-4"
      >
        <Loader2 className="size-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">{t.verifying}</p>
      </div>
    );
  }

  if (!user) {
    // useEffect above is about to router.replace to /login; render nothing
    // in the meantime so the app shell never flashes.
    return null;
  }

  return <>{children}</>;
}
