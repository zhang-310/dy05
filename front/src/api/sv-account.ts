import request from '@/utils/request'

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

// ─── API 方法 ──────────────────────────────────────

/**
 * 账号列表
 */
export function accountList(params: SvAccountSearchParams) {
  return request.post<{
    total: number
    list: SvAccount[]
    pageNum: number
    pageSize: number
  }>('short-video/account/list', params)
}

/**
 * 账号详情
 */
export function accountGet(id: number) {
  return request.post<SvAccountDetail>('short-video/account/get', { id })
}

/**
 * 更新账号
 */
export function accountUpdate(params: SvAccountUpdateParams) {
  return request.post<void>('short-video/account/update', params)
}

/**
 * 删除账号
 */
export function accountDelete(id: number) {
  return request.post<void>('short-video/account/delete', { id })
}

/**
 * 账号视频列表（分页、筛选、排序）
 */
export function accountVideos(params: AccountVideosQueryParams) {
  return request.post<{
    total: number
    list: AccountVideo[]
    pageNum: number
    pageSize: number
  }>('short-video/account/videos', params)
}

/**
 * 账号下全部已采集视频的聚合分析（播放、点赞、深度分析状态分布等）
 */
export function accountAnalytics(accountId: number) {
  return request.post<SvAccountAnalytics>('short-video/account/analytics', { id: accountId })
}

/**
 * 刷新账号统计
 */
export function accountRefreshStats(id: number) {
  return request.post<void>('short-video/account/refresh-stats', { id })
}
