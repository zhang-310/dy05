import { useLocation } from 'react-router-dom'
import { useUserStore } from '@/stores'

const ROLE_PREFIX: Record<string, string> = {
  admin: '/admin',
  institution: '/org',
  talent: '/talent',
  user: '/talent',
}

/**
 * 当前角色对应的路由前缀（/admin | /org | /talent）。
 * 优先根据当前 pathname 推导，避免管理员在 /admin/ai/knowledge 点击「文档/搜索」跳到 /talent 下。
 */
export function useRolePrefix() {
  const { pathname } = useLocation()
  const roles = useUserStore((s) => s.userInfo?.roles ?? [])
  const roleCode = roles[0] ?? ''

  if (pathname.startsWith('/admin')) return '/admin'
  if (pathname.startsWith('/org')) return '/org'
  if (pathname.startsWith('/talent')) return '/talent'

  return ROLE_PREFIX[roleCode ?? ''] ?? '/talent'
}
