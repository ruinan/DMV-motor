"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  ClipboardList,
  Bookmark,
  RotateCw,
  Timer,
  User,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { LanguageToggle } from "@/components/language-toggle";
import type { Dictionary, Locale } from "@/lib/dictionaries";

type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
};

type Props = {
  t: Dictionary;
  lang: Locale;
};

export function AppSidebar({ t, lang }: Props) {
  const pathname = usePathname();
  const { user, signOut } = useAuth();

  const items: NavItem[] = [
    { href: `/${lang}/dashboard`, label: t.nav.dashboard, icon: LayoutDashboard },
    { href: `/${lang}/practice`, label: t.nav.practice, icon: ClipboardList },
    { href: `/${lang}/mistakes`, label: t.nav.mistakes, icon: Bookmark },
    { href: `/${lang}/review`, label: t.nav.review, icon: RotateCw },
    { href: `/${lang}/mock`, label: t.nav.mockExam, icon: Timer },
    { href: `/${lang}/me`, label: t.nav.account, icon: User },
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
        {items.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
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
              <LogOutIcon />
            </button>
          </div>
        </div>
      )}
    </aside>
  );
}

function LogOutIcon() {
  return (
    <svg
      className="size-4"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" x2="9" y1="12" y2="12" />
    </svg>
  );
}
