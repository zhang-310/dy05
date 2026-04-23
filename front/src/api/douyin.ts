import request from '@/utils/request'
import type { PageResult } from '@/types/common'

// Account
export interface DyAccount {
  id: number; userId: number; accountName: string; accountId: string
  followCount: number; fanCount: number; videoCount: number; totalLikes: number
  description: string; status: number; createTime: string; updateTime: string
  authStatus?: 'valid' | 'expired' | 'unbound'
  lastSyncTime?: string
}
export interface DyAccountQuery { page?: number; rows?: number; accountName?: string; accountId?: string; status?: number; authStatus?: string }
export interface DyAccountSave { id?: number; userId?: number; accountName: string; accountId: string; followCount?: number; fanCount?: number; videoCount?: number; totalLikes?: number; description?: string; status: number }
export interface DyAccountStats { accountId: string; totalVideos: number; totalViews: number; totalLikes: number; totalShares: number; totalComments: number; avgViewsPerVideo: number; avgLikesPerVideo: number }

// Persona
export interface DyPersona {
  id: number; accountId: number; personaName: string; personaType: string
  description: string; tone: string; targetAudience: string; contentStyle: string
  keywords: string; isDefault: number; status: number; interactionStyle: string
  languageStyle: string; contentRatio: string; localFlavor: string; liveStyle: string
  personaTraits: string; ipType: string; ageRange: string; positioningTags: string
  createTime: string; updateTime: string
}
export interface DyPersonaSave {
  id?: number; accountId?: number; personaName: string; personaType: string
  description?: string; tone?: string; targetAudience?: string; contentStyle?: string
  keywords?: string; isDefault?: number
}

// Video
export interface DyVideo {
  id: number; accountId: number; videoId: string; title: string; description: string
  viewCount: number; likeCount: number; shareCount: number; commentCount: number
  downloadCount: number; videoType: string; publishTime: string; createTime: string
}
export interface DyVideoQuery { page?: number; rows?: number; accountId?: number; keyword?: string; videoType?: string }
export interface DyVideoSave { id?: number; accountId: number; videoId: string; title: string; description?: string; videoType?: string }

// Fan Profile
export interface DyFanProfile {
  id: number; accountId: number; ageRange: string; gender: string
  region: string; interests: string; createTime: string
  maleRatio?: number; femaleRatio?: number
}
export interface DyFanProfileStats { date: string; fanCount: number; newFans: number; lostFans: number; ageRange?: string; ratio?: number }

export const douyinApi = {
  accountList: (p: DyAccountQuery) => request.post<PageResult<DyAccount>>('/douyin/account/search', p),
  accountGet: (id: number) => request.post<DyAccount>('/douyin/account/get', { id }),
  accountSave: (p: Partial<DyAccountSave>) => request.post<void>('/douyin/account/save', p),
  accountDelete: (id: number) => request.post<void>('/douyin/account/delete', { id }),
  accountStats: (id: number) => request.post<DyAccountStats>('/douyin/account/statistics', { id }),

  personaList: (personaType?: string) => request.post<DyPersona[]>('/douyin/persona/list', { personaType }),
  personaGet: (id: number) => request.post<DyPersona>('/douyin/persona/get', { id }),
  personaSave: (p: Partial<DyPersonaSave>) => request.post<void>('/douyin/persona/save', p),
  personaDelete: (id: number) => request.post<void>('/douyin/persona/delete', { id }),
  personaSetDefault: (id: number) => request.post<void>('/douyin/persona/set-default', { id }),
  personaGetDefault: () => request.post<DyPersona>('/douyin/persona/get-default', {}),
  personaGetByAccount: (accountId: number) => request.post<DyPersona | null>('/douyin/persona/get-by-account', { accountId }),
  personaTemplates: () => request.post<DyPersona[]>('/douyin/persona/templates', {}),

  // 视频
  videoSearch: (p: DyVideoQuery) => request.post<import('@/types/common').PageResult<DyVideo>>('/douyin/video/search', p),
  videoGet: (id: number) => request.post<DyVideo>('/douyin/video/get', { id }),
  videoSave: (p: Partial<DyVideoSave>) => request.post<number>('/douyin/video/save', p),
  videoSync: (id: number) => request.post<void>('/douyin/video/sync', { id }),

  // 粉丝画像
  fanProfileGet: (accountId: number) => request.post<DyFanProfile>('/douyin/fan-profile/get', { accountId }),
  fanProfileStats: (accountId: number) => request.post<DyFanProfileStats[]>('/douyin/fan-profile/stats', { accountId }),
  fanProfileSync: (accountId: number) => request.post<void>(`/douyin/fan-profile/sync/${accountId}`, {}),

  // OAuth 授权（P0）
  oauthUrl: (accountId: number) =>
    request.post<{ authUrl: string }>('/douyin/oauth/auth-url', { accountId }),
  tokenStatus: (accountId: number) =>
    request.post<{ status: 'valid' | 'expired' | 'refreshing'; expireTime?: string; daysLeft?: number }>('/douyin/oauth/token-status', { accountId }),
  tokenRefresh: (accountId: number) =>
    request.post<void>('/douyin/oauth/token-refresh', { accountId }),
  oauthRevoke: (accountId: number) =>
    request.post<void>('/douyin/oauth/revoke', { accountId }),
}
