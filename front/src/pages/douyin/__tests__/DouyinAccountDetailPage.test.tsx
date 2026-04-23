import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import DouyinAccountDetailPage from '../DouyinAccountDetailPage'
import { douyinApi } from '@/api/douyin'

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    accountGet: vi.fn(),
    accountStats: vi.fn(),
  },
}))

describe('DouyinAccountDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(douyinApi.accountGet).mockResolvedValue({
      id: 1,
      userId: 1,
      accountName: '美妆达人号',
      accountId: 'beauty001',
      fanCount: 25000,
      status: 1,
      createTime: '2026-04-10 10:00:00',
    } as never)
    vi.mocked(douyinApi.accountStats).mockResolvedValue({
      accountId: 'beauty001',
      totalVideos: 120,
      totalViews: 500000,
      totalLikes: 80000,
      totalShares: 5000,
      totalComments: 6000,
      avgViewsPerVideo: 4166,
      avgLikesPerVideo: 666,
    } as never)
  })

  it('loads account detail and renders summary data', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/douyin/account/1']}>
        <Routes>
          <Route path="/douyin/account/:id" element={<DouyinAccountDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(douyinApi.accountGet).toHaveBeenCalledWith(1)
      expect(douyinApi.accountStats).toHaveBeenCalledWith(1)
    })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: '美妆达人号' })).toBeInTheDocument()
      expect(screen.getByText('基本信息')).toBeInTheDocument()
      expect(screen.getByText('数据统计')).toBeInTheDocument()
    })
  })
})
