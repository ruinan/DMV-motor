import type { SVGProps } from "react";

/**
 * Lightweight SVG flags for the language toggle. Used instead of emoji because
 * emoji flags render inconsistently on Windows (no national-flag glyphs by
 * default). 60×40 viewBox keeps a 3:2 aspect that scales cleanly.
 */

function FivePointStar({
  cx,
  cy,
  r,
  fill,
}: {
  cx: number;
  cy: number;
  r: number;
  fill: string;
}) {
  const pts: string[] = [];
  for (let i = 0; i < 10; i++) {
    const angle = (Math.PI / 5) * i - Math.PI / 2;
    const radius = i % 2 === 0 ? r : r * 0.4;
    pts.push(
      `${(cx + radius * Math.cos(angle)).toFixed(2)},${(cy + radius * Math.sin(angle)).toFixed(2)}`,
    );
  }
  return <polygon points={pts.join(" ")} fill={fill} />;
}

export function FlagUS(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 60 40" aria-hidden="true" {...props}>
      <rect width="60" height="40" fill="#bd3d44" />
      {[5.7, 11.4, 17.1, 22.8, 28.5, 34.2].map((y, i) => (
        <rect key={i} y={y} width="60" height="2.85" fill="#fff" />
      ))}
      <rect width="24" height="22.8" fill="#192f5d" />
      {[3, 8, 13, 18].flatMap((cy) =>
        [3, 8, 13, 18].map((cx) => (
          <FivePointStar
            key={`${cx}-${cy}`}
            cx={cx}
            cy={cy}
            r={1.4}
            fill="#fff"
          />
        )),
      )}
    </svg>
  );
}

export function FlagCN(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 60 40" aria-hidden="true" {...props}>
      <rect width="60" height="40" fill="#de2910" />
      <FivePointStar cx={10} cy={10} r={4} fill="#ffde00" />
      <FivePointStar cx={20} cy={4} r={1.4} fill="#ffde00" />
      <FivePointStar cx={24} cy={8} r={1.4} fill="#ffde00" />
      <FivePointStar cx={24} cy={14} r={1.4} fill="#ffde00" />
      <FivePointStar cx={20} cy={18} r={1.4} fill="#ffde00" />
    </svg>
  );
}
