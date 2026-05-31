import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders, screen, waitFor, fireEvent } from '@/test/utils'
import AuditLogPage from '../AuditLogPage'
import { logApi } from '@/api/log'

vi.mock('@/api/log', () => ({
  logApi: {
    auditLogPage: vi.fn(),
  },
}))

describe('AuditLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(logApi.auditLogPage).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          userId: 1,
          username: 'admin',
          entity: 'product',
          entityId: 5,
          action: 'update',
          oldValue: '{"name":"旧"}',
          newValue: '{"name":"新"}',
          ip: '127.0.0.1',
          userAgent: 'Mozilla/5.0',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads audit logs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AuditLogPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('审计日志')).toBeInTheDocument()

    await waitFor(() => {
      expect(logApi.auditLogPage).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        keyword: undefined,
        username: undefined,
        entity: undefined,
        action: undefined,
        status: undefined,
        startTime: undefined,
        endTime: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getAllByText('product').length).toBeGreaterThan(0)
    })

    expect(screen.getByTestId('audit-log-page-workbench')).toHaveAttribute('data-contract-scope', 'log-audit-readonly-detail')
    expect(screen.getByTestId('audit-log-page-workbench')).toHaveAttribute('data-ready-endpoints', '/log/audit/search')
    expect(screen.getByTestId('audit-log-source-contract')).toHaveAttribute('data-no-synthetic-detail-fetch', 'true')
    expect(screen.getByTestId('audit-log-grid-contract')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('audit-log-search-contract')).toHaveAttribute('data-no-client-side-filter-only', 'true')

    fireEvent.click(screen.getByTestId('audit-log-detail-action'))

    await waitFor(() => {
      expect(screen.getByText('修改前')).toBeInTheDocument()
      expect(screen.getByText('修改后')).toBeInTheDocument()
    })
    expect(screen.getByTestId('audit-log-detail-contract')).toHaveAttribute('data-contract-source', '/log/audit/search')
    expect(screen.getByTestId('audit-log-detail-contract')).toHaveAttribute('data-no-synthetic-detail-fetch', 'true')
    expect(screen.getByTestId('audit-log-detail-contract')).toHaveAttribute('data-no-client-side-value-diff', 'true')
  })

  it('renders wrapped audit log payloads', async () => {
    vi.mocked(logApi.auditLogPage).mockResolvedValue({
      rows: [
        {
          id: 2,
          username: 'auditor',
          entity: 'config',
          entityId: 9,
          action: 'update',
          beforeValue: '{"enabled":false}',
          afterValue: '{"enabled":true}',
          status: 0,
          errorMsg: 'write rejected',
        },
      ],
      totalRecords: 1,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AuditLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('auditor')).toBeInTheDocument()
    expect(screen.getAllByText('config').length).toBeGreaterThan(0)
    expect(screen.getByText('失败审计')).toBeInTheDocument()
  })

  it('uses theme-aware before and after value surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AuditLogPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(await screen.findByTestId('audit-log-detail-action'))

    expect(await screen.findByTestId('audit-log-before-value-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(238, 238, 238)',
    })
    expect(screen.getByTestId('audit-log-after-value-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(232, 245, 233)',
    })
  })

  it('shows missing before and after values without generating local diffs', async () => {
    vi.mocked(logApi.auditLogPage).mockResolvedValue({
      list: [
        {
          id: 3,
          username: 'ops',
          entity: 'login',
          entityId: null,
          action: 'create',
          ip: '127.0.0.1',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      total: 1,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AuditLogPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByTestId('audit-log-detail-action'))

    expect(await screen.findByTestId('audit-log-before-missing')).toHaveAttribute('data-no-local-before-after-fallback', 'true')
    expect(screen.getByTestId('audit-log-after-missing')).toHaveAttribute('data-no-local-before-after-fallback', 'true')
    expect(screen.getByTestId('audit-log-detail-contract')).toHaveAttribute('data-has-before', 'false')
    expect(screen.getByTestId('audit-log-detail-contract')).toHaveAttribute('data-has-after', 'false')
  })
})
