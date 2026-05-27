import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { AppChrome } from "@/components/app-chrome";
import { RequireAuth } from "@/components/require-auth";

/**
 * App-shell layout for logged-in surfaces (dashboard, account, mistakes,
 * etc.). Auth gate lives at this level so the chrome doesn't flash before
 * the redirect; the actual sidebar / nav decision (full chrome vs focus
 * mode for a mid-exam attempt) is made by AppChrome on the client.
 */
export default async function AppLayout({
  children,
  params,
}: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <RequireAuth lang={lang} t={t.auth}>
      <AppChrome t={t} lang={lang}>{children}</AppChrome>
    </RequireAuth>
  );
}
