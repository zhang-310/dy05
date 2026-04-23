import { describe, it, expect } from 'vitest'
import { pageHasRunningTask, statusToUi } from './taskQueueListFilters'

describe('pageHasRunningTask', () => {
  it('returns false for empty list', () => {
    expect(pageHasRunningTask(undefined)).toBe(false)
    expect(pageHasRunningTask([])).toBe(false)
  })

  it('returns true when any row is running', () => {
    expect(pageHasRunningTask([{ status: 1 }, { status: 2 }])).toBe(true)
    expect(pageHasRunningTask([{ status: 'running' }])).toBe(true)
  })

  it('returns false when no running row', () => {
    expect(pageHasRunningTask([{ status: 2 }, { status: 0 }])).toBe(false)
  })
})

describe('statusToUi', () => {
  it('maps running string to 1', () => {
    expect(statusToUi('running')).toBe(1)
  })
})
