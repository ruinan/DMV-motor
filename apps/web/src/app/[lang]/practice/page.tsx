import { notFound } from "next/navigation";
import { getDictionary, hasLocale } from "@/lib/dictionaries";
import { PracticeShell } from "./PracticeShell";

export default async function PracticePage({
  params,
}: PageProps<"/[lang]/practice">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);
  // PracticeShell branches on client-side auth state to render either the
  // marketing-style full-screen card (anonymous) or the dashboard chrome
  // with sidebar (signed in) — same URL, different layout.
  return <PracticeShell t={t} lang={lang} />;
}
