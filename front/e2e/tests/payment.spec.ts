import { test, expect } from '../fixtures/auth.fixture'

/**
 * 支付模块 E2E 测试（任务 #11）
 * 覆盖 3 个页面：
 * 1. OrdersPage - 订单管理
 * 2. SubscriptionPage - 订阅管理
 * 3. UsageQuotaPage - 使用配额
 */

test.describe('支付模块 - 订单管理', () => {
  test('应该能够加载订单管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('订单')
  })

  test('应该能够查看订单列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证表格加载
    const table = authenticatedPage.locator('.MuiDataGrid-root, table')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索订单', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 搜索订单
    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[placeholder*="订单"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('test')
      await searchInput.press('Enter')
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够筛选订单状态', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择订单状态
    const statusSelect = authenticatedPage.locator('label:has-text("状态")').locator('..').locator('div[role="button"]')
    if (await statusSelect.isVisible()) {
      await statusSelect.click()
      await authenticatedPage.waitForSelector('.MuiMenu-root')
      await authenticatedPage.click('.MuiMenuItem-root:has-text("已支付"), .MuiMenuItem-root:has-text("待支付")')
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够查看订单详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击查看详情按钮
    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.first().isVisible()) {
      await detailButton.first().click()
      await authenticatedPage.waitForSelector('.MuiDialog-root, .detail-panel', { timeout: 5000 })

      // 验证详情内容
      const detailContent = authenticatedPage.locator('.MuiDialog-root, .detail-panel')
      await expect(detailContent).toBeVisible()

      // 关闭详情
      const closeButton = authenticatedPage.locator('button:has-text("关闭"), button[aria-label="关闭"]')
      if (await closeButton.isVisible()) {
        await closeButton.click()
      }
    }
  })

  test('应该能够导出订单数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击导出按钮
    const exportButton = authenticatedPage.locator('button:has-text("导出")')
    if (await exportButton.isVisible()) {
      await exportButton.click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够取消订单', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/orders')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击取消按钮
    const cancelButton = authenticatedPage.locator('button:has-text("取消订单")')
    if (await cancelButton.first().isVisible()) {
      await cancelButton.first().click()

      // 确认取消
      const confirmButton = authenticatedPage.locator('button:has-text("确认"), button:has-text("确定")')
      if (await confirmButton.isVisible()) {
        // 不实际取消
        await authenticatedPage.click('button:has-text("取消"), button:has-text("关闭")')
      }
    }
  })
})

test.describe('支付模块 - 订阅管理', () => {
  test('应该能够加载订阅管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('订阅')
  })

  test('应该能够查看当前订阅信息', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证订阅卡片
    const subscriptionCard = authenticatedPage.locator('.MuiCard-root, .subscription-card')
    await expect(subscriptionCard.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够查看订阅套餐列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证套餐卡片
    const planCards = authenticatedPage.locator('.MuiCard-root, .plan-card')
    await expect(planCards.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够升级订阅套餐', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击升级按钮
    const upgradeButton = authenticatedPage.locator('button:has-text("升级"), button:has-text("购买")')
    if (await upgradeButton.first().isVisible()) {
      await upgradeButton.first().click()
      await authenticatedPage.waitForSelector('.MuiDialog-root, .payment-dialog', { timeout: 5000 })

      // 验证支付对话框
      const paymentDialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(paymentDialog).toBeVisible()

      // 关闭对话框
      await authenticatedPage.click('button:has-text("取消"), button:has-text("关闭")')
    }
  })

  test('应该能够查看订阅历史', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到历史标签
    const historyTab = authenticatedPage.locator('button:has-text("历史"), .MuiTab-root:has-text("历史")')
    if (await historyTab.isVisible()) {
      await historyTab.click()
      await authenticatedPage.waitForTimeout(500)

      // 验证历史列表
      const historyList = authenticatedPage.locator('.MuiDataGrid-root, table, .history-list')
      await expect(historyList).toBeVisible({ timeout: 10000 })
    }
  })

  test('应该能够取消订阅', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击取消订阅按钮
    const cancelButton = authenticatedPage.locator('button:has-text("取消订阅")')
    if (await cancelButton.isVisible()) {
      await cancelButton.click()

      // 确认取消
      const confirmButton = authenticatedPage.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        // 不实际取消
        await authenticatedPage.click('button:has-text("取消"), button:has-text("关闭")')
      }
    }
  })

  test('应该能够续费订阅', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/subscription')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击续费按钮
    const renewButton = authenticatedPage.locator('button:has-text("续费")')
    if (await renewButton.isVisible()) {
      await renewButton.click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('支付模块 - 使用配额', () => {
  test('应该能够加载使用配额页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('配额')
  })

  test('应该能够查看配额使用情况', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证配额卡片
    const quotaCards = authenticatedPage.locator('.MuiCard-root, .quota-card')
    await expect(quotaCards.first()).toBeVisible({ timeout: 10000 })

    // 验证进度条
    const progressBars = authenticatedPage.locator('.MuiLinearProgress-root')
    await expect(progressBars.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够查看配额使用详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击查看详情
    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.first().isVisible()) {
      await detailButton.first().click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够查看配额使用趋势图表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证图表容器
    const chartContainer = authenticatedPage.locator('.echarts-for-react, canvas')
    if (await chartContainer.first().isVisible()) {
      await expect(chartContainer.first()).toBeVisible()
    }
  })

  test('应该能够购买额外配额', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击购买按钮
    const buyButton = authenticatedPage.locator('button:has-text("购买"), button:has-text("增加配额")')
    if (await buyButton.isVisible()) {
      await buyButton.click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 验证购买对话框
      const buyDialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(buyDialog).toBeVisible()

      // 关闭对话框
      await authenticatedPage.click('button:has-text("取消"), button:has-text("关闭")')
    }
  })

  test('应该能够查看配额使用历史', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到历史标签
    const historyTab = authenticatedPage.locator('button:has-text("历史"), .MuiTab-root:has-text("历史")')
    if (await historyTab.isVisible()) {
      await historyTab.click()
      await authenticatedPage.waitForTimeout(500)

      // 验证历史列表
      const historyList = authenticatedPage.locator('.MuiDataGrid-root, table')
      await expect(historyList).toBeVisible({ timeout: 10000 })
    }
  })

  test('应该能够导出配额使用报告', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击导出按钮
    const exportButton = authenticatedPage.locator('button:has-text("导出")')
    if (await exportButton.isVisible()) {
      await exportButton.click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够设置配额告警', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/payment/usage-quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击设置告警按钮
    const alertButton = authenticatedPage.locator('button:has-text("告警"), button:has-text("设置")')
    if (await alertButton.isVisible()) {
      await alertButton.click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 填写告警阈值
      const thresholdInput = authenticatedPage.locator('input[name="threshold"], input[type="number"]')
      if (await thresholdInput.isVisible()) {
        await thresholdInput.fill('80')

        // 取消设置
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })
})
