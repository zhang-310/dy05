import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AiDashboardPage from '../AiDashboardPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    dashboardStats: vi.fn(),
    callVolumeTrend: vi.fn(),
    callTypeDistribution: vi.fn(),
    quotaTrend: vi.fn(),
    dashboardCostBreakdown: vi.fn(),
    infraHealth: vi.fn(),
    cacheStats: vi.fn(),
    cacheDiagnostics: vi.fn(),
    searchStats: vi.fn(),
    infraDetail: vi.fn(),
    monitoringConfig: vi.fn(),
    dashboardKbQualityRescan: vi.fn(),
    kbList: vi.fn(),
    adminCallLogList: vi.fn(),
  },
}))

describe('AiDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.dashboardStats).mockResolvedValue({
      todayCalls: 128,
      monthTokens: 235000,
      successRate: 97.5,
      avgQualityScore: 82.3,
      qualityDocCount: 100,
      evaluatedQualityDocCount: 92,
      unevaluatedQualityDocCount: 8,
      lowQualityDocCount: 3,
      qualityEvaluationCoverage: 92,
    } as never)
    vi.mocked(aiApi.callVolumeTrend).mockResolvedValue([] as never)
    vi.mocked(aiApi.callTypeDistribution).mockResolvedValue([] as never)
    vi.mocked(aiApi.quotaTrend).mockResolvedValue([] as never)
    vi.mocked(aiApi.dashboardCostBreakdown).mockResolvedValue([] as never)
    vi.mocked(aiApi.searchStats).mockResolvedValue({ totalRequests: 0, qps: '0.00' } as never)
    vi.mocked(aiApi.infraDetail).mockResolvedValue({} as never)
    vi.mocked(aiApi.monitoringConfig).mockResolvedValue({} as never)
    vi.mocked(aiApi.kbList).mockResolvedValue([
      { id: 1, kbName: 'douyin', description: '抖音官方学习中心', totalDocuments: 120, totalTokens: 60000, status: 1 },
      { id: 2, kbName: 'douyin_weigui', description: '抖音违规规则', totalDocuments: 80, totalTokens: 30000, status: 1 },
      { id: 3, kbName: 'douyin_viral_patterns', description: '爆款模式库', totalDocuments: 42, totalTokens: 24000, status: 1 },
      { id: 4, kbName: 'douyin_performance_reflections', description: '复盘经验库', totalDocuments: 18, totalTokens: 10000, status: 1 },
      { id: 5, kbName: 'douyin_ops_strategy', description: '运营策略库', totalDocuments: 12, totalTokens: 8000, status: 1 },
    ] as never)
    vi.mocked(aiApi.adminCallLogList).mockResolvedValue({
      list: [
        { id: 1, callType: 'live_script_full', status: 1, referencedChunkIds: '1,2,3' },
        { id: 2, callType: 'script_gen', status: 1, referencedChunkIds: '4,5' },
        { id: 3, callType: 'kb_search', status: 1, referencedChunkIds: '6' },
        { id: 4, callType: 'evolution', status: 1, referencedChunkIds: '7' },
      ],
      total: 4,
      page: 0,
      rows: 80,
    } as never)
    vi.mocked(aiApi.infraHealth).mockResolvedValue([
      { component: 'milvus', ok: true },
      { component: 'elasticsearch', ok: true },
    ] as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({
      hitRate: 85.2,
      hit: 852,
      total: 1000,
    } as never)
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      ok: true,
      dbSize: 128,
      kbCacheTtlSeconds: 21600,
      embeddingCacheTtlDays: 14,
      redisStats: {
        keyspaceHits: 1200,
        keyspaceMisses: 300,
        globalHitRate: 80,
      },
      businessStats: {
        hitRate: 85.2,
        hit: 852,
        total: 1000,
        keyCount: 42,
      },
      scan: {
        scanned: 64,
        prefixCounts: { 'cache:kb': 42, 'cache:embedding': 20 },
        ttlBuckets: { lt1d: 42, gte1d: 20 },
      },
      suggestions: ['Redis 缓存结构正常'],
    } as never)
  })

  it('loads dashboard stats and renders KPI cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AiDashboardPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'AI 总控首页' })).toBeInTheDocument()
    expect(screen.getByTestId('ai-dashboard-workbench')).toHaveAttribute('data-no-client-quality-score-synthesis', 'true')
    expect(screen.getByTestId('ai-dashboard-workbench')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')

    await waitFor(() => {
      expect(aiApi.dashboardStats).toHaveBeenCalled()
      expect(aiApi.infraHealth).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('今日AI调用')).toBeInTheDocument()
      expect(screen.getAllByText('128').length).toBeGreaterThan(0)
      expect(screen.getByText('已评估 92/100，覆盖率 92.0%，低质 3')).toBeInTheDocument()
      expect(screen.getByText('基础设施状态')).toBeInTheDocument()
      expect(screen.getByText('KB 业务缓存命中率')).toBeInTheDocument()
      expect(screen.getByText('总控状态')).toBeInTheDocument()
      expect(screen.getByText('业务链路接入')).toBeInTheDocument()
      expect(screen.getByText('官方规则引用')).toBeInTheDocument()
      expect(screen.getByText('知识资产完备')).toBeInTheDocument()
      expect(screen.getByText('业务链路 AI 接入看板')).toBeInTheDocument()
      expect(screen.getByText('知识资产中心')).toBeInTheDocument()
      expect(screen.getByText('直播话术生成')).toBeInTheDocument()
      expect(screen.getByText('短视频脚本/分镜')).toBeInTheDocument()
      expect(screen.getByText('违规检测与千川素材审核')).toBeInTheDocument()
      expect(screen.getByText('官方规则库')).toBeInTheDocument()
      expect(screen.getByText('违规规则库')).toBeInTheDocument()
      expect(screen.getByText('调用量趋势（按日）')).toBeInTheDocument()
      expect(screen.getByText('成本与缓存明细')).toBeInTheDocument()
    })
    expect(screen.getByTestId('ai-dashboard-command-center')).toHaveAttribute('data-no-static-operating-status', 'true')
    expect(screen.getByTestId('ai-dashboard-business-chain-board')).toHaveAttribute('data-no-static-chain-status', 'true')
    expect(screen.getByTestId('ai-dashboard-knowledge-assets')).toHaveAttribute('data-no-static-kb-assets', 'true')
  })

  it('shows explicit unreported infra state and low cache suggestions', async () => {
    vi.mocked(aiApi.dashboardStats).mockResolvedValue({
      todayCalls: 0,
      monthTokens: 0,
      successRate: 100,
      avgQualityScore: 30.8,
      qualityDocCount: 100,
      evaluatedQualityDocCount: 31,
      lowQualityDocCount: 28,
      qualityEvaluationCoverage: 31,
    } as never)
    vi.mocked(aiApi.infraHealth).mockResolvedValue([] as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({
      hitRate: 22.28,
      hit: 2228,
      miss: 7772,
      total: 10000,
      keyCount: 18,
    } as never)
    vi.mocked(aiApi.cacheDiagnostics).mockResolvedValue({
      dbSize: 18,
      kbCacheTtlSeconds: 21600,
      businessStats: {
        hitRate: 22.28,
        hit: 2228,
        total: 10000,
        keyCount: 18,
      },
      redisStats: {
        globalHitRate: 22.28,
        keyspaceHits: 2228,
        keyspaceMisses: 7772,
      },
      suggestions: ['缓存命中率偏低，建议检查热点 key 或增加缓存 TTL'],
    } as never)
    vi.mocked(aiApi.kbList).mockResolvedValue([
      { id: 2, kbName: 'douyin_weigui', description: '抖音违规规则', totalDocuments: 0, status: 1 },
    ] as never)
    vi.mocked(aiApi.adminCallLogList).mockResolvedValue({ list: [], total: 0, page: 0, rows: 80 } as never)

    renderWithProviders(
      <MemoryRouter>
        <AiDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('已评估 31/100，覆盖率 31.0%，低质 28')).toBeInTheDocument()
    expect(await screen.findByText('健康检查接口未返回组件明细，已按“未上报”展示；请确认后端监控采集是否启用。')).toBeInTheDocument()
    expect(screen.getAllByText('未上报').length).toBeGreaterThan(0)
    expect(screen.queryByText('未接入')).not.toBeInTheDocument()
    expect(screen.getByText('缓存命中率偏低，建议检查热点 key 或增加缓存 TTL')).toBeInTheDocument()
    expect(screen.getByText('知识库质量分偏低的处理顺序')).toBeInTheDocument()
    expect(screen.getByTestId('ai-dashboard-quality-action-advice')).toHaveAttribute('data-no-local-quality-score-mutation', 'true')
    expect(screen.getByTestId('ai-dashboard-quality-action-advice')).toHaveAttribute('data-quality-score', '30.8')
    expect(screen.getByText(/先补齐未评估文档/)).toBeInTheDocument()
    expect(screen.getByText('Redis 命中率优化动作')).toBeInTheDocument()
    expect(screen.getByTestId('ai-dashboard-cache-action-advice')).toHaveAttribute('data-no-automatic-cache-ttl-mutation', 'true')
    expect(screen.getByTestId('ai-dashboard-business-cache-contract')).toHaveAttribute('data-hit-rate', '22.3')
    expect(screen.getByText(/先看 Key 前缀分布与 TTL 分布/)).toBeInTheDocument()
    expect(screen.getByText(/违规检测链路必须补齐 douyin_weigui 知识库/)).toBeInTheDocument()
    expect(screen.getAllByText('尚未形成闭环').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/缺失：douyin/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/官方规则库未就绪/).length).toBeGreaterThan(0)
  })

  it('separates hard gate readiness from recent telemetry when no calls were made', async () => {
    vi.mocked(aiApi.dashboardStats).mockResolvedValue({
      todayCalls: 0,
      monthTokens: 0,
      successRate: 100,
      avgQualityScore: 82.3,
      qualityDocCount: 100,
      evaluatedQualityDocCount: 92,
      lowQualityDocCount: 0,
      qualityEvaluationCoverage: 92,
    } as never)
    vi.mocked(aiApi.adminCallLogList).mockResolvedValue({ list: [], total: 0, page: 0, rows: 80 } as never)

    renderWithProviders(
      <MemoryRouter>
        <AiDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/后端生成、槽位保存和审批通过均已启用官方引用强门禁/)).toBeInTheDocument()
    expect(screen.getByText(/短视频文案、脚本和制作方案生成已要求 douyin \+ douyin_weigui 引用/)).toBeInTheDocument()
    expect(screen.getByText(/短视频审核、发布审核和直播审核链路缺少 douyin_weigui 时不能通过/)).toBeInTheDocument()
    expect(screen.getAllByText(/当前暂无近期调用样本/).length).toBeGreaterThanOrEqual(3)
    expect(screen.getByText(/当前没有近期业务 AI 调用，强门禁已按能力状态展示/)).toBeInTheDocument()
    expect(screen.getByText(/强门禁链路就绪/)).toBeInTheDocument()
    expect(screen.getByText(/没有近期业务输出调用时显示未上报，不再误判为未接入/)).toBeInTheDocument()
    expect(screen.queryByText(/没有日志或没有引用时按缺口处理/)).not.toBeInTheDocument()
    expect(screen.queryByText(/官方规则引用未达标/)).not.toBeInTheDocument()
  })

  it('does not count kb_search telemetry as business output reference coverage', async () => {
    vi.mocked(aiApi.adminCallLogList).mockResolvedValue({
      list: [
        { id: 11, callType: 'kb_search', status: 1 },
        { id: 12, callType: 'kb_search', status: 1 },
      ],
      total: 2,
      page: 0,
      rows: 80,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AiDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/强门禁链路就绪/)).toBeInTheDocument()
    expect(screen.getAllByText('未上报').length).toBeGreaterThan(0)
    expect(screen.getByText(/当前没有近期业务 AI 调用，强门禁已按能力状态展示/)).toBeInTheDocument()
    expect(screen.queryByText('近期调用 2')).not.toBeInTheDocument()
    expect(screen.queryByText('引用覆盖 0%')).not.toBeInTheDocument()
  })

  it('shows endpoint-specific partial failure messages and rescan failure inline', async () => {
    vi.mocked(aiApi.dashboardStats).mockRejectedValue(new Error('stats offline') as never)
    vi.mocked(aiApi.callVolumeTrend).mockRejectedValue(new Error('trend offline') as never)
    vi.mocked(aiApi.callTypeDistribution).mockRejectedValue(new Error('dist offline') as never)
    vi.mocked(aiApi.quotaTrend).mockRejectedValue(new Error('quota offline') as never)
    vi.mocked(aiApi.dashboardCostBreakdown).mockRejectedValue(new Error('cost offline') as never)
    vi.mocked(aiApi.cacheStats).mockRejectedValue(new Error('cache stats offline') as never)
    vi.mocked(aiApi.cacheDiagnostics).mockRejectedValue(new Error('cache diagnostics offline') as never)
    vi.mocked(aiApi.searchStats).mockRejectedValue(new Error('search offline') as never)
    vi.mocked(aiApi.infraDetail).mockRejectedValue(new Error('detail offline') as never)
    vi.mocked(aiApi.kbList).mockRejectedValue(new Error('kb offline') as never)
    vi.mocked(aiApi.adminCallLogList).mockRejectedValue(new Error('call log offline') as never)
    vi.mocked(aiApi.dashboardKbQualityRescan).mockRejectedValue(new Error('rescan offline') as never)

    renderWithProviders(
      <MemoryRouter>
        <AiDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/AI 仪表盘统计加载失败：stats offline。来源：\/ai\/admin\/dashboard\/stats/)).toBeInTheDocument()
    expect(screen.getByTestId('ai-dashboard-stats-error')).toHaveAttribute('data-no-local-dashboard-fallback', 'true')
    expect(screen.getByTestId('ai-dashboard-stats-error')).toHaveAttribute('data-no-client-quality-score-synthesis', 'true')
    expect(await screen.findByText(/调用量趋势加载失败：trend offline。来源：\/ai\/admin\/dashboard\/call-volume-trend/)).toBeInTheDocument()
    expect(screen.getByText(/调用类型分布加载失败：dist offline。来源：\/ai\/admin\/dashboard\/call-type-distribution/)).toBeInTheDocument()
    expect(screen.getByText(/检索统计加载失败：search offline。来源：\/ai\/admin\/infra\/search\/stats/)).toBeInTheDocument()
    expect(screen.getByText(/缓存统计加载失败：cache stats offline。来源：\/ai\/admin\/infra\/cache\/stats/)).toBeInTheDocument()
    expect(screen.getByText(/缓存诊断加载失败：cache diagnostics offline。来源：\/ai\/admin\/infra\/cache\/diagnostics/)).toBeInTheDocument()
    expect(screen.getByText(/额度趋势加载失败：quota offline。来源：\/ai\/admin\/dashboard\/quota-trend/)).toBeInTheDocument()
    expect(screen.getByText(/Token 用量拆解加载失败：cost offline。来源：\/ai\/admin\/dashboard\/cost-breakdown/)).toBeInTheDocument()
    expect(screen.getByText(/基础设施详情加载失败：detail offline。来源：\/ai\/admin\/infra\/detail/)).toBeInTheDocument()
    expect(screen.getByText(/知识资产加载失败：kb offline/)).toBeInTheDocument()
    expect(screen.getByText(/业务链路调用日志加载失败：call log offline/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '重评估知识质量' }))
    expect(await screen.findByText(/知识库质量重评估失败：rescan offline。来源：\/ai\/admin\/dashboard\/kb-quality\/rescan/)).toBeInTheDocument()
    expect(screen.getByTestId('ai-dashboard-quality-rescan-error')).toHaveAttribute('data-no-local-quality-score-mutation', 'true')
  })
})
