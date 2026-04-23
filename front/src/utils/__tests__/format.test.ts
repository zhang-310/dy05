import { describe, it, expect } from 'vitest'
import { formatMoney, formatGmv, formatPercent, formatNumber, formatDateTime } from '../format'

describe('format', () => {
  describe('formatMoney', () => {
    it('formats number to currency with 2 decimals', () => {
      expect(formatMoney(1234.56)).toBe('¥1,234.56')
    })

    it('formats integer with .00', () => {
      expect(formatMoney(1000)).toBe('¥1,000.00')
    })

    it('returns dash for null', () => {
      expect(formatMoney(null)).toBe('-')
    })

    it('returns dash for undefined', () => {
      expect(formatMoney(undefined)).toBe('-')
    })

    it('returns original string for NaN', () => {
      expect(formatMoney('invalid')).toBe('invalid')
    })
  })

  describe('formatGmv', () => {
    it('formats large numbers in compact mode (亿)', () => {
      expect(formatGmv(123456789)).toBe('¥1.23亿')
    })

    it('formats medium numbers in compact mode (万)', () => {
      expect(formatGmv(123456)).toBe('¥12.35万')
    })

    it('formats small numbers with locale', () => {
      expect(formatGmv(9999)).toBe('¥9,999.00')
    })

    it('formats without compact mode', () => {
      expect(formatGmv(123456, { compact: false })).toBe('¥123,456.00')
    })

    it('uses custom unit', () => {
      expect(formatGmv(10000, { unit: '$' })).toBe('$1.00万')
    })

    it('returns dash for null', () => {
      expect(formatGmv(null)).toBe('-')
    })

    it('returns original string for NaN', () => {
      expect(formatGmv('invalid')).toBe('invalid')
    })
  })

  describe('formatPercent', () => {
    it('formats decimal to percentage', () => {
      expect(formatPercent(0.28)).toBe('28.0%')
    })

    it('formats with custom decimals', () => {
      expect(formatPercent(0.12345, 2)).toBe('12.35%')
    })

    it('formats zero', () => {
      expect(formatPercent(0)).toBe('0.0%')
    })

    it('returns dash for null', () => {
      expect(formatPercent(null)).toBe('-')
    })

    it('returns dash for NaN', () => {
      expect(formatPercent('invalid')).toBe('-')
    })
  })

  describe('formatNumber', () => {
    it('formats number with thousand separators', () => {
      expect(formatNumber(1234567)).toBe('1,234,567')
    })

    it('formats decimal numbers', () => {
      expect(formatNumber(1234.56)).toBe('1,234.56')
    })

    it('returns dash for null', () => {
      expect(formatNumber(null)).toBe('-')
    })

    it('returns original string for NaN', () => {
      expect(formatNumber('invalid')).toBe('invalid')
    })
  })

  describe('formatDateTime', () => {
    it('formats ISO datetime to YYYY-MM-DD HH:mm', () => {
      expect(formatDateTime('2026-03-29T14:30:00')).toBe('2026-03-29 14:30')
    })

    it('handles datetime with milliseconds', () => {
      expect(formatDateTime('2026-03-29T14:30:00.123Z')).toBe('2026-03-29 14:30')
    })

    it('returns dash for null', () => {
      expect(formatDateTime(null)).toBe('-')
    })

    it('returns original string if too short', () => {
      expect(formatDateTime('2026')).toBe('2026')
    })
  })
})
