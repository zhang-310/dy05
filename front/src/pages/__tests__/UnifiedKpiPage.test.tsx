import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import UnifiedKpiPage from '../UnifiedKpiPage'
import { dashboardApi } from '@/api/dashboard'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    adminStats: vi.fn(),
    kpiUnified: vi.fn(),
    liveFormatGmv: vi.fn(),
    productGmvSummary: vi.fn(),
    profitMatrixPreview: vi.fn(),
    conversionFunnel: vi.fn(),
    cockpitPreviewRows: vi.fn(),
    cockpitExport: vi.fn(),
  },
}))

vi.mock('@/utils/echarts-registry', () => ({
  LazyECharts: ({ option }: { option: unknown }) => <div data-testid="kpi-chart">{JSON.stringify(option)}</div>,
}))

function renderUnifiedKpi() {
  return renderWithProviders(
    <MemoryRouter>
      <UnifiedKpiPage />
    </MemoryRouter>,
  )
}

function renderUnifiedKpiWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <UnifiedKpiPage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('UnifiedKpiPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn(() => 'blob:kpi'), writable: true })
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), writable: true })
    HTMLAnchorElement.prototype.click = vi.fn()
    vi.mocked(dashboardApi.adminStats).mockResolvedValue({
      totalUsers: 10,
      activeUsers: 6,
      todayUsers: 2,
      totalVideos: 20,
      publishedVideos: 12,
      todayVideos: 3,
      totalLiveSessions: 5,
      completedSessions: 2,
      todaySessions: 1,
      totalShortVideos: 30,
      publishedShortVideos: 10,
      todayShortVideos: 4,
      totalCopyItems: 100,
      approvedCopyItems: 80,
      todayCopyItems: 5,
      todayAiCalls: 40,
      todayAiAttempts: 50,
      aiSuccessRate: 0.8,
      todayRevenue: 1999,
    } as never)
    vi.mocked(dashboardApi.kpiUnified).mockResolvedValue({
      gmvToday: 2999,
      gmvMom: 0.12,
      gmvYoy: 0.2,
      ordersToday: 15,
      avgOrderValue: 199.9,
      conversionRate: 0.08,
      liveSessions: 1,
      aiCallsToday: 40,
    } as never)
    vi.mocked(dashboardApi.liveFormatGmv).mockResolvedValue([
      { format: '专场', gmv: 3000, sessions: 2, avgGmv: 1500 },
    ] as never)
    vi.mocked(dashboardApi.productGmvSummary).mockResolvedValue([
      { productId: 9, productName: '精华', totalGmv: 900, totalOrders: 0, sessionCount: 3, avgPrice: 0 },
    ] as never)
    vi.mocked(dashboardApi.profitMatrixPreview).mockResolvedValue({
      lookbackDays: 30,
      since: '2026-04-22',
      rows: [{ liveFormat: '专场', sessionCount: 2, totalGmv: 3000, estimatedMarginRate: 0.25, isEstimated: true, note: '毛利率为估算值' }],
    } as never)
    vi.mocked(dashboardApi.conversionFunnel).mockResolvedValue({
      exposure: 100,
      clicks: 20,
      addToCart: 5,
      orders: 0,
      payments: 0,
      steps: [
        { name: '观看', value: 100, rate: 100 },
        { name: '点赞', value: 20, rate: 20 },
        { name: '进入商品', value: 5, rate: 5 },
      ],
    } as never)
    vi.mocked(dashboardApi.cockpitPreviewRows).mockResolvedValue([
      { sessionId: 18, liveTitle: '晚场直播', status: 1, startTime: '2026-05-22 20:00:00', gmv: 1299, productLineCount: 6 },
    ] as never)
    vi.mocked(dashboardApi.cockpitExport).mockResolvedValue({
      csv: '场次ID,标题\n18,晚场直播\n',
      filename: 'cockpit.csv',
      rowCount: 1,
    } as never)
  })

  it('renders admin stats and unified KPI from real dashboard endpoints', async () => {
    renderUnifiedKpi()

    expect(screen.getAllByRole('progressbar').length).toBeGreaterThan(0)

    await waitFor(() => {
      expect(dashboardApi.adminStats).toHaveBeenCalled()
      expect(dashboardApi.kpiUnified).toHaveBeenCalledWith(30)
      expect(dashboardApi.liveFormatGmv).toHaveBeenCalledWith(30)
      expect(dashboardApi.productGmvSummary).toHaveBeenCalledWith(30)
      expect(dashboardApi.profitMatrixPreview).toHaveBeenCalledWith(30)
      expect(dashboardApi.conversionFunnel).toHaveBeenCalledWith(30)
    })

    expect(await screen.findByRole('heading', { name: '统一 KPI' })).toBeInTheDocument()
    expect(screen.getByText('今日 GMV')).toBeInTheDocument()
    expect(screen.getByText('今日 AI 调用')).toBeInTheDocument()
    expect(screen.getByText('环比 12.0%')).toBeInTheDocument()
    expect(screen.getAllByText(/\/dashboard\/admin\/stats/).length).toBeGreaterThan(0)
    expect(await screen.findByText('商品 GMV 汇总')).toBeInTheDocument()
    expect(screen.getByText(/sessionCount` 表示覆盖场次/)).toBeInTheDocument()
    expect(screen.getByText('利润矩阵预览')).toBeInTheDocument()
    expect(screen.getByText(/当前后端返回观看、点赞、进入商品三步/)).toBeInTheDocument()
  })

  it('exposes unified KPI endpoint contract and unsupported action anchors', async () => {
    renderUnifiedKpi()

    const workbench = await screen.findByTestId('unified-kpi-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'unified-kpi-dashboard')
    expect(workbench).toHaveAttribute('data-no-local-kpi-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-finance-settlement', 'true')
    expect(workbench).toHaveAttribute('data-row-retained-on-export-error', 'true')
    for (const endpoint of [
      '/dashboard/admin/stats',
      '/dashboard/kpi-unified',
      '/dashboard/live-format-gmv',
      '/dashboard/product-gmv-summary',
      '/dashboard/profit-matrix-preview',
      '/dashboard/conversion-funnel',
      '/dashboard/cockpit-preview',
      '/dashboard/cockpit-export',
    ]) {
      expect(workbench.getAttribute('data-ready-endpoints')).toContain(endpoint)
    }
    for (const endpoint of [
      '/finance/settlement',
      '/dashboard/conversion-funnel?channel',
      '/dashboard/cockpit-export-local',
      '/inventory/update',
      '/payment/order-funnel',
    ]) {
      expect(workbench.getAttribute('data-unsupported-endpoints')).toContain(endpoint)
    }
    expect(screen.getByTestId('unified-kpi-contract-alert')).toHaveAttribute('data-profit-source', '/dashboard/profit-matrix-preview')
    expect(screen.getByTestId('unified-kpi-source-chip-grid')).toHaveAttribute('data-ready-endpoints', workbench.getAttribute('data-ready-endpoints') ?? '')
    expect(screen.getAllByTestId('unified-kpi-source-chip')).toHaveLength(4)
    expect(screen.getByTestId('unified-kpi-product-table-card')).toHaveAttribute('data-contract-endpoint', '/dashboard/product-gmv-summary')
    expect(screen.getByTestId('unified-kpi-profit-table-card')).toHaveAttribute('data-no-finance-settlement', 'true')
    expect(screen.getByTestId('unified-kpi-cockpit-card')).toHaveAttribute('data-contract-export-endpoint', '/dashboard/cockpit-export')
  })

  it('does not multiply backend percentage values again', async () => {
    vi.mocked(dashboardApi.kpiUnified).mockResolvedValue({
      gmvToday: 2999,
      gmvMom: 25,
      gmvYoy: 0.2,
      ordersToday: 15,
      avgOrderValue: 199.9,
      conversionRate: 8,
      liveSessions: 1,
      aiCallsToday: 40,
    } as never)

    renderUnifiedKpi()

    expect(await screen.findByText('环比 25.0%')).toBeInTheDocument()
    expect(screen.getByText('8.0%')).toBeInTheDocument()
  })

  it('keeps partial error visible when admin stats fails', async () => {
    vi.mocked(dashboardApi.adminStats).mockRejectedValue(new Error('no admin'))

    renderUnifiedKpi()

    expect(await screen.findByText('管理员统计加载失败')).toBeInTheDocument()
    expect(screen.getByText('今日 GMV')).toBeInTheDocument()
    expect(screen.getByTestId('unified-kpi-admin-stats-error')).toHaveAttribute('data-contract-endpoint', '/dashboard/admin/stats')
    expect(screen.getByTestId('unified-kpi-error-stack')).toHaveAttribute('data-row-retained-on-error', 'true')
  })

  it('previews cockpit rows and exports csv with the same filters', async () => {
    renderUnifiedKpi()

    fireEvent.change(await screen.findByLabelText('开始日期'), { target: { value: '2026-05-01' } })
    fireEvent.mouseDown(screen.getByLabelText('场次状态'))
    fireEvent.click(await screen.findByRole('option', { name: '直播中' }))
    fireEvent.click(screen.getByRole('button', { name: '预览' }))

    await waitFor(() => {
      expect(dashboardApi.cockpitPreviewRows).toHaveBeenCalledWith({
        lookbackDays: 30,
        dateFrom: '2026-05-01',
        sessionStatus: 1,
      })
    })
    expect(await screen.findByText('晚场直播')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '导出 CSV' }))
    await waitFor(() => {
      expect(dashboardApi.cockpitExport).toHaveBeenCalledWith({
        lookbackDays: 30,
        dateFrom: '2026-05-01',
        sessionStatus: 1,
      })
    })
    expect(await screen.findByText(/已从 \/dashboard\/cockpit-export 导出 1 行/)).toBeInTheDocument()
    expect(screen.getByTestId('unified-kpi-export-success')).toHaveAttribute('data-contract-endpoint', '/dashboard/cockpit-export')
    expect(screen.getByText(/筛选：lookbackDays=30，dateFrom=2026-05-01，sessionStatus=直播中/)).toBeInTheDocument()
    expect(screen.getByTestId('unified-kpi-cockpit-filter-summary')).toHaveAttribute('data-filter-retained', 'true')
  })

  it('keeps dashboard sections visible when a secondary endpoint fails', async () => {
    vi.mocked(dashboardApi.profitMatrixPreview).mockRejectedValue(new Error('profit down'))
    vi.mocked(dashboardApi.cockpitExport).mockRejectedValue(new Error('export down'))

    renderUnifiedKpi()

    expect(await screen.findByText('利润矩阵加载失败')).toBeInTheDocument()
    expect(screen.getByText(/\/dashboard\/profit-matrix-preview：profit down/)).toBeInTheDocument()
    expect(screen.getByTestId('unified-kpi-profit-error')).toHaveAttribute('data-no-finance-settlement', 'true')
    expect(screen.getByText('直播形式 GMV')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '导出 CSV' }))
    expect(await screen.findByText('导出失败')).toBeInTheDocument()
    expect(screen.getByText(/\/dashboard\/cockpit-export 导出失败（lookbackDays=30/)).toBeInTheDocument()
    expect(screen.getByText(/当前筛选和预览结果已保留/)).toBeInTheDocument()
    expect(screen.getByTestId('unified-kpi-export-error')).toHaveAttribute('data-row-retained-on-export-error', 'true')
    expect(screen.getByTestId('unified-kpi-cockpit-grid')).toHaveAttribute('data-row-retained-on-export-error', 'true')
  })

  it('keeps cockpit filters visible when preview fails', async () => {
    vi.mocked(dashboardApi.cockpitPreviewRows).mockRejectedValue(new Error('preview down'))

    renderUnifiedKpi()

    fireEvent.change(await screen.findByLabelText('开始日期'), { target: { value: '2026-05-01' } })
    fireEvent.mouseDown(screen.getByLabelText('场次状态'))
    fireEvent.click(await screen.findByRole('option', { name: '直播中' }))
    fireEvent.click(screen.getByRole('button', { name: '预览' }))

    expect(await screen.findByText('驾驶舱预览加载失败')).toBeInTheDocument()
    expect(screen.getByText(/\/dashboard\/cockpit-preview 加载失败（lookbackDays=30，dateFrom=2026-05-01，sessionStatus=直播中/)).toBeInTheDocument()
    expect(screen.getByTestId('unified-kpi-cockpit-preview-error')).toHaveAttribute('data-filter-retained', 'true')
    expect(screen.getByTestId('unified-kpi-cockpit-filter-summary')).toHaveTextContent('dateFrom=2026-05-01')
  })

  it('uses theme-aware KPI and chart colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderUnifiedKpiWithTheme()

    expect(await screen.findByRole('heading', { name: '统一 KPI' })).toBeInTheDocument()

    const kpiValues = await screen.findAllByTestId('unified-kpi-card-value-surface')
    expect(kpiValues.map((node) => node.getAttribute('data-kpi-tone'))).toEqual([
      'warning',
      'error',
      'success',
      'error',
      'primary',
      'secondary',
      'info',
      'success',
    ])
    const kpiColors = kpiValues.map((node) => window.getComputedStyle(node).color)
    for (const legacyRgb of [
      'rgb(251, 140, 0)',
      'rgb(229, 57, 53)',
      'rgb(67, 160, 71)',
      'rgb(198, 40, 40)',
      'rgb(25, 118, 210)',
      'rgb(123, 31, 162)',
      'rgb(0, 151, 167)',
      'rgb(56, 142, 60)',
    ]) {
      expect(kpiColors).not.toContain(legacyRgb)
    }

    const liveFormatChart = await screen.findByTestId('unified-kpi-live-format-chart-surface')
    const funnelChart = await screen.findByTestId('unified-kpi-funnel-chart-surface')
    expect(liveFormatChart).toHaveAttribute('data-chart-color', '#81c784')
    expect(funnelChart).toHaveAttribute('data-chart-colors', '#e3f2fd|#81c784|#ffb74d|#f3e5f5|#4fc3f7')

    const chartText = screen.getAllByTestId('kpi-chart').map((node) => node.textContent ?? '').join('\n')
    expect(chartText).not.toContain('#00897b')
  })
})
