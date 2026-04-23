import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 智能体模块页面
 */

export class AgentListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/agent/list')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '智能体管理')
    await expectTableLoaded(this.page)
  }

  async createAgent(data: { name: string; description: string; type: string }) {
    await this.page.click('button:has-text("新建智能体")')
    await fillForm(this.page, {
      agentName: data.name,
      agentDescription: data.description,
    })
    await selectOption(this.page, '智能体类型', data.type)
    await this.page.click('button:has-text("创建")')
    await expectToast(this.page, '创建成功')
  }

  async searchAgent(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async editAgent(agentName: string, newDescription: string) {
    const row = this.page.locator(`tr:has-text("${agentName}")`)
    await row.locator('button:has-text("编辑")').click()
    await this.page.fill('textarea[name="agentDescription"]', newDescription)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteAgent(agentName: string) {
    const row = this.page.locator(`tr:has-text("${agentName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }

  async openChat(agentName: string) {
    const row = this.page.locator(`tr:has-text("${agentName}")`)
    await row.locator('button:has-text("对话")').click()
    await this.page.waitForURL(/agent\/chat/)
  }
}

export class AgentChatPage {
  constructor(private page: Page) {}

  async goto(agentId: number) {
    await this.page.goto(`/admin/agent/chat?agentId=${agentId}`)
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '智能体对话')
  }

  async sendMessage(message: string) {
    await this.page.fill('textarea[placeholder*="输入消息"]', message)
    await this.page.click('button:has-text("发送")')
    await this.page.waitForSelector('.message-item:last-child', { timeout: 30000 })
  }

  async waitForResponse() {
    await this.page.waitForSelector('.message-item.assistant:last-child', { timeout: 30000 })
  }

  async clearHistory() {
    await this.page.click('button:has-text("清空历史")')
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '清空成功')
  }

  async exportChat() {
    await this.page.click('button:has-text("导出对话")')
    await expectToast(this.page, '导出成功')
  }

  async regenerateResponse() {
    await this.page.click('button:has-text("重新生成")')
    await this.page.waitForSelector('.message-item.assistant:last-child', { timeout: 30000 })
  }
}

// NOTE: AgentConfigPage is commented out because this route doesn't exist in the router
// export class AgentConfigPage {
//   constructor(private page: Page) {}
//
//   async goto(agentId: number) {
//     await this.page.goto(`/admin/agent/config?agentId=${agentId}`)
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '智能体配置')
//   }
//
//   async updateSystemPrompt(prompt: string) {
//     await this.page.fill('textarea[name="systemPrompt"]', prompt)
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async updateTemperature(temperature: number) {
//     await this.page.fill('input[name="temperature"]', String(temperature))
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async addKnowledgeBase(kbName: string) {
//     await this.page.click('button:has-text("添加知识库")')
//     await this.page.fill('input[placeholder*="搜索知识库"]', kbName)
//     await this.page.click(`.kb-search-result:has-text("${kbName}")`)
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '添加成功')
//   }
//
//   async removeKnowledgeBase(kbName: string) {
//     const kb = this.page.locator(`.kb-item:has-text("${kbName}")`)
//     await kb.locator('button[aria-label="删除"]').click()
//     await expectToast(this.page, '删除成功')
//   }
//
//   async enableFunction(functionName: string) {
//     await this.page.click(`label:has-text("${functionName}")`)
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
// }

// NOTE: AgentAnalyticsPage is commented out because this route doesn't exist in the router
// export class AgentAnalyticsPage {
//   constructor(private page: Page) {}
//
//   async goto(agentId: number) {
//     await this.page.goto(`/admin/agent/analytics?agentId=${agentId}`)
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '智能体分析')
//   }
//
//   async selectDateRange(startDate: string, endDate: string) {
//     await this.page.fill('input[name="startDate"]', startDate)
//     await this.page.fill('input[name="endDate"]', endDate)
//     await this.page.click('button:has-text("查询")')
//     await waitForPageLoad(this.page)
//   }
//
//   async viewConversationDetail(conversationId: number) {
//     await this.page.click(`[data-conversation-id="${conversationId}"]`)
//     await this.page.waitForSelector('.conversation-detail')
//   }
//
//   async exportAnalyticsReport() {
//     await this.page.click('button:has-text("导出报告")')
//     await expectToast(this.page, '导出成功')
//   }
// }
