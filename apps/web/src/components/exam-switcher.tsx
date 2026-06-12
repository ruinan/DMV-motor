"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Bike, Car, Check, ChevronDown, Loader2, Lock } from "lucide-react";
import { useExams } from "@/lib/hooks/use-exams";
import { useMe, examName } from "@/lib/hooks/use-me";
import { useSetExam } from "@/lib/hooks/use-set-exam";
import { useEntitlements } from "@/lib/hooks/use-entitlements";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { OpenExamDialog } from "@/components/open-exam-dialog";
import type { Locale } from "@/lib/dictionaries";

/**
 * Compact exam switcher (dropdown). Switching re-scopes the whole authed
 * surface (questions, history, mistakes, progress). Renders nothing until the
 * user has a current exam — onboarding (the dashboard picker card) handles the
 * first choice. Two looks:
 *   - "plain": muted text + chevron, for the sidebar / mobile top bar.
 *   - "chip":  bordered pill, for the dashboard header.
 */
export function ExamSwitcher({
  lang,
  variant = "plain",
  prefix,
  switchLabel,
  confirm,
  openLabels,
}: {
  lang: Locale;
  variant?: "plain" | "chip";
  prefix?: string; // e.g. "Preparing for" (chip only)
  switchLabel: string; // aria-label
  confirm: { title: string; body: string; yes: string; cancel: string };
  // Labels for the "open this exam" sheet (free trial vs subscribe) shown when
  // tapping an exam the user hasn't opened yet. {exam} in title is interpolated.
  openLabels: {
    locked: string;
    title: string;
    body: string;
    free: string;
    subscribe: string;
    cancel: string;
  };
}) {
  const router = useRouter();
  const exams = useExams(lang);
  const me = useMe();
  const entitlements = useEntitlements();
  const setExam = useSetExam();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState<string | null>(null);
  // Double-check before switching — re-scopes practice/mock/progress (B37).
  const [confirmId, setConfirmId] = useState<string | null>(null);
  // The exam whose "open" sheet (free trial / subscribe) is showing, if any.
  const [openExamId, setOpenExamId] = useState<string | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  const current = me.data?.current_exam ?? null;
  const currentId = current?.id ?? null;

  // An exam is "opened" (full switch) when it's the current one or the user
  // holds an active pass for it. Everything else is locked: tapping it offers a
  // free trial or a subscription instead of switching straight in.
  const subscribedIds = new Set(
    (entitlements.data ?? []).filter((e) => e.subscribed).map((e) => e.exam_id),
  );
  function isLocked(examId: string) {
    if (examId === currentId) return false;
    // Until we know the user's passes, don't flash a lock on an owned exam.
    if (!entitlements.data) return false;
    return !subscribedIds.has(examId);
  }

  useEffect(() => {
    if (!open) return;
    function onDocClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onEsc(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("keydown", onEsc);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("keydown", onEsc);
    };
  }, [open]);

  // No exam yet → nothing to switch (the onboarding card prompts the choice).
  if (!current) return null;

  const currentLabel = examName(current, lang);
  const options = exams.data ?? [];
  const ExamIcon = current.license_class.startsWith("M") ? Bike : Car;

  function pick(id: string) {
    setOpen(false);
    if (id === currentId) return;
    // Not opened yet → offer free trial / subscribe instead of switching in.
    if (isLocked(id)) {
      setOpenExamId(id);
      return;
    }
    setConfirmId(id); // confirm before re-scoping everything
  }

  async function doSwitch(id: string) {
    setConfirmId(null);
    setPending(id);
    try {
      await setExam(id);
    } finally {
      setPending(null);
    }
  }

  // Free trial: re-scope to the exam (no payment) and drop into practice.
  async function openFree(id: string) {
    setOpenExamId(null);
    setPending(id);
    try {
      await setExam(id);
      router.push(`/${lang}/practice`);
    } finally {
      setPending(null);
    }
  }

  // Subscribe: the settings catalog handles per-exam checkout / redeem.
  function openSubscribe() {
    setOpenExamId(null);
    router.push(`/${lang}/me#subscription`);
  }

  const openExam = options.find((e) => e.id === openExamId) ?? null;

  // Both variants are accent-tinted so the switcher is an obvious, tappable
  // control (the old "plain" was muted grey text that users missed) and it picks
  // up the per-exam accent color (theme.css [data-exam]).
  const trigger =
    variant === "chip"
      ? "inline-flex items-center gap-1.5 rounded-full border border-primary/40 bg-accent px-3 py-1.5 text-sm font-medium text-primary hover:bg-primary/10"
      // "plain" (sidebar / mobile bar): fill the row so it reads as a deliberate
      // control, not an awkward little pill — chevron pinned right.
      : "flex w-full items-center justify-between gap-1.5 rounded-md border border-primary/40 bg-accent px-3 py-2 text-xs font-semibold text-primary hover:bg-primary/10";

  return (
    <div ref={ref} className={variant === "plain" ? "relative w-full" : "relative"}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        disabled={!!pending}
        aria-label={switchLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={`${trigger} disabled:opacity-60`}
      >
        {variant === "chip" && prefix && (
          <span className="text-muted-foreground">{prefix}:</span>
        )}
        <ExamIcon className="size-3.5 shrink-0" aria-hidden />
        <span
          className={
            variant === "plain"
              ? "flex-1 truncate text-left"
              : "max-w-[12rem] truncate"
          }
        >
          {currentLabel}
        </span>
        {pending ? (
          <Loader2 className="size-3.5 animate-spin" />
        ) : (
          <ChevronDown className="size-3.5" aria-hidden />
        )}
      </button>

      {open && options.length > 0 && (
        <ul
          role="listbox"
          className="absolute left-0 z-30 mt-1 max-h-72 min-w-[14rem] overflow-auto rounded-lg border border-border bg-card p-1 shadow-lg"
        >
          {options.map((exam) => {
            const active = exam.id === currentId;
            const locked = isLocked(exam.id);
            return (
              <li key={exam.id} role="option" aria-selected={active}>
                <button
                  type="button"
                  onClick={() => pick(exam.id)}
                  disabled={!!pending}
                  className={`flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition-colors ${
                    active
                      ? "bg-primary/10 font-medium text-primary"
                      : "text-foreground hover:bg-muted"
                  } disabled:opacity-60`}
                >
                  <span className="flex size-4 shrink-0 items-center justify-center">
                    {active ? (
                      <Check className="size-4" />
                    ) : pending === exam.id ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : null}
                  </span>
                  <span className={`truncate ${locked ? "text-muted-foreground" : ""}`}>
                    {exam.name}
                  </span>
                  {locked && (
                    <Lock
                      className="ml-auto size-3.5 shrink-0 text-muted-foreground"
                      aria-label={openLabels.locked}
                    />
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <ConfirmDialog
        open={confirmId !== null}
        title={confirm.title}
        body={confirm.body}
        confirmLabel={confirm.yes}
        cancelLabel={confirm.cancel}
        onConfirm={() => confirmId && doSwitch(confirmId)}
        onCancel={() => setConfirmId(null)}
      />

      <OpenExamDialog
        open={openExam !== null}
        title={openLabels.title.replace("{exam}", openExam?.name ?? "")}
        body={openLabels.body}
        freeLabel={openLabels.free}
        subscribeLabel={openLabels.subscribe}
        cancelLabel={openLabels.cancel}
        onFree={() => openExam && openFree(openExam.id)}
        onSubscribe={openSubscribe}
        onCancel={() => setOpenExamId(null)}
      />
    </div>
  );
}
