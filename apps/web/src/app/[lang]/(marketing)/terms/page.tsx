import { notFound } from "next/navigation";
import { hasLocale } from "@/lib/dictionaries";
import { termsContent } from "@/content/legal";
import { LegalPage } from "@/components/legal-page";

export default async function TermsPage({ params }: PageProps<"/[lang]/terms">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  return <LegalPage doc={termsContent[lang]} />;
}
