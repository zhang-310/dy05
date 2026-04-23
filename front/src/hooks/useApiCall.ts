/**
 * 统一 API 调用 Hook：loading、error、统一错误提示
 */
import { useState, useCallback } from 'react'
import { useSnackbar } from 'notistack'

export interface UseApiCallOptions<T, _Args extends unknown[] = unknown[]> {
  onSuccess?: (result: T) => void
  onError?: (err: Error) => void
  successMessage?: string
  showError?: boolean
}

export function useApiCall<T, Fn extends (...a: never[]) => Promise<T>>(
  apiFn: Fn,
  options: UseApiCallOptions<T, Parameters<Fn>> = {}
) {
  type Args = Parameters<Fn>
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const { enqueueSnackbar } = useSnackbar()
  const { onSuccess, onError, successMessage, showError = true } = options

  const execute = useCallback(
    async (...args: Args): Promise<T | undefined> => {
      setLoading(true)
      setError(null)
      try {
        const result = await apiFn(...args)
        if (successMessage) enqueueSnackbar(successMessage, { variant: 'success' })
        onSuccess?.(result)
        return result
      } catch (err) {
        const e = err instanceof Error ? err : new Error(String(err))
        setError(e)
        if (showError) enqueueSnackbar(e.message || '操作失败', { variant: 'error' })
        onError?.(e)
        return undefined
      } finally {
        setLoading(false)
      }
    },
    [apiFn, onSuccess, onError, successMessage, showError, enqueueSnackbar]
  )

  return { execute, loading, error }
}
