import { useCallback, useRef, useState, useEffect } from 'react'

/** 防抖回调：首次点击后 delay 内再次点击会重置计时，避免快速重复提交 */
export function useDebouncedCallback<A extends unknown[]>(
  callback: (...args: A) => void | Promise<void>,
  delay: number
): (...args: A) => void {
  const callbackRef = useRef(callback)
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  callbackRef.current = callback

  return useCallback(
    (...args: A) => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current)
      timeoutRef.current = setTimeout(() => {
        callbackRef.current(...args)
        timeoutRef.current = null
      }, delay)
    },
    [delay]
  )
}

/** 防抖值：value 变化后等待 delay 毫秒无变化才更新返回值 */
export function useDebouncedValue<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debouncedValue
}

/** 节流回调：首次点击立即执行，delay 内再次点击被忽略，防止快速重复点击 */
export function useThrottledCallback<A extends unknown[]>(
  callback: (...args: A) => void | Promise<void>,
  delay: number
): (...args: A) => void {
  const callbackRef = useRef(callback)
  const lastRunRef = useRef(0)
  callbackRef.current = callback

  return useCallback(
    (...args: A) => {
      const now = Date.now()
      if (now - lastRunRef.current >= delay) {
        lastRunRef.current = now
        callbackRef.current(...args)
      }
    },
    [delay]
  )
}
