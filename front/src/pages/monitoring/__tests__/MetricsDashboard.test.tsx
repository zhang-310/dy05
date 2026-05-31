import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { MetricsDashboard } from '../MetricsDashboard'
import { ComponentStatus, AlertSeverity, AlertStatus } from '@/types/monitoring'
import { useMonitoringData } from '@/hooks/useMonitoringData'

vi.mock('@/hooks/useMonitoringData', () => ({
  useMonitoringData: vi.fn(),
}))

const refreshAll = vi.fn()
const fetchPerformanceTrend = vi.fn()

function mockMonitoringData(overrides = {}) {
  vi.mocked(useMonitoringData).mockReturnValue({
    realtimeMetrics: {
      cpuUsage: 38.4,
      memoryUsage: 62.2,
      responseTime: 148,
      errorRate: 0.42,
      networkIn: 12.3,
      networkOut: 8.7,
      requestsPerSecond: 321,
      cacheHitRate: 77.8,
      queueDepth: 6,
      activeConnections: 42,
    },
    activeAlerts: [
      {
        alertId: 1,
        alertType: 'metric_threshold',
        ruleName: 'Redis 命中率偏低',
        severity: AlertSeverity.HIGH,
        status: AlertStatus.ACTIVE,
        message: '缓存命中率低于阈值',
      },
    ],
    healthStatus: {
      status: ComponentStatus.DEGRADED,
      message: 'cache degraded',
      components: {
        database: { status: ComponentStatus.UP, responseTime: 12, message: '连接正常' },
        cache: { status: ComponentStatus.DEGRADED, responseTime: 68, message: '命中率偏低' },
      },
    },
    performanceTrends: new Map([
      ['responseTime', {
        metricName: 'responseTime',
        unit: 'ms',
        timeRange: 'hour',
        dataPoints: [
          { timestamp: 1, value: 120 },
          { timestamp: 2, value: 180 },
        ],
        summary: { average: 150, min: 120, max: 180, percentile95: 175, percentile99: 180 },
      }],
      ['errorRate', {
        metricName: 'errorRate',
        unit: '%',
        timeRange: 'hour',
        dataPoints: [
          { timestamp: 1, value: 0.2 },
          { timestamp: 2, value: 0.5 },
        ],
        summary: { average: 0.35, min: 0.2, max: 0.5, percentile95: 0.48, percentile99: 0.5 },
      }],
    ]),
    isLoading: false,
    error: null,
    requestErrors: {},
    streamStatus: 'connected',
    lastUpdatedAt: '2026-05-22T12:00:00.000Z',
    alertsCount: { info: 0, low: 0, medium: 0, high: 1, critical: 0 },
    refreshAll,
    fetchRealtimeMetrics: vi.fn(),
    fetchActiveAlerts: vi.fn(),
    fetchHealthStatus: vi.fn(),
    fetchPerformanceTrend,
    acknowledgeAlert: vi.fn(),
    resolveAlert: vi.fn(),
    ...overrides,
  })
}

describe('MetricsDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    mockMonitoringData()
  })

  function renderWithTheme() {
    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <MetricsDashboard />
        </MemoryRouter>
      </AppThemeProvider>,
    )
  }

  it('renders realtime metrics, health and active alert diagnostics', () => {
    renderWithProviders(
      <MemoryRouter>
        <MetricsDashboard />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '实时指标监控' })).toBeInTheDocument()
    const workbench = screen.getByTestId('metrics-dashboard-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'platform-monitoring-realtime-observability')
    expect(workbench).toHaveAttribute('data-no-local-metric-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-local-alert-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-alert-action-buttons', 'true')
    expect(workbench).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getAllByText(/monitoring\/metrics\/realtime/).length).toBeGreaterThan(0)
    expect(screen.getByText('CPU 使用率')).toBeInTheDocument()
    expect(screen.getByText('38.4%')).toBeInTheDocument()
    expect(screen.getByText('系统健康')).toBeInTheDocument()
    expect(screen.getAllByText(ComponentStatus.DEGRADED).length).toBeGreaterThan(0)
    expect(screen.getByText('Redis 命中率偏低')).toBeInTheDocument()
    expect(screen.getByText('database')).toBeInTheDocument()
    expect(screen.getByText('cache')).toBeInTheDocument()
    expect(screen.getByText('响应时间趋势摘要')).toBeInTheDocument()
    expect(screen.getByText('错误率趋势摘要')).toBeInTheDocument()
    expect(screen.getByText('监控链路诊断')).toBeInTheDocument()
    expect(screen.getByText('SSE 已连接')).toBeInTheDocument()
    expect(screen.getByText('150ms')).toBeInTheDocument()
    expect(screen.getByText('0.35%')).toBeInTheDocument()
    expect(screen.getByTestId('metrics-dashboard-cache-hit-diagnostic')).toHaveAttribute('data-cache-hit-rate', '77.8')
    expect(screen.getByTestId('metrics-dashboard-cache-hit-diagnostic')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByTestId('metrics-dashboard-active-alerts-card')).toHaveAttribute('data-no-alert-action-buttons', 'true')
    expect(screen.queryByRole('button', { name: /确认|解决|关闭/ })).not.toBeInTheDocument()
    expect(fetchPerformanceTrend).toHaveBeenCalledWith('responseTime', 'hour')
    expect(fetchPerformanceTrend).toHaveBeenCalledWith('errorRate', 'hour')
  })

  it('shows visible error with retry action', () => {
    mockMonitoringData({
      error: '实时指标加载失败：network down',
      requestErrors: { 实时指标加载: '实时指标加载失败：network down' },
      streamStatus: 'closed',
    })

    renderWithProviders(
      <MemoryRouter>
        <MetricsDashboard />
      </MemoryRouter>,
    )

    expect(screen.getByText('监控数据加载失败')).toBeInTheDocument()
    expect(screen.getAllByText(/network down/).length).toBeGreaterThan(0)
    expect(screen.getByText('SSE 已关闭，轮询兜底')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /重试/ }))
    expect(refreshAll).toHaveBeenCalled()
  })

  it('explains missing component and active alert data as explicit degradation', () => {
    mockMonitoringData({
      activeAlerts: [],
      healthStatus: {
        status: ComponentStatus.UP,
        components: {},
      },
      alertsCount: { info: 0, low: 0, medium: 0, high: 0, critical: 0 },
    })

    renderWithProviders(
      <MemoryRouter>
        <MetricsDashboard />
      </MemoryRouter>,
    )

    expect(screen.getByText(/健康接口未返回组件明细/)).toBeInTheDocument()
    expect(screen.getByText(/当前没有活跃告警/)).toBeInTheDocument()
    expect(screen.getByTestId('metrics-dashboard-components-empty')).toHaveAttribute('data-no-local-component-fallback', 'true')
    expect(screen.getByTestId('metrics-dashboard-active-alerts-empty')).toHaveAttribute('data-no-local-alert-fallback', 'true')
  })

  it('loads real trend data when the time range changes and explains missing trend data', () => {
    mockMonitoringData({
      performanceTrends: new Map(),
    })

    renderWithProviders(
      <MemoryRouter>
        <MetricsDashboard />
      </MemoryRouter>,
    )

    expect(screen.getByText(/趋势接口未返回响应时间数据/)).toBeInTheDocument()
    expect(screen.getByText(/趋势接口未返回错误率数据/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '1天' }))

    expect(fetchPerformanceTrend).toHaveBeenCalledWith('responseTime', 'day')
    expect(fetchPerformanceTrend).toHaveBeenCalledWith('errorRate', 'day')
    expect(screen.getByText(/当前选择 1天 视图/)).toBeInTheDocument()
    expect(screen.getByTestId('metrics-dashboard-response-trend-empty')).toHaveAttribute('data-no-local-trend-fallback', 'true')
    expect(screen.getByTestId('metrics-dashboard-error-trend-empty')).toHaveAttribute('data-no-local-trend-fallback', 'true')
  })

  it('does not coerce missing realtime metrics into fake zero values', () => {
    mockMonitoringData({
      realtimeMetrics: {},
      activeAlerts: [],
      healthStatus: {
        status: ComponentStatus.UP,
        components: {},
      },
      performanceTrends: new Map(),
      alertsCount: { info: 0, low: 0, medium: 0, high: 0, critical: 0 },
    })

    renderWithProviders(
      <MemoryRouter>
        <MetricsDashboard />
      </MemoryRouter>,
    )

    expect(screen.getAllByText('未返回').length).toBeGreaterThan(4)
    const cpuCard = screen.getAllByTestId('metrics-dashboard-stat-card-surface').find(
      (card) => card.getAttribute('data-metric-key') === 'cpuUsage',
    )
    expect(cpuCard).toHaveAttribute('data-value-state', 'missing')
    expect(cpuCard).toHaveAttribute('data-no-local-metric-fallback', 'true')
    expect(screen.getByTestId('metrics-dashboard-cache-hit-diagnostic')).toHaveTextContent('未返回')
  })

  it('marks low cache hit rate as actionable degraded metric', () => {
    mockMonitoringData({
      realtimeMetrics: {
        cpuUsage: 38.4,
        memoryUsage: 62.2,
        responseTime: 148,
        errorRate: 0.42,
        networkIn: 12.3,
        networkOut: 8.7,
        requestsPerSecond: 321,
        cacheHitRate: 22.28,
        queueDepth: 6,
        activeConnections: 42,
      },
    })

    renderWithProviders(
      <MemoryRouter>
        <MetricsDashboard />
      </MemoryRouter>,
    )

    const cacheDiagnostic = screen.getByTestId('metrics-dashboard-cache-hit-diagnostic')
    expect(cacheDiagnostic).toHaveTextContent('严重偏低')
    expect(cacheDiagnostic).toHaveAttribute('data-cache-hit-rate', '22.3')
    expect(cacheDiagnostic).toHaveAttribute('data-threshold-error', '30')
  })

  it('uses theme-aware metric card colors in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    mockMonitoringData({
      realtimeMetrics: {
        cpuUsage: 88.4,
        memoryUsage: 62.2,
        responseTime: 148,
        errorRate: 6.2,
        networkIn: 12.3,
        networkOut: 8.7,
        requestsPerSecond: 321,
        cacheHitRate: 77.8,
        queueDepth: 6,
        activeConnections: 42,
      },
      healthStatus: {
        status: ComponentStatus.DOWN,
        message: 'db down',
        components: {
          database: { status: ComponentStatus.DOWN, responseTime: 0, message: '连接失败' },
        },
      },
      alertsCount: { info: 0, low: 0, medium: 0, high: 1, critical: 1 },
    })

    renderWithTheme()

    const values = screen.getAllByTestId('metrics-dashboard-stat-value-surface')
    values.forEach((value) => {
      expect(value).not.toHaveStyle({ color: 'rgb(25, 118, 210)' })
      expect(value).not.toHaveStyle({ color: 'rgb(46, 125, 50)' })
      expect(value).not.toHaveStyle({ color: 'rgb(211, 47, 47)' })
      expect(value).not.toHaveStyle({ color: 'rgb(245, 124, 0)' })
      expect(value).not.toHaveStyle({ color: 'rgb(76, 175, 80)' })
    })
  })
})
