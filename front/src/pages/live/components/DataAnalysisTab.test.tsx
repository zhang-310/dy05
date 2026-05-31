import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { DataAnalysisTab } from './DataAnalysisTab'
import { liveApi } from '@/api/live'

const toast = vi.hoisted(() => vi.fn())
const coreDataState = vi.hoisted(() => ({
  session: {
    id: 18,
    liveTitle: '数据复盘测试场次',
    status: 2,
  },
  products: [
    { id: 1, productId: 101, productName: '修护精华' },
    { id: 2, productId: 102, productName: '防晒乳' },
  ],
  scripts: [
    { id: 11, productId: 101, status: 1 },
    { id: 12, productId: 102, status: 0 },
  ],
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    dataSession: vi.fn(),
    dataSessionWithCompare: vi.fn(),
    dataProduct: vi.fn(),
    dataSessionSync: vi.fn(),
    analysisGet: vi.fn(),
    analysisGenerate: vi.fn(),
    scriptExport: vi.fn(),
  },
}))

vi.mock('../contexts', () => ({
  useCoreData: () => ({
    session: coreDataState.session,
    products: coreDataState.products,
    scripts: coreDataState.scripts,
  }),
}))

function renderWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('DataAnalysisTab contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    coreDataState.session = {
      id: 18,
      liveTitle: '数据复盘测试场次',
      status: 2,
    }
    coreDataState.products = [
      { id: 1, productId: 101, productName: '修护精华' },
      { id: 2, productId: 102, productName: '防晒乳' },
    ]
    coreDataState.scripts = [
      { id: 11, productId: 101, status: 1 },
      { id: 12, productId: 102, status: 0 },
    ]
    vi.mocked(liveApi.dataSession).mockResolvedValue({
      sessionId: 18,
      viewerPeak: 2888,
      gmv: 32800,
      orderCount: 236,
      conversionRate: 0.126,
      avgOrderValue: 139,
      createTime: '2026-05-24 22:00:00',
    })
    vi.mocked(liveApi.dataSessionWithCompare).mockResolvedValue({
      prev: {
        sessionId: 17,
        viewerPeak: 2200,
        gmv: 26000,
        orderCount: 190,
        conversionRate: 0.11,
        avgOrderValue: 137,
        createTime: '2026-05-23 22:00:00',
      },
    })
    vi.mocked(liveApi.dataProduct).mockResolvedValue([
      { id: 1, sessionId: 18, productId: 101, productName: '修护精华', exposures: 12000, clicks: 1600, orders: 120, gmv: 19800 },
    ])
    vi.mocked(liveApi.analysisGet).mockResolvedValue({ summary: 'GMV 主要由修护精华贡献，防晒乳承接稳定。' })
    vi.mocked(liveApi.dataSessionSync).mockResolvedValue({} as never)
    vi.mocked(liveApi.analysisGenerate).mockResolvedValue({ summary: '已生成' })
  })

  it('marks data review contract and does not expose script export on the page', async () => {
    renderWithTheme(<DataAnalysisTab />)

    const root = await screen.findByTestId('live-data-analysis-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'live-session-data-review')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/data/session'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/analysis/generate'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('script-export'))
    expect(screen.getByTestId('live-data-analysis-toolbar')).toHaveAttribute('data-no-script-export', 'true')
    expect(screen.queryByTitle('导出话术')).not.toBeInTheDocument()
    expect(liveApi.scriptExport).not.toHaveBeenCalled()

    expect(await screen.findByText('GMV 主要由修护精华贡献，防晒乳承接稳定。')).toBeInTheDocument()
    expect(screen.getByTestId('live-product-data-table-card')).toHaveAttribute('data-no-local-product-data-fallback', 'true')
    expect(screen.getByTestId('live-data-context-summary')).toHaveAttribute('data-contract-source', '/live/product/by-session|/live/script/by-session')
  })

  it('loads compare only when toggled and keeps compare errors explicit', async () => {
    vi.mocked(liveApi.dataSessionWithCompare).mockRejectedValue(new Error('compare down'))

    renderWithTheme(<DataAnalysisTab />)

    expect(await screen.findByTestId('live-data-analysis-workbench')).toHaveAttribute('data-show-compare', 'false')
    expect(liveApi.dataSessionWithCompare).not.toHaveBeenCalled()

    fireEvent.click(screen.getByText('显示环比'))

    expect(await screen.findByTestId('live-data-compare-error')).toHaveTextContent('/live/data/session/with-compare 环比数据加载失败：compare down')
    expect(screen.getByTestId('live-data-compare-error')).toHaveAttribute('data-no-local-session-data-fallback', 'true')
    await waitFor(() => {
      expect(liveApi.dataSessionWithCompare).toHaveBeenCalledWith({ sessionId: 18 })
    })
  })

  it('shows endpoint-specific errors without static data or analysis fallback', async () => {
    vi.mocked(liveApi.dataSession).mockRejectedValue(new Error('session data down'))
    vi.mocked(liveApi.dataProduct).mockRejectedValue(new Error('product data down'))
    vi.mocked(liveApi.analysisGet).mockRejectedValue(new Error('analysis down'))

    renderWithTheme(<DataAnalysisTab />)

    expect(await screen.findByTestId('live-data-session-error')).toHaveTextContent('/live/data/session 场次数据加载失败：session data down')
    expect(screen.getByTestId('live-product-data-error')).toHaveAttribute('data-no-local-product-data-fallback', 'true')
    expect(screen.getByTestId('live-analysis-error')).toHaveAttribute('data-no-local-analysis-fallback', 'true')
    expect(screen.queryByTestId('live-product-data-row')).not.toBeInTheDocument()
    expect(screen.queryByText('GMV 主要由修护精华贡献')).not.toBeInTheDocument()
  })

  it('keeps sync and analysis generation failures as endpoint toasts', async () => {
    vi.mocked(liveApi.dataSessionSync).mockRejectedValue(new Error('sync down'))
    vi.mocked(liveApi.analysisGenerate).mockRejectedValue(new Error('analysis generate down'))

    renderWithTheme(<DataAnalysisTab />)

    fireEvent.click(await screen.findByTestId('live-data-analysis-sync-button'))
    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith('/live/data/session/sync 数据同步失败：sync down', 'error')
    })

    fireEvent.click(screen.getByTestId('live-analysis-generate-button'))
    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith('/live/analysis/generate AI 复盘分析失败：analysis generate down', 'error')
    })
  })

  it('shows not-started and empty product-analysis states without local fallback', async () => {
    coreDataState.session = {
      id: 18,
      liveTitle: '未开播场次',
      status: 0,
    }
    vi.mocked(liveApi.dataProduct).mockResolvedValue([])
    vi.mocked(liveApi.analysisGet).mockResolvedValue({} as never)

    const { unmount } = renderWithTheme(<DataAnalysisTab />)

    expect(screen.getByTestId('live-data-analysis-workbench')).toHaveAttribute('data-state', 'not-started')
    expect(screen.getByTestId('live-data-analysis-unavailable-state')).toHaveAttribute('data-no-local-session-data-fallback', 'true')
    expect(liveApi.dataSession).not.toHaveBeenCalled()

    unmount()
    coreDataState.session = {
      id: 18,
      liveTitle: '空数据场次',
      status: 2,
    }

    renderWithTheme(<DataAnalysisTab />)

    expect(await screen.findByTestId('live-product-data-empty-state')).toHaveAttribute('data-no-local-product-data-fallback', 'true')
    expect(screen.getByTestId('live-analysis-summary')).toHaveTextContent('暂无分析结论')
  })
})
