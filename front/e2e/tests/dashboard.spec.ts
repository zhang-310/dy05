import { test, expect } from '../fixtures/auth.fixture'
import { DashboardPage, ReportPage, PaymentPage, ABTestPage, WorkflowPage } from '../pages/dashboard.page'

/**
 * Dashboard 及其他模块 E2E 测试
 * 覆盖管理驾驶舱、报表、支付、A/B测试、工作流等核心功能
 */

test.describe('Dashboard 模块 - 管理驾驶舱', () => {
  test('应该能够查看管理驾驶舱', async ({ authenticatedPage }) => {
    const dashboardPage = new DashboardPage(authenticatedPage)

    await dashboardPage.goto()
    await dashboardPage.verifyPageLoaded()

    // 验证驾驶舱页面加载
    await expect(authenticatedPage.locator('.dashboard, .MuiGrid-root')).toBeVisible()
  })

  test('应该能够选择日期范围', async ({ authenticatedPage }) => {
    const dashboardPage = new DashboardPage(authenticatedPage)

    await dashboardPage.goto()

    await dashboardPage.selectDateRange('2026-01-01', '2026-01-31')

    // 验证数据更新
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看 KPI 卡片详情', async ({ authenticatedPage }) => {
    const dashboardPage = new DashboardPage(authenticatedPage)

    await dashboardPage.goto()

    await dashboardPage.viewKpiCard('GMV')

    // 验证详情显示
    await expect(authenticatedPage.locator('.kpi-detail')).toBeVisible()
  })

  test('应该能够刷新数据', async ({ authenticatedPage }) => {
    const dashboardPage = new DashboardPage(authenticatedPage)

    await dashboardPage.goto()

    await dashboardPage.refreshData()

    // 验证数据刷新
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够导出驾驶舱数据', async ({ authenticatedPage }) => {
    const dashboardPage = new DashboardPage(authenticatedPage)

    await dashboardPage.goto()

    await dashboardPage.exportDashboard()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })
})

test.describe('报表模块 - 报表中心', () => {
  test('应该能够生成报表', async ({ authenticatedPage }) => {
    const reportPage = new ReportPage(authenticatedPage)

    await reportPage.goto()
    await reportPage.verifyPageLoaded()

    await reportPage.generateReport({
      type: '销售报表',
      startDate: '2026-01-01',
      endDate: '2026-01-31',
    })

    // 验证生成成功
    await expect(authenticatedPage.locator('text=生成成功')).toBeVisible()
  })

  test('应该能够查看报表', async ({ authenticatedPage }) => {
    const reportPage = new ReportPage(authenticatedPage)

    await reportPage.goto()

    // 生成报表
    await reportPage.generateReport({
      type: '直播报表',
      startDate: '2026-01-01',
      endDate: '2026-01-31',
    })

    // 查看报表
    await reportPage.viewReport('直播报表')

    // 验证报表内容显示
    await expect(authenticatedPage.locator('.report-content')).toBeVisible()
  })

  test('应该能够下载报表', async ({ authenticatedPage }) => {
    const reportPage = new ReportPage(authenticatedPage)

    await reportPage.goto()

    // 假设有报表存在
    await reportPage.downloadReport('销售报表')

    // 验证下载成功
    await expect(authenticatedPage.locator('text=下载成功')).toBeVisible()
  })

  test('应该能够删除报表', async ({ authenticatedPage }) => {
    const reportPage = new ReportPage(authenticatedPage)

    await reportPage.goto()

    // 生成临时报表
    await reportPage.generateReport({
      type: '临时报表',
      startDate: '2026-01-01',
      endDate: '2026-01-31',
    })

    // 删除报表
    await reportPage.deleteReport('临时报表')

    // 验证删除成功
    await expect(authenticatedPage.locator('text=删除成功')).toBeVisible()
  })

  test('应该能够设置定时报表', async ({ authenticatedPage }) => {
    const reportPage = new ReportPage(authenticatedPage)

    await reportPage.goto()

    await reportPage.scheduleReport({
      type: '周报',
      frequency: '每周一',
      recipients: ['admin@example.com', 'manager@example.com'],
    })

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })
})

test.describe('支付模块 - 支付管理', () => {
  test('应该能够查看订单列表', async ({ authenticatedPage }) => {
    const paymentPage = new PaymentPage(authenticatedPage)

    await paymentPage.goto()
    await paymentPage.verifyPageLoaded()

    // 验证订单列表加载
    await expect(authenticatedPage.locator('.MuiDataGrid-root')).toBeVisible()
  })

  test('应该能够搜索订单', async ({ authenticatedPage }) => {
    const paymentPage = new PaymentPage(authenticatedPage)

    await paymentPage.goto()

    await paymentPage.searchOrder('ORDER123456')

    // 验证搜索结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够按状态筛选订单', async ({ authenticatedPage }) => {
    const paymentPage = new PaymentPage(authenticatedPage)

    await paymentPage.goto()

    await paymentPage.filterByStatus('已支付')

    // 验证筛选结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看订单详情', async ({ authenticatedPage }) => {
    const paymentPage = new PaymentPage(authenticatedPage)

    await paymentPage.goto()

    // 假设有订单存在
    await paymentPage.viewOrderDetail('ORDER123456')

    // 验证详情显示
    await expect(authenticatedPage.locator('.order-detail')).toBeVisible()
  })

  test('应该能够退款', async ({ authenticatedPage }) => {
    const paymentPage = new PaymentPage(authenticatedPage)

    await paymentPage.goto()

    await paymentPage.refundOrder('ORDER123456', 99.99, '用户申请退款')

    // 验证退款成功
    await expect(authenticatedPage.locator('text=退款成功')).toBeVisible()
  })

  test('应该能够导出订单', async ({ authenticatedPage }) => {
    const paymentPage = new PaymentPage(authenticatedPage)

    await paymentPage.goto()

    await paymentPage.exportOrders()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })
})

test.describe('A/B 测试模块', () => {
  test('应该能够创建 A/B 测试实验', async ({ authenticatedPage }) => {
    const abtestPage = new ABTestPage(authenticatedPage)

    await abtestPage.goto()
    await abtestPage.verifyPageLoaded()

    const experimentName = '测试实验 - ' + Date.now()
    await abtestPage.createExperiment({
      name: experimentName,
      description: '测试不同话术的转化率',
      variants: ['变体A', '变体B', '变体C'],
    })

    // 验证创建成功
    await expect(authenticatedPage.locator('text=创建成功')).toBeVisible()
  })

  test('应该能够开始实验', async ({ authenticatedPage }) => {
    const abtestPage = new ABTestPage(authenticatedPage)

    await abtestPage.goto()

    // 创建实验
    const experimentName = '开始测试实验 - ' + Date.now()
    await abtestPage.createExperiment({
      name: experimentName,
      description: '测试实验',
      variants: ['A', 'B'],
    })

    // 开始实验
    await abtestPage.startExperiment(experimentName)

    // 验证实验已开始
    await expect(authenticatedPage.locator('text=实验已开始')).toBeVisible()
  })

  test('应该能够停止实验', async ({ authenticatedPage }) => {
    const abtestPage = new ABTestPage(authenticatedPage)

    await abtestPage.goto()

    // 假设有正在运行的实验
    await abtestPage.stopExperiment('运行中的实验')

    // 验证实验已停止
    await expect(authenticatedPage.locator('text=实验已停止')).toBeVisible()
  })

  test('应该能够查看实验结果', async ({ authenticatedPage }) => {
    const abtestPage = new ABTestPage(authenticatedPage)

    await abtestPage.goto()

    // 假设有已完成的实验
    await abtestPage.viewExperimentResults('已完成的实验')

    // 验证结果显示
    await expect(authenticatedPage.locator('.experiment-results')).toBeVisible()
  })

  test('应该能够删除实验', async ({ authenticatedPage }) => {
    const abtestPage = new ABTestPage(authenticatedPage)

    await abtestPage.goto()

    // 创建临时实验
    const experimentName = '临时实验 - ' + Date.now()
    await abtestPage.createExperiment({
      name: experimentName,
      description: '临时实验',
      variants: ['A', 'B'],
    })

    // 删除实验
    await abtestPage.deleteExperiment(experimentName)

    // 验证删除成功
    await expect(authenticatedPage.locator('text=删除成功')).toBeVisible()
  })
})

test.describe('工作流模块', () => {
  test('应该能够创建工作流', async ({ authenticatedPage }) => {
    const workflowPage = new WorkflowPage(authenticatedPage)

    await workflowPage.goto()
    await workflowPage.verifyPageLoaded()

    const workflowName = '测试工作流 - ' + Date.now()
    await workflowPage.createWorkflow({
      name: workflowName,
      description: '自动化测试工作流',
      trigger: '直播结束',
    })

    // 验证创建成功
    await expect(authenticatedPage.locator('text=创建成功')).toBeVisible()
  })

  test('应该能够编辑工作流', async ({ authenticatedPage }) => {
    const workflowPage = new WorkflowPage(authenticatedPage)

    await workflowPage.goto()

    // 创建工作流
    const workflowName = '编辑测试工作流 - ' + Date.now()
    await workflowPage.createWorkflow({
      name: workflowName,
      description: '用于编辑测试',
      trigger: '商品上架',
    })

    // 编辑工作流
    await workflowPage.editWorkflow(workflowName)

    // 验证跳转到编辑页面
    await expect(authenticatedPage).toHaveURL(/workflow\/edit/)
  })

  test('应该能够启用工作流', async ({ authenticatedPage }) => {
    const workflowPage = new WorkflowPage(authenticatedPage)

    await workflowPage.goto()

    // 创建工作流
    const workflowName = '启用测试工作流 - ' + Date.now()
    await workflowPage.createWorkflow({
      name: workflowName,
      description: '用于启用测试',
      trigger: '订单支付',
    })

    // 启用工作流
    await workflowPage.enableWorkflow(workflowName)

    // 验证启用成功
    await expect(authenticatedPage.locator('text=启用成功')).toBeVisible()
  })

  test('应该能够禁用工作流', async ({ authenticatedPage }) => {
    const workflowPage = new WorkflowPage(authenticatedPage)

    await workflowPage.goto()

    // 假设有已启用的工作流
    await workflowPage.disableWorkflow('已启用的工作流')

    // 验证禁用成功
    await expect(authenticatedPage.locator('text=禁用成功')).toBeVisible()
  })

  test('应该能够查看工作流历史', async ({ authenticatedPage }) => {
    const workflowPage = new WorkflowPage(authenticatedPage)

    await workflowPage.goto()

    // 假设有工作流存在
    await workflowPage.viewWorkflowHistory('测试工作流')

    // 验证历史显示
    await expect(authenticatedPage.locator('.workflow-history')).toBeVisible()
  })

  test('应该能够删除工作流', async ({ authenticatedPage }) => {
    const workflowPage = new WorkflowPage(authenticatedPage)

    await workflowPage.goto()

    // 创建临时工作流
    const workflowName = '临时工作流 - ' + Date.now()
    await workflowPage.createWorkflow({
      name: workflowName,
      description: '临时工作流',
      trigger: '用户注册',
    })

    // 删除工作流
    await workflowPage.deleteWorkflow(workflowName)

    // 验证删除成功
    await expect(authenticatedPage.locator('text=删除成功')).toBeVisible()
  })
})
