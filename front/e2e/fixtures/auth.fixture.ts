import { test as base, Page } from '@playwright/test'

/**
 * 认证 Fixture
 * 提供自动登录功能，避免每个测试都重复登录流程
 */

export interface AuthFixture {
  authenticatedPage: Page
}

export const test = base.extend<AuthFixture>({
  authenticatedPage: async ({ page }, use) => {
    // 使用真实登录流程（后端必须运行）
    const username = process.env.TEST_USERNAME || 'admin'
    const password = process.env.TEST_PASSWORD || 'admin123'

    console.log(`🔐 开始登录: ${username}`)

    // 导航到登录页
    await page.goto('/login', { waitUntil: 'domcontentloaded' })
    await page.waitForLoadState('networkidle', { timeout: 10000 })

    // 填写登录表单
    try {
      // 尝试多种选择器策略
      const usernameInput = page.locator('input[name="username"]').or(page.getByLabel('用户名')).or(page.getByPlaceholder('用户名'))
      const passwordInput = page.locator('input[name="password"]').or(page.getByLabel('密码')).or(page.getByPlaceholder('密码'))
      const loginButton = page.locator('button[type="submit"]').or(page.getByRole('button', { name: /登录|Login/i }))

      await usernameInput.waitFor({ state: 'visible', timeout: 5000 })
      await usernameInput.fill(username)

      await passwordInput.waitFor({ state: 'visible', timeout: 5000 })
      await passwordInput.fill(password)

      await loginButton.waitFor({ state: 'visible', timeout: 5000 })
      await loginButton.click()

      // 等待登录成功 - 检查是否跳转到仪表盘或其他页面
      await page.waitForURL(/\/(admin\/dashboard|dashboard|home)/, { timeout: 15000 })

      // 等待页面加载完成
      await page.waitForLoadState('networkidle', { timeout: 10000 })

      console.log('✅ 登录成功')
    } catch (error) {
      console.error('❌ 登录失败:', error)

      // 截图以便调试
      await page.screenshot({ path: `test-results/login-failure-${Date.now()}.png` })

      throw new Error(`Authentication failed: ${error}`)
    }

    // 将已认证的页面传递给测试
    await use(page)
  },
})

export { expect } from '@playwright/test'
