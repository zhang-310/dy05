import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import AiQuotaPage from '../AiQuotaPage'
import { aiApi } from '@/api/ai'

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts" />,
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    quotaGet: vi.fn(),
    quotaTrend: vi.fn(),
    quotaHistory: vi.fn(),
    quotaUpdate: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('AiQuotaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.quotaGet).mockResolvedValue({
      dailyMax: 1000,
      items: [
        { feature: 'script_gen', used: 800, limit: 1000, unit: '次', period: 'daily' },
        { feature: 'kb_search', used: 50, limit: 100, unit: '次', period: 'daily' },
      ],
    } as never)
    vi.mocked(aiApi.quotaTrend).mockResolvedValue([
      { date: '2026-05-20', usedCount: 850, maxCount: 1100 },
    ] as never)
    vi.mocked(aiApi.quotaHistory).mockResolvedValue({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [{ id: 1, feature: 'script_gen', used: 800, limit: 1000, period: 'daily', createTime: '2026-05-20T10:00:00' }],
    } as never)
  })

  function renderPage() {
    renderWithProviders(
      <MemoryRouter>
        <AiQuotaPage />
      </MemoryRouter>,
    )
  }

  function renderPageWithTheme() {
    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AiQuotaPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )
  }

  it('renders quota summary and history', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: 'AI 配额管理' })).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/admin/quota/get,/ai/admin/dashboard/quota-trend,/ai/admin/quota/history,/ai/admin/quota/update',
    )
    expect(screen.getByTestId('ai-quota-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/admin/quota/mock,/ai/admin/quota/local-cache,/ai/admin/dashboard/static-quota-trend',
    )
    expect(screen.getByTestId('ai-quota-page')).toHaveAttribute('data-no-local-quota-fallback', 'true')

    await waitFor(() => {
      expect(aiApi.quotaGet).toHaveBeenCalled()
      expect(aiApi.quotaTrend).toHaveBeenCalledWith({ days: 30 })
      expect(aiApi.quotaHistory).toHaveBeenCalled()
    })

    expect(await screen.findByText('总使用率')).toBeInTheDocument()
    expect(screen.getByText('预警功能')).toBeInTheDocument()
    expect(screen.getAllByText('话术生成').length).toBeGreaterThan(0)
    expect(screen.getByText('近30天额度趋势（全站 ai_call_quota 按日汇总）')).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-feature-list')).toHaveAttribute('data-source-endpoint', '/ai/admin/quota/get')
    expect(screen.getByTestId('ai-quota-trend-chart-surface')).toHaveAttribute('data-source-endpoint', '/ai/admin/dashboard/quota-trend')
    expect(screen.getByTestId('ai-quota-history-grid')).toHaveAttribute('data-pagination-mode', 'server')
  })

  it('renders missing quota limits as unconfigured instead of zero percent', async () => {
    vi.mocked(aiApi.quotaGet).mockResolvedValue({
      dailyMax: 1000,
      items: [
        { feature: 'script_gen', used: 80, unit: '次', period: 'daily' },
        { feature: 'kb_search', used: 50, limit: 100, unit: '次', period: 'daily' },
      ],
    } as never)

    renderPage()

    expect(await screen.findByText('1 个配额项未返回上限，已按“未配置”展示；请检查 ai.quota.feature.*.limit 或 dailyMax 配置。')).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-missing-limit-warning')).toHaveAttribute('data-degrade-source', '/ai/admin/quota/get')
    expect(screen.getAllByText('未配置').length).toBeGreaterThan(0)
    expect(screen.getByText('后端未返回该功能上限，页面不按 0% 处理；请检查 ai.quota.feature 配置。')).toBeInTheDocument()
  })

  it('labels partial loading errors by data source', async () => {
    vi.mocked(aiApi.quotaTrend).mockRejectedValue(new Error('quota trend timeout') as never)
    vi.mocked(aiApi.quotaHistory).mockRejectedValue(new Error('history table locked') as never)

    renderPage()

    expect(await screen.findByText(/配额趋势加载失败（\/ai\/admin\/dashboard\/quota-trend）：quota trend timeout/)).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-load-error')).toHaveAttribute('data-no-local-error-fallback', 'true')
    expect(screen.getByText(/历史用量加载失败（\/ai\/admin\/quota\/history）：history table locked/)).toBeInTheDocument()
    expect(screen.getAllByText('话术生成').length).toBeGreaterThan(0)
  })

  it('shows explicit quota trend downgrade when chart data is empty', async () => {
    vi.mocked(aiApi.quotaTrend).mockResolvedValue([] as never)

    renderPage()

    expect(await screen.findByText('暂无趋势数据；如已有调用，请检查 ai_call_quota 是否按日写入。')).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-trend-empty')).toHaveAttribute('data-source-endpoint', '/ai/admin/dashboard/quota-trend')
    expect(screen.getByTestId('ai-quota-trend-empty')).toHaveAttribute('data-no-static-trend-fallback', 'true')
  })

  it('submits quota limit edits to the real update endpoint', async () => {
    const user = userEvent.setup()
    vi.mocked(aiApi.quotaUpdate).mockResolvedValue(undefined as never)

    renderPage()

    await screen.findByText('总使用率')
    await user.click(screen.getByRole('button', { name: '调整配额上限' }))
    const dailyMaxInput = screen.getByLabelText(/日总调用上限/)
    await user.clear(dailyMaxInput)
    await user.type(dailyMaxInput, '1500')
    await user.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(aiApi.quotaUpdate).toHaveBeenCalledWith(expect.objectContaining({ dailyMax: 1500 }))
    })
  })

  it('keeps quota edit dialog input and shows update endpoint when save fails', async () => {
    const user = userEvent.setup()
    vi.mocked(aiApi.quotaUpdate).mockRejectedValueOnce(new Error('config write denied') as never)

    renderPage()

    await screen.findByText('总使用率')
    await user.click(screen.getByRole('button', { name: '调整配额上限' }))
    const dailyMaxInput = screen.getByLabelText(/日总调用上限/)
    await user.clear(dailyMaxInput)
    await user.type(dailyMaxInput, '1500')
    await user.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/配额更新失败（\/ai\/admin\/quota\/update）：config write denied/)).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-update-error')).toHaveAttribute('data-preserves-form-input', 'true')
    expect(screen.getByDisplayValue('1500')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '调整配额上限' })).toBeInTheDocument()
  })

  it('uses theme-aware quota warning and progress surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    expect(await screen.findByText('80.0%')).toBeInTheDocument()
    expect(screen.getByTestId('ai-quota-warning-icon-surface')).toBeInTheDocument()
    expect(screen.getAllByTestId('ai-quota-percent-chip-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(255, 152, 0)',
    })
    expect(screen.getAllByTestId('ai-quota-progress-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(255, 152, 0)',
    })
  })
})
