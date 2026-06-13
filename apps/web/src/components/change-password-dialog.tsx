"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { KeyRound, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export type ChangePasswordLabels = {
  title: string;
  body: string;
  current: string;
  next: string;
  confirm: string;
  submit: string;
  submitting: string;
  mismatch: string;
  weak: string;
  wrongCurrent: string;
  generic: string;
  cancel: string;
};

/**
 * Change-password form. Re-authenticates with the current password (Firebase
 * requires a recent login to set a new one), then updates it. Portals to <body>
 * so the overlay isn't trapped by an ancestor stacking context.
 */
export function ChangePasswordDialog({
  open,
  labels,
  onSuccess,
  onCancel,
}: {
  open: boolean;
  labels: ChangePasswordLabels;
  onSuccess: () => void;
  onCancel: () => void;
}) {
  const { changePassword } = useAuth();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (!open) return null;

  function reset() {
    setCurrent("");
    setNext("");
    setConfirm("");
    setErr(null);
  }
  function cancel() {
    reset();
    onCancel();
  }

  async function submit() {
    if (busy || !current || !next) return;
    setErr(null);
    if (next.length < 6) {
      setErr(labels.weak);
      return;
    }
    if (next !== confirm) {
      setErr(labels.mismatch);
      return;
    }
    setBusy(true);
    try {
      await changePassword(current, next);
      reset();
      onSuccess();
    } catch (e) {
      const code = (e as { code?: string })?.code;
      setErr(
        code === "auth/wrong-password" || code === "auth/invalid-credential"
          ? labels.wrongCurrent
          : code === "auth/weak-password"
            ? labels.weak
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
            <KeyRound className="size-4" aria-hidden />
          </span>
          <h2 className="text-base font-semibold text-foreground">{labels.title}</h2>
        </div>
        <p className="mb-4 text-sm text-muted-foreground">{labels.body}</p>
        <div className="flex flex-col gap-2">
          <input
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            placeholder={labels.current}
            className={field}
          />
          <input
            type="password"
            autoComplete="new-password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
            placeholder={labels.next}
            className={field}
          />
          <input
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") submit();
            }}
            placeholder={labels.confirm}
            className={field}
          />
        </div>
        {err && <p className="mt-3 text-sm text-destructive">{err}</p>}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={cancel} disabled={busy}>
            {labels.cancel}
          </Button>
          <Button onClick={submit} disabled={busy || !current || !next || !confirm}>
            {busy && <Loader2 className="size-4 animate-spin" aria-hidden />}
            {busy ? labels.submitting : labels.submit}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
