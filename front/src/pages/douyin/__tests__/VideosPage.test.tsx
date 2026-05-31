import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import VideosPage from '../VideosPage'
import { douyinApi } from '@/api/douyin'

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    videoSearch: vi.fn(),
  },
}))

describe('VideosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(douyinApi.videoSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          accountId: 1,
          videoId: 'v-1',
          title: '爆款视频标题',
          description: '测试视频',
          viewCount: 10000,
          likeCount: 800,
          commentCount: 60,
          shareCount: 20,
          downloadCount: 0,
          videoType: 'normal',
          publishTime: '2026-04-10 10:00:00',
          createTime: '2026-04-10 10:00:00',
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
      expect(douyinApi.videoSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('爆款视频标题')).toBeInTheDocument()
      expect(screen.getByText('itemId v-1')).toBeInTheDocument()
      expect(screen.getByText('普通视频')).toBeInTheDocument()
      expect(screen.getByText('当前页播放')).toBeInTheDocument()
      expect(screen.getByText(/当前后端视频 VO 未返回封面 URL/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('douyin-videos-page')).toHaveAttribute('data-search-list-only', 'true')
    expect(screen.getByTestId('douyin-videos-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/douyin/video/browser-scrape'),
    )
    expect(screen.getByTestId('douyin-videos-boundary-contract')).toHaveAttribute('data-server-filter-payload', 'true')
    expect(screen.getByTestId('douyin-videos-sync-downgrade')).toHaveAttribute('data-readonly-search-page', 'true')
    expect(screen.getByTestId('douyin-videos-grid-contract')).toHaveAttribute('data-no-detail-prefetch', 'true')
  })

  it('submits backend supported title, accountId and videoType filters', async () => {
    renderWithProviders(
      <MemoryRouter>
        <VideosPage />
      </MemoryRouter>,
    )

    await screen.findByText('爆款视频标题')
    fireEvent.change(screen.getByLabelText('视频标题'), { target: { value: '屏障' } })
    fireEvent.change(screen.getByLabelText('账号ID'), { target: { value: '7' } })
    fireEvent.mouseDown(screen.getByLabelText('视频类型'))
    fireEvent.click(await screen.findByRole('option', { name: '普通视频' }))
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    await waitFor(() => {
      expect(douyinApi.videoSearch).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        title: '屏障',
        accountId: 7,
        videoType: 'normal',
      })
    })
    expect(screen.getByText(/只提交后端真实支持的 accountId、title、videoType 和分页字段/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-videos-boundary-contract')).toHaveAttribute('data-source-endpoint', '/douyin/video/search')
  })

  it('shows video search failure with endpoint and filter context', async () => {
    vi.mocked(douyinApi.videoSearch).mockRejectedValueOnce(new Error('video search down') as never)

    renderWithProviders(
      <MemoryRouter>
        <VideosPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/\/douyin\/video\/search 视频列表加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/douyin\/videos; accountId=空; title=空; videoType=全部; page=0; rows=20/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-videos-list-error')).toHaveAttribute('data-no-local-video-fallback', 'true')
  })

  it('keeps empty state explicit without browser scrape fallback', async () => {
    vi.mocked(douyinApi.videoSearch).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <VideosPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('douyin-videos-empty')).toHaveAttribute('data-no-browser-direct-scrape', 'true')
    expect(screen.getByText(/当前筛选条件没有视频数据/)).toBeInTheDocument()
  })
})
