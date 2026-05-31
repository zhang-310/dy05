import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import SvAccountDetailPage from '../SvAccountDetailPage'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import {
  accountAnalytics,
  accountGet,
  accountRefreshStats,
  accountUpdate,
  accountVideos,
  type AccountVideo,
  type SvAccountAnalytics,
  type SvAccountDetail,
} from '@/api/sv-account'
import { batchDeepAnalyze } from '@/api/viral-analysis'

vi.mock('@/api/sv-account', () => ({
  accountGet: vi.fn(),
  accountUpdate: vi.fn(),
  accountVideos: vi.fn(),
  accountRefreshStats: vi.fn(),
  accountAnalytics: vi.fn(),
}))

vi.mock('@/api/viral-analysis', () => ({
  batchDeepAnalyze: vi.fn(),
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

const navigate = vi.fn()
let searchParamsState = new URLSearchParams()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useSearchParams: () => [
      searchParamsState,
      vi.fn((next: Record<string, string> | URLSearchParams) => {
        searchParamsState = next instanceof URLSearchParams ? next : new URLSearchParams(next)
      }),
    ],
  }
})

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, actionSlot }: any) => (
      <div>
        {actionSlot}
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({
                          row,
                          value: row[col.field],
                        })
                      : String(row[col.field] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  }
})

const account: SvAccountDetail = {
  id: 7,
  secUid: 'sec-7',
  douyinId: 'skin-ops',
  nickname: '护肤实验室',
  avatarUrl: 'https://cdn.test/avatar.jpg',
  signature: '用真实实验拆解护肤内容',
  followerCount: 125000,
  followingCount: 12,
  totalFavorited: 500000,
  videoCount: 88,
  isVerified: true,
  verificationType: '企业认证',
  collectCount: 3,
  totalCollectedVideos: 2,
  avgViewCount: 22000,
  avgLikeCount: 1300,
  avgViralScore: 82.6,
  topViralScore: 96.3,
  industryTags: '["护肤"]',
  contentTags: '["实验","测评"]',
  accountCategory: '美妆',
  sourceType: 'keyword_search',
  sourceKeyword: '精华液',
  status: 'active',
  notes: '重点跟踪账号',
  createTime: '2026-05-01 10:00:00',
  updateTime: '2026-05-10 10:00:00',
  taskCount: 2,
  pendingAnalysisCount: 1,
  analyzedCount: 1,
}

const accountVideo: AccountVideo = {
  id: 101,
  title: '精华液三秒开场',
  coverUrl: 'https://cdn.test/video-cover.jpg',
  videoUrl: 'https://douyin.test/video/101',
  douyinVideoId: 'aweme-101',
  authorName: '护肤实验室',
  viewCount: 31000,
  likeCount: 2100,
  shareCount: 130,
  viralScore: 91,
  deepAnalyzeStatus: 'pending',
  deepAnalyzeProgress: '等待拆解',
  createTime: '2026-05-11 10:00:00',
}

const analytics: SvAccountAnalytics = {
  videoCount: 2,
  sumViewCount: 62000,
  sumLikeCount: 4200,
  sumShareCount: 260,
  avgViewPerVideo: 31000,
  avgLikePerVideo: 2100,
  avgSharePerVideo: 130,
  avgViralScore: 91.5,
  maxViewCount: 45000,
  minViewCount: 17000,
  deepPendingCount: 1,
  deepProcessingCount: 0,
  deepCompletedCount: 1,
  deepFailedCount: 0,
  deepOtherCount: 0,
}

function renderPage(initialEntry = '/shortvideo/accounts/7') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/shortvideo/accounts/:id" element={<SvAccountDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderPageWithTheme(initialEntry = '/shortvideo/accounts/7') {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/shortvideo/accounts/:id" element={<SvAccountDetailPage />} />
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('SvAccountDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    searchParamsState = new URLSearchParams()
    vi.mocked(accountGet).mockResolvedValue(account as never)
    vi.mocked(accountUpdate).mockResolvedValue(undefined as never)
    vi.mocked(accountRefreshStats).mockResolvedValue(undefined as never)
    vi.mocked(accountVideos).mockResolvedValue({
      total: 1,
      list: [accountVideo],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(accountAnalytics).mockResolvedValue(analytics as never)
  })

  it('loads account detail and saves edited metadata', async () => {
    renderPage()

    expect(await screen.findByText('账号详情')).toBeInTheDocument()
    const root = screen.getByTestId('sv-account-detail-page')
    expect(root).toHaveAttribute('data-ready-endpoints', '/short-video/account/get|/short-video/account/update|/short-video/account/refresh-stats|/short-video/account/videos|/short-video/account/analytics|/short-video/viral/deep-analyze/batch')
    expect(root).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/accounts/:id?tab=analysis'))
    expect(root).toHaveAttribute('data-supported-actions', expect.stringContaining('submit-page-pending-deep-analyze'))
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/account/static-analytics'))
    expect(root).toHaveAttribute('data-no-local-account-fallback', 'true')
    expect(screen.getByTestId('sv-account-detail-back-button')).toHaveAttribute('data-target-route', shortvideoRoutes.accounts)
    expect(screen.getByTestId('sv-account-refresh-stats-button')).toHaveAttribute('data-source-endpoint', '/short-video/account/refresh-stats')
    expect(accountGet).toHaveBeenCalledWith(7)
    expect(screen.getByText('护肤实验室')).toBeInTheDocument()
    expect(screen.getByText('12.5万')).toBeInTheDocument()
    expect(screen.getByText('关键词采集：精华液')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /刷新统计/ }))
    await waitFor(() => {
      expect(accountRefreshStats).toHaveBeenCalledWith(7)
      expect(toast).toHaveBeenCalledWith('统计数据已刷新', 'success')
    })

    fireEvent.click(screen.getByRole('button', { name: /编辑/ }))
    fireEvent.change(screen.getByPlaceholderText('如：美妆、护肤、彩妆'), {
      target: { value: '功效护肤' },
    })
    fireEvent.change(screen.getByPlaceholderText('添加备注信息'), {
      target: { value: '优先二创账号' },
    })
    fireEvent.click(screen.getByRole('button', { name: /保存/ }))

    await waitFor(() => {
      expect(accountUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 7,
          accountCategory: '功效护肤',
          notes: '优先二创账号',
        }),
      )
      expect(toast).toHaveBeenCalledWith('账号信息已更新', 'success')
    })
  })

  it('keeps editing values and shows endpoint when update fails', async () => {
    vi.mocked(accountUpdate).mockRejectedValueOnce(new Error('update blocked') as never)

    renderPage()

    expect(await screen.findByText('账号详情')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /编辑/ }))
    fireEvent.change(screen.getByPlaceholderText('如：美妆、护肤、彩妆'), {
      target: { value: '功效护肤' },
    })
    fireEvent.change(screen.getByPlaceholderText('添加备注信息'), {
      target: { value: '失败后继续编辑' },
    })
    fireEvent.click(screen.getByRole('button', { name: /保存/ }))

    expect(await screen.findByText(/保存账号信息失败（POST \/short-video\/account\/update）：update blocked/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-update-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('sv-account-update-error')).toHaveAttribute('data-no-local-account-mutation', 'true')
    expect(screen.getByText(/\/short-video\/account\/update/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('功效护肤')).toBeInTheDocument()
    expect(screen.getByDisplayValue('失败后继续编辑')).toBeInTheDocument()
  })

  it('shows refresh-stats endpoint when refresh fails', async () => {
    vi.mocked(accountRefreshStats).mockRejectedValueOnce(new Error('refresh down') as never)

    renderPage()

    expect(await screen.findByText('账号详情')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /刷新统计/ }))

    expect(await screen.findByText(/刷新统计失败（POST \/short-video\/account\/refresh-stats）：refresh down/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-refresh-error')).toHaveAttribute('data-no-local-refresh-stats', 'true')
    expect(screen.getByText(/\/short-video\/account\/refresh-stats/)).toBeInTheDocument()
  })

  it('loads account videos, filters rows, and starts deep analysis', async () => {
    searchParamsState = new URLSearchParams({ tab: 'videos' })
    renderPage('/shortvideo/accounts/7?tab=videos')

    expect(await screen.findByText('精华液三秒开场')).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-videos-grid')).toHaveAttribute('data-source-endpoint', '/short-video/account/videos')
    expect(screen.getByTestId('sv-account-videos-grid')).toHaveAttribute('data-no-local-video-fallback', 'true')
    expect(screen.getByTestId('sv-account-videos-apply-filter-button')).toHaveAttribute('data-source-endpoint', '/short-video/account/videos')
    expect(screen.getByTestId('sv-account-videos-refresh-button')).toHaveAttribute('data-source-endpoint', '/short-video/account/videos')
    expect(screen.getByTestId('sv-account-video-open-viral-button')).toHaveAttribute('data-target-route', expect.stringContaining('/shortvideo/viral-videos?videoId=101'))
    expect(screen.getByTestId('sv-account-video-deep-analyze-button')).toHaveAttribute('data-source-endpoint', '/short-video/viral/deep-analyze/batch')
    expect(screen.getByTestId('sv-account-deep-analyze-page-pending-button')).toHaveAttribute('data-no-local-deep-analyze', 'true')
    expect(accountVideos).toHaveBeenCalledWith(
      expect.objectContaining({
        accountId: 7,
        page: 0,
        rows: 20,
        sortName: 'createTime',
        sortOrder: 'desc',
      }),
    )

    fireEvent.change(screen.getByLabelText('标题关键词'), {
      target: { value: '精华液' },
    })
    fireEvent.mouseDown(screen.getByLabelText('深度分析状态'))
    fireEvent.click(within(screen.getByRole('listbox')).getByText('待分析'))
    fireEvent.click(screen.getByRole('button', { name: '应用筛选' }))

    await waitFor(() => {
      expect(accountVideos).toHaveBeenLastCalledWith(
        expect.objectContaining({
          keyword: '精华液',
          deepAnalyzeStatus: 'pending',
        }),
      )
    })

    expect(screen.getByRole('button', { name: '本页待分析排队' })).toBeEnabled()
  })

  it('uses theme-aware video filter and batch panel surfaces in dark mode', async () => {
    searchParamsState = new URLSearchParams({ tab: 'videos' })
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme('/shortvideo/accounts/7?tab=videos')

    expect(await screen.findByText('精华液三秒开场')).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-video-filter-header')).toHaveStyle({
      backgroundColor: 'rgba(255, 255, 255, 0.04)',
    })
    expect(screen.getByTestId('sv-account-video-batch-panel')).toHaveStyle({
      backgroundColor: 'rgba(0, 0, 0, 0.12)',
    })
  })

  it('keeps video actions available and shows endpoint when batch deep analyze fails', async () => {
    vi.mocked(batchDeepAnalyze).mockRejectedValueOnce(new Error('queue down') as never)
    searchParamsState = new URLSearchParams({ tab: 'videos' })

    renderPage('/shortvideo/accounts/7?tab=videos')

    expect(await screen.findByText('精华液三秒开场')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '本页待分析排队' }))

    expect(await screen.findByText(/深度拆解提交失败（POST \/short-video\/viral\/deep-analyze\/batch）：queue down/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-deep-analyze-error')).toHaveAttribute('data-no-local-deep-analyze', 'true')
    expect(screen.getByTestId('sv-account-deep-analyze-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/\/short-video\/viral\/deep-analyze\/batch/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '本页待分析排队' })).toBeEnabled()
  })

  it('loads account analytics and renders aggregate state', async () => {
    searchParamsState = new URLSearchParams({ tab: 'analysis' })
    renderPage('/shortvideo/accounts/7?tab=analysis')

    expect(await screen.findByText('以下统计基于该账号关联的全部已采集视频（爆款库）实时聚合，用于横向对比账号整体内容表现与深度分析进度。')).toBeInTheDocument()

    await waitFor(() => {
      expect(accountAnalytics).toHaveBeenCalledWith(7)
    })

    expect(screen.getByText('已采集视频条数')).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-analytics-panel')).toHaveAttribute('data-source-endpoint', '/short-video/account/analytics')
    expect(screen.getByText('条均播放 / 累计播放')).toBeInTheDocument()
    expect(screen.getByText('累计 6.2万 · 单条最高 4.5万')).toBeInTheDocument()
    expect(screen.getByText('深度分析状态（全量已采视频）')).toBeInTheDocument()
    expect(screen.getByText('待分析 1')).toBeInTheDocument()
    expect(screen.getByText('已完成 1')).toBeInTheDocument()
  })

  it('shows empty analytics guidance when no collected videos exist', async () => {
    vi.mocked(accountAnalytics).mockResolvedValue({
      ...analytics,
      videoCount: 0,
    } as never)
    searchParamsState = new URLSearchParams({ tab: 'analysis' })

    renderPage('/shortvideo/accounts/7?tab=analysis')

    expect(await screen.findByText(/当前账号在系统中尚无已采集入库的短视频/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-analytics-empty')).toHaveAttribute('data-no-static-analytics-fallback', 'true')
    expect(screen.getByText('详情页快照（账号表）')).toBeInTheDocument()
    expect(screen.getByText('平均播放 2.2万 · 平均点赞 1300 · 任务 2 个')).toBeInTheDocument()
  })

  it('shows inline errors for account and analytics failures', async () => {
    vi.mocked(accountGet).mockRejectedValueOnce(new Error('账号不存在') as never)

    renderPage()

    expect(await screen.findByText(/账号详情加载失败（POST \/short-video\/account\/get）：账号不存在/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-detail-load-error')).toHaveAttribute('data-no-local-account-fallback', 'true')
  })

  it('shows video and analytics endpoint failures without local/static fallback', async () => {
    vi.mocked(accountVideos).mockRejectedValueOnce(new Error('videos down') as never)
    vi.mocked(accountAnalytics).mockRejectedValueOnce(new Error('analytics down') as never)
    searchParamsState = new URLSearchParams({ tab: 'videos' })

    renderPage('/shortvideo/accounts/7?tab=videos')

    expect(await screen.findByText(/账号视频加载失败（POST \/short-video\/account\/videos）：videos down/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-videos-error')).toHaveAttribute('data-no-local-video-fallback', 'true')
    expect(screen.queryByText('本地视频')).not.toBeInTheDocument()

    searchParamsState = new URLSearchParams({ tab: 'analysis' })
    renderPage('/shortvideo/accounts/7?tab=analysis')

    expect(await screen.findByText(/账号综合分析加载失败（POST \/short-video\/account\/analytics）：analytics down/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-analytics-error')).toHaveAttribute('data-no-static-analytics-fallback', 'true')
  })
})
