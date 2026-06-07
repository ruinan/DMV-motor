"use client";

import Image from "next/image";
import { useEffect, useState, type CSSProperties, type ReactNode } from "react";

export type HeroSlide = {
  src: string;
  alt: string;
  /** Dominant accent of this image — the hero recolors to match it. */
  primary: string;
  accent: string;
};

/**
 * Marketing hero: a crossfading image carousel whose accent color shifts to match
 * the active image, so the hero UI (badge, CTA) stays visually unified with the
 * photo. The hero text is passed as {@link children} so it lives inside the
 * color-themed wrapper and recolors with the image. Falls back to the brand
 * gradient behind the images, so a missing/loading asset never looks broken.
 *
 * Pure CSS crossfade (opacity, ease-in-out) — no animation lib. Auto-advances
 * every 6s; a single slide just sits still.
 */
export function HeroBanner({
  slides,
  children,
}: {
  slides: HeroSlide[];
  children: ReactNode;
}) {
  const [i, setI] = useState(0);

  useEffect(() => {
    if (slides.length <= 1) return;
    const id = setInterval(() => setI((x) => (x + 1) % slides.length), 6000);
    return () => clearInterval(id);
  }, [slides.length]);

  const active = slides[i] ?? slides[0];
  const themeStyle = {
    "--primary": active.primary,
    "--accent": active.accent,
    "--ring": active.primary,
  } as CSSProperties;

  return (
    <div
      style={themeStyle}
      className="flex w-full flex-col items-center gap-12 transition-colors duration-1000 sm:gap-16"
    >
      {children}

      <div className="relative h-64 w-full max-w-5xl overflow-hidden rounded-xl bg-gradient-to-br from-primary/20 via-primary/5 to-accent shadow-sm md:h-96">
        {slides.map((s, idx) => (
          <Image
            key={s.src}
            src={s.src}
            alt={s.alt}
            fill
            priority={idx === 0}
            sizes="(max-width: 1280px) 100vw, 1280px"
            className={`object-cover transition-opacity duration-1000 ease-in-out ${
              idx === i ? "opacity-100" : "opacity-0"
            }`}
          />
        ))}

        {slides.length > 1 && (
          <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1.5">
            {slides.map((s, idx) => (
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
    </div>
  );
}
