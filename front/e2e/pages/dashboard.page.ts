import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - Dashboard 模块页面
 */

export class DashboardPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/dashboard')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '管理驾驶舱')
  }

  async selectDateRange(startDate: string, endDate: string) {
    await this.page.fill('input[name="startDate"]', startDate)
    await this.page.fill('input[name="endDate"]', endDate)
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async viewKpiCard(kpiName: string) {
    await this.page.click(`.kpi-card:has-text("${kpiName}")`)
    await this.page.waitForSelector('.kpi-detail')
  }

  async refreshData() {
    await this.page.click('button:has-text("刷新")')
    await waitForPageLoad(this.page)
  }

  async exportDashboard() {
    await this.page.click('button:has-text("导出")')
    await expectToast(this.page, '导出成功')
  }
}

// NOTE: ReportPage is commented out because this route doesn't exist in the router
// export class ReportPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/report')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '报表中心')
//     await expectTableLoaded(this.page)
//   }
//
//   async generateReport(data: { type: string; startDate: string; endDate: string }) {
//     await this.page.click('button:has-text("生成报表")')
//     await selectOption(this.page, '报表类型', data.type)
//     await fillForm(this.page, {
//       startDate: data.startDate,
//       endDate: data.endDate,
//     })
//     await this.page.click('button:has-text("生成")')
//     await expectToast(this.page, '生成成功')
//   }
//
//   async viewReport(reportName: string) {
//     const row = this.page.locator(`tr:has-text("${reportName}")`)
//     await row.locator('button:has-text("查看")').click()
//     await this.page.waitForSelector('.report-content')
//   }
//
//   async downloadReport(reportName: string) {
//     const row = this.page.locator(`tr:has-text("${reportName}")`)
//     await row.locator('button:has-text("下载")').click()
//     await expectToast(this.page, '下载成功')
//   }
//
//   async deleteReport(reportName: string) {
//     const row = this.page.locator(`tr:has-text("${reportName}")`)
//     await row.locator('button[aria-label="删除"]').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '删除成功')
//   }
//
//   async scheduleReport(data: { type: string; frequency: string; recipients: string[] }) {
//     await this.page.click('button:has-text("定时报表")')
//     await selectOption(this.page, '报表类型', data.type)
//     await selectOption(this.page, '频率', data.frequency)
//
//     for (const recipient of data.recipients) {
//       await this.page.fill('input[placeholder*="添加收件人"]', recipient)
//       await this.page.press('input[placeholder*="添加收件人"]', 'Enter')
//     }
//
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
// }

export class PaymentPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/payment/orders')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '订单管理')
    await expectTableLoaded(this.page)
  }

  async searchOrder(orderId: string) {
    await this.page.fill('input[placeholder*="订单号"]', orderId)
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async filterByStatus(status: string) {
    await selectOption(this.page, '订单状态', status)
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async viewOrderDetail(orderId: string) {
    const row = this.page.locator(`tr:has-text("${orderId}")`)
    await row.locator('button:has-text("详情")').click()
    await this.page.waitForSelector('.order-detail')
  }

  async refundOrder(orderId: string, amount: number, reason: string) {
    const row = this.page.locator(`tr:has-text("${orderId}")`)
    await row.locator('button:has-text("退款")').click()
    await this.page.fill('input[name="refundAmount"]', String(amount))
    await this.page.fill('textarea[name="refundReason"]', reason)
    await this.page.click('button:has-text("确认退款")')
    await expectToast(this.page, '退款成功')
  }

  async exportOrders() {
    await this.page.click('button:has-text("导出订单")')
    await expectToast(this.page, '导出成功')
  }
}

// NOTE: ABTestPage is commented out because this route doesn't exist in the router
// The router has /admin/abtest/experiments instead
// export class ABTestPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/abtest')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, 'A/B 测试')
//     await expectTableLoaded(this.page)
//   }
//
//   async createExperiment(data: { name: string; description: string; variants: string[] }) {
//     await this.page.click('button:has-text("新建实验")')
//     await fillForm(this.page, {
//       experimentName: data.name,
//       experimentDescription: data.description,
//     })
//
//     for (const variant of data.variants) {
//       await this.page.click('button:has-text("添加变体")')
//       await this.page.fill('input[name="variantName"]', variant)
//     }
//
//     await this.page.click('button:has-text("创建")')
//     await expectToast(this.page, '创建成功')
//   }
//
//   async startExperiment(experimentName: string) {
//     const row = this.page.locator(`tr:has-text("${experimentName}")`)
//     await row.locator('button:has-text("开始")').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '实验已开始')
//   }
//
//   async stopExperiment(experimentName: string) {
//     const row = this.page.locator(`tr:has-text("${experimentName}")`)
//     await row.locator('button:has-text("停止")').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '实验已停止')
//   }
//
//   async viewExperimentResults(experimentName: string) {
//     const row = this.page.locator(`tr:has-text("${experimentName}")`)
//     await row.locator('button:has-text("结果")').click()
//     await this.page.waitForSelector('.experiment-results')
//   }
//
//   async deleteExperiment(experimentName: string) {
//     const row = this.page.locator(`tr:has-text("${experimentName}")`)
//     await row.locator('button[aria-label="删除"]').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '删除成功')
//   }
// }

// NOTE: WorkflowPage is commented out because this route doesn't exist in the router
// export class WorkflowPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/workflow')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '工作流')
//     await expectTableLoaded(this.page)
//   }
//
//   async createWorkflow(data: { name: string; description: string; trigger: string }) {
//     await this.page.click('button:has-text("新建工作流")')
//     await fillForm(this.page, {
//       workflowName: data.name,
//       workflowDescription: data.description,
//     })
//     await selectOption(this.page, '触发器', data.trigger)
//     await this.page.click('button:has-text("创建")')
//     await expectToast(this.page, '创建成功')
//   }
//
//   async editWorkflow(workflowName: string) {
//     const row = this.page.locator(`tr:has-text("${workflowName}")`)
//     await row.locator('button:has-text("编辑")').click()
//     await this.page.waitForURL(/workflow\/edit/)
//   }
//
//   async enableWorkflow(workflowName: string) {
//     const row = this.page.locator(`tr:has-text("${workflowName}")`)
//     await row.locator('button:has-text("启用")').click()
//     await expectToast(this.page, '启用成功')
//   }
//
//   async disableWorkflow(workflowName: string) {
//     const row = this.page.locator(`tr:has-text("${workflowName}")`)
//     await row.locator('button:has-text("禁用")').click()
//     await expectToast(this.page, '禁用成功')
//   }
//
//   async viewWorkflowHistory(workflowName: string) {
//     const row = this.page.locator(`tr:has-text("${workflowName}")`)
//     await row.locator('button:has-text("历史")').click()
//     await this.page.waitForSelector('.workflow-history')
//   }
//
//   async deleteWorkflow(workflowName: string) {
//     const row = this.page.locator(`tr:has-text("${workflowName}")`)
//     await row.locator('button[aria-label="删除"]').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '删除成功')
//   }
// }
