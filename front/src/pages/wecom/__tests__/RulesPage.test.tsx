import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import RulesPage from '../RulesPage'
import { wecomApi } from '@/api/wecom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

vi.mock('@/api/wecom', () => ({
  wecomApi: {
    list: vi.fn(),
    ruleList: vi.fn(),
    ruleSave: vi.fn(),
    ruleDelete: vi.fn(),
    ruleUpdateStatus: vi.fn(),
    push: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('RulesPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [{ id: 1, robotName: '告警机器人' }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotId: 1,
          ruleName: '开播提醒',
          triggerType: 'live_start',
          triggerConfig: '',
          messageTemplate: '直播开始啦',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 100,
    } as never)
  })

  it('loads rules and renders rule card content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(wecomApi.ruleList).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getAllByText('开播提醒').length).toBeGreaterThan(0)
      expect(screen.getByText('新增规则')).toBeInTheDocument()
    })
  })

  it('shows explicit template downgrade tab without calling missing backend template APIs', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '消息模板' }))

    expect(screen.getByText(/消息模板 CRUD 后端接口尚未接入/)).toBeInTheDocument()
    expect(screen.getByTestId('wecom-rule-template-workbench')).toHaveAttribute('data-contract-endpoint', '/wecom/template/*')
    expect(screen.getByTestId('wecom-rule-template-downgrade')).toHaveAttribute('data-fallback-field', 'rule.messageTemplate')
    expect(screen.getByTestId('wecom-rule-template-fallback-surface')).toHaveAttribute('data-contract-status', 'local-planning')
    expect(screen.getAllByText('{直播间}').length).toBeGreaterThan(0)
    expect(screen.getAllByTestId('wecom-rule-template-variable-chip')[0]).toHaveAttribute('data-contract-field', 'template-variable')
  })

  it('renders rule capability states and wrapped rule data', async () => {
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      data: {
        rows: [
          {
            id: 9,
            robotId: 1,
            ruleName: '包装规则',
            triggerType: 'manual',
            triggerConfig: '{}',
            messageTemplate: '包装消息',
            status: 1,
            createTime: '2026-05-22 10:00:00',
          },
        ],
        totalRecords: 1,
      },
      total: 1,
      list: [
        {
          id: 9,
          robotId: 1,
          ruleName: '包装规则',
          triggerType: 'manual',
          triggerConfig: '{}',
          messageTemplate: '包装消息',
          status: 1,
          createTime: '2026-05-22 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 100,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装规则')).toBeInTheDocument()
    expect(screen.getAllByText('已接入').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('显式降级').length).toBeGreaterThanOrEqual(2)
  })

  it('exposes auditable rule contracts and manual trigger downgrade', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    const page = await screen.findByTestId('wecom-rule-page')
    expect(page).toHaveAttribute('data-contract-scope', 'wecom-rule')
    expect(page).toHaveAttribute('data-unsupported-actions', 'template-crud,template-reference')
    expect(page.getAttribute('data-ready-endpoints')).toContain('/wecom/push')

    const workbench = await screen.findByTestId('wecom-rules-workbench')
    expect(workbench).toHaveAttribute('data-rule-count', '1')
    expect(workbench).toHaveAttribute('data-enabled-rule-count', '1')

    const cards = screen.getAllByTestId('wecom-rule-capability-card')
    expect(cards).toHaveLength(4)
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'rule-list')).toHaveAttribute('data-contract-status', 'ready')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'manual-trigger-via-push')).toHaveAttribute('data-source-field', 'rule.messageTemplate')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'template-crud')).toHaveAttribute('data-contract-endpoint', '/wecom/template/*')

    expect(screen.getByTestId('wecom-rule-manual-trigger-downgrade')).toHaveAttribute('data-contract-endpoint', '/wecom/push')
    expect(screen.getByTestId('wecom-rule-manual-trigger-downgrade')).toHaveAttribute('data-source-field', 'rule.messageTemplate')
    expect(screen.getAllByTestId('wecom-rule-kpi-card').find(card => card.textContent?.includes('手动触发'))).toHaveAttribute('data-contract-status', 'degraded')
    expect(await screen.findByTestId('wecom-rule-manual-trigger-action')).toHaveAttribute('data-contract-action', 'manual-trigger-via-push')

    expect(wecomApi.push).not.toHaveBeenCalled()
  })

  it('uses theme-aware trigger colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      total: 8,
      list: [
        { id: 1, robotId: 1, ruleName: '手动规则', triggerType: 'manual', triggerConfig: '{}', messageTemplate: '手动消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 2, robotId: 1, ruleName: '定时规则', triggerType: 'schedule', triggerConfig: '{}', messageTemplate: '定时消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 3, robotId: 1, ruleName: '事件规则', triggerType: 'event', triggerConfig: '{}', messageTemplate: '事件消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 4, robotId: 1, ruleName: 'GMV规则', triggerType: 'gmv_milestone', triggerConfig: '{}', messageTemplate: 'GMV消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 5, robotId: 1, ruleName: '审批规则', triggerType: 'script_approve', triggerConfig: '{}', messageTemplate: '审批消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 6, robotId: 1, ruleName: '开播规则', triggerType: 'live_start', triggerConfig: '{}', messageTemplate: '开播消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 7, robotId: 1, ruleName: '告警规则', triggerType: 'alert', triggerConfig: '{}', messageTemplate: '告警消息', status: 1, createTime: '2026-05-22 10:00:00' },
        { id: 8, robotId: 1, ruleName: '未知规则', triggerType: 'custom_event', triggerConfig: '{}', messageTemplate: '未知消息', status: 1, createTime: '2026-05-22 10:00:00' },
      ],
      pageNum: 0,
      pageSize: 100,
    } as never)

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <RulesPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByText('手动规则')).toBeInTheDocument()
    const cards = await screen.findAllByTestId('wecom-rule-trigger-card-surface')
    const chips = await screen.findAllByTestId('wecom-rule-trigger-chip-surface')
    expect(cards.map(node => node.getAttribute('data-trigger-tone'))).toEqual([
      'primary',
      'success',
      'warning',
      'error',
      'info',
      'success',
      'warning',
      'default',
    ])
    expect(chips.map(node => node.getAttribute('data-trigger-color'))).toEqual([
      '#e3f2fd',
      '#81c784',
      '#ffb74d',
      '#e57373',
      '#4fc3f7',
      '#81c784',
      '#ffb74d',
      'rgba(255, 255, 255, 0.5)',
    ])
    for (const legacy of ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#999', '#fff']) {
      expect(chips.map(node => node.getAttribute('data-trigger-color')).join('|')).not.toContain(legacy)
      expect(cards.map(node => node.getAttribute('data-trigger-color')).join('|')).not.toContain(legacy)
    }
  })

  it('saves rule with default trigger config when omitted', async () => {
    vi.mocked(wecomApi.ruleSave).mockResolvedValue(undefined as never)

    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增规则' }))
    fireEvent.change(screen.getByLabelText('规则名称'), { target: { value: '手动通知' } })
    fireEvent.change(screen.getByLabelText('消息模板（支持 {变量} 占位符）'), { target: { value: '测试消息' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(wecomApi.ruleSave).toHaveBeenCalledWith(expect.objectContaining({
        robotId: 1,
        ruleName: '手动通知',
        triggerType: 'manual',
        triggerConfig: '{}',
        messageTemplate: '测试消息',
      }))
    })
  })

  it('shows source endpoint when rule save or manual push fails', async () => {
    vi.mocked(wecomApi.ruleSave).mockRejectedValue(new Error('rule save down'))
    vi.mocked(wecomApi.push).mockRejectedValue(new Error('manual push down'))

    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增规则' }))
    fireEvent.change(screen.getByLabelText('规则名称'), { target: { value: '失败规则' } })
    fireEvent.change(screen.getByLabelText('消息模板（支持 {变量} 占位符）'), { target: { value: '失败消息' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect((await screen.findAllByText(/\/wecom\/rule\/save 保存失败：rule save down/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/ruleName=失败规则/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    fireEvent.click(screen.getByRole('button', { name: '触发' }))
    expect(screen.getByText(/ruleId=1/)).toBeInTheDocument()
    expect(screen.getByText(/robotId=1/)).toBeInTheDocument()
    expect(screen.getByTestId('wecom-rule-manual-trigger-dialog')).toHaveAttribute('data-contract-endpoint', '/wecom/push')
    fireEvent.click(screen.getByRole('button', { name: '确认触发' }))
    expect(await screen.findByText(/\/wecom\/push 手动触发失败：manual push down/)).toBeInTheDocument()
    expect(screen.getByText(/ruleName=开播提醒/)).toBeInTheDocument()
  })
})
