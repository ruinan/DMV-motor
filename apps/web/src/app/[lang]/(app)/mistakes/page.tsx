import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { MistakesView } from "./MistakesView";

export default async function MistakesPage({
  params,
}: PageProps<"/[lang]/mistakes">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <MistakesView t={t.mistakes} lang={lang} />;
}
