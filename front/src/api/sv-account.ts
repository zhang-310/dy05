import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeRecord as normalizeResponseRecord,
  normalizePage as normalizeResponsePage,
  readNumber,
} from '@/utils/response-normalize'

// ─── 类型定义 ──────────────────────────────────────

export interface SvAccount {
  id: number
  secUid: string
  douyinId?: string
  nickname?: string
  avatarUrl?: string
  signature?: string

  // 账号数据
  followerCount: number
  followingCount: number
  totalFavorited: number
  videoCount: number

  // 认证信息
  isVerified: boolean
  verificationType?: string

  // 采集统计
  collectCount: number
  lastCollectTime?: string
  totalCollectedVideos: number

  // 分析统计
  avgViewCount: number
  avgLikeCount: number
  avgViralScore: number
  topViralScore: number

  // 标签与分类
  industryTags?: string
  contentTags?: string
  accountCategory?: string

  // 来源信息
  sourceType: string
  sourceKeyword?: string

  // 状态
  status: string
  notes?: string

  createTime: string
  updateTime: string
}

export interface SvAccountDetail extends SvAccount {
  taskCount: number
  pendingAnalysisCount: number
  analyzedCount: number
  latestTaskId?: number
}

export interface SvAccountSearchParams {
  keyword?: string
  accountCategory?: string
  sourceType?: string
  sourceKeyword?: string
  status?: string
  minFollowerCount?: number
  minViralScore?: number
  page: number
  rows: number
  sortName?: string
  sortOrder?: string
}

export interface SvAccountUpdateParams {
  id: number
  accountCategory?: string
  industryTags?: string
  contentTags?: string
  notes?: string
  status?: string
}

export interface AccountVideo {
  id: number
  title: string
  coverUrl?: string
  videoUrl?: string
  douyinVideoId?: string
  authorName?: string
  viewCount: number
  likeCount: number
  shareCount: number
  viralScore: number
  deepAnalyzeStatus: string
  /** 列表接口仍可能返回短进度摘要 */
  deepAnalyzeProgress?: string | null
  publishTime?: string
  createTime: string
  updateTime?: string
}

/** 与后端 AccountVideosQueryVO 一致 */
export interface AccountVideosQueryParams {
  accountId: number
  page?: number
  rows?: number
  keyword?: string
  deepAnalyzeStatus?: string
  sortName?: 'createTime' | 'viewCount' | 'viralScore' | 'updateTime' | 'id'
  sortOrder?: 'asc' | 'desc'
}

/** 与后端 SvAccountAnalyticsVO 对齐：该账号下已采集视频聚合 */
export interface SvAccountAnalytics {
  videoCount: number
  sumViewCount: number
  sumLikeCount: number
  sumShareCount: number
  avgViewPerVideo: number
  avgLikePerVideo: number
  avgSharePerVideo: number
  avgViralScore: number
  maxViewCount: number
  minViewCount: number
  deepPendingCount: number
  deepProcessingCount: number
  deepCompletedCount: number
  deepFailedCount: number
  deepOtherCount: number
}

function readOptionalNumber(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

function readStringAlias(row: Record<string, unknown>, keys: string[], fallback = ''): string {
  for (const key of keys) {
    const value = row[key]
    if (typeof value === 'string' && value.trim()) return value.trim()
    if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  }
  return fallback
}

function readOptionalStringAlias(row: Record<string, unknown>, keys: string[]): string | undefined {
  const value = readStringAlias(row, keys)
  return value || undefined
}

function readBooleanAlias(value: unknown, fallback = false): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value !== 0
  if (typeof value === 'string') {
    const text = value.trim().toLowerCase()
    if (['true', '1', 'yes', 'y', 'verified'].includes(text)) return true
    if (['false', '0', 'no', 'n', 'unverified'].includes(text)) return false
  }
  return fallback
}

function unwrapRecord(raw: unknown): Record<string, unknown> {
  const value = normalizeResponseRecord(raw)
  return isRecord(value) ? value : {}
}

function normalizeAccount(raw: unknown): SvAccount {
  const row = unwrapRecord(raw)
  return {
    ...(row as unknown as SvAccount),
    id: readNumber(row, ['id', 'accountId'], 0),
    secUid: readStringAlias(row, ['secUid', 'sec_uid', 'secUserId']),
    douyinId: readOptionalStringAlias(row, ['douyinId', 'douyin_id', 'uniqueId']),
    nickname: readOptionalStringAlias(row, ['nickname', 'nickName', 'authorName']),
    avatarUrl: readOptionalStringAlias(row, ['avatarUrl', 'avatar', 'authorAvatar']),
    signature: readOptionalStringAlias(row, ['signature', 'description', 'bio']),
    followerCount: readNumber(row, ['followerCount', 'followers', 'fansCount'], 0),
    followingCount: readNumber(row, ['followingCount', 'followings', 'following'], 0),
    totalFavorited: readNumber(row, ['totalFavorited', 'favoritedCount', 'totalLikeCount'], 0),
    videoCount: readNumber(row, ['videoCount', 'awemeCount', 'worksCount'], 0),
    isVerified: readBooleanAlias(row.isVerified ?? row.verified),
    verificationType: readOptionalStringAlias(row, ['verificationType', 'verifyType']),
    collectCount: readNumber(row, ['collectCount', 'collectionCount'], 0),
    lastCollectTime: readOptionalStringAlias(row, ['lastCollectTime', 'lastCollectedAt']),
    totalCollectedVideos: readNumber(row, ['totalCollectedVideos', 'collectedVideoCount', 'viralVideoCount'], 0),
    avgViewCount: readNumber(row, ['avgViewCount', 'averageViewCount'], 0),
    avgLikeCount: readNumber(row, ['avgLikeCount', 'averageLikeCount'], 0),
    avgViralScore: readNumber(row, ['avgViralScore', 'averageViralScore'], 0),
    topViralScore: readNumber(row, ['topViralScore', 'maxViralScore'], 0),
    industryTags: readOptionalStringAlias(row, ['industryTags', 'industryTag']),
    contentTags: readOptionalStringAlias(row, ['contentTags', 'contentTag']),
    accountCategory: readOptionalStringAlias(row, ['accountCategory', 'category']),
    sourceType: readStringAlias(row, ['sourceType', 'source'], 'unknown'),
    sourceKeyword: readOptionalStringAlias(row, ['sourceKeyword', 'keyword']),
    status: readStringAlias(row, ['status'], 'active'),
    notes: readOptionalStringAlias(row, ['notes', 'remark']),
    createTime: readStringAlias(row, ['createTime', 'createdAt']),
    updateTime: readStringAlias(row, ['updateTime', 'updatedAt']),
  }
}

function normalizeAccountDetail(raw: unknown): SvAccountDetail {
  const row = unwrapRecord(raw)
  return {
    ...normalizeAccount(row),
    taskCount: readNumber(row, ['taskCount', 'collectTaskCount'], 0),
    pendingAnalysisCount: readNumber(row, ['pendingAnalysisCount', 'deepPendingCount'], 0),
    analyzedCount: readNumber(row, ['analyzedCount', 'deepCompletedCount'], 0),
    latestTaskId: readOptionalNumber(row.latestTaskId ?? row.lastTaskId),
  }
}

function normalizeAccountVideo(raw: unknown): AccountVideo {
  const row = unwrapRecord(raw)
  return {
    ...(row as unknown as AccountVideo),
    id: readNumber(row, ['id', 'videoId', 'viralVideoId'], 0),
    title: readStringAlias(row, ['title', 'desc', 'description']),
    coverUrl: readOptionalStringAlias(row, ['coverUrl', 'cover', 'coverImage']),
    videoUrl: readOptionalStringAlias(row, ['videoUrl', 'url', 'playUrl']),
    douyinVideoId: readOptionalStringAlias(row, ['douyinVideoId', 'awemeId']),
    authorName: readOptionalStringAlias(row, ['authorName', 'nickname']),
    viewCount: readNumber(row, ['viewCount', 'playCount'], 0),
    likeCount: readNumber(row, ['likeCount', 'diggCount'], 0),
    shareCount: readNumber(row, ['shareCount'], 0),
    viralScore: readNumber(row, ['viralScore', 'score'], 0),
    deepAnalyzeStatus: readStringAlias(row, ['deepAnalyzeStatus', 'analysisStatus'], 'pending'),
    deepAnalyzeProgress: readOptionalStringAlias(row, ['deepAnalyzeProgress', 'analysisProgress']),
    publishTime: readOptionalStringAlias(row, ['publishTime', 'publishedAt']),
    createTime: readStringAlias(row, ['createTime', 'createdAt']),
    updateTime: readOptionalStringAlias(row, ['updateTime', 'updatedAt']),
  }
}

function normalizeAnalytics(raw: unknown): SvAccountAnalytics {
  const row = unwrapRecord(raw)
  return {
    videoCount: readNumber(row, ['videoCount', 'totalVideoCount'], 0),
    sumViewCount: readNumber(row, ['sumViewCount', 'totalViewCount'], 0),
    sumLikeCount: readNumber(row, ['sumLikeCount', 'totalLikeCount'], 0),
    sumShareCount: readNumber(row, ['sumShareCount', 'totalShareCount'], 0),
    avgViewPerVideo: readNumber(row, ['avgViewPerVideo', 'avgViewCount'], 0),
    avgLikePerVideo: readNumber(row, ['avgLikePerVideo', 'avgLikeCount'], 0),
    avgSharePerVideo: readNumber(row, ['avgSharePerVideo', 'avgShareCount'], 0),
    avgViralScore: readNumber(row, ['avgViralScore', 'averageViralScore'], 0),
    maxViewCount: readNumber(row, ['maxViewCount', 'topViewCount'], 0),
    minViewCount: readNumber(row, ['minViewCount'], 0),
    deepPendingCount: readNumber(row, ['deepPendingCount', 'pendingAnalysisCount'], 0),
    deepProcessingCount: readNumber(row, ['deepProcessingCount', 'processingAnalysisCount'], 0),
    deepCompletedCount: readNumber(row, ['deepCompletedCount', 'analyzedCount'], 0),
    deepFailedCount: readNumber(row, ['deepFailedCount', 'failedAnalysisCount'], 0),
    deepOtherCount: readNumber(row, ['deepOtherCount', 'otherAnalysisCount'], 0),
  }
}

// ─── API 方法 ──────────────────────────────────────

/**
 * 账号列表
 */
export function accountList(params: SvAccountSearchParams) {
  return request
    .post<unknown>('/short-video/account/list', params)
    .then((raw): PageResult<SvAccount> => normalizeResponsePage<unknown, SvAccount>(
      raw,
      normalizeAccount,
      params.page,
      params.rows,
    ))
}

/**
 * 账号详情
 */
export function accountGet(id: number) {
  return request.post<unknown>('/short-video/account/get', { id }).then(normalizeAccountDetail)
}

/**
 * 更新账号
 */
export function accountUpdate(params: SvAccountUpdateParams) {
  return request.post<void>('/short-video/account/update', params)
}

/**
 * 删除账号
 */
export function accountDelete(id: number) {
  return request.post<void>('/short-video/account/delete', { id })
}

/**
 * 账号视频列表（分页、筛选、排序）
 */
export function accountVideos(params: AccountVideosQueryParams) {
  return request
    .post<unknown>('/short-video/account/videos', params)
    .then((raw): PageResult<AccountVideo> => normalizeResponsePage<unknown, AccountVideo>(
      raw,
      normalizeAccountVideo,
      params.page ?? 0,
      params.rows ?? 20,
    ))
}

/**
 * 账号下全部已采集视频的聚合分析（播放、点赞、深度分析状态分布等）
 */
export function accountAnalytics(accountId: number) {
  return request.post<unknown>('/short-video/account/analytics', { id: accountId }).then(normalizeAnalytics)
}

/**
 * 刷新账号统计
 */
export function accountRefreshStats(id: number) {
  return request.post<void>('/short-video/account/refresh-stats', { id })
}
