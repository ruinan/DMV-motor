import Link from "next/link";
import { notFound } from "next/navigation";
import { ClipboardList, Bookmark, Timer, ShieldCheck } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { HeroImage } from "@/components/hero-image";
import { getDictionary, hasLocale, type Dictionary } from "@/lib/dictionaries";

export default async function Home({ params }: PageProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <main className="mx-auto flex w-full max-w-7xl flex-col items-center gap-12 px-6 py-12 sm:py-16">
      {/* Hero */}
      <section className="flex w-full max-w-3xl flex-col items-center gap-6 text-center">
        <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary">
          <ShieldCheck className="size-4" />
          {t.home.badge}
        </span>
        <h1 className="text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
          {t.home.title}
        </h1>
        <p className="max-w-xl text-lg text-muted-foreground">
          {t.home.subtitle}
        </p>
        <Link
          href={`/${lang}/practice`}
          className={`${buttonVariants({ size: "lg" })} px-6 py-3 text-base shadow-sm transition-transform hover:-translate-y-0.5`}
        >
          {t.home.ctaPractice}
        </Link>
      </section>

      <HeroImage src="/images/hero-coast-rider.png" alt={t.home.heroImageAlt} />

      {/* Features grid */}
      <section className="grid w-full max-w-5xl grid-cols-1 gap-6 md:grid-cols-3">
        <FeatureCard
          icon={<ClipboardList className="size-6" />}
          title={t.home.features.questions.title}
          body={t.home.features.questions.body}
        />
        <FeatureCard
          icon={<Bookmark className="size-6" />}
          title={t.home.features.mistakes.title}
          body={t.home.features.mistakes.body}
        />
        <FeatureCard
          icon={<Timer className="size-6" />}
          title={t.home.features.mock.title}
          body={t.home.features.mock.body}
        />
      </section>
    </main>
  );
}

function FeatureCard({
  icon,
  title,
  body,
}: {
  icon: React.ReactNode;
  title: Dictionary["home"]["features"]["questions"]["title"];
  body: Dictionary["home"]["features"]["questions"]["body"];
}) {
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-border/30 bg-card p-6 shadow-sm transition-colors hover:border-primary/30">
      <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
        {icon}
      </div>
      <div>
        <h3 className="mb-1 text-lg font-semibold text-foreground">{title}</h3>
        <p className="text-sm text-muted-foreground">{body}</p>
      </div>
    </div>
  );
}
