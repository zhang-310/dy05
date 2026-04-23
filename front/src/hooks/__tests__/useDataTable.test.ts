import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useDataTable } from '../useDataTable'

describe('useDataTable', () => {
  const mockFetchData = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockFetchData.mockResolvedValue({
      total: 100,
      list: [
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' },
        { id: 3, name: 'Item 3' },
      ],
    })
  })

  it('initializes with default state', () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    expect(result.current.data).toEqual([])
    expect(result.current.total).toBe(0)
    expect(result.current.page).toBe(0)
    expect(result.current.rowsPerPage).toBe(10)
    expect(result.current.keyword).toBe('')
    expect(result.current.searchInput).toBe('')
    expect(result.current.selected.size).toBe(0)
    expect(result.current.idKey).toBe('id')
    expect(result.current.allSelected).toBe(false)
  })

  it('loads data on mount', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    expect(result.current.loading).toBe(true)

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
      expect(result.current.data).toHaveLength(3)
      expect(result.current.total).toBe(100)
    })

    expect(mockFetchData).toHaveBeenCalledWith({
      page: 0,
      rows: 10,
      keyword: undefined,
    })
  })

  it('uses custom initial page and rows per page', async () => {
    const { result } = renderHook(() =>
      useDataTable({
        fetchData: mockFetchData,
        initialPage: 2,
        initialRowsPerPage: 20,
      })
    )

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.page).toBe(2)
    expect(result.current.rowsPerPage).toBe(20)
    expect(mockFetchData).toHaveBeenCalledWith({
      page: 2,
      rows: 20,
      keyword: undefined,
    })
  })

  it('uses custom idKey', () => {
    const { result } = renderHook(() =>
      useDataTable({ fetchData: mockFetchData, idKey: 'customId' })
    )

    expect(result.current.idKey).toBe('customId')
  })

  it('handles fetch error', async () => {
    mockFetchData.mockRejectedValue(new Error('Network error'))

    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
      expect(result.current.error).toBe('Network error')
    })
  })

  it('handles non-Error fetch failure', async () => {
    mockFetchData.mockRejectedValue('String error')

    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.error).toBe('加载失败')
    })
  })

  it('changes page and reloads data', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.setPage(2)
    })

    await waitFor(() => {
      expect(mockFetchData).toHaveBeenCalledWith({
        page: 2,
        rows: 10,
        keyword: undefined,
      })
    })
  })

  it('changes rows per page and reloads data', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.setRowsPerPage(25)
    })

    await waitFor(() => {
      expect(mockFetchData).toHaveBeenCalledWith({
        page: 0,
        rows: 25,
        keyword: undefined,
      })
    })
  })

  it('handles search with keyword', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.setSearchInput('test keyword')
    })

    act(() => {
      result.current.handleSearch()
    })

    await waitFor(() => {
      expect(result.current.keyword).toBe('test keyword')
      expect(result.current.page).toBe(0)
      expect(mockFetchData).toHaveBeenCalledWith({
        page: 0,
        rows: 10,
        keyword: 'test keyword',
      })
    })
  })

  it('trims search input before setting keyword', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.setSearchInput('  trimmed  ')
    })

    act(() => {
      result.current.handleSearch()
    })

    await waitFor(() => {
      expect(result.current.keyword).toBe('trimmed')
    })
  })

  it('resets search and reloads data', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.setSearchInput('test')
    })

    act(() => {
      result.current.handleSearch()
    })

    await waitFor(() => {
      expect(result.current.keyword).toBe('test')
    })

    act(() => {
      result.current.handleReset()
    })

    expect(result.current.searchInput).toBe('')
    expect(result.current.keyword).toBe('')
    expect(result.current.page).toBe(0)

    await waitFor(() => {
      expect(mockFetchData).toHaveBeenCalledWith({
        page: 0,
        rows: 10,
        keyword: undefined,
      })
    })
  })

  it('refreshes data', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    mockFetchData.mockClear()

    act(() => {
      result.current.handleRefresh()
    })

    await waitFor(() => {
      expect(mockFetchData).toHaveBeenCalledTimes(1)
    })
  })

  it('selects all items', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.data).toHaveLength(3)
    })

    act(() => {
      result.current.handleSelectAll(true)
    })

    expect(result.current.selected.size).toBe(3)
    expect(result.current.selected.has(1)).toBe(true)
    expect(result.current.selected.has(2)).toBe(true)
    expect(result.current.selected.has(3)).toBe(true)
    expect(result.current.allSelected).toBe(true)
  })

  it('deselects all items', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.data).toHaveLength(3)
    })

    act(() => {
      result.current.handleSelectAll(true)
    })

    expect(result.current.selected.size).toBe(3)

    act(() => {
      result.current.handleSelectAll(false)
    })

    expect(result.current.selected.size).toBe(0)
    expect(result.current.allSelected).toBe(false)
  })

  it('selects one item', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.data).toHaveLength(3)
    })

    act(() => {
      result.current.handleSelectOne(2, true)
    })

    expect(result.current.selected.size).toBe(1)
    expect(result.current.selected.has(2)).toBe(true)
  })

  it('deselects one item', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.data).toHaveLength(3)
    })

    act(() => {
      result.current.handleSelectAll(true)
      result.current.handleSelectOne(2, false)
    })

    expect(result.current.selected.size).toBe(2)
    expect(result.current.selected.has(2)).toBe(false)
  })

  it('uses custom idKey for selection', async () => {
    mockFetchData.mockResolvedValue({
      total: 2,
      list: [
        { customId: 'a', name: 'Item A' },
        { customId: 'b', name: 'Item B' },
      ],
    })

    const { result } = renderHook(() =>
      useDataTable({ fetchData: mockFetchData, idKey: 'customId' })
    )

    await waitFor(() => {
      expect(result.current.data).toHaveLength(2)
    })

    act(() => {
      result.current.handleSelectAll(true)
    })

    expect(result.current.selected.has('a')).toBe(true)
    expect(result.current.selected.has('b')).toBe(true)
  })

  it('falls back to id when custom idKey is missing', async () => {
    mockFetchData.mockResolvedValue({
      total: 2,
      list: [
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' },
      ],
    })

    const { result } = renderHook(() =>
      useDataTable({ fetchData: mockFetchData, idKey: 'missingKey' })
    )

    await waitFor(() => {
      expect(result.current.data).toHaveLength(2)
    })

    act(() => {
      result.current.handleSelectAll(true)
    })

    expect(result.current.selected.has(1)).toBe(true)
    expect(result.current.selected.has(2)).toBe(true)
  })

  it('sets error manually', () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    act(() => {
      result.current.setError('Custom error')
    })

    expect(result.current.error).toBe('Custom error')
  })

  it('clears error on successful load', async () => {
    mockFetchData.mockRejectedValueOnce(new Error('First error'))

    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.error).toBe('First error')
    })

    mockFetchData.mockResolvedValue({ total: 1, list: [{ id: 1 }] })

    act(() => {
      result.current.handleRefresh()
    })

    await waitFor(() => {
      expect(result.current.error).toBe('')
    })
  })

  it('allSelected is false when no data', () => {
    mockFetchData.mockResolvedValue({ total: 0, list: [] })

    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    expect(result.current.allSelected).toBe(false)
  })

  it('manually sets selected items', async () => {
    const { result } = renderHook(() => useDataTable({ fetchData: mockFetchData }))

    await waitFor(() => {
      expect(result.current.data).toHaveLength(3)
    })

    act(() => {
      result.current.setSelected(new Set([1, 3]))
    })

    expect(result.current.selected.size).toBe(2)
    expect(result.current.selected.has(1)).toBe(true)
    expect(result.current.selected.has(3)).toBe(true)
  })
})
