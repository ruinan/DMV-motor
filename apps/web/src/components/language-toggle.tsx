"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { FlagCN, FlagUS } from "@/components/icons/flags";
import { type Locale } from "@/lib/dictionaries";

type Props = {
  currentLang: Locale;
  ariaLabel: string;
};

export function LanguageToggle({ currentLang, ariaLabel }: Props) {
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
  const otherHref = segments.join("/") || `/${otherLang}`;

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
