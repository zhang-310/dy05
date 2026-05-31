import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import * as request from '@/utils/request'
import { dashboardApi } from '../dashboard'
import { liveApi } from '../live'
import { orgApi } from '../org'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

vi.mock('../dashboard', () => ({
  dashboardApi: {
    orgStats: vi.fn(),
  },
}))

vi.mock('../live', () => ({
  liveApi: {
    sessionSearch: vi.fn(),
  },
}))

describe('org API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-21T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('uses real organization member and invite contracts', async () => {
    mockPost.mockResolvedValue([
      { id: 1, userId: 7, username: 'talent-a', roleInOrg: 'TALENT', status: 1, joinedAt: '2026-05-20' },
    ])
    const members = await orgApi.members()
    await orgApi.searchTalents('张')
    await orgApi.inviteTalent(9)
    await orgApi.removeMember(7)

    expect(members[0]).toMatchObject({ id: 1, userId: 7, username: 'talent-a', roleInOrg: 'TALENT' })
    expect(mockPost).toHaveBeenCalledWith('/organization/members', {})
    expect(mockPost).toHaveBeenCalledWith('/organization/search-talents', { keyword: '张' })
    expect(mockPost).toHaveBeenCalledWith('/organization/invite', { userId: 9 })
    expect(mockPost).toHaveBeenCalledWith('/organization/remove', { userId: 7 })
  })

  it('normalizes wrapped organization profile records', async () => {
    mockPost.mockResolvedValueOnce({
      result: {
        organization: {
          org_id: '6',
          org_name: '包装机构',
          org_code: 'ORG-WRAP',
          contact_name: '负责人',
          contact_phone: '13800138000',
          owner_id: '99',
          create_time: '2026-05-21 09:00:00',
          status: '1',
        },
      },
    })

    const org = await orgApi.my()

    expect(org).toMatchObject({
      id: 6,
      orgName: '包装机构',
      orgCode: 'ORG-WRAP',
      contactName: '负责人',
      contactPhone: '13800138000',
      ownerId: 99,
      status: 1,
      createTime: '2026-05-21 09:00:00',
    })
    expect(mockPost).toHaveBeenCalledWith('/organization/my', {})
  })

  it('normalizes wrapped organization arrays and paged reviews', async () => {
    mockPost
      .mockResolvedValueOnce({
        result: {
          members: [
            {
              member_id: '2',
              user_id: '8',
              username: 'wrapped-member',
              nick_name: '包装成员',
              role_in_org: 'operator',
              joined_at: '2026-05-21 10:00:00',
              status: '1',
            },
          ],
        },
      })
      .mockResolvedValueOnce({ payload: { talents: [{ user_id: '10', user_name: 'wrapped-talent', nick_name: '包装达人', phone: '13900139000' }] } })
      .mockResolvedValueOnce({
        detail: {
          reviews: [
            {
              review_id: '3',
              session_id: '19',
              status: '1',
              total_gmv: '¥2,300',
              conversion_rate: '0.08',
              total_viewers: '300',
              peak_viewers: '180',
              model_used: 'deepseek',
              tokens_used: '500',
              create_time: '2026-05-21 22:00:00',
            },
          ],
          totalRecords: '1',
        },
      })

    const members = await orgApi.members()
    const talents = await orgApi.searchTalents('包')
    const reviews = await orgApi.liveReviewList({ page: 0, rows: 20 })

    expect(members[0]).toMatchObject({
      id: 2,
      userId: 8,
      username: 'wrapped-member',
      nickname: '包装成员',
      roleInOrg: 'operator',
      joinedAt: '2026-05-21 10:00:00',
    })
    expect(talents[0]).toMatchObject({ id: 10, username: 'wrapped-talent', nickname: '包装达人', mobile: '13900139000' })
    expect(reviews.total).toBe(1)
    expect(reviews.list[0]).toMatchObject({
      id: 3,
      sessionId: 19,
      totalGmv: 2300,
      conversionRate: 0.08,
      totalViewers: 300,
      peakViewers: 180,
      modelUsed: 'deepseek',
      tokensUsed: 500,
      createTime: '2026-05-21 22:00:00',
    })
  })

  it('uses ai evolution live review params contract', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [
        { id: 2, sessionId: 18, status: 1, totalGmv: '1200.50', conversionRate: '0.04', totalViewers: 200 },
      ],
      pageNum: 0,
      pageSize: 20,
    })

    const page = await orgApi.liveReviewList({ page: 0, rows: 20, status: 1 })
    await orgApi.liveReviewGet(2)
    await orgApi.liveReviewTrigger({ sessionId: 18, accountId: 3 })
    await orgApi.liveReviewDelete(2)

    expect(page.list[0]).toMatchObject({ id: 2, sessionId: 18, totalGmv: 1200.5, conversionRate: 0.04 })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/live-review/list', { page: 0, rows: 20, status: 1 })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/live-review/get', undefined, { params: { id: 2 } })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/live-review/trigger', { sessionId: 18, accountId: 3 })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/live-review/delete', undefined, { params: { id: 2 } })
  })

  it('normalizes wrapped live review detail records', async () => {
    mockPost.mockResolvedValueOnce({
      payload: {
        review: {
          review_id: '5',
          session_id: '22',
          account_id: '7',
          total_gmv: '3300',
          total_viewers: '900',
          peak_viewers: '300',
          conversion_rate: '0.12',
          top_scripts: '高转化开场',
          weak_points: '产品利益点重复',
          report_content: '整体节奏稳定',
          model_used: 'gpt-4o',
          tokens_used: '1200',
          status: '1',
        },
      },
    })

    const review = await orgApi.liveReviewGet(5)

    expect(review).toMatchObject({
      id: 5,
      sessionId: 22,
      accountId: 7,
      totalGmv: 3300,
      totalViewers: 900,
      peakViewers: 300,
      conversionRate: 0.12,
      topScripts: '高转化开场',
      weakPoints: '产品利益点重复',
      reportContent: '整体节奏稳定',
      modelUsed: 'gpt-4o',
      tokensUsed: 1200,
      status: 1,
    })
  })

  it('builds org analytics snapshot from dashboard and live sessions', async () => {
    vi.mocked(dashboardApi.orgStats).mockResolvedValue({ totalUsers: 3, todayAiCalls: 5 } as never)
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 100 } as never)

    await orgApi.analyticsSnapshot(7)

    expect(dashboardApi.orgStats).toHaveBeenCalled()
    expect(liveApi.sessionSearch).toHaveBeenCalledWith(expect.objectContaining({
      page: 0,
      rows: 100,
      scheduledTimeFrom: '2026-05-15',
      scheduledTimeTo: '2026-05-21',
    }))
  })

  it('keeps partial org analytics results when one source fails', async () => {
    vi.mocked(dashboardApi.orgStats).mockRejectedValue(new Error('stats down'))
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      total: 1,
      list: [{ id: 18, status: 2, totalGmv: 1200 }],
      pageNum: 0,
      pageSize: 100,
    } as never)

    const snapshot = await orgApi.analyticsSnapshot(7)

    expect(snapshot.stats.totalUsers).toBe(0)
    expect(snapshot.sessions).toHaveLength(1)
    expect(snapshot.errors?.stats).toBe('stats down')
    expect(snapshot.errors?.sessions).toBeUndefined()
  })

  it('normalizes wrapped analytics stats and session aliases', async () => {
    vi.mocked(dashboardApi.orgStats).mockResolvedValue({ result: { stats: { totalUsers: 8, todayAiCalls: 12 } } } as never)
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      result: {
        sessions: [
          {
            session_id: '19',
            live_title: '包装直播',
            status: '1',
            live_format: '双人',
            scheduled_time: '2026-05-21 20:00:00',
            total_viewers: '300',
            total_gmv: '2300',
          },
        ],
      },
    } as never)

    const snapshot = await orgApi.analyticsSnapshot(7)

    expect(snapshot.stats).toMatchObject({ totalUsers: 8, todayAiCalls: 12 })
    expect(snapshot.sessions[0]).toMatchObject({
      id: 19,
      liveTitle: '包装直播',
      status: 1,
      liveFormat: '双人',
      scheduledTime: '2026-05-21 20:00:00',
      viewers: 300,
      totalGmv: '2300',
    })
  })
})
