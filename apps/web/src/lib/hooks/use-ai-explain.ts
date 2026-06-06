"use client";

import { useEffect, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api-client";

/**
 * AI explanation thread for one question, in one language.
 *
 * Layer 0 is the base "why was I wrong" explanation (server DB-cached). Layers
 * 1..N are "深入分析" deep-dives the server generates on demand but does NOT
 * persist — the whole thread lives here and in the browser's localStorage
 * (decision memory §35: "clear cache 就没", 减服务器压力). On revisit we hydrate
 * from localStorage and show the history instantly, no server round-trip.
 */
export type AiLayer = {
  depth: number;
  text: string;
  cached: boolean;
  /** Deep-dive direction the user tapped (example/mnemonic/distractors/rule). */
  aspect?: string;
};

export type AiExplainState = {
  status: "idle" | "loading" | "error";
  layers: AiLayer[];
  /** Remaining deep-dives for this question; null until the first response. */
  depthRemaining: number | null;
  errorCode?: string;
  errorMessage?: string;
};

export type AiExplainIdentity = {
  questionId: string;
  variantId?: string;
  selectedChoiceKey?: string;
  language: string;
};

type AiExplainResponse = {
  explanation: string;
  cached: boolean;
  model: string;
  language: string;
  depth: number;
  depth_remaining: number;
};

type StoredThread = { layers: AiLayer[]; depthRemaining: number | null };

const STORAGE_PREFIX = "ai-explain:v1:";

function storageKey(questionId: string, language: string): string {
  return `${STORAGE_PREFIX}${questionId}:${language}`;
}

function readThread(key: string): StoredThread | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredThread;
    if (!Array.isArray(parsed.layers)) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeThread(key: string, thread: StoredThread): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, JSON.stringify(thread));
  } catch {
    // Quota / privacy mode — degrade gracefully: the thread just won't persist.
  }
}

const EMPTY: AiExplainState = { status: "idle", layers: [], depthRemaining: null };

/**
 * Remove every saved AI-explanation thread from localStorage. Threads are keyed
 * by question (not learning cycle), so a learning reset alone leaves stale
 * deep-dive layers behind — the user then "starts fresh" but still sees a pile of
 * old 深入分析 layers. Call this on reset so the browser side is truly cleared.
 * Throws if localStorage can't be enumerated/written so the caller can retry.
 */
export function clearAllAiThreads(): void {
  if (typeof window === "undefined") return;
  const keys: string[] = [];
  for (let i = 0; i < window.localStorage.length; i++) {
    const k = window.localStorage.key(i);
    if (k && k.startsWith(STORAGE_PREFIX)) keys.push(k);
  }
  for (const k of keys) window.localStorage.removeItem(k);
}

export function useAiExplain(identity: AiExplainIdentity) {
  const { questionId, variantId, selectedChoiceKey, language } = identity;
  const key = storageKey(questionId, language);
  const [state, setState] = useState<AiExplainState>(EMPTY);

  // Thinking-time cooldown: the backend RATE_LIMITED error carries retry-after
  // seconds ("…try again in Ns"). We remember when it ends (epoch ms) so the UI
  // can disable the buttons + count down, and — crucially — we auto-retry the
  // SAME request when it elapses. The user clicked once to wait for THAT answer;
  // making them click again (and re-enter the cooldown) is wrong.
  const [cooldownUntil, setCooldownUntil] = useState(0);
  const retryTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Hydrate the saved thread when the question/language changes. Showing the
  // history straight from localStorage avoids a server hit on revisit.
  useEffect(() => {
    const hydrate = () => {
      const saved = readThread(key);
      setState(
        saved
          ? { status: "idle", layers: saved.layers, depthRemaining: saved.depthRemaining }
          : EMPTY,
      );
      setCooldownUntil(0);
    };
    hydrate();
  }, [key]);

  // Cancel a scheduled auto-retry when the question/language changes or on unmount.
  useEffect(() => {
    return () => {
      if (retryTimer.current) {
        clearTimeout(retryTimer.current);
        retryTimer.current = null;
      }
    };
  }, [key]);

  async function call(depth: number, aspect?: string) {
    // Feed the thread so far back so deep-dive layers are progressive and don't
    // repeat (the server truncates). Only for deep dives (depth > 0).
    const priorContext =
      depth > 0 ? state.layers.map((l) => l.text).join("\n\n") : "";
    // A fresh request supersedes any scheduled auto-retry.
    if (retryTimer.current) {
      clearTimeout(retryTimer.current);
      retryTimer.current = null;
    }
    setState((s) => ({ ...s, status: "loading", errorCode: undefined, errorMessage: undefined }));
    try {
      const res = await apiFetch<AiExplainResponse>("/api/v1/ai/explain", {
        method: "POST",
        body: JSON.stringify({
          question_id: questionId,
          variant_id: variantId,
          selected_choice_key: selectedChoiceKey,
          language,
          depth,
          ...(aspect ? { aspect } : {}),
          ...(priorContext ? { prior_context: priorContext } : {}),
        }),
      });
      setState((s) => {
        const layers = [
          ...s.layers,
          { depth: res.depth, text: res.explanation, cached: res.cached, aspect },
        ];
        writeThread(key, { layers, depthRemaining: res.depth_remaining });
        return { status: "idle", layers, depthRemaining: res.depth_remaining };
      });
      setCooldownUntil(0);
    } catch (err) {
      const code = err instanceof ApiError ? err.code : undefined;
      const message = err instanceof ApiError ? err.message : "Network error";
      // Keep the layers we already have — only the new request failed.
      setState((s) => ({ ...s, status: "error", errorCode: code, errorMessage: message }));
      // Thinking-time cooldown ("…try again in Ns"): count it down and auto-retry
      // this exact request when it elapses, so the user gets the answer they
      // waited for without clicking again. Cap at 120s as a sanity bound.
      const m = code === "RATE_LIMITED" ? message.match(/in\s+(\d+)\s*s/) : null;
      if (m) {
        const secs = Math.min(parseInt(m[1], 10), 120);
        setCooldownUntil(Date.now() + secs * 1000);
        retryTimer.current = setTimeout(() => {
          retryTimer.current = null;
          void call(depth, aspect);
        }, secs * 1000);
      }
    }
  }

  /** Reveal the base explanation (depth 0). No-op if we already have it. */
  function explain() {
    if (state.layers.some((l) => l.depth === 0)) return;
    void call(0);
  }

  /** Request a deeper layer in a chosen direction ("深入分析"). */
  function deepen(aspect: string) {
    void call(state.layers.length, aspect);
  }

  return { state, explain, deepen, cooldownUntil };
}
