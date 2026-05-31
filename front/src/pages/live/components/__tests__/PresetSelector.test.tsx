import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { PresetSelector } from '../PresetSelector'
import { liveApi } from '@/api/live'

vi.mock('@/api/live', () => ({
  liveApi: {
    presetList: vi.fn(),
    presetSave: vi.fn(),
    presetDelete: vi.fn(),
  },
}))

describe('PresetSelector', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(liveApi.presetList).mockResolvedValue([])
    vi.mocked(liveApi.presetSave).mockResolvedValue({ id: 2 } as never)
    vi.mocked(liveApi.presetDelete).mockResolvedValue(undefined as never)
  })

  it('renders selector contract without local preset fallback', async () => {
    const onApply = vi.fn()
    renderWithProviders(
      <PresetSelector
        currentStyle="professional"
        currentModelId={5}
        currentUseKbRef={true}
        onApplyPreset={onApply}
      />
    )

    const button = screen.getByTestId('generation-preset-open-button')
    expect(button).toHaveAttribute('data-contract-source', '/live/generation-preset/list')
    expect(button).toHaveAttribute('data-no-local-preset-fallback', 'true')
    fireEvent.click(button)

    expect(await screen.findByTestId('generation-preset-menu')).toHaveAttribute('data-contract-scope', 'live-generation-preset-selector')
    expect(screen.getByTestId('generation-preset-empty-state')).toHaveAttribute('data-no-local-preset-fallback', 'true')
    expect(liveApi.presetList).toHaveBeenCalled()
  })

  it('saves advanced generation options to the backend preset payload', async () => {
    renderWithProviders(
      <PresetSelector
        currentStyle="promotion"
        currentModelId={7}
        currentUseKbRef={false}
        currentIpType="phenomenal"
        currentMaterialType="joke"
        currentScriptModule="conversion_engine"
        currentRetentionStrategy="high_suspense"
        currentInteractionLevel="heavy"
        onApplyPreset={vi.fn()}
      />
    )

    fireEvent.click(screen.getByTestId('generation-preset-open-button'))
    fireEvent.click(await screen.findByTestId('generation-preset-save-open'))
    fireEvent.change(screen.getByTestId('generation-preset-name-input'), { target: { value: '大促生成预设' } })
    fireEvent.click(screen.getByTestId('generation-preset-save-submit'))

    await waitFor(() => {
      expect(liveApi.presetSave).toHaveBeenCalledWith({
        presetName: '大促生成预设',
        style: 'promotion',
        genStyle: 'promotion',
        modelId: 7,
        useKbRef: false,
        ipType: 'phenomenal',
        materialType: 'joke',
        scriptModule: 'conversion_engine',
        retentionStrategy: 'high_suspense',
        interactionLevel: 'heavy',
      })
    })
  })

  it('shows preset load and save failures without static fallback', async () => {
    vi.mocked(liveApi.presetList).mockRejectedValueOnce(new Error('preset list down'))

    renderWithProviders(
      <PresetSelector
        currentStyle="natural"
        currentModelId=""
        currentUseKbRef={true}
        onApplyPreset={vi.fn()}
      />
    )

    fireEvent.click(screen.getByTestId('generation-preset-open-button'))

    expect(await screen.findByTestId('generation-preset-load-error')).toHaveTextContent('/live/generation-preset/list 加载失败：preset list down')
    expect(screen.getByTestId('generation-preset-load-error')).toHaveAttribute('data-no-local-preset-fallback', 'true')

    vi.mocked(liveApi.presetSave).mockRejectedValueOnce(new Error('preset save down'))
    fireEvent.click(screen.getByTestId('generation-preset-save-open'))
    fireEvent.change(screen.getByTestId('generation-preset-name-input'), { target: { value: '保留输入' } })
    fireEvent.click(screen.getByTestId('generation-preset-save-submit'))

    expect(await screen.findByTestId('generation-preset-save-error')).toHaveTextContent('/live/generation-preset/save 保存失败：preset save down')
    expect(screen.getByTestId('generation-preset-save-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getByTestId('generation-preset-name-input')).toHaveValue('保留输入')
  })
})
