"use client";

import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/api-client";

/**
 * Local state for a single AI-explain interaction. Kept per-component so each
 * feedback card has its own lifecycle (no global cache; the backend already
 * memoises the result on (user, question, language) via the ai_explanations
 * table, so re-clicking the same question on the same locale returns
 * `cached: true` instantly without a second LLM call).
 */
export type AiExplainState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "ok"; text: string; cached: boolean }
  | { kind: "error"; message: string; code?: string };

export type AiExplainInput = {
  question_id: string;
  variant_id?: string;
  selected_choice_key?: string;
  language: string;
};

type AiExplainResponse = {
  explanation: string;
  cached: boolean;
  model: string;
  language: string;
};

export function useAiExplain() {
  const [state, setState] = useState<AiExplainState>({ kind: "idle" });

  async function explain(input: AiExplainInput) {
    setState({ kind: "loading" });
    try {
      const res = await apiFetch<AiExplainResponse>("/api/v1/ai/explain", {
        method: "POST",
        body: JSON.stringify(input),
      });
      setState({ kind: "ok", text: res.explanation, cached: res.cached });
    } catch (err) {
      if (err instanceof ApiError) {
        setState({ kind: "error", message: err.message, code: err.code });
      } else {
        setState({ kind: "error", message: "Network error" });
      }
    }
  }

  function reset() {
    setState({ kind: "idle" });
  }

  return { state, explain, reset };
}
