import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { ReviewPackView } from "./ReviewPackView";

export default async function ReviewPage({
  params,
}: PageProps<"/[lang]/review">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <ReviewPackView t={t.review} lang={lang} />;
}
