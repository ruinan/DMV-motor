"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  ClipboardList,
  Bookmark,
  Timer,
  User,
  type LucideIcon,
} from "lucide-react";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Tab = {
  href: string;
  label: string;
  icon: LucideIcon;
};

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function MobileTabBar({ t, lang }: Props) {
  const pathname = usePathname();

  // 5 tabs — Review pushed to sidebar/dashboard CTA on mobile to keep
  // bottom bar uncluttered. Mistakes is more frequent + high-stakes.
  const tabs: Tab[] = [
    { href: `/${lang}/dashboard`, label: t.nav.dashboard, icon: LayoutDashboard },
    { href: `/${lang}/practice`, label: t.nav.practice, icon: ClipboardList },
    { href: `/${lang}/mistakes`, label: t.nav.mistakes, icon: Bookmark },
    { href: `/${lang}/mock`, label: t.nav.mockExam, icon: Timer },
    { href: `/${lang}/me`, label: t.nav.account, icon: User },
  ];

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-40 border-t border-border bg-card pb-[env(safe-area-inset-bottom)] md:hidden"
      aria-label="Primary"
    >
      <div className="flex items-center justify-around">
        {tabs.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link
              key={href}
              href={href}
              className={`flex min-h-[64px] min-w-[64px] flex-col items-center justify-center gap-1 px-2 py-2 text-[10px] font-medium transition-colors ${
                active
                  ? "text-primary"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Icon className="size-5" />
              <span>{label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
