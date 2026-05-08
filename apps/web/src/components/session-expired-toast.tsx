"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { subscribeSessionExpired } from "@/lib/session-expired-bus";

const AUTO_DISMISS_MS = 6_000;

type Strings = {
  title: string;
  body: string;
  dismiss: string;
};

type Props = {
  t: Strings;
};

/**
 * Renders a top-right toast when api-client reports a 401 that survived a
 * force-refreshed token retry. Mounted at the root layout so it's reachable
 * from every surface — including unguarded pages like /practice — and
 * auto-dismisses after 6s so it doesn't stick around if the user is
 * mid-redirect to /login.
 */
export function SessionExpiredToast({ t }: Props) {
  const [visible, setVisible] = useState(false);

  useEffect(() => subscribeSessionExpired(() => setVisible(true)), []);

  useEffect(() => {
    if (!visible) return;
    const id = window.setTimeout(() => setVisible(false), AUTO_DISMISS_MS);
    return () => window.clearTimeout(id);
  }, [visible]);

  if (!visible) return null;

  return (
    <div
      role="alert"
      aria-live="assertive"
      data-testid="session-expired-toast"
      className="fixed right-4 top-4 z-50 flex max-w-sm items-start gap-3 rounded-xl border border-destructive/40 bg-card p-4 shadow-lg"
    >
      <div className="flex-1">
        <p className="text-sm font-semibold text-foreground">{t.title}</p>
        <p className="mt-1 text-sm text-muted-foreground">{t.body}</p>
      </div>
      <button
        type="button"
        onClick={() => setVisible(false)}
        aria-label={t.dismiss}
        className="text-muted-foreground transition-colors hover:text-foreground"
      >
        <X className="size-4" />
      </button>
    </div>
  );
}
