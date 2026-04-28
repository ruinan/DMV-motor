import type { NextConfig } from "next";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Proxy /api/v1/* to the backend so the browser never makes a cross-origin
  // request — keeps CORS off the table in both dev (localhost:8080) and prod
  // (Cloud Run). Vercel resolves the rewrite server-side; the browser sees
  // only same-origin traffic.
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${apiBaseUrl}/api/v1/:path*`,
      },
      {
        source: "/actuator/:path*",
        destination: `${apiBaseUrl}/actuator/:path*`,
      },
    ];
  },
};

export default nextConfig;
