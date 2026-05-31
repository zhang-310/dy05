import { describe, expect, it } from 'vitest'
import {
  ADMIN_SHORTVIDEO_BASE,
  TALENT_SHORTVIDEO_BASE,
  shortvideoAccountDetailPath,
  shortvideoBenchmarkAnalysisPath,
  shortvideoBenchmarkQualityScriptPath,
  shortvideoRoutes,
  shortvideoSubtitlePath,
} from '../shortvideoRoutes'

describe('shortvideoRoutes', () => {
  it('keeps admin shortvideo base only as a legacy compatibility prefix', () => {
    expect(ADMIN_SHORTVIDEO_BASE).toBe('/admin/shortvideo')
    expect(TALENT_SHORTVIDEO_BASE).toBe('/talent/shortvideo')
  })

  it('puts active short video business routes under the talent directory', () => {
    expect(shortvideoRoutes.dashboard).toBe('/talent/shortvideo/dashboard')
    expect(shortvideoRoutes.projects).toBe('/talent/shortvideo/projects')
    expect(shortvideoRoutes.collect).toBe('/talent/shortvideo/collect')
    expect(shortvideoRoutes.publish).toBe('/talent/shortvideo/publish')
  })

  it('builds entity deep links under the talent shortvideo directory', () => {
    expect(shortvideoSubtitlePath(18)).toBe('/talent/shortvideo/subtitle-editor/18')
    expect(shortvideoBenchmarkAnalysisPath(9)).toBe('/talent/shortvideo/benchmark/analysis/9')
    expect(shortvideoBenchmarkQualityScriptPath(7)).toBe('/talent/shortvideo/benchmark/quality-scripts/7')
    expect(shortvideoAccountDetailPath(3, 'videos')).toBe('/talent/shortvideo/accounts/3?tab=videos')
  })
})
