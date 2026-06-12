/**
 * "Free" (opened, unpaid) status pill. One shared component so the switcher, the
 * settings exam picker, and the subscription catalog all render it identically
 * (they drifted before). Styled to match the plan badge in the sidebar footer
 * (app-sidebar.tsx) — same amber pill — so "Free" reads consistently app-wide.
 */
export function FreeBadge({ label }: { label: string }) {
  return (
    <span className="inline-block shrink-0 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-amber-700">
      {label}
    </span>
  );
}
