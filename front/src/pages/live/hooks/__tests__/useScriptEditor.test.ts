import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Mock dependencies
vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))
vi.mock('@/api/live', () => ({
  checkViolation: vi.fn(),
  saveLiveScript: vi.fn(),
  deleteLiveScript: vi.fn(),
  updateScriptExecuted: vi.fn(),
  saveScriptToLibrary: vi.fn(),
  saveBatchToLibrary: vi.fn(),
  exportScripts: vi.fn(),
}))

import { useScriptEditor } from '../useScriptEditor'
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

function makeDeps(overrides?: Partial<Parameters<typeof useScriptEditor>[0]>) {
  return {
    sessionId: 1 as number | '',
    scripts: [] as LiveScript[],
    setScripts: vi.fn() as React.Dispatch<React.SetStateAction<LiveScript[]>>,
    scriptSections: [],
    loadData: vi.fn().mockResolvedValue(undefined),
    session: { id: 1, liveTitle: 'test' },
    ...overrides,
  }
}

describe('useScriptEditor', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.clear()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('initializes with no editing state', () => {
    const { result } = renderHook(() => useScriptEditor(makeDeps()))
    expect(result.current.editingId).toBeNull()
    expect(result.current.editContent).toBe('')
    expect(result.current.dirty).toBe(false)
  })

  it('handleStartEdit sets editing state', () => {
    const { result } = renderHook(() => useScriptEditor(makeDeps()))
    act(() => {
      result.current.handleStartEdit(10, 'hello world', makeScript({ id: 10, scriptType: 'opening' }))
    })
    expect(result.current.editingId).toBe(10)
    expect(result.current.editContent).toBe('hello world')
    expect(result.current.dirty).toBe(false)
  })

  it('handleCancelEdit resets editing state and clears draft', () => {
    const { result } = renderHook(() => useScriptEditor(makeDeps()))
    act(() => {
      result.current.handleStartEdit(10, 'hello')
    })
    expect(result.current.editingId).toBe(10)
    act(() => {
      result.current.handleCancelEdit()
    })
    expect(result.current.editingId).toBeNull()
    expect(result.current.editContent).toBe('')
    expect(result.current.dirty).toBe(false)
  })

  it('handleEditContentChange sets dirty and content', () => {
    const { result } = renderHook(() => useScriptEditor(makeDeps()))
    act(() => {
      result.current.handleStartEdit(10, 'original')
    })
    act(() => {
      result.current.handleEditContentChange('updated')
    })
    expect(result.current.editContent).toBe('updated')
    expect(result.current.dirty).toBe(true)
  })

  it('undo/redo stack works correctly', () => {
    const { result } = renderHook(() => useScriptEditor(makeDeps()))
    act(() => {
      result.current.handleStartEdit(10, 'v1')
    })
    expect(result.current.canUndo).toBe(false)

    // Type "v2" and wait for debounce
    act(() => {
      result.current.handleEditContentChange('v2')
    })
    act(() => {
      vi.advanceTimersByTime(600)
    })
    expect(result.current.canUndo).toBe(true)

    // Undo
    act(() => {
      result.current.handleUndo()
    })
    expect(result.current.editContent).toBe('v1')
    expect(result.current.canRedo).toBe(true)

    // Redo
    act(() => {
      result.current.handleRedo()
    })
    expect(result.current.editContent).toBe('v2')
  })

  it('draft save/restore via localStorage', () => {
    const deps = makeDeps()
    const { result, unmount } = renderHook(() => useScriptEditor(deps))

    // Start edit and change content
    act(() => {
      result.current.handleStartEdit(10, 'original')
    })
    act(() => {
      result.current.handleEditContentChange('draft content')
    })
    // Wait for draft auto-save debounce (1500ms)
    act(() => {
      vi.advanceTimersByTime(2000)
    })

    // Verify draft was saved
    const draftKey = `live-script-draft-1-10`
    const saved = localStorage.getItem(draftKey)
    expect(saved).not.toBeNull()
    const parsed = JSON.parse(saved!)
    expect(parsed.content).toBe('draft content')

    unmount()

    // Re-render and start editing same script — should recover draft
    const { result: result2 } = renderHook(() => useScriptEditor(deps))
    act(() => {
      result2.current.handleStartEdit(10, 'original', makeScript({ id: 10, scriptType: 'product' }))
    })
    expect(result2.current.editContent).toBe('draft content')
    expect(result2.current.draftRecoveryId).toBe(10)
  })

  it('does not write the removed global live edit store key', () => {
    const deps = makeDeps()
    const { result } = renderHook(() => useScriptEditor(deps))

    act(() => {
      result.current.handleStartEdit(10, 'original')
    })
    act(() => {
      result.current.handleEditContentChange('hook-owned draft')
    })
    act(() => {
      vi.advanceTimersByTime(2000)
    })

    expect(localStorage.getItem('live-edit-storage')).toBeNull()
    expect(localStorage.getItem('live-script-draft-1-10')).not.toBeNull()
  })

  it('discardDraftRecovery restores original content', () => {
    // Pre-set a draft
    localStorage.setItem('live-script-draft-1-20', JSON.stringify({ content: 'draft', savedAt: Date.now() }))

    const deps = makeDeps()
    const { result } = renderHook(() => useScriptEditor(deps))
    act(() => {
      result.current.handleStartEdit(20, 'original', makeScript({ id: 20, scriptContent: 'original', scriptType: 'product' }))
    })
    expect(result.current.editContent).toBe('draft')

    act(() => {
      result.current.discardDraftRecovery()
    })
    expect(result.current.editContent).toBe('original')
    expect(result.current.draftRecoveryId).toBeNull()
  })

  it('handleSaveEdit calls saveLiveScript and clears state', async () => {
    const deps = makeDeps({
      scriptSections: [{ key: 'opening', title: '开场', scripts: [makeScript({ id: 10, scriptType: 'opening', scriptContent: 'hi' })] }],
    })
    vi.mocked(api.saveLiveScript).mockResolvedValue({ id: 10, scriptContent: 'saved' } as any)

    const { result } = renderHook(() => useScriptEditor(deps))
    act(() => {
      result.current.handleStartEdit(10, 'hi', makeScript({ id: 10, scriptType: 'opening', scriptContent: 'hi' }))
    })
    act(() => {
      result.current.handleEditContentChange('updated')
    })

    await act(async () => {
      await result.current.handleSaveEdit()
    })

    expect(api.saveLiveScript).toHaveBeenCalledWith(expect.objectContaining({
      id: 10,
      sessionId: 1,
      scriptContent: 'updated',
    }))
    expect(result.current.editingId).toBeNull()
    expect(result.current.dirty).toBe(false)
  })

  it('handleDeleteScript calls API and removes script optimistically', async () => {
    const scripts = [
      makeScript({ id: 1, scriptContent: 'a' }),
      makeScript({ id: 2, scriptContent: 'b' }),
    ]
    const setScripts = vi.fn()
    const deps = makeDeps({ scripts, setScripts })
    vi.mocked(api.deleteLiveScript).mockResolvedValue(undefined)

    const { result } = renderHook(() => useScriptEditor(deps))

    // Set delete confirm
    act(() => {
      result.current.setDeleteConfirm(scripts[0])
    })

    await act(async () => {
      await result.current.handleDeleteScript()
    })

    expect(api.deleteLiveScript).toHaveBeenCalledWith(1)
    expect(result.current.deleteConfirm).toBeNull()
  })
})
