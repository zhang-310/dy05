import { test, expect } from '../fixtures/auth.fixture'
import { AgentListPage, AgentChatPage, AgentConfigPage, AgentAnalyticsPage } from '../pages/agent.page'

/**
 * 智能体模块 E2E 测试
 * 覆盖智能体管理、对话、配置、分析等核心功能
 */

test.describe('智能体模块 - 智能体管理', () => {
  test('应该能够创建新智能体', async ({ authenticatedPage }) => {
    const agentPage = new AgentListPage(authenticatedPage)

    await agentPage.goto()
    await agentPage.verifyPageLoaded()

    const agentName = '测试智能体 - ' + Date.now()
    await agentPage.createAgent({
      name: agentName,
      description: '这是一个自动化测试创建的智能体',
      type: '客服助手',
    })

    // 验证智能体出现在列表中
    await expect(authenticatedPage.locator(`text=${agentName}`)).toBeVisible()
  })

  test('应该能够搜索智能体', async ({ authenticatedPage }) => {
    const agentPage = new AgentListPage(authenticatedPage)

    await agentPage.goto()
    await agentPage.searchAgent('客服')

    // 验证搜索结果
    await expect(authenticatedPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  })

  test('应该能够编辑智能体', async ({ authenticatedPage }) => {
    const agentPage = new AgentListPage(authenticatedPage)

    await agentPage.goto()

    // 创建测试智能体
    const agentName = '编辑测试智能体 - ' + Date.now()
    await agentPage.createAgent({
      name: agentName,
      description: '原始描述',
      type: '营销助手',
    })

    // 编辑智能体
    await agentPage.editAgent(agentName, '这是修改后的智能体描述')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够删除智能体', async ({ authenticatedPage }) => {
    const agentPage = new AgentListPage(authenticatedPage)

    await agentPage.goto()

    // 创建临时智能体
    const agentName = '临时智能体 - ' + Date.now()
    await agentPage.createAgent({
      name: agentName,
      description: '临时描述',
      type: '通用助手',
    })

    // 删除智能体
    await agentPage.deleteAgent(agentName)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${agentName}`)).toHaveCount(0)
  })

  test('应该能够打开智能体对话', async ({ authenticatedPage }) => {
    const agentPage = new AgentListPage(authenticatedPage)

    await agentPage.goto()

    // 创建智能体
    const agentName = '对话测试智能体 - ' + Date.now()
    await agentPage.createAgent({
      name: agentName,
      description: '用于测试对话功能',
      type: '客服助手',
    })

    // 打开对话
    await agentPage.openChat(agentName)

    // 验证跳转到对话页面
    await expect(authenticatedPage).toHaveURL(/agent\/chat/)
  })
})

test.describe('智能体模块 - 智能体对话', () => {
  let agentId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    // 创建测试智能体
    const agentPage = new AgentListPage(authenticatedPage)
    await agentPage.goto()
    await agentPage.createAgent({
      name: '对话测试 - ' + Date.now(),
      description: '用于对话测试',
      type: '客服助手',
    })

    // 获取智能体 ID（实际应从页面获取）
    agentId = 1
  })

  test('应该能够发送消息并接收回复', async ({ authenticatedPage }) => {
    const chatPage = new AgentChatPage(authenticatedPage)

    await chatPage.goto(agentId)
    await chatPage.verifyPageLoaded()

    // 发送消息
    await chatPage.sendMessage('你好，请介绍一下你自己')

    // 等待回复
    await chatPage.waitForResponse()

    // 验证回复显示
    await expect(authenticatedPage.locator('.message-item.assistant')).toBeVisible()
  })

  test('应该能够进行多轮对话', async ({ authenticatedPage }) => {
    const chatPage = new AgentChatPage(authenticatedPage)

    await chatPage.goto(agentId)

    // 第一轮对话
    await chatPage.sendMessage('你好')
    await chatPage.waitForResponse()

    // 第二轮对话
    await chatPage.sendMessage('请推荐一款护肤品')
    await chatPage.waitForResponse()

    // 第三轮对话
    await chatPage.sendMessage('价格是多少')
    await chatPage.waitForResponse()

    // 验证有多条消息
    await expect(authenticatedPage.locator('.message-item')).toHaveCount(6) // 3条用户消息 + 3条助手回复
  })

  test('应该能够清空对话历史', async ({ authenticatedPage }) => {
    const chatPage = new AgentChatPage(authenticatedPage)

    await chatPage.goto(agentId)

    // 发送消息
    await chatPage.sendMessage('测试消息')
    await chatPage.waitForResponse()

    // 清空历史
    await chatPage.clearHistory()

    // 验证消息已清空
    await expect(authenticatedPage.locator('.message-item')).toHaveCount(0)
  })

  test('应该能够导出对话记录', async ({ authenticatedPage }) => {
    const chatPage = new AgentChatPage(authenticatedPage)

    await chatPage.goto(agentId)

    // 发送消息
    await chatPage.sendMessage('导出测试消息')
    await chatPage.waitForResponse()

    // 导出对话
    await chatPage.exportChat()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })

  test('应该能够重新生成回复', async ({ authenticatedPage }) => {
    const chatPage = new AgentChatPage(authenticatedPage)

    await chatPage.goto(agentId)

    // 发送消息
    await chatPage.sendMessage('请生成一段话术')
    await chatPage.waitForResponse()

    // 重新生成
    await chatPage.regenerateResponse()

    // 验证新回复生成
    await expect(authenticatedPage.locator('.message-item.assistant:last-child')).toBeVisible()
  })
})

test.describe('智能体模块 - 智能体配置', () => {
  let agentId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    agentId = 1 // 实际应从测试数据获取
  })

  test('应该能够更新系统提示词', async ({ authenticatedPage }) => {
    const configPage = new AgentConfigPage(authenticatedPage)

    await configPage.goto(agentId)
    await configPage.verifyPageLoaded()

    await configPage.updateSystemPrompt('你是一个专业的护肤品顾问，擅长为用户推荐合适的护肤产品。')

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够调整温度参数', async ({ authenticatedPage }) => {
    const configPage = new AgentConfigPage(authenticatedPage)

    await configPage.goto(agentId)

    await configPage.updateTemperature(0.7)

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够添加知识库', async ({ authenticatedPage }) => {
    const configPage = new AgentConfigPage(authenticatedPage)

    await configPage.goto(agentId)

    await configPage.addKnowledgeBase('护肤品知识库')

    // 验证添加成功
    await expect(authenticatedPage.locator('text=添加成功')).toBeVisible()
  })

  test('应该能够移除知识库', async ({ authenticatedPage }) => {
    const configPage = new AgentConfigPage(authenticatedPage)

    await configPage.goto(agentId)

    // 先添加知识库
    await configPage.addKnowledgeBase('测试知识库')

    // 移除知识库
    await configPage.removeKnowledgeBase('测试知识库')

    // 验证删除成功
    await expect(authenticatedPage.locator('text=删除成功')).toBeVisible()
  })

  test('应该能够启用功能', async ({ authenticatedPage }) => {
    const configPage = new AgentConfigPage(authenticatedPage)

    await configPage.goto(agentId)

    await configPage.enableFunction('联网搜索')

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })
})

test.describe('智能体模块 - 智能体分析', () => {
  let agentId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    agentId = 1
  })

  test('应该能够查看智能体分析数据', async ({ authenticatedPage }) => {
    const analyticsPage = new AgentAnalyticsPage(authenticatedPage)

    await analyticsPage.goto(agentId)
    await analyticsPage.verifyPageLoaded()

    // 验证分析页面加载
    await expect(authenticatedPage.locator('.analytics-chart, .MuiGrid-root')).toBeVisible()
  })

  test('应该能够选择日期范围', async ({ authenticatedPage }) => {
    const analyticsPage = new AgentAnalyticsPage(authenticatedPage)

    await analyticsPage.goto(agentId)

    await analyticsPage.selectDateRange('2026-01-01', '2026-01-31')

    // 验证数据更新
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看对话详情', async ({ authenticatedPage }) => {
    const analyticsPage = new AgentAnalyticsPage(authenticatedPage)

    await analyticsPage.goto(agentId)

    // 查看第一条对话详情（假设对话 ID 为 1）
    await analyticsPage.viewConversationDetail(1)

    // 验证详情显示
    await expect(authenticatedPage.locator('.conversation-detail')).toBeVisible()
  })

  test('应该能够导出分析报告', async ({ authenticatedPage }) => {
    const analyticsPage = new AgentAnalyticsPage(authenticatedPage)

    await analyticsPage.goto(agentId)

    await analyticsPage.exportAnalyticsReport()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })
})
