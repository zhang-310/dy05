/**
 * 统一错误处理工具函数
 */

interface ApiError {
  message?: string
  status?: number
  code?: string
}

/**
 * 从错误对象中提取用户友好的错误消息
 */
export function getErrorMessage(error: unknown): string {
  if (!error) {
    return '未知错误'
  }

  // 标准 Error 对象
  if (error instanceof Error) {
    return error.message
  }

  // API 错误响应
  if (typeof error === 'object' && error !== null) {
    const apiError = error as ApiError
    if (apiError.message) {
      return apiError.message
    }
  }

  // 字符串错误
  if (typeof error === 'string') {
    return error
  }

  return '操作失败，请稍后重试'
}

/**
 * 处理 API 错误并返回用户友好的消息
 */
export function handleApiError(error: unknown, context?: string): string {
  const baseMessage = getErrorMessage(error)

  if (context) {
    return `${context}失败: ${baseMessage}`
  }

  return baseMessage
}

/**
 * 判断错误是否为网络错误
 */
export function isNetworkError(error: unknown): boolean {
  if (error instanceof Error) {
    return error.message.includes('Network') ||
           error.message.includes('网络') ||
           error.message.includes('timeout')
  }
  return false
}

/**
 * 判断错误是否为认证错误
 */
export function isAuthError(error: unknown): boolean {
  if (typeof error === 'object' && error !== null) {
    const apiError = error as ApiError
    return apiError.status === 401 || apiError.status === 403
  }
  return false
}
