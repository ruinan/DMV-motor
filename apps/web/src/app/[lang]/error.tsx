"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { AlertTriangle, Home, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

// [lang]/error.tsx catches any uncaught render error inside the locale
// subtree — i.e. (marketing) / (app) / (auth) groups, /practice, etc. —
// without taking down the html shell. Errors that happen in [lang]/layout
// itself escape this boundary and are caught by app/global-error.tsx.
//
// error.tsx must be a Client Component (Next requires it for the React
// error boundary contract), so we can't await the server-side dictionary
// loader. The inline STRINGS table covers the same en/zh contract; if we
// add a third locale the table needs to grow alongside messages/*.json.

const STRINGS = {
  en: {
    title: "Something went wrong",
    body: "An unexpected error occurred while loading this page. You can try again, or head back to the home page.",
    tryAgain: "Try again",
    backHome: "Back home",
    digestLabel: "Reference",
  },
  zh: {
    title: "出错了",
    body: "加载页面时发生了一个意外错误。你可以重试，或返回首页。",
    tryAgain: "重试",
    backHome: "回到首页",
    digestLabel: "错误编号",
  },
} as const;

type Lang = keyof typeof STRINGS;

export default function ErrorPage({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  const params = useParams<{ lang: string }>();
  const lang: Lang = params?.lang === "zh" ? "zh" : "en";
  const t = STRINGS[lang];

  useEffect(() => {
    // Surface the error in the browser console so devs can inspect it
    // even when the digest is the only thing rendered.
    console.error("[lang/error]", error);
  }, [error]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <div className="w-full max-w-md rounded-xl border border-destructive/40 bg-card p-6 text-center shadow-sm">
        <div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-full bg-destructive/10">
          <AlertTriangle className="size-6 text-destructive" />
        </div>
        <h1 className="text-xl font-semibold text-foreground">{t.title}</h1>
        <p className="mt-2 text-sm text-muted-foreground">{t.body}</p>
        {error.digest && (
          <p className="mt-3 font-mono text-xs text-muted-foreground/70">
            {t.digestLabel}: {error.digest}
          </p>
        )}
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <Button onClick={() => unstable_retry()}>
            <RefreshCw className="size-4" />
            {t.tryAgain}
          </Button>
          <Link
            href={`/${lang}`}
            className="inline-flex items-center justify-center gap-2 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium transition-colors hover:bg-muted"
          >
            <Home className="size-4" />
            {t.backHome}
          </Link>
        </div>
      </div>
    </div>
  );
}
