import { describe, expect, it } from 'vitest'
import { clampEndDurationTrim, clampStartTrim, effectiveEndSec } from './timelineTrimMath'

describe('timelineTrimMath (V-1)', () => {
  it('effectiveEndSec caps by sourceLen', () => {
    expect(effectiveEndSec(100, 8)).toBe(8)
    expect(effectiveEndSec(undefined, 8)).toBe(8)
  })

  it('clampEndDurationTrim enforces min 0.05 and max sourceLen', () => {
    expect(clampEndDurationTrim(0, 10)).toBe(0.05)
    expect(clampEndDurationTrim(99, 10)).toBe(10)
    expect(clampEndDurationTrim(3, 10)).toBe(3)
  })

  it('clampStartTrim stays within endSec - 0.1', () => {
    expect(clampStartTrim(5, 5)).toBe(4.9)
    expect(clampStartTrim(-1, 5)).toBe(0)
    expect(clampStartTrim(0, 5)).toBe(0)
  })
})
