/**
 * auth 工具函数测试
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getToken, setToken, removeToken, isAuthenticated } from '../auth'

const TOKEN_KEY = 'token'

describe('auth utils', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('getToken', () => {
    it('returns null when no token', () => {
      expect(getToken()).toBeNull()
    })
    it('returns token from localStorage', () => {
      localStorage.setItem(TOKEN_KEY, 'abc123')
      expect(getToken()).toBe('abc123')
    })
  })

  describe('setToken', () => {
    it('stores token in localStorage', () => {
      setToken('xyz789')
      expect(localStorage.getItem(TOKEN_KEY)).toBe('xyz789')
    })
  })

  describe('removeToken', () => {
    it('removes token from localStorage', () => {
      localStorage.setItem(TOKEN_KEY, 'old')
      removeToken()
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    })
  })

  describe('isAuthenticated', () => {
    it('returns false when no token', () => {
      expect(isAuthenticated()).toBe(false)
    })
    it('returns true when token exists', () => {
      localStorage.setItem(TOKEN_KEY, 'any')
      expect(isAuthenticated()).toBe(true)
    })
  })
})
