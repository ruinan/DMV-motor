"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState, type ComponentType, type SVGProps } from "react";
import { Check, ChevronDown, Globe } from "lucide-react";
import { FlagCN, FlagUS } from "@/components/icons/flags";
import { LANGUAGE_OPTIONS } from "@/lib/locales";
import type { Locale } from "@/lib/dictionaries";

type Props = {
  currentLang: Locale;
  ariaLabel: string;
  /** Align the menu to the trigger's left edge instead of the right. */
  align?: "left" | "right";
};

/**
 * Language selector (dropdown). Replaces the old 2-way flag toggle so the app
 * scales past two languages (see {@link LANGUAGE_OPTIONS}). Each option rebuilds
 * the URL's leading /<lang> segment AND preserves the query string, so switching
 * language on a page whose state lives in the query (e.g. mock review at
 * `?review=1`) doesn't drop that marker.
 *
 * useSearchParams forces client-side rendering up to the nearest Suspense
 * boundary (Next prerendering rule), so the boundary is self-contained here; the
 * fallback renders the same trigger with path-only hrefs until the query is
 * available on the client.
 */
const FLAGS: Partial<Record<Locale, ComponentType<SVGProps<SVGSVGElement>>>> = {
  en: FlagUS,
  zh: FlagCN,
};

export function LanguageSelect(props: Props) {
  return (
    <Suspense fallback={<LanguageSelectInner {...props} query="" />}>
      <WithQuery {...props} />
    </Suspense>
  );
}

function WithQuery(props: Props) {
  const query = useSearchParams().toString();
  return <LanguageSelectInner {...props} query={query} />;
}

function LanguageSelectInner({
  currentLang,
  ariaLabel,
  align = "right",
  query,
}: Props & { query: string }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

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

  // Replace the leading /<currentLang> segment, preserve everything after.
  function hrefFor(code: Locale): string {
    const segments = pathname.split("/");
    if (segments[1] === currentLang) segments[1] = code;
    else segments.splice(1, 0, code);
    const path = segments.join("/") || `/${code}`;
    return query ? `${path}?${query}` : path;
  }

  const current = LANGUAGE_OPTIONS.find((o) => o.code === currentLang);
  const CurrentFlag = FLAGS[currentLang];

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        className="inline-flex items-center gap-1.5 rounded-md border border-border bg-background px-2.5 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
      >
        {CurrentFlag ? (
          <CurrentFlag className="h-4 w-4 rounded-full" />
        ) : (
          <Globe className="size-4" aria-hidden />
        )}
        <span>{current?.nativeName ?? currentLang.toUpperCase()}</span>
        <ChevronDown className="size-3.5" aria-hidden />
      </button>

      {open && (
        <ul
          role="listbox"
          className={`absolute z-50 mt-1 min-w-[10rem] overflow-hidden rounded-lg border border-border bg-card p-1 shadow-lg ${
            align === "left" ? "left-0" : "right-0"
          }`}
        >
          {LANGUAGE_OPTIONS.map((o) => {
            const active = o.code === currentLang;
            const Flag = FLAGS[o.code];
            return (
              <li key={o.code} role="option" aria-selected={active}>
                <Link
                  href={hrefFor(o.code)}
                  onClick={() => setOpen(false)}
                  className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors ${
                    active
                      ? "bg-primary/10 font-medium text-primary"
                      : "text-foreground hover:bg-muted"
                  }`}
                >
                  {Flag ? (
                    <Flag className="h-4 w-4 rounded-full" />
                  ) : (
                    <Globe className="size-4" aria-hidden />
                  )}
                  <span className="flex-1">{o.nativeName}</span>
                  {active && <Check className="size-4 shrink-0" />}
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
