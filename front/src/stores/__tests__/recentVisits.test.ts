import { describe, it, expect, beforeEach, vi } from 'vitest'
import { isTrackableVisitPath, normalizeTrackableVisitPath, useRecentVisitsStore } from '../recentVisits'

describe('recentVisits', () => {
  beforeEach(() => {
    useRecentVisitsStore.setState({ items: [], favorites: [] })
    vi.clearAllMocks()
  })

  describe('push', () => {
    it('adds new visit to items', () => {
      const { push } = useRecentVisitsStore.getState()

      push('/admin/products', 'Products')

      const state = useRecentVisitsStore.getState()
      expect(state.items).toHaveLength(1)
      expect(state.items[0]).toMatchObject({
        path: '/admin/products',
        label: 'Products',
      })
      expect(state.items[0].at).toBeGreaterThan(0)
    })

    it('moves existing path to front', () => {
      const { push } = useRecentVisitsStore.getState()

      push('/admin/products', 'Products')
      push('/admin/live', 'Live')
      push('/admin/products', 'Products')

      const state = useRecentVisitsStore.getState()
      expect(state.items).toHaveLength(2)
      expect(state.items[0].path).toBe('/admin/products')
      expect(state.items[1].path).toBe('/admin/live')
    })

    it('limits items to 10', () => {
      const { push } = useRecentVisitsStore.getState()

      for (let i = 0; i < 15; i++) {
        push(`/admin/page${i}`, `Page ${i}`)
      }

      const state = useRecentVisitsStore.getState()
      expect(state.items).toHaveLength(10)
    })

    it('ignores empty path', () => {
      const { push } = useRecentVisitsStore.getState()

      push('', 'Empty')

      const state = useRecentVisitsStore.getState()
      expect(state.items).toHaveLength(0)
    })

    it('ignores root path', () => {
      const { push } = useRecentVisitsStore.getState()

      push('/', 'Root')

      const state = useRecentVisitsStore.getState()
      expect(state.items).toHaveLength(0)
    })

    it('ignores unsafe external and login paths', () => {
      const { push } = useRecentVisitsStore.getState()

      push('https://evil.example/admin', 'External')
      push('//evil.example/admin', 'Protocol relative')
      push('/login?returnUrl=%2Fadmin%2Fdashboard', 'Login')

      const state = useRecentVisitsStore.getState()
      expect(state.items).toHaveLength(0)
    })

    it('uses path as label if label is empty', () => {
      const { push } = useRecentVisitsStore.getState()

      push('/admin/test', '')

      const state = useRecentVisitsStore.getState()
      expect(state.items[0].label).toBe('/admin/test')
    })

    it('stores normalized stale paths instead of dead internal links', () => {
      const { push } = useRecentVisitsStore.getState()

      push('/admin/agent', '旧智能体')
      push('/admin/live/realtime?sessionId=18', '旧实时面板')
      push('/org/content/library', '旧机构内容库')

      const paths = useRecentVisitsStore.getState().items.map((item) => item.path)
      expect(paths).toEqual(['/org/dashboard', '/admin/live/sessions/18/realtime', '/admin/ai/agent/list'])
    })
  })

  describe('isTrackableVisitPath', () => {
    it('accepts only safe non-root internal paths', () => {
      expect(isTrackableVisitPath('/admin/dashboard')).toBe(true)
      expect(isTrackableVisitPath('/')).toBe(false)
      expect(isTrackableVisitPath('/login')).toBe(false)
      expect(isTrackableVisitPath('//evil.example/admin')).toBe(false)
      expect(isTrackableVisitPath('https://evil.example/admin')).toBe(false)
    })

    it('normalizes known stale internal routes to registered routes', () => {
      expect(normalizeTrackableVisitPath('/admin/agent')).toBe('/admin/ai/agent/list')
      expect(normalizeTrackableVisitPath('/admin/live/realtime?sessionId=18')).toBe('/admin/live/sessions/18/realtime')
      expect(normalizeTrackableVisitPath('/admin/live/realtime')).toBe('/admin/live/sessions')
      expect(normalizeTrackableVisitPath('/admin/shortvideo/seo?keyword=abc')).toBe('/admin/shortvideo/seo-optimize?keyword=abc')
      expect(normalizeTrackableVisitPath('/admin/shortvideo/viral-analysis')).toBe('/admin/ai/viral-analysis')
      expect(normalizeTrackableVisitPath('/admin/shortvideo/subtitle-editor/0')).toBe('/admin/shortvideo/subtitles')
      expect(normalizeTrackableVisitPath('/admin/shortvideo/subtitle-editor/not-a-video')).toBe('/admin/shortvideo/subtitles')
      expect(normalizeTrackableVisitPath('/admin/shortvideo/subtitle-editor/18')).toBe('/admin/shortvideo/subtitle-editor/18')
      expect(normalizeTrackableVisitPath('/org/content/library')).toBe('/org/dashboard')
      expect(normalizeTrackableVisitPath('/talent/ai/knowledge')).toBe('/talent/dashboard')
      expect(normalizeTrackableVisitPath('/talent/shortvideo/dashboard')).toBe('/talent/shortvideo')
      expect(normalizeTrackableVisitPath('/login?returnUrl=%2Fadmin%2Fdashboard')).toBeNull()
    })
  })

  describe('toggleFavorite', () => {
    it('adds new favorite', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/products', 'Products')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(1)
      expect(state.favorites[0]).toEqual({
        path: '/admin/products',
        label: 'Products',
      })
    })

    it('removes existing favorite', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/products', 'Products')
      toggleFavorite('/admin/products', 'Products')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(0)
    })

    it('limits favorites to 30', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      for (let i = 0; i < 35; i++) {
        toggleFavorite(`/admin/page${i}`, `Page ${i}`)
      }

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(30)
    })

    it('ignores empty path', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('', 'Empty')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(0)
    })

    it('ignores root path', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/', 'Root')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(0)
    })

    it('ignores unsafe external and login paths', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('https://evil.example/admin', 'External')
      toggleFavorite('//evil.example/admin', 'Protocol relative')
      toggleFavorite('/login', 'Login')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(0)
    })

    it('normalizes stale favorite paths and toggles by normalized route', () => {
      const { toggleFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/agent', '旧智能体')
      expect(useRecentVisitsStore.getState().favorites).toEqual([
        { path: '/admin/ai/agent/list', label: '旧智能体' },
      ])

      toggleFavorite('/admin/ai/agent/list', '智能体列表')
      expect(useRecentVisitsStore.getState().favorites).toEqual([])
    })
  })

  describe('removeFavorite', () => {
    it('removes favorite by path', () => {
      const { toggleFavorite, removeFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/products', 'Products')
      toggleFavorite('/admin/live', 'Live')
      removeFavorite('/admin/products')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(1)
      expect(state.favorites[0].path).toBe('/admin/live')
    })

    it('does nothing for non-existent path', () => {
      const { toggleFavorite, removeFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/products', 'Products')
      removeFavorite('/admin/non-existent')

      const state = useRecentVisitsStore.getState()
      expect(state.favorites).toHaveLength(1)
    })
  })

  describe('isFavorite', () => {
    it('returns true for favorited path', () => {
      const { toggleFavorite, isFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/products', 'Products')

      expect(isFavorite('/admin/products')).toBe(true)
    })

    it('returns false for non-favorited path', () => {
      const { isFavorite } = useRecentVisitsStore.getState()

      expect(isFavorite('/admin/products')).toBe(false)
    })

    it('checks favorite state through normalized route aliases', () => {
      const { toggleFavorite, isFavorite } = useRecentVisitsStore.getState()

      toggleFavorite('/admin/live/realtime?sessionId=18', '旧实时面板')

      expect(isFavorite('/admin/live/sessions/18/realtime')).toBe(true)
      expect(isFavorite('/admin/live/realtime?sessionId=18')).toBe(true)
    })
  })
})
