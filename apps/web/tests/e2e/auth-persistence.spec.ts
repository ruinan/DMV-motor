import { test, expect, type Page } from "@playwright/test";
import { createTestUser, resetEmulator } from "./helpers";

const EMAIL = "persistence@test.local";
const PASSWORD = "test-password-1234";

async function signIn(page: Page) {
  await page.goto("/en/login");
  await page.locator("#email").fill(EMAIL);
  await page.locator("#password").fill(PASSWORD);
  // Submit button is type=submit; the "Sign in" tab is type=button. Match
  // by attribute to avoid ambiguity now that the form has tabs.
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/en\/me$/);
}

test.describe("auth persistence (#1)", () => {
  test.beforeEach(async () => {
    await resetEmulator();
    await createTestUser(EMAIL, PASSWORD);
  });

  test("/me survives a hard reload", async ({ page }) => {
    await signIn(page);
    await page.reload();
    // If Firebase falls back to in-memory persistence, RequireAuth fires the
    // useEffect-driven router.replace("/login") within ~1s of mount.
    await expect(page).toHaveURL(/\/en\/me$/);
  });

  test("/dashboard survives a hard reload", async ({ page }) => {
    await signIn(page);
    await page.goto("/en/dashboard");
    await expect(page).toHaveURL(/\/en\/dashboard$/);
    await page.reload();
    await expect(page).toHaveURL(/\/en\/dashboard$/);
  });

  test("navigating from /practice (unguarded) to /dashboard preserves session", async ({
    page,
  }) => {
    await signIn(page);
    // /practice is outside the (app) group — no RequireAuth wrapper. We hop
    // there then back to /dashboard to mimic the user's "exit practice → land
    // on dashboard" flow without depending on backend.
    await page.goto("/en/practice");
    await expect(page).toHaveURL(/\/en\/practice$/);
    await page.goto("/en/dashboard");
    await expect(page).toHaveURL(/\/en\/dashboard$/);
  });

  test("signed-in /practice keeps sidebar chrome through a hard reload", async ({
    page,
  }) => {
    await signIn(page);
    await page.goto("/en/practice");
    // Sidebar brand is rendered only by PracticeShell's signed-in branch
    // (anonymous branch is the full-screen card without the sidebar).
    const sidebarBrand = page.getByText("M1 Prep").first();
    await expect(sidebarBrand).toBeVisible();
    await page.reload();
    await expect(page).toHaveURL(/\/en\/practice$/);
    await expect(sidebarBrand).toBeVisible();
  });

  test("401 with a valid token retries with a refreshed token instead of signing out", async ({
    page,
  }) => {
    // Route must be installed BEFORE signIn, otherwise the /me request that
    // /en/me fires on first mount slips through the mock. Mock the first
    // call as 401, the second as 200 — api-client should see the 401, force-
    // refresh the Firebase token, retry, and surface the 200, leaving the
    // user on /me with no session-expired toast.
    let calls = 0;
    await page.route("**/api/v1/me", async (route) => {
      calls += 1;
      if (calls === 1) {
        await route.fulfill({
          status: 401,
          contentType: "application/json",
          body: JSON.stringify({
            success: false,
            error: { code: "UNAUTHORIZED", message: "stale token" },
          }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              user_id: "1",
              email: EMAIL,
              language: "en",
              access: {
                state: "free_trial",
                has_active_pass: false,
                expires_at: null,
                mock_remaining: 0,
              },
              learning: {
                has_in_progress_practice: false,
                has_in_progress_review: false,
              },
            },
            meta: {},
          }),
        });
      }
    });

    await signIn(page);
    // Wait for at least 2 hits on /api/v1/me before asserting — the retry
    // is fully async (Firebase token refresh + second fetch) and can land
    // after toHaveURL resolves.
    await expect.poll(() => calls, { timeout: 7_000 }).toBeGreaterThanOrEqual(2);
    await expect(page).toHaveURL(/\/en\/me$/);
    await expect(
      page.getByTestId("session-expired-toast"),
    ).not.toBeVisible();
  });
});
