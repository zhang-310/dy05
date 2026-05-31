import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AttributionPage from '../AttributionPage'
import request from '@/utils/request'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => <div data-testid="echarts-mock">{JSON.stringify(option)}</div>,
}))

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('AttributionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(request.post).mockImplementation((url: string) => {
      if (url === '/live/session/search') {
        return Promise.resolve({ list: [] } as never)
      }
      return Promise.resolve([] as never)
    })
  })

  it('renders current attribution workspace shell', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '归因分析' })).toBeInTheDocument()
    const root = screen.getByTestId('attribution-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'ai-attribution-workbench')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/live/session/search')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/ai/attribution/summary')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/shortvideo/seo/suggest-publish-time')
    expect(root).toHaveAttribute('data-no-local-attribution-fallback', 'true')
    expect(root).toHaveAttribute('data-no-template-writeback', 'true')
    expect(screen.getByRole('tab', { name: '场次归因' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '话术归因' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '时段分析' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '场次对比' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '导出报告 PDF' })).toBeDisabled()
    expect(screen.getByText(/PDF 导出、热力图导出、“设为标准模板”和“跳转场次话术”暂无后端落库接口/)).toBeInTheDocument()
    expect(screen.getByTestId('attribution-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(screen.getByTestId('attribution-contract-downgrade')).toHaveAttribute('data-contract-scope', 'ai-attribution')
    expect(screen.getByTestId('attribution-contract-downgrade')).toHaveAttribute(
      'data-ready-endpoints',
      '/live/session/search,/ai/attribution/trigger,/ai/attribution/summary,/ai/attribution/session,/shortvideo/seo/suggest-publish-time',
    )
    expect(screen.getByTestId('attribution-contract-downgrade').getAttribute('data-unsupported-actions')).toContain('local-attribution-fallback')
    expect(screen.getByTestId('attribution-pdf-export-action')).toHaveAttribute('data-contract-status', 'unsupported')
    expect(screen.getByTestId('attribution-pdf-export-action')).toHaveAttribute('data-contract-action', 'pdf-export')
    expect(screen.getByTestId('attribution-session-tab')).toHaveAttribute('data-no-local-attribution-fallback', 'true')
    expect(screen.getByLabelText('选择场次')).toBeInTheDocument()
    expect(screen.getByText('请选择场次以查看归因数据，或先触发归因分析。')).toBeInTheDocument()

    await waitFor(() => {
      expect(request.post).toHaveBeenCalledWith('/live/session/search', { rows: 100 })
    })
  })

  it('marks heatmap export as disabled downgrade', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '时段分析' }))

    expect(screen.getByTestId('attribution-time-tab')).toHaveAttribute('data-no-local-heatmap-fallback', 'true')
    expect(screen.getByRole('button', { name: '导出热力图' })).toBeDisabled()
    expect(screen.getByTestId('attribution-heatmap-export-action')).toHaveAttribute('data-contract-status', 'unsupported')
    expect(screen.getByTestId('attribution-heatmap-export-action')).toHaveAttribute('data-contract-action', 'heatmap-export')
    expect(screen.getByText(/热力图导出按钮保持禁用/)).toBeInTheDocument()
  })

  it('marks local attribution summaries as local-only and does not call unsupported endpoints', async () => {
    vi.mocked(request.post).mockImplementation((url: string, body?: unknown) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [
              { id: 18, liveTitle: '晚场直播', createTime: '2026-05-22 20:00:00' },
              { id: 19, liveTitle: '早场直播', createTime: '2026-05-23 08:00:00' },
            ],
          },
        } as never)
      }
      if (url === '/ai/attribution/session') {
        return Promise.resolve({
          data: {
            rows: [
              { id: 2, sessionId: 18, attributionType: 'script_sales', scriptContent: '高转化开场话术', contributedGmv: 900, contributedSales: 6, conversionRate: 0.12, contributionRatio: 0.38, effectScore: 92, status: 1, createTime: '2026-05-22' },
              { id: 3, sessionId: 18, attributionType: 'script_sales', scriptContent: '低效铺垫话术', contributedGmv: 300, contributedSales: 2, conversionRate: 0.04, contributionRatio: 0.12, effectScore: 52, status: 1, createTime: '2026-05-22' },
            ],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary') {
        const sessionId = (body as { sessionId?: number } | undefined)?.sessionId ?? 18
        return Promise.resolve({
          data: {
            sessionId,
            totalGmv: sessionId === 18 ? 2400 : 1200,
            totalSales: sessionId === 18 ? 16 : 8,
            productAttributions: 1,
            scriptAttributions: 2,
            overallScore: sessionId === 18 ? 92 : 78,
            status: 'completed',
          },
        } as never)
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '话术归因' }))
    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/晚场直播/))

    const scriptLocalSummary = await screen.findByTestId('script-local-summary-surface')
    expect(scriptLocalSummary).toHaveAttribute('data-contract-status', 'local-only')
    expect(scriptLocalSummary).toHaveAttribute('data-contract-action', 'adopt-template')
    expect(scriptLocalSummary).toHaveAttribute('data-contract-endpoint', 'unavailable')
    expect(scriptLocalSummary).toHaveAttribute('data-no-template-writeback', 'true')

    fireEvent.click(screen.getByRole('tab', { name: '场次对比' }))
    fireEvent.click(await screen.findByText('晚场直播'))
    fireEvent.click(await screen.findByText('早场直播'))

    const compareLocalDiff = await screen.findByTestId('compare-local-diff-surface')
    expect(compareLocalDiff).toHaveAttribute('data-contract-status', 'local-only')
    expect(compareLocalDiff).toHaveAttribute('data-contract-actions', 'adopt-template,jump-to-script')
    expect(compareLocalDiff).toHaveAttribute('data-contract-endpoint', 'unavailable')
    expect(compareLocalDiff).toHaveAttribute('data-no-template-writeback', 'true')

    const calledUrls = vi.mocked(request.post).mock.calls.map(([url]) => url)
    expect(calledUrls).not.toContain('/ai/attribution/pdf-export')
    expect(calledUrls).not.toContain('/ai/attribution/heatmap-export')
    expect(calledUrls).not.toContain('/ai/attribution/adopt-template')
    expect(calledUrls).not.toContain('/ai/attribution/jump-to-script')
  })

  it('renders wrapped session options and attribution summary details', async () => {
    vi.mocked(request.post).mockImplementation((url: string) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [{ id: 18, liveTitle: '包装直播场次', createTime: '2026-05-22 10:00:00' }],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary') {
        return Promise.resolve({
          data: { sessionId: 18, totalGmv: 1200, totalSales: 8, productAttributions: 1, scriptAttributions: 1, overallScore: 88, aiAnalysis: '包装归因结论', status: 'completed' },
        } as never)
      }
      if (url === '/ai/attribution/session') {
        return Promise.resolve({
          data: {
            rows: [
              { id: 1, sessionId: 18, attributionType: 'product_gmv', productName: '面膜', contributedGmv: 1200, contributedSales: 8, contributionRatio: 1, effectScore: 88, status: 1, createTime: '2026-05-22' },
            ],
          },
        } as never)
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/包装直播场次/))

    expect(await screen.findByText('包装归因结论')).toBeInTheDocument()
    expect(await screen.findByText(/商品: 面膜/)).toBeInTheDocument()
  })

  it('keeps empty attribution responses as explicit no-fallback empty state', async () => {
    vi.mocked(request.post).mockImplementation((url: string) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [{ id: 18, liveTitle: '空归因场次', createTime: '2026-05-22 10:00:00' }],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary') {
        return Promise.resolve(null as never)
      }
      if (url === '/ai/attribution/session') {
        return Promise.resolve([] as never)
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/空归因场次/))

    expect(await screen.findByTestId('attribution-session-empty-no-fallback')).toHaveAttribute('data-no-local-attribution-fallback', 'true')
    expect(screen.queryByText('¥0')).not.toBeInTheDocument()
  })

  it('keeps selected session visible when trigger attribution fails', async () => {
    vi.mocked(request.post).mockImplementation((url: string) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [{ id: 18, liveTitle: '包装直播场次', createTime: '2026-05-22 10:00:00' }],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary' || url === '/ai/attribution/session') {
        return Promise.resolve({ data: [] } as never)
      }
      if (url === '/ai/attribution/trigger') {
        return Promise.reject(new Error('rate limited'))
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/包装直播场次/))
    fireEvent.click(screen.getByRole('button', { name: '触发归因分析' }))

    expect(await screen.findByText(/\/ai\/attribution\/trigger 触发失败，sessionId=18：rate limited/)).toBeInTheDocument()
    expect(screen.getByTestId('attribution-trigger-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getByLabelText('选择场次')).toHaveTextContent('包装直播场次')
  })

  it('names selected session ids when compare summary loading fails', async () => {
    vi.mocked(request.post).mockImplementation((url: string, body?: unknown) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [
              { id: 18, liveTitle: '晚场直播', createTime: '2026-05-22 20:00:00' },
              { id: 19, liveTitle: '早场直播', createTime: '2026-05-23 08:00:00' },
            ],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary') {
        const sessionId = (body as { sessionId?: number } | undefined)?.sessionId
        return sessionId === 19
          ? Promise.reject(new Error('summary down'))
          : Promise.resolve({ data: { sessionId, totalGmv: 1200, totalSales: 8, productAttributions: 1, scriptAttributions: 1, overallScore: 88, status: 'completed' } } as never)
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '场次对比' }))
    fireEvent.click(await screen.findByText('晚场直播'))
    fireEvent.click(await screen.findByText('早场直播'))

    expect(await screen.findByText('场次对比加载失败')).toBeInTheDocument()
    expect(screen.getByTestId('compare-summary-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getByText(/selectedSessionIds=18,19/)).toBeInTheDocument()
    expect(screen.getByText(/已选场次仍保留/)).toBeInTheDocument()
  })

  it('uses theme-aware attribution report surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(request.post).mockImplementation((url: string, body?: unknown) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [
              { id: 18, liveTitle: '晚场直播', createTime: '2026-05-22 20:00:00' },
              { id: 19, liveTitle: '早场直播', createTime: '2026-05-23 08:00:00' },
            ],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary') {
        const sessionId = (body as { sessionId?: number } | undefined)?.sessionId ?? 18
        return Promise.resolve({
          data: {
            sessionId,
            totalGmv: sessionId === 18 ? 2400 : 1200,
            totalSales: sessionId === 18 ? 16 : 8,
            productAttributions: 1,
            scriptAttributions: 2,
            overallScore: sessionId === 18 ? 92 : 78,
            aiAnalysis: sessionId === 18 ? '暗色归因结论' : undefined,
            status: 'completed',
          },
        } as never)
      }
      if (url === '/ai/attribution/session') {
        return Promise.resolve({
          data: {
            rows: [
              { id: 1, sessionId: 18, attributionType: 'product_gmv', productName: '面膜', contributedGmv: 1200, contributedSales: 8, contributionRatio: 0.5, effectScore: 88, status: 1, createTime: '2026-05-22' },
              { id: 2, sessionId: 18, attributionType: 'script_sales', scriptContent: '高转化开场话术', contributedGmv: 900, contributedSales: 6, conversionRate: 0.12, contributionRatio: 0.38, effectScore: 92, status: 1, createTime: '2026-05-22' },
              { id: 3, sessionId: 18, attributionType: 'script_sales', scriptContent: '低效铺垫话术', contributedGmv: 300, contributedSales: 2, conversionRate: 0.04, contributionRatio: 0.12, effectScore: 52, status: 1, createTime: '2026-05-22' },
            ],
          },
        } as never)
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AttributionPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/晚场直播/))

    expect(await screen.findByTestId('attribution-ai-analysis-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(240, 247, 255)',
    })
    expect((await screen.findAllByTestId('session-script-attribution-surface'))[0]).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })

    fireEvent.click(screen.getByRole('tab', { name: '话术归因' }))
    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/晚场直播/))

    expect(await screen.findByTestId('script-attribution-top-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(232, 245, 233)',
    })
    expect(await screen.findByTestId('script-attribution-row-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
    expect(screen.getByTestId('script-local-summary-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(240, 247, 255)',
    })

    fireEvent.click(screen.getByRole('tab', { name: '场次对比' }))
    fireEvent.click(await screen.findByText('晚场直播'))
    fireEvent.click(await screen.findByText('早场直播'))

    expect(await screen.findByTestId('compare-local-diff-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(243, 244, 246)',
    })
    expect(screen.getAllByTestId('compare-best-metric-cell')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(241, 248, 233)',
    })
  })

  it('uses theme-aware attribution chart colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(request.post).mockImplementation((url: string) => {
      if (url === '/live/session/search') {
        return Promise.resolve({
          data: {
            records: [{ id: 18, liveTitle: '晚场直播', createTime: '2026-05-22 20:00:00' }],
          },
        } as never)
      }
      if (url === '/ai/attribution/summary') {
        return Promise.resolve({
          data: {
            sessionId: 18,
            totalGmv: 2400,
            totalSales: 16,
            productAttributions: 1,
            scriptAttributions: 2,
            overallScore: 92,
            aiAnalysis: '暗色归因结论',
            status: 'completed',
          },
        } as never)
      }
      if (url === '/ai/attribution/session') {
        return Promise.resolve({
          data: {
            rows: [
              { id: 1, sessionId: 18, attributionType: 'product_gmv', productName: '面膜', contributedGmv: 1200, contributedSales: 8, contributionRatio: 0.5, effectScore: 88, status: 1, createTime: '2026-05-22' },
              { id: 2, sessionId: 18, attributionType: 'script_sales', scriptContent: '高转化开场话术', contributedGmv: 900, contributedSales: 6, conversionRate: 0.12, contributionRatio: 0.38, effectScore: 92, status: 1, createTime: '2026-05-22' },
            ],
          },
        } as never)
      }
      return Promise.resolve([] as never)
    })

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AttributionPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/晚场直播/))

    const funnelChart = await screen.findByTestId('attribution-funnel-chart-surface')
    expect(funnelChart).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(funnelChart.textContent).not.toContain('#5470c6')
    expect(funnelChart.textContent).toContain('#e3f2fd')

    fireEvent.click(screen.getByRole('tab', { name: '话术归因' }))
    fireEvent.mouseDown(await screen.findByLabelText('选择场次'))
    fireEvent.click(await screen.findByText(/晚场直播/))

    const barChart = await screen.findByTestId('script-attribution-bar-chart-surface')
    expect(barChart).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(barChart.textContent).not.toContain('#5470c6')
    expect(barChart.textContent).toContain('#e3f2fd')
  })
})
