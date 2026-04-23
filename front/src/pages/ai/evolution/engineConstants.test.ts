import { describe, it, expect } from 'vitest'
import { parseScopeKbId } from './engineConstants'

describe('parseScopeKbId', () => {
  it('returns undefined for empty string', () => {
    expect(parseScopeKbId('')).toBeUndefined()
  })

  it('returns finite number for numeric string', () => {
    expect(parseScopeKbId('42')).toBe(42)
  })

  it('returns undefined for non-numeric string', () => {
    expect(parseScopeKbId('abc')).toBeUndefined()
  })
})
