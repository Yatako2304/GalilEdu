import { defineConfig, devices } from '@playwright/test';

const PORT = 3000;

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      testMatch: /.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // Levanta el front con MSW activado (no necesita backend)
  webServer: {
    command: `npm run dev -- --port ${PORT}`,
    timeout: 30 * 1000,
    port: PORT,
    reuseExistingServer: !process.env.CI,
  },
});
