import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { MockLanding } from "./MockLanding";

export default async function MockPage({
  params,
}: PageProps<"/[lang]/mock">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <MockLanding t={t.mock} lang={lang} />;
}
