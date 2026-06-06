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
import { useAuth } from "@/lib/auth-context";
import { LanguageSelect } from "@/components/language-select";
import { ExamSwitcher } from "@/components/exam-switcher";
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

  // 4-item IA: Study (data overview), Practice (drills + mistakes + review),
  // Exam (mock), Settings (account). Sub-pages like /mistakes /review /progress
  // remain reachable from their parent context, just not in the rail.
  const items: NavItem[] = [
    { href: `/${lang}/dashboard`, label: t.nav.study, icon: BookOpen },
    { href: `/${lang}/practice`, label: t.nav.practice, icon: ClipboardList },
    { href: `/${lang}/mock`, label: t.nav.exam, icon: Timer },
    { href: `/${lang}/me`, label: t.nav.settings, icon: Settings },
  ];

  return (
    <aside className="hidden md:flex md:fixed md:inset-y-0 md:left-0 md:w-64 md:flex-col md:border-r md:border-border md:bg-card">
      {/* Two rows so the exam switcher gets its own full-width line instead of
          being squeezed against the language control. */}
      <div className="flex flex-col gap-2.5 px-4 py-4">
        <div className="flex items-center justify-between gap-2">
          <Link
            href={`/${lang}/dashboard`}
            className="truncate text-lg font-bold leading-tight tracking-tight text-primary"
          >
            {t.nav.appBrand}
          </Link>
          <LanguageSelect currentLang={lang} ariaLabel={t.site.switchLanguage} />
        </div>
        <ExamSwitcher lang={lang} variant="plain" switchLabel={t.nav.switchExam} />
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
