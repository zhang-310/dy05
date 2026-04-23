import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))
vi.mock('@/hooks/useDebouncedCallback', () => ({
  useThrottledCallback: (fn: () => void) => fn,
}))
vi.mock('@/api/live', () => ({
  batchChatForScript: vi.fn(),
  batchSaveLiveScripts: vi.fn(),
  checkSimilarity: vi.fn(),
  generateSkeletonStream: vi.fn(),
  refineScriptStream: vi.fn(),
  deleteLiveScript: vi.fn(),
  saveLiveScript: vi.fn(),
}))

import { useQualityCheck } from '../useQualityCheck'
import type { UseQualityCheckDeps } from '../useQualityCheck'
import type { LiveScript } from '@/api/live'
import * as api from '@/api/live'

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

function makeDeps(overrides?: Partial<UseQualityCheckDeps>): UseQualityCheckDeps {
  return {
    sessionId: 1,
    scripts: [],
    setScripts: vi.fn(),
    scriptSections: [],
    loadData: vi.fn().mockResolvedValue(undefined),
    session: { id: 1, liveTitle: 'test' },
    setEditingId: vi.fn(),
    setEditingScript: vi.fn(),
    setEditContent: vi.fn(),
    setEditDurationLimit: vi.fn(),
    setEditRequirement: vi.fn(),
    prepareChatForSkeleton: vi.fn(),
    selectedModelId: 1,
    ...overrides,
  }
}

describe('useQualityCheck', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('initializes with empty selections and collapsed sections', () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))
    expect(result.current.selectedScriptIds.size).toBe(0)
    expect(result.current.lockedScriptIds.size).toBe(0)
    expect(result.current.batchOpen).toBe(false)
    // opening and closing expanded by default
    expect(result.current.expandedSections.has('opening')).toBe(true)
    expect(result.current.expandedSections.has('closing')).toBe(true)
  })

  it('toggleScriptSelect adds and removes script ids', () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))
    act(() => {
      result.current.toggleScriptSelect(1)
    })
    expect(result.current.selectedScriptIds.has(1)).toBe(true)
    act(() => {
      result.current.toggleScriptSelect(1)
    })
    expect(result.current.selectedScriptIds.has(1)).toBe(false)
  })

  it('toggleScriptLock adds and removes lock ids', () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))
    act(() => {
      result.current.toggleScriptLock(5)
    })
    expect(result.current.lockedScriptIds.has(5)).toBe(true)
    act(() => {
      result.current.toggleScriptLock(5)
    })
    expect(result.current.lockedScriptIds.has(5)).toBe(false)
  })

  it('handleBatchApply skips locked scripts', async () => {
    vi.mocked(api.batchChatForScript).mockResolvedValue({ 2: 'updated' })
    vi.mocked(api.batchSaveLiveScripts).mockResolvedValue([makeScript({ id: 2, scriptContent: 'updated' })])

    const { result } = renderHook(() => useQualityCheck(makeDeps()))

    // Select scripts 1, 2 and lock script 1
    act(() => {
      result.current.toggleScriptSelect(1)
      result.current.toggleScriptSelect(2)
      result.current.toggleScriptLock(1)
    })

    // Open batch dialog and set message
    act(() => {
      result.current.setBatchOpen(true)
      result.current.setBatchMessage('改成促销')
    })

    await act(async () => {
      await result.current.handleBatchApply()
    })

    // Only unlocked script 2 should be sent
    expect(api.batchChatForScript).toHaveBeenCalledWith({ ids: [2], message: '改成促销', modelId: 1 })
  })

  it('handleBatchApply does nothing when all selected are locked', async () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))

    act(() => {
      result.current.toggleScriptSelect(1)
      result.current.toggleScriptLock(1)
      result.current.setBatchOpen(true)
      result.current.setBatchMessage('test')
    })

    await act(async () => {
      await result.current.handleBatchApply()
    })

    expect(api.batchChatForScript).not.toHaveBeenCalled()
  })

  it('removeFromSelected removes id from selection', () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))
    act(() => {
      result.current.toggleScriptSelect(1)
      result.current.toggleScriptSelect(2)
    })
    expect(result.current.selectedScriptIds.size).toBe(2)

    act(() => {
      result.current.removeFromSelected(1)
    })
    expect(result.current.selectedScriptIds.has(1)).toBe(false)
    expect(result.current.selectedScriptIds.has(2)).toBe(true)
  })

  it('toggleSection expands and collapses sections', () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))
    // 'product_1' is initially collapsed
    expect(result.current.expandedSections.has('product_1')).toBe(false)
    act(() => {
      result.current.toggleSection('product_1')
    })
    expect(result.current.expandedSections.has('product_1')).toBe(true)
    act(() => {
      result.current.toggleSection('product_1')
    })
    expect(result.current.expandedSections.has('product_1')).toBe(false)
  })

  it('handleExpandSections and handleCollapseSections', () => {
    const { result } = renderHook(() => useQualityCheck(makeDeps()))
    act(() => {
      result.current.handleExpandSections(['a', 'b', 'c'])
    })
    expect(result.current.expandedSections.has('a')).toBe(true)
    expect(result.current.expandedSections.has('b')).toBe(true)
    expect(result.current.expandedSections.has('c')).toBe(true)

    act(() => {
      result.current.handleCollapseSections()
    })
    expect(result.current.expandedSections.size).toBe(0)
  })

  it('handleBatchDeleteSelected deletes selected scripts', async () => {
    vi.mocked(api.deleteLiveScript).mockResolvedValue(undefined)
    const loadData = vi.fn().mockResolvedValue(undefined)
    const { result } = renderHook(() => useQualityCheck(makeDeps({ loadData })))

    act(() => {
      result.current.toggleScriptSelect(10)
      result.current.toggleScriptSelect(20)
    })

    await act(async () => {
      await result.current.handleBatchDeleteSelected()
    })

    expect(api.deleteLiveScript).toHaveBeenCalledWith(10)
    expect(api.deleteLiveScript).toHaveBeenCalledWith(20)
    expect(loadData).toHaveBeenCalled()
    expect(result.current.selectedScriptIds.size).toBe(0)
  })

  it('handleCheckSimilarityThrottled calls checkSimilarity', async () => {
    vi.mocked(api.checkSimilarity).mockResolvedValue([{ scriptId1: 1, scriptId2: 2, type1: 'opening', type2: 'product', similarityLevel: 'high', suggestion: '建议修改' }])
    const { result } = renderHook(() => useQualityCheck(makeDeps()))

    await act(async () => {
      await result.current.handleCheckSimilarityThrottled()
    })

    expect(api.checkSimilarity).toHaveBeenCalledWith({ sessionId: 1 })
    expect(result.current.similarityOpen).toBe(true)
  })
})
