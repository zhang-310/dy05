import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import AiMonitoringPage from '../AiMonitoringPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    infraHealth: vi.fn(),
    cacheStats: vi.fn(),
    cacheDiagnostics: vi.fn(),
    searchStats: vi.fn(),
  },
}))

describe('AiMonitoringPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.infraHealth).mockResolvedValue([
      { component: 'redis', ok: true, message: 'ready' },
      { component: 'milvus', ok: false, message: 'connection refused' },
    ] as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({
      hitRate: 22.28,
      hit: 2228,
      total: 10000,
      keyCount: 30,
    } as never)
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      host: 'redis://dy-redis:6379',
      dbSize: 3039,
      kbCacheTtlSeconds: 21600,
      embeddingCacheTtlDays: 14,
      redisStats: {
        globalHitRate: 62.01,
        keyspaceHits: 1200,
        keyspaceMisses: 735,
      },
      businessStats: {
        hitRate: 22.28,
        hit: 2228,
        total: 10000,
      },
      scan: {
        prefixCounts: { 'cache:kb': 30, 'cache:embedding': 2952 },
        ttlBuckets: { lt1d: 30, gte1d: 2952 },
      },
      suggestions: ['KB 业务缓存命中率偏低，建议做热点 query 预热'],
    } as never)
    vi.mocked(aiApi.searchStats).mockResolvedValue({
      p95Ms: 180,
      totalRequests: 50,
      qps: 0.2,
    } as never)
  })

  function renderPage() {
    renderWithProviders(
      <MemoryRouter>
        <AiMonitoringPage />
      </MemoryRouter>,
    )
  }

  it('renders health, cache diagnostics, and suggestions', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: 'AI 运行监控' })).toBeInTheDocument()
    expect(screen.getByTestId('ai-monitoring-page')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByTestId('ai-monitoring-page')).toHaveAttribute('data-no-cache-flush-action', 'true')

    await waitFor(() => {
      expect(aiApi.infraHealth).toHaveBeenCalled()
      expect(aiApi.cacheDiagnostics).toHaveBeenCalled()
      expect(aiApi.searchStats).toHaveBeenCalled()
    })

    expect(await screen.findByText('异常组件：milvus（connection refused）')).toBeInTheDocument()
    expect(screen.getAllByText('KB 业务命中率').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Redis 全局命中率').length).toBeGreaterThan(0)
    expect(screen.getByTestId('ai-monitoring-cache-contract')).toHaveAttribute('data-no-mock-hot-key-injection', 'true')
    expect(screen.getByText('cache:embedding 2,952')).toBeInTheDocument()
    expect(screen.getByText('KB 业务缓存命中率偏低，建议做热点 query 预热')).toBeInTheDocument()
  })

  it('treats empty health details as unreported instead of disconnected', async () => {
    vi.mocked(aiApi.infraHealth).mockResolvedValue([] as never)

    renderPage()

    expect(await screen.findByText('健康检查接口未返回组件明细，页面按“未上报”处理；请确认后端 `/ai/admin/infra/health` 采集器是否启用。')).toBeInTheDocument()
    expect(screen.getByTestId('ai-monitoring-unreported-health')).toHaveAttribute('data-no-disconnected-inference', 'true')
    expect(screen.getByText('健康检查接口未返回组件明细，按“未上报”展示；请确认后端 `/ai/admin/infra/health` 已可访问并启用采集。')).toBeInTheDocument()
    expect(screen.getByText('0/0')).toBeInTheDocument()
  })

  it('does not render missing Redis hit rate as zero percent', async () => {
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      host: 'redis://dy-redis:6379',
      dbSize: 3039,
      kbCacheTtlSeconds: 21600,
      embeddingCacheTtlDays: 14,
      businessStats: {
        hitRate: 82,
        hit: 8200,
        total: 10000,
      },
      scan: {
        prefixCounts: {},
        ttlBuckets: {},
      },
      suggestions: [],
    } as never)

    renderPage()

    await waitFor(() => expect(aiApi.cacheDiagnostics).toHaveBeenCalled())
    await waitFor(() => expect(screen.getAllByText('Redis 全局命中率').length).toBeGreaterThan(0))
    expect(screen.getAllByText('未上报').length).toBeGreaterThan(0)
    expect(screen.getByText('缓存诊断接口未上报 Redis INFO 命中率，不能按 0% 处理。')).toBeInTheDocument()
    expect(screen.queryByText('0.0%')).not.toBeInTheDocument()
  })

  it('labels partial load errors by source', async () => {
    vi.mocked(aiApi.infraHealth).mockRejectedValue(new Error('health timeout') as never)
    vi.mocked(aiApi.cacheDiagnostics).mockRejectedValue(new Error('redis info denied') as never)

    renderPage()

    expect(await screen.findByText(/健康检查加载失败（\/ai\/admin\/infra\/health）：health timeout/)).toBeInTheDocument()
    expect(screen.getByTestId('ai-monitoring-load-error')).toHaveAttribute('data-no-local-health-fallback', 'true')
    expect(screen.getByText(/缓存诊断加载失败（\/ai\/admin\/infra\/cache\/diagnostics）：redis info denied/)).toBeInTheDocument()
    expect(screen.getByText('Hit 2,228 / Total 10,000')).toBeInTheDocument()
  })

  it('generates explicit cache advice when low hit rate has no backend suggestion', async () => {
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      host: 'redis://dy-redis:6379',
      dbSize: 3039,
      kbCacheTtlSeconds: 21600,
      embeddingCacheTtlDays: 14,
      redisStats: {
        globalHitRate: 22.28,
        keyspaceHits: 2228,
        keyspaceMisses: 7772,
      },
      businessStats: {
        hitRate: 22.28,
        hit: 2228,
        total: 10000,
      },
      scan: {
        prefixCounts: { 'cache:kb': 30 },
        ttlBuckets: { lt1d: 30 },
      },
      suggestions: [],
    } as never)

    renderPage()

    expect(await screen.findByText('KB 业务缓存命中率低于 50%，但后端未返回诊断建议；请检查热点 query 归一化、预热任务和业务缓存 TTL。')).toBeInTheDocument()
    expect(screen.getAllByTestId('ai-monitoring-suggestion')[0]).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByText('Redis 全局命中率低于 50%，但后端未返回诊断建议；请核对热点 key、淘汰策略和缓存 TTL 配置。')).toBeInTheDocument()
    expect(screen.queryByText('暂无明确告警建议，当前监控数据未触发缓存或检索侧风险提示。')).not.toBeInTheDocument()
  })

  it('shows a visible downgrade when search latency percentiles are missing', async () => {
    vi.mocked(aiApi.searchStats).mockResolvedValue({
      totalRequests: 50,
      qps: 0.2,
    } as never)

    renderPage()

    expect(await screen.findByText('检索统计接口未上报延迟分位数据，当前只展示已返回的请求量和 QPS。')).toBeInTheDocument()
    expect(screen.getByTestId('ai-monitoring-search-latency-missing')).toHaveAttribute('data-no-local-search-metric-fallback', 'true')
  })
})
