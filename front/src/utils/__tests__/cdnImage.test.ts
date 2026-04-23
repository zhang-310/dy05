/**
 * cdnImage 工具函数测试
 */
import { describe, it, expect } from 'vitest'
import { cdnThumb } from '../cdnImage'

describe('cdnImage', () => {
  describe('cdnThumb', () => {
    it('returns empty for null/undefined', () => {
      expect(cdnThumb(null, 80, 80)).toBe('')
      expect(cdnThumb(undefined, 80, 80)).toBe('')
    })

    it('appends @!wXh for valid url', () => {
      expect(cdnThumb('https://cdn.example.com/img.jpg', 80, 80)).toBe(
        'https://cdn.example.com/img.jpg@!80X80'
      )
      expect(cdnThumb('https://cdn.example.com/img.jpg', 300, 250)).toBe(
        'https://cdn.example.com/img.jpg@!300X250'
      )
    })

    it('does not duplicate @! when already present', () => {
      const url = 'https://cdn.example.com/img.jpg@!80X80'
      expect(cdnThumb(url, 300, 250)).toBe(url)
    })
  })
})
