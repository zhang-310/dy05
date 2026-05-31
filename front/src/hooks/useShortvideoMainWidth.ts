import { useCallback, useMemo, useSyncExternalStore } from 'react'

const STORAGE_KEY = 'shortvideo.mainContentMax'
const CHANGE_EVENT = 'shortvideo-main-width'

/** 预设与「全宽」；默认 1440 适合大屏阅读区 */
export const SHORTVIDEO_WIDTH_OPTIONS = [
  { value: 'full', label: '全宽' },
  { value: '1920', label: '1920' },
  { value: '1680', label: '1680' },
  { value: '1536', label: '1536' },
  { value: '1440', label: '1440' },
  { value: '1280', label: '1280' },
  { value: '1120', label: '1120' },
  { value: '1024', label: '1024' },
] as const

export type ShortvideoMainWidthToken = (typeof SHORTVIDEO_WIDTH_OPTIONS)[number]['value']

const ALLOWED = new Set<string>(SHORTVIDEO_WIDTH_OPTIONS.map(o => o.value))

export function normalizeShortvideoMainWidth(raw: string | null): ShortvideoMainWidthToken {
  if (raw && ALLOWED.has(raw)) return raw as ShortvideoMainWidthToken
  return '1440'
}

export function tokenToMaxWidth(token: ShortvideoMainWidthToken): 'full' | number {
  if (token === 'full') return 'full'
  return Number(token)
}

function getSnapshot(): string {
  if (typeof window === 'undefined') return '1440'
  return localStorage.getItem(STORAGE_KEY) ?? '1440'
}

function subscribe(onStoreChange: () => void) {
  const onStorage = (e: StorageEvent) => {
    if (e.key === STORAGE_KEY || e.key === null) onStoreChange()
  }
  const onCustom = () => onStoreChange()
  window.addEventListener('storage', onStorage)
  window.addEventListener(CHANGE_EVENT, onCustom)
  return () => {
    window.removeEventListener('storage', onStorage)
    window.removeEventListener(CHANGE_EVENT, onCustom)
  }
}

function getServerSnapshot() {
  return '1440'
}

export function setShortvideoMainWidth(token: ShortvideoMainWidthToken) {
  localStorage.setItem(STORAGE_KEY, normalizeShortvideoMainWidth(token))
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

export function useShortvideoMainWidth() {
  const raw = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
  const selectValue = useMemo(() => normalizeShortvideoMainWidth(raw), [raw])
  const maxWidth = useMemo(() => tokenToMaxWidth(selectValue), [selectValue])
  const setWidth = useCallback((token: ShortvideoMainWidthToken) => {
    setShortvideoMainWidth(token)
  }, [])
  return { maxWidth, selectValue, setWidth }
}
