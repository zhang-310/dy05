import { useEffect, useState, useCallback } from 'react'

/**
 * 轮询 Hook，用于实时进度、状态等
 * @param fetchFn 获取数据的函数
 * @param interval 轮询间隔（毫秒）
 * @param stopCondition 满足时停止轮询
 */
export function usePolling<T>(
  fetchFn: () => Promise<T>,
  interval: number = 2000,
  stopCondition?: (data: T) => boolean
) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const poll = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchFn()
      setData(result)
      return result
    } catch (e) {
      setError(e instanceof Error ? e : new Error(String(e)))
      return null
    } finally {
      setLoading(false)
    }
  }, [fetchFn])

  useEffect(() => {
    let cancelled = false
    let timer: ReturnType<typeof setInterval> | null = null

    const run = async () => {
      if (cancelled) return
      const result = await poll()
      if (cancelled) return
      if (result != null && stopCondition?.(result)) return
      timer = setInterval(run, interval)
    }

    run()
    return () => {
      cancelled = true
      if (timer) clearInterval(timer)
    }
  }, [poll, interval, stopCondition])

  return { data, loading, error, refetch: poll }
}
