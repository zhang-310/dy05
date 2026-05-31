import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  analyzeProductScriptVersion,
  getProductScriptOptimizationSuggestions,
  regenerateProductScriptBySuggestion,
} from '../optimization'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('product script optimization API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uses real product script optimization endpoints', async () => {
    mockPost.mockResolvedValue({})

    await analyzeProductScriptVersion(21)
    await getProductScriptOptimizationSuggestions(21, 31, 5)
    await regenerateProductScriptBySuggestion(21, 41, ['FRIENDLY'])

    expect(mockPost).toHaveBeenCalledWith('/product/script/analyze', {
      scriptVersionId: 21,
      dataSource: 'PRODUCT_SCRIPT',
      analysisType: 'COMPREHENSIVE',
    })
    expect(mockPost).toHaveBeenCalledWith('/product/script/suggestions', {
      scriptVersionId: 21,
      analysisResultId: 31,
      topN: 5,
    })
    expect(mockPost).toHaveBeenCalledWith('/product/script/regenerate', {
      scriptVersionId: 21,
      suggestionId: 41,
      generationStyles: ['FRIENDLY'],
    })
  })
})
