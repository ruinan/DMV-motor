"use client";

import { useEffect, useState } from "react";
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

export function useAiExplain(identity: AiExplainIdentity) {
  const { questionId, variantId, selectedChoiceKey, language } = identity;
  const key = storageKey(questionId, language);
  const [state, setState] = useState<AiExplainState>(EMPTY);

  // Hydrate the saved thread when the question/language changes. Showing the
  // history straight from localStorage avoids a server hit on revisit.
  useEffect(() => {
    const saved = readThread(key);
    setState(
      saved
        ? { status: "idle", layers: saved.layers, depthRemaining: saved.depthRemaining }
        : EMPTY,
    );
  }, [key]);

  async function call(depth: number, aspect?: string) {
    // Feed the thread so far back so deep-dive layers are progressive and don't
    // repeat (the server truncates). Only for deep dives (depth > 0).
    const priorContext =
      depth > 0 ? state.layers.map((l) => l.text).join("\n\n") : "";
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
    } catch (err) {
      // Keep the layers we already have — only the new request failed.
      setState((s) => ({
        ...s,
        status: "error",
        errorCode: err instanceof ApiError ? err.code : undefined,
        errorMessage: err instanceof ApiError ? err.message : "Network error",
      }));
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

  return { state, explain, deepen };
}
