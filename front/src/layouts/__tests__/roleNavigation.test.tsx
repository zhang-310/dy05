import { describe, expect, it } from 'vitest'
import { ADMIN_NAV_GROUPS, ORG_NAV_ITEMS, TALENT_NAV_ITEMS, USER_NAV_ITEMS } from '../roleNavigation'

function flattenAdminPaths() {
  return ADMIN_NAV_GROUPS.flatMap((group) => [
    ...(group.path ? [group.path] : []),
    ...(group.children ?? []).map((child) => child.path),
  ])
}

describe('roleNavigation', () => {
  it('keeps admin navigation limited to platform management directories', () => {
    const adminPaths = flattenAdminPaths()
    const forbiddenPrefixes = [
      '/admin/live',
      '/admin/product',
      '/admin/script',
      '/admin/copy',
      '/admin/content',
      '/admin/shortvideo',
      '/admin/douyin',
    ]

    expect(adminPaths.length).toBeGreaterThan(0)
    expect(adminPaths).toContain('/admin/profile')
    for (const path of adminPaths) {
      expect(forbiddenPrefixes.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))).toBe(false)
    }
  })

  it('keeps org business navigation under the org shell', () => {
    expect(ORG_NAV_ITEMS.length).toBeGreaterThan(0)
    expect(ORG_NAV_ITEMS.every((item) => item.path.startsWith('/org/'))).toBe(true)
    expect(ORG_NAV_ITEMS.some((item) => item.path.startsWith('/org/live/'))).toBe(true)
    expect(ORG_NAV_ITEMS.some((item) => item.path.startsWith('/org/product/'))).toBe(true)
    expect(ORG_NAV_ITEMS.some((item) => item.path === '/org/profile')).toBe(true)
  })

  it('keeps talent creative navigation under the talent shell', () => {
    expect(TALENT_NAV_ITEMS.length).toBeGreaterThan(0)
    expect(TALENT_NAV_ITEMS.every((item) => item.path.startsWith('/talent/'))).toBe(true)
    expect(TALENT_NAV_ITEMS.some((item) => item.path.startsWith('/talent/shortvideo'))).toBe(true)
    expect(TALENT_NAV_ITEMS.some((item) => item.path.startsWith('/talent/douyin'))).toBe(true)
    expect(TALENT_NAV_ITEMS.some((item) => item.path === '/talent/profile')).toBe(true)
  })

  it('keeps normal user navigation inside the user shell', () => {
    expect(USER_NAV_ITEMS.length).toBeGreaterThan(0)
    expect(USER_NAV_ITEMS.every((item) => item.path.startsWith('/user/'))).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/dashboard')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/shortvideo')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/shortvideo/create')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/shortvideo/planning')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/shortvideo/materials')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/shortvideo/publish')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path === '/user/profile')).toBe(true)
    expect(USER_NAV_ITEMS.some((item) => item.path.startsWith('/admin/'))).toBe(false)
    expect(USER_NAV_ITEMS.some((item) => item.path.startsWith('/org/'))).toBe(false)
    expect(USER_NAV_ITEMS.some((item) => item.path.startsWith('/talent/'))).toBe(false)
  })
})
