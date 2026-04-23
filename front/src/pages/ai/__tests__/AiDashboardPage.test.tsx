import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
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
    searchStats: vi.fn(),
    infraDetail: vi.fn(),
    monitoringConfig: vi.fn(),
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
    } as never)
    vi.mocked(aiApi.callVolumeTrend).mockResolvedValue([] as never)
    vi.mocked(aiApi.callTypeDistribution).mockResolvedValue([] as never)
    vi.mocked(aiApi.quotaTrend).mockResolvedValue([] as never)
    vi.mocked(aiApi.dashboardCostBreakdown).mockResolvedValue([] as never)
    vi.mocked(aiApi.searchStats).mockResolvedValue({ totalRequests: 0, qps: '0.00' } as never)
    vi.mocked(aiApi.infraDetail).mockResolvedValue({} as never)
    vi.mocked(aiApi.monitoringConfig).mockResolvedValue({} as never)
    vi.mocked(aiApi.infraHealth).mockResolvedValue([
      { component: 'milvus', ok: true },
      { component: 'elasticsearch', ok: true },
    ] as never)
    vi.mocked(aiApi.cacheStats).mockResolvedValue({
      hitRate: 85.2,
      hit: 852,
      total: 1000,
    } as never)
  })

  it('loads dashboard stats and renders KPI cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AiDashboardPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'AI 中心仪表盘' })).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.dashboardStats).toHaveBeenCalled()
      expect(aiApi.infraHealth).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('今日AI调用')).toBeInTheDocument()
      expect(screen.getByText('128')).toBeInTheDocument()
      expect(screen.getByText('基础设施健康')).toBeInTheDocument()
    })
  })
})
