"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BookOpen,
  ClipboardList,
  LogOut,
  Timer,
  Settings,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { LanguageToggle } from "@/components/language-toggle";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
  activePaths: string[];
};

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function AppSidebar({ t, lang }: Props) {
  const pathname = usePathname();
  const { user, signOut } = useAuth();

  // 4-item IA: Study (global state + study detail), Practice (drills),
  // Exam (mock), Settings (account). Secondary study pages stay reachable
  // from their parent context, just not in the rail.
  const items: NavItem[] = [
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
    <aside className="hidden md:flex md:fixed md:inset-y-0 md:left-0 md:w-64 md:flex-col md:border-r md:border-border md:bg-card">
      <div className="flex h-16 items-center justify-between px-6">
        <Link
          href={`/${lang}/dashboard`}
          className="text-lg font-bold tracking-tight text-primary"
        >
          {t.nav.appBrand}
        </Link>
        <LanguageToggle currentLang={lang} ariaLabel={t.site.switchLanguage} />
      </div>

      <nav className="flex flex-1 flex-col gap-1 px-3 pt-2">
        {items.map(({ href, label, icon: Icon, activePaths }) => {
          const active = activePaths.some((path) => isActivePath(pathname, path));
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 rounded-md border-l-4 px-3 py-2.5 text-sm font-medium transition-colors ${
                active
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-transparent text-muted-foreground hover:bg-muted hover:text-foreground"
              }`}
            >
              <Icon className="size-4" />
              <span>{label}</span>
            </Link>
          );
        })}
      </nav>

      {user && (
        <div className="mt-auto border-t border-border p-4">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
              {(user.email ?? "?").charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 truncate">
              <p className="truncate text-xs text-muted-foreground">
                {user.email}
              </p>
            </div>
            <button
              onClick={() => signOut()}
              aria-label={t.nav.signOut}
              title={t.nav.signOut}
              className="rounded-md p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </div>
      )}
    </aside>
  );
}

function isActivePath(pathname: string, basePath: string): boolean {
  return pathname === basePath || pathname.startsWith(`${basePath}/`);
}
