import { notFound } from "next/navigation";
import { hasLocale } from "@/lib/dictionaries";
import { privacyContent } from "@/content/legal";
import { LegalPage } from "@/components/legal-page";

export default async function PrivacyPage({ params }: PageProps<"/[lang]/privacy">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  return <LegalPage doc={privacyContent[lang]} />;
}
