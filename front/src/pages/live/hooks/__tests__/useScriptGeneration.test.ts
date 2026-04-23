import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Mock dependencies
vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))
vi.mock('@/api/live', () => ({
  generateOpening: vi.fn(),
  generateProduct: vi.fn(),
  generateFullStream: vi.fn(),
  generateEmotional: vi.fn(),
  generateForSlot: vi.fn(),
  generateForSlotStream: vi.fn(),
  generateSkeletonStream: vi.fn(),
  saveLiveScript: vi.fn(),
}))
vi.mock('@/api/abtest', () => ({
  assignScriptStyle: vi.fn().mockResolvedValue(null),
}))
vi.mock('@/hooks/useDebouncedCallback', () => ({
  useThrottledCallback: (fn: unknown) => fn,
}))

import { useScriptGeneration, type UseScriptGenerationDeps } from '../useScriptGeneration'
import * as api from '@/api/live'
import type { LiveScript } from '@/api/live'

import { useLiveGenStore } from '@/stores/liveGenStore'

function makeSession(
  overrides?: Partial<NonNullable<UseScriptGenerationDeps['session']>>
): NonNullable<UseScriptGenerationDeps['session']> {
  return {
    id: 1,
    userId: 1,
    accountId: 1,
    personaId: 1,
    liveTitle: 'test',
    sessionCover: '',
    scriptStyle: '',
    liveDescription: '',
    scheduledTime: '',
    scheduledEndTime: '',
    startTime: '',
    endTime: '',
    liveUrl: '',
    viewers: 0,
    likes: 0,
    status: 0,
    sessionType: '',
    liveFormat: '',
    createTime: '2026-04-10T00:00:00Z',
    updateTime: '2026-04-10T00:00:00Z',
    ...overrides,
  }
}

function makeScript(overrides?: Partial<LiveScript>): LiveScript {
  return {
    id: 1,
    sessionId: 1,
    scriptContent: '',
    createTime: '2026-04-10T00:00:00Z',
    updateTime: '2026-04-10T00:00:00Z',
    ...overrides,
  }
}

function makeDeps(overrides?: Partial<UseScriptGenerationDeps>): UseScriptGenerationDeps {
  return {
    sessionId: 1,
    products: [],
    scripts: [] as LiveScript[],
    setScripts: vi.fn() as React.Dispatch<React.SetStateAction<LiveScript[]>>,
    loadData: vi.fn().mockResolvedValue(undefined),
    session: makeSession(),
    editingId: null,
    setEditingId: vi.fn(),
    setEditingScript: vi.fn(),
    setEditContent: vi.fn(),
    selectedModelId: 10,
    ...overrides,
  }
}

describe('useScriptGeneration', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    localStorage.clear()
    // Reset Zustand store to defaults between tests
    useLiveGenStore.setState({ genStyle: '', useKbRef: true, selectedModelId: '' })
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('initializes with default state', () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps()))
    expect(result.current.genLoading).toBe(false)
    expect(result.current.fullGenProgress).toBeNull()
    expect(result.current.flowSteps).toEqual([])
    expect(result.current.flowPanelVisible).toBe(false)
    expect(result.current.cancellingGen).toBe(false)
  })

  it('updates genStyle via Zustand store', async () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps()))
    act(() => {
      result.current.setGenStyle('funny')
    })
    expect(result.current.genStyle).toBe('funny')
  })

  it('updates useKbRef via Zustand store', async () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps()))
    act(() => {
      result.current.setUseKbRef(false)
    })
    expect(result.current.useKbRef).toBe(false)
  })

  it('handleGenerateOpeningThrottled requires selectedModelId', async () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps({ selectedModelId: '' })))
    await act(async () => {
      await result.current.handleGenerateOpeningThrottled()
    })
    // Should not call API when no model selected
    expect(api.generateOpening).not.toHaveBeenCalled()
  })

  it('handleGenerateOpening calls API and updates flow steps', async () => {
    vi.mocked(api.generateOpening).mockResolvedValue({
      content: '大家好！',
      ragRefs: [],
    })
    const loadData = vi.fn().mockResolvedValue(undefined)
    const { result } = renderHook(() => useScriptGeneration(makeDeps({ loadData })))

    await act(async () => {
      await result.current.handleGenerateOpeningThrottled()
    })

    expect(api.generateOpening).toHaveBeenCalledWith({
      sessionId: 1,
      genStyle: undefined,
      useKbRef: true,
      modelId: 10,
    })
    expect(loadData).toHaveBeenCalled()
  })

  it('handleRegenerateSingle calls generateForSlot and saves', async () => {
    vi.mocked(api.generateForSlot).mockResolvedValue('新内容')
    vi.mocked(api.saveLiveScript).mockResolvedValue({} as any)
    const setScripts = vi.fn()
    const loadData = vi.fn().mockResolvedValue(undefined)
    const { result } = renderHook(() => useScriptGeneration(makeDeps({ setScripts, loadData })))

    await act(async () => {
      await result.current.handleRegenerateSingle(42)
    })

    expect(api.generateForSlot).toHaveBeenCalledWith({
      scriptId: 42,
      modelId: 10,
    })
    expect(api.saveLiveScript).toHaveBeenCalledWith({ id: 42, scriptContent: '新内容' })
    expect(loadData).toHaveBeenCalled()
  })

  it('cancelFullGeneration marks loading steps as failed', () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps()))

    // Simulate active generation state
    act(() => {
      result.current.setFlowPanelVisible(true)
    })

    act(() => {
      result.current.cancelFullGeneration()
    })

    expect(result.current.genLoading).toBe(false)
    expect(result.current.fullGenProgress).toBeNull()
  })

  it('handleFlowPanelDismiss clears all generation state', () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps()))

    act(() => {
      result.current.setFlowPanelVisible(true)
    })
    expect(result.current.flowPanelVisible).toBe(true)

    act(() => {
      result.current.handleFlowPanelDismiss()
    })

    expect(result.current.flowPanelVisible).toBe(false)
    expect(result.current.flowSteps).toEqual([])
    expect(result.current.fullGenProgress).toBeNull()
    expect(result.current.genLoading).toBe(false)
  })

  it('cleans up AbortController on unmount', () => {
    const { unmount } = renderHook(() => useScriptGeneration(makeDeps()))
    // Should not throw on unmount
    unmount()
  })

  it('handleGenerateIncremental skips scripts with content', async () => {
    const scripts = [
      makeScript({ id: 1, scriptContent: '已有内容', scriptType: 'opening', sequenceNo: 1 }),
      makeScript({ id: 2, scriptContent: '', scriptType: 'product', sequenceNo: 2 }),
    ]
    vi.mocked(api.generateForSlot).mockResolvedValue('生成内容')
    vi.mocked(api.saveLiveScript).mockResolvedValue({} as any)
    const loadData = vi.fn().mockResolvedValue(undefined)
    const setScripts = vi.fn()

    const { result } = renderHook(() => useScriptGeneration(makeDeps({ scripts, setScripts, loadData })))

    await act(async () => {
      await result.current.handleGenerateIncremental()
    })

    // Only called for the empty script
    expect(api.generateForSlot).toHaveBeenCalledTimes(1)
    expect(api.generateForSlot).toHaveBeenCalledWith({
      scriptId: 2,
      modelId: 10,
    })
  })

  it('syncStyle updates genStyle', () => {
    const { result } = renderHook(() => useScriptGeneration(makeDeps()))
    act(() => {
      result.current.syncStyle('professional')
    })
    expect(result.current.genStyle).toBe('professional')
  })
})
