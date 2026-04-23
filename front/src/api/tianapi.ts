import request from '@/utils/request'

export interface HotItem {
  word: string
  label?: string
  hotIndex?: number
  source?: string
  link?: string
  hotZh?: string
  position?: number
}

export const tianapi = {
  hotDouyin: () => request.post<HotItem[]>('/tianapi/hot/douyin'),
  hotToutiao: () => request.post<HotItem[]>('/tianapi/hot/toutiao'),
  hotWeibo: () => request.post<HotItem[]>('/tianapi/hot/weibo'),
  hotNetwork: () => request.post<HotItem[]>('/tianapi/hot/network'),
  status: () => request.post<{ enabled: boolean }>('/tianapi/status'),
}
