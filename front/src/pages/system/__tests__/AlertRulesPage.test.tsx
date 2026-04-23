import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
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

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
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
  })

  it('renders title and loads alert rules', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AlertRulesPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('告警规则')).toBeInTheDocument()

    await waitFor(() => {
      expect(systemApi.alertRuleSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('CPU 超限')).toBeInTheDocument()
      expect(screen.getByText('cpu')).toBeInTheDocument()
    })
  })
})
