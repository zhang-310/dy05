import { describe, it, expect, vi } from 'vitest'
import { renderWithProviders } from '@/test/utils'
import { ShortVideoInspirationPanel } from '../ShortVideoInspirationPanel'

vi.mock('@/api/shortvideo', () => ({
  getRecommendedVirals: vi.fn().mockResolvedValue([]),
}))

describe('ShortVideoInspirationPanel', () => {
  it('renders without crashing', () => {
    const { container } = renderWithProviders(<ShortVideoInspirationPanel />)
    expect(container).toBeInTheDocument()
  })

  it('renders with onAdaptHook callback', () => {
    const onAdapt = vi.fn()
    const { container } = renderWithProviders(<ShortVideoInspirationPanel onAdaptHook={onAdapt} />)
    expect(container).toBeInTheDocument()
  })
})
