/**
 * liveProductStore 单元测试
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { useLiveProductStore } from '../liveProductStore'

describe('liveProductStore', () => {
  beforeEach(() => {
    useLiveProductStore.setState({
      selectedIds: [],
      sortStrategy: 'manual',
    })
  })

  it('setSelected updates selectedIds', () => {
    useLiveProductStore.getState().setSelected([1, 2, 3])
    expect(useLiveProductStore.getState().selectedIds).toEqual([1, 2, 3])
  })

  it('toggleSelected adds id when not present', () => {
    useLiveProductStore.getState().toggleSelected(5)
    expect(useLiveProductStore.getState().selectedIds).toEqual([5])
  })

  it('toggleSelected removes id when present', () => {
    useLiveProductStore.setState({ selectedIds: [1, 2, 3] })
    useLiveProductStore.getState().toggleSelected(2)
    expect(useLiveProductStore.getState().selectedIds).toEqual([1, 3])
  })

  it('clearSelected clears all selected ids', () => {
    useLiveProductStore.setState({ selectedIds: [1, 2, 3] })
    useLiveProductStore.getState().clearSelected()
    expect(useLiveProductStore.getState().selectedIds).toEqual([])
  })

  it('setSortStrategy updates sort strategy', () => {
    useLiveProductStore.getState().setSortStrategy('price')
    expect(useLiveProductStore.getState().sortStrategy).toBe('price')
  })
})
