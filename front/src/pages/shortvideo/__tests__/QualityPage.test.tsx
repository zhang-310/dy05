import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import QualityPage from '../QualityPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => <div data-testid="echarts">{JSON.stringify(option)}</div>,
}))

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    dataTrend: vi.fn(),
    dashboardStats: vi.fn(),
    qualityOverview: vi.fn(),
    qualityTrend: vi.fn(),
    qualityModelRanking: vi.fn(),
    qualityCameraRanking: vi.fn(),
    qualityAiReflections: vi.fn(),
    feedbackWeeklyReport: vi.fn(),
  },
}))

describe('QualityPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(shortvideoApi.dataTrend).mockResolvedValue([
      { date: '2026-05-21', playCount: 1000, likeCount: 80, commentCount: 10, shareCount: 3, completionRate: 0.42 },
    ] as never)
    vi.mocked(shortvideoApi.dashboardStats).mockResolvedValue({ totalVideoCount: 2, totalPlayCount: 1000 } as never)
    vi.mocked(shortvideoApi.qualityOverview).mockResolvedValue({ avgQualityScore: 86 } as never)
    vi.mocked(shortvideoApi.qualityTrend).mockResolvedValue([{ date: '2026-05-21', score: 86 }] as never)
    vi.mocked(shortvideoApi.qualityModelRanking).mockResolvedValue([{ rank: 1, model: 'kling', avgScore: 91 }] as never)
    vi.mocked(shortvideoApi.qualityCameraRanking).mockResolvedValue([{ rank: 1, cameraType: 'push_in', avgScore: 88 }] as never)
    vi.mocked(shortvideoApi.qualityAiReflections).mockResolvedValue(['模型 kling 近 7 天平均分最高（91.0），建议优先用于同类项目。'] as never)
    vi.mocked(shortvideoApi.feedbackWeeklyReport).mockResolvedValue({ summary: '质量稳定' } as never)
  })

  it('renders independent quality diagnostics and charts', async () => {
    renderWithProviders(
      <MemoryRouter>
        <QualityPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '短视频质量看板' })).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-quality-page')).toHaveAttribute(
      'data-ready-endpoints',
      [
        '/short-video/dashboard/trend',
        '/short-video/dashboard/stats',
        '/short-video/quality-dashboard/overview',
        '/short-video/quality-dashboard/trend',
        '/short-video/quality-dashboard/model-ranking',
        '/short-video/quality-dashboard/camera-ranking',
        '/short-video/quality-dashboard/ai-reflections',
        '/short-video/feedback/weekly-report',
      ].join('|'),
    )
    expect(screen.getByTestId('shortvideo-quality-page')).toHaveAttribute('data-no-local-quality-fallback', 'true')
    expect(screen.getByText(/质量概览来自/)).toBeInTheDocument()
    expect(await screen.findByText('totalVideoCount')).toBeInTheDocument()
    expect(await screen.findByText('avgQualityScore')).toBeInTheDocument()
    expect(await screen.findByTestId('shortvideo-quality-kpi-contract')).toHaveAttribute('data-no-static-dashboard-fallback', 'true')
    expect(await screen.findByText('反馈周报（摘要）')).toBeInTheDocument()
    expect(await screen.findByTestId('shortvideo-quality-feedback-report-contract')).toHaveAttribute('data-source-endpoint', '/short-video/feedback/weekly-report')
    expect(await screen.findByText('模型排名')).toBeInTheDocument()
    expect(await screen.findByText('1. kling')).toBeInTheDocument()
    expect(await screen.findByTestId('shortvideo-quality-model-ranking-contract')).toHaveAttribute('data-source-endpoint', '/short-video/quality-dashboard/model-ranking')
    expect(await screen.findByText('运镜排名')).toBeInTheDocument()
    expect(await screen.findByText('1. push_in')).toBeInTheDocument()
    expect(await screen.findByTestId('shortvideo-quality-camera-ranking-contract')).toHaveAttribute('data-source-endpoint', '/short-video/quality-dashboard/camera-ranking')
    expect(await screen.findByText(/模型 kling 近 7 天平均分最高/)).toBeInTheDocument()
    expect(await screen.findByTestId('shortvideo-quality-reflections-contract')).toHaveAttribute('data-no-template-reflections', 'true')
    expect(await screen.findAllByTestId('echarts')).toHaveLength(3)
  })

  it('uses theme-aware chart colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <QualityPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    const qualityChart = await screen.findByTestId('shortvideo-quality-score-trend-chart-surface')
    const dataTrendChart = await screen.findByTestId('shortvideo-quality-data-trend-chart-surface')
    const completionChart = await screen.findByTestId('shortvideo-quality-completion-chart-surface')

    expect(qualityChart).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(dataTrendChart).toHaveAttribute('data-chart-colors', '#e3f2fd|#f3e5f5|#ffb74d|#81c784')
    expect(completionChart).toHaveAttribute('data-chart-color', '#f3e5f5')

    const chartOptions = (await screen.findAllByTestId('echarts')).map(node => node.textContent ?? '')
    const serialized = chartOptions.join('\n')
    expect(serialized).not.toContain('#1976d2')
    expect(serialized).not.toContain('#e91e63')
    expect(serialized).not.toContain('#ff9800')
    expect(serialized).not.toContain('#4caf50')
    expect(serialized).not.toContain('#7b1fa2')
  })

  it('keeps quality sections independently degraded with endpoint sources', async () => {
    vi.mocked(shortvideoApi.dataTrend).mockRejectedValueOnce(new Error('trend down') as never)
    vi.mocked(shortvideoApi.dashboardStats).mockRejectedValueOnce(new Error('stats down') as never)
    vi.mocked(shortvideoApi.qualityOverview).mockRejectedValueOnce(new Error('quality down') as never)
    vi.mocked(shortvideoApi.qualityTrend).mockRejectedValueOnce(new Error('quality trend down') as never)
    vi.mocked(shortvideoApi.qualityModelRanking).mockRejectedValueOnce(new Error('model rank down') as never)
    vi.mocked(shortvideoApi.qualityCameraRanking).mockRejectedValueOnce(new Error('camera rank down') as never)
    vi.mocked(shortvideoApi.qualityAiReflections).mockRejectedValueOnce(new Error('reflection down') as never)
    vi.mocked(shortvideoApi.feedbackWeeklyReport).mockRejectedValueOnce(new Error('feedback down') as never)

    renderWithProviders(
      <MemoryRouter>
        <QualityPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/互动趋势加载失败（POST \/short-video\/dashboard\/trend）：trend down/)).toBeInTheDocument()
    expect(await screen.findByText(/Dashboard 统计加载失败（POST \/short-video\/dashboard\/stats）：stats down/)).toBeInTheDocument()
    expect(await screen.findByText(/质量概览加载失败（POST \/short-video\/quality-dashboard\/overview）：quality down/)).toBeInTheDocument()
    expect(await screen.findByText(/质量趋势加载失败（POST \/short-video\/quality-dashboard\/trend）：quality trend down/)).toBeInTheDocument()
    expect(await screen.findByText(/模型排名加载失败（POST \/short-video\/quality-dashboard\/model-ranking）：model rank down/)).toBeInTheDocument()
    expect(await screen.findByText(/运镜排名加载失败（POST \/short-video\/quality-dashboard\/camera-ranking）：camera rank down/)).toBeInTheDocument()
    expect(await screen.findByText(/AI 反思加载失败（POST \/short-video\/quality-dashboard\/ai-reflections）：reflection down/)).toBeInTheDocument()
    expect(await screen.findByText(/反馈周报不可用（POST \/short-video\/feedback\/weekly-report）：feedback down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-quality-data-trend-error')).toHaveAttribute('data-no-static-dashboard-fallback', 'true')
    expect(screen.getByTestId('shortvideo-quality-overview-error')).toHaveAttribute('data-no-local-quality-fallback', 'true')
    expect(screen.getByTestId('shortvideo-quality-reflections-error')).toHaveAttribute('data-no-template-reflections', 'true')
    expect(screen.getByText(/趋势图不会用假播放量补齐/)).toBeInTheDocument()
    expect(screen.getByText(/不展示静态模型榜/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '刷新' }))
    expect(shortvideoApi.dataTrend).toHaveBeenCalledTimes(2)
  })
})
