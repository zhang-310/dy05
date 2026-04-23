import { describe, it, expect } from 'vitest'
import { shortvideoRoutes, SHORTVIDEO_SMOKE_ROUTE_PATHS, ADMIN_SHORTVIDEO_BASE } from './shortvideoRoutes'

describe('shortvideoRoutes', () => {
  it('uses single admin base prefix', () => {
    for (const path of Object.values(shortvideoRoutes)) {
      expect(path.startsWith(`${ADMIN_SHORTVIDEO_BASE}/`)).toBe(true)
    }
  })

  it('has unique paths', () => {
    const paths = Object.values(shortvideoRoutes)
    expect(new Set(paths).size).toBe(paths.length)
  })

  it('smoke list matches registered route count and uniqueness', () => {
    expect(SHORTVIDEO_SMOKE_ROUTE_PATHS.length).toBeGreaterThan(20)
    expect(new Set(SHORTVIDEO_SMOKE_ROUTE_PATHS).size).toBe(SHORTVIDEO_SMOKE_ROUTE_PATHS.length)
  })
})
