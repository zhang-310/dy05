import request from '@/utils/request'
import { dashboardApi, type AdminStats } from './dashboard'
import { liveApi, type LiveSession } from './live'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeArray,
  normalizeRecord,
  parseJsonValue,
  readNumber,
} from '@/utils/response-normalize'

export interface OrganizationInfo {
  id: number
  orgName: string
  orgCode?: string
  contactName?: string
  contactPhone?: string
  status?: number
  ownerId?: number
  createTime?: string
}

export interface OrgMember {
  id: number
  orgId?: number
  userId: number
  username: string
  nickname?: string
  roleInOrg: string
  status: number
  invitedAt?: string
  joinedAt?: string
}

export interface TalentCandidate {
  id: number
  username: string
  nickname?: string
  mobile?: string
}

export interface LiveReview {
  id: number
  sessionId: number
  accountId?: number
  ownerId?: number
  totalViewers: number
  totalGmv: number
  conversionRate: number
  peakViewers: number
  topScripts?: string
  weakPoints?: string
  reportContent?: string
  modelUsed?: string
  tokensUsed?: number
  status: number
  createTime?: string
}

export interface OrgAnalyticsSnapshot {
  stats: AdminStats
  sessions: LiveSession[]
  errors?: {
    stats?: string
    sessions?: string
  }
}

function toNumber(value: unknown, fallback = 0): number {
  if (value == null || value === '') return fallback
  const normalized = typeof value === 'number' ? value : String(value).replace(/[^\d.-]/g, '')
  if (normalized === '' || normalized === '-' || normalized === '.' || normalized === '-.') return fallback
  const n = typeof normalized === 'number' ? normalized : Number(normalized)
  return Number.isFinite(n) ? n : fallback
}

function findNumber(raw: unknown, keys: string[]): number | undefined {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) return undefined
  for (const key of keys) {
    const n = toNumber(value[key], Number.NaN)
    if (Number.isFinite(n)) return n
  }
  for (const key of ['data', 'result', 'detail', 'record', 'item', 'payload', 'body', 'page'] as const) {
    if (key in value) {
      const nested = findNumber(value[key], keys)
      if (nested != null) return nested
    }
  }
  return undefined
}

function readCurrencyNumber(raw: unknown, keys: string[], fallback: number): number {
  return findNumber(raw, keys) ?? fallback
}

function stringOrUndefined(value: unknown): string | undefined {
  if (value == null || value === '') return undefined
  return String(value)
}

function metricValue(value: unknown): number | string | undefined {
  if (typeof value === 'number' || typeof value === 'string') return value
  return undefined
}

function normalizeOrganization(raw: unknown): OrganizationInfo | null {
  const o = normalizeRecord(raw, ['organization', 'org'])
  if (Object.keys(o).length === 0) return null
  return {
    id: toNumber(o.id ?? o.orgId ?? o.organizationId ?? o.org_id),
    orgName: String(o.orgName ?? o.org_name ?? o.name ?? o.organizationName ?? '未命名机构'),
    orgCode: stringOrUndefined(o.orgCode ?? o.org_code ?? o.code),
    contactName: stringOrUndefined(o.contactName ?? o.contact_name),
    contactPhone: stringOrUndefined(o.contactPhone ?? o.contact_phone ?? o.phone),
    status: o.status == null ? undefined : toNumber(o.status),
    ownerId: o.ownerId == null && o.owner_id == null ? undefined : toNumber(o.ownerId ?? o.owner_id),
    createTime: stringOrUndefined(o.createTime ?? o.create_time ?? o.createdAt ?? o.created_at),
  }
}

function normalizeMember(raw: unknown): OrgMember {
  const m = normalizeRecord(raw, ['member', 'user'])
  return {
    id: toNumber(m.id ?? m.memberId ?? m.member_id ?? m.userId ?? m.user_id),
    orgId: m.orgId == null && m.org_id == null ? undefined : toNumber(m.orgId ?? m.org_id),
    userId: toNumber(m.userId ?? m.user_id ?? m.id),
    username: String(m.username ?? m.userName ?? m.user_name ?? m.name ?? '-'),
    nickname: stringOrUndefined(m.nickname ?? m.nickName ?? m.nick_name ?? m.displayName),
    roleInOrg: String(m.roleInOrg ?? m.role_in_org ?? m.orgRole ?? m.role ?? '-'),
    status: toNumber(m.status, 0),
    invitedAt: stringOrUndefined(m.invitedAt ?? m.invited_at),
    joinedAt: stringOrUndefined(m.joinedAt ?? m.joined_at),
  }
}

function normalizeTalent(raw: unknown): TalentCandidate {
  const t = normalizeRecord(raw, ['talent', 'user'])
  return {
    id: toNumber(t.id ?? t.userId ?? t.user_id),
    username: String(t.username ?? t.userName ?? t.user_name ?? t.name ?? '-'),
    nickname: stringOrUndefined(t.nickname ?? t.nickName ?? t.nick_name ?? t.displayName),
    mobile: stringOrUndefined(t.mobile ?? t.phone ?? t.contactPhone ?? t.contact_phone),
  }
}

function normalizeLiveReview(raw: unknown): LiveReview {
  const r = normalizeRecord(raw, ['review', 'liveReview'])
  return {
    id: toNumber(r.id ?? r.reviewId ?? r.review_id),
    sessionId: toNumber(r.sessionId ?? r.session_id),
    accountId: r.accountId == null && r.account_id == null ? undefined : toNumber(r.accountId ?? r.account_id),
    ownerId: r.ownerId == null && r.owner_id == null ? undefined : toNumber(r.ownerId ?? r.owner_id),
    totalViewers: toNumber(r.totalViewers ?? r.total_viewers ?? r.viewers),
    totalGmv: toNumber(r.totalGmv ?? r.total_gmv ?? r.gmv),
    conversionRate: toNumber(r.conversionRate ?? r.conversion_rate),
    peakViewers: toNumber(r.peakViewers ?? r.peak_viewers),
    topScripts: stringOrUndefined(r.topScripts ?? r.top_scripts),
    weakPoints: stringOrUndefined(r.weakPoints ?? r.weak_points),
    reportContent: stringOrUndefined(r.reportContent ?? r.report_content ?? r.content),
    modelUsed: stringOrUndefined(r.modelUsed ?? r.model_used),
    tokensUsed: r.tokensUsed == null && r.tokens_used == null ? undefined : toNumber(r.tokensUsed ?? r.tokens_used),
    status: toNumber(r.status),
    createTime: stringOrUndefined(r.createTime ?? r.create_time ?? r.createdAt ?? r.created_at),
  }
}

function toPage<T>(raw: unknown, normalize: (value: unknown) => T): PageResult<T> {
  const list = normalizeArray(raw)
  return {
    total: readCurrencyNumber(raw, ['total', 'totalElements', 'count', 'totalCount', 'totalRecords'], list.length),
    list: list.map(normalize),
    pageNum: readNumber(raw, ['pageNum', 'page', 'pageNumber', 'current'], 0),
    pageSize: readNumber(raw, ['pageSize', 'size', 'rows'], list.length),
  }
}

function normalizeStats(raw: unknown): AdminStats {
  return normalizeRecord(raw, ['stats']) as unknown as AdminStats
}

function normalizeSessions(raw: unknown): LiveSession[] {
  return normalizeArray(raw).map(row => {
    const s = normalizeRecord(row, ['session'])
    return {
      ...(s as unknown as LiveSession),
      id: toNumber(s.id ?? s.sessionId ?? s.session_id),
      userId: toNumber(s.userId ?? s.user_id ?? s.ownerId ?? s.owner_id),
      accountId: toNumber(s.accountId ?? s.account_id),
      personaId: toNumber(s.personaId ?? s.persona_id),
      liveTitle: String(s.liveTitle ?? s.live_title ?? s.title ?? ''),
      sessionCover: String(s.sessionCover ?? s.session_cover ?? ''),
      scriptStyle: String(s.scriptStyle ?? s.script_style ?? ''),
      liveDescription: String(s.liveDescription ?? s.live_description ?? s.description ?? ''),
      scheduledTime: String(s.scheduledTime ?? s.scheduled_time ?? ''),
      scheduledEndTime: String(s.scheduledEndTime ?? s.scheduled_end_time ?? ''),
      startTime: String(s.startTime ?? s.start_time ?? ''),
      endTime: String(s.endTime ?? s.end_time ?? ''),
      liveUrl: String(s.liveUrl ?? s.live_url ?? ''),
      viewers: toNumber(s.viewers ?? s.totalViewers ?? s.total_viewers),
      likes: toNumber(s.likes),
      status: toNumber(s.status),
      sessionType: String(s.sessionType ?? s.session_type ?? ''),
      liveFormat: String(s.liveFormat ?? s.live_format ?? ''),
      createTime: String(s.createTime ?? s.create_time ?? ''),
      updateTime: String(s.updateTime ?? s.update_time ?? ''),
      totalGmv: metricValue(s.totalGmv ?? s.total_gmv ?? s.gmv),
      cumulativeGmv: metricValue(s.cumulativeGmv ?? s.cumulative_gmv),
      totalRevenue: metricValue(s.totalRevenue ?? s.total_revenue),
    }
  })
}

const EMPTY_ORG_STATS: AdminStats = {
  totalUsers: 0,
  activeUsers: 0,
  todayUsers: 0,
  totalVideos: 0,
  publishedVideos: 0,
  todayVideos: 0,
  totalLiveSessions: 0,
  completedSessions: 0,
  todaySessions: 0,
  totalShortVideos: 0,
  publishedShortVideos: 0,
  todayShortVideos: 0,
  totalCopyItems: 0,
  approvedCopyItems: 0,
  todayCopyItems: 0,
  todayAiCalls: 0,
  todayAiAttempts: 0,
  aiSuccessRate: 0,
  todayRevenue: 0,
}

function endpointError(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error || '接口异常')
}

export const orgApi = {
  my: () => request.post<unknown>('/organization/my', {}).then(normalizeOrganization),
  members: () => request.post<unknown>('/organization/members', {}).then(list => normalizeArray(list).map(normalizeMember)),
  searchTalents: (keyword: string) =>
    request.post<unknown>('/organization/search-talents', { keyword }).then(list => normalizeArray(list).map(normalizeTalent)),
  inviteTalent: (userId: number) => request.post<void>('/organization/invite', { userId }),
  removeMember: (userId: number) => request.post<void>('/organization/remove', { userId }),

  liveReviewList: (params: { page?: number; rows?: number; status?: number }) =>
    request.post<unknown>('/ai/evolution/live-review/list', params).then(page => toPage(page, normalizeLiveReview)),
  liveReviewGet: (id: number) =>
    request.post<unknown>('/ai/evolution/live-review/get', undefined, { params: { id } }).then(normalizeLiveReview),
  liveReviewTrigger: (params: { sessionId: number; accountId?: number }) =>
    request.post<number>('/ai/evolution/live-review/trigger', params),
  liveReviewDelete: (id: number) =>
    request.post<void>('/ai/evolution/live-review/delete', undefined, { params: { id } }),

  analyticsSnapshot: async (lookbackDays = 30): Promise<OrgAnalyticsSnapshot> => {
    const end = new Date()
    const start = new Date(end)
    start.setDate(end.getDate() - Math.max(lookbackDays - 1, 0))
    const [statsResult, sessionsResult] = await Promise.allSettled([
      dashboardApi.orgStats().then(normalizeStats),
      liveApi.sessionSearch({
        page: 0,
        rows: 100,
        scheduledTimeFrom: start.toISOString().slice(0, 10),
        scheduledTimeTo: end.toISOString().slice(0, 10),
      }).then(normalizeSessions),
    ])
    const errors: OrgAnalyticsSnapshot['errors'] = {}
    if (statsResult.status === 'rejected') errors.stats = endpointError(statsResult.reason)
    if (sessionsResult.status === 'rejected') errors.sessions = endpointError(sessionsResult.reason)

    return {
      stats: statsResult.status === 'fulfilled' ? statsResult.value : EMPTY_ORG_STATS,
      sessions: sessionsResult.status === 'fulfilled' ? sessionsResult.value : [],
      errors: errors.stats || errors.sessions ? errors : undefined,
    }
  },
}
