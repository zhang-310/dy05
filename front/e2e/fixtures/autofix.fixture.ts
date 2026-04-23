import { test as base, expect } from '@playwright/test'
import { chromium, Page } from '@playwright/test'

/**
 * 自动修复测试 Fixture
 * 当测试失败时，自动尝试修复常见问题
 */

type AutoFixOptions = {
  maxRetries?: number
  autoLogin?: boolean
  clearCache?: boolean
  waitForStability?: boolean
}

const defaultOptions: AutoFixOptions = {
  maxRetries: 3,
  autoLogin: true,
  clearCache: false,
  waitForStability: true,
}

export const test = base.extend<{
  autoFixPage: Page
  autoFixOptions: AutoFixOptions
}>({
  autoFixOptions: [defaultOptions, { option: true }],

  autoFixPage: async ({ page, autoFixOptions }, use) => {
    const options = { ...defaultOptions, ...autoFixOptions }

    // 包装页面方法，添加自动修复逻辑
    const originalGoto = page.goto.bind(page)
    const originalClick = page.click.bind(page)
    const originalFill = page.fill.bind(page)

    // 自动修复的 goto
    page.goto = async (url: string, gotoOptions?: any) => {
      let lastError: Error | null = null

      for (let attempt = 0; attempt < (options.maxRetries || 1); attempt++) {
        try {
          // 等待页面稳定
          if (options.waitForStability && attempt > 0) {
            await page.waitForTimeout(1000)
          }

          const response = await originalGoto(url, gotoOptions)

          // 检查是否被重定向到登录页
          if (options.autoLogin && page.url().includes('/login')) {
            console.log(`[AutoFix] 检测到未登录，尝试自动登录...`)
            await autoLogin(page)
            // 重新导航到目标页面
            return await originalGoto(url, gotoOptions)
          }

          return response
        } catch (error) {
          lastError = error as Error
          console.log(`[AutoFix] goto 失败 (尝试 ${attempt + 1}/${options.maxRetries}): ${error}`)

          // 尝试修复
          if (attempt < (options.maxRetries || 1) - 1) {
            await tryFix(page, error as Error, options)
          }
        }
      }

      throw lastError
    }

    // 自动修复的 click
    page.click = async (selector: string, clickOptions?: any) => {
      let lastError: Error | null = null

      for (let attempt = 0; attempt < (options.maxRetries || 1); attempt++) {
        try {
          // 等待元素可见
          await page.waitForSelector(selector, { state: 'visible', timeout: 5000 })

          // 滚动到元素
          await page.locator(selector).scrollIntoViewIfNeeded()

          // 等待元素稳定
          if (options.waitForStability) {
            await page.waitForTimeout(100)
          }

          return await originalClick(selector, clickOptions)
        } catch (error) {
          lastError = error as Error
          console.log(`[AutoFix] click 失败 (尝试 ${attempt + 1}/${options.maxRetries}): ${selector}`)

          if (attempt < (options.maxRetries || 1) - 1) {
            // 尝试关闭可能的遮罩层
            await tryCloseOverlays(page)
            await page.waitForTimeout(500)
          }
        }
      }

      throw lastError
    }

    // 自动修复的 fill
    page.fill = async (selector: string, value: string, fillOptions?: any) => {
      let lastError: Error | null = null

      for (let attempt = 0; attempt < (options.maxRetries || 1); attempt++) {
        try {
          // 等待元素可见
          await page.waitForSelector(selector, { state: 'visible', timeout: 5000 })

          // 清空现有内容
          await originalClick(selector)
          await page.keyboard.press('Control+A')
          await page.keyboard.press('Backspace')

          return await originalFill(selector, value, fillOptions)
        } catch (error) {
          lastError = error as Error
          console.log(`[AutoFix] fill 失败 (尝试 ${attempt + 1}/${options.maxRetries}): ${selector}`)

          if (attempt < (options.maxRetries || 1) - 1) {
            await page.waitForTimeout(500)
          }
        }
      }

      throw lastError
    }

    await use(page)
  },
})

/**
 * 自动登录
 */
async function autoLogin(page: Page) {
  try {
    // 检查是否在登录页
    if (!page.url().includes('/login')) {
      await page.goto('/login')
    }

    // 填写登录表单
    await page.fill('input[name="username"]', process.env.TEST_USERNAME || 'admin')
    await page.fill('input[name="password"]', process.env.TEST_PASSWORD || 'admin123')
    await page.click('button:has-text("登录")')

    // 等待登录完成
    await page.waitForURL(/\/admin/, { timeout: 10000 })
    console.log(`[AutoFix] 自动登录成功`)
  } catch (error) {
    console.error(`[AutoFix] 自动登录失败:`, error)
    throw error
  }
}

/**
 * 尝试修复常见问题
 */
async function tryFix(page: Page, error: Error, options: AutoFixOptions) {
  const errorMessage = error.message

  // 修复 1: 清除缓存和 Cookie
  if (options.clearCache && errorMessage.includes('timeout')) {
    console.log(`[AutoFix] 清除缓存和 Cookie...`)
    await page.context().clearCookies()
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
  }

  // 修复 2: 关闭遮罩层
  if (errorMessage.includes('not visible') || errorMessage.includes('obscured')) {
    console.log(`[AutoFix] 尝试关闭遮罩层...`)
    await tryCloseOverlays(page)
  }

  // 修复 3: 刷新页面
  if (errorMessage.includes('detached') || errorMessage.includes('navigation')) {
    console.log(`[AutoFix] 刷新页面...`)
    await page.reload({ waitUntil: 'networkidle' })
  }

  // 修复 4: 重新登录
  if (errorMessage.includes('401') || errorMessage.includes('403') || errorMessage.includes('unauthorized')) {
    console.log(`[AutoFix] 检测到认证问题，重新登录...`)
    await autoLogin(page)
  }

  // 修复 5: 等待网络空闲
  if (errorMessage.includes('timeout') || errorMessage.includes('waiting')) {
    console.log(`[AutoFix] 等待网络空闲...`)
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {})
  }
}

/**
 * 尝试关闭遮罩层
 */
async function tryCloseOverlays(page: Page) {
  try {
    // 关闭对话框
    const dialogClose = page.locator('.MuiDialog-root button[aria-label="关闭"], .MuiDialog-root button:has-text("取消")')
    if (await dialogClose.isVisible()) {
      await dialogClose.first().click()
      await page.waitForTimeout(300)
    }

    // 关闭 Snackbar
    const snackbarClose = page.locator('.notistack-snackbar button[aria-label="关闭"]')
    if (await snackbarClose.isVisible()) {
      await snackbarClose.first().click()
      await page.waitForTimeout(300)
    }

    // 关闭 Drawer
    const drawerClose = page.locator('.MuiDrawer-root button[aria-label="关闭"]')
    if (await drawerClose.isVisible()) {
      await drawerClose.first().click()
      await page.waitForTimeout(300)
    }

    // 按 ESC 键
    await page.keyboard.press('Escape')
    await page.waitForTimeout(300)
  } catch (error) {
    // 忽略错误
  }
}

/**
 * 自动修复断言
 */
export async function expectWithRetry(
  assertion: () => Promise<void>,
  options: { maxRetries?: number; retryDelay?: number } = {}
) {
  const maxRetries = options.maxRetries || 3
  const retryDelay = options.retryDelay || 1000

  let lastError: Error | null = null

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      await assertion()
      return
    } catch (error) {
      lastError = error as Error
      console.log(`[AutoFix] 断言失败 (尝试 ${attempt + 1}/${maxRetries})`)

      if (attempt < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, retryDelay))
      }
    }
  }

  throw lastError
}

export { expect }
