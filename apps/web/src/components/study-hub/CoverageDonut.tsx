"use client";

/**
 * Knowledge Coverage donut for the Study Hub hero. Hand-rolled SVG, no
 * charting lib — just stroke-dasharray to make a ring.
 */
type Props = {
  mastered: number;
  total: number;
  label: string;
  sublabel?: string;
  size?: number;
};

export function CoverageDonut({ mastered, total, label, sublabel, size = 160 }: Props) {
  const safeTotal = Math.max(total, 1);
  const pct = mastered / safeTotal;
  const radius = (size - 16) / 2;
  const circumference = 2 * Math.PI * radius;
  const dashLen = pct * circumference;
  const center = size / 2;

  return (
    <div className="flex flex-col items-center gap-2">
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        role="img"
        aria-label={`${label}: ${mastered} of ${total} mastered`}
      >
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="10"
          className="text-muted/40"
        />
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={`${dashLen} ${circumference}`}
          transform={`rotate(-90 ${center} ${center})`}
          className="text-primary transition-all"
        />
        <text
          x={center}
          y={center - 4}
          textAnchor="middle"
          className="fill-foreground text-3xl font-bold"
        >
          {mastered}
        </text>
        <text
          x={center}
          y={center + 18}
          textAnchor="middle"
          className="fill-muted-foreground text-xs"
        >
          / {total}
        </text>
      </svg>
      <p className="text-center text-sm font-semibold text-foreground">{label}</p>
      {sublabel && (
        <p className="text-center text-xs text-muted-foreground">{sublabel}</p>
      )}
    </div>
  );
}
