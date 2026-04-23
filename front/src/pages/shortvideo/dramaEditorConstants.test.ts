import { describe, it, expect } from 'vitest'
import { DRAMA_GENRES, bandChipColor } from './dramaEditorConstants'

describe('dramaEditorConstants', () => {
  it('exposes expected genre presets', () => {
    expect(DRAMA_GENRES).toContain('都市')
    expect(DRAMA_GENRES).toContain('甜宠')
  })

  it('maps continuity bands to chip colors', () => {
    expect(bandChipColor('good')).toBe('success')
    expect(bandChipColor('watch')).toBe('warning')
    expect(bandChipColor('risk')).toBe('error')
    expect(bandChipColor('unknown')).toBe('default')
  })
})
