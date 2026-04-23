import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 话术模块页面
 */

export class ScriptListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/script/list')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '话术管理')
    await expectTableLoaded(this.page)
  }

  async createScript(data: { title: string; content: string; type: string }) {
    await this.page.click('button:has-text("新建话术")')
    await fillForm(this.page, {
      scriptTitle: data.title,
      scriptContent: data.content,
    })
    await selectOption(this.page, '话术类型', data.type)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async searchScript(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async editScript(scriptTitle: string, newContent: string) {
    const row = this.page.locator(`tr:has-text("${scriptTitle}")`)
    await row.locator('button:has-text("编辑")').click()
    await this.page.fill('textarea[name="scriptContent"]', newContent)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteScript(scriptTitle: string) {
    const row = this.page.locator(`tr:has-text("${scriptTitle}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }

  async copyScript(scriptTitle: string) {
    const row = this.page.locator(`tr:has-text("${scriptTitle}")`)
    await row.locator('button:has-text("复制")').click()
    await expectToast(this.page, '复制成功')
  }
}

export class ScriptGenerationPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/script/generate')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '话术生成')
  }

  async generateScript(data: { productName: string; style: string; duration: number }) {
    await this.page.fill('input[name="productName"]', data.productName)
    await selectOption(this.page, '话术风格', data.style)
    await this.page.fill('input[name="duration"]', String(data.duration))
    await this.page.click('button:has-text("生成话术")')
    await this.page.waitForSelector('text=生成完成', { timeout: 30000 })
  }

  async saveGeneratedScript(title: string) {
    await this.page.fill('input[name="scriptTitle"]', title)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async regenerate() {
    await this.page.click('button:has-text("重新生成")')
    await this.page.waitForSelector('text=生成完成', { timeout: 30000 })
  }
}

export class ScriptOptimizationPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/script/optimize')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '话术优化')
  }

  async optimizeScript(scriptContent: string, optimizationType: string) {
    await this.page.fill('textarea[name="originalScript"]', scriptContent)
    await selectOption(this.page, '优化类型', optimizationType)
    await this.page.click('button:has-text("开始优化")')
    await this.page.waitForSelector('text=优化完成', { timeout: 30000 })
  }

  async compareVersions() {
    await this.page.click('button:has-text("对比版本")')
    await this.page.waitForSelector('.version-comparison')
  }

  async applyOptimization() {
    await this.page.click('button:has-text("应用优化")')
    await expectToast(this.page, '应用成功')
  }
}

export class ScriptCompliancePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/system/compliance')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '合规检测')
  }

  async checkCompliance(scriptContent: string) {
    await this.page.fill('textarea[name="scriptContent"]', scriptContent)
    await this.page.click('button:has-text("检测")')
    await this.page.waitForSelector('.compliance-result', { timeout: 10000 })
  }

  async viewViolations() {
    await this.page.click('button:has-text("查看违规项")')
    await this.page.waitForSelector('.violation-list')
  }

  async fixViolation(violationIndex: number) {
    const violation = this.page.locator(`.violation-item:nth-child(${violationIndex})`)
    await violation.locator('button:has-text("修复")').click()
    await expectToast(this.page, '修复成功')
  }
}

export class ScriptTemplatePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/script/templates')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '话术模板')
    await expectTableLoaded(this.page)
  }

  async createTemplate(data: { name: string; content: string; category: string }) {
    await this.page.click('button:has-text("新建模板")')
    await fillForm(this.page, {
      templateName: data.name,
      templateContent: data.content,
    })
    await selectOption(this.page, '模板类目', data.category)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async useTemplate(templateName: string) {
    const row = this.page.locator(`tr:has-text("${templateName}")`)
    await row.locator('button:has-text("使用")').click()
    await this.page.waitForURL(/script\/generate/)
  }

  async editTemplate(templateName: string, newContent: string) {
    const row = this.page.locator(`tr:has-text("${templateName}")`)
    await row.locator('button:has-text("编辑")').click()
    await this.page.fill('textarea[name="templateContent"]', newContent)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteTemplate(templateName: string) {
    const row = this.page.locator(`tr:has-text("${templateName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}
