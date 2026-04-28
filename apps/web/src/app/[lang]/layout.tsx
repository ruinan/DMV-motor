import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import { notFound } from "next/navigation";
import { hasLocale, locales } from "@/lib/dictionaries";
import { AuthProvider } from "@/lib/auth-context";
import { QueryProvider } from "@/lib/query-provider";
import "../globals.css";

const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin"],
});

const mono = JetBrains_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "DMV Motor — California M1 prep",
  description:
    "California M1 motorcycle written-exam prep. Practice, mistakes, mock exams.",
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

  return (
    <html
      lang={lang}
      className={`${inter.variable} ${mono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-background text-foreground">
        <QueryProvider>
          <AuthProvider>{children}</AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
