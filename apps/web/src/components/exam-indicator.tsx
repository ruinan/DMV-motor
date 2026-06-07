"use client";

import { Bike, Car } from "lucide-react";
import { useMe, examName } from "@/lib/hooks/use-me";
import type { Locale } from "@/lib/dictionaries";

/**
 * Non-interactive "you're studying X" label. The single exam SWITCHER lives in
 * the sidebar (one control, per product decision); every other surface just shows
 * which exam it's scoped to as plain text — study / mock / practice headers use
 * this so the user always knows the active exam without a second dropdown.
 * Renders nothing for anonymous / pre-onboarding (no current exam).
 */
export function ExamIndicator({ lang, prefix }: { lang: Locale; prefix?: string }) {
  const me = useMe();
  const current = me.data?.current_exam;
  if (!current) return null;
  const Icon = current.license_class.startsWith("M") ? Bike : Car;
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-accent px-3 py-1.5 text-sm font-medium text-primary">
      <Icon className="size-3.5 shrink-0" aria-hidden />
      {prefix && <span className="text-muted-foreground">{prefix}:</span>}
      <span className="max-w-[12rem] truncate">{examName(current, lang)}</span>
    </span>
  );
}
