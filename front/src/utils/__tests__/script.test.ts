/**
 * script 工具函数测试
 */
import { describe, it, expect } from 'vitest'
import {
  estimateDuration,
  estimateDurationFromText,
  formatDurationSec,
} from '../script'

describe('script utils', () => {
  describe('estimateDuration', () => {
    it('estimates duration from word count', () => {
      expect(estimateDuration(9)).toBe(3) // 9 words / 3 = 3 seconds
      expect(estimateDuration(15)).toBe(5) // 15 words / 3 = 5 seconds
      expect(estimateDuration(10)).toBe(4) // 10 words / 3 = 3.33, ceil to 4
    })
  })

  describe('estimateDurationFromText', () => {
    it('returns 0 for null/undefined/empty', () => {
      expect(estimateDurationFromText(null)).toBe(0)
      expect(estimateDurationFromText(undefined)).toBe(0)
      expect(estimateDurationFromText('')).toBe(0)
    })
    it('estimates from text (ignoring spaces)', () => {
      expect(estimateDurationFromText('你好世界')).toBe(2) // 4 chars / 3 = 1.33, ceil to 2
      expect(estimateDurationFromText('你好 世界')).toBe(2) // spaces ignored, still 4 chars
    })
  })

  describe('formatDurationSec', () => {
    it('formats seconds to mm:ss', () => {
      expect(formatDurationSec(0)).toBe('00:00')
      expect(formatDurationSec(30)).toBe('00:30')
      expect(formatDurationSec(90)).toBe('01:30')
      expect(formatDurationSec(125)).toBe('02:05')
    })
  })
})
