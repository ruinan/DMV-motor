"use client";

import { useEffect, useRef, useState } from "react";
import { Bike, Car, Check, ChevronDown, Loader2 } from "lucide-react";
import { useExams } from "@/lib/hooks/use-exams";
import { useMe, examName } from "@/lib/hooks/use-me";
import { useSetExam } from "@/lib/hooks/use-set-exam";
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
}: {
  lang: Locale;
  variant?: "plain" | "chip";
  prefix?: string; // e.g. "Preparing for" (chip only)
  switchLabel: string; // aria-label
}) {
  const exams = useExams(lang);
  const me = useMe();
  const setExam = useSetExam();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState<string | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  const current = me.data?.current_exam ?? null;
  const currentId = current?.id ?? null;

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

  async function pick(id: string) {
    if (id === currentId) {
      setOpen(false);
      return;
    }
    setPending(id);
    try {
      await setExam(id);
    } finally {
      setPending(null);
      setOpen(false);
    }
  }

  // Both variants are accent-tinted so the switcher is an obvious, tappable
  // control (the old "plain" was muted grey text that users missed) and it picks
  // up the per-exam accent color (theme.css [data-exam]).
  const trigger =
    variant === "chip"
      ? "inline-flex items-center gap-1.5 rounded-full border border-primary/40 bg-accent px-3 py-1.5 text-sm font-medium text-primary hover:bg-primary/10"
      : "inline-flex items-center gap-1.5 rounded-md border border-primary/40 bg-accent px-2.5 py-1.5 text-xs font-semibold text-primary hover:bg-primary/10";

  return (
    <div ref={ref} className="relative">
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
        <span className="max-w-[12rem] truncate">{currentLabel}</span>
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
                  <span className="truncate">{exam.name}</span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
