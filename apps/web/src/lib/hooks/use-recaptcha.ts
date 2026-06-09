"use client";

import { useCallback } from "react";

/**
 * reCAPTCHA Enterprise (score-based) integration. Disabled — and a complete
 * no-op — unless NEXT_PUBLIC_RECAPTCHA_SITE_KEY is set, so local dev runs without
 * it (the backend guard is likewise disabled there). When set, `execute(action)`
 * returns a token to attach as the X-Recaptcha-Token header; the backend verifies
 * it for register / login (precheck) and subscription changes.
 */
const SITE_KEY = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY;

type Grecaptcha = {
  enterprise: {
    ready: (cb: () => void) => void;
    execute: (key: string, opts: { action: string }) => Promise<string>;
  };
};

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
  }
}

let scriptPromise: Promise<void> | null = null;

function loadScript(): Promise<void> {
  if (!SITE_KEY || typeof window === "undefined") return Promise.resolve();
  if (window.grecaptcha?.enterprise) return Promise.resolve();
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise<void>((resolve, reject) => {
    const s = document.createElement("script");
    s.src = `https://www.google.com/recaptcha/enterprise.js?render=${SITE_KEY}`;
    s.async = true;
    s.onload = () => resolve();
    s.onerror = () => reject(new Error("recaptcha load failed"));
    document.head.appendChild(s);
  });
  return scriptPromise;
}

export function useRecaptcha() {
  const enabled = !!SITE_KEY;

  /** Returns a token for {@code action}, or null when reCAPTCHA isn't configured. */
  const execute = useCallback(async (action: string): Promise<string | null> => {
    if (!SITE_KEY) return null;
    try {
      await loadScript();
      const g = window.grecaptcha?.enterprise;
      if (!g) return null;
      return await new Promise<string>((resolve, reject) => {
        g.ready(() => {
          g.execute(SITE_KEY, { action }).then(resolve).catch(reject);
        });
      });
    } catch {
      return null; // never block the user on a reCAPTCHA load hiccup
    }
  }, []);

  return { enabled, execute };
}

/** Header object with the reCAPTCHA token for `action`, or {} when disabled. */
export async function recaptchaHeaders(
  execute: (action: string) => Promise<string | null>,
  action: string,
): Promise<Record<string, string>> {
  const token = await execute(action);
  return token ? { "X-Recaptcha-Token": token } : {};
}
