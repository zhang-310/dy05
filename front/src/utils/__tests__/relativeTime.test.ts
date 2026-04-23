/**
 * relativeTime 工具函数测试
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { formatRelativeTime } from '../relativeTime'

describe('relativeTime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-18T12:00:00Z'))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns 刚刚 for < 1 min ago', () => {
    const d = new Date('2026-03-18T11:59:30Z')
    expect(formatRelativeTime(d)).toBe('刚刚')
  })

  it('returns 分钟前 for < 1 hour', () => {
    const d = new Date('2026-03-18T11:30:00Z')
    expect(formatRelativeTime(d)).toContain('分钟前')
  })

  it('returns 小时前 for < 24 hours', () => {
    const d = new Date('2026-03-18T10:00:00Z')
    expect(formatRelativeTime(d)).toContain('小时前')
  })

  it('returns 天前 for < 7 days', () => {
    const d = new Date('2026-03-16T12:00:00Z')
    expect(formatRelativeTime(d)).toContain('天前')
  })

  it('accepts string date', () => {
    expect(formatRelativeTime('2026-03-18T11:59:00Z')).toBeDefined()
  })

  it('accepts number timestamp', () => {
    expect(formatRelativeTime(Date.now() - 60000)).toBeDefined()
  })
})
