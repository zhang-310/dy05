import { describe, it, expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useScriptSearch } from '../useScriptSearch'
import type { LiveScript } from '@/api/live'

function makeScript(id: number, scriptType: string, scriptContent: string): LiveScript {
  return {
    id,
    sessionId: 1,
    scriptTitle: `script-${id}`,
    scriptType,
    scriptContent,
  } as LiveScript
}

describe('useScriptSearch', () => {
  const scripts = [
    makeScript(1, 'opening', '欢迎来到直播间'),
    makeScript(2, 'product', '这款护肤品主打修护屏障'),
    makeScript(3, 'closing', '喜欢的话记得点关注'),
  ]

  it('returns all scripts when query is empty', () => {
    const { result } = renderHook(() => useScriptSearch(scripts))
    expect(result.current.filtered).toEqual(scripts)
  })

  it('filters by script type and content with case-insensitive matching', () => {
    const { result } = renderHook(() => useScriptSearch(scripts))

    act(() => result.current.setQuery('PRODUCT'))
    expect(result.current.filtered.map(s => s.id)).toEqual([2])

    act(() => result.current.setQuery('关注'))
    expect(result.current.filtered.map(s => s.id)).toEqual([3])
  })

  it('clear resets query and filtered result', () => {
    const { result } = renderHook(() => useScriptSearch(scripts))

    act(() => result.current.setQuery('护肤'))
    expect(result.current.filtered.map(s => s.id)).toEqual([2])

    act(() => result.current.clear())
    expect(result.current.query).toBe('')
    expect(result.current.filtered).toEqual(scripts)
  })
})
