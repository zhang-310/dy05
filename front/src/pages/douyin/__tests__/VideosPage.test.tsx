import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import VideosPage from '../VideosPage'
import request from '@/utils/request'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('VideosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(request.post).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          title: '爆款视频标题',
          playCount: 10000,
          likeCount: 800,
          commentCount: 60,
          shareCount: 20,
          duration: 30,
          status: 1,
          publishTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads video list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <VideosPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(request.post).toHaveBeenCalledWith('/douyin/video/search', { page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('爆款视频标题')).toBeInTheDocument()
      expect(screen.getByText('发布')).toBeInTheDocument()
    })
  })
})
