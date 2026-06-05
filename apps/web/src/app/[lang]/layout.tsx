import type { Metadata } from "next";
import localFont from "next/font/local";
import { notFound } from "next/navigation";
import { getDictionary, hasLocale, locales } from "@/lib/dictionaries";
import { AuthProvider } from "@/lib/auth-context";
import { QueryProvider } from "@/lib/query-provider";
import { SessionExpiredToast } from "@/components/session-expired-toast";
import "../globals.css";

// Self-hosted variable fonts (OFL). The .woff2 originals come from
// @fontsource-variable/{inter,jetbrains-mono} — vendored into the repo so
// `next build` never reaches out to fonts.gstatic.com. Drops a latent build-
// time dependency on Google Fonts availability without giving up the CSS
// variable wiring that next/font provides.
const inter = localFont({
  src: "../fonts/Inter-Variable.woff2",
  variable: "--font-sans",
  display: "swap",
  weight: "100 900",
});

const mono = localFont({
  src: "../fonts/JetBrainsMono-Variable.woff2",
  variable: "--font-mono",
  display: "swap",
  weight: "100 800",
});

export const metadata: Metadata = {
  title: "DMV Prep — DMV written-exam prep",
  description:
    "DMV written-exam prep. Practice, mistakes, and timed mock exams.",
};

export function generateStaticParams() {
  return locales.map((lang) => ({ lang }));
}

/**
 * Root layout — JUST html/body + fonts + global providers. The visual chrome
 * (SiteHeader/SiteFooter for marketing pages, AppSidebar/MobileTabBar for
 * logged-in surfaces) lives in nested route-group layouts so each surface
 * gets the appropriate frame without leaking into the other.
 */
export default async function RootLayout({
  children,
  params,
}: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const dict = await getDictionary(lang);

  return (
    <html
      lang={lang}
      className={`${inter.variable} ${mono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-background text-foreground">
        <QueryProvider>
          <AuthProvider>
            {children}
            <SessionExpiredToast
              t={{
                title: dict.auth.sessionExpiredTitle,
                body: dict.auth.sessionExpiredBody,
                dismiss: dict.auth.dismiss,
              }}
            />
          </AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
