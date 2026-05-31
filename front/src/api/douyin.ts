import request from '@/utils/request'
import { isRecord, normalizeArray, normalizePage, parseJsonValue } from '@/utils/response-normalize'

// Account
export interface DyAccount {
  id: number; userId: number; accountName: string; accountId: string
  followCount: number; fanCount: number; videoCount: number; totalLikes: number
  description: string; status: number; createTime: string; updateTime: string
  authStatus?: 'valid' | 'expired' | 'unbound'
  lastSyncTime?: string
}
export interface DyAccountQuery { page?: number; rows?: number; accountName?: string; accountId?: string; status?: number }
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
  coverUrl?: string
}
export interface DyVideoQuery { page?: number; rows?: number; accountId?: number; title?: string; videoType?: string }
export interface DyVideoSave { id?: number; accountId: number; videoId: string; title: string; description?: string; videoType?: string }

// Fan Profile
export interface DyFanProfileStatItem {
  key?: string
  value?: string
  count?: number
  percentage?: number
}
export interface DyFanProfile {
  id?: number; accountId: number; accountName?: string; totalFans?: number
  ageRange?: string; gender?: string; region?: string; interests?: string; createTime?: string
  maleRatio?: number; femaleRatio?: number
  ageDistribution?: DyFanProfileStatItem[]
  genderDistribution?: DyFanProfileStatItem[]
  provinceDistribution?: DyFanProfileStatItem[]
  cityDistribution?: DyFanProfileStatItem[]
  interestTags?: DyFanProfileStatItem[]
  activeTimeDistribution?: DyFanProfileStatItem[]
  deviceDistribution?: DyFanProfileStatItem[]
  syncTime?: number | string
}
export interface DyFanProfileStats {
  id?: number; accountId?: number; ownerId?: number; statType?: string
  statKey?: string; statValue?: string; count?: number; percentage?: number
  date?: string; fanCount?: number; newFans?: number; lostFans?: number; ageRange?: string; ratio?: number
  syncTime?: string
}

export interface DyTokenStatus {
  status: 'valid' | 'expired' | 'refreshing' | 'unknown'
  expireTime?: string
  daysLeft?: number
}

function unwrapObject(raw: unknown): Record<string, unknown> | null {
  let value = parseJsonValue(raw)
  for (let i = 0; i < 3; i += 1) {
    if (!isRecord(value)) return null
    if ('data' in value && (isRecord(value.data) || value.data == null)) {
      value = parseJsonValue(value.data)
      continue
    }
    return value
  }
  return isRecord(value) ? value : null
}

function readString(raw: unknown, fallback = '') {
  return raw == null ? fallback : String(raw)
}

function readOptionalString(raw: unknown) {
  if (raw == null || raw === '') return undefined
  return String(raw)
}

function readOptionalNumber(raw: unknown) {
  const n = Number(raw)
  return Number.isFinite(n) ? n : undefined
}

function normalizeStatItem(raw: unknown): DyFanProfileStatItem {
  const record = unwrapObject(raw) ?? {}
  return {
    key: readOptionalString(record.key ?? record.statKey ?? record.name ?? record.label),
    value: readOptionalString(record.value ?? record.statValue ?? record.name ?? record.label ?? record.key),
    count: readOptionalNumber(record.count ?? record.total),
    percentage: readOptionalNumber(record.percentage ?? record.ratio ?? record.percent),
  }
}

function normalizeStatItems(raw: unknown): DyFanProfileStatItem[] {
  return normalizeArray<unknown>(raw).map(normalizeStatItem)
}

function normalizeFanProfile(raw: unknown): DyFanProfile | null {
  const record = unwrapObject(raw)
  if (!record) return null
  return {
    ...(record as Partial<DyFanProfile>),
    id: readOptionalNumber(record.id),
    accountId: readOptionalNumber(record.accountId) ?? 0,
    accountName: readOptionalString(record.accountName),
    totalFans: readOptionalNumber(record.totalFans ?? record.fanCount),
    ageRange: readOptionalString(record.ageRange),
    gender: readOptionalString(record.gender),
    region: readOptionalString(record.region),
    interests: readOptionalString(record.interests),
    createTime: readOptionalString(record.createTime),
    maleRatio: readOptionalNumber(record.maleRatio),
    femaleRatio: readOptionalNumber(record.femaleRatio),
    ageDistribution: normalizeStatItems(record.ageDistribution ?? record.age_distribution),
    genderDistribution: normalizeStatItems(record.genderDistribution ?? record.gender_distribution),
    provinceDistribution: normalizeStatItems(record.provinceDistribution ?? record.province_distribution),
    cityDistribution: normalizeStatItems(record.cityDistribution ?? record.city_distribution),
    interestTags: normalizeStatItems(record.interestTags ?? record.interest_tags),
    activeTimeDistribution: normalizeStatItems(record.activeTimeDistribution ?? record.active_time),
    deviceDistribution: normalizeStatItems(record.deviceDistribution ?? record.device_distribution),
    syncTime: record.syncTime as number | string | undefined,
  }
}

function normalizeFanProfileStats(raw: unknown): DyFanProfileStats {
  const record = unwrapObject(raw) ?? {}
  return {
    ...(record as Partial<DyFanProfileStats>),
    id: readOptionalNumber(record.id),
    accountId: readOptionalNumber(record.accountId),
    ownerId: readOptionalNumber(record.ownerId),
    statType: readOptionalString(record.statType),
    statKey: readOptionalString(record.statKey ?? record.key),
    statValue: readOptionalString(record.statValue ?? record.value),
    count: readOptionalNumber(record.count),
    percentage: readOptionalNumber(record.percentage ?? record.ratio),
    date: readOptionalString(record.date),
    fanCount: readOptionalNumber(record.fanCount),
    newFans: readOptionalNumber(record.newFans),
    lostFans: readOptionalNumber(record.lostFans),
    ageRange: readOptionalString(record.ageRange),
    ratio: readOptionalNumber(record.ratio ?? record.percentage),
    syncTime: readOptionalString(record.syncTime),
  }
}

function normalizeTokenStatus(raw: unknown): DyTokenStatus | null {
  const record = unwrapObject(raw)
  if (!record) return null
  const rawStatus = readString(record.status, 'unknown')
  const status: DyTokenStatus['status'] = rawStatus === 'valid' || rawStatus === 'expired' || rawStatus === 'refreshing'
    ? rawStatus
    : 'unknown'
  return {
    status,
    expireTime: readOptionalString(record.expireTime ?? record.expiresAt ?? record.expiredAt),
    daysLeft: readOptionalNumber(record.daysLeft ?? record.remainingDays),
  }
}

function normalizeOauthUrl(raw: unknown): { authUrl: string; state?: string } {
  const record = unwrapObject(raw) ?? {}
  return {
    authUrl: readString(record.authUrl ?? record.url),
    state: readOptionalString(record.state),
  }
}

function normalizeAccount(raw: unknown): DyAccount {
  const record = unwrapObject(raw) ?? {}
  return {
    ...(record as Partial<DyAccount>),
    id: readOptionalNumber(record.id) ?? 0,
    userId: readOptionalNumber(record.userId ?? record.ownerId) ?? 0,
    accountName: readString(record.accountName ?? record.name),
    accountId: readString(record.accountId ?? record.openId),
    followCount: readOptionalNumber(record.followCount) ?? 0,
    fanCount: readOptionalNumber(record.fanCount ?? record.totalFans) ?? 0,
    videoCount: readOptionalNumber(record.videoCount ?? record.totalVideos) ?? 0,
    totalLikes: readOptionalNumber(record.totalLikes ?? record.likeCount) ?? 0,
    description: readString(record.description),
    status: readOptionalNumber(record.status) ?? 1,
    createTime: readString(record.createTime ?? record.createdAt),
    updateTime: readString(record.updateTime ?? record.updatedAt),
    authStatus: record.authStatus as DyAccount['authStatus'],
    lastSyncTime: readOptionalString(record.lastSyncTime ?? record.syncTime),
  }
}

function normalizeAccountStats(raw: unknown): DyAccountStats {
  const record = unwrapObject(raw) ?? {}
  return {
    accountId: readString(record.accountId),
    totalVideos: readOptionalNumber(record.totalVideos ?? record.videoCount) ?? 0,
    totalViews: readOptionalNumber(record.totalViews ?? record.viewCount) ?? 0,
    totalLikes: readOptionalNumber(record.totalLikes ?? record.likeCount) ?? 0,
    totalShares: readOptionalNumber(record.totalShares ?? record.shareCount) ?? 0,
    totalComments: readOptionalNumber(record.totalComments ?? record.commentCount) ?? 0,
    avgViewsPerVideo: readOptionalNumber(record.avgViewsPerVideo ?? record.averageViews) ?? 0,
    avgLikesPerVideo: readOptionalNumber(record.avgLikesPerVideo ?? record.averageLikes) ?? 0,
  }
}

function normalizeVideo(raw: unknown): DyVideo {
  const record = unwrapObject(raw) ?? {}
  return {
    ...(record as Partial<DyVideo>),
    id: readOptionalNumber(record.id) ?? 0,
    accountId: readOptionalNumber(record.accountId) ?? 0,
    videoId: readString(record.videoId ?? record.itemId),
    title: readString(record.title),
    description: readString(record.description),
    viewCount: readOptionalNumber(record.viewCount ?? record.playCount) ?? 0,
    likeCount: readOptionalNumber(record.likeCount) ?? 0,
    shareCount: readOptionalNumber(record.shareCount) ?? 0,
    commentCount: readOptionalNumber(record.commentCount) ?? 0,
    downloadCount: readOptionalNumber(record.downloadCount) ?? 0,
    videoType: readString(record.videoType ?? record.type),
    publishTime: readString(record.publishTime ?? record.publishedAt),
    createTime: readString(record.createTime ?? record.createdAt),
    coverUrl: readOptionalString(record.coverUrl ?? record.cover),
  }
}

export const douyinApi = {
  accountList: (p: DyAccountQuery) =>
    request.post<unknown>('/douyin/account/search', p).then(raw => normalizePage<unknown, DyAccount>(
      raw,
      normalizeAccount,
      p.page ?? 0,
      p.rows ?? 20,
    )),
  accountGet: (id: number) => request.post<unknown>('/douyin/account/get', undefined, { params: { id } }).then(normalizeAccount),
  accountSave: (p: Partial<DyAccountSave>) => request.post<void>('/douyin/account/save', p),
  accountDelete: (id: number) => request.post<void>('/douyin/account/delete', undefined, { params: { id } }),
  accountStats: (id: number) => request.post<unknown>('/douyin/account/statistics', undefined, { params: { id } }).then(normalizeAccountStats),

  personaList: (personaType?: string) =>
    request.post<unknown>('/douyin/persona/list', undefined, { params: { personaType } })
      .then(raw => normalizeArray<DyPersona>(raw)),
  personaGet: (id: number) => request.post<DyPersona>('/douyin/persona/get', undefined, { params: { id } }),
  personaSave: (p: Partial<DyPersonaSave>) => request.post<void>('/douyin/persona/save', p),
  personaDelete: (id: number) => request.post<void>('/douyin/persona/delete', undefined, { params: { id } }),
  personaSetDefault: (id: number) => request.post<void>('/douyin/persona/set-default', undefined, { params: { id } }),
  personaGetDefault: () => request.post<DyPersona>('/douyin/persona/get-default', {}),
  personaGetByAccount: (accountId: number) =>
    request.post<unknown>('/douyin/persona/get-by-account', { accountId })
      .then(raw => unwrapObject(raw) as DyPersona | null),
  personaTemplates: () => request.post<unknown>('/douyin/persona/templates', {}).then(raw => normalizeArray<DyPersona>(raw)),

  // 视频
  videoSearch: (p: DyVideoQuery) =>
    request.post<unknown>('/douyin/video/search', p).then(raw => normalizePage<unknown, DyVideo>(
      raw,
      normalizeVideo,
      p.page ?? 0,
      p.rows ?? 20,
    )),
  videoGet: (id: number) => request.post<unknown>('/douyin/video/get', undefined, { params: { id } }).then(normalizeVideo),
  videoSave: (p: Partial<DyVideoSave>) => request.post<number>('/douyin/video/save', p),
  videoSync: (accountId: number) => request.post<void>('/douyin/video/sync', undefined, { params: { accountId } }),

  // 粉丝画像
  fanProfileGet: (accountId: number) =>
    request.post<unknown>('/douyin/fan-profile/get', { accountId }).then(normalizeFanProfile),
  fanProfileStats: (accountId: number) =>
    request.post<unknown>('/douyin/fan-profile/stats', { accountId }).then(raw => normalizeArray<unknown>(raw).map(normalizeFanProfileStats)),
  fanProfileSync: (accountId: number) => request.post<void>(`/douyin/fan-profile/sync/${accountId}`, {}),

  // OAuth 授权（P0）
  oauthUrl: (accountId: number) =>
    request.post<unknown>('/douyin/oauth/auth-url', { accountId }).then(normalizeOauthUrl),
  tokenStatus: (accountId: number) =>
    request.post<unknown>('/douyin/oauth/token-status', { accountId }).then(normalizeTokenStatus),
  tokenRefresh: (accountId: number) =>
    request.post<void>('/douyin/oauth/token-refresh', { accountId }),
  oauthRevoke: (accountId: number) =>
    request.post<void>('/douyin/oauth/revoke', { accountId }),
}
