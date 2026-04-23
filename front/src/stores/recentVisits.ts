import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface RecentVisitItem {
  path: string
  label: string
  at: number
}

export interface FavoriteNavItem {
  path: string
  label: string
}

interface RecentVisitsState {
  items: RecentVisitItem[]
  favorites: FavoriteNavItem[]
  push: (path: string, label: string) => void
  toggleFavorite: (path: string, label: string) => void
  removeFavorite: (path: string) => void
  isFavorite: (path: string) => boolean
}

export const useRecentVisitsStore = create<RecentVisitsState>()(
  persist(
    (set, get) => ({
      items: [],
      favorites: [],
      push(path, label) {
        if (!path || path === '/') return
        const next = get().items.filter((i) => i.path !== path)
        next.unshift({ path, label: label || path, at: Date.now() })
        set({ items: next.slice(0, 10) })
      },
      toggleFavorite(path, label) {
        if (!path || path === '/') return
        const favs = get().favorites
        const exists = favs.some((f) => f.path === path)
        if (exists) {
          set({ favorites: favs.filter((f) => f.path !== path) })
        } else {
          const next = favs.filter((f) => f.path !== path)
          next.unshift({ path, label: label || path })
          set({ favorites: next.slice(0, 30) })
        }
      },
      removeFavorite(path) {
        set({ favorites: get().favorites.filter((f) => f.path !== path) })
      },
      isFavorite(path) {
        return get().favorites.some((f) => f.path === path)
      },
    }),
    { name: 'dy-recent-visits-v2' }
  )
)
