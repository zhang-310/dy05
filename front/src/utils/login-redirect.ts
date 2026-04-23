/** 登录页查询参数：登录成功后跳回该站内路径（含 query、hash） */
export const RETURN_URL_QUERY = 'returnUrl'

export function getCurrentLocationForReturn(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.location.pathname + window.location.search + window.location.hash
}

/** 防止开放重定向：仅允许同源相对路径，且不能回到登录页 */
export function isSafeInternalReturnPath(path: string): boolean {
  const p = path.trim()
  if (!p.startsWith('/') || p.startsWith('//')) {
    return false
  }
  if (p === '/login' || p.startsWith('/login/') || p.startsWith('/login?')) {
    return false
  }
  return true
}

export function getValidatedReturnPathFromParam(raw: string | null | undefined): string | null {
  if (raw == null || raw === '') {
    return null
  }
  try {
    const decoded = decodeURIComponent(raw)
    return isSafeInternalReturnPath(decoded) ? decoded : null
  } catch {
    return null
  }
}

export function parseReturnPathFromSearch(search: string): string | null {
  const q = search.startsWith('?') ? search.slice(1) : search
  const raw = new URLSearchParams(q).get(RETURN_URL_QUERY)
  return getValidatedReturnPathFromParam(raw)
}

export function buildLoginHref(returnPath: string): string {
  const path = returnPath.trim()
  if (!path || !isSafeInternalReturnPath(path)) {
    return '/login'
  }
  return `/login?${RETURN_URL_QUERY}=${encodeURIComponent(path)}`
}
