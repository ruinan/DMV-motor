import { notFound, redirect } from "next/navigation";
import { hasLocale } from "@/lib/dictionaries";

export default async function ReviewTaskPage({
  params,
}: PageProps<"/[lang]/review/[taskId]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  redirect(`/${lang}/practice`);
}
