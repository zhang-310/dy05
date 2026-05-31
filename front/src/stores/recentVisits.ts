import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { isSafeInternalReturnPath } from '@/utils/login-redirect'

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

export function isTrackableVisitPath(path: string): boolean {
  return isSafeInternalReturnPath(path) && path !== '/'
}

export function normalizeTrackableVisitPath(path: string): string | null {
  const trimmed = path.trim()
  if (!isTrackableVisitPath(trimmed)) return null

  let url: URL
  try {
    url = new URL(trimmed, 'http://dy.local')
  } catch {
    return null
  }

  const pathname = url.pathname.replace(/\/+$/, '') || '/'
  const tail = `${url.search}${url.hash}`
  if (pathname === '/admin/agent') return '/admin/ai/agent/list'
  if (pathname === '/admin/live/realtime') {
    const sessionId = Number(url.searchParams.get('sessionId') ?? '')
    if (Number.isFinite(sessionId) && sessionId > 0) return `/org/live/sessions/${sessionId}/realtime${url.hash}`
    return '/org/live/sessions'
  }
  if (pathname === '/admin/shortvideo/seo') return `/talent/shortvideo/seo-optimize${tail}`
  if (pathname === '/admin/shortvideo/viral-analysis') return `/admin/ai/viral-analysis${tail}`
  if (pathname === '/admin/shortvideo' || pathname.startsWith('/admin/shortvideo/')) {
    return `/talent${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/live' || pathname.startsWith('/admin/live/')) {
    return `/org${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/product' || pathname.startsWith('/admin/product/')) {
    return `/org${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/script' || pathname.startsWith('/admin/script/')) {
    return `/org${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/copy' || pathname.startsWith('/admin/copy/')) {
    return `/org${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/content' || pathname.startsWith('/admin/content/')) {
    return `/org${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/slangdict' || pathname.startsWith('/admin/slangdict/')) {
    return `/org${pathname.slice('/admin'.length)}${tail}`
  }
  if (pathname === '/admin/douyin' || pathname.startsWith('/admin/douyin/')) {
    return `/talent${pathname.slice('/admin'.length)}${tail}`
  }
  const subtitleEditorMatch = pathname.match(/^\/admin\/shortvideo\/subtitle-editor\/([^/]+)$/)
  if (subtitleEditorMatch) {
    const videoId = Number(subtitleEditorMatch[1])
    if (!Number.isFinite(videoId) || videoId <= 0) return '/talent/shortvideo/subtitles'
  }

  if (pathname === '/org/content/library' || pathname.startsWith('/org/ai/') || pathname.startsWith('/org/agent') || pathname.startsWith('/org/product/')) {
    return '/org/dashboard'
  }

  if (pathname === '/talent/content/library' || pathname.startsWith('/talent/ai/') || pathname.startsWith('/talent/agent') || pathname.startsWith('/talent/product/')) {
    return '/talent/dashboard'
  }

  if (pathname === '/org/shortvideo' || pathname.startsWith('/org/shortvideo/')) return '/org/dashboard'
  if (pathname === '/talent/shortvideo/dashboard' || pathname.startsWith('/talent/shortvideo/dashboard/')) return '/talent/shortvideo'
  if (pathname === '/user/shortvideo/dashboard' || pathname.startsWith('/user/shortvideo/dashboard/')) return '/user/shortvideo'

  return trimmed
}

function sanitizeRecentItems(items: unknown): RecentVisitItem[] {
  if (!Array.isArray(items)) return []
  const deduped: RecentVisitItem[] = []
  for (const item of items) {
    if (typeof item !== 'object' || item === null) continue
    const row = item as Partial<RecentVisitItem>
    if (typeof row.path !== 'string') continue
    const path = normalizeTrackableVisitPath(row.path)
    if (!path || deduped.some((existing) => existing.path === path)) continue
    deduped.push({
      path,
      label: typeof item.label === 'string' && item.label ? item.label : item.path,
      at: Number.isFinite(Number(item.at)) ? Number(item.at) : Date.now(),
    })
    if (deduped.length >= 10) break
  }
  return deduped
}

function sanitizeFavorites(items: unknown): FavoriteNavItem[] {
  if (!Array.isArray(items)) return []
  const deduped: FavoriteNavItem[] = []
  for (const item of items) {
    if (typeof item !== 'object' || item === null) continue
    const row = item as Partial<FavoriteNavItem>
    if (typeof row.path !== 'string') continue
    const path = normalizeTrackableVisitPath(row.path)
    if (!path || deduped.some((existing) => existing.path === path)) continue
    deduped.push({
      path,
      label: typeof item.label === 'string' && item.label ? item.label : item.path,
    })
    if (deduped.length >= 30) break
  }
  return deduped
}

export const useRecentVisitsStore = create<RecentVisitsState>()(
  persist(
    (set, get) => ({
      items: [],
      favorites: [],
      push(path, label) {
        const normalizedPath = normalizeTrackableVisitPath(path)
        if (!normalizedPath) return
        const next = sanitizeRecentItems(get().items).filter((i) => i.path !== normalizedPath)
        next.unshift({ path: normalizedPath, label: label || normalizedPath, at: Date.now() })
        set({ items: next.slice(0, 10) })
      },
      toggleFavorite(path, label) {
        const normalizedPath = normalizeTrackableVisitPath(path)
        if (!normalizedPath) return
        const favs = sanitizeFavorites(get().favorites)
        const exists = favs.some((f) => f.path === normalizedPath)
        if (exists) {
          set({ favorites: favs.filter((f) => f.path !== normalizedPath) })
        } else {
          const next = favs.filter((f) => f.path !== normalizedPath)
          next.unshift({ path: normalizedPath, label: label || normalizedPath })
          set({ favorites: next.slice(0, 30) })
        }
      },
      removeFavorite(path) {
        const normalizedPath = normalizeTrackableVisitPath(path)
        if (!normalizedPath) return
        set({ favorites: sanitizeFavorites(get().favorites).filter((f) => f.path !== normalizedPath) })
      },
      isFavorite(path) {
        const normalizedPath = normalizeTrackableVisitPath(path)
        return !!normalizedPath && sanitizeFavorites(get().favorites).some((f) => f.path === normalizedPath)
      },
    }),
    {
      name: 'dy-recent-visits-v2',
      merge: (persisted, current) => {
        const state = typeof persisted === 'object' && persisted !== null
          ? persisted as Partial<RecentVisitsState>
          : {}
        return {
          ...current,
          items: sanitizeRecentItems(state.items),
          favorites: sanitizeFavorites(state.favorites),
        }
      },
    }
  )
)
