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
 * Deep-dive directions the learner can tap. Click-only (no free text) so the
 * anti-abuse stance holds; each one steers the next layer to a different angle
 * so layers don't repeat. Codes must match the backend (DeepSeekAiExplanation
 * Provider.aspectFocus*). Labels are component-local (tied to the codes) —
 * keyed by language to avoid threading 4 keys through every dictionary slice.
 */
const ASPECTS: { code: string; en: string; zh: string }[] = [
  { code: "example", en: "Example", zh: "举例" },
  { code: "mnemonic", en: "Memory aid", zh: "记忆法" },
  { code: "distractors", en: "Why others wrong", zh: "错项辨析" },
  { code: "rule", en: "The rule", zh: "背后规则" },
];

function aspectLabel(code: string | undefined, language: string): string {
  const a = ASPECTS.find((x) => x.code === code);
  return a ? (language === "zh" ? a.zh : a.en) : "";
}

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
          {layer.aspect && (
            <span className="mb-1 inline-block rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-primary">
              {aspectLabel(layer.aspect, language)}
            </span>
          )}
          <p className="text-sm leading-relaxed text-foreground">{layer.text}</p>
          {i === 0 && layer.cached && (
            <span className="text-xs text-muted-foreground">{t.aiExplainCached}</span>
          )}
        </div>
      ))}

      {state.status === "error" && (
        <p className="text-sm text-muted-foreground">{errorMessage()}</p>
      )}

      {/* Deep-dive: pick a direction. Each is progressive + non-repeating
          (server is fed the thread so far). Hidden once the depth cap is hit. */}
      {capReached ? (
        <p className="text-xs text-muted-foreground">{t.aiExplainDepthReached}</p>
      ) : loading ? (
        <span className="inline-flex items-center gap-1.5 text-sm text-primary">
          <Loader2 className="size-4 animate-spin" />
          {t.aiExplainLoading}
        </span>
      ) : (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-muted-foreground">
            {t.aiExplainDeepen}
          </span>
          {ASPECTS.map((a) => (
            <Button
              key={a.code}
              variant="outline"
              size="sm"
              onClick={() => deepen(a.code)}
              className="h-7 gap-1 px-2.5 text-xs text-primary"
            >
              <Sparkles className="size-3.5" />
              {language === "zh" ? a.zh : a.en}
            </Button>
          ))}
        </div>
      )}
    </div>
  );
}
