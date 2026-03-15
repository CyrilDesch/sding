import { defineConfig, devices } from '@playwright/test'

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:5173'
export const API_URL = process.env.API_URL ?? 'http://localhost:8080'

// When BASE_URL is explicitly provided (e.g. docker run -e BASE_URL=...),
// skip webServer — the stack is expected to be running externally.
const webServer = process.env.BASE_URL
  ? undefined
  : { command: 'pnpm dev', url: BASE_URL, reuseExistingServer: true, timeout: 30_000 }

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  webServer,
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
