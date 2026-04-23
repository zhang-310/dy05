/**
 * pageResult 工具函数测试
 */
import { describe, it, expect } from 'vitest'
import { normalizePageResult } from '../pageResult'

describe('pageResult', () => {
  describe('normalizePageResult', () => {
    it('returns empty for null/undefined', () => {
      expect(normalizePageResult(null)).toEqual({ total: 0, list: [], pageNum: 0, pageSize: 30 })
      expect(normalizePageResult(undefined)).toEqual({ total: 0, list: [], pageNum: 0, pageSize: 30 })
    })
    it('returns empty for non-object', () => {
      expect(normalizePageResult('')).toEqual({ total: 0, list: [], pageNum: 0, pageSize: 30 })
      expect(normalizePageResult(123)).toEqual({ total: 0, list: [], pageNum: 0, pageSize: 30 })
    })
    it('normalizes valid response', () => {
      expect(normalizePageResult({ total: 5, list: [{ id: 1 }] })).toEqual({
        total: 5,
        list: [{ id: 1 }],
        pageNum: 0,
        pageSize: 30,
      })
    })
    it('handles missing list', () => {
      expect(normalizePageResult({ total: 10 })).toEqual({ total: 10, list: [], pageNum: 0, pageSize: 30 })
    })
    it('handles non-array list', () => {
      expect(normalizePageResult({ total: 1, list: 'invalid' })).toEqual({
        total: 1,
        list: [],
        pageNum: 0,
        pageSize: 30,
      })
    })
    it('normalizes numeric strings and records fallback', () => {
      expect(normalizePageResult({ total: '7', records: [{ id: 9 }], pageNum: '2', pageSize: '50' })).toEqual({
        total: 7,
        list: [{ id: 9 }],
        pageNum: 2,
        pageSize: 50,
      })
    })
  })
})
