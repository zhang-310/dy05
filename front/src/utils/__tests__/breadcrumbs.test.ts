import { describe, it, expect } from 'vitest'
import { getBreadcrumbs } from '../breadcrumbs'

describe('breadcrumbs', () => {
  describe('getBreadcrumbs', () => {
    it('returns only home for dashboard', () => {
      const result = getBreadcrumbs('/admin/dashboard')
      expect(result).toEqual([{ label: '首页' }])
    })

    it('returns breadcrumbs with group for auth pages', () => {
      const result = getBreadcrumbs('/admin/auth/users')
      expect(result).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '系统管理' },
        { label: '用户管理' }
      ])
    })

    it('returns breadcrumbs with group for live pages', () => {
      const result = getBreadcrumbs('/admin/live/sessions')
      expect(result).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: '直播场次' }
      ])
    })

    it('returns breadcrumbs with group for AI pages', () => {
      const result = getBreadcrumbs('/admin/ai/knowledge')
      expect(result).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: 'AI 中心' },
        { label: '知识库' }
      ])
    })

    it('returns only home and group for unknown route in known group', () => {
      const result = getBreadcrumbs('/admin/auth/unknown')
      expect(result).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '系统管理' }
      ])
    })

    it('returns only home for completely unknown route', () => {
      const result = getBreadcrumbs('/unknown/path')
      expect(result).toEqual([
        { label: '首页', path: '/admin/dashboard' }
      ])
    })
  })
})
