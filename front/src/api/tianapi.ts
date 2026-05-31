import request from '@/utils/request'
import { isRecord, normalizeArray, parseJsonValue } from '@/utils/response-normalize'

export interface HotItem {
  word: string
  label?: string
  hotIndex?: number
  source?: string
  link?: string
  hotZh?: string
  position?: number
}

function toNumber(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  const n = typeof value === 'number' ? value : Number(String(value).replace(/[^\d.-]/g, ''))
  return Number.isFinite(n) ? n : undefined
}

function mapHotItem(raw: unknown): HotItem {
  const item = isRecord(raw) ? raw : {}
  return {
    word: String(item.word ?? item.title ?? item.name ?? ''),
    label: item.label == null ? undefined : String(item.label),
    hotIndex: toNumber(item.hotIndex ?? item.hotValue ?? item.heat),
    source: item.source == null ? undefined : String(item.source),
    link: item.link == null && item.url == null ? undefined : String(item.link ?? item.url),
    hotZh: item.hotZh == null ? undefined : String(item.hotZh),
    position: toNumber(item.position ?? item.rank),
  }
}

function normalizeHotList(raw: unknown): HotItem[] {
  return normalizeArray<unknown>(raw).map(mapHotItem).filter(item => item.word.trim().length > 0)
}

function normalizeStatus(raw: unknown): { enabled: boolean } {
  const value = parseJsonValue(raw)
  const record = isRecord(value) && isRecord(value.data) ? value.data : isRecord(value) ? value : {}
  return { enabled: Boolean(record.enabled ?? record.configured ?? record.available) }
}

export const tianapi = {
  hotDouyin: () => request.post<unknown>('/tianapi/hot/douyin').then(normalizeHotList),
  hotToutiao: () => request.post<unknown>('/tianapi/hot/toutiao').then(normalizeHotList),
  hotWeibo: () => request.post<unknown>('/tianapi/hot/weibo').then(normalizeHotList),
  hotNetwork: () => request.post<unknown>('/tianapi/hot/network').then(normalizeHotList),
  status: () => request.post<unknown>('/tianapi/status').then(normalizeStatus),
}
