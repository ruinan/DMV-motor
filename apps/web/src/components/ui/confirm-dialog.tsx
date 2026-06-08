"use client";

import { useEffect } from "react";
import { createPortal } from "react-dom";
import { Button } from "@/components/ui/button";

type Props = {
  open: boolean;
  title: string;
  body: string;
  confirmLabel: string;
  cancelLabel: string;
  /** Visual weight of the confirm button. Destructive for irreversible acts. */
  variant?: "default" | "destructive";
  /** Disables the confirm button + shows a busy label while an async action runs. */
  busy?: boolean;
  busyLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Shared modal confirmation dialog. Replaces ad-hoc window.confirm() calls and
 * the per-feature inline dialogs that had drifted apart (practice exit, mock
 * exit, etc). Click-outside + Escape both cancel; the confirm button carries
 * the destructive tint when the action can't be undone.
 */
export function ConfirmDialog({
  open,
  title,
  body,
  confirmLabel,
  cancelLabel,
  variant = "default",
  busy = false,
  busyLabel,
  onConfirm,
  onCancel,
}: Props) {
  // Escape closes the dialog. Bound only while open so it doesn't swallow
  // Escape elsewhere.
  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape" && !busy) onCancel();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, busy, onCancel]);

  if (!open) return null;

  // Portal to <body> so the fixed overlay can't be trapped inside an ancestor
  // stacking context (e.g. the sidebar that hosts the exam switcher) and get
  // overlapped by page content like the locked-coverage card.
  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={busy ? undefined : onCancel}
    >
      <div
        className="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h3
          id="confirm-dialog-title"
          className="text-lg font-semibold text-foreground"
        >
          {title}
        </h3>
        <p className="mt-2 text-sm text-muted-foreground">{body}</p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button variant={variant} onClick={onConfirm} disabled={busy}>
            {busy && busyLabel ? busyLabel : confirmLabel}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
