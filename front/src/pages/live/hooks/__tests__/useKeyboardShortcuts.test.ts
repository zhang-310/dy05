import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useKeyboardShortcuts } from '../useKeyboardShortcuts'

describe('useKeyboardShortcuts', () => {
  const onToggleHelp = vi.fn()
  const onSelectAll = vi.fn()
  const onExpandAll = vi.fn()
  const onCollapseAll = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('binds shortcuts when enabled', () => {
    renderHook(() =>
      useKeyboardShortcuts({
        enabled: true,
        onToggleHelp,
        onSelectAll,
        onExpandAll,
        onCollapseAll,
      }),
    )

    window.dispatchEvent(new KeyboardEvent('keydown', { key: '/', ctrlKey: true, bubbles: true }))
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'a', ctrlKey: true, bubbles: true }))
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'E', ctrlKey: true, shiftKey: true, bubbles: true }))
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'c', ctrlKey: true, shiftKey: true, bubbles: true }))

    expect(onToggleHelp).toHaveBeenCalledTimes(1)
    expect(onSelectAll).toHaveBeenCalledTimes(1)
    expect(onExpandAll).toHaveBeenCalledTimes(1)
    expect(onCollapseAll).toHaveBeenCalledTimes(1)
  })

  it('ignores shortcuts while typing in input elements', () => {
    renderHook(() =>
      useKeyboardShortcuts({
        enabled: true,
        onToggleHelp,
        onSelectAll,
      }),
    )

    const input = document.createElement('input')
    document.body.appendChild(input)
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'a', ctrlKey: true, bubbles: true }))

    expect(onSelectAll).not.toHaveBeenCalled()
    document.body.removeChild(input)
  })

  it('does not bind shortcuts when disabled', () => {
    renderHook(() =>
      useKeyboardShortcuts({
        enabled: false,
        onToggleHelp,
        onSelectAll,
      }),
    )

    window.dispatchEvent(new KeyboardEvent('keydown', { key: '?', bubbles: true }))
    expect(onToggleHelp).not.toHaveBeenCalled()
    expect(onSelectAll).not.toHaveBeenCalled()
  })
})
