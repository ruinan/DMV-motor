import { test, expect } from "@playwright/test";
import { resetEmulator, getOobLink } from "./helpers";

const PASSWORD = "test-password-1234";
const MFA_GATE = "Set up two-factor authentication";

/**
 * Regression for the new-user deadlock: Identity Platform refuses TOTP
 * enrollment until the email is verified, but the app forced the 2FA gate
 * immediately after sign-up with no verification step — locking every new
 * user out. A verification gate must sit *before* the 2FA gate.
 */
test.describe("forced email verification before 2FA", () => {
  test.beforeEach(async () => {
    await resetEmulator();
  });

  test("a new sign-up must verify email before the 2FA gate", async ({
    page,
  }) => {
    const email = `verify-${Date.now()}@test.local`;

    // Register through the real UI (Create-account tab).
    await page.goto("/en/login");
    await page.getByRole("tab", { name: "Create account" }).click();
    await page.locator("#email").fill(email);
    await page.locator("#password").fill(PASSWORD);
    await page.locator('button[type="submit"]').click();

    // The email-verification gate must block the shell; the 2FA gate is NOT
    // reachable yet.
    await expect(page.getByTestId("email-verify-gate")).toBeVisible();
    await expect(page.getByRole("heading", { name: MFA_GATE })).toHaveCount(0);

    // The gate auto-sent a verification email; apply the link as the user would.
    const link = await getOobLink(email, "VERIFY_EMAIL");
    await page.request.get(link);

    // Confirm verification → we advance to the 2FA gate (no longer deadlocked).
    await page.getByTestId("verify-continue").click();
    await expect(page.getByRole("heading", { name: MFA_GATE })).toBeVisible();
  });
});
