import { describe, it, expect, vi, beforeEach } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useScriptToolbar } from '../useScriptToolbar'
import type { LiveProduct, LiveScript } from '@/api/live'

const mockClearAllScripts = vi.fn()
const mockRebuildScriptSlots = vi.fn()
const mockBatchSortScripts = vi.fn()

vi.mock('@/api/live', () => ({
  clearAllScripts: (...args: unknown[]) => mockClearAllScripts(...args),
  rebuildScriptSlots: (...args: unknown[]) => mockRebuildScriptSlots(...args),
  batchSortScripts: (...args: unknown[]) => mockBatchSortScripts(...args),
}))

function makeScript(id: number, sequenceNo: number, productId?: number): LiveScript {
  return {
    id,
    sessionId: 1,
    scriptTitle: `script-${id}`,
    scriptContent: `content-${id}`,
    scriptType: 'product',
    sequenceNo,
    productId,
  } as LiveScript
}

function makeProduct(id: number, productId: number): LiveProduct {
  return {
    id,
    sessionId: 1,
    productId,
    productName: `product-${id}`,
    price: 99,
    sortOrder: id,
    position: id,
    status: 1,
    createTime: '',
  } as LiveProduct
}

describe('useScriptToolbar', () => {
  const toast = vi.fn()
  const loadData = vi.fn().mockResolvedValue(undefined)
  const productManager = { handleProductClick: vi.fn() }
  const qualityCheck = { expandSection: vi.fn() }
  const setScripts = vi.fn()

  const scripts = [makeScript(1, 1, 101), makeScript(2, 2, 102), makeScript(3, 3, 103)]
  const products = [makeProduct(1, 101), makeProduct(2, 102)]

  beforeEach(() => {
    vi.clearAllMocks()
    loadData.mockResolvedValue(undefined)
    productManager.handleProductClick.mockReturnValue('product-0')
    mockClearAllScripts.mockResolvedValue(undefined)
    mockRebuildScriptSlots.mockResolvedValue(undefined)
    mockBatchSortScripts.mockResolvedValue(undefined)
  })

  it('opens clear-all confirmation only when session and scripts are available', () => {
    const { result } = renderHook(() =>
      useScriptToolbar({
        sessionId: 1,
        scripts,
        setScripts,
        products,
        loadData,
        toast,
        productManager,
        qualityCheck,
      }),
    )

    act(() => result.current.handleClearAllScripts())
    expect(result.current.clearAllConfirmOpen).toBe(true)
  })

  it('clears all scripts and shows success toast', async () => {
    const { result } = renderHook(() =>
      useScriptToolbar({
        sessionId: 1,
        scripts,
        setScripts,
        products,
        loadData,
        toast,
        productManager,
        qualityCheck,
      }),
    )

    await act(async () => {
      await result.current.doClearAllScripts()
    })

    expect(mockClearAllScripts).toHaveBeenCalledWith(1)
    expect(loadData).toHaveBeenCalled()
    expect(toast).toHaveBeenCalledWith('已清空全部话术', 'success')
    expect(result.current.clearAllLoading).toBe(false)
  })

  it('rebuilds slots and closes confirmation on success', async () => {
    const { result } = renderHook(() =>
      useScriptToolbar({
        sessionId: 1,
        scripts,
        setScripts,
        products,
        loadData,
        toast,
        productManager,
        qualityCheck,
      }),
    )

    act(() => result.current.handleRebuildSlots())
    expect(result.current.rebuildConfirmOpen).toBe(true)

    await act(async () => {
      await result.current.doRebuildSlots()
    })

    expect(mockRebuildScriptSlots).toHaveBeenCalledWith(1)
    expect(loadData).toHaveBeenCalled()
    expect(toast).toHaveBeenCalledWith('槽位已重建，话术已按产品顺序重置', 'success')
    expect(result.current.rebuildConfirmOpen).toBe(false)
  })

  it('updates highlight and scroll target when product click resolves to section key', () => {
    const { result } = renderHook(() =>
      useScriptToolbar({
        sessionId: 1,
        scripts,
        setScripts,
        products,
        loadData,
        toast,
        productManager,
        qualityCheck,
      }),
    )

    act(() => result.current.handleProductClick(101))

    expect(productManager.handleProductClick).toHaveBeenCalledWith(101)
    expect(qualityCheck.expandSection).toHaveBeenCalledWith('product-0')
    expect(result.current.highlightedProductId).toBe(101)
    expect(result.current.scrollToSectionKey).toBe('product-0')
  })

  it('optimistically reorders scripts and persists order', async () => {
    const { result } = renderHook(() =>
      useScriptToolbar({
        sessionId: 1,
        scripts,
        setScripts,
        products,
        loadData,
        toast,
        productManager,
        qualityCheck,
      }),
    )

    await act(async () => {
      await result.current.handleReorderScripts(1, 3)
    })

    expect(setScripts).toHaveBeenCalledWith([
      expect.objectContaining({ id: 2, sequenceNo: 1 }),
      expect.objectContaining({ id: 3, sequenceNo: 2 }),
      expect.objectContaining({ id: 1, sequenceNo: 3 }),
    ])
    expect(mockBatchSortScripts).toHaveBeenCalledWith({ sessionId: 1, ids: [2, 3, 1] })
    expect(result.current.reorderLoading).toBe(false)
  })
})
