import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { LanguageToggle } from "@/components/language-toggle";

/**
 * Standalone layout for the auth funnel (login, future password reset, etc.).
 * Deliberately skips SiteHeader/SiteFooter — the auth card is the entire
 * surface. Only chrome is a single language toggle pinned top-right.
 */
export default async function AuthLayout({
  children,
  params,
}: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <div className="absolute right-6 top-6 z-10">
        <LanguageToggle
          currentLang={lang}
          ariaLabel={t.site.switchLanguage}
        />
      </div>
      {children}
    </div>
  );
}
