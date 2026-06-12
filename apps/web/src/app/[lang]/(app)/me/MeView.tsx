"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  CheckCircle2,
  CloudUpload,
  CreditCard,
  GraduationCap,
  Globe2,
  KeyRound,
  Loader2,
  LogOut,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Ticket,
  Trash2,
  User,
} from "lucide-react";
import { useAuth, hasMfaEnrolled } from "@/lib/auth-context";
import { TotpEnroll } from "@/components/totp-enroll";
import { apiFetch, ApiError } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { ExamPicker } from "@/components/exam-picker";
import { examName, type CurrentExam } from "@/lib/hooks/use-me";
import { useExams } from "@/lib/hooks/use-exams";
import { useEntitlements } from "@/lib/hooks/use-entitlements";
import { useBillingConfig } from "@/lib/hooks/use-billing-config";
import { useBackup } from "@/lib/hooks/use-backup";
import { useRecaptcha } from "@/lib/hooks/use-recaptcha";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { clearAllAiThreads } from "@/lib/hooks/use-ai-explain";
import { ReauthDialog } from "@/components/reauth-dialog";
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
  current_exam: CurrentExam | null;
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
            <ExamSection t={t} lang={lang} data={data} />
            <LanguageSection t={t} lang={lang} data={data} />
          </Group>

          <Group label={t.groupBilling}>
            <SubscriptionSection t={t} lang={lang} data={data} />
            <RedeemSection t={t} />
            <BackupSection t={t} data={data} />
            <PaymentSection t={t} />
          </Group>

          <Group label={t.groupSecurity}>
            <MfaSection t={t} />
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
// Exam (state × license the user is preparing for)
// ---------------------------------------------------------------------------

function ExamSection({
  t,
  lang,
  data,
}: {
  t: Dictionary["me"];
  lang: Locale;
  data: MeResponse;
}) {
  const current = data.current_exam
    ? examName(data.current_exam, lang)
    : t.examNotSet;

  return (
    <Section
      id="exam"
      icon={<GraduationCap className="size-4" />}
      title={t.sectionExam}
      description={t.sectionExamBody}
    >
      <p className="mb-3 text-sm text-muted-foreground">
        {t.examCurrent}:{" "}
        <span className="font-medium text-foreground">{current}</span>
      </p>
      <ExamPicker
        lang={lang}
        labels={{
          loading: t.loading,
          errorGeneric: t.errorGeneric,
          empty: t.examPickerEmpty,
          openLabels: {
            locked: t.examLocked,
            freeBadge: t.examFree,
            title: t.examOpenTitle,
            body: t.examOpenBody,
            free: t.examOpenFree,
            subscribe: t.examOpenSubscribe,
            cancel: t.cancel,
          },
        }}
      />
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
  lang,
  data,
}: {
  t: Dictionary["me"];
  lang: Locale;
  data: MeResponse;
}) {
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
      {/* Current exam's access at a glance — mock quota + expiry the catalog
          badges don't surface. data.access is scoped to the current exam (V32). */}
      <dl className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Field
          label={t.accessState}
          value={
            (t[`state_${data.access.state}` as keyof typeof t] as
              | string
              | undefined) ?? data.access.state
          }
        />
        <Field label={t.mockRemaining} value={data.access.mock_remaining} />
        <Field
          label={t.expiresAt}
          value={expiresAt ?? <span className="text-muted-foreground">—</span>}
        />
      </dl>

      <div className="mt-5">
        <ExamCatalog t={t} lang={lang} />
      </div>
    </Section>
  );
}

// Per-exam subscription catalog (V32 model): each active exam can be
// subscribed/unsubscribed independently. When Stripe is wired (billing-config
// enabled) the buttons run real hosted Checkout / cancel; otherwise they fall
// back to the dev grant/revoke backdoor in non-prod, or "Coming soon" in prod.
function ExamCatalog({ t, lang }: { t: Dictionary["me"]; lang: Locale }) {
  const exams = useExams(lang);
  const entitlements = useEntitlements();
  const billing = useBillingConfig();
  const { execute: recaptcha } = useRecaptcha();
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState<string | null>(null);
  const [errMsg, setErrMsg] = useState<string | null>(null);
  // A billing action that hit the backend reauth gate, parked until the user
  // re-enters their password — then retried with a fresh token.
  const [pendingReauth, setPendingReauth] = useState<{
    examId: string;
    subscribed: boolean;
  } | null>(null);
  const billingEnabled = billing.data?.enabled ?? false;
  const devEnabled = process.env.NODE_ENV !== "production";
  const canManage = billingEnabled || devEnabled;

  // Coming back from Stripe Checkout (?billing=success) — the fulfillment
  // webhook may have just created the pass; refetch so the catalog flips.
  useEffect(() => {
    if (
      typeof window !== "undefined" &&
      window.location.search.includes("billing=success")
    ) {
      queryClient.invalidateQueries();
    }
  }, [queryClient]);

  const subscribedById = new Map(
    (entitlements.data ?? []).map((e) => [e.exam_id, e.subscribed]),
  );

  async function toggle(examId: string, subscribed: boolean) {
    if (busy || !canManage) return;
    setBusy(examId);
    setErrMsg(null);
    try {
      // Bot check on subscription changes — token is null (no header) when
      // reCAPTCHA isn't configured (dev), so the backend gate no-ops there.
      const token = await recaptcha(subscribed ? "unsubscribe" : "subscribe");
      const headers = token ? { "X-Recaptcha-Token": token } : undefined;
      if (billingEnabled) {
        if (subscribed) {
          await apiFetch(`/api/v1/billing/cancel?exam_id=${examId}`, {
            method: "POST",
            headers,
          });
          await queryClient.invalidateQueries();
        } else {
          // Hand off to Stripe's hosted Checkout — leaves the app entirely.
          const res = await apiFetch<{ url: string }>(
            `/api/v1/billing/checkout-session?exam_id=${examId}`,
            { method: "POST", headers },
          );
          window.location.assign(res.url);
          return;
        }
      } else {
        // Dev fallback (non-prod, no Stripe key): the grant/revoke backdoor.
        const path = subscribed ? "revoke-pass" : "grant-pass";
        await apiFetch(`/api/v1/dev/${path}?exam_id=${examId}`, {
          method: "POST",
          headers,
        });
        await queryClient.invalidateQueries();
      }
    } catch (e) {
      // Backend reauth gate → prompt for the password, then retry this action.
      if (e instanceof ApiError && e.code === "REAUTH_REQUIRED") {
        setPendingReauth({ examId, subscribed });
        return;
      }
      if (
        e instanceof ApiError &&
        (e.code === "RECAPTCHA_FAILED" || e.code === "RECAPTCHA_REQUIRED")
      ) {
        setErrMsg(t.errorRecaptcha);
        return;
      }
      setErrMsg(e instanceof ApiError ? e.message : t.errorGeneric);
    } finally {
      setBusy(null);
    }
  }

  if (exams.isLoading || entitlements.isLoading) {
    return (
      <p className="text-sm text-muted-foreground">{t.loading}</p>
    );
  }
  if (exams.error) {
    return <p className="text-sm text-destructive">{t.errorGeneric}</p>;
  }
  if (!exams.data || exams.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t.catalogEmpty}</p>;
  }

  return (
    <div className="flex flex-col gap-2">
      {exams.data.map((exam) => {
        const subscribed = subscribedById.get(exam.id) ?? false;
        const isBusy = busy === exam.id;
        return (
          <div
            key={exam.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border p-3"
          >
            <div className="min-w-0">
              <p className="text-sm font-medium text-foreground">{exam.name}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {subscribed
                  ? t.catalogSubscribedNote
                  : `${t.catalogPrice} · ${t.catalogFreeNote}`}
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <span
                className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                  subscribed
                    ? "bg-primary/10 text-primary"
                    : "bg-muted text-muted-foreground"
                }`}
              >
                {subscribed ? t.catalogSubscribed : t.catalogFree}
              </span>
              {canManage ? (
                <Button
                  variant={subscribed ? "outline" : "default"}
                  size="sm"
                  onClick={() => toggle(exam.id, subscribed)}
                  disabled={!!busy}
                >
                  {isBusy ? (
                    "…"
                  ) : subscribed ? (
                    t.catalogUnsubscribe
                  ) : (
                    <>
                      {t.catalogSubscribe} ({t.catalogPrice})
                    </>
                  )}
                </Button>
              ) : (
                <ComingSoon label={t.comingSoon} />
              )}
            </div>
          </div>
        );
      })}
      {errMsg && <p className="text-sm text-destructive">{errMsg}</p>}
      {devEnabled && !billingEnabled && (
        <p className="mt-1 text-xs text-muted-foreground">{t.catalogDevNote}</p>
      )}

      <ReauthDialog
        open={pendingReauth !== null}
        labels={{
          title: t.reauthTitle,
          body: t.reauthBody,
          placeholder: t.reauthPlaceholder,
          confirm: t.reauthConfirm,
          confirming: t.reauthConfirming,
          wrong: t.reauthWrong,
          cancel: t.cancel,
        }}
        onSuccess={() => {
          const p = pendingReauth;
          setPendingReauth(null);
          if (p) toggle(p.examId, p.subscribed);
        }}
        onCancel={() => setPendingReauth(null)}
      />
    </div>
  );
}

// ---------------------------------------------------------------------------
// Activation code — redeem a code to unlock an exam (gift / promo / offline)
// ---------------------------------------------------------------------------

function RedeemSection({ t }: { t: Dictionary["me"] }) {
  const queryClient = useQueryClient();
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const value = code.trim();
    if (!value || busy) return;
    setBusy(true);
    setMsg(null);
    try {
      await apiFetch(`/api/v1/access/redeem?code=${encodeURIComponent(value)}`, {
        method: "POST",
      });
      await queryClient.invalidateQueries();
      setCode("");
      setMsg({ ok: true, text: t.redeemSuccess });
    } catch (err) {
      const errCode = err instanceof ApiError ? err.code : "";
      const text =
        errCode === "ALREADY_REDEEMED"
          ? t.redeemAlready
          : errCode === "CODE_EXHAUSTED"
            ? t.redeemExhausted
            : errCode === "CODE_EXPIRED"
              ? t.redeemExpired
              : errCode === "INVALID_CODE"
                ? t.redeemInvalid
                : t.redeemError;
      setMsg({ ok: false, text });
    } finally {
      setBusy(false);
    }
  }

  return (
    <Section
      id="redeem"
      icon={<Ticket className="size-4" />}
      title={t.redeemTitle}
      description={t.redeemBody}
    >
      <form onSubmit={submit} className="flex flex-wrap items-center gap-2">
        <input
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder={t.redeemPlaceholder}
          autoComplete="off"
          className="h-10 min-w-0 flex-1 rounded-lg border border-border bg-background px-3 font-mono text-sm uppercase tracking-wider text-foreground placeholder:font-sans placeholder:normal-case placeholder:tracking-normal focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
        />
        <Button type="submit" disabled={busy || !code.trim()}>
          {busy ? "…" : t.redeemSubmit}
        </Button>
      </form>
      {msg && (
        <p
          className={`mt-3 text-sm ${msg.ok ? "text-success" : "text-destructive"}`}
        >
          {msg.text}
        </p>
      )}
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
// Progress backup — paid restorable snapshots of the current exam's progress
// ---------------------------------------------------------------------------

function BackupSection({
  t,
  data,
}: {
  t: Dictionary["me"];
  data: MeResponse;
}) {
  const queryClient = useQueryClient();
  const backup = useBackup();
  const hasPass = data.access.has_active_pass;
  const [busy, setBusy] = useState<"sync" | "restore" | null>(null);
  const [errMsg, setErrMsg] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [confirmRestore, setConfirmRestore] = useState(false);
  const [pendingReauth, setPendingReauth] = useState(false);

  async function doSync() {
    if (busy || !hasPass) return;
    setBusy("sync");
    setErrMsg(null);
    setInfo(null);
    try {
      await apiFetch("/api/v1/backup/sync", { method: "POST" });
      await queryClient.invalidateQueries({ queryKey: ["backup"] });
      setInfo(t.backupSynced);
    } catch (e) {
      setErrMsg(e instanceof ApiError ? e.message : t.backupError);
    } finally {
      setBusy(null);
    }
  }

  async function doRestore() {
    if (busy) return;
    setBusy("restore");
    setErrMsg(null);
    setInfo(null);
    try {
      const res = await apiFetch<{ restored_mistakes: number }>(
        "/api/v1/backup/restore",
        { method: "POST" },
      );
      // Restore re-applies mistakes into the current cycle — refetch broadly so
      // the dashboard / mistakes / recommendations reflect it.
      await queryClient.invalidateQueries();
      setInfo(t.backupRestored.replace("{n}", String(res.restored_mistakes)));
    } catch (e) {
      if (e instanceof ApiError && e.code === "REAUTH_REQUIRED") {
        setPendingReauth(true);
        return;
      }
      setErrMsg(e instanceof ApiError ? e.message : t.backupError);
    } finally {
      setBusy(null);
    }
  }

  const b = backup.data?.backup ?? null;
  const has = backup.data?.has_backup ?? false;

  return (
    <Section
      id="backup"
      icon={<CloudUpload className="size-4" />}
      title={t.sectionBackup}
      description={t.sectionBackupBody}
    >
      {!hasPass ? (
        <a
          href="#subscription"
          className="text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          {t.backupPaidOnly}
        </a>
      ) : backup.isLoading ? (
        <p className="text-sm text-muted-foreground">{t.loading}</p>
      ) : (
        <>
          {has && b ? (
            <div className="mb-4 rounded-lg border border-border/60 bg-muted/30 p-4">
              <p className="text-sm font-medium text-foreground">
                {t.backupLastSynced.replace(
                  "{time}",
                  new Date(b.updated_at).toLocaleString(),
                )}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {t.backupDetail
                  .replace("{readiness}", String(b.readiness_score))
                  .replace("{sessions}", String(b.practice_total_sessions))
                  .replace("{mistakes}", String(b.active_mistakes_count))}
              </p>
            </div>
          ) : (
            <p className="mb-4 text-sm text-muted-foreground">{t.backupAuto}</p>
          )}

          <div className="flex flex-wrap items-center gap-3">
            <Button onClick={doSync} disabled={busy !== null}>
              {busy === "sync" ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <CloudUpload className="size-4" />
              )}
              {busy === "sync" ? t.backupSaving : t.backupNow}
            </Button>
            {has && (
              <Button
                variant="outline"
                onClick={() => setConfirmRestore(true)}
                disabled={busy !== null}
              >
                {busy === "restore" && <Loader2 className="size-4 animate-spin" />}
                {busy === "restore" ? t.backupRestoring : t.backupRestore}
              </Button>
            )}
          </div>

          {errMsg && <p className="mt-3 text-sm text-destructive">{errMsg}</p>}
          {info && <p className="mt-3 text-sm text-success">{info}</p>}

          <ConfirmDialog
            open={confirmRestore}
            title={t.backupRestoreConfirmTitle}
            body={t.backupRestoreConfirmBody}
            confirmLabel={t.backupRestore}
            cancelLabel={t.cancel}
            onConfirm={() => {
              setConfirmRestore(false);
              doRestore();
            }}
            onCancel={() => setConfirmRestore(false)}
          />
          <ReauthDialog
            open={pendingReauth}
            labels={{
              title: t.reauthTitle,
              body: t.reauthBody,
              placeholder: t.reauthPlaceholder,
              confirm: t.reauthConfirm,
              confirming: t.reauthConfirming,
              wrong: t.reauthWrong,
              cancel: t.cancel,
            }}
            onSuccess={() => {
              setPendingReauth(false);
              doRestore();
            }}
            onCancel={() => setPendingReauth(false)}
          />
        </>
      )}
    </Section>
  );
}

// ---------------------------------------------------------------------------
// Two-factor authentication (TOTP)
// ---------------------------------------------------------------------------

function MfaSection({ t }: { t: Dictionary["me"] }) {
  const { user } = useAuth();
  const [done, setDone] = useState(false);
  const enrolled = hasMfaEnrolled(user) || done;

  return (
    <Section
      id="mfa"
      icon={<ShieldCheck className="size-4" />}
      title={t.sectionMfa}
      description={t.sectionMfaBody}
    >
      {enrolled ? (
        <p className="flex items-center gap-2 text-sm font-medium text-success">
          <CheckCircle2 className="size-4" />
          {t.mfaEnrolled}
        </p>
      ) : (
        <TotpEnroll
          strings={{
            setup: t.mfaSetup,
            scan: t.mfaScan,
            manualKey: t.mfaManualKey,
            codePlaceholder: t.mfaCodePlaceholder,
            verify: t.mfaVerify,
            badCode: t.mfaBadCode,
            recentLogin: t.mfaRecentLogin,
            generic: t.errorGeneric,
          }}
          onEnrolled={() => setDone(true)}
        />
      )}
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
      // Also purge browser-side AI deep-dive threads — they're stored in
      // localStorage keyed by question (not learning cycle), so a reset alone
      // leaves stale 深入分析 layers behind. Retry a few times, then surface an
      // error so the user can retry rather than silently keeping old data.
      let cleared = false;
      for (let attempt = 1; attempt <= 3 && !cleared; attempt++) {
        try {
          clearAllAiThreads();
          cleared = true;
        } catch {
          if (attempt === 3) throw new Error(t.errorGeneric);
          await new Promise((r) => setTimeout(r, 200));
        }
      }
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
