import { test, expect, type Page, type ConsoleMessage } from "@playwright/test";
import { createTestUser, resetEmulator } from "./helpers";

/**
 * Round 4 #5 — capture every console message + uncaught page error during a
 * walk through the app. Failure modes we expect to flush out:
 *   - hydration mismatches (Next.js 16 / React 19)
 *   - missing-key warnings, duplicate-id warnings
 *   - failed asset loads (404/CORS) surfacing as console errors
 *   - Firebase SDK warnings about persistence / authDomain / popup blockers
 *
 * Public pages (marketing, login) need only the Auth emulator + Next dev
 * server (both started by playwright.config webServer). Authenticated pages
 * additionally need the backend on :8080 with seeded data — these are gated
 * on BACKEND_RUNNING=true so the spec is still useful when the backend is
 * down.
 */

const EMAIL = "console-walk@test.local";
const PASSWORD = "test-password-1234";

type Captured = {
  url: string;
  errors: string[];
  warnings: string[];
  pageErrors: string[];
  failedRequests: string[];
};

function attachListeners(page: Page, capture: Captured) {
  page.on("console", (msg: ConsoleMessage) => {
    const text = `[${msg.type()}] ${msg.text()}`;
    if (msg.type() === "error") capture.errors.push(text);
    else if (msg.type() === "warning") capture.warnings.push(text);
  });
  page.on("pageerror", (err) => {
    capture.pageErrors.push(`${err.name}: ${err.message}`);
  });
  page.on("requestfailed", (req) => {
    const failure = req.failure();
    if (!failure) return;
    // Ignore expected emulator/auth-related failures during sign-out cleanup.
    if (req.url().includes("/emulator/")) return;
    capture.failedRequests.push(`${req.method()} ${req.url()} — ${failure.errorText}`);
  });
}

function summarize(captures: Captured[]): string {
  return captures
    .map((c) => {
      const lines: string[] = [`URL: ${c.url}`];
      if (c.errors.length) lines.push(`  errors:    ${c.errors.length}`);
      if (c.warnings.length) lines.push(`  warnings:  ${c.warnings.length}`);
      if (c.pageErrors.length) lines.push(`  uncaught:  ${c.pageErrors.length}`);
      if (c.failedRequests.length) lines.push(`  reqFailed: ${c.failedRequests.length}`);
      const all = [...c.errors, ...c.pageErrors, ...c.failedRequests, ...c.warnings];
      for (const m of all) lines.push(`    - ${m}`);
      return lines.join("\n");
    })
    .join("\n\n");
}

test.describe("console-error walk (#5)", () => {
  test("public pages emit no console errors", async ({ page }) => {
    const captures: Captured[] = [];

    // /practice belongs here too: per docs/development/api-contract.md §4 the
    // free-trial set is open to anonymous users, and PracticeFlow auto-picks
    // entry_type=free_trial when not signed in.
    for (const url of [
      "/en",
      "/zh",
      "/en/login",
      "/zh/login",
      "/en/practice",
      "/zh/practice",
    ]) {
      const c: Captured = {
        url,
        errors: [],
        warnings: [],
        pageErrors: [],
        failedRequests: [],
      };
      attachListeners(page, c);
      await page.goto(url);
      // Let lazy chunks settle and any post-mount fetches resolve.
      await page.waitForLoadState("networkidle");
      captures.push(c);
      page.removeAllListeners("console");
      page.removeAllListeners("pageerror");
      page.removeAllListeners("requestfailed");
    }

    const totalErrors = captures.reduce(
      (n, c) => n + c.errors.length + c.pageErrors.length,
      0,
    );
    if (totalErrors > 0) {
      console.log("\n=== Console capture ===\n" + summarize(captures));
    }
    expect(totalErrors, summarize(captures)).toBe(0);
  });

  test("anonymous /practice renders without auth redirect", async ({ page }) => {
    // sec audit #4 regression: /practice used to live under the (app)
    // RequireAuth wrapper, so anonymous visitors got bounced to /login —
    // breaking the documented "free-trial set is open to anonymous" contract.
    await page.goto("/en/practice");
    await expect(page).toHaveURL(/\/en\/practice$/);
    // Start button is the idle-phase CTA; rendered without a backend call.
    await expect(page.getByRole("button", { name: /start/i })).toBeVisible();
  });

  test.describe("authenticated routes (requires BACKEND_RUNNING=true)", () => {
    test.skip(
      process.env.BACKEND_RUNNING !== "true",
      "Backend not running on :8080 — skip. Start backend, then re-run with BACKEND_RUNNING=true npm run test:e2e.",
    );

    test.beforeEach(async () => {
      await resetEmulator();
      await createTestUser(EMAIL, PASSWORD);
    });

    test("authed walk emits no console errors", async ({ page }) => {
      const captures: Captured[] = [];

      // Sign in once.
      await page.goto("/en/login");
      await page.locator("#email").fill(EMAIL);
      await page.locator("#password").fill(PASSWORD);
      await page.locator('button[type="submit"]').click();
      await expect(page).toHaveURL(/\/en\/me$/);

      // Now collect across every protected route.
      for (const url of [
        "/en/dashboard",
        "/en/me",
        "/en/mistakes",
        "/en/review",
        "/en/mock",
        "/en/progress",
        "/en/practice",
      ]) {
        const c: Captured = {
          url,
          errors: [],
          warnings: [],
          pageErrors: [],
          failedRequests: [],
        };
        attachListeners(page, c);
        await page.goto(url);
        await page.waitForLoadState("networkidle");
        captures.push(c);
        page.removeAllListeners("console");
        page.removeAllListeners("pageerror");
        page.removeAllListeners("requestfailed");
      }

      const totalErrors = captures.reduce(
        (n, c) => n + c.errors.length + c.pageErrors.length,
        0,
      );
      if (totalErrors > 0) {
        console.log("\n=== Console capture ===\n" + summarize(captures));
      }
      expect(totalErrors, summarize(captures)).toBe(0);
    });
  });
});
