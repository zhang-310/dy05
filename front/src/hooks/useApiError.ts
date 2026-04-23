import { useCallback } from 'react'

/**
 * 统一 API 错误处理 Hook
 * 替代分散在各组件中的 console.error
 */
export function useApiError() {
  const handleError = useCallback((error: unknown, context?: string) => {
    const message = error instanceof Error ? error.message : String(error)

    // 开发环境打印详细错误
    if (import.meta.env.DEV) {
      console.error(`[${context || 'API'}]`, error)
    }

    // 尝试获取后端错误码
    const apiError = error as { status?: number; message?: string }
    const displayMessage = apiError?.message || message || '操作失败，请稍后重试'

    // 发送到全局 toast
    window.dispatchEvent(new CustomEvent('app:error', {
      detail: { message: displayMessage, context }
    }))

    return displayMessage
  }, [])

  return { handleError }
}
