import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { wecomApi } from '../wecom'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('wecom API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts robot query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await wecomApi.list({ page: 0, rows: 20, robotName: '通知机器人' })
    expect(mockPost).toHaveBeenCalledWith('/wecom/robot/list', {
      page: 0,
      rows: 20,
      robotName: '通知机器人',
    })
  })

  it('manualPush posts rule id and data payload', async () => {
    mockPost.mockResolvedValue({ success: true })
    await wecomApi.manualPush(7, { sessionId: 99 })
    expect(mockPost).toHaveBeenCalledWith('/wecom/rule/manual-push', { ruleId: 7, sessionId: 99 })
  })

  it('retryPush posts log id payload', async () => {
    mockPost.mockResolvedValue({ success: true })
    await wecomApi.retryPush(12)
    expect(mockPost).toHaveBeenCalledWith('/wecom/log/retry', { logId: 12 })
  })

  it('templateList posts empty payload when params omitted', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await wecomApi.templateList()
    expect(mockPost).toHaveBeenCalledWith('/wecom/template/list', {})
  })
})
