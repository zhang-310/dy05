import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E 测试配置
 * 覆盖所有 128 个页面组件的完整功能测试
 * 包含自动修复功能
 */
export default defineConfig({
  testDir: './e2e/tests',

  // 测试超时配置
  timeout: 90000, // 增加到 90 秒
  expect: {
    timeout: 15000, // 增加到 15 秒
  },

  // 并行配置
  fullyParallel: false, // 串行运行避免数据冲突
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 2, // 本地和 CI 都启用 2 次重试
  workers: process.env.CI ? 1 : 2,

  // 报告配置
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['list'],
  ],

  // 全局配置
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
    navigationTimeout: 60000, // 增加到 60 秒
    actionTimeout: 30000, // 增加到 30 秒
  },

  // 测试项目
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 12'] },
    },
    // 自动修复测试项目
    {
      name: 'chromium-autofix',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /.*autofix\.spec\.ts/,
    },
  ],

  // Web 服务器
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
    stdout: 'ignore',
    stderr: 'pipe',
  },

  // 全局钩子
  globalSetup: require.resolve('./e2e/global-setup.ts'),
  globalTeardown: require.resolve('./e2e/global-teardown.ts'),
})
