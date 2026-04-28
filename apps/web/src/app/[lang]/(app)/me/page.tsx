import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { MeView } from "./MeView";

export default async function MePage({ params }: PageProps<"/[lang]/me">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <MeView t={t.me} lang={lang} />;
}
