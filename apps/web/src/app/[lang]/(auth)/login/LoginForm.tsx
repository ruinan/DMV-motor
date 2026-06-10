"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, GraduationCap, Eye, EyeOff, Shield } from "lucide-react";
import { useAuth, MfaRequiredError } from "@/lib/auth-context";
import { useRecaptcha } from "@/lib/hooks/use-recaptcha";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { MultiFactorResolver } from "firebase/auth";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["auth"];
  lang: Locale;
};

type Mode = "signin" | "create";

export function LoginForm({ t, lang }: Props) {
  const router = useRouter();
  const { signIn, signUp, resetPassword, resolveMfaSignIn } = useAuth();
  const { execute: recaptcha } = useRecaptcha();
  const [mode, setMode] = useState<Mode>("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  // Set when sign-in returns a 2FA challenge; we then prompt for the TOTP code.
  const [mfaResolver, setMfaResolver] = useState<MultiFactorResolver | null>(null);
  const [mfaCode, setMfaCode] = useState("");

  function switchMode(next: Mode) {
    setMode(next);
    setError(null);
    setInfo(null);
  }

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    setInfo(null);
    const action = mode === "signin" ? "login" : "register";
    try {
      // Bot precheck before handing off to Firebase — no-op when reCAPTCHA isn't
      // configured (token is null). The backend verifies the token.
      const token = await recaptcha(action);
      if (token) {
        await apiFetch(`/api/v1/auth/recaptcha-verify?action=${action}`, {
          method: "POST",
          headers: { "X-Recaptcha-Token": token },
        });
      }
      if (mode === "signin") {
        await signIn(email, password);
      } else {
        await signUp(email, password);
      }
      // Land on the study hub; the app shell's gate sends the user to /start
      // first if they haven't chosen an exam yet.
      router.push(`/${lang}/dashboard`);
    } catch (err) {
      if (err instanceof MfaRequiredError) {
        // 2FA account → show the code prompt; keep the password form hidden.
        setMfaResolver(err.resolver);
      } else if (
        err instanceof ApiError &&
        (err.code === "RECAPTCHA_FAILED" || err.code === "RECAPTCHA_REQUIRED")
      ) {
        setError(t.errorRecaptcha);
      } else {
        setError(mode === "signin" ? t.error : t.errorCreate);
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function onSubmitMfa(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!mfaResolver) return;
    setSubmitting(true);
    setError(null);
    try {
      await resolveMfaSignIn(mfaResolver, mfaCode.trim());
      router.push(`/${lang}/dashboard`);
    } catch {
      setError(t.mfaBadCode);
    } finally {
      setSubmitting(false);
    }
  }

  async function onForgot() {
    setError(null);
    setInfo(null);
    if (!email) {
      setError(t.resetNeedEmail);
      return;
    }
    try {
      await resetPassword(email);
      setInfo(t.resetSent);
    } catch {
      setError(t.resetError);
    }
  }

  const submitLabel =
    mode === "signin"
      ? submitting
        ? t.loading
        : t.submit
      : submitting
        ? t.loadingCreate
        : t.submitCreate;

  return (
    <main className="w-full max-w-[420px] rounded-xl border border-border/40 bg-card p-6 shadow-sm sm:p-8">
      {/* Brand */}
      <div className="mb-8 flex flex-col items-center text-center">
        <div className="mb-3 flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
          <GraduationCap className="size-7" strokeWidth={2.25} />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          DMV Prep
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t.appTagline}</p>
      </div>

      {mfaResolver ? (
        /* 2FA challenge — prompt for the authenticator code */
        <form onSubmit={onSubmitMfa} className="flex flex-col gap-4">
          <p className="text-sm text-muted-foreground">{t.mfaPrompt}</p>
          <input
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            autoFocus
            value={mfaCode}
            onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, ""))}
            placeholder={t.mfaCodePlaceholder}
            className="h-12 w-full rounded-xl border border-border bg-background px-4 text-center font-mono text-lg tracking-[0.4em] text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
          {error && (
            <p role="alert" className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting || mfaCode.length < 6}
            className="mt-1 inline-flex h-11 w-full items-center justify-center rounded-xl bg-primary text-base font-bold text-primary-foreground shadow-sm transition-all hover:shadow-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? t.loading : t.mfaVerifySignIn}
          </button>
          <button
            type="button"
            onClick={() => {
              setMfaResolver(null);
              setMfaCode("");
              setError(null);
            }}
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            {t.mfaCancel}
          </button>
        </form>
      ) : (
        <>
      {/* Tabs */}
      <div
        role="tablist"
        aria-label={t.tabSignIn}
        className="mb-6 flex border-b border-border"
      >
        <TabButton
          active={mode === "signin"}
          onClick={() => switchMode("signin")}
        >
          {t.tabSignIn}
        </TabButton>
        <TabButton
          active={mode === "create"}
          onClick={() => switchMode("create")}
        >
          {t.tabCreate}
        </TabButton>
      </div>

      {/* Form */}
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="email"
            className="text-sm font-semibold text-foreground"
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
            placeholder={t.emailPlaceholder}
            className="h-11 w-full rounded-xl border border-border bg-background px-4 text-base text-foreground transition-colors placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="password"
            className="text-sm font-semibold text-foreground"
          >
            {t.password}
          </label>
          <div className="relative">
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              required
              minLength={mode === "create" ? 6 : undefined}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={
                mode === "signin" ? "current-password" : "new-password"
              }
              placeholder={t.passwordPlaceholder}
              className="h-11 w-full rounded-xl border border-border bg-background pl-4 pr-12 text-base text-foreground transition-colors placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
            <button
              type="button"
              onClick={() => setShowPassword((s) => !s)}
              aria-label={showPassword ? t.hidePassword : t.showPassword}
              className="absolute right-1 top-1/2 inline-flex size-10 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors hover:text-foreground"
            >
              {showPassword ? (
                <EyeOff className="size-5" />
              ) : (
                <Eye className="size-5" />
              )}
            </button>
          </div>
        </div>

        {mode === "signin" && (
          <div className="flex justify-end">
            <button
              type="button"
              onClick={onForgot}
              className="text-sm text-primary transition-opacity hover:opacity-80"
            >
              {t.forgotPassword}
            </button>
          </div>
        )}

        {error && (
          <p
            role="alert"
            className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {error}
          </p>
        )}
        {info && (
          <p
            role="status"
            className="rounded-md bg-primary/10 px-3 py-2 text-sm text-primary"
          >
            {info}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="mt-1 inline-flex h-11 w-full items-center justify-center rounded-xl bg-primary text-base font-bold text-primary-foreground shadow-sm transition-all hover:shadow-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitLabel}
        </button>

        {/* Back to landing — sits directly under the primary CTA so the
            visual hierarchy is: action → escape hatch → trust footer. */}
        <Link
          href={`/${lang}`}
          className="inline-flex items-center justify-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          <span>{t.backToLanding}</span>
        </Link>
      </form>
        </>
      )}

      {/* Reassurance footer */}
      <div className="mt-8 flex flex-col items-center gap-1.5 border-t border-border pt-5 text-xs text-muted-foreground">
        <span className="flex items-center gap-1.5">
          <Shield className="size-3.5" />
          {t.securedBy}
        </span>
        <span>{t.recaptchaNotice}</span>
      </div>
    </main>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={`flex-1 border-b-2 pb-3 text-base transition-colors ${
        active
          ? "border-primary font-bold text-primary"
          : "border-transparent text-muted-foreground hover:text-foreground"
      }`}
    >
      {children}
    </button>
  );
}
