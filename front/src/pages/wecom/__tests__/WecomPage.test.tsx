import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import WecomPage from '../WecomPage'
import { wecomApi } from '@/api/wecom'

vi.mock('@/api/wecom', () => ({
  wecomApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    updateStatus: vi.fn(),
    push: vi.fn(),
    ruleList: vi.fn(),
    ruleSave: vi.fn(),
    ruleDelete: vi.fn(),
    ruleUpdateStatus: vi.fn(),
    logList: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('WecomPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotName: '告警机器人',
          webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('renders title and loads robot list on default tab', async () => {
    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '企微推送' })).toBeInTheDocument()

    await waitFor(() => {
      expect(wecomApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('告警机器人')).toBeInTheDocument()
    })
  })

  it('filters rules by robot locally after clicking a robot rule count', async () => {
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotName: '告警机器人',
          webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc',
          status: 1,
          createTime: '2026-04-10 10:00:00',
          ruleCount: 2,
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 11,
          robotId: 1,
          ruleName: '开播提醒',
          triggerType: 'live_start',
          triggerConfig: '',
          messageTemplate: '直播开始啦',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
        {
          id: 12,
          robotId: 2,
          ruleName: '其他规则',
          triggerType: 'event',
          triggerConfig: '',
          messageTemplate: '其他',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 2,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('告警机器人')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '查看 告警机器人 的规则' }))

    await waitFor(() => {
      expect(screen.getByText(/当前仅显示机器人「告警机器人」的规则/)).toBeInTheDocument()
    })
  })

  it('saves robot without requiring frontend ownerId', async () => {
    vi.mocked(wecomApi.save).mockResolvedValue(undefined as never)

    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '添加机器人' }))
    fireEvent.change(await screen.findByRole('textbox', { name: /机器人名称/ }), { target: { value: '测试机器人' } })
    fireEvent.change(screen.getByRole('textbox', { name: /Webhook URL/ }), {
      target: { value: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(wecomApi.save).toHaveBeenCalledWith(expect.objectContaining({
        robotName: '测试机器人',
        webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test',
      }))
    })
  })

  it('shows source endpoint when robot save fails and keeps dialog open', async () => {
    vi.mocked(wecomApi.save).mockRejectedValue(new Error('robot save down'))

    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '添加机器人' }))
    const dialog = screen.getByRole('dialog', { name: '添加机器人' })
    fireEvent.change(screen.getByRole('textbox', { name: /机器人名称/ }), { target: { value: '失败机器人' } })
    fireEvent.change(screen.getByRole('textbox', { name: /Webhook URL/ }), {
      target: { value: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/\/wecom\/robot\/save 保存失败：robot save down/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/robotName=失败机器人/).length).toBeGreaterThan(0)
    expect(dialog).toBeInTheDocument()
  })

  it('shows source endpoint when rule save or manual push fails', async () => {
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 11,
          robotId: 1,
          ruleName: '开播提醒',
          triggerType: 'live_start',
          triggerConfig: '{}',
          messageTemplate: '直播开始啦',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 1,
    } as never)
    vi.mocked(wecomApi.ruleSave).mockRejectedValue(new Error('rule save down'))
    vi.mocked(wecomApi.push).mockRejectedValue(new Error('push down'))

    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '推送规则' }))
    fireEvent.click(await screen.findByRole('button', { name: '新建规则' }))
    fireEvent.change(screen.getByRole('textbox', { name: '规则名称' }), { target: { value: '失败规则' } })
    fireEvent.change(screen.getByRole('textbox', { name: /消息模板/ }), { target: { value: '失败推送' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect((await screen.findAllByText(/\/wecom\/rule\/save 保存失败：rule save down/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/ruleName=失败规则/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    fireEvent.click(screen.getByRole('button', { name: '手动触发' }))
    expect(screen.getByText(/ruleId=11/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认触发' }))
    expect(await screen.findByText(/\/wecom\/push 手动触发失败：push down/)).toBeInTheDocument()
    expect(screen.getByText(/ruleName=开播提醒/)).toBeInTheDocument()
  })

  it('renders capability states and tolerates wrapped robot responses', async () => {
    vi.mocked(wecomApi.list).mockResolvedValue({
      data: {
        rows: [
          {
            id: 5,
            robotName: '包装机器人',
            webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=wrapped',
            status: 1,
            createTime: '2026-05-22 10:00:00',
          },
        ],
        totalRecords: 1,
      },
      total: 1,
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装机器人')).toBeInTheDocument()
    expect(screen.getAllByText('已接入').length).toBeGreaterThanOrEqual(3)
    expect(screen.getAllByText('显式降级').length).toBeGreaterThanOrEqual(3)
    expect(screen.getByText(/未提供 \/wecom\/log\/retry/)).toBeInTheDocument()
  })

  it('exposes auditable wecom contract states without unsupported endpoints', async () => {
    vi.mocked(wecomApi.logList).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 31,
          robotId: 1,
          robotName: '告警机器人',
          ruleId: null,
          messageContent: '发送成功',
          status: 1,
          createTime: '2026-05-22 10:00:00',
        },
        {
          id: 32,
          robotId: 1,
          robotName: '告警机器人',
          ruleId: null,
          messageContent: '发送失败',
          status: 0,
          errorMessage: 'webhook timeout',
          createTime: '2026-05-22 10:01:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    const workbench = await screen.findByTestId('wecom-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'wecom-push')
    expect(workbench).toHaveAttribute('data-unsupported-actions', 'template-crud,push-stats,log-retry')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/wecom/push')

    const cards = screen.getAllByTestId('wecom-capability-card')
    expect(cards).toHaveLength(6)
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'template-crud')).toHaveAttribute('data-contract-status', 'degraded')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'push-stats')).toHaveAttribute('data-contract-endpoint', '/wecom/statistics/summary')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'log-retry')).toHaveAttribute('data-contract-status', 'unsupported')

    const logTab = screen.getByRole('tab', { name: '推送日志' })
    fireEvent.click(logTab)
    await waitFor(() => {
      expect(logTab).toHaveAttribute('aria-selected', 'true')
    })
    const logWorkbench = await screen.findByTestId('wecom-log-workbench')
    expect(logWorkbench).toHaveAttribute('data-contract-status', 'degraded')
    expect(logWorkbench).toHaveAttribute('data-degraded-endpoint', '/wecom/statistics/summary')
    expect(screen.getByTestId('wecom-log-stats-downgrade')).toHaveAttribute('data-source-endpoint', '/wecom/log/list')
    expect(screen.getByTestId('wecom-log-stats-downgrade')).toHaveAttribute('data-unsupported-endpoint', '/wecom/log/retry')
    expect(screen.getAllByTestId('wecom-log-kpi-card')[0]).toHaveAttribute('data-contract-status', 'local-derived')
    expect(await screen.findByTestId('wecom-log-retry-action')).toHaveAttribute('data-contract-endpoint', '/wecom/log/retry')
    expect(screen.getByTestId('wecom-log-retry-action')).toHaveAttribute('data-no-clickable-retry', 'true')
    expect(screen.queryByRole('button', { name: '重发' })).not.toBeInTheDocument()

    const templateTab = screen.getByRole('tab', { name: '消息模板' })
    fireEvent.click(templateTab)
    await waitFor(() => {
      expect(templateTab).toHaveAttribute('aria-selected', 'true')
    })
    expect(await screen.findByTestId('wecom-template-workbench')).toHaveAttribute('data-fallback-field', 'rule.messageTemplate')
    expect(screen.getByTestId('wecom-template-crud-downgrade')).toHaveAttribute('data-contract-endpoint', '/wecom/template/*')
    expect(screen.getByTestId('wecom-template-fallback-surface')).toHaveAttribute('data-contract-status', 'local-planning')

    expect(wecomApi.logList).toHaveBeenCalledWith(expect.objectContaining({ rows: 20 }))
    expect(wecomApi.push).not.toHaveBeenCalled()
  })

  it('uses theme-aware trigger and template variable surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 21,
          robotId: 1,
          ruleName: '事件触发规则',
          triggerType: 'event',
          triggerConfig: '{}',
          messageTemplate: '【{直播间}】{主播} 触发事件',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 1,
    } as never)

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <WecomPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '推送规则' }))
    expect(await screen.findByText('事件触发规则')).toBeInTheDocument()
    expect(screen.getByTestId('wecom-rule-trigger-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 247, 237)',
    })
    expect(screen.getByTestId('wecom-rule-trigger-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })

    fireEvent.click(screen.getByRole('tab', { name: '消息模板' }))
    expect(await screen.findByText('模板变量约定')).toBeInTheDocument()
    expect(screen.getAllByTestId('wecom-template-variable-chip-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(255, 247, 237)',
    })
  })
})
