import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { agentApi } from '../agent'

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

  it('conversationCreate posts agent id and title', async () => {
    mockPost.mockResolvedValue(1)
    await agentApi.conversationCreate(8, '新对话')
    expect(mockPost).toHaveBeenCalledWith('/agent/conversation/create', { agentId: 8, title: '新对话' })
  })

  it('messageSend posts conversation message payload', async () => {
    mockPost.mockResolvedValue(99)
    await agentApi.messageSend(11, '你好')
    expect(mockPost).toHaveBeenCalledWith('/agent/message/send', { conversationId: 11, content: '你好' })
  })

  it('messageRate posts rating payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await agentApi.messageRate(15, 'up')
    expect(mockPost).toHaveBeenCalledWith('/agent/message/rate', { messageId: 15, rating: 'up' })
  })
})
