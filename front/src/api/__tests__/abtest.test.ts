import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { abtestApi } from '../abtest'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('abtest API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts experiment query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await abtestApi.list({ page: 0, rows: 20, experimentName: '直播实验' })
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/list', {
      page: 0,
      rows: 20,
      experimentName: '直播实验',
    })
  })

  it('setWinner posts experiment and variant ids', async () => {
    mockPost.mockResolvedValue(undefined)
    await abtestApi.setWinner(3, 5)
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/set-winner', {
      experimentId: 3,
      variantId: 5,
    })
  })

  it('start posts running status payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await abtestApi.start(9)
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/update-status', { id: 9, status: 1 })
  })

  it('eventRecord posts tracking event payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await abtestApi.eventRecord({ experimentId: 7, variantId: 2, eventType: 'conversion' })
    expect(mockPost).toHaveBeenCalledWith('/abtest/event/record', {
      experimentId: 7,
      variantId: 2,
      eventType: 'conversion',
    })
  })
})
