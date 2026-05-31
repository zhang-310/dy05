import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import KnowledgeSourcePage from '../KnowledgeSourcePage'
import ViralAnalysisPage from '../ViralAnalysisPage'
import { aiApi } from '@/api/ai'
import { douyinApi } from '@/api/douyin'

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbSourceList: vi.fn(),
    kbSourceSave: vi.fn(),
    kbSourceDelete: vi.fn(),
    evolveList: vi.fn(),
    evolveStats: vi.fn(),
    evolveViralGet: vi.fn(),
    evolveTrigger: vi.fn(),
    evolveDelete: vi.fn(),
  },
}))

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    videoSearch: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, searchSlot }: any) => (
      <div>
        <div>{searchSlot}</div>
        {rows.length === 0 ? <div>暂无数据</div> : null}
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueFormatter ? col.valueFormatter(row[col.field]) : row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

function renderPage(ui: React.ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

function renderPageWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('Knowledge source and viral analysis pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')

    vi.mocked(aiApi.kbSourceList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 7,
          sourceName: '产品资料目录',
          sourcePath: '/data/knowledge/product',
          sourceType: 'local',
          fileCount: 12,
          indexCount: 36,
          lastIndexTime: '2026-05-21 09:30:00',
          status: 1,
          createTime: '2026-05-21 08:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.kbSourceSave).mockResolvedValue(8 as never)
    vi.mocked(aiApi.kbSourceDelete).mockResolvedValue(undefined as never)

    vi.mocked(aiApi.evolveList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 31,
          videoId: 101,
          accountId: 5,
          viewCount: 100000,
          qualityScore: 88,
          viralScore: 88,
          reportContent: '{"report":"本分析仅基于元数据"}',
          status: 1,
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.evolveStats).mockResolvedValue({
      viralAnalysisTotal: 1,
      viralAnalysisDone: 1,
      liveReviewTotal: 0,
      liveReviewDone: 0,
      indexQueuePending: 0,
    } as never)
    vi.mocked(aiApi.evolveViralGet).mockResolvedValue({
      id: 31,
      videoId: 101,
      accountId: 5,
      qualityScore: 88,
      viralScore: 88,
      reportContent: '{"report":"本分析仅基于元数据"}',
      successFactors: '元数据表现突出',
      replicableMethods: '复用选题结构',
      status: 1,
    } as never)
    vi.mocked(aiApi.evolveTrigger).mockResolvedValue(31 as never)
    vi.mocked(aiApi.evolveDelete).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.videoSearch).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 30 } as never)
  })

  it('KnowledgeSourcePage uses admin source contract and saves sourcePath', async () => {
    renderPage(<KnowledgeSourcePage />)

    expect(screen.getByRole('heading', { name: '知识来源管理' })).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-source-page')).toHaveAttribute('data-no-legacy-source-sync', 'true')
    expect(screen.getByTestId('knowledge-source-boundary-contract')).toHaveAttribute('data-no-detail-fetch', 'true')
    expect(screen.getByText(/\/ai\/admin\/knowledge-source\/search/)).toBeInTheDocument()
    expect(screen.getByText(/不再调用旧的/)).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.kbSourceList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        keyword: undefined,
        sourceType: undefined,
        status: undefined,
      })
    })
    expect(await screen.findByText('产品资料目录')).toBeInTheDocument()
    expect(screen.getAllByText('/data/knowledge/product').length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '新增来源' }))
    expect(screen.getByTestId('knowledge-source-form-contract')).toHaveAttribute('data-no-browser-local-file-access', 'true')
    fireEvent.change(screen.getByLabelText(/来源名称/), { target: { value: '直播资料目录' } })
    fireEvent.change(screen.getByLabelText(/知识源路径/), { target: { value: '/data/knowledge/live' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(aiApi.kbSourceSave).toHaveBeenCalledWith({
        sourceName: '直播资料目录',
        sourcePath: '/data/knowledge/live',
        sourceType: 'local',
        status: 1,
      })
    })
  })

  it('KnowledgeSourcePage shows backend permission failures inline', async () => {
    vi.mocked(aiApi.kbSourceList).mockRejectedValueOnce(new Error('仅管理员可访问'))

    renderPage(<KnowledgeSourcePage />)

    expect(await screen.findByText(/知识来源加载失败/)).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-source-list-error')).toHaveAttribute('data-no-local-source-fallback', 'true')
    expect(screen.getByText(/仅管理员可访问/)).toBeInTheDocument()
    expect(screen.getAllByText(/POST \/ai\/admin\/knowledge-source\/search/).length).toBeGreaterThan(0)
  })

  it('KnowledgeSourcePage normalizes wrapped source records', async () => {
    vi.mocked(aiApi.kbSourceList).mockResolvedValueOnce({
      records: [
        {
          id: 8,
          sourceName: '包装知识源',
          sourcePath: '/data/knowledge/wrapped',
          sourceType: 'local',
          fileCount: 2,
          indexCount: 6,
          status: 1,
        },
      ],
      totalElements: 1,
    } as never)

    renderPage(<KnowledgeSourcePage />)

    expect(await screen.findByText('包装知识源')).toBeInTheDocument()
    expect(screen.getAllByText('/data/knowledge/wrapped').length).toBeGreaterThan(0)
  })

  it('KnowledgeSourcePage keeps edit dialog and sourcePath when save endpoint fails', async () => {
    vi.mocked(aiApi.kbSourceSave).mockRejectedValueOnce(new Error('save denied'))

    renderPage(<KnowledgeSourcePage />)

    await screen.findByText('产品资料目录')
    fireEvent.click(screen.getByRole('button', { name: '新增来源' }))
    fireEvent.change(screen.getByLabelText(/来源名称/), { target: { value: '直播资料目录' } })
    fireEvent.change(screen.getByLabelText(/知识源路径/), { target: { value: '/data/knowledge/live' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/保存失败（POST \/ai\/admin\/knowledge-source\/save）：save denied/)).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-source-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByRole('dialog', { name: '新增来源' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('/data/knowledge/live')).toBeInTheDocument()
    expect(screen.getByDisplayValue('直播资料目录')).toBeInTheDocument()
  })

  it('KnowledgeSourcePage keeps source row when delete endpoint fails', async () => {
    vi.mocked(aiApi.kbSourceDelete).mockRejectedValueOnce(new Error('delete denied'))

    renderPage(<KnowledgeSourcePage />)

    expect(await screen.findByText('产品资料目录')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(await screen.findByRole('dialog', { name: '确认删除' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/删除失败（POST \/ai\/admin\/knowledge-source\/delete\?id=7）：delete denied/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('knowledge-source-delete-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByText('产品资料目录')).toBeInTheDocument()
  })

  it('ViralAnalysisPage renders real contract and opens parsed detail', async () => {
    renderPage(<ViralAnalysisPage />)

    expect(screen.getByRole('heading', { name: '爆款分析' })).toBeInTheDocument()
    const page = screen.getByTestId('viral-analysis-page')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/viral/list'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/douyin/video/search'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/ai/evolution/viral/mock'))
    expect(page).toHaveAttribute('data-no-local-list', 'true')
    expect(page).toHaveAttribute('data-no-static-report', 'true')
    expect(screen.getByTestId('viral-analysis-boundary-contract')).toHaveAttribute('data-no-local-analysis', 'true')
    expect(screen.getByText(/\/ai\/evolution\/viral\/list/)).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.evolveList).toHaveBeenCalledWith({ page: 0, rows: 20, status: undefined })
    })
    expect(await screen.findByText('爆款分析总数')).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-stats-cards')).toHaveAttribute('data-ready-endpoint', '/ai/evolution/stats')
    expect(screen.getByTestId('viral-analysis-grid')).toHaveAttribute('data-pagination-mode', 'server')
    expect(screen.getByText(/本分析仅基于元数据/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '详情' }))
    await waitFor(() => {
      expect(aiApi.evolveViralGet).toHaveBeenCalledWith(31)
    })
    expect(await screen.findByTestId('viral-analysis-detail-dialog')).toHaveAttribute('data-ready-endpoint', '/ai/evolution/viral/get')
    expect(screen.getByTestId('viral-analysis-report-preview-surface')).toHaveAttribute('data-no-static-report', 'true')
    expect(await screen.findByText(/元数据表现突出/)).toBeInTheDocument()
  })

  it('ViralAnalysisPage uses a theme-aware report preview surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme(<ViralAnalysisPage />)

    await screen.findByText(/本分析仅基于元数据/)
    fireEvent.click(screen.getByRole('button', { name: '详情' }))
    await waitFor(() => {
      expect(aiApi.evolveViralGet).toHaveBeenCalledWith(31)
    })

    const preview = await screen.findByTestId('viral-analysis-report-preview-surface')
    expect(preview).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('ViralAnalysisPage displays list and stats failures inline', async () => {
    vi.mocked(aiApi.evolveList).mockRejectedValueOnce(new Error('list down'))
    vi.mocked(aiApi.evolveStats).mockRejectedValueOnce(new Error('stats down'))

    renderPage(<ViralAnalysisPage />)

    expect(await screen.findByText(/爆款分析列表加载失败/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-list-error')).toHaveAttribute('data-no-local-list', 'true')
    expect(screen.getByText(/list down/)).toBeInTheDocument()
    expect(screen.getAllByText(/POST \/ai\/evolution\/viral\/list/).length).toBeGreaterThan(0)
    expect(await screen.findByText(/统计数据不可用/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-stats-error')).toHaveAttribute('data-no-static-stats', 'true')
    expect(screen.getByText(/stats down/)).toBeInTheDocument()
    expect(screen.getByText(/POST \/ai\/evolution\/stats/)).toBeInTheDocument()
  })

  it('ViralAnalysisPage normalizes wrapped viral records', async () => {
    vi.mocked(aiApi.evolveList).mockResolvedValueOnce({
      result: {
        records: [
          {
            id: '32',
            video_id: '202',
            account_id: '7',
            quality_score: '77',
            viral_score: '79',
            report_content: '包装爆款拆解',
            status: '1',
            create_time: '2026-05-22 10:00:00',
          },
        ],
        totalRecords: '1',
      },
    } as never)

    renderPage(<ViralAnalysisPage />)

    expect(await screen.findByText('包装爆款拆解')).toBeInTheDocument()
  })

  it('ViralAnalysisPage keeps trigger dialog and ids when trigger endpoint fails', async () => {
    vi.mocked(aiApi.evolveTrigger).mockRejectedValueOnce(new Error('queue down'))

    renderPage(<ViralAnalysisPage />)

    await screen.findByText(/本分析仅基于元数据/)
    fireEvent.click(screen.getByRole('button', { name: '触发拆解' }))
    expect(await screen.findByRole('dialog', { name: '触发爆款拆解' })).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-trigger-dialog')).toHaveAttribute('data-no-local-trigger', 'true')
    fireEvent.change(screen.getByLabelText(/抖音视频 ID/), { target: { value: '101' } })
    fireEvent.change(screen.getByLabelText(/账号 ID/), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: '提交' }))

    expect(await screen.findByText(/触发失败（POST \/ai\/evolution\/viral\/trigger）：queue down/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-trigger-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByRole('dialog', { name: '触发爆款拆解' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('101')).toBeInTheDocument()
    expect(screen.getByDisplayValue('5')).toBeInTheDocument()
  })

  it('ViralAnalysisPage keeps analysis row when delete endpoint fails', async () => {
    vi.mocked(aiApi.evolveDelete).mockRejectedValueOnce(new Error('delete denied'))

    renderPage(<ViralAnalysisPage />)

    expect(await screen.findByText(/本分析仅基于元数据/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(await screen.findByRole('dialog', { name: '删除爆款分析' })).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-delete-dialog-contract')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/删除失败（POST \/ai\/evolution\/viral\/delete\?id=31）：delete denied/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('viral-analysis-delete-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByText(/本分析仅基于元数据/)).toBeInTheDocument()
  })

  it('ViralAnalysisPage shows detail and video search endpoint failures', async () => {
    vi.mocked(aiApi.evolveViralGet).mockRejectedValueOnce(new Error('detail denied'))
    vi.mocked(douyinApi.videoSearch).mockRejectedValueOnce(new Error('douyin search down'))

    renderPage(<ViralAnalysisPage />)

    await screen.findByText(/本分析仅基于元数据/)
    fireEvent.click(screen.getByRole('button', { name: '详情' }))
    expect(await screen.findByText(/POST \/ai\/evolution\/viral\/get\?id=31/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-analysis-detail-error')).toHaveAttribute('data-no-static-report', 'true')
    fireEvent.click(screen.getByRole('button', { name: '关闭' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /爆款拆解详情/ })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '触发拆解' }))
    expect(await screen.findByRole('dialog', { name: '触发爆款拆解' })).toBeInTheDocument()
    const triggerDialog = screen.getByRole('dialog', { name: '触发爆款拆解' })
    const searchInput = triggerDialog.querySelector('input[placeholder="输入关键词筛选 DouyinVideo"]') as HTMLInputElement
    const user = userEvent.setup()
    await user.type(searchInput, '屏障')

    await waitFor(() => {
      expect(douyinApi.videoSearch).toHaveBeenCalledWith({ page: 0, rows: 30, title: '屏障' })
    }, { timeout: 2000 })
    expect(await screen.findByTestId('viral-analysis-video-search-error')).toHaveAttribute('data-no-local-video-search', 'true')
    expect(screen.getByText(/视频搜索失败（POST \/douyin\/video\/search）：douyin search down/)).toBeInTheDocument()
  })
})
