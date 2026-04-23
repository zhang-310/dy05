import { describe, it, expect } from 'vitest'
import { formatDate, formatDateShort, formatDateOnly } from '../date'

describe('date', () => {
  describe('formatDate', () => {
    it('formats ISO datetime to YYYY-MM-DD HH:mm:ss', () => {
      expect(formatDate('2024-03-15T14:30:45.123Z')).toBe('2024-03-15 14:30:45')
    })

    it('returns dash for null', () => {
      expect(formatDate(null)).toBe('-')
    })

    it('returns dash for undefined', () => {
      expect(formatDate(undefined)).toBe('-')
    })

    it('returns dash for empty string', () => {
      expect(formatDate('')).toBe('-')
    })
  })

  describe('formatDateShort', () => {
    it('formats ISO datetime to YYYY-MM-DD HH:mm', () => {
      expect(formatDateShort('2024-03-15T14:30:45.123Z')).toBe('2024-03-15 14:30')
    })

    it('returns dash for null', () => {
      expect(formatDateShort(null)).toBe('-')
    })

    it('returns dash for undefined', () => {
      expect(formatDateShort(undefined)).toBe('-')
    })
  })

  describe('formatDateOnly', () => {
    it('formats ISO datetime to YYYY-MM-DD', () => {
      expect(formatDateOnly('2024-03-15T14:30:45.123Z')).toBe('2024-03-15')
    })

    it('returns dash for null', () => {
      expect(formatDateOnly(null)).toBe('-')
    })

    it('returns dash for undefined', () => {
      expect(formatDateOnly(undefined)).toBe('-')
    })
  })
})
