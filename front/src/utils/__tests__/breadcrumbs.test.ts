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

    it('labels shortvideo SEO legacy and canonical paths consistently', () => {
      expect(getBreadcrumbs('/admin/shortvideo/seo')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: 'SEO 优化' },
      ])
      expect(getBreadcrumbs('/admin/shortvideo/seo-optimize')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: 'SEO 优化' },
      ])
    })

    it('labels the entity-free subtitle tool entry separately from concrete subtitle details', () => {
      expect(getBreadcrumbs('/admin/shortvideo/subtitles')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: '字幕编辑' },
      ])
      expect(getBreadcrumbs('/admin/shortvideo/subtitle-editor/18')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: '字幕编辑' },
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

    it('returns dynamic route labels for detail pages', () => {
      expect(getBreadcrumbs('/admin/ai/agent/chat/18?tab=run')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: 'AI 中心' },
        { label: '智能体对话' },
      ])
      expect(getBreadcrumbs('/admin/shortvideo/benchmark/quality-scripts/9')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: '质量脚本详情' },
      ])
      expect(getBreadcrumbs('/admin/live/sessions/7/realtime')).toEqual([
        { label: '首页', path: '/admin/dashboard' },
        { label: '运营管理' },
        { label: '实时面板' },
      ])
    })

    it('keeps role shell dashboards inside their own breadcrumb scope', () => {
      expect(getBreadcrumbs('/org/dashboard')).toEqual([
        { label: '机构' },
        { label: '工作台' },
      ])
      expect(getBreadcrumbs('/talent/dashboard')).toEqual([
        { label: '达人' },
        { label: '工作台' },
      ])
      expect(getBreadcrumbs('/org/live/sessions/6')).toEqual([
        { label: '机构首页', path: '/org/dashboard' },
        { label: '机构' },
        { label: '机构场次工作台' },
      ])
      expect(getBreadcrumbs('/talent/live/sessions/6')).toEqual([
        { label: '达人首页', path: '/talent/dashboard' },
        { label: '达人' },
        { label: '达人场次工作台' },
      ])
      expect(getBreadcrumbs('/org/live/sessions')).toEqual([
        { label: '机构首页', path: '/org/dashboard' },
        { label: '机构' },
        { label: '机构直播场次' },
      ])
    })
  })
})
