import { test, expect, type Page } from "@playwright/test";
import { createTestUser, resetEmulator } from "./helpers";

const EMAIL = "persistence@test.local";
const PASSWORD = "test-password-1234";

async function signIn(page: Page) {
  await page.goto("/en/login");
  await page.locator("#email").fill(EMAIL);
  await page.locator("#password").fill(PASSWORD);
  await page.getByRole("button", { name: /sign in|登录/i }).click();
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
});
