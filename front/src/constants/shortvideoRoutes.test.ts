import { describe, it, expect } from 'vitest'
import {
  shortvideoRoutes,
  shortvideoLegacyRedirects,
  SHORTVIDEO_LEGACY_ROUTE_PATHS,
  SHORTVIDEO_SMOKE_ROUTE_PATHS,
  ADMIN_SHORTVIDEO_BASE,
  shortvideoSubtitlePath,
} from './shortvideoRoutes'

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

  it('keeps legacy SEO path as redirect-only route outside the smoke list', () => {
    expect(shortvideoLegacyRedirects.seo).toEqual({
      from: `${ADMIN_SHORTVIDEO_BASE}/seo`,
      to: shortvideoRoutes.seoOptimize,
      label: 'SEO 优化',
    })
    expect(SHORTVIDEO_LEGACY_ROUTE_PATHS).toEqual([`${ADMIN_SHORTVIDEO_BASE}/seo`])
    expect(SHORTVIDEO_SMOKE_ROUTE_PATHS).not.toContain(shortvideoLegacyRedirects.seo.from)
  })

  it('keeps entity-bound subtitle detail routes out of global smoke entry list', () => {
    expect(shortvideoSubtitlePath(18)).toBe(`${ADMIN_SHORTVIDEO_BASE}/subtitle-editor/18`)
    expect(SHORTVIDEO_SMOKE_ROUTE_PATHS).not.toContain(shortvideoSubtitlePath(0))
  })
})
