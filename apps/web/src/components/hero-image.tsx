"use client";

import Image from "next/image";
import { useState } from "react";

type Props = {
  src: string;
  alt: string;
};

/**
 * Async hero image with graceful fallback.
 *
 * - The wrapper div ALWAYS shows the brand gradient (acts as both initial
 *   placeholder while the image streams in, and the permanent fallback if
 *   the asset is missing or fails to load).
 * - The <Image> renders on top, fades in on load. If load errors (e.g. the
 *   asset is missing from /public) we mount-out the <Image> so the gradient
 *   stays clean — no broken-image icon.
 * - next/image handles lazy loading + responsive srcset by default;
 *   we keep priority off because this image sits below the hero copy
 *   and shouldn't compete with the LCP text.
 */
export function HeroImage({ src, alt }: Props) {
  const [loaded, setLoaded] = useState(false);
  const [errored, setErrored] = useState(false);

  return (
    <div className="relative h-64 w-full max-w-5xl overflow-hidden rounded-xl bg-gradient-to-br from-primary/20 via-primary/5 to-accent shadow-sm md:h-96">
      {!errored && (
        <Image
          src={src}
          alt={alt}
          fill
          sizes="(max-width: 1280px) 100vw, 1280px"
          className={`object-cover transition-opacity duration-700 ${
            loaded ? "opacity-100" : "opacity-0"
          }`}
          onLoad={() => setLoaded(true)}
          onError={() => setErrored(true)}
        />
      )}
    </div>
  );
}
