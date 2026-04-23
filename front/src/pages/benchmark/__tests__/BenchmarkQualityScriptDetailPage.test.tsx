import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
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
      <MemoryRouter initialEntries={['/benchmark/quality-script/1']}>
        <Routes>
          <Route path="/benchmark/quality-script/:id" element={<BenchmarkQualityScriptDetailPage />} />
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
  })
})
