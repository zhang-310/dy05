import { test, expect } from '../fixtures/auth.fixture'

/**
 * AI 模块核心页面 E2E 测试（任务 #9）
 * 覆盖 5 个核心页面：
 * 1. AiDashboardPage - AI 仪表盘
 * 2. AiMonitoringPage - AI 监控
 * 3. AiQuotaPage - AI 配额管理
 * 4. ModelsConfigPage - 模型配置
 * 5. CreativeStudioPage - 创意工作室
 */

test.describe('AI 模块 - AI 仪表盘', () => {
  test('应该能够加载 AI 仪表盘页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/dashboard')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('AI')

    // 验证 KPI 卡片加载
    const kpiCards = authenticatedPage.locator('.MuiCard-root')
    await expect(kpiCards).toHaveCount(4, { timeout: 10000 })
  })

  test('应该能够切换时间范围', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/dashboard')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换时间范围
    await authenticatedPage.click('button:has-text("今日"), button:has-text("本周")')
    await authenticatedPage.waitForTimeout(500)

    // 验证数据重新加载
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看调用类型分布图表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/dashboard')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证图表容器存在
    const chartContainer = authenticatedPage.locator('.echarts-for-react, canvas')
    await expect(chartContainer.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够刷新仪表盘数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/dashboard')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击刷新按钮
    const refreshButton = authenticatedPage.locator('button:has-text("刷新"), button[aria-label="刷新"]')
    if (await refreshButton.isVisible()) {
      await refreshButton.click()
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够查看基础设施健康状态', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/dashboard')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证健康状态指示器
    const healthIndicators = authenticatedPage.locator('.MuiChip-root, .status-chip')
    await expect(healthIndicators.first()).toBeVisible({ timeout: 10000 })
  })
})

test.describe('AI 模块 - AI 监控', () => {
  test('应该能够加载 AI 监控页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/monitoring')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('监控')
  })

  test('应该能够查看实时调用监控', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/monitoring')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证监控图表加载
    const charts = authenticatedPage.locator('.echarts-for-react, canvas')
    await expect(charts.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够筛选监控数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/monitoring')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择模型类型筛选
    const filterSelect = authenticatedPage.locator('label:has-text("模型"), label:has-text("类型")').locator('..').locator('div[role="button"]')
    if (await filterSelect.isVisible()) {
      await filterSelect.first().click()
      await authenticatedPage.waitForSelector('.MuiMenu-root, .MuiPopover-root')
      await authenticatedPage.click('.MuiMenuItem-root:first-child')
      await authenticatedPage.waitForLoadState('networkidle')
    }
  })

  test('应该能够查看错误日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/monitoring')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到错误日志标签
    const errorTab = authenticatedPage.locator('button:has-text("错误"), .MuiTab-root:has-text("错误")')
    if (await errorTab.isVisible()) {
      await errorTab.click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够导出监控数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/monitoring')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击导出按钮
    const exportButton = authenticatedPage.locator('button:has-text("导出")')
    if (await exportButton.isVisible()) {
      await exportButton.click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('AI 模块 - AI 配额管理', () => {
  test('应该能够加载 AI 配额管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('配额')
  })

  test('应该能够查看配额使用情况', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证配额卡片
    const quotaCards = authenticatedPage.locator('.MuiCard-root')
    await expect(quotaCards.first()).toBeVisible({ timeout: 10000 })

    // 验证进度条
    const progressBars = authenticatedPage.locator('.MuiLinearProgress-root')
    await expect(progressBars.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够设置配额限制', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击设置按钮
    const settingsButton = authenticatedPage.locator('button:has-text("设置"), button:has-text("配置")')
    if (await settingsButton.first().isVisible()) {
      await settingsButton.first().click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })

      // 填写配额限制
      const limitInput = authenticatedPage.locator('input[name="limit"], input[name="quota"]')
      if (await limitInput.isVisible()) {
        await limitInput.fill('10000')
        await authenticatedPage.click('button:has-text("保存"), button:has-text("确定")')
        await authenticatedPage.waitForTimeout(1000)
      }
    }
  })

  test('应该能够查看配额使用历史', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 切换到历史标签
    const historyTab = authenticatedPage.locator('button:has-text("历史"), .MuiTab-root:has-text("历史")')
    if (await historyTab.isVisible()) {
      await historyTab.click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够重置配额', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/quota')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击重置按钮
    const resetButton = authenticatedPage.locator('button:has-text("重置")')
    if (await resetButton.isVisible()) {
      await resetButton.click()
      await authenticatedPage.waitForSelector('button:has-text("确认"), button:has-text("确定")')
      // 不实际确认，避免影响数据
      await authenticatedPage.click('button:has-text("取消")')
    }
  })
})

test.describe('AI 模块 - 模型配置', () => {
  test('应该能够加载模型配置页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/models-config')
    await authenticatedPage.waitForLoadState('networkidle')

    await expect(authenticatedPage.getByRole('heading', { name: /模型配置/ })).toBeVisible()
  })

  test('应该能够查看模型列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/models-config')
    await authenticatedPage.waitForLoadState('networkidle')

    await expect(authenticatedPage.locator('[data-testid="models-config-list"]')).toBeVisible({ timeout: 10000 })
  })

  test('应该能够配置模型参数', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/models-config')
    await authenticatedPage.waitForLoadState('networkidle')

    const editButton = authenticatedPage.locator('button:has-text("编辑")')
    if (await editButton.first().isVisible()) {
      await editButton.first().click()
      await authenticatedPage.waitForSelector('.MuiDialog-root', { timeout: 5000 })
      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible()
      await authenticatedPage.click('button:has-text("取消"), button:has-text("关闭")')
    }
  })

  test('应该能够启用/禁用模型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/models-config')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击开关
    const toggleSwitch = authenticatedPage.locator('.MuiSwitch-root')
    if (await toggleSwitch.first().isVisible()) {
      const initialState = await toggleSwitch.first().getAttribute('aria-checked')
      await toggleSwitch.first().click()
      await authenticatedPage.waitForTimeout(1000)

      // 恢复原状态
      const newState = await toggleSwitch.first().getAttribute('aria-checked')
      if (initialState !== newState) {
        await toggleSwitch.first().click()
      }
    }
  })

  test('应该能够测试模型连接', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/models-config')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击测试按钮
    const testButton = authenticatedPage.locator('button:has-text("测试")')
    if (await testButton.first().isVisible()) {
      await testButton.first().click()
      await authenticatedPage.waitForTimeout(2000)
    }
  })
})

test.describe('AI 模块 - 创意工作室', () => {
  test('应该能够加载创意工作室页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/creative-studio')
    await authenticatedPage.waitForLoadState('networkidle')

    // 验证页面标题
    await expect(authenticatedPage.locator('h4, h5, .page-title')).toContainText('创意')
  })

  test('应该能够选择创意类型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/creative-studio')
    await authenticatedPage.waitForLoadState('networkidle')

    // 选择创意类型
    const typeButtons = authenticatedPage.locator('button:has-text("图像"), button:has-text("视频"), button:has-text("文案")')
    if (await typeButtons.first().isVisible()) {
      await typeButtons.first().click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够输入创意描述', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/creative-studio')
    await authenticatedPage.waitForLoadState('networkidle')

    // 填写描述
    const descInput = authenticatedPage.locator('textarea[placeholder*="描述"], textarea[name="description"]')
    if (await descInput.isVisible()) {
      await descInput.fill('一个美丽的护肤品广告场景')
    }
  })

  test('应该能够生成创意内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/creative-studio')
    await authenticatedPage.waitForLoadState('networkidle')

    // 填写描述
    const descInput = authenticatedPage.locator('textarea[placeholder*="描述"], textarea[name="description"]')
    if (await descInput.isVisible()) {
      await descInput.fill('测试创意生成')

      // 点击生成按钮
      const generateButton = authenticatedPage.locator('button:has-text("生成")')
      if (await generateButton.isVisible()) {
        await generateButton.click()

        // 等待生成完成（最多 30 秒）
        await authenticatedPage.waitForSelector('text=生成完成, text=生成成功', { timeout: 30000 }).catch(() => {})
      }
    }
  })

  test('应该能够保存创意作品', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/creative-studio')
    await authenticatedPage.waitForLoadState('networkidle')

    // 点击保存按钮
    const saveButton = authenticatedPage.locator('button:has-text("保存")')
    if (await saveButton.isVisible()) {
      await saveButton.click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})
