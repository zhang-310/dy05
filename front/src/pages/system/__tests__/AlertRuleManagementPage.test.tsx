import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import AlertRuleManagementPage from '../AlertRuleManagementPage'
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

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('AlertRuleManagementPage', () => {
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
  })

  it('reuses the consolidated monitoring alert rules page instead of legacy system endpoints', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AlertRuleManagementPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '告警规则' })).toBeInTheDocument()
    expect(screen.getByTestId('alert-rule-management-wrapper')).toHaveAttribute('data-contract-scope', 'legacy-system-alert-rule-management-wrapper')
    expect(screen.getByTestId('alert-rule-management-wrapper')).toHaveAttribute('data-child-page', 'AlertRulesPage')
    expect(screen.getByTestId('alert-rule-management-wrapper')).toHaveAttribute('data-no-legacy-system-alert-rule-endpoints', 'true')

    await waitFor(() => {
      expect(systemApi.alertRuleSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    expect(await screen.findByText('CPU 超限')).toBeInTheDocument()
    expect(screen.getAllByText(/monitoring\/alert-rules/).length).toBeGreaterThan(0)
  })
})
