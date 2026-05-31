"use client";

import { Loader2, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAiExplain } from "@/lib/hooks/use-ai-explain";

/**
 * The strings the block needs. Each feature dictionary slice (practice / mock /
 * mistakes) already carries these keys, so a slice can be passed structurally.
 */
export type AiExplainLabels = {
  aiExplainButton: string;
  aiExplainLoading: string;
  aiExplainHeading: string;
  aiExplainCached: string;
  aiExplainError: string;
  aiExplainCooldown: string;
  aiExplainUnavailable: string;
  aiExplainAuthRequired: string;
  aiExplainDeepen: string;
  aiExplainDepthReached: string;
};

type Props = {
  questionId: string;
  variantId?: string;
  selectedChoiceKey?: string;
  language: string;
  t: AiExplainLabels;
  isLoggedIn: boolean;
};

/**
 * One self-contained AI-explanation surface (enhance1, unifies the four
 * previously-duplicated blocks). Owns its own thread via {@link useAiExplain}:
 *
 *  - empty → a click-to-reveal "Why was I wrong?" button;
 *  - once revealed → the explanation, plus a "深入分析 / Go deeper" button that
 *    appends a deeper layer (history accumulates and persists in localStorage,
 *    so revisiting shows it instantly without a server call);
 *  - when the per-question depth cap is hit → a "deepest reached" note.
 *
 * Mount it with {@code key={questionId}} so moving to a new question starts a
 * fresh thread.
 */
export function AiExplainBlock({
  questionId,
  variantId,
  selectedChoiceKey,
  language,
  t,
  isLoggedIn,
}: Props) {
  const { state, explain, deepen } = useAiExplain({
    questionId,
    variantId,
    selectedChoiceKey,
    language,
  });
  const loading = state.status === "loading";

  function errorMessage(): string {
    if (state.errorCode === "RATE_LIMITED") return t.aiExplainCooldown;
    if (state.errorCode === "AI_UNAVAILABLE") return t.aiExplainUnavailable;
    return t.aiExplainError;
  }

  // Nothing revealed yet → click-to-reveal button (keeps LLM cost opt-in).
  if (state.layers.length === 0) {
    return (
      <div className="mt-3 border-t border-border/60 pt-3">
        {state.status === "error" && (
          <p className="mb-2 text-sm text-muted-foreground">{errorMessage()}</p>
        )}
        <Button
          variant="outline"
          size="sm"
          onClick={explain}
          disabled={!isLoggedIn || loading}
          className="gap-1.5"
        >
          {loading ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Sparkles className="size-4" />
          )}
          {loading ? t.aiExplainLoading : t.aiExplainButton}
        </Button>
        {!isLoggedIn && (
          <p className="mt-2 text-xs text-muted-foreground">{t.aiExplainAuthRequired}</p>
        )}
      </div>
    );
  }

  const capReached = state.depthRemaining !== null && state.depthRemaining <= 0;

  return (
    <div className="mt-3 space-y-3 border-t border-border/60 pt-3">
      <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-primary">
        <Sparkles className="size-3.5" />
        {t.aiExplainHeading}
      </p>

      {state.layers.map((layer, i) => (
        <div key={i} className={i > 0 ? "border-l-2 border-primary/20 pl-3" : undefined}>
          <p className="text-sm leading-relaxed text-foreground">{layer.text}</p>
          {i === 0 && layer.cached && (
            <span className="text-xs text-muted-foreground">{t.aiExplainCached}</span>
          )}
        </div>
      ))}

      {state.status === "error" && (
        <p className="text-sm text-muted-foreground">{errorMessage()}</p>
      )}

      {capReached ? (
        <p className="text-xs text-muted-foreground">{t.aiExplainDepthReached}</p>
      ) : (
        <Button
          variant="ghost"
          size="sm"
          onClick={deepen}
          disabled={loading}
          className="gap-1.5 text-primary"
        >
          {loading ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Sparkles className="size-4" />
          )}
          {loading ? t.aiExplainLoading : t.aiExplainDeepen}
        </Button>
      )}
    </div>
  );
}
