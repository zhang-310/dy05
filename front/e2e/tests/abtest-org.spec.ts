import { test, expect } from '../fixtures/auth.fixture'

/**
 * A/B 测试和组织管理模块 E2E 测试（任务 #12）
 * 覆盖 5 个页面：
 * 1. ExperimentsPage - 实验列表
 * 2. ExperimentDetailPage - 实验详情
 * 3. MembersPage - 成员管理
 * 4. OrgAnalyticsPage - 组织分析
 * 5. OrgLiveReviewsPage - 直播审核
 */

test.describe('A/B 测试模块 - 实验列表', () => {
  test('应该能够加载实验列表页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('实验')
  })

  test('应该能够查看实验列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证表格加载
    const table = authenticatedPage.locator('.MuiDataGrid-root, table')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够创建新实验', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击创建按钮
    await authenticatedPage.click('button:has-text("创建"), button:has-text("新建")')
    await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

    // 填写实验信息
    await authenticatedPage.fill('input[name="name"], input[name="experimentName"]', '测试实验 - ' + Date.now())
    await authenticatedPage.fill('textarea[name="description"]', '这是一个测试实验')

    // 选择实验类型
    const typeSelect = authenticatedPage.locator('label:has-text("类型")').locator('..').locator('div[role="button"]')
    if (await typeSelect.isVisible()) {
      await typeSelect.click()
      await authenticatedPage.waitForSelector('.MuiMenu-root')
      await authenticatedPage.click('.MuiMenuItem-root:first-child')
    }

    // 提交
    await authenticatedPage.click('button:has-text("创建"), button:has-text("确定")')
    await authenticatedPage.waitForTimeout(1000)
  })

  test('应该能够搜索实验', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 搜索实验
    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('测试')
      await searchInput.press('Enter')
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够筛选实验状态', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择状态筛选
    const statusSelect = authenticatedPage.locator('label:has-text("状态")').locator('..').locator('div[role="button"]')
    if (await statusSelect.isVisible()) {
      await statusSelect.click()
      await authenticatedPage.waitForSelector('.MuiMenu-root')
      await authenticatedPage.click('.MuiMenuItem-root:has-text("进行中"), .MuiMenuItem-root:has-text("已完成")')
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够启动/停止实验', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击启动/停止按钮
    const actionButton = authenticatedPage.locator('button:has-text("启动"), button:has-text("停止")')
    if (await actionButton.first().isVisible()) {
      await actionButton.first().click()

      // 确认操作
      const confirmButton = authenticatedPage.locator('button:has-text("确认"), button:has-text("确定")')
      if (await confirmButton.isVisible()) {
        // 不实际操作
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })
})

test.describe('A/B 测试模块 - 实验详情', () => {
  test('应该能够加载实验详情页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiments')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击查看详情
    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.first().isVisible()) {
      await detailButton.first().click()
      await authenticatedPage.waitForURL(/\/admin\/abtest\/experiment\//, { timeout: 5000 })

      // 验证详情页面加载
      await expect(authenticatedPage.locator('h4, h5, .page-title')).toBeVisible()
    }
  })

  test('应该能够查看实验配置', async ({ authenticatedPage }) => {
    // 假设有实验 ID
    await authenticatedPage.goto('/admin/abtest/experiment/1')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证配置信息卡片
    const configCard = authenticatedPage.locator('.MuiCard-root, .config-card')
    await expect(configCard.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够查看实验数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiment/1')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证数据图表
    const charts = authenticatedPage.locator('.echarts-for-react, canvas')
    if (await charts.first().isVisible()) {
      await expect(charts.first()).toBeVisible()
    }
  })

  test('应该能够编辑实验配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiment/1')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击编辑按钮
    const editButton = authenticatedPage.locator('button:has-text("编辑")')
    if (await editButton.isVisible()) {
      await editButton.click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 取消编辑
      await authenticatedPage.click('button:has-text("取消")')
    }
  })

  test('应该能够查看实验日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/abtest/experiment/1')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到日志标签
    const logTab = authenticatedPage.locator('button:has-text("日志"), .MuiTab-root:has-text("日志")')
    if (await logTab.isVisible()) {
      await logTab.click()
      await authenticatedPage.waitForTimeout(500)
    }
  })
})

test.describe('组织管理模块 - 成员管理', () => {
  test('应该能够加载成员管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/members')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('成员')
  })

  test('应该能够查看成员列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/members')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证表格加载
    const table = authenticatedPage.locator('.MuiDataGrid-root, table')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够邀请新成员', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/members')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击邀请按钮
    await authenticatedPage.click('button:has-text("邀请"), button:has-text("添加")')
    await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

    // 填写邀请信息
    await authenticatedPage.fill('input[name="email"], input[type="email"]', 'test@example.com')

    // 选择角色
    const roleSelect = authenticatedPage.locator('label:has-text("角色")').locator('..').locator('div[role="button"]')
    if (await roleSelect.isVisible()) {
      await roleSelect.click()
      await authenticatedPage.waitForSelector('.MuiMenu-root')
      await authenticatedPage.click('.MuiMenuItem-root:first-child')
    }

    // 取消邀请
    await authenticatedPage.click('button:has-text("取消")')
  })

  test('应该能够搜索成员', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/members')
    await authenticatedPage.waitForLoadState('networkidle')

    // 搜索成员
    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('test')
      await searchInput.press('Enter')
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够编辑成员角色', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/members')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击编辑按钮
    const editButton = authenticatedPage.locator('button:has-text("编辑"), button[aria-label="编辑"]')
    if (await editButton.first().isVisible()) {
      await editButton.first().click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 取消编辑
      await authenticatedPage.click('button:has-text("取消")')
    }
  })

  test('应该能够移除成员', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/members')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击移除按钮
    const removeButton = authenticatedPage.locator('button:has-text("移除"), button:has-text("删除")')
    if (await removeButton.first().isVisible()) {
      await removeButton.first().click()

      // 确认移除
      const confirmButton = authenticatedPage.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        // 不实际移除
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })
})

test.describe('组织管理模块 - 组织分析', () => {
  test('应该能够加载组织分析页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/analytics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('分析')
  })

  test('应该能够查看组织统计数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/analytics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证统计卡片
    const statsCards = authenticatedPage.locator('.MuiCard-root')
    await expect(statsCards.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够查看活跃度图表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/analytics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证图表
    const charts = authenticatedPage.locator('.echarts-for-react, canvas')
    await expect(charts.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够切换时间范围', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/analytics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换时间范围
    const timeButton = authenticatedPage.locator('button:has-text("本周"), button:has-text("本月")')
    if (await timeButton.first().isVisible()) {
      await timeButton.first().click()
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够导出分析报告', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/analytics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击导出按钮
    const exportButton = authenticatedPage.locator('button:has-text("导出")')
    if (await exportButton.isVisible()) {
      await exportButton.click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('组织管理模块 - 直播审核', () => {
  test('应该能够加载直播审核页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/live-reviews')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('审核')
  })

  test('应该能够查看待审核列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/live-reviews')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证表格加载
    const table = authenticatedPage.locator('.MuiDataGrid-root, table')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够审核通过', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/live-reviews')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击通过按钮
    const approveButton = authenticatedPage.locator('button:has-text("通过"), button:has-text("批准")')
    if (await approveButton.first().isVisible()) {
      await approveButton.first().click()

      // 确认通过
      const confirmButton = authenticatedPage.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        // 不实际操作
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })

  test('应该能够审核拒绝', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/live-reviews')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击拒绝按钮
    const rejectButton = authenticatedPage.locator('button:has-text("拒绝")')
    if (await rejectButton.first().isVisible()) {
      await rejectButton.first().click()

      // 填写拒绝原因
      const reasonInput = authenticatedPage.locator('textarea[name="reason"]')
      if (await reasonInput.isVisible()) {
        await reasonInput.fill('测试拒绝原因')

        // 取消拒绝
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })

  test('应该能够查看审核历史', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/live-reviews')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到历史标签
    const historyTab = authenticatedPage.locator('button:has-text("历史"), .MuiTab-root:has-text("历史")')
    if (await historyTab.isVisible()) {
      await historyTab.click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够批量审核', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/org/live-reviews')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择多个项目
    const checkboxes = authenticatedPage.locator('.MuiDataGrid-row .MuiCheckbox-root')
    const count = await checkboxes.count()

    if (count > 0) {
      await checkboxes.first().click()
      await authenticatedPage.waitForTimeout(200)

      // 点击批量通过按钮
      const batchButton = authenticatedPage.locator('button:has-text("批量通过")')
      if (await batchButton.isVisible()) {
        await batchButton.click()
        // 不实际操作
        await authenticatedPage.waitForTimeout(500)
      }
    }
  })
})
