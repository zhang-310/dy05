import { renderHook } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { usePageContext } from '../usePageContext'

function wrapper(path: string) {
  return ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>
  )
}

describe('usePageContext', () => {
  it('recognizes current admin live session workbench routes', () => {
    const { result } = renderHook(() => usePageContext(), {
      wrapper: wrapper('/admin/live/sessions/18?step=2&tab=scripts'),
    })

    expect(result.current.kind).toBe('live_workbench')
    expect(result.current.liveSessionId).toBe(18)
    expect(result.current.headline).toBe('直播场次 #18')
  })

  it('recognizes org and talent live session workbench shells', () => {
    const { result: orgResult } = renderHook(() => usePageContext(), {
      wrapper: wrapper('/org/live/sessions/33'),
    })
    const { result: talentResult } = renderHook(() => usePageContext(), {
      wrapper: wrapper('/talent/live/sessions/44'),
    })

    expect(orgResult.current.kind).toBe('live_workbench')
    expect(orgResult.current.liveSessionId).toBe(33)
    expect(talentResult.current.kind).toBe('live_workbench')
    expect(talentResult.current.liveSessionId).toBe(44)
  })
})
