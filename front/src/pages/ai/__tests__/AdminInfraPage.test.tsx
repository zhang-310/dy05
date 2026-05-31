import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import AdminInfraPage from '../AdminInfraPage'
import { aiApi } from '@/api/ai'

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts" />,
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    infraHealth: vi.fn(),
    cacheStats: vi.fn(),
    cacheDiagnostics: vi.fn(),
    searchStats: vi.fn(),
  },
}))

describe('AdminInfraPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.infraHealth).mockResolvedValue([
      { component: 'redis', ok: true, message: 'ready' },
      { component: 'milvus', ok: false, message: 'connection refused' },
      { component: 'elasticsearch', ok: true },
      { component: 'llm', ok: true },
    ] as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({
      hit: 80,
      miss: 20,
      total: 100,
      hitRate: 80,
      keyCount: 42,
    } as never)
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      host: 'redis://dy-redis:6379',
      dbSize: 42,
      kbCacheTtlSeconds: 21600,
      embeddingCacheTtlDays: 14,
      redisStats: { globalHitRate: 62.01, keyspaceHits: 1200, keyspaceMisses: 735 },
      scan: {
        scanned: 42,
        prefixCounts: { 'cache:kb': 30 },
        ttlBuckets: { lt1d: 30 },
      },
      suggestions: ['检查热点 key'],
    } as never)
    vi.mocked(aiApi.searchStats).mockResolvedValue({
      totalRequests: 10,
      qps: 0.1,
      avgLatencyMs: 80,
      p95Ms: 120,
    } as never)
  })

  it('renders infra health and redis diagnostics', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AdminInfraPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'AI 基础设施管理' })).toBeInTheDocument()
    expect(screen.getByTestId('admin-infra-page')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByTestId('admin-infra-page')).toHaveAttribute('data-no-cache-flush-action', 'true')

    await waitFor(() => {
      expect(aiApi.infraHealth).toHaveBeenCalled()
      expect(aiApi.cacheDiagnostics).toHaveBeenCalled()
    })

    expect(await screen.findByText(/异常组件：\s*milvus（connection refused）/)).toBeInTheDocument()
    expect(screen.getByText('Redis 诊断')).toBeInTheDocument()
    expect(screen.getByTestId('admin-infra-cache-diagnostics-contract')).toHaveAttribute('data-no-mock-hot-key-injection', 'true')
    expect(screen.getByText('cache:kb 30')).toBeInTheDocument()
    expect(screen.getByText('检查热点 key')).toBeInTheDocument()
  })

  it('does not label missing health components as unintegrated', async () => {
    vi.mocked(aiApi.infraHealth).mockResolvedValue([] as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({
      hit: 0,
      miss: 0,
      total: 0,
      hitRate: 0,
      keyCount: 0,
    } as never)
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      message: 'Redis 未配置',
      suggestions: ['检查 spring.data.redis.host/port'],
    } as never)
    vi.mocked(aiApi.searchStats).mockResolvedValue({
      message: 'SearchMetricsCollector 未配置',
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AdminInfraPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('健康检查接口未返回组件明细，页面按“未上报”展示 Milvus、Elasticsearch、Redis 和 LLM；请确认后端采集器和配置是否启用。')).toBeInTheDocument()
    expect(screen.getByTestId('admin-infra-unreported-health')).toHaveAttribute('data-no-disconnected-inference', 'true')
    expect(screen.getAllByText('未上报').length).toBeGreaterThan(0)
    expect(screen.getAllByText('后端健康检查未返回该组件').length).toBeGreaterThan(0)
    expect(screen.queryByText('未接入')).not.toBeInTheDocument()
  })

  it('shows endpoint scoped errors for partial infra failures', async () => {
    vi.mocked(aiApi.infraHealth).mockRejectedValue(new Error('health offline') as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({ hit: 0, miss: 0, total: 0, hitRate: 0 } as never)
    vi.mocked(aiApi.cacheDiagnostics).mockRejectedValue(new Error('redis diag offline') as never)
    vi.mocked(aiApi.searchStats).mockResolvedValue({ totalRequests: 0 } as never)

    renderWithProviders(
      <MemoryRouter>
        <AdminInfraPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/健康检查失败（\/ai\/admin\/infra\/health）：health offline/)).toBeInTheDocument()
    expect(screen.getByTestId('admin-infra-load-error')).toHaveAttribute('data-no-local-health-fallback', 'true')
    expect(screen.getByText(/Redis 诊断失败（\/ai\/admin\/infra\/cache\/diagnostics）：redis diag offline/)).toBeInTheDocument()
  })
})
