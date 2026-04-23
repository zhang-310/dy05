/**
 * error-codes 工具函数测试
 */
import { describe, it, expect } from 'vitest'
import { getErrorMessage } from '../error-codes'

describe('error-codes', () => {
  it('getErrorMessage returns message for known code', () => {
    const msg = getErrorMessage(2001)
    expect(msg).toBeDefined()
    expect(typeof msg).toBe('string')
  })

  it('getErrorMessage returns fallback for unknown code', () => {
    const msg = getErrorMessage(99999)
    expect(msg).toBeDefined()
    expect(typeof msg).toBe('string')
  })

  it('getErrorMessage handles 200 success', () => {
    const msg = getErrorMessage(200)
    expect(msg).toBeDefined()
  })
})
