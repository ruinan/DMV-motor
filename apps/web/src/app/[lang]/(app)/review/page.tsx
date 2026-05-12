import { notFound, redirect } from "next/navigation";
import { hasLocale } from "@/lib/dictionaries";

export default async function ReviewPage({
  params,
}: PageProps<"/[lang]/review">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  redirect(`/${lang}/practice`);
}
