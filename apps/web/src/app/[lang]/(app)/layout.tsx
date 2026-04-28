import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { AppSidebar } from "@/components/app-sidebar";
import { MobileTabBar } from "@/components/mobile-tab-bar";
import { MobileAppBar } from "@/components/mobile-app-bar";
import { RequireAuth } from "@/components/require-auth";

/**
 * App-shell layout for logged-in surfaces (dashboard, account, mistakes,
 * etc.). Provides the desktop sidebar nav + mobile top bar + bottom tab bar.
 * Children render in the central content area, padded to account for the
 * fixed nav elements.
 */
export default async function AppLayout({
  children,
  params,
}: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <div className="min-h-screen bg-background">
      <AppSidebar t={t} lang={lang} />
      <MobileAppBar t={t} lang={lang} />
      {/* Reserve space for the fixed sidebar (md+) and bottom tab bar (mobile) */}
      <main className="md:pl-64">
        <div className="mx-auto w-full max-w-7xl px-4 pb-24 pt-6 md:px-8 md:pb-12 md:pt-8">
          <RequireAuth lang={lang}>{children}</RequireAuth>
        </div>
      </main>
      <MobileTabBar t={t} lang={lang} />
    </div>
  );
}
