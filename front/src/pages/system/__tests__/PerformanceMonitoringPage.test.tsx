import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import PerformanceMonitoringPage from '../PerformanceMonitoringPage'
import { systemApi } from '@/api/system'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('@/api/system', () => ({
  systemApi: {
    performanceCurrent: vi.fn(),
    performanceTimeseries: vi.fn(),
    performanceSlowQuery: vi.fn(),
    performanceAnalysis: vi.fn(),
    performanceCacheStatistics: vi.fn(),
  },
}))

describe('PerformanceMonitoringPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.performanceCurrent).mockResolvedValue({
      cpu: 16,
      memory: 48,
      requestCount: 1200,
      status: 'UP',
    } as never)
    vi.mocked(systemApi.performanceTimeseries).mockResolvedValue([
      { time: '10:00', value: 180, p95: 360 },
    ] as never)
    vi.mocked(systemApi.performanceSlowQuery).mockResolvedValue({
      total: 1,
      list: [{ path: '/api/v1/live/session/search', avgMs: 640, callCount: 8, maxMs: 1300 }],
      pageNum: 0,
      pageSize: 10,
    } as never)
    vi.mocked(systemApi.performanceAnalysis).mockResolvedValue({
      status: 'DEGRADED',
      message: '慢查询采集样本不足',
    } as never)
    vi.mocked(systemApi.performanceCacheStatistics).mockResolvedValue({
      hitRate: 0.2228,
      hitCount: 2228,
      missCount: 7772,
      hotKeys: ['cache:kb:1'],
    } as never)
  })

  it('renders real performance data and normalizes cache hit ratio', async () => {
    renderWithProviders(
      <MemoryRouter>
        <PerformanceMonitoringPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '性能监控' })).toBeInTheDocument()

    await waitFor(() => {
      expect(systemApi.performanceCurrent).toHaveBeenCalled()
      expect(systemApi.performanceTimeseries).toHaveBeenCalledWith({ metric: 'response_time', hours: 24 })
      expect(systemApi.performanceSlowQuery).toHaveBeenCalledWith({ rows: 10, minMs: 500 })
      expect(systemApi.performanceCacheStatistics).toHaveBeenCalled()
    })

    expect(await screen.findByText('22.3%')).toBeInTheDocument()
    expect(screen.getByText('/api/v1/live/session/search')).toBeInTheDocument()
    expect(screen.getAllByText('DEGRADED')[0]).toBeInTheDocument()
    expect(screen.getByText(/不再填充 mock 指标/)).toBeInTheDocument()
    expect(screen.getByText(/JVM、请求性能窗口与 Redis INFO/)).toBeInTheDocument()
    expect(screen.getByTestId('mock-echarts')).toHaveTextContent('"P50"')

    const collectorDowngrade = screen.getByTestId('performance-contract-downgrade')
    expect(collectorDowngrade).toHaveAttribute('data-downgrade-tone', 'collector-gap')
    expect(collectorDowngrade).toHaveAttribute('data-contract-scope', 'system-performance')
    expect(collectorDowngrade).toHaveAttribute('data-contract-endpoint', '/system/performance')

    const contractCards = screen.getAllByTestId('performance-contract-card')
    expect(contractCards).toHaveLength(5)
    expect(contractCards.some(card =>
      card.getAttribute('data-collector-name') === '响应时序'
      && card.getAttribute('data-contract-status') === 'ready'
      && card.getAttribute('data-contract-endpoint') === '/system/performance/api/timeseries'
      && card.getAttribute('data-sample-count') === '1',
    )).toBe(true)
    expect(contractCards.some(card =>
      card.getAttribute('data-collector-name') === 'Redis 缓存'
      && card.getAttribute('data-contract-status') === 'ready'
      && card.getAttribute('data-contract-endpoint') === '/system/performance/cache/statistics',
    )).toBe(true)

    const cacheAdvice = screen.getByTestId('performance-cache-hit-rate-advice')
    expect(cacheAdvice).toHaveAttribute('data-cache-health', 'critical')
    expect(cacheAdvice).toHaveAttribute('data-cache-hit-rate', '0.2228')
    expect(cacheAdvice).toHaveTextContent('热点 key 是否稳定')
    expect(cacheAdvice).toHaveTextContent('不自动改写 TTL 或伪造热 key')

    expect(screen.getByTestId('performance-timeseries-panel')).toHaveAttribute('data-contract-status', 'ready')
    expect(screen.getByTestId('performance-slow-query-panel')).toHaveAttribute('data-contract-status', 'ready')
    expect(screen.getByTestId('performance-cache-panel')).toHaveAttribute('data-cache-health', 'critical')
    expect(screen.getByTestId('performance-monitoring-page-workbench')).toHaveAttribute('data-contract-scope', 'system-performance-collector-observability')
    expect(screen.getByTestId('performance-monitoring-page-workbench')).toHaveAttribute('data-no-mock-cache-hot-key-injection', 'true')
    expect(screen.getByTestId('performance-monitoring-page-workbench')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByTestId('performance-monitoring-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/performance/cache/ttl/update')
    expect(screen.getByTestId('performance-contract-downgrade').getAttribute('data-unsupported-endpoints')).toContain('/system/performance/mock')
    expect(screen.getByTestId('performance-cache-hit-rate-advice')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByTestId('performance-cache-hit-rate-advice').getAttribute('data-unsupported-endpoints')).toContain('/system/performance/cache/flush')
  })

  it('shows empty-state copy when collectors return no rows', async () => {
    vi.mocked(systemApi.performanceTimeseries).mockResolvedValue([] as never)
    vi.mocked(systemApi.performanceSlowQuery).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 10,
    } as never)
    vi.mocked(systemApi.performanceCacheStatistics).mockResolvedValue({
      hitCount: 0,
      missCount: 0,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <PerformanceMonitoringPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('暂无 API 时序数据')).toBeInTheDocument()
    expect(screen.getByText('暂无慢查询数据。若数据库慢查询采集未开启，这里会保持空态。')).toBeInTheDocument()
    expect(screen.getAllByText('--')[0]).toBeInTheDocument()

    expect(screen.getByTestId('performance-timeseries-panel')).toHaveAttribute('data-contract-status', 'degraded')
    expect(screen.getByTestId('performance-timeseries-panel')).toHaveAttribute('data-sample-count', '0')
    expect(screen.getByTestId('performance-timeseries-empty')).toHaveAttribute('data-no-mock-timeseries-injection', 'true')
    expect(screen.getByTestId('performance-slow-query-panel')).toHaveAttribute('data-contract-status', 'degraded')
    expect(screen.getByTestId('performance-slow-query-empty')).toHaveAttribute('data-no-mock-slow-query-injection', 'true')
    expect(screen.getByTestId('performance-cache-panel')).toHaveAttribute('data-contract-status', 'degraded')
    expect(screen.getByTestId('performance-monitoring-page-workbench')).toHaveAttribute('data-all-collectors-degraded', 'true')
    expect(screen.queryByTestId('performance-cache-hit-rate-advice')).not.toBeInTheDocument()
  })

  it('marks per-collector request errors without local performance fallback', async () => {
    vi.mocked(systemApi.performanceCurrent).mockRejectedValue(new Error('current down') as never)
    vi.mocked(systemApi.performanceTimeseries).mockRejectedValue(new Error('timeseries down') as never)
    vi.mocked(systemApi.performanceSlowQuery).mockRejectedValue(new Error('slow down') as never)
    vi.mocked(systemApi.performanceAnalysis).mockRejectedValue(new Error('analysis down') as never)
    vi.mocked(systemApi.performanceCacheStatistics).mockRejectedValue(new Error('cache down') as never)

    renderWithProviders(
      <MemoryRouter>
        <PerformanceMonitoringPage />
      </MemoryRouter>,
    )

    const errorContract = await screen.findByTestId('performance-monitoring-error-contract')
    expect(errorContract).toHaveAttribute('data-current-error', 'true')
    expect(errorContract).toHaveAttribute('data-timeseries-error', 'true')
    expect(errorContract).toHaveAttribute('data-slow-query-error', 'true')
    expect(errorContract).toHaveAttribute('data-analysis-error', 'true')
    expect(errorContract).toHaveAttribute('data-cache-error', 'true')
    expect(errorContract).toHaveAttribute('data-no-local-performance-metric-fallback', 'true')
    expect(screen.getByTestId('performance-monitoring-page-workbench')).toHaveAttribute('data-has-error', 'true')
  })

  it('tolerates wrapped performance payload mocks', async () => {
    vi.mocked(systemApi.performanceCurrent).mockResolvedValue({ data: { cpu: 20, memory: 55, requestCount: 1800, status: 'UP' } } as never)
    vi.mocked(systemApi.performanceTimeseries).mockResolvedValue({ records: [{ time: '11:00', p50: 100, p95: 400, p99: 900 }] } as never)
    vi.mocked(systemApi.performanceSlowQuery).mockResolvedValue({ data: { rows: [{ path: '/api/wrapped', avgMs: 800, callCount: 4, maxMs: 1600 }] } } as never)
    vi.mocked(systemApi.performanceAnalysis).mockResolvedValue({ data: { status: 'WRAPPED', message: '包装分析' } } as never)
    vi.mocked(systemApi.performanceCacheStatistics).mockResolvedValue({ data: { hitRatePercent: 66.6, hitCount: 666, missCount: 334 } } as never)

    renderWithProviders(
      <MemoryRouter>
        <PerformanceMonitoringPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('66.6%')).toBeInTheDocument()
    expect(screen.getByText('/api/wrapped')).toBeInTheDocument()
    expect(screen.getAllByText('WRAPPED')[0]).toBeInTheDocument()
  })

  it('uses theme-aware response time trend colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <PerformanceMonitoringPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    const chart = await screen.findByTestId('mock-echarts')
    expect(chart).toHaveTextContent('"P95"')
    expect(chart).not.toHaveTextContent('#ff9800')
    expect(chart).not.toHaveTextContent('#f44336')
    expect(chart).not.toHaveTextContent('#4caf50')
  })
})
