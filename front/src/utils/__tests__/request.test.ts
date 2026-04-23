/**
 * request 测试：拦截器、错误处理、Token 刷新
 */
import { describe, it, expect } from 'vitest'
import request, { isAuthBusinessCode } from '../request'

describe('request', () => {
  it('exports request wrapper', () => {
    expect(request).toBeDefined()
    expect(typeof request.get).toBe('function')
    expect(typeof request.post).toBe('function')
    expect(typeof request.delete).toBe('function')
  })

  it('identifies auth business error codes from backend RESTResult', () => {
    expect(isAuthBusinessCode(2001)).toBe(true)
    expect(isAuthBusinessCode(2002)).toBe(true)
    expect(isAuthBusinessCode(2003)).toBe(true)
    expect(isAuthBusinessCode(200)).toBe(false)
    expect(isAuthBusinessCode(1002)).toBe(false)
    expect(isAuthBusinessCode('2001')).toBe(false)
  })
})
