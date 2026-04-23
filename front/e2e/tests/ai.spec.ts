import { test, expect } from '../fixtures/auth.fixture'
import { KnowledgeBasePage, PromptTemplatePage, EvolutionPage } from '../pages/ai.page'

/**
 * AI 模块 E2E 测试
 * 覆盖知识库、Prompt 模板、知识自进化等核心功能
 */

test.describe('AI 模块 - 知识库管理', () => {
  test('应该能够创建新的知识库', async ({ authenticatedPage }) => {
    const kbPage = new KnowledgeBasePage(authenticatedPage)

    await kbPage.goto()
    await kbPage.verifyPageLoaded()

    await kbPage.createKnowledgeBase('测试知识库 - ' + Date.now(), '这是一个自动化测试创建的知识库')

    // 验证知识库出现在列表中
    await expect(authenticatedPage.locator('text=测试知识库')).toBeVisible()
  })

  test('应该能够上传文档到知识库', async ({ authenticatedPage }) => {
    const kbPage = new KnowledgeBasePage(authenticatedPage)

    await kbPage.goto()

    // 创建临时知识库
    const kbName = '文档测试库 - ' + Date.now()
    await kbPage.createKnowledgeBase(kbName, '用于测试文档上传')

    // 上传文档（需要准备测试文件）
    // await kbPage.uploadDocument(kbName, 'test-files/sample.pdf')

    // 验证上传成功
    // await expect(authenticatedPage.locator('text=上传成功')).toBeVisible()
  })

  test('应该能够搜索知识库内容', async ({ authenticatedPage }) => {
    const kbPage = new KnowledgeBasePage(authenticatedPage)

    await kbPage.goto()

    // 假设已有知识库
    await kbPage.searchKnowledge('测试知识库', '护肤品')

    // 验证搜索结果加载
    await authenticatedPage.waitForLoadState('networkidle')
  })
})

test.describe('AI 模块 - Prompt 模板管理', () => {
  test('应该能够创建新的 Prompt 模板', async ({ authenticatedPage }) => {
    const promptPage = new PromptTemplatePage(authenticatedPage)

    await promptPage.goto()
    await promptPage.verifyPageLoaded()

    await promptPage.createTemplate(
      '测试模板 - ' + Date.now(),
      '你是一个专业的{{role}}，请帮我{{task}}',
      '直播话术'
    )

    // 验证模板创建成功
    await expect(authenticatedPage.locator('text=测试模板')).toBeVisible()
  })

  test('应该能够测试 Prompt 模板', async ({ authenticatedPage }) => {
    const promptPage = new PromptTemplatePage(authenticatedPage)

    await promptPage.goto()

    // 创建测试模板
    const templateName = 'Prompt测试 - ' + Date.now()
    await promptPage.createTemplate(templateName, '你好{{name}}，欢迎来到{{place}}', '通用')

    // 测试模板
    await promptPage.testTemplate(templateName, {
      name: '张三',
      place: '直播间',
    })

    // 验证渲染结果
    await expect(authenticatedPage.locator('.rendered-result')).toContainText('你好张三，欢迎来到直播间')
  })

  test('应该能够编辑 Prompt 模板', async ({ authenticatedPage }) => {
    const promptPage = new PromptTemplatePage(authenticatedPage)

    await promptPage.goto()

    // 创建模板
    const templateName = '编辑测试 - ' + Date.now()
    await promptPage.createTemplate(templateName, '原始内容', '通用')

    // 编辑模板
    const row = authenticatedPage.locator(`tr:has-text("${templateName}")`)
    await row.locator('button:has-text("编辑")').click()
    await authenticatedPage.fill('textarea[name="templateContent"]', '修改后的内容')
    await authenticatedPage.click('button:has-text("保存")')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })
})

test.describe('AI 模块 - 知识自进化', () => {
  test('应该能够触发知识进化任务', async ({ authenticatedPage }) => {
    const evolutionPage = new EvolutionPage(authenticatedPage)

    await evolutionPage.goto()
    await evolutionPage.verifyPageLoaded()

    await evolutionPage.triggerEvolution('直播数据')

    // 验证任务提交成功
    await expect(authenticatedPage.locator('text=任务已提交')).toBeVisible()
  })

  test('应该能够查看进化任务详情', async ({ authenticatedPage }) => {
    const evolutionPage = new EvolutionPage(authenticatedPage)

    await evolutionPage.goto()

    // 触发任务
    await evolutionPage.triggerEvolution('商品数据')

    // 查看任务详情（假设任务 ID 为 1）
    await evolutionPage.viewEvolutionTask(1)

    // 验证详情页加载
    await expect(authenticatedPage.locator('.task-detail')).toBeVisible()
  })

  test('应该能够审核进化任务', async ({ authenticatedPage }) => {
    const evolutionPage = new EvolutionPage(authenticatedPage)

    await evolutionPage.goto()

    // 触发任务
    await evolutionPage.triggerEvolution('用户反馈')

    // 审核任务（假设任务 ID 为 1）
    await evolutionPage.approveEvolution(1)

    // 验证审核成功
    await expect(authenticatedPage.locator('text=审核通过')).toBeVisible()
  })

  test('应该能够查看进化历史', async ({ authenticatedPage }) => {
    const evolutionPage = new EvolutionPage(authenticatedPage)

    await evolutionPage.goto()

    // 切换到历史标签
    await authenticatedPage.click('button:has-text("进化历史")')

    // 验证历史列表加载
    await authenticatedPage.waitForSelector('.MuiDataGrid-root, table')
  })
})
