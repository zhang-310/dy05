import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ExperimentDetailPage from '../ExperimentDetailPage'
import { abtestApi } from '@/api/abtest'

vi.mock('@/api/abtest', () => ({
  abtestApi: {
    get: vi.fn(),
    result: vi.fn(),
    dailyTrend: vi.fn(),
    setWinner: vi.fn(),
    updateStatus: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/utils/echarts-registry', () => ({
  LazyECharts: () => <div data-testid="echarts">chart</div>,
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/admin/ai/abtest/7']}>
      <Routes>
        <Route path="/admin/ai/abtest/:id" element={<ExperimentDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ExperimentDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(abtestApi.get).mockResolvedValue({
      id: 7,
      name: '直播开场 A/B',
      experimentName: '直播开场 A/B',
      description: '测试两种开场风格',
      experimentType: 'script_style',
      targetEntityType: 'live_session',
      targetEntityId: 18,
      status: 1,
      winnerVariantId: null,
      startTime: '2026-05-20T10:00:00',
      endTime: '',
      createTime: '2026-05-20T09:00:00',
      trafficSplit: 0,
      variants: {
        records: [
          { id: 1, experimentId: 7, variantName: 'A', variantType: 'A', conversionRate: 0.04, exposures: 1000, conversions: 40, trafficRatio: 50, scriptStyle: 'warm', isWinner: false },
          { id: 2, experimentId: 7, variantName: 'B', variantType: 'B', conversionRate: 0.08, exposures: 1000, conversions: 80, trafficRatio: 50, scriptStyle: 'direct', isWinner: false },
        ],
      },
    } as never)
    vi.mocked(abtestApi.result).mockResolvedValue({
      experimentId: 7,
      experimentName: '直播开场 A/B',
      variantStats: {
        items: [
          { id: 1, experimentId: 7, variantName: 'A', variantType: 'A', conversionRate: 0.04, exposures: 1000, conversions: 40, trafficRatio: 50, scriptStyle: 'warm', isWinner: false },
          { id: 2, experimentId: 7, variantName: 'B', variantType: 'B', conversionRate: 0.08, exposures: 1000, conversions: 80, trafficRatio: 50, scriptStyle: 'direct', isWinner: false },
        ],
      },
      winnerVariantId: null,
      totalExposures: 2000,
      totalConversions: 120,
      overallConversionRate: 0.06,
    } as never)
    vi.mocked(abtestApi.dailyTrend).mockResolvedValue({
      records: [
        { date: '2026-05-20', variantAConversionRate: 4, variantBConversionRate: 8 },
      ],
    } as never)
    vi.mocked(abtestApi.setWinner).mockResolvedValue(undefined)
    vi.mocked(abtestApi.updateStatus).mockResolvedValue(undefined)
  })

  it('renders wrapped experiment fields and disables unsupported segment analysis', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: '直播开场 A/B', level: 1 })).toBeInTheDocument()
    const root = screen.getByTestId('abtest-experiment-detail-page')
    expect(root).toHaveAttribute('data-ready-endpoints', '/abtest/experiment/get|/abtest/experiment/result|/abtest/experiment/daily-trend|/abtest/experiment/update-status|/abtest/experiment/set-winner')
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/abtest/experiment/segment-analysis'))
    expect(root).toHaveAttribute('data-segment-analysis-supported', 'false')
    expect(root).toHaveAttribute('data-no-segment-analysis-call', 'true')
    expect(screen.getByTestId('abtest-detail-statistical-panel')).toHaveAttribute('data-derived-from-endpoints', '/abtest/experiment/get|/abtest/experiment/result')
    expect(screen.getByText('script_style')).toBeInTheDocument()
    expect(screen.getByText('live_session #18')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '分群分析' }))
    const panel = await screen.findByTestId('abtest-segment-analysis-panel')
    const downgrade = screen.getByTestId('abtest-segment-analysis-downgrade')
    const dimensionInput = screen.getByTestId('abtest-segment-dimension-input')

    expect(panel).toHaveAttribute('data-contract-status', 'unsupported')
    expect(panel).toHaveAttribute('data-contract-endpoint', '/abtest/experiment/segment-analysis')
    expect(panel).toHaveAttribute('data-no-segment-analysis-call', 'true')
    expect(downgrade).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(downgrade).toHaveAttribute('data-contract-endpoint', '/abtest/experiment/segment-analysis')
    expect(downgrade).toHaveAttribute('data-no-segment-analysis-call', 'true')
    expect(downgrade).toHaveTextContent('后端当前没有 `/abtest/experiment/segment-analysis` 接口')
    expect(dimensionInput).toHaveAttribute('data-contract-status', 'planning-only')
    expect(screen.getAllByTestId('abtest-segment-contract-card')).toHaveLength(3)
    expect(abtestApi.segmentAnalysis).toBeUndefined()
  })

  it('uses backend status mapping and winner payload', async () => {
    renderPage()

    await screen.findByRole('heading', { name: '直播开场 A/B', level: 1 })
    fireEvent.click(screen.getByRole('button', { name: '暂停' }))

    await waitFor(() => expect(abtestApi.updateStatus).toHaveBeenCalledWith(7, 3))

    fireEvent.click(screen.getAllByRole('button', { name: '设为获胜者' })[1])
    await waitFor(() => {
      expect(abtestApi.setWinner).toHaveBeenCalledWith(7, 2, '前端根据统计结果设置获胜变体')
    })
  })

  it('confirms statistically best variant through real set-winner action', async () => {
    renderPage()

    await screen.findByRole('heading', { name: '直播开场 A/B', level: 1 })
    fireEvent.click(screen.getByRole('button', { name: '确认获胜变体' }))

    expect(await screen.findByRole('dialog', { name: '确认获胜变体并结束实验' })).toBeInTheDocument()
    expect(screen.getByText(/会设置获胜变体并结束实验/)).toBeInTheDocument()
    expect(screen.getByText(/不会自动替换直播场次或短视频项目中的话术/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '确认获胜' }))

    await waitFor(() => {
      expect(abtestApi.setWinner).toHaveBeenCalledWith(7, 2, '前端根据统计结果设置获胜变体')
    })
  })

  it('shows source endpoint when status update or winner action fails', async () => {
    vi.mocked(abtestApi.updateStatus).mockRejectedValue(new Error('status down') as never)
    vi.mocked(abtestApi.setWinner).mockRejectedValue(new Error('winner down') as never)

    renderPage()

    await screen.findByRole('heading', { name: '直播开场 A/B', level: 1 })
    fireEvent.click(screen.getByRole('button', { name: '暂停' }))

    expect(await screen.findByText(/状态更新失败（POST \/abtest\/experiment\/update-status）：status down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-detail-action-error')).toHaveAttribute('data-no-local-abtest-mutation', 'true')
    expect(screen.getByText(/targetStatus=3\/已暂停; route=\/admin\/ai\/abtest\/7; experimentId=7; experimentName=直播开场 A\/B/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '确认获胜变体' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认获胜' }))

    expect((await screen.findAllByText(/设置获胜变体失败（POST \/abtest\/experiment\/set-winner）：winner down/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('abtest-detail-winner-dialog-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getAllByText(/variantId=2; variantName=B/).length).toBeGreaterThan(0)
    expect(screen.getByRole('dialog', { name: '确认获胜变体并结束实验' })).toBeInTheDocument()
  })

  it('shows detail endpoint and retry when experiment detail loading fails', async () => {
    vi.mocked(abtestApi.get).mockRejectedValueOnce(new Error('detail down') as never)

    renderPage()

    expect(await screen.findByText(/实验详情加载失败（POST \/abtest\/experiment\/get）：detail down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-detail-load-error')).toHaveAttribute('data-no-local-abtest-result', 'true')
    expect(screen.getByText(/route=\/admin\/ai\/abtest\/7; experimentId=7/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '重试' }))

    await waitFor(() => {
      expect(abtestApi.get).toHaveBeenCalledTimes(2)
    })
  })

  it('shows result and trend endpoints when partial analytics fail', async () => {
    vi.mocked(abtestApi.result).mockRejectedValueOnce(new Error('result down') as never)
    vi.mocked(abtestApi.dailyTrend).mockRejectedValueOnce(new Error('trend down') as never)

    renderPage()

    expect(await screen.findByRole('heading', { name: '直播开场 A/B', level: 1 })).toBeInTheDocument()
    expect(await screen.findByText(/统计结果加载失败（POST \/abtest\/experiment\/result）：result down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-detail-result-error')).toHaveAttribute('data-no-static-abtest-result', 'true')
    expect(screen.getByText(/route=\/admin\/ai\/abtest\/7; experimentId=7; experimentName=直播开场 A\/B/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '每日趋势' }))
    expect(await screen.findByText(/趋势加载失败（POST \/abtest\/experiment\/daily-trend）：trend down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-detail-trend-error')).toHaveAttribute('data-no-local-trend-fallback', 'true')
    expect(screen.getAllByText(/tab=1; segmentDimension=device/).length).toBeGreaterThan(0)
  })
})
