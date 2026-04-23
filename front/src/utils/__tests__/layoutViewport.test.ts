import { describe, it, expect } from 'vitest'
import { LAYOUT_APPBAR_HEIGHT_PX, LAYOUT_APPBAR_HEIGHT_DELTA_FROM_LEGACY } from '../layoutViewport'

describe('layoutViewport constants', () => {
  it('has correct appbar height', () => {
    expect(LAYOUT_APPBAR_HEIGHT_PX).toBe(56)
  })

  it('has correct delta from legacy', () => {
    expect(LAYOUT_APPBAR_HEIGHT_DELTA_FROM_LEGACY).toBe(12)
  })

  it('both are numbers', () => {
    expect(typeof LAYOUT_APPBAR_HEIGHT_PX).toBe('number')
    expect(typeof LAYOUT_APPBAR_HEIGHT_DELTA_FROM_LEGACY).toBe('number')
  })
})
