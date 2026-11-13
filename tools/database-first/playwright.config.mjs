import { defineConfig } from '@playwright/test';
import './env.mjs';
export default defineConfig({
  testDir: './tests', workers: 1, retries: 0, timeout: 30000,
  reporter: [['list'], ['json', { outputFile: 'target/playwright-results.json' }]],
  use: { baseURL: `http://127.0.0.1:${process.env.DBF_HTTP_PORT || 8095}`, viewport: { width: 1440, height: 1000 } }
});
