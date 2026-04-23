import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast } from '../utils/test-helpers'

/**
 * Page Object Model - AI 模块页面
 */

export class KnowledgeBasePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/ai/knowledge')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '知识库')
    await expectTableLoaded(this.page)
  }

  async createKnowledgeBase(name: string, description: string) {
    await this.page.click('button:has-text("新建知识库")')
    await fillForm(this.page, { name, description })
    await this.page.click('button:has-text("创建")')
    await expectToast(this.page, '创建成功')
  }

  async uploadDocument(kbName: string, filePath: string) {
    const row = this.page.locator(`tr:has-text("${kbName}")`)
    await row.click()
    await this.page.click('button:has-text("上传文档")')
    await this.page.setInputFiles('input[type="file"]', filePath)
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '上传成功')
  }

  async searchKnowledge(kbName: string, query: string) {
    const row = this.page.locator(`tr:has-text("${kbName}")`)
    await row.locator('button:has-text("搜索")').click()
    await this.page.fill('input[placeholder*="搜索"]', query)
    await this.page.click('button:has-text("搜索")')
    await waitForPageLoad(this.page)
  }
}

export class PromptTemplatePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/ai/prompt-templates')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, 'Prompt 模板')
    await expectTableLoaded(this.page)
  }

  async createTemplate(name: string, content: string, type: string) {
    await this.page.click('button:has-text("新建模板")')
    await fillForm(this.page, { templateName: name, templateContent: content })
    await this.page.click(`label:has-text("${type}")`)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async testTemplate(templateName: string, variables: Record<string, string>) {
    const row = this.page.locator(`tr:has-text("${templateName}")`)
    await row.locator('button:has-text("测试")').click()

    for (const [key, value] of Object.entries(variables)) {
      await this.page.fill(`input[name="${key}"]`, value)
    }

    await this.page.click('button:has-text("渲染")')
    await this.page.waitForSelector('.rendered-result')
  }
}

export class EvolutionPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/ai/evolution')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '知识自进化')
  }

  async triggerEvolution(source: string) {
    await this.page.click('button:has-text("触发进化")')
    await this.page.click(`label:has-text("${source}")`)
    await this.page.click('button:has-text("开始")')
    await expectToast(this.page, '任务已提交')
  }

  async viewEvolutionTask(taskId: number) {
    await this.page.click(`[data-task-id="${taskId}"]`)
    await this.page.waitForSelector('.task-detail')
  }

  async approveEvolution(taskId: number) {
    await this.page.click(`[data-task-id="${taskId}"] button:has-text("审核")`)
    await this.page.click('button:has-text("通过")')
    await expectToast(this.page, '审核通过')
  }
}
