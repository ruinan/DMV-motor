"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import type { Locale } from "@/lib/dictionaries";

type Props = {
  lang: Locale;
  children: ReactNode;
};

/**
 * Client guard for the (app) route group. While Firebase is hydrating its
 * persisted auth state we render nothing useful (a thin loading hint); once
 * we know there's no user we replace to /login. Children render only when a
 * user is present, so downstream components can assume `useAuth().user` is
 * non-null.
 */
export function RequireAuth({ lang, children }: Props) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace(`/${lang}/login`);
    }
  }, [loading, user, router, lang]);

  if (loading || !user) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <p className="text-sm text-muted-foreground">…</p>
      </div>
    );
  }

  return <>{children}</>;
}
