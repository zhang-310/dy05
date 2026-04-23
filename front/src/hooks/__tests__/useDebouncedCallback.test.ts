import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useDebouncedCallback, useDebouncedValue, useThrottledCallback } from '../useDebouncedCallback'

describe('useDebouncedCallback', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('useDebouncedCallback', () => {
    it('delays callback execution', () => {
      const callback = vi.fn()
      const { result } = renderHook(() => useDebouncedCallback(callback, 500))

      act(() => {
        result.current('test')
      })

      expect(callback).not.toHaveBeenCalled()

      act(() => {
        vi.advanceTimersByTime(500)
      })

      expect(callback).toHaveBeenCalledWith('test')
      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('resets timer on subsequent calls', () => {
      const callback = vi.fn()
      const { result } = renderHook(() => useDebouncedCallback(callback, 500))

      act(() => {
        result.current('call1')
      })

      act(() => {
        vi.advanceTimersByTime(300)
      })

      act(() => {
        result.current('call2')
      })

      act(() => {
        vi.advanceTimersByTime(300)
      })

      expect(callback).not.toHaveBeenCalled()

      act(() => {
        vi.advanceTimersByTime(200)
      })

      expect(callback).toHaveBeenCalledWith('call2')
      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('handles multiple arguments', () => {
      const callback = vi.fn()
      const { result } = renderHook(() => useDebouncedCallback(callback, 500))

      act(() => {
        result.current('arg1', 'arg2', 'arg3')
      })

      act(() => {
        vi.advanceTimersByTime(500)
      })

      expect(callback).toHaveBeenCalledWith('arg1', 'arg2', 'arg3')
    })
  })

  describe('useDebouncedValue', () => {
    it('returns initial value immediately', () => {
      const { result } = renderHook(() => useDebouncedValue('initial', 500))

      expect(result.current).toBe('initial')
    })

    it('delays value update', async () => {
      const { result, rerender } = renderHook(
        ({ value }) => useDebouncedValue(value, 500),
        { initialProps: { value: 'initial' } }
      )

      expect(result.current).toBe('initial')

      rerender({ value: 'updated' })

      expect(result.current).toBe('initial')

      await act(async () => {
        vi.advanceTimersByTime(500)
        await vi.runAllTimersAsync()
      })

      expect(result.current).toBe('updated')
    })

    it('resets timer on rapid value changes', async () => {
      const { result, rerender } = renderHook(
        ({ value }) => useDebouncedValue(value, 500),
        { initialProps: { value: 'v1' } }
      )

      rerender({ value: 'v2' })
      await act(async () => {
        vi.advanceTimersByTime(300)
      })

      rerender({ value: 'v3' })
      await act(async () => {
        vi.advanceTimersByTime(300)
      })

      expect(result.current).toBe('v1')

      await act(async () => {
        vi.advanceTimersByTime(200)
        await vi.runAllTimersAsync()
      })

      expect(result.current).toBe('v3')
    })
  })

  describe('useThrottledCallback', () => {
    it('executes callback immediately on first call', () => {
      const callback = vi.fn()
      const { result } = renderHook(() => useThrottledCallback(callback, 500))

      act(() => {
        result.current('test')
      })

      expect(callback).toHaveBeenCalledWith('test')
      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('ignores calls within delay period', () => {
      const callback = vi.fn()
      const { result } = renderHook(() => useThrottledCallback(callback, 500))

      act(() => {
        result.current('call1')
      })

      act(() => {
        vi.advanceTimersByTime(300)
      })

      act(() => {
        result.current('call2')
      })

      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback).toHaveBeenCalledWith('call1')
    })

    it('allows call after delay period', () => {
      const callback = vi.fn()
      const { result } = renderHook(() => useThrottledCallback(callback, 500))

      act(() => {
        result.current('call1')
      })

      act(() => {
        vi.advanceTimersByTime(500)
      })

      act(() => {
        result.current('call2')
      })

      expect(callback).toHaveBeenCalledTimes(2)
      expect(callback).toHaveBeenNthCalledWith(1, 'call1')
      expect(callback).toHaveBeenNthCalledWith(2, 'call2')
    })
  })
})
