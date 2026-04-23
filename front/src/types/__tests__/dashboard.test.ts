import { describe, it, expect } from 'vitest'
import { parseMoney, calcGmvDelta, formatCurrencyYuan } from '../dashboard'

describe('parseMoney', () => {
  it('returns 0 for null/undefined', () => {
    expect(parseMoney(null)).toBe(0)
    expect(parseMoney(undefined)).toBe(0)
  })

  it('parses numeric values', () => {
    expect(parseMoney(100)).toBe(100)
    expect(parseMoney(0)).toBe(0)
    expect(parseMoney(-50.5)).toBe(-50.5)
  })

  it('parses string numbers (BigDecimal from backend)', () => {
    expect(parseMoney('123.45')).toBe(123.45)
    expect(parseMoney('0')).toBe(0)
    expect(parseMoney('99999.99')).toBe(99999.99)
  })

  it('returns 0 for NaN', () => {
    expect(parseMoney(NaN)).toBe(0)
  })

  it('returns 0 for non-numeric strings', () => {
    expect(parseMoney('abc')).toBe(0)
    expect(parseMoney('')).toBe(0)
  })

  it('passes through Infinity as number (isNaN check only)', () => {
    // parseMoney checks typeof === 'number' && !isNaN, so Infinity passes through
    expect(parseMoney(Infinity)).toBe(Infinity)
    expect(parseMoney(-Infinity)).toBe(-Infinity)
  })
})

describe('calcGmvDelta', () => {
  it('returns null when both values are 0', () => {
    expect(calcGmvDelta(0, 0)).toBeNull()
  })

  it('returns 100% up when yesterday is 0 but today > 0', () => {
    expect(calcGmvDelta(500, 0)).toEqual({ pct: 100, up: true })
  })

  it('returns null when today is 0 and yesterday is 0', () => {
    expect(calcGmvDelta(0, 0)).toBeNull()
  })

  it('calculates positive growth', () => {
    const result = calcGmvDelta(1200, 1000)
    expect(result).not.toBeNull()
    expect(result!.pct).toBeCloseTo(20)
    expect(result!.up).toBe(true)
  })

  it('calculates negative decline', () => {
    const result = calcGmvDelta(800, 1000)
    expect(result).not.toBeNull()
    expect(result!.pct).toBeCloseTo(-20)
    expect(result!.up).toBe(false)
  })

  it('returns 0% when no change', () => {
    const result = calcGmvDelta(1000, 1000)
    expect(result).not.toBeNull()
    expect(result!.pct).toBe(0)
    expect(result!.up).toBe(true)
  })

  it('handles negative yesterday (returns null-like behavior or 100%)', () => {
    // yesterday <= 0 and today > 0 → { pct: 100, up: true }
    expect(calcGmvDelta(100, -50)).toEqual({ pct: 100, up: true })
  })
})

describe('formatCurrencyYuan', () => {
  it('formats zero', () => {
    expect(formatCurrencyYuan(0)).toBe('¥0')
  })

  it('formats integer amounts', () => {
    const result = formatCurrencyYuan(1000)
    expect(result).toContain('¥')
    expect(result).toContain('1,000') // zh-CN locale uses comma separator
  })

  it('formats decimal amounts', () => {
    const result = formatCurrencyYuan(99.5)
    expect(result).toContain('¥')
    expect(result).toContain('99.5')
  })

  it('formats large amounts', () => {
    const result = formatCurrencyYuan(10000000)
    expect(result).toContain('¥')
    expect(result).toContain('10,000,000')
  })
})
