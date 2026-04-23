import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useApiCall } from '../useApiCall'
import { SnackbarProvider, useSnackbar } from 'notistack'
import { ReactNode } from 'react'

vi.mock('notistack', async () => {
  const actual = await vi.importActual('notistack')
  return {
    ...actual,
    useSnackbar: vi.fn(),
  }
})

describe('useApiCall', () => {
  const mockEnqueueSnackbar = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useSnackbar).mockReturnValue({
      enqueueSnackbar: mockEnqueueSnackbar,
      closeSnackbar: vi.fn(),
    } as any)
  })

  const wrapper = ({ children }: { children: ReactNode }) => (
    <SnackbarProvider>{children}</SnackbarProvider>
  )

  it('initializes with default state', () => {
    const mockApi = vi.fn().mockResolvedValue('result')
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBeNull()
  })

  it('executes API call successfully', async () => {
    const mockApi = vi.fn().mockResolvedValue('success')
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    let returnValue: string | undefined
    await act(async () => {
      returnValue = await result.current.execute()
    })

    expect(mockApi).toHaveBeenCalled()
    expect(returnValue).toBe('success')
    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBeNull()
  })

  it('sets loading state during execution', async () => {
    const mockApi = vi.fn(() => new Promise((resolve) => setTimeout(() => resolve('result'), 100)))
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    act(() => {
      result.current.execute()
    })

    expect(result.current.loading).toBe(true)

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
  })

  it('handles API call error', async () => {
    const mockApi = vi.fn().mockRejectedValue(new Error('API error'))
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(result.current.error).toBeInstanceOf(Error)
    expect(result.current.error?.message).toBe('API error')
    expect(result.current.loading).toBe(false)
  })

  it('shows success message when provided', async () => {
    const mockApi = vi.fn().mockResolvedValue('result')
    const { result } = renderHook(
      () => useApiCall(mockApi, { successMessage: '操作成功' }),
      { wrapper }
    )

    await act(async () => {
      await result.current.execute()
    })

    expect(mockEnqueueSnackbar).toHaveBeenCalledWith('操作成功', { variant: 'success' })
  })

  it('shows error message by default', async () => {
    const mockApi = vi.fn().mockRejectedValue(new Error('Failed'))
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(mockEnqueueSnackbar).toHaveBeenCalledWith('Failed', { variant: 'error' })
  })

  it('does not show error message when showError is false', async () => {
    const mockApi = vi.fn().mockRejectedValue(new Error('Failed'))
    const { result } = renderHook(
      () => useApiCall(mockApi, { showError: false }),
      { wrapper }
    )

    await act(async () => {
      await result.current.execute()
    })

    expect(mockEnqueueSnackbar).not.toHaveBeenCalled()
  })

  it('calls onSuccess callback', async () => {
    const mockApi = vi.fn().mockResolvedValue('result')
    const onSuccess = vi.fn()
    const { result } = renderHook(() => useApiCall(mockApi, { onSuccess }), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(onSuccess).toHaveBeenCalledWith('result')
  })

  it('calls onError callback', async () => {
    const mockApi = vi.fn().mockRejectedValue(new Error('Failed'))
    const onError = vi.fn()
    const { result } = renderHook(() => useApiCall(mockApi, { onError }), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(onError).toHaveBeenCalledWith(expect.any(Error))
  })

  it('passes arguments to API function', async () => {
    const mockApi = vi.fn().mockResolvedValue('result')
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    await act(async () => {
      await result.current.execute('arg1', 123, { key: 'value' })
    })

    expect(mockApi).toHaveBeenCalledWith('arg1', 123, { key: 'value' })
  })

  it('returns undefined on error', async () => {
    const mockApi = vi.fn().mockRejectedValue(new Error('Failed'))
    const { result } = renderHook(() => useApiCall(mockApi, { showError: false }), { wrapper })

    let returnValue: string | undefined
    await act(async () => {
      returnValue = await result.current.execute()
    })

    expect(returnValue).toBeUndefined()
  })

  it('clears previous error on new execution', async () => {
    const mockApi = vi.fn()
      .mockRejectedValueOnce(new Error('First error'))
      .mockResolvedValueOnce('success')

    const { result } = renderHook(() => useApiCall(mockApi, { showError: false }), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(result.current.error).toBeInstanceOf(Error)

    await act(async () => {
      await result.current.execute()
    })

    expect(result.current.error).toBeNull()
  })

  it('handles non-Error thrown values', async () => {
    const mockApi = vi.fn().mockRejectedValue('string error')
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(result.current.error).toBeInstanceOf(Error)
    expect(result.current.error?.message).toBe('string error')
  })

  it('shows default error message when error has no message', async () => {
    const mockApi = vi.fn().mockRejectedValue(new Error(''))
    const { result } = renderHook(() => useApiCall(mockApi), { wrapper })

    await act(async () => {
      await result.current.execute()
    })

    expect(mockEnqueueSnackbar).toHaveBeenCalledWith('操作失败', { variant: 'error' })
  })

  it('maintains stable execute function reference', () => {
    const mockApi = vi.fn().mockResolvedValue('result')
    const { result, rerender } = renderHook(() => useApiCall(mockApi), { wrapper })

    const firstExecute = result.current.execute
    rerender()
    const secondExecute = result.current.execute

    expect(firstExecute).toBe(secondExecute)
  })
})
