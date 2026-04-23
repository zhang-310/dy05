import { describe, it, expect, beforeEach } from 'vitest'
import { usePaginationStore } from '../pagination'

describe('pagination', () => {
  beforeEach(() => {
    usePaginationStore.setState({ pageByKey: {} })
  })

  it('initializes with empty pageByKey', () => {
    const state = usePaginationStore.getState()
    expect(state.pageByKey).toEqual({})
  })

  it('setPage stores page number for key', () => {
    const { setPage } = usePaginationStore.getState()

    setPage('product-list', 3)

    const state = usePaginationStore.getState()
    expect(state.pageByKey['product-list']).toBe(3)
  })

  it('setPage updates existing page', () => {
    const { setPage } = usePaginationStore.getState()

    setPage('product-list', 1)
    setPage('product-list', 5)

    const state = usePaginationStore.getState()
    expect(state.pageByKey['product-list']).toBe(5)
  })

  it('setPage preserves other pages', () => {
    const { setPage } = usePaginationStore.getState()

    setPage('product-list', 2)
    setPage('live-sessions', 4)

    const state = usePaginationStore.getState()
    expect(state.pageByKey).toEqual({
      'product-list': 2,
      'live-sessions': 4,
    })
  })
})
