"use client";

import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import type { TotpSecret } from "firebase/auth";
import { Loader2, ShieldCheck } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";

export type TotpEnrollStrings = {
  setup: string;
  scan: string;
  manualKey: string;
  codePlaceholder: string;
  verify: string;
  badCode: string;
  recentLogin: string;
  generic: string;
};

/**
 * Shared TOTP enrollment widget: tap to generate a secret, scan the QR (or copy
 * the key) into an authenticator app, then confirm a 6-digit code. Used both in
 * the /me security section and the forced-enrollment gate, so the strings are
 * passed in (different dictionary namespaces).
 */
export function TotpEnroll({
  strings,
  onEnrolled,
  align = "start",
}: {
  strings: TotpEnrollStrings;
  onEnrolled?: () => void;
  /** "center" matches the full-screen enrollment gate; "start" (default) suits
   *  the left-aligned /me security row. */
  align?: "start" | "center";
}) {
  const { startTotpEnrollment, finishTotpEnrollment } = useAuth();
  const centered = align === "center";
  const [secret, setSecret] = useState<TotpSecret | null>(null);
  const [qrUrl, setQrUrl] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function begin() {
    if (busy) return;
    setBusy(true);
    setErr(null);
    try {
      const res = await startTotpEnrollment();
      setSecret(res.secret);
      setQrUrl(res.qrUrl);
    } catch (e) {
      setErr(
        (e as { code?: string })?.code === "auth/requires-recent-login"
          ? strings.recentLogin
          : strings.generic,
      );
    } finally {
      setBusy(false);
    }
  }

  async function confirm() {
    if (busy || !secret) return;
    setBusy(true);
    setErr(null);
    try {
      await finishTotpEnrollment(secret, code.trim());
      onEnrolled?.();
    } catch {
      setErr(strings.badCode);
    } finally {
      setBusy(false);
    }
  }

  if (!secret) {
    return (
      <div className={centered ? "flex flex-col items-center" : ""}>
        <Button onClick={begin} disabled={busy}>
          {busy ? <Loader2 className="size-4 animate-spin" /> : <ShieldCheck className="size-4" />}
          {strings.setup}
        </Button>
        {err && <p className="mt-3 text-sm text-destructive">{err}</p>}
      </div>
    );
  }

  return (
    <div className={`flex flex-col gap-4 ${centered ? "items-center text-center" : ""}`}>
      <p className="text-sm text-muted-foreground">{strings.scan}</p>
      <div className="w-fit rounded-lg border border-border bg-white p-3">
        <QRCodeSVG value={qrUrl} size={168} />
      </div>
      <p className="text-xs text-muted-foreground">
        {strings.manualKey}{" "}
        <code className="select-all rounded bg-muted px-1.5 py-0.5 font-mono text-foreground">
          {secret.secretKey}
        </code>
      </p>
      <div className={`flex flex-wrap items-center gap-2 ${centered ? "justify-center" : ""}`}>
        <input
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
          placeholder={strings.codePlaceholder}
          className="w-32 rounded-md border border-border bg-background px-3 py-2 text-center font-mono tracking-widest"
        />
        <Button onClick={confirm} disabled={busy || code.length < 6}>
          {busy ? <Loader2 className="size-4 animate-spin" /> : null}
          {strings.verify}
        </Button>
      </div>
      {err && <p className="text-sm text-destructive">{err}</p>}
    </div>
  );
}
