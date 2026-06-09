import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";

/**
 * Marketing surface layout — wraps the public landing page in the brand
 * chrome (SiteHeader + SiteFooter). /login lives in the (auth) group with
 * its own minimal layout so the auth card isn't sandwiched in marketing
 * navigation.
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
      <SiteFooter t={t.footer} lang={lang} />
    </div>
  );
}
