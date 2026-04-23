import { test, expect, expectWithRetry } from '../fixtures/autofix.fixture'

/**
 * 自动修复测试示例
 * 演示如何使用自动修复功能
 */

test.describe('自动修复测试示例', () => {
  test('示例 1: 自动处理登录过期', async ({ autoFixPage }) => {
    // autoFixPage 会自动检测登录状态，如果未登录会自动登录
    await autoFixPage.goto('/admin/dashboard')

    // 验证页面加载
    await expect(autoFixPage.locator('h1, h2, .page-title')).toBeVisible()
  })

  test('示例 2: 自动重试点击操作', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/product/list')

    // 即使元素被遮罩层覆盖，也会自动关闭遮罩层并重试
    await autoFixPage.click('button:has-text("新建商品")')

    // 验证对话框打开
    await expect(autoFixPage.locator('.MuiDialog-root')).toBeVisible()
  })

  test('示例 3: 自动重试填写表单', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/product/list')
    await autoFixPage.click('button:has-text("新建商品")')

    // 自动处理输入框未就绪的情况
    await autoFixPage.fill('input[name="productName"]', '测试商品')
    await autoFixPage.fill('input[name="price"]', '99.99')

    await autoFixPage.click('button:has-text("保存")')

    // 使用带重试的断言
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('text=保存成功')).toBeVisible()
    })
  })

  test('示例 4: 自动处理网络延迟', async ({ autoFixPage }) => {
    // 自动等待网络空闲
    await autoFixPage.goto('/admin/live/sessions')

    // 等待数据加载
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
    }, { maxRetries: 5, retryDelay: 2000 })
  })

  test('示例 5: 自定义重试选项', async ({ page, autoFixOptions }) => {
    // 可以自定义重试选项
    const customOptions = {
      ...autoFixOptions,
      maxRetries: 5,
      clearCache: true,
      waitForStability: true,
    }

    // 使用自定义选项
    await page.goto('/admin/dashboard')
  })
})

test.describe('实际场景测试 - 带自动修复', () => {
  test('创建直播场次 - 自动修复版', async ({ autoFixPage }) => {
    // 导航到直播场次页面
    await autoFixPage.goto('/admin/live/sessions')

    // 点击新建按钮（自动处理遮罩层）
    await autoFixPage.click('button:has-text("新建场次")')

    // 填写表单（自动处理输入框未就绪）
    const sessionTitle = '自动修复测试场次 - ' + Date.now()
    await autoFixPage.fill('input[name="liveTitle"]', sessionTitle)
    await autoFixPage.fill('textarea[name="liveDescription"]', '这是一个自动修复测试')

    // 保存（自动重试）
    await autoFixPage.click('button:has-text("保存")')

    // 验证成功（带重试）
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('text=保存成功')).toBeVisible()
    })

    // 验证场次出现在列表中（带重试）
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator(`text=${sessionTitle}`)).toBeVisible()
    }, { maxRetries: 5, retryDelay: 1000 })
  })

  test('生成 AI 话术 - 自动修复版', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/script/generate')

    // 填写生成参数
    await autoFixPage.fill('input[name="productName"]', '补水面膜')
    await autoFixPage.click('label:has-text("专业种草")')
    await autoFixPage.fill('input[name="duration"]', '60')

    // 点击生成（自动处理按钮不可用的情况）
    await autoFixPage.click('button:has-text("生成话术")')

    // 等待生成完成（长时间操作，使用更长的重试间隔）
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('text=生成完成')).toBeVisible()
    }, { maxRetries: 10, retryDelay: 3000 })

    // 验证生成结果
    await expect(autoFixPage.locator('.generated-script, textarea[name="scriptContent"]')).toBeVisible()
  })

  test('批量操作 - 自动修复版', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/product/list')

    // 选择多个商品（自动处理复选框）
    const checkboxes = autoFixPage.locator('.MuiDataGrid-row .MuiCheckbox-root')
    const count = await checkboxes.count()

    for (let i = 0; i < Math.min(count, 3); i++) {
      await checkboxes.nth(i).click()
      await autoFixPage.waitForTimeout(200)
    }

    // 批量操作
    await autoFixPage.click('button:has-text("批量操作")')
    await autoFixPage.click('button:has-text("导出")')

    // 验证操作成功
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('text=导出成功')).toBeVisible()
    })
  })

  test('复杂表单提交 - 自动修复版', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/agent/list')
    await autoFixPage.click('button:has-text("新建智能体")')

    // 填写复杂表单
    const agentName = '自动修复智能体 - ' + Date.now()
    await autoFixPage.fill('input[name="agentName"]', agentName)
    await autoFixPage.fill('textarea[name="agentDescription"]', '这是一个测试智能体')

    // 选择类型（自动处理下拉框）
    await autoFixPage.click('label:has-text("智能体类型") + div')
    await autoFixPage.waitForSelector('.MuiMenu-root, .MuiPopover-root')
    await autoFixPage.click('.MuiMenuItem-root:has-text("客服助手")')

    // 配置参数
    await autoFixPage.fill('textarea[name="systemPrompt"]', '你是一个专业的客服助手')
    await autoFixPage.fill('input[name="temperature"]', '0.7')

    // 提交表单
    await autoFixPage.click('button:has-text("创建")')

    // 验证创建成功
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('text=创建成功')).toBeVisible()
    })

    await expectWithRetry(async () => {
      await expect(autoFixPage.locator(`text=${agentName}`)).toBeVisible()
    }, { maxRetries: 5, retryDelay: 1000 })
  })

  test('分页和搜索 - 自动修复版', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/copy/library')

    // 搜索（自动处理输入延迟）
    await autoFixPage.fill('input[placeholder*="搜索"]', '营销')
    await autoFixPage.press('input[placeholder*="搜索"]', 'Enter')

    // 等待搜索结果
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
    }, { maxRetries: 5, retryDelay: 1000 })

    // 翻页（自动处理分页按钮）
    const nextButton = autoFixPage.locator('button[aria-label="下一页"], button:has-text("下一页")')
    if (await nextButton.isVisible()) {
      await nextButton.click()
      await autoFixPage.waitForLoadState('networkidle')
    }
  })

  test('文件上传 - 自动修复版', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/ai/knowledge-base')

    // 创建知识库
    await autoFixPage.click('button:has-text("新建知识库")')
    const kbName = '测试知识库 - ' + Date.now()
    await autoFixPage.fill('input[name="name"]', kbName)
    await autoFixPage.fill('textarea[name="description"]', '测试描述')
    await autoFixPage.click('button:has-text("创建")')

    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('text=创建成功')).toBeVisible()
    })

    // 上传文档（自动处理文件选择器）
    // 注意：实际文件上传需要准备测试文件
    // const fileInput = autoFixPage.locator('input[type="file"]')
    // await fileInput.setInputFiles('test-files/sample.pdf')
  })

  test('实时数据更新 - 自动修复版', async ({ autoFixPage }) => {
    await autoFixPage.goto('/admin/system/monitoring')

    // 等待初始数据加载
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('.metrics-chart, .monitoring-dashboard')).toBeVisible()
    })

    // 刷新数据
    await autoFixPage.click('button:has-text("刷新")')

    // 验证数据更新（带重试）
    await expectWithRetry(async () => {
      await expect(autoFixPage.locator('.metrics-chart')).toBeVisible()
    }, { maxRetries: 5, retryDelay: 2000 })
  })
})
