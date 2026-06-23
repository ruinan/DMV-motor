"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { AlertTriangle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export type DeleteAccountLabels = {
  title: string;
  body: string;
  warn: string;
  password: string;
  submit: string;
  submitting: string;
  wrongPassword: string;
  generic: string;
  cancel: string;
};

/**
 * Permanent account-deletion confirmation. Requires the current password — that
 * doubles as the re-authentication both the backend reauth gate and Firebase
 * deleteUser() demand. On success {@link useAuth().deleteAccount} hard-deletes
 * all server-side data, removes the Firebase identity, and redirects home, so
 * there's no success state to render here. Portals to <body> to escape the
 * card's stacking context.
 */
export function DeleteAccountDialog({
  open,
  labels,
  onCancel,
}: {
  open: boolean;
  labels: DeleteAccountLabels;
  onCancel: () => void;
}) {
  const { deleteAccount } = useAuth();
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (!open) return null;

  function cancel() {
    setPassword("");
    setErr(null);
    onCancel();
  }

  async function submit() {
    if (busy || !password) return;
    setErr(null);
    setBusy(true);
    try {
      await deleteAccount(password);
      // Success navigates away — leave the dialog as-is during the redirect.
    } catch (e) {
      const code = (e as { code?: string })?.code;
      setErr(
        code === "auth/wrong-password" || code === "auth/invalid-credential"
          ? labels.wrongPassword
          : labels.generic,
      );
      setBusy(false);
    }
  }

  const field =
    "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-destructive focus:ring-1 focus:ring-destructive/30";

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm rounded-xl border border-destructive/40 bg-card p-6 shadow-xl">
        <div className="mb-3 flex items-center gap-2">
          <span className="inline-flex size-9 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
            <AlertTriangle className="size-4" aria-hidden />
          </span>
          <h2 className="text-base font-semibold text-foreground">{labels.title}</h2>
        </div>
        <p className="mb-3 text-sm text-muted-foreground">{labels.body}</p>
        <p className="mb-4 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-foreground">
          {labels.warn}
        </p>
        <input
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit();
          }}
          placeholder={labels.password}
          className={field}
        />
        {err && <p className="mt-3 text-sm text-destructive">{err}</p>}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={cancel} disabled={busy}>
            {labels.cancel}
          </Button>
          <Button variant="destructive" onClick={submit} disabled={busy || !password}>
            {busy && <Loader2 className="size-4 animate-spin" aria-hidden />}
            {busy ? labels.submitting : labels.submit}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
