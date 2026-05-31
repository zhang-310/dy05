export type RoleCode = 'admin' | 'institution' | 'talent' | 'user'
export type RoleScope = 'admin' | 'org' | 'talent' | 'user'

export const ROLE_SCOPE_PREFIX: Record<RoleScope, string> = {
  admin: '/admin',
  org: '/org',
  talent: '/talent',
  user: '/user',
}

export const ROLE_DEFAULT_PATH: Record<RoleCode, string> = {
  admin: '/admin/dashboard',
  institution: '/org/dashboard',
  talent: '/talent/dashboard',
  user: '/user/dashboard',
}

export const ROLE_TO_SCOPE: Record<RoleCode, RoleScope> = {
  admin: 'admin',
  institution: 'org',
  talent: 'talent',
  user: 'user',
}

export function roleDefaultPath(roleCode?: string | null): string {
  return ROLE_DEFAULT_PATH[(roleCode ?? '') as RoleCode] ?? ROLE_DEFAULT_PATH.user
}

export function roleScopeFromPath(pathname: string): RoleScope {
  if (pathname.startsWith('/org')) return 'org'
  if (pathname.startsWith('/talent')) return 'talent'
  if (pathname.startsWith('/user')) return 'user'
  return 'admin'
}

export function roleScopeFromRoleCode(roleCode?: string | null): RoleScope {
  return ROLE_TO_SCOPE[(roleCode ?? '') as RoleCode] ?? 'user'
}

export function allowedRoleScope(roleCode: string | null | undefined, scope: RoleScope): boolean {
  if (roleCode === 'admin') return true
  return roleScopeFromRoleCode(roleCode) === scope
}

export function migrateLegacyAdminBusinessPath(pathname: string): string | null {
  if (pathname === '/admin/live' || pathname.startsWith('/admin/live/')) {
    return `/org${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/product' || pathname.startsWith('/admin/product/')) {
    return `/org${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/script' || pathname.startsWith('/admin/script/')) {
    return `/org${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/copy' || pathname.startsWith('/admin/copy/')) {
    return `/org${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/content' || pathname.startsWith('/admin/content/')) {
    return `/org${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/slangdict' || pathname.startsWith('/admin/slangdict/')) {
    return `/org${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/shortvideo' || pathname.startsWith('/admin/shortvideo/')) {
    return `/talent${pathname.slice('/admin'.length)}`
  }
  if (pathname === '/admin/douyin' || pathname.startsWith('/admin/douyin/')) {
    return `/talent${pathname.slice('/admin'.length)}`
  }
  return null
}
