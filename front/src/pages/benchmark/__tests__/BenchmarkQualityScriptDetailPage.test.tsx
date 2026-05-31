import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import BenchmarkQualityScriptDetailPage from '../BenchmarkQualityScriptDetailPage'
import { benchmarkQualityScriptApi, benchmarkScriptSimilarityApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  benchmarkQualityScriptApi: {
    get: vi.fn(),
  },
  benchmarkScriptSimilarityApi: {
    findSimilar: vi.fn(),
  },
}))

describe('BenchmarkQualityScriptDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(benchmarkQualityScriptApi.get).mockResolvedValue({
      id: 1,
      scriptContent: '护肤品直播脚本正文',
      qualityScore: 91.5,
      scriptType: '产品介绍',
      industry: '护肤',
      sceneType: '直播',
      videoDuration: 60,
      viewsCount: 120000,
      likesCount: 8000,
      commentsCount: 500,
      sharesCount: 200,
      engagementRate: 12.5,
      viralScore: 88.2,
      completionRate: 76.3,
      aiRating: 8.9,
      hookStrategy: '痛点切入',
      contentStructure: '三段式',
    } as never)
    vi.mocked(benchmarkScriptSimilarityApi.findSimilar).mockResolvedValue([] as never)
  })

  it('loads script detail and renders key sections', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/quality-scripts/1']}>
        <Routes>
          <Route path="/admin/shortvideo/benchmark/quality-scripts/:id" element={<BenchmarkQualityScriptDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(benchmarkQualityScriptApi.get).toHaveBeenCalledWith(1)
      expect(benchmarkScriptSimilarityApi.findSimilar).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('质量脚本详情')).toBeInTheDocument()
      expect(screen.getByText('脚本内容')).toBeInTheDocument()
      expect(screen.getByText('护肤品直播脚本正文')).toBeInTheDocument()
      expect(screen.getByText('痛点切入')).toBeInTheDocument()
    })
    expect(screen.getByTestId('benchmark-quality-script-detail-workbench')).toHaveAttribute('data-contract-scope', 'benchmark-quality-script-detail-readonly')
    expect(screen.getByTestId('benchmark-quality-script-detail-workbench')).toHaveAttribute('data-no-auto-vector-generation', 'true')
    expect(screen.getByTestId('benchmark-quality-script-detail-workbench').getAttribute('data-unsupported-endpoints')).toContain('/benchmark/script-similarity/generate-embedding')
    expect(screen.getByTestId('benchmark-quality-script-detail-source-contract')).toHaveAttribute('data-no-local-similar-script-fallback', 'true')
    expect(screen.getByTestId('benchmark-quality-script-detail-contract')).toHaveAttribute('data-no-detail-page-script-mutation', 'true')
    expect(screen.getByTestId('benchmark-quality-script-similar-empty')).toHaveAttribute('data-no-local-similar-script-fallback', 'true')
  })

  it('shows endpoint context when detail and similar requests fail', async () => {
    vi.mocked(benchmarkQualityScriptApi.get).mockRejectedValue(new Error('detail down'))
    vi.mocked(benchmarkScriptSimilarityApi.findSimilar).mockRejectedValue(new Error('similar down'))

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/quality-scripts/9']}>
        <Routes>
          <Route path="/admin/shortvideo/benchmark/quality-scripts/:id" element={<BenchmarkQualityScriptDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/detail down/)).toBeInTheDocument()
    expect(screen.getByText(/endpoint=\/benchmark\/quality-script\/get; scriptId=9/)).toBeInTheDocument()
    expect(screen.getByText(/similar down/)).toBeInTheDocument()
    expect(screen.getByText(/endpoint=\/benchmark\/script-similarity\/find-similar; scriptId=9; topK=10; minScore=0.7/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-quality-script-detail-error')).toHaveAttribute('data-no-local-detail-fallback', 'true')
    expect(screen.getByTestId('benchmark-quality-script-similar-error')).toHaveAttribute('data-no-auto-vector-generation', 'true')
    expect(screen.getByTestId('benchmark-quality-script-similar-error')).toHaveAttribute('data-no-local-similar-script-fallback', 'true')
  })

  it('renders backend similar scripts and navigates without local recommendation fallback', async () => {
    vi.mocked(benchmarkScriptSimilarityApi.findSimilar).mockResolvedValue([
      {
        scriptId: 2,
        videoId: 9,
        scriptContent: '相似脚本',
        qualityScore: 88,
        similarityScore: 0.82,
        industry: '护肤',
      },
    ] as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/quality-scripts/1']}>
        <Routes>
          <Route path="/admin/shortvideo/benchmark/quality-scripts/:id" element={<BenchmarkQualityScriptDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('benchmark-quality-script-similar-item')).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-quality-script-similar-contract')).toHaveAttribute('data-similar-count', '1')
    expect(screen.getByTestId('benchmark-quality-script-similar-contract')).toHaveAttribute('data-no-auto-milvus-indexing', 'true')
    expect(screen.getByTestId('benchmark-quality-script-similar-item')).toHaveAttribute('data-similarity-score', '0.82')
    fireEvent.click(screen.getByTestId('benchmark-quality-script-similar-item'))
    await waitFor(() => {
      expect(benchmarkQualityScriptApi.get).toHaveBeenCalledWith(2)
    })
  })

  it('uses a theme-aware script content surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/quality-scripts/1']}>
          <Routes>
            <Route path="/admin/shortvideo/benchmark/quality-scripts/:id" element={<BenchmarkQualityScriptDetailPage />} />
          </Routes>
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByTestId('benchmark-quality-script-content-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(250, 250, 250)',
    })
  })
})
