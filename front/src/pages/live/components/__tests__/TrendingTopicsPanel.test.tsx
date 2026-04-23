import { describe, it, expect, vi } from 'vitest'
import { renderWithProviders } from '@/test/utils'
import { TrendingTopicsPanel } from '../TrendingTopicsPanel'

vi.mock('@/api/brain', () => ({
  brainTrendsCurrent: vi.fn().mockResolvedValue([]),
}))

describe('TrendingTopicsPanel', () => {
  it('renders without crashing', () => {
    const onInject = vi.fn()
    const { container } = renderWithProviders(<TrendingTopicsPanel onInjectKeyword={onInject} />)
    expect(container).toBeInTheDocument()
  })

  it('renders with selected keywords', () => {
    const onInject = vi.fn()
    const { container } = renderWithProviders(
      <TrendingTopicsPanel onInjectKeyword={onInject} selectedKeywords={['测试关键词']} />
    )
    expect(container).toBeInTheDocument()
  })
})
