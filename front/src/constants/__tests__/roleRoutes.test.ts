import { describe, expect, it } from 'vitest'
import {
  allowedRoleScope,
  migrateLegacyAdminBusinessPath,
  roleDefaultPath,
  roleScopeFromPath,
  roleScopeFromRoleCode,
} from '../roleRoutes'

describe('roleRoutes', () => {
  it('maps persisted role codes to independent role home directories', () => {
    expect(roleDefaultPath('admin')).toBe('/admin/dashboard')
    expect(roleDefaultPath('institution')).toBe('/org/dashboard')
    expect(roleDefaultPath('talent')).toBe('/talent/dashboard')
    expect(roleDefaultPath('user')).toBe('/user/dashboard')
  })

  it('falls back unknown roles to the normal user directory', () => {
    expect(roleDefaultPath('operator')).toBe('/user/dashboard')
    expect(roleScopeFromRoleCode('operator')).toBe('user')
  })

  it('infers role scope from the first path segment', () => {
    expect(roleScopeFromPath('/admin/ai/dashboard')).toBe('admin')
    expect(roleScopeFromPath('/org/live/sessions')).toBe('org')
    expect(roleScopeFromPath('/talent/shortvideo')).toBe('talent')
    expect(roleScopeFromPath('/user/dashboard')).toBe('user')
  })

  it('allows admin to enter every shell and keeps other roles scoped', () => {
    expect(allowedRoleScope('admin', 'org')).toBe(true)
    expect(allowedRoleScope('admin', 'talent')).toBe(true)
    expect(allowedRoleScope('institution', 'org')).toBe(true)
    expect(allowedRoleScope('institution', 'admin')).toBe(false)
    expect(allowedRoleScope('talent', 'talent')).toBe(true)
    expect(allowedRoleScope('talent', 'org')).toBe(false)
    expect(allowedRoleScope('user', 'user')).toBe(true)
    expect(allowedRoleScope('user', 'admin')).toBe(false)
  })

  it('migrates legacy admin business directories before role gate checks', () => {
    expect(migrateLegacyAdminBusinessPath('/admin/live/sessions/18')).toBe('/org/live/sessions/18')
    expect(migrateLegacyAdminBusinessPath('/admin/product/list')).toBe('/org/product/list')
    expect(migrateLegacyAdminBusinessPath('/admin/script/list')).toBe('/org/script/list')
    expect(migrateLegacyAdminBusinessPath('/admin/content/library')).toBe('/org/content/library')
    expect(migrateLegacyAdminBusinessPath('/admin/slangdict')).toBe('/org/slangdict')
    expect(migrateLegacyAdminBusinessPath('/admin/shortvideo/projects')).toBe('/talent/shortvideo/projects')
    expect(migrateLegacyAdminBusinessPath('/admin/douyin/accounts')).toBe('/talent/douyin/accounts')
    expect(migrateLegacyAdminBusinessPath('/admin/ai/dashboard')).toBeNull()
  })
})
