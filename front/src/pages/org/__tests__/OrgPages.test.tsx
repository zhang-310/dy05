import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import MembersPage from '../MembersPage'
import OrgAnalyticsPage from '../OrgAnalyticsPage'
import OrgLiveReviewsPage from '../OrgLiveReviewsPage'
import { orgApi } from '@/api/org'

vi.mock('@/api/org', () => ({
  orgApi: {
    my: vi.fn(),
    members: vi.fn(),
    searchTalents: vi.fn(),
    inviteTalent: vi.fn(),
    removeMember: vi.fn(),
    analyticsSnapshot: vi.fn(),
    liveReviewList: vi.fn(),
    liveReviewGet: vi.fn(),
    liveReviewTrigger: vi.fn(),
    liveReviewDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts">chart</div>,
}))

function renderPage(ui: ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('Org pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(orgApi.my).mockResolvedValue({
      id: 1,
      orgName: '测试机构',
      orgCode: 'ORG001',
      status: 1,
      createTime: '2026-05-20 10:00:00',
    } as never)
    vi.mocked(orgApi.members).mockResolvedValue([
      {
        id: 1,
        userId: 7,
        username: 'talent-a',
        nickname: '达人A',
        roleInOrg: 'TALENT',
        status: 1,
        joinedAt: '2026-05-20 10:00:00',
      },
    ] as never)
    vi.mocked(orgApi.searchTalents).mockResolvedValue([
      { id: 9, username: 'new-talent', nickname: '新达人', mobile: '13800138000' },
    ] as never)
    vi.mocked(orgApi.analyticsSnapshot).mockResolvedValue({
      stats: { totalUsers: 3, todayAiCalls: 5 },
      sessions: [
        {
          id: 18,
          liveTitle: '五月直播',
          status: 2,
          liveFormat: '单人',
          scheduledTime: '2026-05-20 20:00:00',
          viewers: 200,
          totalGmv: 1200,
        },
      ],
    } as never)
    vi.mocked(orgApi.liveReviewList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 2,
          sessionId: 18,
          status: 1,
          totalGmv: 1200,
          conversionRate: 0.04,
          totalViewers: 200,
          peakViewers: 120,
          modelUsed: 'deepseek',
          tokensUsed: 300,
          reportContent: '本场转化稳定',
          topScripts: '开场话术',
          weakPoints: '节奏偏慢',
          createTime: '2026-05-20 22:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(orgApi.liveReviewGet).mockResolvedValue({
      id: 2,
      sessionId: 18,
      status: 1,
      totalGmv: 1200,
      conversionRate: 0.04,
      totalViewers: 200,
      peakViewers: 120,
      modelUsed: 'deepseek',
      tokensUsed: 300,
      reportContent: '详情报告内容',
      topScripts: '详情优秀话术',
      weakPoints: '详情薄弱点',
      createTime: '2026-05-20 22:00:00',
    } as never)
  })

  it('renders members from real organization endpoints and invites by user id', async () => {
    vi.mocked(orgApi.inviteTalent).mockResolvedValue(undefined)
    vi.mocked(orgApi.removeMember).mockResolvedValue(undefined)
    renderPage(<MembersPage />)

    expect(await screen.findByRole('heading', { name: '机构成员' })).toBeInTheDocument()
    const workbench = screen.getByTestId('org-members-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'org-members')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/organization/my,/organization/members,/organization/search-talents,/organization/invite,/organization/remove')
    expect(workbench).toHaveAttribute('data-unsupported-actions', 'update-role,server-export,bulk-invite')
    expect(screen.getByTestId('org-member-role-downgrade')).toHaveAttribute('data-contract-endpoint', '/organization/member/update-role')
    expect(screen.getByTestId('org-member-list-surface')).toHaveAttribute('data-server-export', 'unsupported')
    expect(screen.queryByRole('button', { name: '导出' })).not.toBeInTheDocument()
    const memberCards = screen.getAllByTestId('org-member-kpi-card')
    expect(memberCards).toHaveLength(4)
    expect(memberCards.find(card => card.getAttribute('data-kpi-label') === '机构')).toHaveAttribute('data-source-endpoint', '/organization/my')
    expect(memberCards.find(card => card.getAttribute('data-kpi-label') === '成员总数')).toHaveAttribute('data-source-endpoint', '/organization/members')
    expect(await screen.findByText('talent-a')).toBeInTheDocument()
    await waitFor(() => expect(workbench).toHaveAttribute('data-row-count', '1'))
    expect(workbench).toHaveAttribute('data-active-count', '1')
    expect(screen.getByText(/组织角色修改接口/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '邀请达人' }))
    expect(await screen.findByTestId('org-member-invite-dialog')).toHaveAttribute('data-search-endpoint', '/organization/search-talents')
    expect(screen.getByTestId('org-member-invite-dialog')).toHaveAttribute('data-invite-endpoint', '/organization/invite')
    expect(screen.getByTestId('org-member-invite-dialog')).toHaveAttribute('data-bulk-invite-status', 'unsupported')
    fireEvent.change(screen.getByLabelText('达人关键词'), { target: { value: '新' } })
    await waitFor(() => expect(orgApi.searchTalents).toHaveBeenCalledWith('新'))
    await waitFor(() => expect(screen.getByRole('combobox', { name: '选择达人' })).not.toHaveAttribute('aria-disabled', 'true'))
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择达人' }))
    fireEvent.click(await screen.findByRole('option', { name: /新达人/ }))
    await waitFor(() => expect(screen.getByTestId('org-member-invite-dialog')).toHaveAttribute('data-selected-user-id', '9'))
    fireEvent.click(screen.getByRole('button', { name: '发送邀请' }))

    await waitFor(() => expect(orgApi.inviteTalent).toHaveBeenCalledWith(9))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: '邀请达人加入机构' })).not.toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '移除' }))
    const removeDialog = await screen.findByRole('dialog', { name: '移除机构成员' })
    expect(orgApi.removeMember).not.toHaveBeenCalled()
    expect(screen.getByTestId('org-member-remove-dialog')).toHaveAttribute('data-contract-endpoint', '/organization/remove')
    expect(screen.getByTestId('org-member-remove-dialog')).toHaveAttribute('data-user-id', '7')
    fireEvent.click(within(removeDialog).getByRole('button', { name: '确认移除' }))
    await waitFor(() => expect(orgApi.removeMember).toHaveBeenCalledWith(7))
  })

  it('keeps invite dialog values when invite fails', async () => {
    vi.mocked(orgApi.inviteTalent).mockRejectedValueOnce(new Error('invite denied') as never)
    renderPage(<MembersPage />)

    expect(await screen.findByRole('heading', { name: '机构成员' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '邀请达人' }))
    const dialog = await screen.findByRole('dialog', { name: '邀请达人加入机构' })
    fireEvent.change(within(dialog).getByLabelText('达人关键词'), { target: { value: '新' } })
    await waitFor(() => expect(orgApi.searchTalents).toHaveBeenCalledWith('新'))
    await waitFor(() => expect(within(dialog).getByRole('combobox', { name: '选择达人' })).not.toHaveAttribute('aria-disabled', 'true'))
    fireEvent.mouseDown(within(dialog).getByRole('combobox', { name: '选择达人' }))
    fireEvent.click(await screen.findByRole('option', { name: /新达人/ }))
    fireEvent.click(within(dialog).getByRole('button', { name: '发送邀请' }))

    expect(await within(dialog).findByText(/邀请失败：invite denied/)).toBeInTheDocument()
    expect(screen.getByTestId('org-member-invite-error')).toHaveAttribute('data-source-endpoint', '/organization/invite')
    expect(screen.getByTestId('org-member-invite-error')).toHaveAttribute('data-selected-user-id', '9')
    expect(screen.getByTestId('org-member-invite-error')).toHaveAttribute('data-keyword', '新')
    expect(within(dialog).getByText(/\/organization\/invite/)).toBeInTheDocument()
    expect(within(dialog).getByText(/userId=9/)).toBeInTheDocument()
    expect(within(dialog).getByText(/keyword=新/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('新')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '邀请达人加入机构' })).toBeInTheDocument()
  })

  it('renders SDK-normalized organization member payloads', async () => {
    vi.mocked(orgApi.members).mockResolvedValue([
      {
        id: 2,
        userId: 8,
        username: 'wrapped-member',
        nickname: '包装成员',
        roleInOrg: 'operator',
        status: 0,
        invitedAt: '2026-05-21 10:00:00',
      },
    ] as never)

    renderPage(<MembersPage />)

    expect(await screen.findByText('wrapped-member')).toBeInTheDocument()
    expect(screen.getByTestId('org-members-workbench')).toHaveAttribute('data-pending-count', '1')
    expect(screen.getByText('包装成员')).toBeInTheDocument()
    expect(screen.getByText('待确认')).toBeInTheDocument()
  })

  it('shows member source downgrades without fake organization or local talent candidates', async () => {
    vi.mocked(orgApi.my).mockResolvedValue(null as never)
    vi.mocked(orgApi.members).mockResolvedValue([] as never)
    vi.mocked(orgApi.searchTalents).mockRejectedValueOnce(new Error('talent search down') as never)
    renderPage(<MembersPage />)

    expect(await screen.findByRole('heading', { name: '机构成员' })).toBeInTheDocument()
    expect(await screen.findByTestId('org-members-missing-org-downgrade')).toHaveAttribute('data-no-fake-org', 'true')
    expect(screen.getByTestId('org-members-workbench')).toHaveAttribute('data-org-state', 'missing')
    expect(screen.getByTestId('org-member-list-surface')).toHaveAttribute('data-row-count', '0')

    fireEvent.click(screen.getByRole('button', { name: '邀请达人' }))
    fireEvent.change(screen.getByLabelText('达人关键词'), { target: { value: '断链' } })
    const searchError = await screen.findByTestId('org-member-talent-search-error')
    expect(searchError).toHaveAttribute('data-source-endpoint', '/organization/search-talents')
    expect(searchError).toHaveAttribute('data-no-local-candidates', 'true')
    expect(searchError).toHaveTextContent(/不会构造本地达人候选/)
  })

  it('renders org analytics from dashboard and live session snapshot', async () => {
    renderPage(<OrgAnalyticsPage />)

    expect(await screen.findByRole('heading', { name: '机构数据分析' })).toBeInTheDocument()
    await waitFor(() => expect(orgApi.analyticsSnapshot).toHaveBeenCalledWith(30))
    expect(screen.getByText(/独立 `\/org\/analytics\/summary\|trend` 尚未落库/)).toBeInTheDocument()
    const workbench = screen.getByTestId('org-analytics-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'org-analytics')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/dashboard/org/stats,/live/session/search')
    expect(workbench).toHaveAttribute('data-unsupported-endpoints', '/org/analytics/summary,/org/analytics/trend')
    expect(workbench).toHaveAttribute('data-lookback-days', '30')
    expect(workbench).toHaveAttribute('data-session-count', '1')
    expect(workbench).toHaveAttribute('data-trend-row-count', '1')
    expect(screen.getByTestId('org-analytics-contract-downgrade')).toHaveAttribute('data-contract-status', 'degraded')
    const kpiCards = screen.getAllByTestId('org-analytics-kpi-card')
    expect(kpiCards).toHaveLength(6)
    expect(kpiCards.find(card => card.getAttribute('data-kpi-label') === '用户数')).toHaveAttribute('data-contract-status', 'server-source')
    expect(kpiCards.find(card => card.getAttribute('data-kpi-label') === '用户数')).toHaveAttribute('data-source-endpoint', '/dashboard/org/stats')
    expect(kpiCards.find(card => card.getAttribute('data-kpi-label') === '总 GMV')).toHaveAttribute('data-contract-status', 'local-derived')
    expect(kpiCards.find(card => card.getAttribute('data-kpi-label') === '总 GMV')).toHaveAttribute('data-source-endpoint', '/live/session/search')
    expect(screen.getByTestId('org-analytics-trend-surface')).toHaveAttribute('data-unsupported-endpoint', '/org/analytics/trend')
    expect(screen.getByTestId('org-analytics-format-surface')).toHaveAttribute('data-unsupported-endpoint', '/org/analytics/summary')
    expect(screen.getAllByText('¥1,200').length).toBeGreaterThan(0)
    expect(screen.getByText('单人')).toBeInTheDocument()
  })

  it('shows partial analytics source errors without fake trend rows', async () => {
    vi.mocked(orgApi.analyticsSnapshot).mockResolvedValue({
      stats: { totalUsers: 0, todayAiCalls: 0 },
      sessions: [],
      errors: { stats: 'stats down', sessions: 'sessions down' },
    } as never)

    renderPage(<OrgAnalyticsPage />)

    expect(await screen.findByText(/机构统计加载失败：stats down/)).toBeInTheDocument()
    expect(screen.getByTestId('org-analytics-stats-source-error')).toHaveAttribute('data-source-endpoint', '/dashboard/org/stats')
    expect(screen.getByTestId('org-analytics-stats-source-error')).toHaveAttribute('data-no-fake-zero', 'true')
    expect(screen.getAllByText(/\/dashboard\/org\/stats/).length).toBeGreaterThan(0)
    expect(screen.getByText(/直播场次加载失败：sessions down/)).toBeInTheDocument()
    expect(screen.getByTestId('org-analytics-sessions-source-error')).toHaveAttribute('data-source-endpoint', '/live/session/search')
    expect(screen.getByTestId('org-analytics-sessions-source-error')).toHaveAttribute('data-no-mock-sessions', 'true')
    expect(screen.getAllByText(/\/live\/session\/search/).length).toBeGreaterThan(0)
    expect(screen.getByTestId('org-analytics-workbench')).toHaveAttribute('data-session-count', '0')
    expect(screen.getByTestId('org-analytics-trend-surface')).toHaveAttribute('data-trend-row-count', '0')
    expect(screen.getByText('暂无可聚合的场次数据')).toBeInTheDocument()
  })

  it('renders wrapped analytics session payloads', async () => {
    vi.mocked(orgApi.analyticsSnapshot).mockResolvedValue({
      stats: { totalUsers: 8, todayAiCalls: 12 },
      sessions: [
        {
          id: 19,
          status: 1,
          liveFormat: '双人',
          scheduledTime: '2026-05-21 20:00:00',
          viewers: 300,
          cumulativeGmv: 2300,
        },
      ],
    } as never)

    renderPage(<OrgAnalyticsPage />)

    expect(await screen.findByRole('heading', { name: '机构数据分析' })).toBeInTheDocument()
    expect(await screen.findByText('双人')).toBeInTheDocument()
    expect(screen.getByTestId('org-analytics-format-surface')).toHaveAttribute('data-format-row-count', '1')
    expect(screen.getAllByText('¥2,300').length).toBeGreaterThan(0)
  })

  it('updates org analytics lookback window without calling unsupported analytics endpoints', async () => {
    renderPage(<OrgAnalyticsPage />)

    expect(await screen.findByRole('heading', { name: '机构数据分析' })).toBeInTheDocument()
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '分析窗口' }))
    fireEvent.click(await screen.findByRole('option', { name: '近 90 天' }))

    await waitFor(() => expect(orgApi.analyticsSnapshot).toHaveBeenCalledWith(90))
    expect(screen.getByTestId('org-analytics-workbench')).toHaveAttribute('data-lookback-days', '90')
    expect(screen.getByTestId('org-analytics-contract-downgrade')).toHaveAttribute('data-unsupported-endpoints', '/org/analytics/summary,/org/analytics/trend')
  })

  it('renders live reviews and triggers real ai evolution review task', async () => {
    vi.mocked(orgApi.liveReviewTrigger).mockResolvedValue(2 as never)
    vi.mocked(orgApi.liveReviewDelete).mockRejectedValueOnce(new Error('delete denied') as never)
    renderPage(<OrgLiveReviewsPage />)

    expect(await screen.findByRole('heading', { name: '直播复盘' })).toBeInTheDocument()
    await waitFor(() => expect(orgApi.liveReviewList).toHaveBeenCalledWith({ status: undefined, page: 0, rows: 20 }))
    const workbench = screen.getByTestId('org-live-review-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'org-live-review')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/ai/evolution/live-review/list,/ai/evolution/live-review/get,/ai/evolution/live-review/trigger,/ai/evolution/live-review/delete')
    expect(workbench).toHaveAttribute('data-unsupported-actions', 'review-approve,review-reject,manual-complete,server-export')
    expect(workbench).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('org-live-review-approval-downgrade')).toHaveAttribute('data-contract-status', 'degraded')
    expect(screen.getByTestId('org-live-review-list-surface')).toHaveAttribute('data-server-export', 'unsupported')
    expect(screen.queryByText('导出')).not.toBeInTheDocument()
    const reviewCards = screen.getAllByTestId('org-live-review-kpi-card')
    expect(reviewCards).toHaveLength(4)
    expect(reviewCards[0]).toHaveAttribute('data-source-endpoint', '/ai/evolution/live-review/list')
    expect(screen.getByText(/没有组织审批状态/)).toBeInTheDocument()
    expect(await screen.findByText('deepseek')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '触发复盘' }))
    const triggerDialog = await screen.findByRole('dialog', { name: '触发直播复盘' })
    expect(screen.getByTestId('org-live-review-trigger-dialog')).toHaveAttribute('data-contract-endpoint', '/ai/evolution/live-review/trigger')
    expect(screen.getByTestId('org-live-review-trigger-dialog')).toHaveAttribute('data-no-internal-complete', 'true')
    fireEvent.change(within(triggerDialog).getByLabelText(/场次 ID/), { target: { value: '18' } })
    fireEvent.click(within(triggerDialog).getByRole('button', { name: '提交' }))
    await waitFor(() => expect(orgApi.liveReviewTrigger).toHaveBeenCalledWith({ sessionId: 18, accountId: undefined }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: '触发直播复盘' })).not.toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    const deleteDialog = await screen.findByRole('dialog', { name: '删除直播复盘记录' })
    expect(orgApi.liveReviewDelete).not.toHaveBeenCalled()
    expect(screen.getByTestId('org-live-review-delete-dialog')).toHaveAttribute('data-contract-endpoint', '/ai/evolution/live-review/delete')
    expect(screen.getByTestId('org-live-review-delete-dialog')).toHaveAttribute('data-review-id', '2')
    expect(within(deleteDialog).getByText(/reviewId=2/)).toBeInTheDocument()
    expect(within(deleteDialog).getByText(/sessionId=18/)).toBeInTheDocument()
    fireEvent.click(within(deleteDialog).getByRole('button', { name: '确认删除' }))
    await waitFor(() => expect(orgApi.liveReviewDelete).toHaveBeenCalledWith(2))
    expect(await within(deleteDialog).findByText(/delete denied/)).toBeInTheDocument()
    expect(within(deleteDialog).getAllByText(/\/ai\/evolution\/live-review\/delete/).length).toBeGreaterThan(0)
  })

  it('keeps trigger dialog values when live review trigger fails', async () => {
    vi.mocked(orgApi.liveReviewTrigger).mockRejectedValueOnce(new Error('trigger denied') as never)
    renderPage(<OrgLiveReviewsPage />)

    expect(await screen.findByRole('heading', { name: '直播复盘' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '触发复盘' }))
    const dialog = await screen.findByRole('dialog', { name: '触发直播复盘' })
    fireEvent.change(within(dialog).getByLabelText(/场次 ID/), { target: { value: '18' } })
    fireEvent.change(within(dialog).getByLabelText(/账号 ID/), { target: { value: '3' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '提交' }))

    expect(await within(dialog).findByText(/提交复盘失败：trigger denied/)).toBeInTheDocument()
    expect(within(dialog).getByText(/\/ai\/evolution\/live-review\/trigger/)).toBeInTheDocument()
    expect(within(dialog).getByText(/sessionId=18/)).toBeInTheDocument()
    expect(within(dialog).getByText(/accountId=3/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('18')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('3')).toBeInTheDocument()
  })

  it('opens live review detail via get endpoint without approval actions', async () => {
    renderPage(<OrgLiveReviewsPage />)

    expect(await screen.findByRole('heading', { name: '直播复盘' })).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: '详情' }))

    const dialog = await screen.findByRole('dialog', { name: '复盘详情' })
    await waitFor(() => expect(orgApi.liveReviewGet).toHaveBeenCalledWith(2))
    expect(screen.getByTestId('org-live-review-detail-dialog')).toHaveAttribute('data-contract-endpoint', '/ai/evolution/live-review/get')
    expect(screen.getByTestId('org-live-review-detail-dialog')).toHaveAttribute('data-review-id', '2')
    expect(await within(dialog).findByText('详情报告内容')).toBeInTheDocument()
    expect(within(dialog).getByText('详情优秀话术')).toBeInTheDocument()
    expect(within(dialog).getByText('详情薄弱点')).toBeInTheDocument()
    expect(within(dialog).queryByRole('button', { name: '通过' })).not.toBeInTheDocument()
    expect(within(dialog).queryByRole('button', { name: '拒绝' })).not.toBeInTheDocument()
    expect(within(dialog).queryByRole('button', { name: '完成' })).not.toBeInTheDocument()
  })

  it('shows live review detail source errors without fake report content', async () => {
    vi.mocked(orgApi.liveReviewGet).mockRejectedValueOnce(new Error('detail down') as never)
    renderPage(<OrgLiveReviewsPage />)

    expect(await screen.findByRole('heading', { name: '直播复盘' })).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: '详情' }))

    const error = await screen.findByTestId('org-live-review-detail-source-error')
    expect(error).toHaveAttribute('data-source-endpoint', '/ai/evolution/live-review/get')
    expect(error).toHaveTextContent(/detail down/)
    expect(error).toHaveTextContent(/不会用空报告冒充详情成功/)
  })

  it('renders SDK-normalized live review payloads', async () => {
    vi.mocked(orgApi.liveReviewList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 3,
          sessionId: 19,
          status: 2,
          totalGmv: 2300,
          conversionRate: 0.08,
          totalViewers: 300,
          peakViewers: 180,
          modelUsed: 'gpt-4o',
          tokensUsed: 500,
          createTime: '2026-05-21 22:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderPage(<OrgLiveReviewsPage />)

    expect(await screen.findByRole('heading', { name: '直播复盘' })).toBeInTheDocument()
    expect(await screen.findByText('gpt-4o')).toBeInTheDocument()
    expect(screen.getAllByText('失败').length).toBeGreaterThan(0)
  })
})
