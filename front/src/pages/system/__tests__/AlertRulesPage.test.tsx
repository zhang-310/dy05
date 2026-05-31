import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AlertRulesPage from '../AlertRulesPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    alertRuleSearch: vi.fn(),
    alertRuleCreate: vi.fn(),
    alertRuleUpdate: vi.fn(),
    alertRuleDelete: vi.fn(),
    alertRuleEnable: vi.fn(),
    alertRuleDisable: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('AlertRulesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.alertRuleSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          ruleName: 'CPU 超限',
          metric: 'cpu',
          threshold: 90,
          operator: '>',
          severity: 'warning',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
    vi.mocked(systemApi.alertRuleEnable).mockResolvedValue(undefined as never)
    vi.mocked(systemApi.alertRuleDisable).mockResolvedValue(undefined as never)
  })

  it('renders title and loads alert rules', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AlertRulesPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '告警规则' })).toBeInTheDocument()
    const workbench = screen.getByTestId('alert-rules-page-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'system-alert-rule-monitoring-endpoints')
    expect(workbench).toHaveAttribute('data-no-local-filter', 'true')
    expect(workbench).toHaveAttribute('data-no-local-rule-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-legacy-system-alert-rule-endpoints', 'true')
    expect(workbench).toHaveAttribute('data-no-optimistic-status-mutation', 'true')

    await waitFor(() => {
      expect(systemApi.alertRuleSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('CPU 超限')).toBeInTheDocument()
      expect(screen.getByText('cpu')).toBeInTheDocument()
      expect(screen.getByText(/只消费分页参数/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('alert-rule-filter-degradation')).toHaveAttribute('data-no-local-filter', 'true')
    expect(screen.getByTestId('alert-rule-filter-degradation')).toHaveAttribute('data-no-legacy-system-alert-rule-endpoints', 'true')
    expect(screen.getByTestId('alert-rule-grid-contract')).toHaveAttribute('data-contract-source', '/monitoring/alert-rules/search')
    expect(screen.getByTestId('alert-rule-grid-contract')).toHaveAttribute('data-no-optimistic-status-mutation', 'true')
  })

  it('disables alert rule through backend enable/disable endpoint', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AlertRulesPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByLabelText('切换 CPU 超限 告警规则'))
    expect(screen.getByTestId('alert-rule-toggle-contract')).toHaveAttribute('data-contract-source', '/monitoring/alert-rules/disable')

    await waitFor(() => {
      expect(systemApi.alertRuleDisable).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('状态已更新', 'success')
    })
  })

  it('renders wrapped alert rule payloads', async () => {
    vi.mocked(systemApi.alertRuleSearch).mockResolvedValue({
      data: {
        records: [
          {
            id: 2,
            ruleName: 'Redis 命中率偏低',
            metric: 'cache.hitRate',
            threshold: 60,
            operator: '<',
            severity: 'critical',
            status: 1,
          },
        ],
        totalElements: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AlertRulesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Redis 命中率偏低')).toBeInTheDocument()
    expect(screen.getByText('cache.hitRate')).toBeInTheDocument()
    expect(screen.getByText('当前页严重')).toBeInTheDocument()
    expect(screen.getByTestId('alert-rules-page-workbench')).toHaveAttribute('data-critical-count', '1')
  })

  it('keeps form input visible when create fails', async () => {
    vi.mocked(systemApi.alertRuleCreate).mockRejectedValue(new Error('create down') as never)

    renderWithProviders(
      <MemoryRouter>
        <AlertRulesPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增规则' }))
    fireEvent.change(screen.getByLabelText('规则名称'), { target: { value: 'Redis 命中率偏低' } })
    fireEvent.change(screen.getByLabelText('监控指标'), { target: { value: 'cache.hitRate' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/\/monitoring\/alert-rules\/create 保存失败：create down/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('alert-rule-form-contract')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('alert-rule-form-contract')).toHaveAttribute('data-rule-name', 'Redis 命中率偏低')
    expect(screen.getByDisplayValue('Redis 命中率偏低')).toBeInTheDocument()
  })

  it('keeps rule row visible when delete or toggle fails', async () => {
    vi.mocked(systemApi.alertRuleDelete).mockRejectedValue(new Error('delete down') as never)
    vi.mocked(systemApi.alertRuleDisable).mockRejectedValue(new Error('toggle down') as never)

    renderWithProviders(
      <MemoryRouter>
        <AlertRulesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('CPU 超限')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(screen.getByTestId('alert-rule-delete-contract')).toHaveAttribute('data-target-id', '1')
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/\/monitoring\/alert-rules\/delete 删除失败：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('alert-rule-operation-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByText('CPU 超限')).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('切换 CPU 超限 告警规则'))
    expect(await screen.findByText(/\/monitoring\/alert-rules\/disable 状态更新失败：toggle down/)).toBeInTheDocument()
    expect(screen.getByTestId('alert-rule-operation-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByText('CPU 超限')).toBeInTheDocument()
  })
})
