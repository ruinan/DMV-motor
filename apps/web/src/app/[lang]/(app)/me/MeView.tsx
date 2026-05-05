"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  CheckCircle2,
  CreditCard,
  Globe2,
  KeyRound,
  LogOut,
  RefreshCw,
  Sparkles,
  Trash2,
  User,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { apiFetch, ApiError } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type MeResponse = {
  user_id: string;
  email: string;
  language: string;
  access: {
    state: "free_trial" | "active" | "expired" | string;
    has_active_pass: boolean;
    expires_at: string | null;
    mock_remaining: number;
  };
  learning: {
    has_in_progress_practice: boolean;
    has_in_progress_review: boolean;
  };
};

type Props = {
  t: Dictionary["me"];
  lang: Locale;
};

export function MeView({ t, lang }: Props) {
  const router = useRouter();
  const { user, loading: authLoading, signOut } = useAuth();

  useEffect(() => {
    if (!authLoading && !user) router.replace(`/${lang}/login`);
  }, [authLoading, user, router, lang]);

  const { data, isLoading, error } = useQuery({
    queryKey: ["me"],
    queryFn: () => apiFetch<MeResponse>("/api/v1/me"),
    enabled: !!user,
  });

  if (authLoading || (!user && !authLoading)) {
    return (
      <div className="flex flex-1 items-center justify-center py-16">
        <p className="text-muted-foreground">{t.loading}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-8">
      <header>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          {t.title}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t.subtitle}</p>
      </header>

      {isLoading && <p className="text-muted-foreground">{t.loading}</p>}

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error instanceof ApiError ? error.message : t.errorGeneric}
        </div>
      )}

      {data && (
        <>
          <Hero t={t} data={data} onSignOut={() => signOut()} />

          <Group label={t.groupAccount}>
            <ProfileSection t={t} data={data} />
            <LanguageSection t={t} lang={lang} data={data} />
          </Group>

          <Group label={t.groupBilling}>
            <SubscriptionSection t={t} data={data} />
            <PaymentSection t={t} />
          </Group>

          <Group label={t.groupSecurity}>
            <SecuritySection t={t} />
          </Group>

          <Group label={t.groupDanger}>
            <DangerZone t={t} />
          </Group>
        </>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Layout primitives — Hero / Group / Section / Field / ComingSoon
// ---------------------------------------------------------------------------

function Hero({
  t,
  data,
  onSignOut,
}: {
  t: Dictionary["me"];
  data: MeResponse;
  onSignOut: () => void;
}) {
  const initial = (data.email?.[0] ?? "?").toUpperCase();
  const stateLabel =
    (t[`state_${data.access.state}` as keyof typeof t] as string | undefined) ??
    data.access.state;
  const stateTone =
    data.access.state === "active"
      ? "bg-primary/10 text-primary"
      : data.access.state === "expired"
        ? "bg-destructive/10 text-destructive"
        : "bg-muted text-muted-foreground";

  return (
    <div className="flex flex-wrap items-center gap-4 rounded-xl border bg-card p-5 shadow-sm">
      <span className="inline-flex size-12 shrink-0 items-center justify-center rounded-full bg-primary/10 text-lg font-semibold text-primary">
        {initial}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-base font-semibold text-foreground">
          {data.email}
        </p>
        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span
            className={`inline-flex items-center rounded-full px-2 py-0.5 font-medium ${stateTone}`}
          >
            {stateLabel}
          </span>
          <span>·</span>
          <span>
            {t.userId}: <span className="font-mono">{data.user_id}</span>
          </span>
        </div>
      </div>
      <Button variant="outline" onClick={onSignOut}>
        <LogOut className="size-4" />
        {t.signOut}
      </Button>
    </div>
  );
}

function Group({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-3">
      <h2 className="px-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {label}
      </h2>
      <div className="flex flex-col gap-3">{children}</div>
    </div>
  );
}

function Section({
  id,
  icon,
  title,
  description,
  tone = "default",
  children,
}: {
  id?: string;
  icon: React.ReactNode;
  title: string;
  description?: string;
  tone?: "default" | "danger";
  children: React.ReactNode;
}) {
  const isDanger = tone === "danger";
  return (
    <section
      id={id}
      className={`scroll-mt-20 rounded-xl border p-5 shadow-sm ${
        isDanger
          ? "border-destructive/30 bg-destructive/5"
          : "border-border bg-card"
      }`}
    >
      <div className="mb-4 flex items-start gap-3">
        <span
          className={`mt-0.5 inline-flex size-9 shrink-0 items-center justify-center rounded-lg ${
            isDanger
              ? "bg-destructive/15 text-destructive"
              : "bg-primary/10 text-primary"
          }`}
        >
          {icon}
        </span>
        <div className="min-w-0">
          <h3
            className={`text-base font-semibold ${
              isDanger ? "text-destructive" : "text-foreground"
            }`}
          >
            {title}
          </h3>
          {description && (
            <p className="mt-0.5 text-sm text-muted-foreground">{description}</p>
          )}
        </div>
      </div>
      {children}
    </section>
  );
}

function ComingSoon({ label }: { label: string }) {
  return (
    <span className="inline-flex shrink-0 items-center rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
      {label}
    </span>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 break-all text-sm font-medium text-foreground">
        {value}
      </dd>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------

function ProfileSection({
  t,
  data,
}: {
  t: Dictionary["me"];
  data: MeResponse;
}) {
  return (
    <Section
      id="profile"
      icon={<User className="size-4" />}
      title={t.sectionProfile}
      description={t.sectionProfileBody}
    >
      <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label={t.userId} value={data.user_id} />
        <Field label={t.email} value={data.email} />
      </dl>
    </Section>
  );
}

// ---------------------------------------------------------------------------
// Language
// ---------------------------------------------------------------------------

function LanguageSection({
  t,
  lang,
  data,
}: {
  t: Dictionary["me"];
  lang: Locale;
  data: MeResponse;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [submitting, setSubmitting] = useState<Locale | null>(null);
  const [errMsg, setErrMsg] = useState<string | null>(null);

  async function pick(next: Locale) {
    if (data.language === next || submitting) return;
    setSubmitting(next);
    setErrMsg(null);
    try {
      await apiFetch("/api/v1/me/language", {
        method: "PUT",
        body: JSON.stringify({ language: next }),
      });
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      const segments = window.location.pathname.split("/");
      if (segments[1] === lang) segments[1] = next;
      router.push(segments.join("/") || `/${next}/me`);
    } catch (e) {
      setErrMsg(e instanceof ApiError ? e.message : t.errorGeneric);
    } finally {
      setSubmitting(null);
    }
  }

  return (
    <Section
      id="language"
      icon={<Globe2 className="size-4" />}
      title={t.sectionLanguage}
      description={t.sectionLanguageBody}
    >
      <div className="flex flex-wrap gap-2">
        {(["en", "zh"] as Locale[]).map((code) => {
          const active = data.language === code;
          const isSubmitting = submitting === code;
          return (
            <button
              key={code}
              type="button"
              onClick={() => pick(code)}
              disabled={!!submitting}
              className={`inline-flex h-10 items-center gap-2 rounded-lg border-2 px-4 text-sm font-medium transition-colors ${
                active
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border bg-background text-foreground hover:bg-muted"
              } disabled:cursor-not-allowed disabled:opacity-60`}
            >
              {active && <CheckCircle2 className="size-4" />}
              {code === "en" ? t.languageEnglish : t.languageChinese}
              {isSubmitting && (
                <span className="text-xs text-muted-foreground">…</span>
              )}
            </button>
          );
        })}
      </div>
      {errMsg && <p className="mt-3 text-sm text-destructive">{errMsg}</p>}
    </Section>
  );
}

// ---------------------------------------------------------------------------
// Subscription — anchor target #subscription
// ---------------------------------------------------------------------------

function SubscriptionSection({
  t,
  data,
}: {
  t: Dictionary["me"];
  data: MeResponse;
}) {
  const state = data.access.state;
  const expiresAt = data.access.expires_at
    ? new Date(data.access.expires_at).toLocaleDateString()
    : null;

  return (
    <Section
      id="subscription"
      icon={<Sparkles className="size-4" />}
      title={t.sectionSubscription}
      description={t.sectionSubscriptionBody}
    >
      <dl className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Field
          label={t.accessState}
          value={
            (t[`state_${state}` as keyof typeof t] as string | undefined) ??
            state
          }
        />
        <Field label={t.mockRemaining} value={data.access.mock_remaining} />
        <Field
          label={t.expiresAt}
          value={
            expiresAt ?? <span className="text-muted-foreground">—</span>
          }
        />
      </dl>

      <div className="mt-5 flex flex-wrap items-center gap-3">
        {state === "free_trial" && (
          <>
            <Button disabled>{t.subscriptionGetPass}</Button>
            <ComingSoon label={t.comingSoon} />
          </>
        )}
        {state === "active" && (
          <>
            <Button variant="outline" disabled>
              {t.subscriptionManage}
            </Button>
            <ComingSoon label={t.comingSoon} />
          </>
        )}
        {state === "expired" && (
          <>
            <Button disabled>{t.subscriptionRenew}</Button>
            <ComingSoon label={t.comingSoon} />
          </>
        )}
      </div>
    </Section>
  );
}

// ---------------------------------------------------------------------------
// Payment
// ---------------------------------------------------------------------------

function PaymentSection({ t }: { t: Dictionary["me"] }) {
  return (
    <Section
      id="payment"
      icon={<CreditCard className="size-4" />}
      title={t.sectionPayment}
      description={t.sectionPaymentBody}
    >
      <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
        <p className="text-muted-foreground">{t.paymentEmpty}</p>
        <ComingSoon label={t.comingSoon} />
      </div>
    </Section>
  );
}

// ---------------------------------------------------------------------------
// Security
// ---------------------------------------------------------------------------

function SecuritySection({ t }: { t: Dictionary["me"] }) {
  return (
    <Section
      id="security"
      icon={<KeyRound className="size-4" />}
      title={t.sectionSecurity}
      description={t.sectionSecurityBody}
    >
      <ul className="flex flex-col gap-3 text-sm">
        <li className="flex items-center justify-between">
          <span>{t.securityChangePassword}</span>
          <ComingSoon label={t.comingSoon} />
        </li>
        <li className="flex items-center justify-between">
          <span>{t.securityTwoFactor}</span>
          <ComingSoon label={t.comingSoon} />
        </li>
      </ul>
    </Section>
  );
}

// ---------------------------------------------------------------------------
// Danger zone — reset learning (real) + delete account (stub)
// Sign out lives in Hero — it's session ending, not destructive.
// ---------------------------------------------------------------------------

function DangerZone({ t }: { t: Dictionary["me"] }) {
  return (
    <Section
      tone="danger"
      icon={<AlertTriangle className="size-4" />}
      title={t.sectionDanger}
      description={t.sectionDangerBody}
    >
      <div className="flex flex-col divide-y divide-destructive/20">
        <ResetLearningRow t={t} />
        <DeleteAccountRow t={t} />
      </div>
    </Section>
  );
}

function ResetLearningRow({ t }: { t: Dictionary["me"] }) {
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [errMsg, setErrMsg] = useState<string | null>(null);

  async function reset() {
    setSubmitting(true);
    setInfo(null);
    setErrMsg(null);
    try {
      await apiFetch("/api/v1/me/reset-learning", {
        method: "POST",
        body: JSON.stringify({ confirm: true }),
      });
      await queryClient.invalidateQueries();
      setInfo(t.learningResetDone);
      setConfirming(false);
    } catch (e) {
      setErrMsg(e instanceof ApiError ? e.message : t.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col gap-3 py-3 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-foreground">
            {t.learningResetCta}
          </p>
          <p className="text-xs text-muted-foreground">{t.sectionLearningBody}</p>
        </div>
        {!confirming && (
          <Button variant="destructive" onClick={() => setConfirming(true)}>
            <RefreshCw className="size-4" />
            {t.learningResetCta}
          </Button>
        )}
      </div>
      {confirming && (
        <div className="flex flex-col gap-3 rounded-lg border border-destructive/30 bg-destructive/10 p-3">
          <p className="text-sm">{t.learningResetWarn}</p>
          <div className="flex flex-wrap gap-2">
            <Button
              variant="destructive"
              onClick={reset}
              disabled={submitting}
            >
              {submitting ? t.learningResetSubmitting : t.learningResetConfirm}
            </Button>
            <Button
              variant="ghost"
              onClick={() => setConfirming(false)}
              disabled={submitting}
            >
              {t.cancel}
            </Button>
          </div>
        </div>
      )}
      {info && <p className="text-sm text-primary">{info}</p>}
      {errMsg && <p className="text-sm text-destructive">{errMsg}</p>}
    </div>
  );
}

function DeleteAccountRow({ t }: { t: Dictionary["me"] }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
      <div>
        <p className="text-sm font-medium text-foreground">
          {t.deleteAccount}
        </p>
        <p className="text-xs text-muted-foreground">{t.deleteAccountBody}</p>
      </div>
      <div className="flex items-center gap-2">
        <Button variant="destructive" disabled>
          <Trash2 className="size-4" />
          {t.deleteAccount}
        </Button>
        <ComingSoon label={t.comingSoon} />
      </div>
    </div>
  );
}
