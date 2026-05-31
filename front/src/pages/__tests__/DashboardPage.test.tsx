import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import '@/i18n'

const navigate = vi.hoisted(() => vi.fn())
let mockPathname = '/admin/dashboard'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useLocation: () => ({ pathname: mockPathname, search: '', hash: '', state: null, key: 'test' }),
  }
})

// Mock all external dependencies
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    adminStats: vi.fn().mockResolvedValue({
      totalUsers: 100,
      activeUsers: 50,
      todayUsers: 5,
      totalVideos: 200,
      publishedVideos: 150,
      todayVideos: 10,
      totalLiveSessions: 50,
      completedSessions: 40,
      todaySessions: 3,
      totalShortVideos: 300,
      publishedShortVideos: 250,
      todayShortVideos: 15,
      totalCopyItems: 100,
      approvedCopyItems: 80,
      todayCopyItems: 5,
      todayAiCalls: 100,
      todayAiAttempts: 120,
      aiSuccessRate: 83.3,
      todayRevenue: 12345.67,
    }),
    orgStats: vi.fn().mockResolvedValue({
      totalUsers: 0,
      activeUsers: 0,
      todayUsers: 0,
      totalVideos: 20,
      publishedVideos: 12,
      todayVideos: 1,
      totalLiveSessions: 8,
      completedSessions: 5,
      todaySessions: 1,
      totalShortVideos: 16,
      publishedShortVideos: 10,
      todayShortVideos: 2,
      totalCopyItems: 30,
      approvedCopyItems: 24,
      todayCopyItems: 3,
      todayAiCalls: 9,
      todayAiAttempts: 10,
      aiSuccessRate: 90,
      todayRevenue: 9000,
    }),
    kpiUnified: vi.fn().mockResolvedValue({
      gmvToday: 12345.67,
      gmvMom: 23.4,
      gmvYoy: 45.6,
      ordersToday: 100,
      avgOrderValue: 123.46,
      conversionRate: 3.5,
      liveSessions: 3,
      activeSessionCount: 1,
      aiCallsToday: 100,
    }),
    liveFormatGmv: vi.fn().mockResolvedValue([]),
    productGmvSummary: vi.fn().mockResolvedValue([]),
    cockpitPreview: vi.fn().mockResolvedValue([]),
    conversionFunnel: vi.fn().mockResolvedValue({
      impressions: 10000,
      clicks: 500,
      addToCart: 100,
      orders: 35,
    }),
  },
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    roleCode: 'admin',
    id: 1,
    username: 'admin',
    nickname: 'Admin',
  })),
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionSearch: vi.fn().mockResolvedValue({
      total: 0,
      list: [],
    }),
    approvalPending: vi.fn().mockResolvedValue({
      total: 0,
      list: [],
    }),
  },
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    callTypeDistribution: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('@/api/system', () => ({
  systemApi: {
    alertActive: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts-mock">ECharts</div>,
}))

vi.mock('@/utils/echarts-registry', () => ({
  echarts: {},
  LazyECharts: ({ option }: { option: unknown }) => <div data-testid="lazy-echarts-mock">{JSON.stringify(option)}</div>,
}))

import DashboardPage from '../DashboardPage'
import { dashboardApi } from '@/api/dashboard'
import { liveApi } from '@/api/live'
import { aiApi } from '@/api/ai'

function renderDashboard() {
  return renderWithProviders(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>
  )
}

function renderDashboardWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </AppThemeProvider>
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
    navigate.mockClear()
    mockPathname = '/admin/dashboard'
    vi.mocked(dashboardApi.adminStats).mockResolvedValue({
      totalUsers: 100,
      activeUsers: 50,
      todayUsers: 5,
      totalVideos: 200,
      publishedVideos: 150,
      todayVideos: 10,
      totalLiveSessions: 50,
      completedSessions: 40,
      todaySessions: 3,
      totalShortVideos: 300,
      publishedShortVideos: 250,
      todayShortVideos: 15,
      totalCopyItems: 100,
      approvedCopyItems: 80,
      todayCopyItems: 5,
      todayAiCalls: 100,
      todayAiAttempts: 120,
      aiSuccessRate: 83.3,
      todayRevenue: 12345.67,
    })
    vi.mocked(dashboardApi.orgStats).mockResolvedValue({
      totalUsers: 0,
      activeUsers: 0,
      todayUsers: 0,
      totalVideos: 20,
      publishedVideos: 12,
      todayVideos: 1,
      totalLiveSessions: 8,
      completedSessions: 5,
      todaySessions: 1,
      totalShortVideos: 16,
      publishedShortVideos: 10,
      todayShortVideos: 2,
      totalCopyItems: 30,
      approvedCopyItems: 24,
      todayCopyItems: 3,
      todayAiCalls: 9,
      todayAiAttempts: 10,
      aiSuccessRate: 90,
      todayRevenue: 9000,
    })
    vi.mocked(dashboardApi.kpiUnified).mockResolvedValue({
      gmvToday: 12345.67,
      gmvMom: 23.4,
      gmvYoy: 45.6,
      ordersToday: 100,
      avgOrderValue: 123.46,
      conversionRate: 3.5,
      liveSessions: 3,
      activeSessionCount: 1,
      aiCallsToday: 100,
    })
    vi.mocked(dashboardApi.liveFormatGmv).mockResolvedValue([])
    vi.mocked(dashboardApi.productGmvSummary).mockResolvedValue([])
    vi.mocked(dashboardApi.cockpitPreview).mockResolvedValue([])
    vi.mocked(dashboardApi.conversionFunnel).mockResolvedValue({
      exposure: 10000,
      clicks: 500,
      addToCart: 100,
      orders: 35,
      payments: 0,
    })
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 4,
    } as never)
    vi.mocked(liveApi.approvalPending).mockResolvedValue([] as never)
  })

  it('renders without crashing', async () => {
    const { container } = renderDashboard()
    expect(container).toBeInTheDocument()
  })

  it('renders with admin role', async () => {
    renderDashboard()
    // Just verify the page renders, don't check specific content
    await waitFor(() => {
      expect(document.body).toBeInTheDocument()
    })
  })

  it('calls dashboard API on mount', async () => {
    renderDashboard()
    await waitFor(() => {
      expect(dashboardApi.adminStats).toHaveBeenCalled()
    })
  })

  it('shows explicit downgrade for AI recommendations', async () => {
    renderDashboard()

    expect(await screen.findByRole('heading', { name: '管理驾驶舱' })).toBeInTheDocument()
    expect(screen.getByText('快速上手')).toBeInTheDocument()
    expect(screen.getByText('AI 推荐链路')).toBeInTheDocument()
    expect(screen.getByText(/没有独立的 AI 推荐落库接口/)).toBeInTheDocument()
    expect(screen.getByTestId('dashboard-ai-recommendation-degradation')).toHaveAttribute('data-contract-status', 'unsupported')
    expect(screen.getByTestId('dashboard-ai-recommendation-degradation')).toHaveAttribute('data-no-local-ai-recommendation-fallback', 'true')
  })

  it('exposes dashboard role shell contract endpoints and no-local anchors', async () => {
    renderDashboard()

    const workbench = await screen.findByTestId('dashboard-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'dashboard-role-shell')
    expect(workbench).toHaveAttribute('data-route-scope', 'admin')
    expect(workbench).toHaveAttribute('data-no-local-kpi-fallback', 'true')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/admin/stats')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/kpi-unified')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/cockpit-preview')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/conversion-funnel')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/product-gmv-summary')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/live-format-gmv')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/live/session/search')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/live/approval/pending')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/monitoring/alerts/active')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/ai/admin/dashboard/call-type-distribution')
    expect(workbench.getAttribute('data-unsupported-endpoints')).toContain('/dashboard/ai-recommendations')
    expect(workbench.getAttribute('data-unsupported-endpoints')).toContain('/dashboard/conversion-funnel?channel')
    expect(screen.getByTestId('dashboard-live-sessions-panel')).toHaveAttribute('data-no-local-session-fallback', 'true')
    expect(screen.getByTestId('dashboard-todo-ai-panel')).toHaveAttribute('data-no-local-ai-recommendation-fallback', 'true')
    expect(screen.getByTestId('dashboard-kpi-strip')).toHaveAttribute('data-contract-endpoint', '/dashboard/kpi-unified')
    expect(screen.getByTestId('dashboard-quick-entry-grid')).toHaveAttribute('data-no-cross-scope-navigation', 'true')
  })

  it('keeps dashboard sections visible when one endpoint fails', async () => {
    vi.mocked(dashboardApi.kpiUnified).mockRejectedValue(new Error('kpi down'))

    renderDashboard()

    expect(await screen.findByText('KPI 加载失败')).toBeInTheDocument()
    expect(screen.getByText('运营指挥中心')).toBeInTheDocument()
    expect(screen.getByTestId('dashboard-kpi-error')).toHaveAttribute('data-contract-endpoint', '/dashboard/kpi-unified')
    expect(screen.getByTestId('dashboard-error-stack')).toHaveAttribute('data-row-retained-on-error', 'true')
  })

  it('routes pending live approvals to the real session workspace script step', async () => {
    vi.mocked(liveApi.approvalPending).mockResolvedValueOnce([
      { id: 33, liveTitle: '待审直播场次' },
    ] as never)

    renderDashboard()

    expect(await screen.findByText('话术待审批 1 条')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '处理' }))

    expect(navigate).toHaveBeenCalledWith('/admin/live/sessions/33?step=2&tab=scripts')
  })

  it('keeps org dashboard actions inside the org shell', async () => {
    mockPathname = '/org/dashboard'
    vi.mocked(liveApi.approvalPending).mockResolvedValueOnce([
      { id: 33, liveTitle: '待审直播场次' },
    ] as never)

    renderDashboard()

    expect(await screen.findByRole('heading', { name: '机构端工作台' })).toBeInTheDocument()
    const workbench = screen.getByTestId('dashboard-workbench')
    expect(workbench).toHaveAttribute('data-route-scope', 'org')
    expect(workbench).toHaveAttribute('data-no-cross-scope-admin-stats', 'true')
    expect(workbench).toHaveAttribute('data-no-cross-scope-ai-distribution', 'true')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/org/stats')
    expect(workbench.getAttribute('data-ready-endpoints')).not.toContain('/dashboard/admin/stats')
    expect(workbench.getAttribute('data-ready-endpoints')).not.toContain('/ai/admin/dashboard/call-type-distribution')
    expect(workbench.getAttribute('data-unsupported-endpoints')).toContain('/dashboard/admin/stats')
    expect(screen.getByTestId('dashboard-route-scope-alert')).toHaveAttribute('data-no-cross-scope-admin-stats', 'true')
    expect(screen.getByText(/当前路由是 机构端 壳/)).toBeInTheDocument()
    expect(dashboardApi.orgStats).toHaveBeenCalled()
    expect(dashboardApi.adminStats).not.toHaveBeenCalled()
    expect(aiApi.callTypeDistribution).not.toHaveBeenCalled()
    expect(screen.getByTestId('dashboard-ai-distribution-unsupported')).toHaveAttribute('data-no-cross-scope-ai-distribution', 'true')
    expect(screen.getByRole('button', { name: '机构视图' })).toHaveAttribute('aria-pressed', 'true')
    expect(await screen.findByText('话术待审批 1 条')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '处理' }))

    expect(navigate).toHaveBeenCalledWith('/org/live/sessions/33')
    fireEvent.click(screen.getByRole('button', { name: '机构场次' }))
    expect(navigate).toHaveBeenCalledWith('/org/live/sessions')
  })

  it('keeps talent dashboard shortcuts out of admin routes', async () => {
    mockPathname = '/talent/dashboard'

    renderDashboard()

    expect(await screen.findByRole('heading', { name: '达人端工作台' })).toBeInTheDocument()
    const workbench = screen.getByTestId('dashboard-workbench')
    expect(workbench).toHaveAttribute('data-route-scope', 'talent')
    expect(workbench).toHaveAttribute('data-no-cross-scope-admin-stats', 'true')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/org/stats')
    expect(workbench.getAttribute('data-ready-endpoints')).not.toContain('/dashboard/admin/stats')
    expect(dashboardApi.orgStats).toHaveBeenCalled()
    expect(dashboardApi.adminStats).not.toHaveBeenCalled()
    expect(aiApi.callTypeDistribution).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: '达人视图' })).toHaveAttribute('aria-pressed', 'true')
    fireEvent.click(screen.getByRole('button', { name: '我的场次' }))
    expect(navigate).toHaveBeenCalledWith('/talent/live/sessions')
    fireEvent.click(screen.getByRole('button', { name: '短视频项目' }))
    expect(navigate).toHaveBeenCalledWith('/talent/shortvideo')
  })

  it('normalizes wrapped approvals and live sessions without crashing', async () => {
    vi.mocked(liveApi.sessionSearch).mockResolvedValueOnce({
      total: 1,
      list: [
        { id: 21, liveTitle: '晚场修护直播', status: 1, viewers: 1288 },
      ],
      pageNum: 0,
      pageSize: 4,
    } as never)
    vi.mocked(liveApi.approvalPending).mockResolvedValueOnce({
      records: [{ id: '44', liveTitle: '包装审批场次' }],
    } as never)

    renderDashboard()

    expect(await screen.findByText('晚场修护直播')).toBeInTheDocument()
    expect(screen.getByText(/在线 1,288 人/)).toBeInTheDocument()
    expect(await screen.findByText('话术待审批 1 条')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '处理' }))
    expect(navigate).toHaveBeenCalledWith('/admin/live/sessions/44?step=2&tab=scripts')
  })

  it('shows explicit unsupported channel downgrade in conversion funnel', async () => {
    renderDashboard()

    const downgrade = await screen.findByTestId('dashboard-funnel-channel-downgrade')
    const input = screen.getByTestId('dashboard-funnel-channel-input')

    expect(downgrade).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(downgrade).toHaveTextContent('/dashboard/conversion-funnel 当前仅按 lookbackDays 聚合')
    expect(input).toHaveAttribute('data-contract-status', 'unsupported')
    expect(input).toHaveAttribute('data-contract-endpoint', '/dashboard/conversion-funnel')
    expect(screen.getByTestId('dashboard-funnel-channel-select')).toHaveTextContent('显式降级：后端未接收 channel')
    expect(screen.getByText(/当前后端返回观看、点赞、进入商品三步/)).toBeInTheDocument()
  })

  it('exposes local error anchors for dashboard secondary endpoints', async () => {
    vi.mocked(dashboardApi.cockpitPreview).mockRejectedValue(new Error('cockpit down'))
    vi.mocked(dashboardApi.productGmvSummary).mockRejectedValue(new Error('product down'))
    vi.mocked(dashboardApi.liveFormatGmv).mockRejectedValue(new Error('format down'))

    renderDashboard()

    expect(await screen.findByText('趋势数据加载失败')).toBeInTheDocument()
    expect(screen.getByTestId('dashboard-cockpit-error')).toHaveAttribute('data-contract-endpoint', '/dashboard/cockpit-preview')
    expect(screen.getByTestId('dashboard-products-error')).toHaveAttribute('data-contract-endpoint', '/dashboard/product-gmv-summary')
    expect(screen.getByTestId('dashboard-live-format-error')).toHaveAttribute('data-contract-endpoint', '/dashboard/live-format-gmv')
    expect(screen.getByTestId('dashboard-role-shell')).toHaveAttribute('data-contract-ready', 'true')
  })

  it('uses theme-aware live status and todo surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(liveApi.sessionSearch).mockResolvedValueOnce({
      total: 1,
      list: [
        { id: 21, liveTitle: '晚场修护直播', status: 1, viewers: 1288 },
      ],
      pageNum: 0,
      pageSize: 4,
    } as never)
    vi.mocked(liveApi.approvalPending).mockResolvedValueOnce([
      { id: 33, liveTitle: '待审直播场次' },
    ] as never)

    renderDashboardWithTheme()

    expect(await screen.findByText('晚场修护直播')).toBeInTheDocument()
    expect(await screen.findByText('话术待审批 1 条')).toBeInTheDocument()
    expect(screen.getByTestId('dashboard-live-status-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(244, 67, 54)',
    })
    expect(screen.getByTestId('dashboard-todo-icon-surface')).toBeInTheDocument()
  })

  it('uses theme-aware dashboard chart colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(dashboardApi.kpiUnified).mockResolvedValueOnce({
      gmvToday: 12345.67,
      gmvMom: 23.4,
      ordersToday: 100,
      avgOrderValue: 123.46,
      conversionRate: 3.5,
      liveSessions: 3,
      activeSessionCount: 1,
      aiCallsToday: 100,
      gmvForecast: [13000, 14000],
    })
    vi.mocked(dashboardApi.cockpitPreview).mockResolvedValueOnce([
      { date: '2026-05-20', gmv: 10000, sessions: 2, orders: 80 },
      { date: '2026-05-21', gmv: 12000, sessions: 3, orders: 96 },
    ] as never)
    vi.mocked(dashboardApi.liveFormatGmv).mockResolvedValueOnce([
      { format: '达人专场', gmv: 16000 },
    ] as never)
    vi.mocked(dashboardApi.conversionFunnel).mockResolvedValueOnce({
      exposure: 10000,
      clicks: 500,
      addToCart: 100,
      orders: 35,
      payments: 20,
    })

    renderDashboardWithTheme()

    const trendChart = await screen.findByTestId('dashboard-trend-chart-surface')
    const liveFormatChart = await screen.findByTestId('dashboard-live-format-chart-surface')
    const funnelChart = await screen.findByTestId('dashboard-funnel-chart-surface')

    expect(trendChart).toHaveAttribute('data-chart-colors', '#e3f2fd|#81c784|#ffb74d')
    expect(liveFormatChart).toHaveAttribute('data-chart-color', '#81c784')
    expect(funnelChart).toHaveAttribute('data-chart-colors', '#e3f2fd|#81c784|#ffb74d|#f3e5f5|#4fc3f7')

    const chartText = screen.getAllByTestId('lazy-echarts-mock').map(node => node.textContent ?? '').join('\n')
    expect(chartText).not.toContain('#ff9800')
    expect(chartText).not.toContain('#1976d2')
    expect(chartText).not.toContain('#4caf50')
    expect(chartText).not.toContain('#00897b')
    expect(chartText).not.toContain('#1565c0')
    expect(chartText).not.toContain('#2e7d32')
    expect(chartText).not.toContain('#ef6c00')
    expect(chartText).not.toContain('#6a1b9a')
    expect(chartText).not.toContain('#00838f')
  })
})
