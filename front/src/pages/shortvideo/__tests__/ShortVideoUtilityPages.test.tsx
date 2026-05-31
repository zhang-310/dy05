import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import ContentEffectPredictPage from '../ContentEffectPredictPage'
import DataAnalysisPage from '../DataAnalysisPage'
import ShortVideoDashboardPage from '../ShortVideoDashboardPage'
import ShortVideoInsightsHubPage from '../ShortVideoInsightsHubPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    seoSuggestTags: vi.fn(),
    seoSuggestAbTitles: vi.fn(),
    dataTrend: vi.fn(),
    dashboardStats: vi.fn(),
    dashboardProjects: vi.fn(),
    dashboardCostBreakdown: vi.fn(),
    videoSearch: vi.fn(),
    list: vi.fn(),
  },
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

describe('ShortVideo utility pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
    vi.mocked(shortvideoApi.seoSuggestTags).mockResolvedValue({ list: ['敏感肌', '修护'] } as never)
    vi.mocked(shortvideoApi.seoSuggestAbTitles).mockResolvedValue({ records: ['敏感肌修护的 3 个关键'] } as never)
    vi.mocked(shortvideoApi.dataTrend).mockResolvedValue([
      { date: '2026-05-20', playCount: 1000, likeCount: 80, commentCount: 12, shareCount: 4 },
    ] as never)
    vi.mocked(shortvideoApi.dashboardStats).mockResolvedValue({
      totalVideoCount: 2,
      totalPlayCount: 1000,
      totalCost: 20,
      roi: 1.5,
    } as never)
    vi.mocked(shortvideoApi.dashboardProjects).mockResolvedValue([
      { id: 7, title: '短视频项目', projectType: 'daily', status: 'processing', stage: '素材准备', progress: 50 },
    ] as never)
    vi.mocked(shortvideoApi.dashboardCostBreakdown).mockResolvedValue({
      scriptCost: 0.01,
      imageCost: 0,
      videoCost: 0.4,
      voiceCost: 0.01,
      storageCost: 0.01,
      total: 0.42,
    } as never)
    vi.mocked(shortvideoApi.videoSearch).mockResolvedValue({
      total: 1,
      list: [{ id: 1, title: '成片 A', playCount: 1000, likeCount: 80, commentCount: 12 }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(shortvideoApi.list).mockResolvedValue({
      total: 1,
      list: [{ id: 7, title: '短视频项目', projectType: 'daily', status: 'processing' }],
      pageNum: 0,
      pageSize: 5,
    } as never)
  })

  it('ContentEffectPredictPage makes degradation explicit and renders wrapped SEO suggestions', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ContentEffectPredictPage />
      </MemoryRouter>,
    )

    expect(screen.getByText(/没有独立播放量\/转化率预测接口/)).toBeInTheDocument()
    expect(screen.getByTestId('content-effect-predict-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/seo/suggest-tags'),
    )
    expect(screen.getByTestId('content-effect-predict-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/effect-predict/local-score'),
    )
    expect(screen.getByTestId('content-effect-boundary-contract')).toHaveAttribute('data-no-play-count-prediction', 'true')

    fireEvent.change(screen.getByLabelText('标题'), { target: { value: '敏感肌修护' } })
    fireEvent.click(screen.getByRole('button', { name: '生成 SEO 传播建议' }))

    await waitFor(() => {
      expect(shortvideoApi.seoSuggestTags).toHaveBeenCalled()
      expect(shortvideoApi.seoSuggestAbTitles).toHaveBeenCalled()
    })

    expect(await screen.findByText('敏感肌')).toBeInTheDocument()
    expect(screen.getByTestId('content-effect-result-contract')).toHaveAttribute('data-no-client-score-synthesis', 'true')
    expect(screen.getByTestId('content-effect-tags-result')).toHaveAttribute('data-source-endpoint', '/short-video/seo/suggest-tags')
    expect(screen.getByTestId('content-effect-ab-title-result')).toHaveAttribute('data-source-endpoint', '/short-video/seo/suggest-ab-titles')
    expect(await screen.findByText(/敏感肌修护的 3 个关键/)).toBeInTheDocument()
    expect(await screen.findByText(/接口来源：POST \/short-video\/seo\/suggest-tags \/ \/short-video\/seo\/suggest-ab-titles/)).toBeInTheDocument()
  })

  it('ContentEffectPredictPage keeps partial SEO results and endpoint errors visible', async () => {
    vi.mocked(shortvideoApi.seoSuggestTags).mockRejectedValueOnce(new Error('tags down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ContentEffectPredictPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('标题'), { target: { value: '敏感肌修护' } })
    fireEvent.change(screen.getByLabelText('脚本内容'), { target: { value: '前三秒讲屏障痛点' } })
    fireEvent.click(screen.getByRole('button', { name: '生成 SEO 传播建议' }))

    expect(await screen.findByText(/SEO 传播建议生成失败：推荐标签失败（POST \/short-video\/seo\/suggest-tags）：tags down/)).toBeInTheDocument()
    expect(screen.getByTestId('content-effect-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('content-effect-tags-empty')).toHaveAttribute('data-no-local-tags-fallback', 'true')
    expect(screen.getByText(/另一条链路结果仍会展示/)).toBeInTheDocument()
    expect(await screen.findByText(/敏感肌修护的 3 个关键/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('敏感肌修护')).toBeInTheDocument()
    expect(screen.getByDisplayValue('前三秒讲屏障痛点')).toBeInTheDocument()
  })

  it('ContentEffectPredictPage shows both endpoint errors when SEO sources fail', async () => {
    vi.mocked(shortvideoApi.seoSuggestTags).mockRejectedValueOnce(new Error('tags down') as never)
    vi.mocked(shortvideoApi.seoSuggestAbTitles).mockRejectedValueOnce(new Error('titles down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ContentEffectPredictPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('标题'), { target: { value: '敏感肌修护' } })
    fireEvent.click(screen.getByRole('button', { name: '生成 SEO 传播建议' }))

    expect(await screen.findByText(/推荐标签失败（POST \/short-video\/seo\/suggest-tags）：tags down/)).toBeInTheDocument()
    expect(screen.getByText(/标题 A\/B 失败（POST \/short-video\/seo\/suggest-ab-titles）：titles down/)).toBeInTheDocument()
    expect(screen.getByText(/当前标题、脚本和计划发布时间会保留/)).toBeInTheDocument()
  })

  it('DataAnalysisPage renders real dashboard contracts and rule-generated report note', async () => {
    renderWithProviders(
      <MemoryRouter>
        <DataAnalysisPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('data-analysis-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/dashboard/trend'),
    )
    expect(screen.getByTestId('data-analysis-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/data-analysis/local-rank'),
    )
    expect(await screen.findByText('总播放量')).toBeInTheDocument()
    expect(screen.getByTestId('data-analysis-trend-tab')).toHaveAttribute('data-no-local-trend-synthesis', 'true')
    expect(await screen.findByTestId('data-analysis-trend-chart')).toHaveAttribute('data-source-endpoint', '/short-video/dashboard/trend')
    expect(await screen.findByTestId('echarts')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '运营报表' }))
    expect(screen.getByTestId('data-analysis-report-tab')).toHaveAttribute('data-rule-generated-report', 'true')
    expect(screen.getByTestId('data-analysis-boundary-contract')).toHaveTextContent(/规则诊断/)
    fireEvent.click(screen.getByRole('button', { name: '生成报表' }))

    expect(await screen.findByText(/运营建议（规则生成，非独立 AI 预测）/)).toBeInTheDocument()
  })

  it('DataAnalysisPage uses theme-aware report preview surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <DataAnalysisPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '运营报表' }))
    fireEvent.click(screen.getByRole('button', { name: '生成报表' }))

    expect(await screen.findByText(/运营建议（规则生成，非独立 AI 预测）/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-report-preview')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('DataAnalysisPage uses theme-aware trend KPI colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <DataAnalysisPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    const kpiValues = await screen.findAllByTestId('shortvideo-data-kpi-value-surface')
    expect(kpiValues).toHaveLength(4)
    expect(kpiValues.map(node => node.getAttribute('data-kpi-tone'))).toEqual(['primary', 'secondary', 'warning', 'success'])
    const colors = kpiValues.map(node => window.getComputedStyle(node).color)
    expect(colors).not.toContain('rgb(25, 118, 210)')
    expect(colors).not.toContain('rgb(233, 30, 99)')
    expect(colors).not.toContain('rgb(255, 152, 0)')
    expect(colors).not.toContain('rgb(76, 175, 80)')
    expect(colors).toEqual([
      'rgb(227, 242, 253)',
      'rgb(243, 229, 245)',
      'rgb(255, 183, 77)',
      'rgb(129, 199, 132)',
    ])
  })

  it('DataAnalysisPage shows endpoint-specific degradation without fake data', async () => {
    vi.mocked(shortvideoApi.dataTrend).mockRejectedValueOnce(new Error('trend down') as never)
    vi.mocked(shortvideoApi.dashboardStats).mockRejectedValueOnce(new Error('stats down') as never)
    vi.mocked(shortvideoApi.videoSearch).mockRejectedValueOnce(new Error('content down') as never)

    renderWithProviders(
      <MemoryRouter>
        <DataAnalysisPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/趋势数据加载失败（POST \/short-video\/dashboard\/trend）：trend down/)).toBeInTheDocument()
    expect(screen.getByTestId('data-analysis-trend-error')).toHaveAttribute('data-no-local-trend-synthesis', 'true')
    expect(screen.getByText(/趋势图不会用假播放量补齐/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '转化漏斗' }))
    expect(await screen.findByText(/漏斗数据加载失败（POST \/short-video\/dashboard\/stats）：stats down/)).toBeInTheDocument()
    expect(screen.getByTestId('data-analysis-funnel-error')).toHaveAttribute('data-no-local-funnel-synthesis', 'true')
    expect(screen.getByText(/不会用 1 或 100% 兜底/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '视频排行' }))
    expect(await screen.findByText(/视频排行加载失败（POST \/short-video\/content\/search）：content down/)).toBeInTheDocument()
    expect(screen.getByTestId('data-analysis-rank-error')).toHaveAttribute('data-no-local-rank-fallback', 'true')
    expect(screen.getByText(/不会展示本地排行/)).toBeInTheDocument()
  })

  it('DataAnalysisPage keeps report period when dashboard report generation fails', async () => {
    vi.mocked(shortvideoApi.dashboardStats).mockRejectedValueOnce(new Error('stats down') as never)

    renderWithProviders(
      <MemoryRouter>
        <DataAnalysisPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '运营报表' }))
    fireEvent.click(screen.getByRole('button', { name: '生成报表' }))

    expect(await screen.findByText(/报表生成失败（POST \/short-video\/dashboard\/stats）：stats down/)).toBeInTheDocument()
    expect(screen.getByTestId('data-analysis-report-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/已选择统计周期 7 天会保留/)).toBeInTheDocument()
  })

  it('DataAnalysisPage tolerates wrapped trend and video rank mocks', async () => {
    vi.mocked(shortvideoApi.dataTrend).mockResolvedValueOnce({
      data: { records: [{ date: '2026-05-22', playCount: 2200, likeCount: 160, commentCount: 20, shareCount: 8 }] },
    } as never)
    vi.mocked(shortvideoApi.videoSearch).mockResolvedValueOnce({
      data: {
        items: [{ id: 8, title: '包装排行视频', playCount: 2200, likeCount: 160, commentCount: 20 }],
        totalElements: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <DataAnalysisPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('2,200')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: '视频排行' }))
    expect(await screen.findByText('包装排行视频')).toBeInTheDocument()
  })

  it('Funnel tab does not fabricate data when dashboard stats are empty', async () => {
    vi.mocked(shortvideoApi.dashboardStats).mockResolvedValue({
      totalVideoCount: 0,
      totalPlayCount: 0,
      totalCost: 0,
      roi: 0,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <DataAnalysisPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '转化漏斗' }))

    expect(await screen.findByText(/暂无漏斗数据/)).toBeInTheDocument()
    expect(screen.getByTestId('data-analysis-funnel-empty')).toHaveAttribute('data-no-local-funnel-synthesis', 'true')
    expect(screen.queryByText(/转化 100.0%/)).not.toBeInTheDocument()
  })

  it('ShortVideoDashboardPage shows trend/project data with refreshable diagnostics', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ShortVideoDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('内容数据趋势')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-dashboard-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/dashboard/stats'),
    )
    expect(screen.getByTestId('shortvideo-dashboard-page')).toHaveAttribute(
      'data-ready-routes',
      expect.stringContaining('/shortvideo/quick-generate'),
    )
    expect(screen.getByTestId('shortvideo-dashboard-page')).toHaveAttribute(
      'data-supported-actions',
      expect.stringContaining('navigate-viral-videos'),
    )
    expect(screen.getByTestId('shortvideo-dashboard-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/dashboard/local-stats'),
    )
    expect(screen.getByTestId('shortvideo-dashboard-boundary-contract')).toHaveAttribute('data-no-static-kpi-fallback', 'true')
    expect(screen.getByTestId('shortvideo-dashboard-refresh-button')).toHaveAttribute('data-source-endpoints', expect.stringContaining('/short-video/dashboard/trend'))
    expect(screen.getByTestId('shortvideo-dashboard-navigation-contract')).toHaveAttribute('data-navigation-only', 'true')
    expect(await screen.findByText('成片项目')).toBeInTheDocument()
    expect(await screen.findByText('成本拆解')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-dashboard-kpi-contract')).toHaveAttribute('data-no-client-kpi-synthesis', 'true')
    expect(screen.getByTestId('shortvideo-dashboard-cost-breakdown')).toHaveAttribute('data-source-endpoint', '/short-video/dashboard/cost-breakdown')
    expect(screen.getByTestId('shortvideo-dashboard-trend-chart')).toHaveAttribute('data-source-endpoint', '/short-video/dashboard/trend')
    expect(screen.getByTestId('shortvideo-dashboard-project-list')).toHaveAttribute('data-source-endpoint', '/short-video/dashboard/projects')
    expect(await screen.findByText('短视频项目')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: '刷新' })).toBeInTheDocument()
  })

  it('ShortVideoDashboardPage shows endpoint-specific partial failures', async () => {
    vi.mocked(shortvideoApi.dashboardStats).mockRejectedValueOnce(new Error('stats down') as never)
    vi.mocked(shortvideoApi.dataTrend).mockRejectedValueOnce(new Error('trend down') as never)
    vi.mocked(shortvideoApi.dashboardProjects).mockRejectedValueOnce(new Error('projects down') as never)
    vi.mocked(shortvideoApi.dashboardCostBreakdown).mockRejectedValueOnce(new Error('cost down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ShortVideoDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/总览指标加载失败（POST \/short-video\/dashboard\/stats）：stats down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-dashboard-stats-error')).toHaveAttribute('data-no-static-kpi-fallback', 'true')
    expect(await screen.findByText(/成本拆解加载失败（POST \/short-video\/dashboard\/cost-breakdown）：cost down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-dashboard-cost-error')).toHaveAttribute('data-no-default-cost-fallback', 'true')
    expect(await screen.findByText(/趋势加载失败（POST \/short-video\/dashboard\/trend）：trend down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-dashboard-trend-error')).toHaveAttribute('data-no-local-trend-synthesis', 'true')
    expect(await screen.findByText(/项目列表加载失败（POST \/short-video\/dashboard\/projects）：projects down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-dashboard-projects-error')).toHaveAttribute('data-no-mock-project-fallback', 'true')
    expect(screen.getByText(/近期项目不会从本地 mock 补齐/)).toBeInTheDocument()
  })

  it('ShortVideoDashboardPage keeps empty and zero dashboard states explicit', async () => {
    vi.mocked(shortvideoApi.dataTrend).mockResolvedValueOnce([] as never)
    vi.mocked(shortvideoApi.dashboardProjects).mockResolvedValueOnce([] as never)

    renderWithProviders(
      <MemoryRouter>
        <ShortVideoDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('shortvideo-dashboard-trend-empty')).toHaveAttribute('data-no-mock-trend-fallback', 'true')
    expect(await screen.findByTestId('shortvideo-dashboard-projects-empty')).toHaveAttribute('data-no-mock-project-fallback', 'true')

    vi.clearAllMocks()
    vi.mocked(shortvideoApi.seoSuggestTags).mockResolvedValue({ list: ['敏感肌', '修护'] } as never)
    vi.mocked(shortvideoApi.seoSuggestAbTitles).mockResolvedValue({ records: ['敏感肌修护的 3 个关键'] } as never)
    vi.mocked(shortvideoApi.dataTrend).mockResolvedValue([
      { date: '2026-05-20', playCount: 0, likeCount: 0, commentCount: 0 },
    ] as never)
    vi.mocked(shortvideoApi.dashboardStats).mockResolvedValue({
      totalVideoCount: 2,
      totalPlayCount: 1000,
      totalCost: 20,
      roi: 1.5,
    } as never)
    vi.mocked(shortvideoApi.dashboardProjects).mockResolvedValue([
      { id: 7, title: '短视频项目', projectType: 'daily', status: 'processing', stage: '素材准备', progress: 50 },
    ] as never)
    vi.mocked(shortvideoApi.dashboardCostBreakdown).mockResolvedValue({
      scriptCost: 0.01,
      imageCost: 0,
      videoCost: 0.4,
      voiceCost: 0.01,
      storageCost: 0.01,
      total: 0.42,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ShortVideoDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('shortvideo-dashboard-trend-zero')).toHaveAttribute('data-no-client-trend-mutation', 'true')
  })

  it('ShortVideoDashboardPage tolerates wrapped dashboard payloads', async () => {
    vi.mocked(shortvideoApi.dashboardStats).mockResolvedValueOnce({
      data: { totalVideoCount: 3, totalPlayCount: 2200, totalCost: 33, roi: 2.1 },
    } as never)
    vi.mocked(shortvideoApi.dashboardProjects).mockResolvedValueOnce({
      records: [{ id: 8, title: '包装项目', status: 'processing', stage: '成片', progress: 80 }],
    } as never)
    vi.mocked(shortvideoApi.dashboardCostBreakdown).mockResolvedValueOnce({
      data: { scriptCost: 1, imageCost: 2, videoCost: 3, voiceCost: 4, storageCost: 5, total: 15 },
    } as never)
    vi.mocked(shortvideoApi.dataTrend).mockResolvedValueOnce({
      items: [{ date: '2026-05-22', playCount: 2200, likeCount: 160, commentCount: 20 }],
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ShortVideoDashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装项目')).toBeInTheDocument()
    expect(await screen.findByText('2,200')).toBeInTheDocument()
    expect(screen.getByText(/合计 ¥15.00/)).toBeInTheDocument()
  })

  it('ShortVideoInsightsHubPage marks route cards by capability status', () => {
    renderWithProviders(
      <MemoryRouter>
        <ShortVideoInsightsHubPage />
      </MemoryRouter>,
    )

    expect(screen.getByText(/洞见中心只保留导航和链路诊断/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-contract-scope', 'shortvideo-insights-navigation-hub')
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-ready-endpoints', 'navigation-only:no-page-api-request')
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-supported-actions', expect.stringContaining('navigate-script-recommendation'))
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/insights/local-ranking'),
    )
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-no-page-api-request', 'true')
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-navigation-hub-only', 'true')
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-no-inline-business-aggregation', 'true')
    expect(screen.getByTestId('shortvideo-insights-hub-workbench')).toHaveAttribute('data-card-count', '8')
    expect(screen.getByTestId('shortvideo-insights-hub-downgrade-contract')).toHaveAttribute('data-no-local-insight-ranking', 'true')
    expect(screen.getAllByText('真实链路').length).toBeGreaterThan(0)
    expect(screen.getAllByText('部分降级').length).toBeGreaterThan(0)
    expect(screen.getByText('质量脚本库')).toBeInTheDocument()
    expect(screen.getByText('脚本推荐')).toBeInTheDocument()
    expect(screen.getAllByTestId('shortvideo-insights-hub-card')).toHaveLength(8)
    expect(screen.getAllByTestId('shortvideo-insights-hub-card')[0]).toHaveAttribute('data-navigation-only', 'true')
    fireEvent.click(screen.getByTestId('shortvideo-insights-hub-card-action-real-质量脚本库'))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/benchmark/quality-scripts'))
  })
})
