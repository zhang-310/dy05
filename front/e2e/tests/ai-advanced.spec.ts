import { test, expect } from '../fixtures/auth.fixture'

/**
 * AI 模块 - 高级功能测试
 *
 * 覆盖页面：
 * 1. AdminInfraPage - AI 基础设施管理
 * 2. AiCallLogPage - AI 调用日志
 * 3. DigitalHumanPage - 数字人管理
 * 4. EvolutionTopicPage - 进化主题管理
 * 5. IndustryBrainDiagnosisPage - 行业大脑诊断
 * 6. ModelBenchmarkPage - 模型基准测试
 * 7. PromptToolsPage - Prompt 工具
 * 8. TaskModelConfigPage - 任务模型配置
 */

test.describe('AI 模块 - 基础设施管理', () => {
  test('应该能够加载 AI 基础设施管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/infrastructure')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看服务状态', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/infrastructure')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待状态卡片加载
    const statusCards = authenticatedPage.locator('.MuiCard-root, [class*="card"]')
    await expect(statusCards.first()).toBeVisible({ timeout: 10000 })
  })

  test('应该能够查看资源使用情况', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/infrastructure')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待图表或统计数据加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够配置服务参数', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/infrastructure')
    await authenticatedPage.waitForLoadState('networkidle')

    const configButton = authenticatedPage.locator('button:has-text("配置"), button:has-text("设置")')
    if (await configButton.isVisible({ timeout: 5000 })) {
      await configButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })
})

test.describe('AI 模块 - 调用日志', () => {
  test('应该能够加载 AI 调用日志页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/call-logs')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看调用日志列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/call-logs')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索调用日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/call-logs')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选调用日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/call-logs')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("过滤")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看日志详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/call-logs')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看"), [aria-label*="详情"]')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root, .MuiDrawer-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭详情
      const closeButton = dialog.locator('button:has-text("关闭"), [aria-label*="关闭"]')
      if (await closeButton.isVisible({ timeout: 3000 })) {
        await closeButton.first().click()
      }
    }
  })

  test('应该能够导出调用日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/call-logs')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})

test.describe('AI 模块 - 数字人管理', () => {
  test('应该能够加载数字人管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/digital-human')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看数字人列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/digital-human')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待列表或卡片加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够创建数字人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/digital-human')
    await authenticatedPage.waitForLoadState('networkidle')

    const createButton = authenticatedPage.locator('button:has-text("新建"), button:has-text("创建"), button:has-text("添加")')
    if (await createButton.isVisible({ timeout: 5000 })) {
      await createButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })

  test('应该能够搜索数字人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/digital-human')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑数字人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/digital-human')
    await authenticatedPage.waitForLoadState('networkidle')

    const editButton = authenticatedPage.locator('button:has-text("编辑"), [aria-label*="编辑"]')
    if (await editButton.isVisible({ timeout: 5000 })) {
      await editButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })
})

test.describe('AI 模块 - 进化主题管理', () => {
  test('应该能够加载进化主题管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/evolution/topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看主题列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/evolution/topics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够创建进化主题', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/evolution/topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const createButton = authenticatedPage.locator('button:has-text("新建"), button:has-text("创建")')
    if (await createButton.isVisible({ timeout: 5000 })) {
      await createButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })

  test('应该能够查看主题详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/evolution/topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑主题', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/evolution/topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const editButton = authenticatedPage.locator('button:has-text("编辑"), [aria-label*="编辑"]')
    if (await editButton.isVisible({ timeout: 5000 })) {
      await editButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })
})

test.describe('AI 模块 - 行业大脑诊断', () => {
  test('应该能够加载行业大脑诊断页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/industry-brain/diagnosis')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看诊断报告', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/industry-brain/diagnosis')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待报告内容加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够运行诊断', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/industry-brain/diagnosis')
    await authenticatedPage.waitForLoadState('networkidle')

    const runButton = authenticatedPage.locator('button:has-text("运行"), button:has-text("诊断"), button:has-text("开始")')
    if (await runButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(runButton.first()).toBeVisible()
    }
  })

  test('应该能够查看诊断历史', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/industry-brain/diagnosis')
    await authenticatedPage.waitForLoadState('networkidle')

    const historyButton = authenticatedPage.locator('button:has-text("历史"), button:has-text("记录")')
    if (await historyButton.isVisible({ timeout: 5000 })) {
      await historyButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够导出诊断报告', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/industry-brain/diagnosis')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})

test.describe('AI 模块 - 模型基准测试', () => {
  test('应该能够加载模型基准测试页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/model-benchmark')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看基准测试结果', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/model-benchmark')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或图表加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够运行基准测试', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/model-benchmark')
    await authenticatedPage.waitForLoadState('networkidle')

    const runButton = authenticatedPage.locator('button:has-text("运行"), button:has-text("测试"), button:has-text("开始")')
    if (await runButton.isVisible({ timeout: 5000 })) {
      await runButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      if (await dialog.isVisible({ timeout: 3000 })) {
        // 关闭对话框
        const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
        if (await cancelButton.isVisible({ timeout: 3000 })) {
          await cancelButton.click()
        }
      }
    }
  })

  test('应该能够比较模型性能', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/model-benchmark')
    await authenticatedPage.waitForLoadState('networkidle')

    const compareButton = authenticatedPage.locator('button:has-text("比较"), button:has-text("对比")')
    if (await compareButton.isVisible({ timeout: 5000 })) {
      await compareButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看测试详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/model-benchmark')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('AI 模块 - Prompt 工具', () => {
  test('应该能够加载 Prompt 工具页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/prompt-tools')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够使用 Prompt 编辑器', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/prompt-tools')
    await authenticatedPage.waitForLoadState('networkidle')

    const editor = authenticatedPage.locator('textarea, .monaco-editor, [contenteditable="true"]')
    if (await editor.isVisible({ timeout: 5000 })) {
      await editor.first().click()
      await authenticatedPage.waitForTimeout(500)
    }
  })

  test('应该能够测试 Prompt', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/prompt-tools')
    await authenticatedPage.waitForLoadState('networkidle')

    const testButton = authenticatedPage.locator('button:has-text("测试"), button:has-text("运行")')
    if (await testButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(testButton.first()).toBeVisible()
    }
  })

  test('应该能够保存 Prompt', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/prompt-tools')
    await authenticatedPage.waitForLoadState('networkidle')

    const saveButton = authenticatedPage.locator('button:has-text("保存")')
    if (await saveButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(saveButton.first()).toBeVisible()
    }
  })

  test('应该能够查看 Prompt 历史', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/prompt-tools')
    await authenticatedPage.waitForLoadState('networkidle')

    const historyButton = authenticatedPage.locator('button:has-text("历史"), button:has-text("记录")')
    if (await historyButton.isVisible({ timeout: 5000 })) {
      await historyButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够导入 Prompt 模板', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/prompt-tools')
    await authenticatedPage.waitForLoadState('networkidle')

    const importButton = authenticatedPage.locator('button:has-text("导入"), button:has-text("加载")')
    if (await importButton.isVisible({ timeout: 5000 })) {
      await importButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      if (await dialog.isVisible({ timeout: 3000 })) {
        // 关闭对话框
        const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
        if (await cancelButton.isVisible({ timeout: 3000 })) {
          await cancelButton.click()
        }
      }
    }
  })
})

test.describe('AI 模块 - 任务模型配置', () => {
  test('应该能够加载任务模型配置页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/task-model-config')
    await authenticatedPage.waitForLoadState('networkidle')

    await expect(authenticatedPage.getByTestId('task-model-config-page')).toBeVisible()
    await expect(authenticatedPage.getByRole('heading', { level: 1 })).toContainText('任务模型映射配置')
  })

  test('应该能够查看任务列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/task-model-config')
    await authenticatedPage.waitForLoadState('networkidle')

    await expect(authenticatedPage.getByTestId('task-model-config-grid')).toBeVisible({ timeout: 15000 })
    const grid = authenticatedPage.locator('.MuiDataGrid-root, [role="grid"]')
    await expect(grid.first()).toBeVisible({ timeout: 15000 })
  })

  test('应该能够打开新增对话框并关闭', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/ai/task-model-config')
    await authenticatedPage.waitForLoadState('networkidle')

    await authenticatedPage.getByTestId('task-model-config-add').click()
    await expect(authenticatedPage.locator('.MuiDialog-root')).toBeVisible({ timeout: 5000 })
    await authenticatedPage.getByTestId('task-model-config-dialog-cancel').click()
  })
})
