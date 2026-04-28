import { NextResponse, type NextRequest } from "next/server";
import { defaultLocale, locales, type Locale } from "@/lib/dictionaries";

function pickLocale(request: NextRequest): Locale {
  const accept = request.headers.get("accept-language") ?? "";
  const preferred = accept.split(",")[0]?.trim().toLowerCase() ?? "";
  if (preferred.startsWith("zh")) return "zh";
  return defaultLocale;
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasLocale = locales.some(
    (l) => pathname === `/${l}` || pathname.startsWith(`/${l}/`),
  );
  if (hasLocale) return;

  const locale = pickLocale(request);
  request.nextUrl.pathname = `/${locale}${pathname}`;
  return NextResponse.redirect(request.nextUrl);
}

export const config = {
  // Skip Next internals, API routes, and any path that looks like a file
  matcher: ["/((?!_next|api|.*\\..*).*)"],
};
