import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useNetworkStatus } from '../useNetworkStatus'

vi.mock('@/stores', () => ({
  useUserStore: vi.fn(),
}))

describe('useNetworkStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('navigator', { onLine: true })
    localStorage.clear()
  })

  it('initializes with online status', () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    const { result } = renderHook(() => useNetworkStatus())

    expect(result.current.status.online).toBe(true)
  })

  it('pings server on mount', async () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    renderHook(() => useNetworkStatus({ healthUrl: '/api/health' }))

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/health',
        expect.objectContaining({
          method: 'POST',
          body: '{}',
        })
      )
    })
  })

  it('sets serverReachable to true on successful ping', async () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    const { result } = renderHook(() => useNetworkStatus())

    await waitFor(() => {
      expect(result.current.status.serverReachable).toBe(true)
      expect(result.current.status.connected).toBe(true)
    })
  })

  it('sets serverReachable to false on failed ping', async () => {
    const mockFetch = vi.fn(() =>
      Promise.reject(new Error('Network error'))
    )
    vi.stubGlobal('fetch', mockFetch)

    const { result } = renderHook(() => useNetworkStatus({ pingInterval: 100 }))

    await waitFor(() => {
      expect(result.current.status.serverReachable).toBe(false)
      expect(result.current.status.connected).toBe(false)
    }, { timeout: 3000 })
  })

  it('enqueueOperation executes immediately when online', async () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    const { result } = renderHook(() => useNetworkStatus({ enableQueue: true }))

    const operation = vi.fn().mockResolvedValue('result')

    await result.current.enqueueOperation(operation, 'test op')

    await waitFor(() => {
      expect(operation).toHaveBeenCalled()
    })
  })

  it('enqueueOperation queues when offline', async () => {
    vi.stubGlobal('navigator', { onLine: false })
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    const { result } = renderHook(() => useNetworkStatus({ enableQueue: true }))

    const operation = vi.fn().mockResolvedValue('result')

    result.current.enqueueOperation(operation, 'test op')

    await waitFor(() => {
      expect(result.current.status.queuedOps).toBe(1)
    })

    expect(operation).not.toHaveBeenCalled()
  })

  it('clearQueue rejects all queued operations', async () => {
    vi.stubGlobal('navigator', { onLine: false })
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    const { result } = renderHook(() => useNetworkStatus({ enableQueue: true }))

    const operation = vi.fn().mockResolvedValue('result')

    let rejected = false
    result.current.enqueueOperation(operation, 'test op').catch(() => {
      rejected = true
    })

    await waitFor(() => {
      expect(result.current.status.queuedOps).toBe(1)
    })

    result.current.clearQueue()

    await waitFor(() => {
      expect(result.current.status.queuedOps).toBe(0)
      expect(rejected).toBe(true)
    })
  })

  it('includes Authorization header when token exists', async () => {
    localStorage.setItem('token', 'test-token')
    const mockFetch = vi.fn(() =>
      Promise.resolve({ status: 200, ok: true } as Response)
    )
    vi.stubGlobal('fetch', mockFetch)

    renderHook(() => useNetworkStatus())

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            'Authorization': 'Bearer test-token',
          }),
        })
      )
    })
  })
})
