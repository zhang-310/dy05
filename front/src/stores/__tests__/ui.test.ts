/**
 * ui store 测试
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { useUiStore } from '../ui'

describe('useUiStore', () => {
  beforeEach(() => {
    useUiStore.setState({
      sidebarOpen: true,
    })
  })

  it('toggleSidebar toggles open state', () => {
    const { toggleSidebar } = useUiStore.getState()
    expect(useUiStore.getState().sidebarOpen).toBe(true)
    toggleSidebar()
    expect(useUiStore.getState().sidebarOpen).toBe(false)
    toggleSidebar()
    expect(useUiStore.getState().sidebarOpen).toBe(true)
  })

  it('setSidebarOpen updates sidebar state', () => {
    const { setSidebarOpen } = useUiStore.getState()
    setSidebarOpen(false)
    expect(useUiStore.getState().sidebarOpen).toBe(false)
    setSidebarOpen(true)
    expect(useUiStore.getState().sidebarOpen).toBe(true)
  })
})
