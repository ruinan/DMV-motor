import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";

/**
 * Marketing surface layout — used by the public landing page and login.
 * Wraps children in the same SiteHeader + SiteFooter chrome so brand
 * navigation feels consistent across the unauthenticated funnel.
 */
export default async function MarketingLayout({
  children,
  params,
}: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader t={t.site} lang={lang} />
      <div className="flex flex-1 flex-col">{children}</div>
      <SiteFooter t={t.footer} />
    </div>
  );
}
