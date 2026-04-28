import Link from "next/link";
import { notFound } from "next/navigation";
import { buttonVariants } from "@/components/ui/button";
import { getDictionary, hasLocale } from "@/lib/dictionaries";

export default async function PracticePage({
  params,
}: PageProps<"/[lang]/practice">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <main className="flex flex-1 items-center justify-center px-6 py-24">
      <div className="max-w-xl text-center">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
          {t.practice.placeholderTitle}
        </h1>
        <p className="mt-4 text-muted-foreground">
          {t.practice.placeholderBody}
        </p>
        <Link
          href={`/${lang}`}
          className={`mt-8 ${buttonVariants({ variant: "outline" })}`}
        >
          {t.practice.backHome}
        </Link>
      </div>
    </main>
  );
}
