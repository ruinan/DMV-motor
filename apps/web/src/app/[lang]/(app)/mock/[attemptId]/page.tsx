import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { MockExam } from "./MockExam";

export default async function MockAttemptPage({
  params,
}: PageProps<"/[lang]/mock/[attemptId]">) {
  const { lang, attemptId } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <MockExam t={t.mock} lang={lang} attemptId={attemptId} />;
}
