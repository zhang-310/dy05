import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import EffectivenessScorePage from '../EffectivenessScorePage'
import { productApi } from '@/api/product'

vi.mock('@/api/product', () => ({
  productApi: {
    effectivenessRanking: vi.fn(),
  },
}))

describe('EffectivenessScorePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(productApi.effectivenessRanking).mockResolvedValue({
      list: [
        {
          productId: 1,
          productName: '修护面膜',
          avgScore: 8.6,
          useCount: 12,
          conversionRate: 0.18,
          tag: '护肤',
          trend: 'up',
        },
      ],
      total: 1,
      summary: {
        dates: [],
        avgScores: [],
        avgScore: 8.6,
        maxScore: 9.4,
        scoredCount: 1,
        avgConversionRate: 0.18,
      },
    })
  })

  it('renders title and loads effectiveness ranking', async () => {
    renderWithProviders(
      <MemoryRouter>
        <EffectivenessScorePage />
      </MemoryRouter>,
    )

    expect(screen.getByText('商品效果评分')).toBeInTheDocument()

    await waitFor(() => {
      expect(productApi.effectivenessRanking).toHaveBeenCalledWith({ tag: '', page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('修护面膜')).toBeInTheDocument()
      expect(screen.getByText('护肤')).toBeInTheDocument()
    })
  })
})
