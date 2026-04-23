import { describe, it, expect } from 'vitest'
import { COMMON_STATUS, LIVE_STATUS } from '../status-chips'

describe('COMMON_STATUS', () => {
  it('has all expected statuses', () => {
    expect(Object.keys(COMMON_STATUS)).toEqual(
      expect.arrayContaining(['active', 'inactive', 'pending', 'error', 'draft', 'published', 'archived'])
    )
  })

  it('active is success color', () => {
    expect(COMMON_STATUS.active).toEqual({ label: '启用', color: 'success' })
  })

  it('error is error color', () => {
    expect(COMMON_STATUS.error).toEqual({ label: '异常', color: 'error' })
  })

  it('draft is default color', () => {
    expect(COMMON_STATUS.draft).toEqual({ label: '草稿', color: 'default' })
  })
})

describe('LIVE_STATUS', () => {
  it('has all expected statuses', () => {
    expect(Object.keys(LIVE_STATUS)).toEqual(
      expect.arrayContaining(['prepare', 'live', 'ended', 'cancelled'])
    )
  })

  it('live is error color (red indicator)', () => {
    expect(LIVE_STATUS.live).toEqual({ label: '直播中', color: 'error' })
  })

  it('cancelled is warning color', () => {
    expect(LIVE_STATUS.cancelled).toEqual({ label: '已取消', color: 'warning' })
  })
})
