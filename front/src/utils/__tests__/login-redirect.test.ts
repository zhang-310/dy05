import { describe, expect, it } from 'vitest'
import {
  buildLoginHref,
  getValidatedReturnPathFromParam,
  isSafeInternalReturnPath,
  parseReturnPathFromSearch,
} from '../login-redirect'

describe('login-redirect', () => {
  it('allows only same-origin internal return paths', () => {
    expect(isSafeInternalReturnPath('/admin/ai/dashboard?tab=cost#today')).toBe(true)
    expect(isSafeInternalReturnPath('https://evil.example/admin')).toBe(false)
    expect(isSafeInternalReturnPath('//evil.example/admin')).toBe(false)
    expect(isSafeInternalReturnPath('/login')).toBe(false)
    expect(isSafeInternalReturnPath('/login?returnUrl=%2Fadmin')).toBe(false)
  })

  it('decodes and validates returnUrl query values', () => {
    expect(getValidatedReturnPathFromParam('%2Fadmin%2Fshortvideo%2Fprojects%3Fpage%3D1')).toBe('/admin/shortvideo/projects?page=1')
    expect(getValidatedReturnPathFromParam('https%3A%2F%2Fevil.example')).toBeNull()
    expect(getValidatedReturnPathFromParam('%E0%A4%A')).toBeNull()
  })

  it('parses search strings and builds login hrefs safely', () => {
    expect(parseReturnPathFromSearch('?returnUrl=%2Fadmin%2Fdashboard')).toBe('/admin/dashboard')
    expect(parseReturnPathFromSearch('returnUrl=%2Flogin')).toBeNull()
    expect(buildLoginHref('/admin/ai/agent/chat/18?tab=history')).toBe('/login?returnUrl=%2Fadmin%2Fai%2Fagent%2Fchat%2F18%3Ftab%3Dhistory')
    expect(buildLoginHref('https://evil.example/admin')).toBe('/login')
  })
})
