"use client";

import Link from "next/link";

// Root-most error boundary. Fires when [lang]/layout.tsx itself throws —
// e.g. a render error in AuthProvider / QueryProvider / dictionary load.
// It REPLACES the html shell, so it must own its own <html><body> tags
// and can't rely on Tailwind / fonts / providers (CSS may not be loaded
// when this paints). Inline styles are intentional.

export default function GlobalError({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  return (
    <html lang="en">
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          fontFamily: "system-ui, -apple-system, Segoe UI, sans-serif",
          background: "#fafafa",
          color: "#111",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "1.5rem",
        }}
      >
        <div
          style={{
            maxWidth: "32rem",
            width: "100%",
            border: "1px solid #e5e7eb",
            borderRadius: "0.75rem",
            background: "#fff",
            padding: "2rem",
            textAlign: "center",
            boxShadow: "0 1px 2px rgba(0,0,0,0.04)",
          }}
        >
          <h1 style={{ fontSize: "1.5rem", fontWeight: 600, margin: "0 0 0.5rem 0" }}>
            Something went wrong
          </h1>
          <p style={{ color: "#555", margin: "0 0 1rem 0", lineHeight: 1.5 }}>
            An unexpected error broke the page. You can try again, or head back
            to the home page.
          </p>
          {error.digest && (
            <p
              style={{
                fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
                fontSize: "0.75rem",
                color: "#888",
                margin: "0 0 1.5rem 0",
              }}
            >
              Reference: {error.digest}
            </p>
          )}
          <div style={{ display: "flex", justifyContent: "center", gap: "0.5rem", flexWrap: "wrap" }}>
            <button
              type="button"
              onClick={() => unstable_retry()}
              style={{
                background: "#0a0a0a",
                color: "#fff",
                border: 0,
                borderRadius: "0.5rem",
                padding: "0.625rem 1rem",
                fontSize: "0.875rem",
                fontWeight: 500,
                cursor: "pointer",
              }}
            >
              Try again
            </button>
            <Link
              href="/"
              style={{
                display: "inline-block",
                border: "1px solid #d4d4d8",
                borderRadius: "0.5rem",
                padding: "0.625rem 1rem",
                fontSize: "0.875rem",
                fontWeight: 500,
                color: "#111",
                textDecoration: "none",
                background: "#fff",
              }}
            >
              Back home
            </Link>
          </div>
        </div>
      </body>
    </html>
  );
}
