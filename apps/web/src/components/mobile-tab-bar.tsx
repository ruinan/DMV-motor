"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BookOpen,
  ClipboardList,
  Timer,
  Settings,
  type LucideIcon,
} from "lucide-react";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type Tab = {
  href: string;
  label: string;
  icon: LucideIcon;
  activePaths: string[];
};

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function MobileTabBar({ t, lang }: Props) {
  const pathname = usePathname();

  // Mirror the 4-item desktop sidebar exactly so the IA stays consistent
  // across breakpoints.
  const tabs: Tab[] = [
    {
      href: `/${lang}/dashboard`,
      label: t.nav.study,
      icon: BookOpen,
      activePaths: [
        `/${lang}/dashboard`,
        `/${lang}/review`,
        `/${lang}/mistakes`,
        `/${lang}/progress`,
      ],
    },
    {
      href: `/${lang}/practice`,
      label: t.nav.practice,
      icon: ClipboardList,
      activePaths: [`/${lang}/practice`],
    },
    {
      href: `/${lang}/mock`,
      label: t.nav.exam,
      icon: Timer,
      activePaths: [`/${lang}/mock`],
    },
    {
      href: `/${lang}/me`,
      label: t.nav.settings,
      icon: Settings,
      activePaths: [`/${lang}/me`],
    },
  ];

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-40 border-t border-border bg-card pb-[env(safe-area-inset-bottom)] md:hidden"
      aria-label="Primary"
    >
      <div className="flex items-center justify-around">
        {tabs.map(({ href, label, icon: Icon, activePaths }) => {
          const active = activePaths.some((path) => isActivePath(pathname, path));
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

function isActivePath(pathname: string, basePath: string): boolean {
  return pathname === basePath || pathname.startsWith(`${basePath}/`);
}
