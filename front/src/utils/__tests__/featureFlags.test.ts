import { describe, it, expect, beforeEach, vi } from 'vitest'
import { isFeatureEnabled, setFeatureFlag, FLAGS } from '../featureFlags'

describe('featureFlags', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('FLAGS', () => {
    it('has STAGE_WORKFLOW flag', () => {
      expect(FLAGS.STAGE_WORKFLOW).toBe('stage_workflow')
    })
  })

  describe('isFeatureEnabled', () => {
    it('returns true for STAGE_WORKFLOW by default (enabled by default)', () => {
      expect(isFeatureEnabled(FLAGS.STAGE_WORKFLOW)).toBe(true)
    })

    it('returns false for unknown flags', () => {
      expect(isFeatureEnabled('nonexistent')).toBe(false)
    })

    it('returns true when explicitly enabled', () => {
      localStorage.setItem('ff_test_flag', '1')
      expect(isFeatureEnabled('test_flag')).toBe(true)
    })

    it('returns false when explicitly disabled (removed)', () => {
      // For a default-enabled flag, setting it to anything but '1' disables
      localStorage.setItem('ff_stage_workflow', '0')
      expect(isFeatureEnabled(FLAGS.STAGE_WORKFLOW)).toBe(false)
    })

    it('handles localStorage errors gracefully', () => {
      const spy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('quota exceeded')
      })
      // Falls back to ENABLED_BY_DEFAULT check
      expect(isFeatureEnabled(FLAGS.STAGE_WORKFLOW)).toBe(true)
      expect(isFeatureEnabled('nonexistent')).toBe(false)
      spy.mockRestore()
    })
  })

  describe('setFeatureFlag', () => {
    it('enables a flag', () => {
      setFeatureFlag('my_flag', true)
      expect(localStorage.getItem('ff_my_flag')).toBe('1')
    })

    it('disables a flag by removing from localStorage', () => {
      localStorage.setItem('ff_my_flag', '1')
      setFeatureFlag('my_flag', false)
      expect(localStorage.getItem('ff_my_flag')).toBeNull()
    })

    it('handles localStorage errors gracefully', () => {
      const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('quota exceeded')
      })
      // Should not throw
      expect(() => setFeatureFlag('test', true)).not.toThrow()
      spy.mockRestore()
    })
  })
})
