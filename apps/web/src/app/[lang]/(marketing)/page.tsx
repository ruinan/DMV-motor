import Link from "next/link";
import { notFound } from "next/navigation";
import {
  ClipboardList,
  Bookmark,
  Timer,
  ShieldCheck,
  ArrowRight,
} from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { HeroImage } from "@/components/hero-image";
import { getDictionary, hasLocale, type Dictionary } from "@/lib/dictionaries";

export default async function Home({ params }: PageProps<"/[lang]">) {
  const { lang } = await params;
  if (!hasLocale(lang)) notFound();
  const t = await getDictionary(lang);

  return (
    <main className="mx-auto flex w-full max-w-7xl flex-col items-center gap-12 px-6 py-12 sm:gap-16 sm:py-20">
      {/* Hero */}
      <section className="flex w-full max-w-3xl flex-col items-center gap-6 text-center">
        <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary">
          <ShieldCheck className="size-4" />
          {t.home.badge}
        </span>
        <h1 className="text-4xl font-bold tracking-tight text-foreground sm:text-5xl md:text-6xl">
          {t.home.title}
        </h1>
        <p className="max-w-xl text-lg leading-relaxed text-muted-foreground">
          {t.home.subtitle}
        </p>
        <div className="flex flex-wrap items-center justify-center gap-2 text-sm">
          <span className="text-muted-foreground">
            {t.home.examsCoveredLabel}:
          </span>
          <span className="rounded-full border border-border bg-card px-3 py-1 font-medium text-foreground">
            {t.home.examClassC}
          </span>
          <span className="rounded-full border border-border bg-card px-3 py-1 font-medium text-foreground">
            {t.home.examM1}
          </span>
        </div>
        <Link
          href={`/${lang}/practice`}
          className={`${buttonVariants({ size: "lg" })} mt-2 gap-2 px-7 py-6 text-base shadow-md transition-all hover:-translate-y-0.5 hover:shadow-lg`}
        >
          {t.home.ctaPractice}
          <ArrowRight className="size-4" />
        </Link>
      </section>

      {/* Hero image */}
      <HeroImage
        src="/images/hero-coast-rider.png"
        alt={t.home.heroImageAlt}
      />

      {/* Features grid */}
      <section className="grid w-full max-w-5xl grid-cols-1 gap-6 md:grid-cols-3">
        <FeatureCard
          icon={<ClipboardList className="size-6" />}
          tone="primary"
          title={t.home.features.questions.title}
          body={t.home.features.questions.body}
        />
        <FeatureCard
          icon={<Bookmark className="size-6" />}
          tone="secondary"
          title={t.home.features.mistakes.title}
          body={t.home.features.mistakes.body}
        />
        <FeatureCard
          icon={<Timer className="size-6" />}
          tone="accent"
          title={t.home.features.mock.title}
          body={t.home.features.mock.body}
        />
      </section>
    </main>
  );
}

const TONE_CLASSES = {
  primary: "bg-primary/10 text-primary",
  secondary: "bg-secondary text-secondary-foreground",
  accent: "bg-accent text-accent-foreground",
} as const;

function FeatureCard({
  icon,
  tone,
  title,
  body,
}: {
  icon: React.ReactNode;
  tone: keyof typeof TONE_CLASSES;
  title: Dictionary["home"]["features"]["questions"]["title"];
  body: Dictionary["home"]["features"]["questions"]["body"];
}) {
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-border/40 bg-card p-6 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md">
      <div
        className={`flex size-12 items-center justify-center rounded-full ${TONE_CLASSES[tone]}`}
      >
        {icon}
      </div>
      <div>
        <h3 className="mb-2 text-lg font-semibold text-foreground">{title}</h3>
        <p className="text-sm leading-relaxed text-muted-foreground">{body}</p>
      </div>
    </div>
  );
}
