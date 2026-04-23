import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useRecentVisitsStore } from '../recentVisits'

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

    it('uses path as label if label is empty', () => {
      const { push } = useRecentVisitsStore.getState()

      push('/admin/test', '')

      const state = useRecentVisitsStore.getState()
      expect(state.items[0].label).toBe('/admin/test')
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
  })
})
