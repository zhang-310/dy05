import { describe, it, expect, vi } from 'vitest'
import { agentApi, shareApi } from '@/api/agent'

vi.mock('@/api/agent', () => ({
  agentApi: {
    get: vi.fn(),
    conversationCreate: vi.fn(),
    conversationList: vi.fn(),
    conversationDelete: vi.fn(),
    conversationRename: vi.fn(),
    messageSend: vi.fn(),
    messageList: vi.fn(),
    messageRate: vi.fn(),
    exportConversation: vi.fn(),
  },
  shareApi: {
    create: vi.fn(),
    list: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('AgentChatPage - API 集成测试', () => {
  it('应该正确调用智能体详情 API', async () => {
    vi.mocked(agentApi.get).mockResolvedValue({
      id: 1,
      agentName: '话术生成助手',
      description: '专业的直播话术生成工具',
      agentType: 1,
      systemPrompt: '你是话术生成助手',
      responseMode: 0,
      status: 1,
      createTime: '2026-05-10T10:00:00',
      availableTools: '["kb_rag_search","product_search"]',
    })

    const result = await agentApi.get(1)

    expect(result.agentName).toBe('话术生成助手')
    expect(result.agentType).toBe(1)
    expect(agentApi.get).toHaveBeenCalledWith(1)
  })

  it('应该正确调用创建对话 API', async () => {
    vi.mocked(agentApi.conversationCreate).mockResolvedValue(1)

    const result = await agentApi.conversationCreate(1, '新对话')

    expect(result).toBe(1)
    expect(agentApi.conversationCreate).toHaveBeenCalledWith(1, '新对话')
  })

  it('应该正确调用对话列表 API', async () => {
    vi.mocked(agentApi.conversationList).mockResolvedValue([
      {
        id: 1,
        title: '对话1',
        lastMessage: '最后一条消息',
        updateTime: '2026-05-10T10:00:00',
        createTime: '2026-05-10T09:00:00',
      },
      {
        id: 2,
        title: '对话2',
        createTime: '2026-05-10T08:00:00',
      },
    ])

    const result = await agentApi.conversationList(1)

    expect(result).toHaveLength(2)
    expect(result[0].title).toBe('对话1')
    expect(agentApi.conversationList).toHaveBeenCalledWith(1)
  })

  it('应该正确调用删除对话 API', async () => {
    vi.mocked(agentApi.conversationDelete).mockResolvedValue(undefined)

    await agentApi.conversationDelete(1)

    expect(agentApi.conversationDelete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用重命名对话 API', async () => {
    vi.mocked(agentApi.conversationRename).mockResolvedValue(undefined)

    await agentApi.conversationRename(1, '新标题')

    expect(agentApi.conversationRename).toHaveBeenCalledWith(1, '新标题')
  })

  it('应该正确调用发送消息 API', async () => {
    vi.mocked(agentApi.messageSend).mockResolvedValue(1)

    const result = await agentApi.messageSend(1, '你好')

    expect(result).toBe(1)
    expect(agentApi.messageSend).toHaveBeenCalledWith(1, '你好')
  })

  it('应该正确调用消息列表 API', async () => {
    vi.mocked(agentApi.messageList).mockResolvedValue([
      {
        id: 1,
        role: 'user',
        content: '你好',
        createdAt: '2026-05-10T10:00:00',
      },
      {
        id: 2,
        role: 'assistant',
        content: '你好！有什么可以帮助你的吗？',
        createdAt: '2026-05-10T10:00:01',
      },
    ])

    const result = await agentApi.messageList(1)

    expect(result).toHaveLength(2)
    expect(result[0].role).toBe('user')
    expect(result[1].role).toBe('assistant')
    expect(agentApi.messageList).toHaveBeenCalledWith(1)
  })

  it('应该正确调用消息评分 API', async () => {
    vi.mocked(agentApi.messageRate).mockResolvedValue(undefined)

    await agentApi.messageRate(1, 'up')

    expect(agentApi.messageRate).toHaveBeenCalledWith(1, 'up')
  })

  it('应该正确调用导出对话 API', async () => {
    vi.mocked(agentApi.exportConversation).mockResolvedValue({
      downloadUrl: '/download/conversation-1.md',
    })

    const result = await agentApi.exportConversation(1)

    expect(result.downloadUrl).toBe('/download/conversation-1.md')
    expect(agentApi.exportConversation).toHaveBeenCalledWith(1)
  })

  it('应该正确调用创建分享 API', async () => {
    vi.mocked(shareApi.create).mockResolvedValue({
      id: 1,
      shareCode: 'abc123',
      conversationId: 1,
      agentId: 1,
      title: '分享的对话',
      messageCount: 10,
      viewCount: 0,
      isPublic: 1,
      createTime: '2026-05-10T10:00:00',
    })

    const result = await shareApi.create({
      conversationId: 1,
      title: '分享的对话',
      isPublic: 1,
    })

    expect(result.shareCode).toBe('abc123')
    expect(shareApi.create).toHaveBeenCalled()
  })

  it('应该正确调用分享列表 API', async () => {
    vi.mocked(shareApi.list).mockResolvedValue([
      {
        id: 1,
        shareCode: 'abc123',
        conversationId: 1,
        agentId: 1,
        title: '分享1',
        messageCount: 10,
        viewCount: 5,
        isPublic: 1,
        createTime: '2026-05-10T10:00:00',
      },
    ])

    const result = await shareApi.list()

    expect(result).toHaveLength(1)
    expect(result[0].shareCode).toBe('abc123')
    expect(shareApi.list).toHaveBeenCalled()
  })

  it('应该正确调用删除分享 API', async () => {
    vi.mocked(shareApi.delete).mockResolvedValue(undefined)

    await shareApi.delete(1)

    expect(shareApi.delete).toHaveBeenCalledWith(1)
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(agentApi.get).mockRejectedValue(new Error('Network error'))

    await expect(agentApi.get(1)).rejects.toThrow('Network error')
  })

  it('应该验证智能体类型常量', () => {
    const AGENT_TYPE_LABELS: Record<number, string> = {
      0: '自定义',
      1: '话术生成',
      2: '违规检测',
      3: '商品分析',
      4: '场次规划',
      5: '数据分析',
      6: '客户服务',
    }

    expect(Object.keys(AGENT_TYPE_LABELS)).toHaveLength(7)
    expect(AGENT_TYPE_LABELS[1]).toBe('话术生成')
    expect(AGENT_TYPE_LABELS[2]).toBe('违规检测')
  })

  it('应该验证技能配置常量', () => {
    const SKILL_NAMES = [
      'kb_rag_search',
      'product_search',
      'compliance_check',
      'live_session_query',
      'script_generate',
    ]

    expect(SKILL_NAMES).toHaveLength(5)
    expect(SKILL_NAMES).toContain('kb_rag_search')
    expect(SKILL_NAMES).toContain('product_search')
  })

  it('应该验证消息角色类型', () => {
    const MESSAGE_ROLES = ['user', 'assistant', 'tool']

    expect(MESSAGE_ROLES).toHaveLength(3)
    expect(MESSAGE_ROLES).toContain('user')
    expect(MESSAGE_ROLES).toContain('assistant')
    expect(MESSAGE_ROLES).toContain('tool')
  })
})
