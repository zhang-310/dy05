import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import DouyinAccountDetailPage from '../DouyinAccountDetailPage'
import { douyinApi } from '@/api/douyin'

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    accountGet: vi.fn(),
    accountStats: vi.fn(),
    tokenStatus: vi.fn(),
    oauthUrl: vi.fn(),
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
    vi.mocked(douyinApi.tokenStatus).mockResolvedValue({
      status: 'valid',
      expireTime: '2026-06-01 10:00:00',
      daysLeft: 17,
    } as never)
    vi.mocked(douyinApi.oauthUrl).mockResolvedValue({ authUrl: 'https://douyin.example/oauth' } as never)
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
      expect(douyinApi.tokenStatus).toHaveBeenCalledWith(1)
    })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: '美妆达人号' })).toBeInTheDocument()
      expect(screen.getByText('基本信息')).toBeInTheDocument()
      expect(screen.getByText('数据统计')).toBeInTheDocument()
      expect(screen.getByText('Token 状态')).toBeInTheDocument()
      expect(screen.getByText('剩余天数：17 天')).toBeInTheDocument()
      expect(screen.getByText(/Token 生命周期来自独立 OAuth 接口/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('douyin-account-detail-page')).toHaveAttribute('data-readonly-detail-page', 'true')
    expect(screen.getByTestId('douyin-account-detail-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/douyin/account/browser-scrape'),
    )
    expect(screen.getByTestId('douyin-account-detail-boundary-contract')).toHaveAttribute('data-no-video-prefetch', 'true')
    expect(screen.getByTestId('douyin-account-basic-card')).toHaveAttribute('data-source-endpoint', '/douyin/account/get')
    expect(screen.getByTestId('douyin-account-stats-card')).toHaveAttribute('data-no-static-statistics-fallback', 'true')
    expect(screen.getByTestId('douyin-account-token-contract')).toHaveAttribute('data-token-error-non-blocking', 'true')
  })

  it('keeps account detail visible when token status fails', async () => {
    vi.mocked(douyinApi.tokenStatus).mockRejectedValue(new Error('oauth down') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/douyin/account/1']}>
        <Routes>
          <Route path="/douyin/account/:id" element={<DouyinAccountDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '美妆达人号' })).toBeInTheDocument()
    expect(screen.getByText('数据统计')).toBeInTheDocument()
    expect(screen.getByText(/Token 状态暂不可用/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-account-token-error')).toHaveAttribute('data-no-local-token-fallback', 'true')
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    await waitFor(() => {
      expect(douyinApi.tokenStatus).toHaveBeenCalledTimes(2)
    })
  })

  it('shows endpoint and account context when account statistics fail', async () => {
    vi.mocked(douyinApi.accountStats).mockRejectedValueOnce(new Error('stats down') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/douyin/account/1']}>
        <Routes>
          <Route path="/douyin/account/:id" element={<DouyinAccountDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/\/douyin\/account\/statistics 账号统计加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/douyin\/accounts\/:id; accountPk=1/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-account-detail-error')).toHaveAttribute('data-no-local-account-fallback', 'true')
    expect(screen.getByTestId('douyin-account-detail-error')).toHaveAttribute('data-no-static-statistics-fallback', 'true')
  })

  it('shows auth-url failure in page with endpoint context', async () => {
    vi.mocked(douyinApi.oauthUrl).mockRejectedValueOnce(new Error('oauth url down') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/douyin/account/1']}>
        <Routes>
          <Route path="/douyin/account/:id" element={<DouyinAccountDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await screen.findByRole('heading', { name: '美妆达人号' })
    fireEvent.click(screen.getByRole('button', { name: '重新授权' }))
    expect(await screen.findByTestId('douyin-account-oauth-error')).toHaveAttribute('data-no-local-oauth-url-fallback', 'true')
    expect(screen.getByText(/\/douyin\/oauth\/auth-url 获取授权链接失败/)).toBeInTheDocument()
    expect(screen.getByText(/accountPk=1/)).toBeInTheDocument()
  })
})
