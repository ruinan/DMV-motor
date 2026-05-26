"use client";

/**
 * Hand-rolled SVG sparkline for the Study Hub mock-history strip. Picks
 * dimensions and stroke from props; no charting lib (design doc decision #3).
 * Color encodes the latest point only when it dipped below the passing-fail
 * line at 70%; otherwise the line stays in the primary color.
 */
type Props = {
  /** Score percentages in chronological order (oldest → newest). */
  values: number[];
  width?: number;
  height?: number;
  className?: string;
};

const PASS_THRESHOLD = 70;

export function Sparkline({ values, width = 160, height = 40, className }: Props) {
  if (values.length === 0) {
    return (
      <div
        style={{ width, height }}
        className={`flex items-center justify-center text-xs text-muted-foreground ${className ?? ""}`}
        aria-label="no data"
      >
        —
      </div>
    );
  }
  if (values.length === 1) {
    return (
      <div
        style={{ width, height }}
        className={`flex items-center justify-center text-xs font-semibold ${className ?? ""}`}
      >
        {values[0]}%
      </div>
    );
  }

  const min = Math.min(...values, PASS_THRESHOLD - 5);
  const max = Math.max(...values, PASS_THRESHOLD + 5);
  const range = max - min || 1;

  const pad = 4;
  const innerW = width - pad * 2;
  const innerH = height - pad * 2;

  const points = values
    .map((v, i) => {
      const x = pad + (i * innerW) / (values.length - 1);
      const y = pad + innerH - ((v - min) / range) * innerH;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  const latest = values[values.length - 1];
  const isPassing = latest >= PASS_THRESHOLD;

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      role="img"
      aria-label={`Score trend (${values.length} attempts)`}
      className={className}
    >
      {/* Pass-fail threshold reference line (subtle) */}
      {min < PASS_THRESHOLD && max > PASS_THRESHOLD && (
        <line
          x1={pad}
          x2={width - pad}
          y1={pad + innerH - ((PASS_THRESHOLD - min) / range) * innerH}
          y2={pad + innerH - ((PASS_THRESHOLD - min) / range) * innerH}
          stroke="currentColor"
          strokeWidth="1"
          strokeDasharray="2 3"
          className="text-muted-foreground/40"
        />
      )}
      <polyline
        points={points}
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
        strokeLinecap="round"
        className={isPassing ? "text-primary" : "text-destructive"}
      />
      {/* Highlight latest point */}
      <circle
        cx={pad + innerW}
        cy={pad + innerH - ((latest - min) / range) * innerH}
        r="3"
        fill="currentColor"
        className={isPassing ? "text-primary" : "text-destructive"}
      />
    </svg>
  );
}
