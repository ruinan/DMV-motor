type Props = {
  percent: number | null | undefined;
  label: string;
  /** Optional — shown below the percentage when null/undefined (locked state) */
  lockedLabel?: string;
};

const RADIUS = 45;
const CIRC = 2 * Math.PI * RADIUS;

function ringColor(p: number): string {
  // Tailwind-class color tiers tied to readiness thresholds:
  // < 60 red (high risk) / 60-84 amber / >= 85 green (pass-ready)
  if (p >= 85) return "text-green-500";
  if (p >= 60) return "text-amber-500";
  return "text-red-500";
}

export function ReadinessRing({ percent, label, lockedLabel }: Props) {
  const value = typeof percent === "number" ? Math.max(0, Math.min(100, percent)) : null;
  const offset = value === null ? CIRC : CIRC * (1 - value / 100);
  const colorClass = value === null ? "text-muted-foreground" : ringColor(value);

  return (
    <div className="relative flex h-40 w-40 flex-shrink-0 items-center justify-center">
      <svg viewBox="0 0 100 100" className="h-full w-full -rotate-90">
        <circle
          cx="50"
          cy="50"
          r={RADIUS}
          fill="none"
          stroke="currentColor"
          strokeWidth="8"
          className="text-muted"
        />
        <circle
          cx="50"
          cy="50"
          r={RADIUS}
          fill="none"
          stroke="currentColor"
          strokeWidth="8"
          strokeLinecap="round"
          strokeDasharray={CIRC}
          strokeDashoffset={offset}
          className={`${colorClass} transition-[stroke-dashoffset] duration-700`}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        {value === null ? (
          <span className="px-2 text-center text-xs leading-tight text-muted-foreground">
            {lockedLabel ?? "—"}
          </span>
        ) : (
          <>
            <span className="text-3xl font-semibold tabular-nums text-foreground">
              {Math.round(value)}%
            </span>
            <span className="text-xs text-muted-foreground">{label}</span>
          </>
        )}
      </div>
    </div>
  );
}
