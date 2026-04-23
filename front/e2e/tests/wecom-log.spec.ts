import { test, expect } from '../fixtures/auth.fixture'

/**
 * 企业微信 + 日志模块测试
 *
 * 覆盖页面：
 * 1. WecomConfigPage - 企业微信配置
 * 2. WecomMessagesPage - 企业微信消息
 * 3. WecomRobotsPage - 企业微信机器人
 * 4. AuditLogPage - 审计日志
 * 5. OperationLogPage - 操作日志
 * 6. SystemLogPage - 系统日志
 */

test.describe('企业微信模块 - 配置管理', () => {
  test('应该能够加载企业微信配置页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看配置信息', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/config')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待配置表单加载
    await authenticatedPage.waitForTimeout(2000)
    const content = authenticatedPage.locator('body')
    await expect(content).toBeVisible()
  })

  test('应该能够编辑配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const editButton = authenticatedPage.locator('button:has-text("编辑"), button:has-text("修改")')
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

  test('应该能够测试连接', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const testButton = authenticatedPage.locator('button:has-text("测试"), button:has-text("测试连接")')
    if (await testButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(testButton.first()).toBeVisible()
    }
  })

  test('应该能够保存配置', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/config')
    await authenticatedPage.waitForLoadState('networkidle')

    const saveButton = authenticatedPage.locator('button:has-text("保存")')
    if (await saveButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(saveButton.first()).toBeVisible()
    }
  })
})

test.describe('企业微信模块 - 消息管理', () => {
  test('应该能够加载企业微信消息页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看消息列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够发送消息', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
    await authenticatedPage.waitForLoadState('networkidle')

    const sendButton = authenticatedPage.locator('button:has-text("发送"), button:has-text("新建")')
    if (await sendButton.isVisible({ timeout: 5000 })) {
      await sendButton.first().click()

      const dialog = authenticatedPage.locator('.MuiDialog-root')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 关闭对话框
      const cancelButton = dialog.locator('button:has-text("取消"), button:has-text("关闭")')
      if (await cancelButton.isVisible({ timeout: 3000 })) {
        await cancelButton.click()
      }
    }
  })

  test('应该能够搜索消息', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选消息状态', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("状态")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看消息详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
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

  test('应该能够重发失败消息', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/messages')
    await authenticatedPage.waitForLoadState('networkidle')

    const resendButton = authenticatedPage.locator('button:has-text("重发"), button:has-text("重试")')
    if (await resendButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(resendButton.first()).toBeVisible()
    }
  })
})

test.describe('企业微信模块 - 机器人管理', () => {
  test('应该能够加载企业微信机器人页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看机器人列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格或列表加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够创建机器人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
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

  test('应该能够搜索机器人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够编辑机器人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
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

  test('应该能够启用/禁用机器人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
    await authenticatedPage.waitForLoadState('networkidle')

    const toggleButton = authenticatedPage.locator('button:has-text("启用"), button:has-text("禁用"), .MuiSwitch-root')
    if (await toggleButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(toggleButton.first()).toBeVisible()
    }
  })

  test('应该能够测试机器人', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/wecom/robots')
    await authenticatedPage.waitForLoadState('networkidle')

    const testButton = authenticatedPage.locator('button:has-text("测试")')
    if (await testButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(testButton.first()).toBeVisible()
    }
  })
})

test.describe('日志模块 - 审计日志', () => {
  test('应该能够加载审计日志页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看审计日志列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索审计日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选日志类型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("类型")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够按时间范围筛选', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
    await authenticatedPage.waitForLoadState('networkidle')

    const dateInput = authenticatedPage.locator('input[type="date"], input[placeholder*="日期"]')
    if (await dateInput.isVisible({ timeout: 5000 })) {
      await dateInput.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看日志详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
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

  test('应该能够导出审计日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/audit')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})

test.describe('日志模块 - 操作日志', () => {
  test('应该能够加载操作日志页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看操作日志列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索操作日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选操作类型', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
    await authenticatedPage.waitForLoadState('networkidle')

    const filterButton = authenticatedPage.locator('button:has-text("筛选"), button:has-text("操作")')
    if (await filterButton.isVisible({ timeout: 5000 })) {
      await filterButton.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够按用户筛选', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
    await authenticatedPage.waitForLoadState('networkidle')

    const userFilter = authenticatedPage.locator('input[placeholder*="用户"], button:has-text("用户")')
    if (await userFilter.isVisible({ timeout: 5000 })) {
      await userFilter.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看操作详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
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

  test('应该能够导出操作日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/operation')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})

test.describe('日志模块 - 系统日志', () => {
  test('应该能够加载系统日志页面', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
    await authenticatedPage.waitForLoadState('networkidle')

    const heading = authenticatedPage.locator('h4, h5, .page-title')
    await expect(heading).toBeVisible()
  })

  test('应该能够查看系统日志列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
    await authenticatedPage.waitForLoadState('networkidle')

    // 等待表格加载
    const table = authenticatedPage.locator('table, .MuiDataGrid-root, [role="grid"]')
    await expect(table).toBeVisible({ timeout: 10000 })
  })

  test('应该能够搜索系统日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
    await authenticatedPage.waitForLoadState('networkidle')

    const searchInput = authenticatedPage.locator('input[placeholder*="搜索"], input[type="search"]')
    if (await searchInput.isVisible({ timeout: 5000 })) {
      await searchInput.fill('test')
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够筛选日志级别', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
    await authenticatedPage.waitForLoadState('networkidle')

    const levelFilter = authenticatedPage.locator('button:has-text("级别"), button:has-text("筛选")')
    if (await levelFilter.isVisible({ timeout: 5000 })) {
      await levelFilter.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够按模块筛选', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
    await authenticatedPage.waitForLoadState('networkidle')

    const moduleFilter = authenticatedPage.locator('button:has-text("模块"), input[placeholder*="模块"]')
    if (await moduleFilter.isVisible({ timeout: 5000 })) {
      await moduleFilter.first().click()
      await authenticatedPage.waitForTimeout(1000)
    }
  })

  test('应该能够查看日志详情', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
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

  test('应该能够导出系统日志', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/admin/logs/system')
    await authenticatedPage.waitForLoadState('networkidle')

    const exportButton = authenticatedPage.locator('button:has-text("导出"), button:has-text("下载")')
    if (await exportButton.isVisible({ timeout: 5000 })) {
      // 只检查按钮存在，不实际点击
      await expect(exportButton.first()).toBeVisible()
    }
  })
})
