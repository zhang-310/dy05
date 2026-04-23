import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useEditProgress } from '../useEditProgress'
import type { LiveScript } from '@/api/live'

function makeScript(id: number, scriptContent: string, scriptType = 'opening'): LiveScript {
  return {
    id,
    sessionId: 1,
    scriptTitle: `script-${id}`,
    scriptContent,
    scriptType,
  } as LiveScript
}

describe('useEditProgress', () => {
  it('returns zero progress for empty scripts', () => {
    const { result } = renderHook(() => useEditProgress([], new Set<number>()))

    expect(result.current).toEqual({
      total: 0,
      filled: 0,
      edited: 0,
      avgWordCount: 0,
      completionRate: 0,
    })
  })

  it('calculates filled, edited, average length and completion rate', () => {
    const scripts = [
      makeScript(1, '欢迎来到直播间'),
      makeScript(2, '   '),
      makeScript(3, '这是一段更长一点的话术内容'),
    ]
    const editedIds = new Set<number>([1, 3])

    const { result } = renderHook(() => useEditProgress(scripts, editedIds))

    expect(result.current.total).toBe(3)
    expect(result.current.filled).toBe(2)
    expect(result.current.edited).toBe(2)
    expect(result.current.avgWordCount).toBe(
      Math.round((scripts[0].scriptContent.length + scripts[1].scriptContent.length + scripts[2].scriptContent.length) / 2),
    )
    expect(result.current.completionRate).toBe(67)
  })
})
