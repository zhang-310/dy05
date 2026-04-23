import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, expectDialogOpen, closeDialog, fillForm, expectToast } from '../utils/test-helpers'

/**
 * Page Object Model - Live 模块页面
 */

export class LiveSessionListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/live/sessions')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '直播场次')
    await expectTableLoaded(this.page)
  }

  async clickCreateButton() {
    await this.page.click('button:has-text("新建场次")')
    await expectDialogOpen(this.page, '新建直播场次')
  }

  async fillSessionForm(data: { title: string; description?: string; scheduledTime?: string }) {
    await fillForm(this.page, {
      liveTitle: data.title,
      liveDescription: data.description || '',
    })
  }

  async submitForm() {
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async searchSession(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async deleteSession(sessionTitle: string) {
    const row = this.page.locator(`tr:has-text("${sessionTitle}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}

export class LiveScriptPage {
  constructor(private page: Page) {}

  async goto(sessionId: number) {
    await this.page.goto(`/admin/live/scripts?sessionId=${sessionId}`)
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '话术管理')
  }

  async addScript(type: string, content: string) {
    await this.page.click(`button:has-text("添加${type}"`)
    await this.page.fill('textarea[name="scriptContent"]', content)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async editScript(scriptId: number, newContent: string) {
    await this.page.click(`[data-script-id="${scriptId}"] button:has-text("编辑")`)
    await this.page.fill('textarea[name="scriptContent"]', newContent)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async generateScript(productName: string) {
    await this.page.click('button:has-text("AI 生成")')
    await this.page.fill('input[name="productName"]', productName)
    await this.page.click('button:has-text("生成")')
    await this.page.waitForSelector('text=生成完成', { timeout: 30000 })
  }
}

export class LiveProductPage {
  constructor(private page: Page) {}

  async goto(sessionId: number) {
    await this.page.goto(`/admin/live/products?sessionId=${sessionId}`)
    await waitForPageLoad(this.page)
  }

  async addProduct(productName: string) {
    await this.page.click('button:has-text("添加商品")')
    await this.page.fill('input[placeholder*="搜索商品"]', productName)
    await this.page.waitForSelector('.product-search-result')
    await this.page.click(`.product-search-result:has-text("${productName}")`)
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '添加成功')
  }

  async setProductType(productName: string, type: string) {
    const row = this.page.locator(`tr:has-text("${productName}")`)
    await row.locator('.product-type-selector').click()
    await this.page.click(`.MuiMenuItem-root:has-text("${type}")`)
  }

  async removeProduct(productName: string) {
    const row = this.page.locator(`tr:has-text("${productName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}
