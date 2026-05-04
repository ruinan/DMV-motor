"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { ApiError } from "@/lib/api-client";

export function QueryProvider({ children }: { children: ReactNode }) {
  // Single client per browser session — useState lazy init is the
  // canonical Next.js + RSC pattern (see TanStack Query docs).
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60_000,
            refetchOnWindowFocus: false,
            // Skip retries on auth/access errors — they won't succeed on
            // retry, and 401 already triggered signOut → /login redirect
            // upstream in apiFetch.
            retry: (failureCount, error) => {
              if (
                error instanceof ApiError &&
                (error.status === 401 || error.status === 403)
              ) {
                return false;
              }
              return failureCount < 2;
            },
          },
          mutations: {
            retry: false,
          },
        },
      }),
  );
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
