import { test, expect } from '../fixtures/auth.fixture'
import { DouyinAccountPage, DouyinDataSyncPage, DouyinAnalyticsPage, DouyinLiveManagementPage } from '../pages/douyin.page'

/**
 * 抖音模块 E2E 测试
 * 覆盖抖音账号管理、数据同步、数据分析、直播管理等核心功能
 */

test.describe('抖音模块 - 账号管理', () => {
  test('应该能够查看抖音账号列表', async ({ authenticatedPage }) => {
    const accountPage = new DouyinAccountPage(authenticatedPage)

    await accountPage.goto()
    await accountPage.verifyPageLoaded()

    // 验证账号列表加载
    await expect(authenticatedPage.locator('.MuiDataGrid-root')).toBeVisible()
  })

  test('应该能够添加抖音账号', async ({ authenticatedPage }) => {
    const accountPage = new DouyinAccountPage(authenticatedPage)

    await accountPage.goto()

    await accountPage.addAccount()

    // 验证跳转到 OAuth 授权页面
    await expect(authenticatedPage).toHaveURL(/oauth/)
  })

  test('应该能够搜索抖音账号', async ({ authenticatedPage }) => {
    const accountPage = new DouyinAccountPage(authenticatedPage)

    await accountPage.goto()

    await accountPage.searchAccount('测试')

    // 验证搜索结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看账号详情', async ({ authenticatedPage }) => {
    const accountPage = new DouyinAccountPage(authenticatedPage)

    await accountPage.goto()

    // 假设有账号存在
    const firstAccount = await authenticatedPage.locator('.MuiDataGrid-row').first().textContent()
    if (firstAccount) {
      await accountPage.viewAccountDetail(firstAccount)

      // 验证详情显示
      await expect(authenticatedPage.locator('.account-detail')).toBeVisible()
    }
  })

  test('应该能够刷新账号授权', async ({ authenticatedPage }) => {
    const accountPage = new DouyinAccountPage(authenticatedPage)

    await accountPage.goto()

    // 假设有账号存在
    const firstAccount = await authenticatedPage.locator('.MuiDataGrid-row').first().textContent()
    if (firstAccount) {
      await accountPage.refreshToken(firstAccount)

      // 验证刷新成功
      await expect(authenticatedPage.locator('text=刷新成功')).toBeVisible()
    }
  })

  test('应该能够移除抖音账号', async ({ authenticatedPage }) => {
    const accountPage = new DouyinAccountPage(authenticatedPage)

    await accountPage.goto()

    // 假设有测试账号
    const testAccount = '测试账号'
    await accountPage.removeAccount(testAccount)

    // 验证删除成功
    await expect(authenticatedPage.locator('text=删除成功')).toBeVisible()
  })
})

test.describe('抖音模块 - 数据同步', () => {
  test('应该能够同步商品数据', async ({ authenticatedPage }) => {
    const syncPage = new DouyinDataSyncPage(authenticatedPage)

    await syncPage.goto()
    await syncPage.verifyPageLoaded()

    await syncPage.syncProducts('测试账号')

    // 验证同步成功
    await expect(authenticatedPage.locator('text=同步成功')).toBeVisible()
  })

  test('应该能够同步直播数据', async ({ authenticatedPage }) => {
    const syncPage = new DouyinDataSyncPage(authenticatedPage)

    await syncPage.goto()

    await syncPage.syncLiveData('测试账号')

    // 验证同步成功
    await expect(authenticatedPage.locator('text=同步成功')).toBeVisible()
  })

  test('应该能够同步视频数据', async ({ authenticatedPage }) => {
    const syncPage = new DouyinDataSyncPage(authenticatedPage)

    await syncPage.goto()

    await syncPage.syncVideoData('测试账号')

    // 验证同步成功
    await expect(authenticatedPage.locator('text=同步成功')).toBeVisible()
  })

  test('应该能够查看同步历史', async ({ authenticatedPage }) => {
    const syncPage = new DouyinDataSyncPage(authenticatedPage)

    await syncPage.goto()

    await syncPage.viewSyncHistory()

    // 验证同步历史显示
    await expect(authenticatedPage.locator('.sync-history')).toBeVisible()
  })

  test('应该能够设置自动同步', async ({ authenticatedPage }) => {
    const syncPage = new DouyinDataSyncPage(authenticatedPage)

    await syncPage.goto()

    await syncPage.setAutoSync(true, 60)

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够关闭自动同步', async ({ authenticatedPage }) => {
    const syncPage = new DouyinDataSyncPage(authenticatedPage)

    await syncPage.goto()

    await syncPage.setAutoSync(false, 0)

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })
})

test.describe('抖音模块 - 数据分析', () => {
  test('应该能够查看数据分析页面', async ({ authenticatedPage }) => {
    const analyticsPage = new DouyinAnalyticsPage(authenticatedPage)

    await analyticsPage.goto()
    await analyticsPage.verifyPageLoaded()

    // 验证分析页面加载
    await expect(authenticatedPage.locator('.analytics-dashboard, .MuiGrid-root')).toBeVisible()
  })

  test('应该能够选择账号查看数据', async ({ authenticatedPage }) => {
    const analyticsPage = new DouyinAnalyticsPage(authenticatedPage)

    await analyticsPage.goto()

    await analyticsPage.selectAccount('测试账号')

    // 验证数据加载
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够选择日期范围', async ({ authenticatedPage }) => {
    const analyticsPage = new DouyinAnalyticsPage(authenticatedPage)

    await analyticsPage.goto()

    await analyticsPage.selectDateRange('2026-01-01', '2026-01-31')

    // 验证数据更新
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看粉丝分析', async ({ authenticatedPage }) => {
    const analyticsPage = new DouyinAnalyticsPage(authenticatedPage)

    await analyticsPage.goto()

    await analyticsPage.viewFansAnalysis()

    // 验证粉丝图表显示
    await expect(authenticatedPage.locator('.fans-chart')).toBeVisible()
  })

  test('应该能够查看内容分析', async ({ authenticatedPage }) => {
    const analyticsPage = new DouyinAnalyticsPage(authenticatedPage)

    await analyticsPage.goto()

    await analyticsPage.viewContentAnalysis()

    // 验证内容图表显示
    await expect(authenticatedPage.locator('.content-chart')).toBeVisible()
  })

  test('应该能够导出分析报告', async ({ authenticatedPage }) => {
    const analyticsPage = new DouyinAnalyticsPage(authenticatedPage)

    await analyticsPage.goto()

    await analyticsPage.exportReport()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })
})

test.describe('抖音模块 - 直播管理', () => {
  test('应该能够创建直播', async ({ authenticatedPage }) => {
    const livePage = new DouyinLiveManagementPage(authenticatedPage)

    await livePage.goto()
    await livePage.verifyPageLoaded()

    await livePage.createLive({
      accountName: '测试账号',
      title: '测试直播 - ' + Date.now(),
      scheduledTime: '2026-04-10 20:00',
    })

    // 验证创建成功
    await expect(authenticatedPage.locator('text=创建成功')).toBeVisible()
  })

  test('应该能够开始直播', async ({ authenticatedPage }) => {
    const livePage = new DouyinLiveManagementPage(authenticatedPage)

    await livePage.goto()

    // 创建直播
    const liveTitle = '开始测试直播 - ' + Date.now()
    await livePage.createLive({
      accountName: '测试账号',
      title: liveTitle,
      scheduledTime: '2026-04-10 20:00',
    })

    // 开始直播
    await livePage.startLive(liveTitle)

    // 验证直播已开始
    await expect(authenticatedPage.locator('text=直播已开始')).toBeVisible()
  })

  test('应该能够结束直播', async ({ authenticatedPage }) => {
    const livePage = new DouyinLiveManagementPage(authenticatedPage)

    await livePage.goto()

    // 假设有正在进行的直播
    const liveTitle = '进行中的直播'
    await livePage.endLive(liveTitle)

    // 验证直播已结束
    await expect(authenticatedPage.locator('text=直播已结束')).toBeVisible()
  })

  test('应该能够查看直播数据', async ({ authenticatedPage }) => {
    const livePage = new DouyinLiveManagementPage(authenticatedPage)

    await livePage.goto()

    // 假设有直播记录
    const liveTitle = '历史直播'
    await livePage.viewLiveData(liveTitle)

    // 验证数据显示
    await expect(authenticatedPage.locator('.live-data')).toBeVisible()
  })
})
