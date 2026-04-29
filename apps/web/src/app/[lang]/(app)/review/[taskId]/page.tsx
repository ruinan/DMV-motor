import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { ReviewTaskRunner } from "./ReviewTaskRunner";

export default async function ReviewTaskPage({
  params,
}: PageProps<"/[lang]/review/[taskId]">) {
  const { lang, taskId } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <ReviewTaskRunner t={t.review} lang={lang} taskId={taskId} />;
}
