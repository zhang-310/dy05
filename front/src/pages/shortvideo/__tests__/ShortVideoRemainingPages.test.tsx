import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import DramaPage from '../DramaPage'
import SvAccountListPage from '../SvAccountListPage'
import ViralChainPage from '../ViralChainPage'
import { shortvideoApi, type SvDrama } from '@/api/shortvideo'
import { accountDelete, accountList, type SvAccount } from '@/api/sv-account'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    dramaList: vi.fn(),
    dramaCreate: vi.fn(),
    dramaUpdate: vi.fn(),
    dramaDelete: vi.fn(),
    dramaGenerateScript: vi.fn(),
    viralAnalyze: vi.fn(),
  },
}))

vi.mock('@/api/sv-account', () => ({
  accountList: vi.fn(),
  accountDelete: vi.fn(),
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, actionSlot, searchSlot }: any) => (
      <div>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueGetter ? col.valueGetter(row[col.field], row) : row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

const drama: SvDrama = {
  id: 3,
  title: '反转短剧',
  genre: '都市',
  totalEpisodes: 12,
  episodesWithSynopsis: 6,
  episodesWithProject: 2,
  status: 'draft',
  createTime: '2026-05-21 10:00:00',
}

const account: SvAccount = {
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
}

function renderPage(ui: React.ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('Short video remaining pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(shortvideoApi.dramaList).mockResolvedValue([drama] as never)
    vi.mocked(shortvideoApi.dramaCreate).mockResolvedValue({ ...drama, id: 4, title: '新短剧' } as never)
    vi.mocked(shortvideoApi.dramaUpdate).mockResolvedValue(drama as never)
    vi.mocked(shortvideoApi.dramaDelete).mockResolvedValue(undefined as never)
    vi.mocked(shortvideoApi.dramaGenerateScript).mockResolvedValue('第一集：高反转开场' as never)
    vi.mocked(shortvideoApi.viralAnalyze).mockResolvedValue({ ok: true, id: 18 } as never)
    vi.mocked(accountList).mockResolvedValue({
      total: 1,
      list: [account],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(accountDelete).mockResolvedValue(undefined as never)
  })

  it('DramaPage exposes real drama contracts and generates script by dramaId', async () => {
    renderPage(<DramaPage />)

    expect(await screen.findByRole('heading', { name: '剧情短视频创作' })).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-drama-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/short-video/drama/list|/short-video/drama/create|/short-video/drama/update|/short-video/drama/delete|/short-video/drama/generate-script',
    )
    expect(screen.getByTestId('shortvideo-drama-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/drama/local-script-template'),
    )
    expect(screen.getByTestId('drama-boundary-contract')).toHaveAttribute('data-no-local-drama-fallback', 'true')
    expect(screen.getByTestId('drama-summary')).toHaveAttribute('data-no-static-summary', 'true')
    expect(screen.getAllByText(/\/short-video\/drama\/\*/).length).toBeGreaterThan(0)
    expect(await screen.findByText('反转短剧')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('短剧 ID'), { target: { value: '3' } })
    fireEvent.change(screen.getByPlaceholderText('主题 / 风格提示...'), { target: { value: '高反转' } })
    fireEvent.click(screen.getByRole('button', { name: '生成剧本' }))

    await waitFor(() => {
      expect(shortvideoApi.dramaGenerateScript).toHaveBeenCalledWith({
        dramaId: 3,
        theme: '高反转',
      })
    })
    expect(await screen.findByText(/第一集：高反转开场/)).toBeInTheDocument()
    expect(screen.getByTestId('drama-script-result-preview')).toHaveAttribute('data-source-endpoint', '/short-video/drama/generate-script')
  })

  it('DramaPage creates and deletes drama through backend endpoints', async () => {
    renderPage(<DramaPage />)

    expect(await screen.findByText('反转短剧')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '新建剧情项目' }))
    fireEvent.change(screen.getByLabelText('剧情标题'), { target: { value: '新短剧' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.dramaCreate).toHaveBeenCalledWith(expect.objectContaining({
        title: '新短剧',
        totalEpisodes: 10,
      }))
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '新建剧情项目' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(shortvideoApi.dramaDelete).toHaveBeenCalledWith(3)
    })
  })

  it('DramaPage tolerates wrapped list mocks and shows action errors inline', async () => {
    vi.mocked(shortvideoApi.dramaList).mockResolvedValue({
      records: [{ ...drama, id: 8, title: '包装短剧项目' }],
    } as never)
    vi.mocked(shortvideoApi.dramaGenerateScript).mockRejectedValue(new Error('drama service down') as never)

    renderPage(<DramaPage />)

    expect(await screen.findByText('包装短剧项目')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('短剧 ID'), { target: { value: '8' } })
    fireEvent.change(screen.getByPlaceholderText('主题 / 风格提示...'), { target: { value: '包装失败' } })
    fireEvent.click(screen.getByRole('button', { name: '生成剧本' }))

    expect(await screen.findByText(/剧本生成失败（POST \/short-video\/drama\/generate-script）：drama service down/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('8')).toBeInTheDocument()
    expect(screen.getByDisplayValue('包装失败')).toBeInTheDocument()
    expect(screen.getByTestId('drama-script-error')).toHaveAttribute('data-input-retained', 'true')
  })

  it('DramaPage uses theme-aware script result surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <DramaPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByText('反转短剧')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('短剧 ID'), { target: { value: '3' } })
    fireEvent.change(screen.getByPlaceholderText('主题 / 风格提示...'), { target: { value: '高反转' } })
    fireEvent.click(screen.getByRole('button', { name: '生成剧本' }))

    expect(await screen.findByText(/第一集：高反转开场/)).toBeInTheDocument()
    expect(screen.getByTestId('drama-script-result-preview')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('DramaPage keeps form and row context when save or delete fails', async () => {
    vi.mocked(shortvideoApi.dramaCreate).mockRejectedValueOnce(new Error('title duplicate') as never)
    vi.mocked(shortvideoApi.dramaDelete).mockRejectedValueOnce(new Error('episodes linked') as never)

    renderPage(<DramaPage />)

    expect(await screen.findByText('反转短剧')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '新建剧情项目' }))
    const dialog = await screen.findByRole('dialog', { name: '新建剧情项目' })
    fireEvent.change(within(dialog).getByLabelText('剧情标题'), { target: { value: '重复短剧' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/短剧保存失败（POST \/short-video\/drama\/create）：title duplicate/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('重复短剧')).toBeInTheDocument()
    expect(screen.getByTestId('drama-action-error')).toHaveAttribute('data-no-local-drama-mutation', 'true')
    expect(screen.getByTestId('drama-form')).toHaveAttribute('data-input-retained', 'true')

    fireEvent.click(within(dialog).getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '新建剧情项目' })).not.toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/短剧删除失败（POST \/short-video\/drama\/delete）：episodes linked/)).length).toBeGreaterThan(0)
    expect(screen.getByText(/上次删除失败：短剧删除失败（POST \/short-video\/drama\/delete）：episodes linked/)).toBeInTheDocument()
    expect(screen.getByText('反转短剧')).toBeInTheDocument()
    expect(screen.getByTestId('drama-action-error')).toHaveAttribute('data-source-endpoints', expect.stringContaining('/short-video/drama/delete'))
  })

  it('SvAccountListPage shows account diagnostics and keeps delete as logical delete', async () => {
    renderPage(<SvAccountListPage />)

    expect(await screen.findByRole('heading', { name: '短视频账号' })).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-list-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/account/list'),
    )
    expect(screen.getByTestId('sv-account-list-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/account/physical-delete'),
    )
    expect(screen.getByTestId('sv-account-list-boundary-contract')).toHaveAttribute('data-logical-delete-only', 'true')
    expect(screen.getByTestId('sv-account-list-diagnostics')).toHaveAttribute('data-current-page-only', 'true')
    expect(screen.getByTestId('sv-account-list-filter-contract')).toHaveAttribute('data-server-filter-payload', 'true')
    expect(screen.getByTestId('sv-account-list-grid-contract')).toHaveAttribute('data-no-account-detail-prefetch', 'true')
    expect(screen.getByText(/\/short-video\/account\/\*/)).toBeInTheDocument()
    expect(screen.getByText(/删除账号为逻辑删除/)).toBeInTheDocument()
    expect(await screen.findByText('护肤实验室')).toBeInTheDocument()
    expect(screen.getByText('账号总数')).toBeInTheDocument()
    expect(screen.getByText('当前页已入库视频')).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('manage-account-videos'))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/accounts/7?tab=videos'))

    fireEvent.click(screen.getByLabelText('delete-account'))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(accountDelete).toHaveBeenCalledWith(7)
    })
  })

  it('SvAccountListPage keeps row visible and shows endpoint when delete fails', async () => {
    vi.mocked(accountDelete).mockRejectedValueOnce(new Error('delete denied') as never)

    renderPage(<SvAccountListPage />)

    expect(await screen.findByText('护肤实验室')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('delete-account'))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/删除账号失败（POST \/short-video\/account\/delete）：delete denied/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-delete-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getAllByText(/\/short-video\/account\/delete/).length).toBeGreaterThan(1)
    expect(screen.getByText(/上次删除失败（POST \/short-video\/account\/delete）：delete denied/)).toBeInTheDocument()
    expect(screen.getByText('护肤实验室')).toBeInTheDocument()
  })

  it('SvAccountListPage shows list error and empty state without local accounts', async () => {
    vi.mocked(accountList).mockRejectedValueOnce(new Error('account list down') as never)

    renderPage(<SvAccountListPage />)

    expect(await screen.findByText(/短视频账号加载失败（POST \/short-video\/account\/list）：account list down/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-account-list-error')).toHaveAttribute('data-no-local-account-fallback', 'true')

    vi.mocked(accountList).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    } as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))

    expect(await screen.findByTestId('sv-account-list-empty')).toHaveAttribute('data-no-local-account-fallback', 'true')
    expect(screen.getByTestId('sv-account-list-empty')).toHaveAttribute('data-no-browser-direct-scrape', 'true')
  })

  it('ViralChainPage only triggers real analysis and documents graph downgrade', async () => {
    renderPage(<ViralChainPage />)

    expect(screen.getByRole('heading', { name: '爆款传播链分析' })).toBeInTheDocument()
    expect(screen.getByTestId('viral-chain-page')).toHaveAttribute('data-ready-endpoints', '/short-video/viral/analyze')
    expect(screen.getByTestId('viral-chain-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/viral-chain/local-graph'),
    )
    expect(screen.getByTestId('viral-chain-boundary-contract')).toHaveAttribute('data-no-graph-node-synthesis', 'true')
    expect(screen.getByTestId('viral-chain-graph-downgrade')).toHaveAttribute('data-no-local-graph-fallback', 'true')
    expect(screen.getByText(/传播链图谱节点\/边接口尚未落库/)).toBeInTheDocument()
    expect(screen.getByText(/不再显示空图表或本地虚拟节点/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '打开爆款库' }))
    expect(navigate).toHaveBeenCalledWith(shortvideoRoutes.viralVideos)

    fireEvent.change(screen.getByLabelText('爆款视频 ID（库内）'), { target: { value: '18' } })
    fireEvent.click(screen.getByRole('button', { name: '触发分析' }))

    await waitFor(() => {
      expect(shortvideoApi.viralAnalyze).toHaveBeenCalledWith(18)
      expect(toast).toHaveBeenCalledWith('爆款 AI 分析任务已提交', 'success')
    })
    expect(await screen.findByTestId('viral-chain-submit-success')).toHaveAttribute('data-result-source', 'analyze-endpoint')
    expect(await screen.findByText(/爆款 #18 的 AI 分析任务已提交/)).toBeInTheDocument()
  })

  it('ViralChainPage shows analysis submit errors inline', async () => {
    vi.mocked(shortvideoApi.viralAnalyze).mockRejectedValue(new Error('viral analyze down') as never)

    renderPage(<ViralChainPage />)

    fireEvent.change(screen.getByLabelText('爆款视频 ID（库内）'), { target: { value: '18' } })
    fireEvent.click(screen.getByRole('button', { name: '触发分析' }))

    expect(await screen.findByText(/分析任务提交失败（POST \/short-video\/viral\/analyze）：viral analyze down/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-chain-analyze-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('18')).toBeInTheDocument()
  })
})
