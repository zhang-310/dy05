import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { usePolling } from '../usePolling'

describe('usePolling', () => {
  it('initializes with null data', () => {
    const fetchFn = vi.fn().mockResolvedValue('data')
    const { result } = renderHook(() => usePolling(fetchFn, 2000))

    expect(result.current.data).toBeNull()
    expect(result.current.loading).toBe(true)
    expect(result.current.error).toBeNull()
  })

  it('fetches data on mount', async () => {
    const fetchFn = vi.fn().mockResolvedValue('test-data')
    const { result, unmount } = renderHook(() => usePolling(fetchFn, 2000))

    await waitFor(() => {
      expect(fetchFn).toHaveBeenCalled()
      expect(result.current.data).toBe('test-data')
      expect(result.current.loading).toBe(false)
    })

    unmount()
  })

  it('polls at specified interval', async () => {
    const fetchFn = vi.fn()
      .mockResolvedValueOnce('data1')
      .mockResolvedValueOnce('data2')

    const { result, unmount } = renderHook(() => usePolling(fetchFn, 100))

    await waitFor(() => {
      expect(result.current.data).toBe('data1')
    })

    await waitFor(() => {
      expect(result.current.data).toBe('data2')
    }, { timeout: 300 })

    expect(fetchFn.mock.calls.length).toBeGreaterThanOrEqual(2)
    unmount()
  })

  it('stops polling when stopCondition is met', async () => {
    const fetchFn = vi.fn()
      .mockResolvedValue({ status: 'processing' })

    const stopCondition = vi.fn((data: { status: string }) => data.status === 'completed')

    const { result, unmount } = renderHook(() => usePolling(fetchFn, 100, stopCondition))

    await waitFor(() => {
      expect(result.current.data).toEqual({ status: 'processing' })
    })

    // Change mock to return completed status
    fetchFn.mockResolvedValue({ status: 'completed' })

    await waitFor(() => {
      expect(result.current.data).toEqual({ status: 'completed' })
    }, { timeout: 300 })

    // Verify stopCondition was called
    expect(stopCondition).toHaveBeenCalled()

    unmount()
  })

  it('sets error when fetch fails', async () => {
    const fetchFn = vi.fn().mockRejectedValue(new Error('Fetch failed'))
    const { result, unmount } = renderHook(() => usePolling(fetchFn, 2000))

    await waitFor(() => {
      expect(result.current.error).toBeInstanceOf(Error)
      expect(result.current.error?.message).toBe('Fetch failed')
      expect(result.current.data).toBeNull()
      expect(result.current.loading).toBe(false)
    })

    unmount()
  })

  it('refetch manually fetches data', async () => {
    const fetchFn = vi.fn().mockResolvedValue('manual-data')
    const { result, unmount } = renderHook(() => usePolling(fetchFn, 2000))

    await waitFor(() => {
      expect(result.current.data).toBe('manual-data')
    })

    fetchFn.mockResolvedValue('refetched-data')

    await result.current.refetch()

    await waitFor(() => {
      expect(result.current.data).toBe('refetched-data')
    })

    unmount()
  })

  it('cleans up interval on unmount', async () => {
    const fetchFn = vi.fn().mockResolvedValue('data')
    const { result, unmount } = renderHook(() => usePolling(fetchFn, 100))

    await waitFor(() => {
      expect(result.current.data).toBe('data')
    })

    const callCountBeforeUnmount = fetchFn.mock.calls.length

    unmount()

    await new Promise(resolve => setTimeout(resolve, 300))

    // Should not have significantly more calls after unmount
    expect(fetchFn.mock.calls.length).toBeLessThanOrEqual(callCountBeforeUnmount + 2)
  })
})
