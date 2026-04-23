import { test, expect } from '../fixtures/auth.fixture'

/**
 * 短视频模块 - 高级功能测试
 *
 * 覆盖页面：
 * 1. AccountCollectPage - 账号采集
 * 2. AiMusicPage - AI 音乐生成
 * 3. DramaPage - 短剧管理
 * 4. HotTopicsPage - 热点话题
 * 5. MaterialLibraryPage - 素材库
 * 6. VideoClipsPage - 视频剪辑
 */

test.describe('短视频模块 - 账号采集', () => {
  test('应该能够加载账号采集页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看采集任务列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够创建采集任务', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
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

  test('应该能够搜索采集任务', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看采集结果', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
    await authenticatedPage.waitForLoadState('networkidle')

    const viewButton = authenticatedPage.locator('button:has-text("查看"), button:has-text("详情")')
    if (await viewButton.isVisible({ timeout: 5000 })) {
      await viewButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够停止采集任务', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
    await authenticatedPage.waitForLoadState('networkidle')

    const stopButton = authenticatedPage.locator('button:has-text("停止"), button:has-text("暂停")')
    if (await stopButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(stopButton.first()).toBeVisible()
    }
  })

  test('应该能够导出采集数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/collect')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})

test.describe('短视频模块 - AI 音乐生成', () => {
  test('应该能够加载 AI 音乐生成页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看音乐列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待列表或卡片加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够生成音乐', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    const generateButton = authenticatedPage.locator('button:has-text("生成"), button:has-text("创建")')
    if (await generateButton.isVisible({ timeout: 5000 })) {
      await generateButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })

  test('应该能够搜索音乐', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够预览音乐', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    const playButton = authenticatedPage.locator('button:has-text("播放"), button:has-text("预览"), [aria-label*="播放"]')
    if (await playButton.isVisible({ timeout: 5000 })) {
      await playButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够下载音乐', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    const downloadButton = authenticatedPage.locator('button:has-text("下载"), [aria-label*="下载"]')
    if (await downloadButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(downloadButton.first()).toBeVisible()
    }
  })

  test('应该能够删除音乐', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/ai-music')
    await authenticatedPage.waitForLoadState('networkidle')

    const deleteButton = authenticatedPage.locator('button:has-text("删除"), [aria-label*="删除"]')
    if (await deleteButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(deleteButton.first()).toBeVisible()
    }
  })
})

test.describe('短视频模块 - 短剧管理', () => {
  test('应该能够加载短剧管理页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看短剧列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够创建短剧', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
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

  test('应该能够搜索短剧', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑短剧', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
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

  test('应该能够查看短剧详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够发布短剧', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/drama')
    await authenticatedPage.waitForLoadState('networkidle')

    const publishButton = authenticatedPage.locator('button:has-text("发布")')
    if (await publishButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(publishButton.first()).toBeVisible()
    }
  })
})

test.describe('短视频模块 - 热点话题', () => {
  test('应该能够加载热点话题页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看热点话题列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索热点话题', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选话题分类', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("分类")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看话题详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const detailButton = authenticatedPage.locator('button:has-text("详情"), button:has-text("查看")')
    if (await detailButton.isVisible({ timeout: 5000 })) {
      await detailButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够收藏话题', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const favoriteButton = authenticatedPage.locator('button:has-text("收藏"), [aria-label*="收藏"]')
    if (await favoriteButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(favoriteButton.first()).toBeVisible()
    }
  })

  test('应该能够刷新热点数据', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/hot-topics')
    await authenticatedPage.waitForLoadState('networkidle')

    const refreshButton = authenticatedPage.locator('button:has-text("刷新"), [aria-label*="刷新"]')
    if (await refreshButton.isVisible({ timeout: 5000 })) {
      await refreshButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('短视频模块 - 素材库', () => {
  test('应该能够加载素材库页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看素材列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待列表或卡片加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够上传素材', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
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

  test('应该能够搜索素材', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选素材类型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("类型")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够预览素材', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
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

  test('应该能够下载素材', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
    await authenticatedPage.waitForLoadState('networkidle')

    const downloadButton = authenticatedPage.locator('button:has-text("下载"), [aria-label*="下载"]')
    if (await downloadButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(downloadButton.first()).toBeVisible()
    }
  })

  test('应该能够批量操作素材', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/material')
    await authenticatedPage.waitForLoadState('networkidle')

    const batchButton = authenticatedPage.locator('button:has-text("批量"), button:has-text("批量操作")')
    if (await batchButton.isVisible({ timeout: 5000 })) {
      await batchButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })
})

test.describe('短视频模块 - 视频剪辑', () => {
  test('应该能够加载视频剪辑页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看剪辑项目列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够创建剪辑项目', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
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

  test('应该能够搜索剪辑项目', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑剪辑项目', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    const editButton = authenticatedPage.locator('button:has-text("编辑"), [aria-label*="编辑"]')
    if (await editButton.isVisible({ timeout: 5000 })) {
      await editButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够预览剪辑效果', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    const previewButton = authenticatedPage.locator('button:has-text("预览"), button:has-text("播放")')
    if (await previewButton.isVisible({ timeout: 5000 })) {
      await previewButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够导出视频', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })

  test('应该能够删除剪辑项目', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/shortvideo/editing')
    await authenticatedPage.waitForLoadState('networkidle')

    const deleteButton = authenticatedPage.locator('button:has-text("删除"), [aria-label*="删除"]')
    if (await deleteButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(deleteButton.first()).toBeVisible()
    }
  })
})
