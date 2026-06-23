"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { AtSign, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export type ChangeEmailLabels = {
  title: string;
  body: string;
  newEmail: string;
  current: string;
  submit: string;
  submitting: string;
  invalidEmail: string;
  inUse: string;
  wrongCurrent: string;
  generic: string;
  cancel: string;
};

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/**
 * Change-email form. Re-authenticates with the current password, then sends a
 * confirmation link to the NEW address (Firebase verifyBeforeUpdateEmail) — the
 * email only switches once that link is clicked, so a typo can't lock you out.
 * Portals to <body> to escape the card's stacking context.
 */
export function ChangeEmailDialog({
  open,
  labels,
  onSuccess,
  onCancel,
}: {
  open: boolean;
  labels: ChangeEmailLabels;
  onSuccess: (newEmail: string) => void;
  onCancel: () => void;
}) {
  const { changeEmail } = useAuth();
  const [email, setEmail] = useState("");
  const [current, setCurrent] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (!open) return null;

  function reset() {
    setEmail("");
    setCurrent("");
    setErr(null);
  }
  function cancel() {
    reset();
    onCancel();
  }

  async function submit() {
    if (busy || !email || !current) return;
    setErr(null);
    if (!EMAIL_RE.test(email)) {
      setErr(labels.invalidEmail);
      return;
    }
    setBusy(true);
    try {
      await changeEmail(email, current);
      const sentTo = email;
      reset();
      onSuccess(sentTo);
    } catch (e) {
      const code = (e as { code?: string })?.code;
      setErr(
        code === "auth/wrong-password" || code === "auth/invalid-credential"
          ? labels.wrongCurrent
          : code === "auth/invalid-email"
            ? labels.invalidEmail
            : code === "auth/email-already-in-use"
              ? labels.inUse
              : labels.generic,
      );
    } finally {
      setBusy(false);
    }
  }

  const field =
    "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary/30";

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-xl">
        <div className="mb-3 flex items-center gap-2">
          <span className="inline-flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <AtSign className="size-4" aria-hidden />
          </span>
          <h2 className="text-base font-semibold text-foreground">{labels.title}</h2>
        </div>
        <p className="mb-4 text-sm text-muted-foreground">{labels.body}</p>
        <div className="flex flex-col gap-2">
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={labels.newEmail}
            className={field}
          />
          <input
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") submit();
            }}
            placeholder={labels.current}
            className={field}
          />
        </div>
        {err && <p className="mt-3 text-sm text-destructive">{err}</p>}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={cancel} disabled={busy}>
            {labels.cancel}
          </Button>
          <Button onClick={submit} disabled={busy || !email || !current}>
            {busy && <Loader2 className="size-4 animate-spin" aria-hidden />}
            {busy ? labels.submitting : labels.submit}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
