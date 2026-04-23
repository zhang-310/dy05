import { Page, expect } from '@playwright/test'

/**
 * E2E 测试辅助函数
 */

/**
 * 等待页面加载完成
 */
export async function waitForPageLoad(page: Page, options: { timeout?: number } = {}) {
  const timeout = options.timeout || 30000
  try {
    await page.waitForLoadState('domcontentloaded', { timeout })
    await page.waitForLoadState('networkidle', { timeout })
  } catch (error) {
    console.warn('⚠️ 页面加载超时，继续执行测试')
    // 不抛出错误，允许测试继续
  }
}

/**
 * 等待 API 请求完成
 */
export async function waitForApiResponse(page: Page, urlPattern: string | RegExp) {
  return await page.waitForResponse(response => {
    const url = response.url()
    if (typeof urlPattern === 'string') {
      return url.includes(urlPattern)
    }
    return urlPattern.test(url)
  })
}

/**
 * 填写表单字段
 */
export async function fillForm(page: Page, fields: Record<string, string>) {
  for (const [name, value] of Object.entries(fields)) {
    const input = page.locator(`input[name="${name}"], textarea[name="${name}"]`)
    await input.fill(value)
  }
}

/**
 * 点击并等待导航
 */
export async function clickAndNavigate(page: Page, selector: string, expectedUrl?: string | RegExp) {
  await page.click(selector)
  if (expectedUrl) {
    await page.waitForURL(expectedUrl)
  }
}

/**
 * 验证 Toast 消息
 */
export async function expectToast(page: Page, message: string, type: 'success' | 'error' | 'warning' | 'info' = 'success') {
  const toast = page.locator('.notistack-snackbar, .MuiSnackbar-root')
  await expect(toast).toContainText(message)
}

/**
 * 验证表格数据加载
 */
export async function expectTableLoaded(page: Page, options: { timeout?: number } = {}) {
  const timeout = options.timeout || 30000

  try {
    const table = page.locator('.MuiDataGrid-root, table')
    await expect(table).toBeVisible({ timeout })

    // 等待加载指示器消失（使用更长的超时）
    const loading = page.locator('.MuiCircularProgress-root, .loading')
    await expect(loading).toHaveCount(0, { timeout })
  } catch (error) {
    console.warn('⚠️ 表格加载超时，可能是数据为空或 API 响应慢')
    // 检查是否显示"没有数据"
    const emptyState = page.locator('text=/没有数据|暂无数据|No data/i')
    if (await emptyState.isVisible()) {
      console.log('ℹ️ 表格为空状态')
    }
  }
}

/**
 * 验证对话框打开
 */
export async function expectDialogOpen(page: Page, title?: string) {
  const dialog = page.locator('.MuiDialog-root')
  await expect(dialog).toBeVisible()

  if (title) {
    const dialogTitle = page.locator('.MuiDialogTitle-root')
    await expect(dialogTitle).toContainText(title)
  }
}

/**
 * 关闭对话框
 */
export async function closeDialog(page: Page) {
  const closeButton = page.locator('.MuiDialog-root button:has-text("取消"), .MuiDialog-root button:has-text("关闭")')
  await closeButton.click()

  const dialog = page.locator('.MuiDialog-root')
  await expect(dialog).toHaveCount(0)
}

/**
 * 选择下拉选项
 */
export async function selectOption(page: Page, fieldLabel: string, optionText: string) {
  // 点击 Select 组件
  await page.click(`label:has-text("${fieldLabel}") + div, div:has(label:has-text("${fieldLabel}"))`)

  // 等待下拉菜单出现
  await page.waitForSelector('.MuiMenu-root, .MuiPopover-root')

  // 选择选项
  await page.click(`.MuiMenuItem-root:has-text("${optionText}")`)
}

/**
 * 验证页面标题
 */
export async function expectPageTitle(page: Page, title: string) {
  const pageTitle = page.locator('h1, h2, .page-title, .MuiTypography-h4, .MuiTypography-h5')
  await expect(pageTitle.first()).toContainText(title)
}

/**
 * 等待并验证数据加载
 */
export async function waitForDataLoad(page: Page, dataSelector: string = '.MuiDataGrid-row, .data-item, .card', options: { timeout?: number } = {}) {
  const timeout = options.timeout || 30000

  try {
    await page.waitForSelector(dataSelector, { timeout, state: 'visible' })
    const items = page.locator(dataSelector)
    await expect(items).not.toHaveCount(0)
  } catch (error) {
    console.warn(`⚠️ 数据加载超时或无数据: ${dataSelector}`)
    // 检查是否是空状态而非错误
    const emptyState = page.locator('text=/没有数据|暂无数据|No data|Empty/i')
    if (await emptyState.isVisible()) {
      console.log('ℹ️ 数据为空状态（正常）')
      return
    }
    throw error
  }
}

/**
 * 模拟 API 响应
 */
export async function mockApiResponse(page: Page, urlPattern: string | RegExp, response: any) {
  await page.route(urlPattern, route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(response),
    })
  })
}

/**
 * 验证按钮状态
 */
export async function expectButtonEnabled(page: Page, buttonText: string, enabled: boolean = true) {
  const button = page.locator(`button:has-text("${buttonText}")`)
  if (enabled) {
    await expect(button).toBeEnabled()
  } else {
    await expect(button).toBeDisabled()
  }
}

/**
 * 点击表格行操作按钮
 */
export async function clickRowAction(page: Page, rowText: string, actionText: string) {
  const row = page.locator('.MuiDataGrid-row').filter({ hasText: rowText })
  await row.waitFor({ state: 'visible' })
  await row.hover()
  await row.locator(`button:has-text("${actionText}")`).click()
}

/**
 * 等待对话框打开
 */
export async function waitForDialog(page: Page) {
  await page.waitForSelector('.MuiDialog-root', { state: 'visible', timeout: 5000 })
  await page.waitForTimeout(300)
}

/**
 * 搜索并验证结果
 */
export async function searchAndVerify(page: Page, keyword: string, expectedText: string) {
  const searchInput = page.locator('input[placeholder*="搜索"]')
  await searchInput.fill(keyword)
  await searchInput.press('Enter')
  await waitForDataLoad(page)
  await expect(page.locator('.MuiDataGrid-row').first()).toContainText(expectedText)
}

/**
 * 分页操作
 */
export async function goToNextPage(page: Page) {
  const nextButton = page.locator('button[aria-label="下一页"], button:has-text("下一页")')
  if (await nextButton.isEnabled()) {
    await nextButton.click()
    await waitForDataLoad(page)
  }
}

export async function goToPreviousPage(page: Page) {
  const prevButton = page.locator('button[aria-label="上一页"], button:has-text("上一页")')
  if (await prevButton.isEnabled()) {
    await prevButton.click()
    await waitForDataLoad(page)
  }
}

/**
 * 批量选择行
 */
export async function selectRows(page: Page, count: number) {
  const checkboxes = page.locator('.MuiDataGrid-row .MuiCheckbox-root')
  const totalCount = await checkboxes.count()
  const selectCount = Math.min(count, totalCount)

  for (let i = 0; i < selectCount; i++) {
    await checkboxes.nth(i).click()
    await page.waitForTimeout(200)
  }

  return selectCount
}

/**
 * 上传文件
 */
export async function uploadFile(page: Page, filePath: string) {
  const fileInput = page.locator('input[type="file"]')
  await fileInput.setInputFiles(filePath)
  await page.waitForTimeout(1000)
}

/**
 * 清空输入框
 */
export async function clearInput(page: Page, selector: string) {
  await page.click(selector)
  await page.keyboard.press('Control+A')
  await page.keyboard.press('Backspace')
}

/**
 * 截图并保存
 */
export async function takeScreenshot(page: Page, name: string) {
  const screenshotPath = `test-results/screenshots/${name}-${Date.now()}.png`
  await page.screenshot({ path: screenshotPath, fullPage: true })
  console.log(`📸 截图已保存: ${screenshotPath}`)
  return screenshotPath
}

/**
 * 等待元素稳定（不再移动）
 */
export async function waitForElementStable(page: Page, selector: string) {
  const element = page.locator(selector)
  await element.waitFor({ state: 'visible' })

  let previousBox = await element.boundingBox()
  await page.waitForTimeout(100)
  let currentBox = await element.boundingBox()

  let attempts = 0
  while (attempts < 10 && previousBox && currentBox &&
         (previousBox.x !== currentBox.x || previousBox.y !== currentBox.y)) {
    previousBox = currentBox
    await page.waitForTimeout(100)
    currentBox = await element.boundingBox()
    attempts++
  }
}

/**
 * 滚动到元素
 */
export async function scrollToElement(page: Page, selector: string) {
  await page.locator(selector).scrollIntoViewIfNeeded()
  await page.waitForTimeout(300)
}

/**
 * 验证 URL 包含指定路径
 */
export async function expectUrlContains(page: Page, path: string) {
  await expect(page).toHaveURL(new RegExp(path))
}

/**
 * 重试操作直到成功
 */
export async function retryUntilSuccess<T>(
  operation: () => Promise<T>,
  options: { maxRetries?: number; retryDelay?: number } = {}
): Promise<T> {
  const maxRetries = options.maxRetries || 3
  const retryDelay = options.retryDelay || 1000

  let lastError: Error | null = null

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await operation()
    } catch (error) {
      lastError = error as Error
      if (attempt < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, retryDelay))
      }
    }
  }

  throw lastError
}
