"use client";

import Link from "next/link";
import Image from "next/image";
import { useEffect, useState, type CSSProperties, type ReactNode } from "react";
import {
  ClipboardList,
  Bookmark,
  Timer,
  ShieldCheck,
  ArrowRight,
  Car,
  Bike,
} from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Slide = {
  src: string;
  /** Dominant accent + a faint page tint, so the whole index recolors per image. */
  primary: string;
  accent: string;
  background: string;
};

// Each slide carries the palette the index recolors to. Car → highway-sign blue,
// motorcycle → amber (the two exams' colors). Add slides as art arrives.
const SLIDES: Slide[] = [
  { src: "/hero/road-1.png", primary: "#1b5e9b", accent: "#e2eaf3", background: "#f5f8ff" },
  { src: "/images/hero-coast-rider.png", primary: "#b45309", accent: "#fdebd2", background: "#fff8f0" },
];

/**
 * Marketing landing. Client so the hero image carousel can drive a per-image
 * theme: the accent (badge / CTA / feature icons) AND a faint page background
 * shift to match the active photo, crossfading. The header + footer live in the
 * marketing layout and stay a neutral dark — independent of the blue/amber accent
 * so they never clash with whichever slide is showing.
 */
export function MarketingHome({
  t,
  lang,
}: {
  t: Dictionary["home"];
  lang: Locale;
}) {
  const [i, setI] = useState(0);

  useEffect(() => {
    if (SLIDES.length <= 1) return;
    const id = setInterval(() => setI((x) => (x + 1) % SLIDES.length), 6000);
    return () => clearInterval(id);
  }, []);

  const active = SLIDES[i] ?? SLIDES[0];
  const themeStyle = {
    "--primary": active.primary,
    "--accent": active.accent,
    "--background": active.background,
  } as CSSProperties;

  return (
    <div
      style={themeStyle}
      className="flex min-h-full flex-col items-center gap-12 bg-background px-6 py-12 transition-colors duration-1000 sm:gap-16 sm:py-20"
    >
      {/* Hero text */}
      <section className="flex w-full max-w-3xl flex-col items-center gap-6 text-center">
        <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary transition-colors duration-1000">
          <ShieldCheck className="size-4" />
          {t.badge}
        </span>
        <h1 className="text-4xl font-bold tracking-tight text-foreground sm:text-5xl md:text-6xl">
          {t.title}
        </h1>
        <p className="max-w-xl text-lg leading-relaxed text-muted-foreground">
          {t.subtitle}
        </p>
        <div className="flex flex-col items-center gap-3">
          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            {t.examsCoveredLabel}
          </span>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <span className="inline-flex items-center gap-2 rounded-full border-2 border-primary/40 bg-primary/5 px-4 py-2 text-base font-semibold text-foreground transition-colors duration-1000">
              <Car className="size-5 text-primary" />
              {t.examClassC}
            </span>
            <span className="inline-flex items-center gap-2 rounded-full border-2 border-primary/40 bg-primary/5 px-4 py-2 text-base font-semibold text-foreground transition-colors duration-1000">
              <Bike className="size-5 text-primary" />
              {t.examM1}
            </span>
          </div>
        </div>
        <p className="max-w-md text-xs text-muted-foreground">{t.freeTierNote}</p>
        <Link
          href={`/${lang}/practice`}
          className={`${buttonVariants({ size: "lg" })} mt-2 gap-2 px-7 py-6 text-base shadow-md transition-all duration-1000 hover:-translate-y-0.5 hover:shadow-lg`}
        >
          {t.ctaPractice}
          <ArrowRight className="size-4" />
        </Link>
      </section>

      {/* Crossfading hero carousel */}
      <div className="relative h-64 w-full max-w-5xl overflow-hidden rounded-xl bg-gradient-to-br from-primary/20 via-primary/5 to-accent shadow-sm transition-colors duration-1000 md:h-96">
        {SLIDES.map((s, idx) => (
          <Image
            key={s.src}
            src={s.src}
            alt={t.heroImageAlt}
            fill
            priority={idx === 0}
            sizes="(max-width: 1280px) 100vw, 1280px"
            className={`object-cover transition-opacity duration-1000 ease-in-out ${
              idx === i ? "opacity-100" : "opacity-0"
            }`}
          />
        ))}
        {SLIDES.length > 1 && (
          <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1.5">
            {SLIDES.map((s, idx) => (
              <button
                key={s.src}
                type="button"
                aria-label={`Slide ${idx + 1}`}
                onClick={() => setI(idx)}
                className={`size-2 rounded-full transition-all ${
                  idx === i ? "w-5 bg-white" : "bg-white/50 hover:bg-white/80"
                }`}
              />
            ))}
          </div>
        )}
      </div>

      {/* Features grid */}
      <section className="grid w-full max-w-5xl grid-cols-1 gap-6 md:grid-cols-3">
        <FeatureCard
          icon={<ClipboardList className="size-6" />}
          title={t.features.questions.title}
          body={t.features.questions.body}
        />
        <FeatureCard
          icon={<Bookmark className="size-6" />}
          title={t.features.mistakes.title}
          body={t.features.mistakes.body}
        />
        <FeatureCard
          icon={<Timer className="size-6" />}
          title={t.features.mock.title}
          body={t.features.mock.body}
        />
      </section>
    </div>
  );
}

function FeatureCard({
  icon,
  title,
  body,
}: {
  icon: ReactNode;
  title: string;
  body: string;
}) {
  return (
    <div className="flex flex-col items-center gap-4 rounded-xl border border-border/40 bg-card p-6 text-center shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md">
      {/* All feature icons use the accent so they recolor with the hero carousel. */}
      <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary transition-colors duration-1000">
        {icon}
      </div>
      <div>
        <h3 className="mb-2 text-lg font-semibold text-foreground">{title}</h3>
        <p className="text-sm leading-relaxed text-muted-foreground">{body}</p>
      </div>
    </div>
  );
}
