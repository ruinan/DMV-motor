import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { ProgressView } from "./ProgressView";

export default async function ProgressPage({
  params,
}: PageProps<"/[lang]/progress">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <ProgressView t={t} lang={lang} />;
}
