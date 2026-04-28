import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { Dashboard } from "./Dashboard";

export default async function DashboardPage({
  params,
}: PageProps<"/[lang]/dashboard">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  return <Dashboard t={t} lang={lang} />;
}
