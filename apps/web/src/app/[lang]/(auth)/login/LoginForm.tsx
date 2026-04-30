"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Bike, Eye, EyeOff, Shield } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Props = {
  t: Dictionary["auth"];
  lang: Locale;
};

type Mode = "signin" | "create";

export function LoginForm({ t, lang }: Props) {
  const router = useRouter();
  const { signIn, signUp, resetPassword } = useAuth();
  const [mode, setMode] = useState<Mode>("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

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
    try {
      if (mode === "signin") {
        await signIn(email, password);
      } else {
        await signUp(email, password);
      }
      router.push(`/${lang}/me`);
    } catch {
      setError(mode === "signin" ? t.error : t.errorCreate);
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
          <Bike className="size-7" strokeWidth={2.25} />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          DMV Motor
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t.appTagline}</p>
      </div>

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

      {/* Reassurance footer */}
      <div className="mt-8 flex items-center justify-center gap-1.5 border-t border-border pt-5 text-xs text-muted-foreground">
        <Shield className="size-3.5" />
        <span>{t.securedBy}</span>
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
