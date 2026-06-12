"use client";

import { useEffect } from "react";
import { createPortal } from "react-dom";
import { Sparkles, Lock } from "lucide-react";
import { Button } from "@/components/ui/button";

type Props = {
  open: boolean;
  title: string;
  body: string;
  freeLabel: string;
  subscribeLabel: string;
  cancelLabel: string;
  onFree: () => void;
  onSubscribe: () => void;
  onCancel: () => void;
};

/**
 * "Open this exam" sheet — shown when a learner taps an exam they haven't opened
 * yet in the switcher. Two paths, per the per-exam model: start a free trial
 * (no payment, just re-scopes to that exam) or subscribe for full access.
 *
 * Portals to <body> for the same reason ConfirmDialog does — the switcher lives
 * inside the sidebar's stacking context, so an inline overlay would be trapped.
 */
export function OpenExamDialog({
  open,
  title,
  body,
  freeLabel,
  subscribeLabel,
  cancelLabel,
  onFree,
  onSubscribe,
  onCancel,
}: Props) {
  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onCancel();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onCancel]);

  if (!open) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="open-exam-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-primary">
          <Lock className="size-5" />
        </div>
        <h3
          id="open-exam-dialog-title"
          className="mt-4 text-lg font-semibold text-foreground"
        >
          {title}
        </h3>
        <p className="mt-2 text-sm text-muted-foreground">{body}</p>
        <div className="mt-6 flex flex-col gap-2">
          <Button onClick={onSubscribe}>{subscribeLabel}</Button>
          <Button variant="outline" onClick={onFree}>
            <Sparkles className="size-4" />
            {freeLabel}
          </Button>
          <button
            type="button"
            onClick={onCancel}
            className="mt-1 text-sm font-medium text-muted-foreground underline-offset-4 hover:underline"
          >
            {cancelLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
