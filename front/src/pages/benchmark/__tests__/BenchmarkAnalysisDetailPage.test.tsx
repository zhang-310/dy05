import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import BenchmarkAnalysisDetailPage from '../BenchmarkAnalysisDetailPage'
import { benchmarkAnalysisApi, benchmarkVideoApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  benchmarkVideoApi: {
    get: vi.fn(),
  },
  benchmarkAnalysisApi: {
    getByVideo: vi.fn(),
  },
}))

describe('BenchmarkAnalysisDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(benchmarkVideoApi.get).mockResolvedValue({
      id: 11,
      title: '护肤爆款视频',
      description: '产品测评',
      likeCount: 24000,
      commentCount: 800,
      shareCount: 120,
      favoriteCount: 90,
      viewCount: 88000,
      isQualified: true,
      bosVideoUrl: 'https://bos.test/v11.mp4',
      analysisStatus: 'completed',
    } as never)
    vi.mocked(benchmarkAnalysisApi.getByVideo).mockResolvedValue({
      id: 1,
      benchmarkVideoId: 11,
      transcriptText: '开头提出敏感肌痛点',
      ocrText: '屏障修护',
      mergedContent: '敏感肌修护方案',
      keyFramesJson: '[{"framePath":"https://cdn.test/f1.jpg","startTime":1.2}]',
      hookStrategy: '痛点切入',
      contentStructure: '问题-方案-行动',
      viralFactors: '强痛点',
      strengths: '节奏清晰',
      aiSummary: '适合复刻',
    } as never)
  })

  it('loads video analysis and renders source diagnostics', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/analysis/11']}>
        <Routes>
          <Route path="/admin/shortvideo/benchmark/analysis/:videoId" element={<BenchmarkAnalysisDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '视频深度分析' })).toBeInTheDocument()
    const page = screen.getByTestId('benchmark-analysis-detail-page')
    expect(page).toHaveAttribute('data-contract-scope', 'benchmark-video-analysis-readonly')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/video/get'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/analysis/get-by-video'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/benchmark/analysis/static-detail'))
    expect(page).toHaveAttribute('data-no-static-analysis-fallback', 'true')

    await waitFor(() => {
      expect(benchmarkVideoApi.get).toHaveBeenCalledWith(11)
      expect(benchmarkAnalysisApi.getByVideo).toHaveBeenCalledWith(11)
    })

    expect(await screen.findByText('文本来源')).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-analysis-summary-contract')).toHaveAttribute('data-no-static-analysis-fallback', 'true')
    expect(screen.getByTestId('benchmark-analysis-result-contract')).toHaveAttribute('data-contract-scope', 'benchmark-analysis-readonly-tabs')
    expect(screen.getByText('关键帧')).toBeInTheDocument()
    expect(screen.getByText('敏感肌修护方案')).toBeInTheDocument()
    expect(screen.getByText('BOS 已上传')).toBeInTheDocument()
  })

  it('renders downgraded analysis issues without mock content', async () => {
    vi.mocked(benchmarkAnalysisApi.getByVideo).mockResolvedValueOnce({
      id: 2,
      benchmarkVideoId: 11,
      transcriptText: '[ASR 识别失败: ffmpeg missing]',
      ocrText: '',
      mergedContent: '',
      sceneDescription: '[场景分析失败: ffprobe missing]',
      aiSummary: '[AI 分析异常: provider down]',
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/analysis/11']}>
        <Routes>
          <Route path="/admin/shortvideo/benchmark/analysis/:videoId" element={<BenchmarkAnalysisDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/分析链路存在降级/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-analysis-downgrade')).toHaveAttribute('data-no-static-analysis-fallback', 'true')
    expect(screen.getAllByText(/ASR 识别失败/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/AI 分析异常/).length).toBeGreaterThan(0)
  })

  it('renders key frame aliases from normalized backend payloads', async () => {
    vi.mocked(benchmarkAnalysisApi.getByVideo).mockResolvedValueOnce({
      id: 3,
      benchmarkVideoId: 11,
      sceneDescription: '两段式展示',
      sceneCount: 2,
      keyFramesJson: JSON.stringify([
        { url: 'https://cdn.test/frame-alias-a.jpg', time: 1.25 },
        { frameUrl: 'https://cdn.test/frame-alias-b.jpg', timestamp: '3.5' },
      ]),
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/analysis/11']}>
        <Routes>
          <Route path="/admin/shortvideo/benchmark/analysis/:videoId" element={<BenchmarkAnalysisDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await screen.findByText('关键帧')
    fireEvent.click(screen.getByRole('tab', { name: '场景分析' }))
    expect(await screen.findByText('两段式展示')).toBeInTheDocument()
    expect(screen.getByAltText('关键帧 1')).toHaveAttribute('src', 'https://cdn.test/frame-alias-a.jpg')
    expect(screen.getByAltText('关键帧 2')).toHaveAttribute('src', 'https://cdn.test/frame-alias-b.jpg')
    expect(screen.getByText('1.25s')).toBeInTheDocument()
    expect(screen.getByText('3.50s')).toBeInTheDocument()
  })
})
