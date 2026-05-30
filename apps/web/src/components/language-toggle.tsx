"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { FlagCN, FlagUS } from "@/components/icons/flags";
import { type Locale } from "@/lib/dictionaries";

type Props = {
  currentLang: Locale;
  ariaLabel: string;
};

/**
 * Language switcher. Rebuilds the URL's leading /<lang> segment AND preserves
 * the query string, so switching language on a page whose state lives in the
 * query (e.g. a mock-review page at `?review=1`) doesn't drop that marker —
 * which would otherwise make AppChrome mistake the page for a focus-mode exam
 * and hide the sidebar.
 *
 * useSearchParams forces client-side rendering up to the nearest Suspense
 * boundary (Next prerendering rule), so the boundary is self-contained here:
 * call sites (marketing header, auth layout, app sidebar) stay simple, and the
 * fallback renders an identical, clickable toggle (path-only href) until the
 * query is available on the client.
 */
export function LanguageToggle(props: Props) {
  return (
    <Suspense fallback={<LanguageToggleLink {...props} query="" />}>
      <LanguageToggleInner {...props} />
    </Suspense>
  );
}

function LanguageToggleInner(props: Props) {
  const query = useSearchParams().toString();
  return <LanguageToggleLink {...props} query={query} />;
}

function LanguageToggleLink({
  currentLang,
  ariaLabel,
  query,
}: Props & { query: string }) {
  const pathname = usePathname();
  const otherLang: Locale = currentLang === "en" ? "zh" : "en";

  // Replace the leading /<currentLang> segment, preserve everything after.
  // /en/me   → /zh/me
  // /en      → /zh
  // /        → /zh   (defensive — proxy normally redirects before we get here)
  const segments = pathname.split("/");
  if (segments[1] === currentLang) {
    segments[1] = otherLang;
  } else {
    segments.splice(1, 0, otherLang);
  }
  const path = segments.join("/") || `/${otherLang}`;
  const otherHref = query ? `${path}?${query}` : path;

  const Flag = otherLang === "zh" ? FlagCN : FlagUS;

  return (
    <Link
      href={otherHref}
      aria-label={ariaLabel}
      className="inline-flex h-9 w-9 items-center justify-center overflow-hidden rounded-full border border-border bg-background transition-colors hover:bg-muted"
    >
      <Flag className="h-5 w-5 rounded-full" />
    </Link>
  );
}
