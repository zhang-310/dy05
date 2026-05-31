import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import BenchmarkScriptRecommendationPage from '../BenchmarkScriptRecommendationPage'
import { benchmarkScriptRecommendationApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  benchmarkScriptRecommendationApi: {
    getPopularScripts: vi.fn(),
    getLatestQualityScripts: vi.fn(),
    recommendByRequirement: vi.fn(),
    smartRecommend: vi.fn(),
  },
}))

vi.mock('notistack', () => ({
  useSnackbar: () => ({ enqueueSnackbar: vi.fn() }),
}))

describe('BenchmarkScriptRecommendationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(benchmarkScriptRecommendationApi.getPopularScripts).mockResolvedValue([
      {
        scriptId: 1,
        videoId: 11,
        scriptContent: '热门护肤脚本',
        qualityScore: 91,
        similarityScore: 0.88,
        industry: '护肤',
      },
    ] as never)
    vi.mocked(benchmarkScriptRecommendationApi.getLatestQualityScripts).mockResolvedValue([
      {
        scriptId: 2,
        videoId: 12,
        scriptContent: '最新高质量脚本',
        qualityScore: 86,
        industry: '彩妆',
      },
    ] as never)
    vi.mocked(benchmarkScriptRecommendationApi.recommendByRequirement).mockResolvedValue([] as never)
    vi.mocked(benchmarkScriptRecommendationApi.smartRecommend).mockResolvedValue([] as never)
  })

  it('loads recommendation inventory and explains vector readiness', async () => {
    renderWithProviders(
      <MemoryRouter>
        <BenchmarkScriptRecommendationPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '脚本推荐引擎' })).toBeInTheDocument()
    const page = screen.getByTestId('benchmark-script-recommendation-page')
    expect(page).toHaveAttribute('data-contract-scope', 'benchmark-script-vector-recommendation')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/script-recommendation/recommend-by-requirement'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/script-recommendation/get-popular-scripts'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/benchmark/script-recommendation/local-list'))
    expect(page).toHaveAttribute('data-no-local-recommendation-fallback', 'true')

    await waitFor(() => {
      expect(benchmarkScriptRecommendationApi.getPopularScripts).toHaveBeenCalledWith(10)
      expect(benchmarkScriptRecommendationApi.getLatestQualityScripts).toHaveBeenCalledWith(10, 70)
    })

    expect(await screen.findByText(/当前可直接展示的推荐样本 2 条/)).toBeInTheDocument()
    expect(screen.getByText('描述您的需求')).toBeInTheDocument()
  })

  it('shows endpoint context when inventory loading fails', async () => {
    vi.mocked(benchmarkScriptRecommendationApi.getPopularScripts).mockRejectedValue(new Error('popular down'))
    vi.mocked(benchmarkScriptRecommendationApi.getLatestQualityScripts).mockRejectedValue(new Error('latest down'))

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkScriptRecommendationPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/popular down/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-recommendation-inventory-error')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.getByText(/endpoint=\/benchmark\/script-recommendation\/get-popular-scripts; topK=10/)).toBeInTheDocument()
    expect(screen.getByText(/latest down/)).toBeInTheDocument()
    expect(screen.getByText(/endpoint=\/benchmark\/script-recommendation\/get-latest-quality-scripts; topK=10; minQualityScore=70/)).toBeInTheDocument()
  })

  it('keeps requirement input and shows endpoint context when requirement recommendation fails', async () => {
    vi.mocked(benchmarkScriptRecommendationApi.recommendByRequirement).mockRejectedValue(new Error('requirement down'))

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkScriptRecommendationPage />
      </MemoryRouter>,
    )

    const textbox = await screen.findByPlaceholderText(/我需要一个护肤品直播脚本/)
    fireEvent.change(textbox, { target: { value: '提高护肤品直播转化' } })
    fireEvent.click(screen.getByRole('button', { name: '搜索推荐' }))

    expect(await screen.findByText(/requirement down/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-requirement-panel-surface')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('benchmark-requirement-error')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.getByText(/endpoint=\/benchmark\/script-recommendation\/recommend-by-requirement; requirementLength=9; topK=10/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('提高护肤品直播转化')).toBeInTheDocument()
  })

  it('keeps smart filters and shows endpoint context when smart recommendation fails', async () => {
    vi.mocked(benchmarkScriptRecommendationApi.smartRecommend).mockRejectedValue(new Error('smart down'))

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkScriptRecommendationPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('tab', { name: /智能推荐/ }))
    const panel = screen.getByRole('tabpanel', { hidden: false })
    fireEvent.mouseDown(within(panel).getByLabelText('行业'))
    fireEvent.click(await screen.findByRole('option', { name: '护肤' }))
    fireEvent.change(within(panel).getByLabelText('推荐数量'), { target: { value: '6' } })
    fireEvent.change(within(panel).getByPlaceholderText(/参考文本/), { target: { value: '爆款开场' } })
    fireEvent.click(within(panel).getByRole('button', { name: '智能推荐' }))

    expect(await screen.findByText(/smart down/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-smart-panel-surface')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('benchmark-smart-error')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.getByText(/endpoint=\/benchmark\/script-recommendation\/smart-recommend; industry=护肤; sceneType=全部; scriptType=全部; minQualityScore=70; referenceTextLength=4; topK=6/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('爆款开场')).toBeInTheDocument()
  })

  it('uses theme-aware recommendation panels in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <BenchmarkScriptRecommendationPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByTestId('benchmark-requirement-panel-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(250, 250, 250)',
    })

    fireEvent.click(screen.getByRole('tab', { name: /智能推荐/ }))
    expect(await screen.findByTestId('benchmark-smart-panel-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(250, 250, 250)',
    })
  })
})
