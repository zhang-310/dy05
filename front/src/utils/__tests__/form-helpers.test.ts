import { describe, it, expect } from 'vitest'
import { required, maxLength, numberRange } from '../form-helpers'

describe('required', () => {
  it('returns error for null', () => {
    expect(required(null)).toBe('必填')
  })

  it('returns error for undefined', () => {
    expect(required(undefined)).toBe('必填')
  })

  it('returns error for empty string', () => {
    expect(required('')).toBe('必填')
  })

  it('returns error for whitespace-only string', () => {
    expect(required('  ')).toBe('必填')
  })

  it('returns null for valid string', () => {
    expect(required('hello')).toBeNull()
  })

  it('uses custom message', () => {
    expect(required('', '请输入')).toBe('请输入')
  })
})

describe('maxLength', () => {
  it('returns null for string within limit', () => {
    const validate = maxLength(5)
    expect(validate('abc')).toBeNull()
  })

  it('returns null for string at limit', () => {
    const validate = maxLength(5)
    expect(validate('abcde')).toBeNull()
  })

  it('returns error for string exceeding limit', () => {
    const validate = maxLength(5)
    expect(validate('abcdef')).toBe('最多 5 个字符')
  })

  it('uses custom message', () => {
    const validate = maxLength(3, '太长了')
    expect(validate('abcd')).toBe('太长了')
  })
})

describe('numberRange', () => {
  it('returns null for number in range', () => {
    const validate = numberRange(1, 100)
    expect(validate('50')).toBeNull()
  })

  it('returns null for number at boundaries', () => {
    const validate = numberRange(1, 100)
    expect(validate('1')).toBeNull()
    expect(validate('100')).toBeNull()
  })

  it('returns error for number below range', () => {
    const validate = numberRange(1, 100)
    expect(validate('0')).toBe('请输入 1–100 之间的数字')
  })

  it('returns error for number above range', () => {
    const validate = numberRange(1, 100)
    expect(validate('101')).toBe('请输入 1–100 之间的数字')
  })

  it('returns error for non-numeric string', () => {
    const validate = numberRange(1, 100)
    expect(validate('abc')).toBe('请输入 1–100 之间的数字')
  })

  it('uses custom message', () => {
    const validate = numberRange(0, 10, '范围错误')
    expect(validate('11')).toBe('范围错误')
  })
})
