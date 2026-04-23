export const ERROR_MESSAGES: Record<number, string> = {
  200: '成功',
  1001: '系统内部错误',
  1002: '参数校验失败',
  1003: '数据不存在',
  1004: '操作失败',
  2001: '用户未登录',
  2002: '用户名或密码错误',
  2003: 'Token 已过期',
  2004: '权限不足',
  3001: '账号不存在',
  3002: '账号已存在',
}

export function getErrorMessage(code: number): string {
  return ERROR_MESSAGES[code] ?? `未知错误 (${code})`
}
