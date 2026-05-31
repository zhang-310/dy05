import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import AiCallLogPage from '../AiCallLogPage'
import { aiApi } from '@/api/ai'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => <div data-testid="echarts">{JSON.stringify(option)}</div>,
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    callVolumeTrend: vi.fn(),
    callTypeDistribution: vi.fn(),
    adminCallLogList: vi.fn(),
  },
}))

describe('AiCallLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(aiApi.callVolumeTrend).mockResolvedValue([
      { date: '2026-05-20', total: 12, success: 10, failed: 2 },
    ] as never)
    vi.mocked(aiApi.callTypeDistribution).mockResolvedValue([
      { callType: 'kb_search', count: 8 },
      { callType: 'tts', count: 4 },
    ] as never)
    vi.mocked(aiApi.adminCallLogList).mockResolvedValue({
      total: 2,
      pageNum: 0,
      pageSize: 20,
      list: [
        {
          id: 101,
          userId: 1,
          callType: 'kb_search',
          modelCode: 'hybrid',
          inputSummary: '查询敏感肌成分资料',
          promptTokens: 10,
          completionTokens: 20,
          totalTokens: 30,
          durationMs: 180,
          status: 1,
          isFallback: 0,
          stageTimings: '{"cache_lookup_ms":2}',
          createTime: '2026-05-20T23:00:00',
        },
        {
          id: 102,
          userId: 1,
          callType: 'live_script_full',
          modelCode: 'qwen',
          inputSummary: '整场话术生成失败',
          promptTokens: '12',
          completionTokens: '0',
          totalTokens: '12',
          durationMs: '1300',
          status: '0',
          isFallback: '1',
          errorMessage: '模型超时',
          stageTimings: '{"llm_ms":1300}',
          createTime: '2026-05-20T23:05:00',
        },
      ],
    } as never)
  })

  function renderPage() {
    renderWithProviders(
      <MemoryRouter>
        <AiCallLogPage />
      </MemoryRouter>,
    )
  }

  function renderPageWithTheme() {
    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AiCallLogPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )
  }

  it('loads charts and the real admin call log table', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: 'AI 调用分析' })).toBeInTheDocument()
    expect(screen.getByTestId('ai-call-log-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/admin/dashboard/call-volume-trend,/ai/admin/dashboard/call-type-distribution,/ai/admin/call-log/search',
    )
    expect(screen.getByTestId('ai-call-log-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/admin/call-log/mock,/ai/admin/call-log/local-cache,/ai/admin/dashboard/static-call-trend',
    )
    expect(screen.getByTestId('ai-call-log-page')).toHaveAttribute('data-no-local-call-log-fallback', 'true')

    await waitFor(() => {
      expect(aiApi.callVolumeTrend).toHaveBeenCalledWith({ days: 14 })
      expect(aiApi.callTypeDistribution).toHaveBeenCalledWith({ days: 14 })
      expect(aiApi.adminCallLogList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        keyword: undefined,
        callType: undefined,
        status: undefined,
      })
    })

    expect(screen.getByText('管理端真实接口：/ai/admin/call-log/search')).toBeInTheDocument()
    expect(await screen.findByText('查询敏感肌成分资料')).toBeInTheDocument()
    expect(screen.getAllByText('知识库检索').length).toBeGreaterThan(0)
    expect(screen.getByText('直播整场话术')).toBeInTheDocument()
    expect(screen.getByText('1/1')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('当前页 fallback 标记数量，来自日志 isFallback')).toBeInTheDocument()
    expect(screen.getAllByTestId('echarts').length).toBe(2)
    expect(screen.getByTestId('ai-call-log-trend-chart-surface')).toHaveAttribute(
      'data-source-endpoint',
      '/ai/admin/dashboard/call-volume-trend',
    )
    expect(screen.getByTestId('ai-call-log-distribution-chart-surface')).toHaveAttribute(
      'data-source-endpoint',
      '/ai/admin/dashboard/call-type-distribution',
    )
    expect(screen.getByTestId('ai-call-log-grid')).toHaveAttribute('data-pagination-mode', 'server')

    fireEvent.change(screen.getByPlaceholderText('搜索输入摘要'), { target: { value: '敏感肌' } })
    fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => {
      expect(aiApi.adminCallLogList).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        keyword: '敏感肌',
        callType: undefined,
        status: undefined,
      })
    })
  })

  it('labels partial loading errors by source and keeps successful log data visible', async () => {
    vi.mocked(aiApi.callVolumeTrend).mockRejectedValue(new Error('trend timeout') as never)
    vi.mocked(aiApi.callTypeDistribution).mockRejectedValue(new Error('distribution unavailable') as never)

    renderPage()

    expect(await screen.findByText(/调用趋势加载失败（\/ai\/admin\/dashboard\/call-volume-trend）：trend timeout/)).toBeInTheDocument()
    expect(screen.getByTestId('ai-call-log-load-error')).toHaveAttribute('data-no-local-error-fallback', 'true')
    expect(screen.getByText(/类型分布加载失败（\/ai\/admin\/dashboard\/call-type-distribution）：distribution unavailable/)).toBeInTheDocument()
    expect(screen.getByText('查询敏感肌成分资料')).toBeInTheDocument()
  })

  it('shows an explicit empty state when the log query returns no rows', async () => {
    vi.mocked(aiApi.adminCallLogList).mockResolvedValue({
      total: 0,
      pageNum: 0,
      pageSize: 20,
      list: [],
    } as never)

    renderPage()

    expect(await screen.findByText('当前筛选没有调用日志；如近期已有 AI 请求，请确认 `ai_call_log` 写入、`/ai/admin/call-log/search` 参数和管理员查询权限。')).toBeInTheDocument()
    expect(screen.getByTestId('ai-call-log-empty-state')).toHaveAttribute('data-source-endpoint', '/ai/admin/call-log/search')
    expect(screen.getByTestId('ai-call-log-empty-state')).toHaveAttribute('data-no-static-log-fallback', 'true')
  })

  it('marks incomplete trend metrics as an explicit backend downgrade', async () => {
    vi.mocked(aiApi.callVolumeTrend).mockResolvedValue([
      { date: '2026-05-20', total: 12, success: 12 },
    ] as never)

    renderPage()

    expect(await screen.findByTestId('ai-call-log-trend-downgrade')).toHaveAttribute(
      'data-degrade-source',
      '/ai/admin/dashboard/call-volume-trend',
    )
  })

  it('opens the detail dialog with error and fallback diagnostics', async () => {
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByText('整场话术生成失败')).toBeInTheDocument()
    const detailButtons = screen.getAllByRole('button', { name: /详情/ })
    await user.click(detailButtons[1])

    expect(await screen.findByRole('heading', { name: '调用日志详情' })).toBeInTheDocument()
    expect(screen.getAllByText('直播整场话术').length).toBeGreaterThan(0)
    expect(screen.getAllByText('失败').length).toBeGreaterThan(0)
    expect(screen.getAllByText('是').length).toBeGreaterThan(0)
    expect(screen.getByText('模型超时')).toBeInTheDocument()
    expect(screen.getByText(/"llm_ms": 1300/)).toBeInTheDocument()
  })

  it('uses a theme-aware stage diagnostics surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    const user = userEvent.setup()
    renderPageWithTheme()

    expect(await screen.findByText('整场话术生成失败')).toBeInTheDocument()
    const detailButtons = screen.getAllByRole('button', { name: /详情/ })
    await user.click(detailButtons[1])

    expect(await screen.findByRole('heading', { name: '调用日志详情' })).toBeInTheDocument()
    expect(screen.getByTestId('ai-call-log-stage-preview')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('uses theme-aware trend chart colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    const trendChart = await screen.findByTestId('ai-call-log-trend-chart-surface')
    await waitFor(() => expect(trendChart.textContent).toContain('调用量'))

    expect(trendChart.textContent).not.toContain('#1976d2')
    expect(trendChart.textContent).not.toContain('#2e7d32')
    expect(trendChart.textContent).not.toContain('#d32f2f')
    expect(trendChart.textContent).toContain('#e3f2fd')
    expect(trendChart.textContent).toContain('#81c784')
    expect(trendChart.textContent).toContain('#e57373')
  })

  it('uses a theme-aware distribution hover shadow in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    const distChart = await screen.findByTestId('ai-call-log-distribution-chart-surface')
    await waitFor(() => expect(distChart.textContent).toContain('调用类型'))

    expect(distChart.textContent).not.toContain('rgba(0,0,0,0.35)')
    expect(distChart.textContent).toContain('rgba(0, 0, 0, 0.55)')
    expect(distChart).toHaveAttribute('data-hover-shadow-color', 'rgba(0, 0, 0, 0.55)')
  })
})
