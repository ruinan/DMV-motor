import { defineConfig, devices } from "@playwright/test";

const PORT = 3100;
const EMULATOR_PORT = 9099;

export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: process.env.CI ? "list" : [["list"], ["html", { open: "never" }]],
  timeout: 30_000,
  expect: { timeout: 7_000 },
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: [
    {
      command: "npm run emulator",
      port: EMULATOR_PORT,
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
      stdout: "ignore",
      stderr: "pipe",
    },
    {
      command: `next dev --port ${PORT}`,
      port: PORT,
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
      env: {
        NEXT_PUBLIC_USE_FIREBASE_EMULATOR: "true",
        // Disable reCAPTCHA in e2e: when a site key is present (.env.local sets
        // one for manual local testing) the login form runs a precheck against
        // the backend, which isn't started for these tests — registration then
        // fails. Empty key → useRecaptcha is a no-op, matching CI.
        NEXT_PUBLIC_RECAPTCHA_SITE_KEY: "",
      },
      stdout: "ignore",
      stderr: "pipe",
    },
  ],
});
