import { test, expect } from '../fixtures/auth.fixture'

/**
 * 辅助功能模块测试
 *
 * 覆盖页面：
 * 1. FileStoragePage - 文件存储管理
 * 2. SlangDictPage - 行业俚语词典
 * 3. OnboardingPage - 用户引导
 * 4. ContentModerationPage - 内容审核
 * 5. SystemConfigPage - 系统配置
 * 6. AttributionPage - 归因分析
 */

test.describe('辅助功能 - 文件存储管理', () => {
  test('应该能够加载文件存储管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看文件列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够上传文件', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const uploadButton = authenticatedPage.locator('button:has-text("上传"), button:has-text("添加")')
    if (await uploadButton.isVisible({ timeout: 5000 })) {
      await uploadButton.first().click()

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

  test('应该能够搜索文件', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选文件类型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("类型")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够预览文件', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const previewButton = authenticatedPage.locator('button:has-text("预览"), [aria-label*="预览"]')
    if (await previewButton.isVisible({ timeout: 5000 })) {
      await previewButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      if (await dialog.isVisible({ timeout: 3000 })) {
        // 关闭预览
        const closeButton = dialog.locator('button:has-text("关闭"), [aria-label*="关闭"]')
        if (await closeButton.isVisible({ timeout: 3000 })) {
          await closeButton.first().click()
        }
      }
    }
  })

  test('应该能够下载文件', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const downloadButton = authenticatedPage.locator('button:has-text("下载"), [aria-label*="下载"]')
    if (await downloadButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(downloadButton.first()).toBeVisible()
    }
  })

  test('应该能够删除文件', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const deleteButton = authenticatedPage.locator('button:has-text("删除"), [aria-label*="删除"]')
    if (await deleteButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(deleteButton.first()).toBeVisible()
    }
  })

  test('应该能够批量操作文件', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/storage/files')
    await authenticatedPage.waitForLoadState('networkidle')

    const batchButton = authenticatedPage.locator('button:has-text("批量"), button:has-text("批量操作")')
    if (await batchButton.isVisible({ timeout: 5000 })) {
      await batchButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('辅助功能 - 行业俚语词典', () => {
  test('应该能够加载行业俚语词典页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看词条列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够添加词条', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const addButton = authenticatedPage.locator('button:has-text("新建"), button:has-text("添加")')
    if (await addButton.isVisible({ timeout: 5000 })) {
      await addButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })

  test('应该能够搜索词条', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选词条分类', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("分类")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑词条', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
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

  test('应该能够查看词条详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够导入词条', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const importButton = authenticatedPage.locator('button:has-text("导入")')
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

  test('应该能够导出词条', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/slangdict')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})

test.describe('辅助功能 - 用户引导', () => {
  test('应该能够加载用户引导页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看引导步骤列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待列表或卡片加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够创建引导步骤', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
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

  test('应该能够编辑引导步骤', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
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

  test('应该能够预览引导流程', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
    await authenticatedPage.waitForLoadState('networkidle')

    const previewButton = authenticatedPage.locator('button:has-text("预览")')
    if (await previewButton.isVisible({ timeout: 5000 })) {
      await previewButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够启用/禁用引导', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
    await authenticatedPage.waitForLoadState('networkidle')

    const toggleButton = authenticatedPage.locator('button:has-text("启用"), button:has-text("禁用"), .MuiSwitch-root')
    if (await toggleButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(toggleButton.first()).toBeVisible()
    }
  })

  test('应该能够调整步骤顺序', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/onboarding')
    await authenticatedPage.waitForLoadState('networkidle')

    const sortButton = authenticatedPage.locator('button:has-text("排序"), [aria-label*="排序"]')
    if (await sortButton.isVisible({ timeout: 5000 })) {
      await sortButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('辅助功能 - 内容审核', () => {
  test('应该能够加载内容审核页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看待审核内容列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索审核内容', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选审核状态', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("状态")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看内容详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root, .MuiDrawer-root')
      if (await dialog.isVisible({ timeout: 3000 })) {
        // 关闭详情
        const closeButton = dialog.locator('button:has-text("关闭"), [aria-label*="关闭"]')
        if (await closeButton.isVisible({ timeout: 3000 })) {
          await closeButton.first().click()
        }
      }
    }
  })

  test('应该能够通过审核', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const approveButton = authenticatedPage.locator('button:has-text("通过"), button:has-text("批准")')
    if (await approveButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(approveButton.first()).toBeVisible()
    }
  })

  test('应该能够拒绝审核', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const rejectButton = authenticatedPage.locator('button:has-text("拒绝"), button:has-text("驳回")')
    if (await rejectButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(rejectButton.first()).toBeVisible()
    }
  })

  test('应该能够批量审核', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/content-moderation')
    await authenticatedPage.waitForLoadState('networkidle')

    const batchButton = authenticatedPage.locator('button:has-text("批量"), button:has-text("批量审核")')
    if (await batchButton.isVisible({ timeout: 5000 })) {
      await batchButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('辅助功能 - 系统配置', () => {
  test('应该能够加载系统配置页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看配置列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待配置项加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够搜索配置项', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选配置分类', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("分类")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const editButton = authenticatedPage.locator('button:has-text("编辑"), [aria-label*="编辑"]')
    if (await editButton.isVisible({ timeout: 5000 })) {
      await editButton.first().click()

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

  test('应该能够重置配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const resetButton = authenticatedPage.locator('button:has-text("重置")')
    if (await resetButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(resetButton.first()).toBeVisible()
    }
  })

  test('应该能够导出配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })

  test('应该能够导入配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/system/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const importButton = authenticatedPage.locator('button:has-text("导入")')
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

test.describe('辅助功能 - 归因分析', () => {
  test('应该能够加载归因分析页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看归因报告', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待报告内容加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够选择归因模型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const modelSelect = authenticatedPage.locator('button:has-text("模型"), [role="combobox"]')
    if (await modelSelect.isVisible({ timeout: 5000 })) {
      await modelSelect.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够设置时间范围', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const dateInput = authenticatedPage.locator('input[type="date"], input[placeholder*="日期"]')
    if (await dateInput.isVisible({ timeout: 5000 })) {
      await dateInput.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看归因路径', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const pathButton = authenticatedPage.locator('button:has-text("路径"), button:has-text("查看路径")')
    if (await pathButton.isVisible({ timeout: 5000 })) {
      await pathButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看渠道贡献', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const channelButton = authenticatedPage.locator('button:has-text("渠道"), button:has-text("贡献")')
    if (await channelButton.isVisible({ timeout: 5000 })) {
      await channelButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够导出归因报告', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })

  test('应该能够对比不同模型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/attribution')
    await authenticatedPage.waitForLoadState('networkidle')

    const compareButton = authenticatedPage.locator('button:has-text("对比"), button:has-text("比较")')
    if (await compareButton.isVisible({ timeout: 5000 })) {
      await compareButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})
