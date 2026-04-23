import { describe, it, expect } from 'vitest'
import { fmtMaterialMeta, mergeTagCsv, materialTypeMap, TAG_PRESETS_FALLBACK } from './materialLibraryFormat'

describe('materialLibraryFormat', () => {
  it('formats width, duration and file size into compact meta text', () => {
    expect(fmtMaterialMeta({ width: 1080, height: 1920, duration: 12, fileSize: 2 * 1024 * 1024 }))
      .toBe('1080×1920 · 12s · 2.0 MB')
  })

  it('returns em dash when no metadata is available', () => {
    expect(fmtMaterialMeta({})).toBe('—')
  })

  it('merges csv tags without duplicates', () => {
    expect(mergeTagCsv('产品展示,口播', '口播，促销')).toBe('产品展示,口播,促销')
  })

  it('keeps stable fallback presets and type labels', () => {
    expect(materialTypeMap.video).toBe('视频')
    expect(TAG_PRESETS_FALLBACK).toContain('美妆')
  })
})
