"use client";

import { useEffect, useState } from "react";

/**
 * Smooths a loading flag so a spinner is shown for at least {@code minMs} once
 * it appears — avoids a sub-300ms flash on fast responses while still covering
 * slow ones (feedback_loading_indicator: "loading 最少 0.3s").
 *
 * setState only ever runs inside setTimeout callbacks (never synchronously in
 * the effect body), so this stays clear of React 19's set-state-in-effect rule.
 */
export function useMinLoading(active: boolean, minMs = 300): boolean {
  const [shown, setShown] = useState(active);

  useEffect(() => {
    if (active) {
      const show = setTimeout(() => setShown(true), 0);
      return () => clearTimeout(show);
    }
    const hide = setTimeout(() => setShown(false), minMs);
    return () => clearTimeout(hide);
  }, [active, minMs]);

  return shown;
}
