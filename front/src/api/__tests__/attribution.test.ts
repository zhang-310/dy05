import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { attributionApi } from '../attribution'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('attribution API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('report posts attribution query payload', async () => {
    mockPost.mockResolvedValue([])
    await attributionApi.report({ days: 7, channel: 'live' })
    expect(mockPost).toHaveBeenCalledWith('/attribution/report', { days: 7, channel: 'live' })
  })

  it('sessionCompare wraps session id array', async () => {
    mockPost.mockResolvedValue([])
    await attributionApi.sessionCompare([1, 2, 3])
    expect(mockPost).toHaveBeenCalledWith('/attribution/session/compare', { sessionIds: [1, 2, 3] })
  })

  it('exportReport posts export payload', async () => {
    mockPost.mockResolvedValue({ downloadUrl: 'https://example.com/a.pdf' })
    await attributionApi.exportReport({ days: 30, model: 'last_touch' })
    expect(mockPost).toHaveBeenCalledWith('/attribution/export', { days: 30, model: 'last_touch' })
  })
})
