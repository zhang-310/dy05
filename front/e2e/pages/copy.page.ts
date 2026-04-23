import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 文案模块页面
 */

export class CopyLibraryPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/copy/library')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '文案库')
    await expectTableLoaded(this.page)
  }

  async createCopy(data: { title: string; content: string; category: string }) {
    await this.page.click('button:has-text("新建文案")')
    await fillForm(this.page, {
      copyTitle: data.title,
      copyContent: data.content,
    })
    await selectOption(this.page, '文案类目', data.category)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async searchCopy(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async editCopy(copyTitle: string, newContent: string) {
    const row = this.page.locator(`tr:has-text("${copyTitle}")`)
    await row.locator('button:has-text("编辑")').click()
    await this.page.fill('textarea[name="copyContent"]', newContent)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteCopy(copyTitle: string) {
    const row = this.page.locator(`tr:has-text("${copyTitle}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }

  async copyCopyToClipboard(copyTitle: string) {
    const row = this.page.locator(`tr:has-text("${copyTitle}")`)
    await row.locator('button:has-text("复制")').click()
    await expectToast(this.page, '复制成功')
  }

  async filterByCategory(category: string) {
    await selectOption(this.page, '文案类目', category)
    await waitForPageLoad(this.page)
  }
}

// NOTE: CopyTemplatePage is commented out because this route doesn't exist in the router
// export class CopyTemplatePage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/copy/template')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '文案模板')
//     await expectTableLoaded(this.page)
//   }
//
//   async createTemplate(data: { name: string; content: string; variables: string[] }) {
//     await this.page.click('button:has-text("新建模板")')
//     await fillForm(this.page, {
//       templateName: data.name,
//       templateContent: data.content,
//     })
//
//     // 添加变量
//     for (const variable of data.variables) {
//       await this.page.fill('input[placeholder*="添加变量"]', variable)
//       await this.page.press('input[placeholder*="添加变量"]', 'Enter')
//     }
//
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async useTemplate(templateName: string, variables: Record<string, string>) {
//     const row = this.page.locator(`tr:has-text("${templateName}")`)
//     await row.locator('button:has-text("使用")').click()
//
//     // 填充变量
//     for (const [key, value] of Object.entries(variables)) {
//       await this.page.fill(`input[name="${key}"]`, value)
//     }
//
//     await this.page.click('button:has-text("生成")')
//     await this.page.waitForSelector('.generated-copy')
//   }
//
//   async editTemplate(templateName: string, newContent: string) {
//     const row = this.page.locator(`tr:has-text("${templateName}")`)
//     await row.locator('button:has-text("编辑")').click()
//     await this.page.fill('textarea[name="templateContent"]', newContent)
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async deleteTemplate(templateName: string) {
//     const row = this.page.locator(`tr:has-text("${templateName}")`)
//     await row.locator('button[aria-label="删除"]').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '删除成功')
//   }
// }

// NOTE: CopyGenerationPage is commented out because this route doesn't exist in the router
// export class CopyGenerationPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/copy/generate')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '文案生成')
//   }
//
//   async generateCopy(data: { theme: string; style: string; length: number }) {
//     await this.page.fill('input[name="theme"]', data.theme)
//     await selectOption(this.page, '文案风格', data.style)
//     await this.page.fill('input[name="length"]', String(data.length))
//     await this.page.click('button:has-text("生成文案")')
//     await this.page.waitForSelector('text=生成完成', { timeout: 30000 })
//   }
//
//   async saveGeneratedCopy(title: string) {
//     await this.page.fill('input[name="copyTitle"]', title)
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async regenerate() {
//     await this.page.click('button:has-text("重新生成")')
//     await this.page.waitForSelector('text=生成完成', { timeout: 30000 })
//   }
//
//   async adjustTone(tone: string) {
//     await selectOption(this.page, '语气调整', tone)
//     await this.page.click('button:has-text("应用")')
//   }
// }

// NOTE: CopyAnalysisPage is commented out because this route doesn't exist in the router
// export class CopyAnalysisPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/copy/analysis')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '文案分析')
//   }
//
//   async analyzeCopy(copyContent: string) {
//     await this.page.fill('textarea[name="copyContent"]', copyContent)
//     await this.page.click('button:has-text("分析")')
//     await this.page.waitForSelector('.analysis-result', { timeout: 10000 })
//   }
//
//   async viewEmotionAnalysis() {
//     await this.page.click('button:has-text("情感分析")')
//     await this.page.waitForSelector('.emotion-chart')
//   }
//
//   async viewKeywordAnalysis() {
//     await this.page.click('button:has-text("关键词分析")')
//     await this.page.waitForSelector('.keyword-list')
//   }
//
//   async exportAnalysisReport() {
//     await this.page.click('button:has-text("导出报告")')
//     await expectToast(this.page, '导出成功')
//   }
// }
