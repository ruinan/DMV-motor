"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { KeyRound, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export type ReauthLabels = {
  title: string;
  body: string;
  placeholder: string;
  confirm: string;
  confirming: string;
  wrong: string;
  cancel: string;
};

/**
 * Re-enter-your-password gate for sensitive actions (billing changes today,
 * password change later). On confirm it calls Firebase reauthenticateWith
 * Credential + refreshes the ID token, then {@code onSuccess} retries the action
 * (the backend ReauthGuard now sees a fresh auth_time). High z-index so it sits
 * above page overlays (e.g. the locked-coverage card).
 */
export function ReauthDialog({
  open,
  labels,
  onSuccess,
  onCancel,
}: {
  open: boolean;
  labels: ReauthLabels;
  onSuccess: () => void;
  onCancel: () => void;
}) {
  const { reauth } = useAuth();
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (!open) return null;

  async function submit() {
    if (submitting || !password) return;
    setSubmitting(true);
    setErr(null);
    try {
      await reauth(password);
      setPassword("");
      onSuccess();
    } catch {
      setErr(labels.wrong);
    } finally {
      setSubmitting(false);
    }
  }

  function cancel() {
    setPassword("");
    setErr(null);
    onCancel();
  }

  // Portal to <body> so the overlay isn't trapped by an ancestor stacking context.
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-xl">
        <div className="mb-3 flex items-center gap-2">
          <span className="inline-flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <KeyRound className="size-4" aria-hidden />
          </span>
          <h2 className="text-base font-semibold text-foreground">{labels.title}</h2>
        </div>
        <p className="mb-4 text-sm text-muted-foreground">{labels.body}</p>
        <input
          type="password"
          value={password}
          autoFocus
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit();
          }}
          placeholder={labels.placeholder}
          className="mb-3 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary/30"
        />
        {err && <p className="mb-3 text-sm text-destructive">{err}</p>}
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={cancel} disabled={submitting}>
            {labels.cancel}
          </Button>
          <Button onClick={submit} disabled={submitting || !password}>
            {submitting && <Loader2 className="size-4 animate-spin" aria-hidden />}
            {submitting ? labels.confirming : labels.confirm}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
