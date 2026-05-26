"use client";

/**
 * Knowledge Coverage donut for the Study Hub hero. Hand-rolled SVG, no
 * charting lib — just stroke-dasharray to make a ring.
 */
type Props = {
  mastered: number;
  covered: number;
  total: number;
  label: string;
  masteredLabel: string;
  coveredLabel: string;
  size?: number;
};

/**
 * Two-tone ring: outer "touched" arc (any attempts) + inner "mastered" arc
 * (full mastery threshold met). One ride through practice moves the touched
 * arc immediately so the user sees something change; mastered moves slower
 * because it requires the 4-of-last-4 + ≥80% gates.
 */
export function CoverageDonut({
  mastered,
  covered,
  total,
  label,
  masteredLabel,
  coveredLabel,
  size = 160,
}: Props) {
  const safeTotal = Math.max(total, 1);
  const masteredPct = mastered / safeTotal;
  const coveredPct = Math.min(covered, total) / safeTotal;

  const radius = (size - 16) / 2;
  const circumference = 2 * Math.PI * radius;
  const masteredDash = masteredPct * circumference;
  const coveredDash = coveredPct * circumference;
  const center = size / 2;

  return (
    <div className="flex flex-col items-center gap-2">
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        role="img"
        aria-label={`${label}: ${mastered} of ${total} mastered, ${covered} touched`}
      >
        {/* background ring */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="10"
          className="text-muted/30"
        />
        {/* outer (touched) tint — laid down first so mastered overlays it */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={`${coveredDash} ${circumference}`}
          transform={`rotate(-90 ${center} ${center})`}
          className="text-primary/30 transition-all"
        />
        {/* mastered (full saturation) */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={`${masteredDash} ${circumference}`}
          transform={`rotate(-90 ${center} ${center})`}
          className="text-primary transition-all"
        />
        <text
          x={center}
          y={center - 4}
          textAnchor="middle"
          className="fill-foreground text-3xl font-bold"
        >
          {covered}
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
      <p className="text-center text-xs text-muted-foreground">
        <span className="font-medium text-primary">{covered}</span> {coveredLabel}
        {" · "}
        <span className="font-medium text-foreground">{mastered}</span>{" "}
        {masteredLabel}
      </p>
    </div>
  );
}
