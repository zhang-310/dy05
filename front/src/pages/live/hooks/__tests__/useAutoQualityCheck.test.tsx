import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { useAutoQualityCheck } from '../useAutoQualityCheck'
import type { LiveScript } from '@/api/live'
import { GenerationContext, type GenerationContextValue } from '../../contexts/GenerationContext'

const mockToast = vi.fn()
const mockAiCheckViolation = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => mockToast,
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    aiCheckViolation: (...args: unknown[]) => mockAiCheckViolation(...args),
  },
}))

function makeScript(id: number, scriptContent: string): LiveScript {
  return {
    id,
    sessionId: 1,
    scriptTitle: `script-${id}`,
    scriptType: 'product',
    scriptContent,
  } as LiveScript
}

function createWrapper(genJustCompleted: boolean) {
  const value: GenerationContextValue = {
    isGenerating: false,
    generationProgress: 100,
    generationMessage: '',
    genJustCompleted,
    slotTimeline: [],
    startGeneration: vi.fn().mockResolvedValue(undefined),
    cancelGeneration: vi.fn(),
  }

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <GenerationContext.Provider value={value}>{children}</GenerationContext.Provider>
  }
}

describe('useAutoQualityCheck', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('runs violation checks and reports success after generation completes', async () => {
    mockAiCheckViolation.mockResolvedValue({})

    renderHook(() => useAutoQualityCheck([
      makeScript(1, '话术一'),
      makeScript(2, '话术二'),
      makeScript(3, '   '),
    ]), { wrapper: createWrapper(true) })

    await waitFor(() => expect(mockAiCheckViolation).toHaveBeenCalledTimes(2))
    expect(mockAiCheckViolation).toHaveBeenCalledWith({ scriptId: 1 })
    expect(mockAiCheckViolation).toHaveBeenCalledWith({ scriptId: 2 })
    expect(mockToast).toHaveBeenCalledWith('生成完成，正在自动检测 2 条话术违规...', 'info')

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith('自动违规检测完成', 'success')
    })
  })

  it('does nothing when generation is not completed', () => {
    renderHook(() => useAutoQualityCheck([makeScript(1, '话术一')]), { wrapper: createWrapper(false) })

    expect(mockAiCheckViolation).not.toHaveBeenCalled()
    expect(mockToast).not.toHaveBeenCalled()
  })
})
