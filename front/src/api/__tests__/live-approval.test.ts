import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  approveScript,
  getApprovalHistory,
  getPendingApprovals,
  rejectScript,
  submitApproval,
} from '../live-approval'

vi.mock('@/utils/request', () => ({
  default: { post: vi.fn() },
}))

describe('live-approval API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('submitApproval posts sessionId', async () => {
    mockPost.mockResolvedValue(undefined)

    await submitApproval({ sessionId: 12 })

    expect(mockPost).toHaveBeenCalledWith('/live/approval/submit', { sessionId: 12 })
  })

  it('approveScript posts sessionId and scriptId', async () => {
    mockPost.mockResolvedValue(undefined)

    await approveScript({ sessionId: 12, scriptId: 34, comment: 'ok' })

    expect(mockPost).toHaveBeenCalledWith('/live/approval/approve', {
      sessionId: 12,
      scriptId: 34,
      comment: 'ok',
    })
  })

  it('rejectScript posts sessionId and scriptId', async () => {
    mockPost.mockResolvedValue(undefined)

    await rejectScript({ sessionId: 12, scriptId: 34, comment: 'need revise' })

    expect(mockPost).toHaveBeenCalledWith('/live/approval/reject', {
      sessionId: 12,
      scriptId: 34,
      comment: 'need revise',
    })
  })

  it('getApprovalHistory posts sessionId', async () => {
    mockPost.mockResolvedValue({ data: { records: [{ id: 1, sessionId: 12, action: 'approve', operatorId: 9 }] } })

    const result = await getApprovalHistory({ sessionId: 12 })

    expect(mockPost).toHaveBeenCalledWith('/live/approval/history', { sessionId: 12 })
    expect(result).toEqual([
      expect.objectContaining({ id: 1, sessionId: 12, action: 'approve', operatorId: 9 }),
    ])
  })

  it('getPendingApprovals posts without body', async () => {
    mockPost.mockResolvedValue({ items: [{ id: 18, userId: 1, liveTitle: '待审批直播' }] })

    const result = await getPendingApprovals()

    expect(mockPost).toHaveBeenCalledWith('/live/approval/pending')
    expect(result).toEqual([
      expect.objectContaining({ id: 18, liveTitle: '待审批直播' }),
    ])
  })
})
