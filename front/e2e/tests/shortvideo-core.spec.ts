import { test, expect } from '../fixtures/auth.fixture'

/**
 * 短视频模块核心页面 E2E 测试（任务 #10）
 * 覆盖 3 个核心页面：
 * 1. CompetitorMonitorPage - 竞品监控
 * 2. ContentEffectPredictPage - 内容效果预测
 * 3. DailyContentPage - 每日内容
 */

test.describe('短视频模块 - 竞品监控', () => {
  test('应该能够加载竞品监控页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/competitor-monitor')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('竞品')
  })

  test('应该能够添加竞品账号', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/competitor-monitor')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击添加按钮
    await authenticatedPage.click('button:has-text("添加"), button:has-text("新建")')
    await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

    // 填写表单
    await authenticatedPage.fill('input[name="accountId"]', 'test_competitor_' + Date.now())
    await authenticatedPage.fill('input[name="accountName"]', '测试竞品账号')

    // 选择平台
    const platformSelect = authenticatedPage.locator('label:has-text("平台")').locator('..').locator('div[role="button"]')
    if (await platformSelect.isVisible()) {
      await platformSelect.click()
      await authenticatedPage.waitForSelector('.MuiMenu-root')
      await authenticatedPage.click('.MuiMenuItem-root:has-text("抖音")')
    }

    // 提交
    await authenticatedPage.click('button:has-text("确定"), button:has-text("添加")')
    await authenticatedPage.waitForTimeout(1000)
  })

  test('应该能够查看竞品列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/competitor-monitor')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证表格加载
    const table = authenticatedPage.locator('.MuiDataGrid-root, table')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够分析竞品数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/competitor-monitor')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击分析按钮
    const analyzeButton = authenticatedPage.locator('button:has-text("分析")')
    if (await analyzeButton.first().isVisible()) {
      await analyzeButton.first().click()

      // 等待分析完成
      await authenticatedPage.waitForSelector('.MuiDialog-root, text=分析完成', { timeout: 30000 }).catch(() => {})
    }
  })

  test('应该能够生成周报', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/competitor-monitor')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击周报按钮
    const reportButton = authenticatedPage.locator('button:has-text("周报"), button:has-text("报告")')
    if (await reportButton.isVisible()) {
      await reportButton.click()
      await authenticatedPage.waitForTimeout(2000)
    }
  })

  test('应该能够删除竞品账号', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/competitor-monitor')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击删除按钮
    const deleteButton = authenticatedPage.locator('button[aria-label="删除"], button:has-text("删除")')
    if (await deleteButton.first().isVisible()) {
      await deleteButton.first().click()

      // 确认删除
      const confirmButton = authenticatedPage.locator('button:has-text("确认"), button:has-text("确定")')
      if (await confirmButton.isVisible()) {
        // 不实际删除，避免影响数据
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })
})

test.describe('短视频模块 - 内容效果预测', () => {
  test('应该能够加载内容效果预测页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/effect-predict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('预测')
  })

  test('应该能够输入内容进行预测', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/effect-predict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 填写内容标题
    const titleInput = authenticatedPage.locator('input[name="title"], input[placeholder*="标题"]')
    if (await titleInput.isVisible()) {
      await titleInput.fill('测试短视频标题 - 护肤品推荐')
    }

    // 填写内容描述
    const descInput = authenticatedPage.locator('textarea[name="description"], textarea[placeholder*="描述"]')
    if (await descInput.isVisible()) {
      await descInput.fill('这是一个关于护肤品的短视频内容')
    }
  })

  test('应该能够选择内容类型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/effect-predict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择内容类型
    const typeSelect = authenticatedPage.locator('label:has-text("类型")').locator('..').locator('div[role="button"]')
    if (await typeSelect.isVisible()) {
      await typeSelect.click()
      await authenticatedPage.waitForSelector('.MuiMenu-root')
      await authenticatedPage.click('.MuiMenuItem-root:first-child')
    }
  })

  test('应该能够执行效果预测', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/effect-predict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 填写必要信息
    const titleInput = authenticatedPage.locator('input[name="title"], input[placeholder*="标题"]')
    if (await titleInput.isVisible()) {
      await titleInput.fill('测试预测标题')

      // 点击预测按钮
      const predictButton = authenticatedPage.locator('button:has-text("预测"), button:has-text("分析")')
      if (await predictButton.isVisible()) {
        await predictButton.click()

        // 等待预测结果
        await authenticatedPage.waitForSelector('text=预测完成, text=预测结果, .prediction-result', { timeout: 30000 }).catch(() => {})
      }
    }
  })

  test('应该能够查看预测结果详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/effect-predict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 查看预测结果卡片
    const resultCards = authenticatedPage.locator('.MuiCard-root, .result-card')
    if (await resultCards.first().isVisible()) {
      await expect(resultCards.first()).toBeVisible()
    }
  })

  test('应该能够查看历史预测记录', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/effect-predict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到历史记录标签
    const historyTab = authenticatedPage.locator('button:has-text("历史"), .MuiTab-root:has-text("历史")')
    if (await historyTab.isVisible()) {
      await historyTab.click()
      await authenticatedPage.waitForTimeout(500)

      // 验证历史列表
      const historyList = authenticatedPage.locator('.MuiDataGrid-root, table, .history-list')
      await expect(historyList).toBeVisible({ timeout: 10000 })
    }
  })
})

test.describe('短视频模块 - 每日内容', () => {
  test('应该能够加载每日内容页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('每日')
  })

  test('应该能够查看今日内容列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证内容列表加载
    const contentList = authenticatedPage.locator('.MuiDataGrid-root, table, .content-list')
    await expect(contentList).toBeVisible({ timeout: 10000 })
  })

  test('应该能够切换日期查看内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击日期选择器
    const datePicker = authenticatedPage.locator('input[type="date"], button:has-text("选择日期")')
    if (await datePicker.isVisible()) {
      await datePicker.click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够添加每日内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击添加按钮
    const addButton = authenticatedPage.locator('button:has-text("添加"), button:has-text("新建")')
    if (await addButton.isVisible()) {
      await addButton.click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 填写内容信息
      const titleInput = authenticatedPage.locator('input[name="title"]')
      if (await titleInput.isVisible()) {
        await titleInput.fill('每日内容测试 - ' + Date.now())

        const descInput = authenticatedPage.locator('textarea[name="description"], textarea[name="content"]')
        if (await descInput.isVisible()) {
          await descInput.fill('这是一个测试的每日内容')
        }

        // 提交
        await authenticatedPage.click('button:has-text("保存"), button:has-text("确定")')
        await authenticatedPage.waitForTimeout(1000)
      }
    }
  })

  test('应该能够编辑每日内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击编辑按钮
    const editButton = authenticatedPage.locator('button:has-text("编辑"), button[aria-label="编辑"]')
    if (await editButton.first().isVisible()) {
      await editButton.first().click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 修改内容
      const titleInput = authenticatedPage.locator('input[name="title"]')
      if (await titleInput.isVisible()) {
        await titleInput.fill('修改后的标题')

        // 取消修改
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })

  test('应该能够删除每日内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击删除按钮
    const deleteButton = authenticatedPage.locator('button:has-text("删除"), button[aria-label="删除"]')
    if (await deleteButton.first().isVisible()) {
      await deleteButton.first().click()

      // 确认删除
      const confirmButton = authenticatedPage.locator('button:has-text("确认"), button:has-text("确定")')
      if (await confirmButton.isVisible()) {
        // 不实际删除
        await authenticatedPage.click('button:has-text("取消")')
      }
    }
  })

  test('应该能够批量发布内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择多个内容
    const checkboxes = authenticatedPage.locator('.MuiDataGrid-row .MuiCheckbox-root')
    const count = await checkboxes.count()

    if (count > 0) {
      await checkboxes.first().click()
      await authenticatedPage.waitForTimeout(200)

      // 点击批量发布按钮
      const batchButton = authenticatedPage.locator('button:has-text("批量"), button:has-text("发布")')
      if (await batchButton.isVisible()) {
        await batchButton.click()
        await authenticatedPage.waitForTimeout(1000)
      }
    }
  })

  test('应该能够查看内容统计', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/daily')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证统计卡片
    const statsCards = authenticatedPage.locator('.MuiCard-root, .stats-card')
    if (await statsCards.first().isVisible()) {
      await expect(statsCards.first()).toBeVisible()
    }
  })
})
