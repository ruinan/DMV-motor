"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { Loader2, MailCheck, RefreshCw, ShieldCheck } from "lucide-react";
import { useAuth, hasMfaEnrolled } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

// Lazy — TotpEnroll pulls in qrcode.react, which only ~first-time users (no
// 2FA yet) ever see; keep it out of the authed-shell bundle every page pays.
const TotpEnroll = dynamic(
  () => import("@/components/totp-enroll").then((m) => m.TotpEnroll),
  {
    loading: () => (
      <Loader2 className="mx-auto size-5 animate-spin text-muted-foreground" />
    ),
  },
);

const STUCK_MS = 8_000;

/** Firebase throttles repeated verification-email sends; treat that as "already
 *  sent" rather than a failure. */
function isRateLimited(e: unknown): boolean {
  return (e as { code?: string })?.code === "auth/too-many-requests";
}

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
  // Set when the user finishes 2FA enrollment in the gate below — lets us pass
  // through immediately even if the Firebase user object reference didn't change.
  const [mfaJustEnrolled, setMfaJustEnrolled] = useState(false);
  // Same idea for the email-verification gate: reload() mutates the user in
  // place, so we flip this to advance without waiting for a new object ref.
  const [emailJustVerified, setEmailJustVerified] = useState(false);

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

  // Forced email verification: Identity Platform refuses second-factor
  // enrollment until the email is verified, so this step must precede the 2FA
  // gate (and any app surface). Without it, a new user is deadlocked — can't
  // enroll 2FA, can't get past the gate. Rendered inline like the MFA gate.
  if (!user.emailVerified && !emailJustVerified) {
    return (
      <EmailVerifyGate
        t={t}
        email={user.email}
        onVerified={() => setEmailJustVerified(true)}
      />
    );
  }

  // Forced 2FA: a signed-in user without an enrolled second factor must set one
  // up before reaching any app surface. Rendered inline (not a redirect) so it
  // can't loop with the exam-onboarding /start gate in AppChrome.
  //
  // Local testing can't enroll a real authenticator easily, so against the Auth
  // emulator we offer a STICKY skip (persisted) — otherwise forced-2FA walls off
  // every local session. Build-time inlined, so prod tree-shakes this out and the
  // gate stays mandatory there. (Backend MFA enforcement is its own flag, off
  // locally by default — see app.auth.mfa-required.)
  const isEmulator = process.env.NEXT_PUBLIC_USE_FIREBASE_EMULATOR === "true";
  const devMfaSkipped =
    isEmulator &&
    typeof window !== "undefined" &&
    window.localStorage.getItem("dmv-dev-skip-mfa") === "1";
  if (!hasMfaEnrolled(user) && !mfaJustEnrolled && !devMfaSkipped) {
    return (
      <MfaGate
        t={t}
        onDone={() => setMfaJustEnrolled(true)}
        onSkip={
          isEmulator
            ? () => {
                window.localStorage.setItem("dmv-dev-skip-mfa", "1");
                setMfaJustEnrolled(true);
              }
            : undefined
        }
      />
    );
  }

  return <>{children}</>;
}

function MfaGate({
  t,
  onDone,
  onSkip,
}: {
  t: Dictionary["auth"];
  onDone: () => void;
  /** Emulator-only sticky skip; undefined (and so hidden) against real Firebase. */
  onSkip?: () => void;
}) {
  const { signOut } = useAuth();
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <div className="w-full max-w-md rounded-xl border border-border/40 bg-card p-6 shadow-sm sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            <ShieldCheck className="size-7" />
          </div>
          <h2 className="text-xl font-bold text-foreground">{t.mfaGateTitle}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t.mfaGateBody}</p>
        </div>
        <TotpEnroll
          strings={{
            setup: t.mfaSetup,
            scan: t.mfaScan,
            manualKey: t.mfaManualKey,
            codePlaceholder: t.mfaCodePlaceholder,
            verify: t.mfaVerify,
            badCode: t.mfaBadCode,
            recentLogin: t.mfaRecentLogin,
            unverifiedEmail: t.mfaUnverifiedEmail,
            generic: t.mfaEnrollError,
          }}
          align="center"
          onEnrolled={onDone}
        />
        {onSkip && (
          <button
            type="button"
            data-testid="mfa-dev-bypass"
            onClick={onSkip}
            className="mt-4 inline-flex h-10 w-full items-center justify-center rounded-xl border border-dashed border-border text-sm font-medium text-muted-foreground transition-colors hover:bg-muted"
          >
            {t.mfaGateDevSkip}
          </button>
        )}
        <button
          type="button"
          onClick={() => signOut()}
          className="mt-6 w-full text-center text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          {t.mfaGateSignOut}
        </button>
      </div>
    </div>
  );
}

function EmailVerifyGate({
  t,
  email,
  onVerified,
}: {
  t: Dictionary["auth"];
  email: string | null;
  onVerified: () => void;
}) {
  const { resendVerificationEmail, reloadUser, devVerifyEmail, signOut } =
    useAuth();
  // Guards the one-shot auto-send so re-renders / dev StrictMode double-mount
  // don't fire a second email (Firebase rate-limits and it's confusing UX).
  const sent = useRef(false);
  const [status, setStatus] = useState<"idle" | "sending" | "sent" | "error">(
    "idle",
  );
  const [checking, setChecking] = useState(false);
  const [stillUnverified, setStillUnverified] = useState(false);
  // Local testing has no real inbox (Auth emulator never sends mail), so offer a
  // one-click bypass that applies the emulator's pending oob code. Build-time
  // inlined; the button never renders in a real (prod) build.
  const isEmulator =
    process.env.NEXT_PUBLIC_USE_FIREBASE_EMULATOR === "true";
  const [bypassing, setBypassing] = useState(false);

  // Auto-send one verification email when the gate first appears — covers both
  // a fresh sign-up and a user stranded here by an earlier build that never
  // sent one.
  useEffect(() => {
    if (sent.current) return;
    sent.current = true;
    setStatus("sending");
    resendVerificationEmail()
      .then(() => setStatus("sent"))
      .catch((e) => setStatus(isRateLimited(e) ? "sent" : "error"));
  }, [resendVerificationEmail]);

  async function resend() {
    setStatus("sending");
    setStillUnverified(false);
    try {
      await resendVerificationEmail();
      setStatus("sent");
    } catch (e) {
      // Firebase throttles verification emails per user. A rate-limit here just
      // means a link was already sent — show the "check your inbox" hint, not a
      // scary failure.
      setStatus(isRateLimited(e) ? "sent" : "error");
    }
  }

  async function check() {
    setChecking(true);
    setStillUnverified(false);
    try {
      if (await reloadUser()) onVerified();
      else setStillUnverified(true);
    } finally {
      setChecking(false);
    }
  }

  async function devBypass() {
    setBypassing(true);
    setStillUnverified(false);
    try {
      if (await devVerifyEmail()) onVerified();
      else setStillUnverified(true);
    } catch {
      setStillUnverified(true);
    } finally {
      setBypassing(false);
    }
  }

  return (
    <div
      data-testid="email-verify-gate"
      className="flex min-h-screen items-center justify-center bg-muted/40 p-4"
    >
      <div className="w-full max-w-md rounded-xl border border-border/40 bg-card p-6 shadow-sm sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            <MailCheck className="size-7" />
          </div>
          <h2 className="text-xl font-bold text-foreground">
            {t.verifyEmailTitle}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {t.verifyEmailBody.replace("{email}", email ?? "")}
          </p>
        </div>

        <button
          type="button"
          data-testid="verify-continue"
          onClick={check}
          disabled={checking}
          className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-primary text-sm font-semibold text-primary-foreground shadow-sm transition-shadow hover:shadow-md disabled:opacity-60"
        >
          {checking ? <Loader2 className="size-4 animate-spin" /> : null}
          {checking ? t.verifyEmailChecking : t.verifyEmailContinue}
        </button>

        {stillUnverified && (
          <p className="mt-3 text-sm text-destructive">
            {t.verifyEmailStillUnverified}
          </p>
        )}
        {status === "error" && (
          <p className="mt-3 text-sm text-destructive">{t.verifyEmailError}</p>
        )}
        {status === "sent" && !stillUnverified && (
          <p className="mt-3 text-sm text-muted-foreground">
            {t.verifyEmailResent}
          </p>
        )}

        <button
          type="button"
          data-testid="verify-resend"
          onClick={resend}
          disabled={status === "sending"}
          className="mt-4 w-full text-center text-sm text-primary transition-opacity hover:opacity-80 disabled:opacity-60"
        >
          {status === "sending" ? t.verifyEmailSending : t.verifyEmailResend}
        </button>

        {isEmulator && (
          <button
            type="button"
            data-testid="verify-dev-bypass"
            onClick={devBypass}
            disabled={bypassing}
            className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl border border-dashed border-border text-sm font-medium text-muted-foreground transition-colors hover:bg-muted disabled:opacity-60"
          >
            {bypassing ? <Loader2 className="size-4 animate-spin" /> : null}
            {bypassing ? t.verifyEmailDevBypassBusy : t.verifyEmailDevBypass}
          </button>
        )}

        <button
          type="button"
          onClick={() => signOut()}
          className="mt-6 w-full text-center text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          {t.verifyEmailSignOut}
        </button>
      </div>
    </div>
  );
}
