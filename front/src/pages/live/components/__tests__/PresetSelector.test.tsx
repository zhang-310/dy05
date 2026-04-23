import { describe, it, expect, vi } from 'vitest'
import { renderWithProviders } from '@/test/utils'
import { PresetSelector } from '../PresetSelector'

vi.mock('@/api/live', () => ({
  listGenerationPresets: vi.fn().mockResolvedValue([]),
  saveGenerationPreset: vi.fn().mockResolvedValue({}),
  deleteGenerationPreset: vi.fn().mockResolvedValue({}),
}))

describe('PresetSelector', () => {
  it('renders without crashing', () => {
    const onApply = vi.fn()
    const { container } = renderWithProviders(
      <PresetSelector
        currentStyle="professional"
        currentModelId={5}
        currentUseKbRef={true}
        onApplyPreset={onApply}
      />
    )
    expect(container).toBeInTheDocument()
  })
})
