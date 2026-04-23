import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  benchmarkQualityScriptApi,
  benchmarkScriptSimilarityApi,
  benchmarkScriptRecommendationApi,
} from '../benchmark'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('benchmark API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('quality script search posts paging payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await benchmarkQualityScriptApi.search({ page: 0, rows: 20, keyword: '护肤' } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/quality-script/search', {
      page: 0,
      rows: 20,
      keyword: '护肤',
    })
  })

  it('generateEmbedding posts script id payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await benchmarkScriptSimilarityApi.generateEmbedding(5)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-similarity/generate-embedding', { scriptId: 5 })
  })

  it('findSimilarByText posts similarity query payload', async () => {
    mockPost.mockResolvedValue([])
    await benchmarkScriptSimilarityApi.findSimilarByText({
      text: '护肤品直播话术',
      topK: 10,
      minScore: 0.7,
    } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-similarity/find-similar-by-text', {
      text: '护肤品直播话术',
      topK: 10,
      minScore: 0.7,
    })
  })

  it('recommendByRequirement posts requirement payload', async () => {
    mockPost.mockResolvedValue([])
    await benchmarkScriptRecommendationApi.recommendByRequirement({
      requirement: '提高护肤品直播转化',
      topK: 5,
    } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-recommendation/recommend-by-requirement', {
      requirement: '提高护肤品直播转化',
      topK: 5,
    })
  })

  it('recommendImprovementScripts posts analysis id payload', async () => {
    mockPost.mockResolvedValue([])
    await benchmarkScriptRecommendationApi.recommendImprovementScripts({
      analysisId: 99,
      topK: 3,
    } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-recommendation/recommend-improvement-scripts', {
      analysisId: 99,
      topK: 3,
    })
  })
})
