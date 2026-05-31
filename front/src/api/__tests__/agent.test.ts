import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { agentApi, workflowApi } from '../agent'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('agent API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts agent query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await agentApi.list({ page: 0, rows: 20, agentName: '客服' })
    expect(mockPost).toHaveBeenCalledWith('/agent/list', { page: 0, rows: 20, agentName: '客服' })
  })

  it('normalizes wrapped agent list responses', async () => {
    mockPost.mockResolvedValue({
      records: [{
        id: 3,
        agentName: '包装智能体',
        description: '来自 records',
        agentType: 1,
        responseMode: 1,
        status: 1,
        createTime: '2026-05-22 10:00:00',
      }],
      totalElements: 1,
      page: 0,
      size: 20,
    })

    const result = await agentApi.list({ page: 0, rows: 20 })

    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [{ id: 3, agentName: '包装智能体' }],
    })
  })

  it('normalizes deeply nested agent page envelopes', async () => {
    mockPost.mockResolvedValue({
      data: {
        result: {
          records: [{
            id: '5',
            name: '深层智能体',
            description: null,
            type: '3',
            mode: '1',
            status: '1',
            createdAt: '2026-05-23 09:00:00',
            tools: ['kb_rag_search', 'product_search'],
            averageRating: '4.5',
          }],
          totalCount: '1',
          pageNumber: '2',
          rows: '50',
        },
      },
    })

    const result = await agentApi.list({ page: 0, rows: 20 })

    expect(result).toMatchObject({
      total: 1,
      pageNum: 2,
      pageSize: 50,
      list: [{
        id: 5,
        agentName: '深层智能体',
        description: '',
        agentType: 3,
        responseMode: 1,
        availableTools: '["kb_rag_search","product_search"]',
        averageRating: 4.5,
      }],
    })
  })

  it('returns an empty agent page instead of leaking non-array records to pages', async () => {
    mockPost.mockResolvedValue({ data: { status: 200, message: 'ok' } })

    const result = await agentApi.list({ page: 3, rows: 15 })

    expect(result).toEqual({
      total: 0,
      list: [],
      pageNum: 3,
      pageSize: 15,
    })
  })

  it('conversationCreate posts agent id and title', async () => {
    mockPost.mockResolvedValue(1)
    await agentApi.conversationCreate(8, '新对话')
    expect(mockPost).toHaveBeenCalledWith('/agent/conversation/create', { agentId: 8, topic: '新对话' })
  })

  it('messageSend posts conversation message payload', async () => {
    mockPost.mockResolvedValue(99)
    await agentApi.messageSend(11, '你好')
    expect(mockPost).toHaveBeenCalledWith('/agent/message/send', { conversationId: 11, senderType: 1, content: '你好', tokens: 0 })
  })

  it('normalizes backend conversation rows', async () => {
    mockPost.mockResolvedValue([
      {
        id: 9,
        conversationTopic: '选品咨询',
        lastMessageTime: '2026-05-20T10:00:00+08:00',
        createTime: '2026-05-20T09:00:00+08:00',
      },
    ])

    const result = await agentApi.conversationList(18)

    expect(result).toEqual([
      {
        id: 9,
        title: '选品咨询',
        lastMessage: undefined,
        updateTime: '2026-05-20T10:00:00+08:00',
        createTime: '2026-05-20T09:00:00+08:00',
      },
    ])
  })

  it('normalizes serialized nested conversation and message lists', async () => {
    mockPost
      .mockResolvedValueOnce('{"data":{"records":[{"id":"11","topic":"嵌套对话","lastMessage":"你好","createdAt":"2026-05-24"}]}}')
      .mockResolvedValueOnce({
        result: {
          items: [{
            id: '12',
            role: 'assistant',
            message: 'ignored',
            content: '已完成',
            tokenUsage: { input: '2', output: '9' },
            toolCalls: { data: { items: ['kb_rag_search'] } },
            createdAt: '2026-05-24T10:00:00+08:00',
          }],
        },
      })

    await expect(agentApi.conversationList(18)).resolves.toEqual([
      {
        id: 11,
        title: '嵌套对话',
        lastMessage: '你好',
        updateTime: undefined,
        createTime: '2026-05-24',
      },
    ])
    await expect(agentApi.messageList(11)).resolves.toMatchObject([
      {
        id: 12,
        role: 'assistant',
        content: '已完成',
        toolCalls: [{ function: { name: 'kb_rag_search', arguments: '' } }],
        tokenUsage: { input: 2, output: 9 },
      },
    ])
  })

  it('normalizes paged backend messages and serialized tool calls', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [
        {
          id: 130,
          senderType: 2,
          content: '分析完成',
          tokens: 20,
          toolCalls: '[{"toolName":"kb_rag_search","arguments":"{\\"query\\":\\"面膜\\"}","result":"命中资料","success":true,"round":1}]',
          rating: 'up',
          createTime: '2026-05-20T10:00:00+08:00',
        },
      ],
      pageNum: 0,
      pageSize: 50,
    })

    const result = await agentApi.messageList(9)

    expect(result).toEqual([
      {
        id: 130,
        role: 'assistant',
        content: '分析完成',
        toolCalls: [
          {
            id: 'kb_rag_search-1',
            type: 'function',
            function: { name: 'kb_rag_search', arguments: '{"query":"面膜"}' },
            result: '命中资料',
          },
        ],
        rating: 'up',
        createdAt: '2026-05-20T10:00:00+08:00',
        tokenUsage: { input: 0, output: 20 },
      },
    ])
  })

  it('conversationDelete posts id as query params for backend request params', async () => {
    mockPost.mockResolvedValue(undefined)
    await agentApi.conversationDelete(11)
    expect(mockPost).toHaveBeenCalledWith('/agent/conversation/delete', undefined, { params: { id: 11 } })
  })

  it('messageRate posts rating payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await agentApi.messageRate(15, 'up')
    expect(mockPost).toHaveBeenCalledWith('/agent/message/rate', { messageId: 15, rating: 'up' })
  })

  it('normalizes wrapped workflow list and execution list responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        items: [{
          id: 12,
          userId: 1,
          name: '包装工作流',
          version: 1,
          status: 1,
          createTime: '2026-05-22 10:00:00',
          steps: [],
        }],
        count: 1,
      })
      .mockResolvedValueOnce({
        data: [{
          id: 21,
          workflowId: 12,
          workflowName: '包装工作流',
          userId: 1,
          status: 1,
          statusLabel: '已完成',
          currentStepOrder: 2,
          totalSteps: 2,
        }],
        total: 1,
      })

    const workflows = await workflowApi.list(0, 100)
    const executions = await workflowApi.executionList(0, 100)

    expect(workflows.list[0].name).toBe('包装工作流')
    expect(executions.list[0].workflowName).toBe('包装工作流')
  })

  it('normalizes share and review list wrappers', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          share: {
            id: '4',
            shareCode: 'abc',
            conversationId: '9',
            agentId: '3',
            title: '分享内容',
            messageCount: '1',
            viewCount: '8',
          },
          messages: { result: { items: [{ id: 99, role: 'assistant', content: 'Markdown **ok**' }] } },
        },
      })
      .mockResolvedValueOnce({
        result: {
          records: [{ id: '7', agentId: '3', userId: '2', username: '小张', rating: '5', createTime: '2026-05-24' }],
          totalCount: '1',
        },
      })

    const share = await (await import('../agent')).shareApi.getData('abc')
    const reviews = await (await import('../agent')).reviewApi.list(3, 0, 10)

    expect(share.share).toMatchObject({ id: 4, shareCode: 'abc', messageCount: 1, viewCount: 8 })
    expect(share.messages).toMatchObject([{ id: 99, role: 'assistant', content: 'Markdown **ok**' }])
    expect(reviews).toMatchObject({
      total: 1,
      list: [{ id: 7, agentId: 3, userId: 2, userName: '小张', rating: 5 }],
    })
  })

  it('normalizes workflow execution detail wrappers', async () => {
    mockPost.mockResolvedValue({
      data: {
        id: '31',
        workflowId: '12',
        workflowName: '深层执行',
        userId: '1',
        status: '1',
        statusText: '已完成',
        currentStepOrder: '2',
        totalSteps: '2',
        contextData: '{"sku":"A100"}',
        durationSeconds: '9',
        stepResults: {
          records: [{ stepOrder: '1', name: '分析', agentId: '7', success: true, output: 'ok' }],
        },
      },
    })

    const result = await workflowApi.executionGet(31)

    expect(result).toMatchObject({
      id: 31,
      workflowId: 12,
      workflowName: '深层执行',
      statusLabel: '已完成',
      contextData: { sku: 'A100' },
      durationSeconds: 9,
      steps: [{ stepOrder: 1, stepName: '分析', agentId: 7, success: true, output: 'ok' }],
    })
  })

  it('normalizes workflow detail steps and serialized dependencies', async () => {
    mockPost.mockResolvedValue({
      id: 12,
      userId: 1,
      name: '包装详情工作流',
      version: 1,
      status: 1,
      createTime: '2026-05-22 10:00:00',
      steps: {
        records: [
          {
            id: 31,
            stepOrder: 2,
            agentId: 7,
            stepName: '二次改写',
            outputKey: 'rewrite',
            dependsOn: '["analysis"]',
            executionMode: 1,
            retryCount: 2,
          },
        ],
      },
    })

    const workflow = await workflowApi.get(12)

    expect(workflow).toMatchObject({
      id: 12,
      name: '包装详情工作流',
      steps: [
        {
          id: 31,
          stepOrder: 2,
          agentId: 7,
          stepName: '二次改写',
          outputKey: 'rewrite',
          dependsOn: ['analysis'],
          executionMode: 1,
          retryCount: 2,
          timeoutSeconds: 300,
        },
      ],
    })
    expect(mockPost).toHaveBeenCalledWith('/agent/workflow/get', { id: 12 })
  })
})
