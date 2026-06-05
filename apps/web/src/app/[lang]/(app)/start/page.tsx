import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { OnboardingExamChoice } from "./OnboardingExamChoice";

export default async function StartPage({ params }: PageProps<"/[lang]/start">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <OnboardingExamChoice lang={lang} labels={t.onboarding} />;
}
