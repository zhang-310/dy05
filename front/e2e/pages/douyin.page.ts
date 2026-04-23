import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 抖音账号模块页面
 */

export class DouyinAccountPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/douyin/accounts')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '抖音账号')
    await expectTableLoaded(this.page)
  }

  async addAccount() {
    await this.page.click('button:has-text("添加账号")')
    await this.page.waitForURL(/oauth/)
  }

  async searchAccount(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async viewAccountDetail(accountName: string) {
    const row = this.page.locator(`tr:has-text("${accountName}")`)
    await row.locator('button:has-text("详情")').click()
    await this.page.waitForSelector('.account-detail')
  }

  async refreshToken(accountName: string) {
    const row = this.page.locator(`tr:has-text("${accountName}")`)
    await row.locator('button:has-text("刷新授权")').click()
    await expectToast(this.page, '刷新成功')
  }

  async removeAccount(accountName: string) {
    const row = this.page.locator(`tr:has-text("${accountName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}

// NOTE: DouyinDataSyncPage is commented out because this route doesn't exist in the router
// export class DouyinDataSyncPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/douyin/sync')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '数据同步')
//   }
//
//   async syncProducts(accountName: string) {
//     await selectOption(this.page, '抖音账号', accountName)
//     await this.page.click('button:has-text("同步商品")')
//     await expectToast(this.page, '同步成功')
//   }
//
//   async syncLiveData(accountName: string) {
//     await selectOption(this.page, '抖音账号', accountName)
//     await this.page.click('button:has-text("同步直播数据")')
//     await expectToast(this.page, '同步成功')
//   }
//
//   async syncVideoData(accountName: string) {
//     await selectOption(this.page, '抖音账号', accountName)
//     await this.page.click('button:has-text("同步视频数据")')
//     await expectToast(this.page, '同步成功')
//   }
//
//   async viewSyncHistory() {
//     await this.page.click('button:has-text("同步历史")')
//     await this.page.waitForSelector('.sync-history')
//   }
//
//   async setAutoSync(enabled: boolean, interval: number) {
//     if (enabled) {
//       await this.page.click('label:has-text("启用自动同步")')
//       await this.page.fill('input[name="syncInterval"]', String(interval))
//     } else {
//       await this.page.click('label:has-text("启用自动同步")')
//     }
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
// }

// NOTE: DouyinAnalyticsPage is commented out because this route doesn't exist in the router
// export class DouyinAnalyticsPage {
//   constructor(private page: Page)
//
//   async goto() {
//     await this.page.goto('/admin/douyin/analytics')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '抖音数据分析')
//   }
//
//   async selectAccount(accountName: string) {
//     await selectOption(this.page, '抖音账号', accountName)
//     await waitForPageLoad(this.page)
//   }
//
//   async selectDateRange(startDate: string, endDate: string) {
//     await this.page.fill('input[name="startDate"]', startDate)
//     await this.page.fill('input[name="endDate"]', endDate)
//     await this.page.click('button:has-text("查询")')
//     await waitForPageLoad(this.page)
//   }
//
//   async viewFansAnalysis() {
//     await this.page.click('button:has-text("粉丝分析")')
//     await this.page.waitForSelector('.fans-chart')
//   }
//
//   async viewContentAnalysis() {
//     await this.page.click('button:has-text("内容分析")')
//     await this.page.waitForSelector('.content-chart')
//   }
//
//   async exportReport() {
//     await this.page.click('button:has-text("导出报告")')
//     await expectToast(this.page, '导出成功')
//   }
// }

// NOTE: DouyinLiveManagementPage is commented out because this route doesn't exist in the router
// export class DouyinLiveManagementPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/douyin/live-management')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '直播管理')
//     await expectTableLoaded(this.page)
//   }
//
//   async createLive(data: { accountName: string; title: string; scheduledTime: string }) {
//     await this.page.click('button:has-text("创建直播")')
//     await selectOption(this.page, '抖音账号', data.accountName)
//     await fillForm(this.page, {
//       liveTitle: data.title,
//       scheduledTime: data.scheduledTime,
//     })
//     await this.page.click('button:has-text("创建")')
//     await expectToast(this.page, '创建成功')
//   }
//
//   async startLive(liveTitle: string) {
//     const row = this.page.locator(`tr:has-text("${liveTitle}")`)
//     await row.locator('button:has-text("开始直播")').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '直播已开始')
//   }
//
//   async endLive(liveTitle: string) {
//     const row = this.page.locator(`tr:has-text("${liveTitle}")`)
//     await row.locator('button:has-text("结束直播")').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '直播已结束')
//   }
//
//   async viewLiveData(liveTitle: string) {
//     const row = this.page.locator(`tr:has-text("${liveTitle}")`)
//     await row.locator('button:has-text("数据")').click()
//     await this.page.waitForSelector('.live-data')
//   }
// }
