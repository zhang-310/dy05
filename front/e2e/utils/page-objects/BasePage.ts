import { Page } from '@playwright/test'

/**
 * 页面对象基类
 * 提供通用的页面操作方法
 */
export class BasePage {
  constructor(protected page: Page) {}

  /**
   * 导航到指定路径
   */
  async goto(path: string) {
    await this.page.goto(path)
    await this.page.waitForLoadState('networkidle')
  }

  /**
   * 获取页面标题
   */
  async getPageTitle() {
    const title = this.page.locator('h1, h2, .page-title, .MuiTypography-h4')
    return await title.first().textContent()
  }

  /**
   * 点击按钮
   */
  async clickButton(text: string) {
    await this.page.click(`button:has-text("${text}")`)
  }

  /**
   * 填写输入框
   */
  async fillInput(name: string, value: string) {
    await this.page.fill(`input[name="${name}"], textarea[name="${name}"]`, value)
  }

  /**
   * 等待加载完成
   */
  async waitForLoading() {
    await this.page.waitForSelector('.MuiCircularProgress-root, .loading', { state: 'hidden', timeout: 30000 }).catch(() => {})
  }

  /**
   * 验证 Toast 消息
   */
  async expectToast(message: string) {
    const toast = this.page.locator('.notistack-snackbar')
    await toast.waitFor({ state: 'visible', timeout: 10000 })
    return toast.filter({ hasText: message })
  }

  /**
   * 打开对话框
   */
  async openDialog(buttonText: string) {
    await this.clickButton(buttonText)
    await this.page.waitForSelector('.MuiDialog-root', { state: 'visible' })
  }

  /**
   * 关闭对话框
   */
  async closeDialog() {
    const closeButton = this.page.locator('.MuiDialog-root button:has-text("取消"), .MuiDialog-root button:has-text("关闭")')
    await closeButton.first().click()
    await this.page.waitForSelector('.MuiDialog-root', { state: 'hidden' })
  }

  /**
   * 搜索
   */
  async search(keyword: string) {
    const searchInput = this.page.locator('input[placeholder*="搜索"]')
    await searchInput.fill(keyword)
    await searchInput.press('Enter')
    await this.waitForLoading()
  }

  /**
   * 选择下拉选项
   */
  async selectOption(label: string, option: string) {
    await this.page.click(`label:has-text("${label}") + div`)
    await this.page.waitForSelector('.MuiMenu-root', { state: 'visible' })
    await this.page.click(`.MuiMenuItem-root:has-text("${option}")`)
    await this.page.waitForSelector('.MuiMenu-root', { state: 'hidden' })
  }

  /**
   * 获取表格行数
   */
  async getTableRowCount() {
    return await this.page.locator('.MuiDataGrid-row').count()
  }

  /**
   * 点击表格行操作
   */
  async clickRowAction(rowText: string, actionText: string) {
    const row = this.page.locator('.MuiDataGrid-row').filter({ hasText: rowText })
    await row.hover()
    await row.locator(`button:has-text("${actionText}")`).click()
  }

  /**
   * 翻页
   */
  async goToNextPage() {
    const nextButton = this.page.locator('button[aria-label="下一页"]')
    if (await nextButton.isEnabled()) {
      await nextButton.click()
      await this.waitForLoading()
    }
  }

  /**
   * 批量选择
   */
  async selectRows(count: number) {
    const checkboxes = this.page.locator('.MuiDataGrid-row .MuiCheckbox-root')
    const total = await checkboxes.count()
    const selectCount = Math.min(count, total)

    for (let i = 0; i < selectCount; i++) {
      await checkboxes.nth(i).click()
      await this.page.waitForTimeout(200)
    }

    return selectCount
  }
}
