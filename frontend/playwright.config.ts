import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E test configuration for OpenDataMask.
 *
 * These tests run against the verification sandbox environment started by
 * `verification/docker-compose.yml`. The frontend is served at http://localhost
 * and the backend API at http://localhost:8080.
 *
 * Usage:
 *   cd frontend
 *   npx playwright test
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI
    ? [['html'], ['junit', { outputFile: 'playwright-results.xml' }]]
    : 'html',
  timeout: 60_000,

  use: {
    baseURL: process.env.ODM_URL ?? 'http://localhost',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
