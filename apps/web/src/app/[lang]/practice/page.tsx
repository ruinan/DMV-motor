import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { PracticeFlow } from "./PracticeFlow";

export default async function PracticePage({
  params,
}: PageProps<"/[lang]/practice">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <PracticeFlow t={t.practice} lang={lang} />;
}
