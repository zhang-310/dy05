import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import CopyApprovalPage from '../CopyApprovalPage'
import { copyApi } from '@/api/copy'

vi.mock('@/api/copy', () => ({
  copyApi: {
    approvalSearch: vi.fn(),
    approvalApprove: vi.fn(),
    approvalReject: vi.fn(),
    approvalRevise: vi.fn(),
    approvalGet: vi.fn(),
    approvalSave: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('CopyApprovalPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(copyApi.approvalSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          copyId: 88,
          approvalStatus: 2,
          comments: '待审核',
          copyTitle: '护肤开场文案',
          copyContent: '大家好，欢迎来到直播间',
          approvalTime: '',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 50,
    } as never)
  })

  it('loads approvals and renders kanban card content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(copyApi.approvalSearch).toHaveBeenCalledWith({ page: 0, rows: 50, approvalStatus: undefined })
    })

    await waitFor(() => {
      expect(screen.getAllByText('待审核').length).toBeGreaterThan(0)
      expect(screen.getByText('文案 #88')).toBeInTheDocument()
      expect(screen.getByText('护肤开场文案')).toBeInTheDocument()
    })

    const root = screen.getByTestId('copy-approval-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'copy-approval-standalone')
    expect(root).toHaveAttribute('data-ready-endpoints', '/copy/approval/search,/copy/approval/get,/copy/approval/save')
    expect(root).toHaveAttribute('data-unsupported-actions', 'approval-revise,approval-stats,server-export')
    expect(root).toHaveAttribute('data-unsupported-endpoints', '/copy/approval/revise,/copy/approval/stats,/copy/approval/export')
    expect(root).toHaveAttribute('data-active-filter', 'all')
    expect(root).toHaveAttribute('data-pending-count', '1')
    expect(screen.getByTestId('copy-approval-contract-alert')).toHaveAttribute('data-no-stats-request', 'true')

    const columns = screen.getAllByTestId('copy-approval-kanban-column-surface')
    expect(columns).toHaveLength(3)
    expect(columns[0]).toHaveAttribute('data-approval-status', '2')
    expect(columns[0]).toHaveAttribute('data-contract-status', 'server-list-grouped')
    expect(columns[0]).toHaveAttribute('data-item-count', '1')
    expect(screen.getByTestId('copy-approval-card')).toHaveAttribute('data-contract-status', 'server-source')
  })

  it('filters approvals by status through the real search endpoint', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    await screen.findByText('文案 #88')
    const approvedFilter = screen.getAllByTestId('copy-approval-status-filter')
      .find(node => node.getAttribute('data-filter-status') === '1')
    expect(approvedFilter).toBeTruthy()
    fireEvent.click(approvedFilter!)

    await waitFor(() => {
      expect(copyApi.approvalSearch).toHaveBeenCalledWith({ page: 0, rows: 50, approvalStatus: 1 })
    })
    expect(screen.getByTestId('copy-approval-workbench')).toHaveAttribute('data-active-filter', '1')
    expect(approvedFilter).toHaveAttribute('data-contract-action', 'filter-by-status')
  })

  it('shows empty state when there are no approval records', async () => {
    vi.mocked(copyApi.approvalSearch).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 50,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('暂无审批记录。请先从文案库提交审批。')).toBeInTheDocument()
  })

  it('opens pending approval detail and triggers approve action', async () => {
    vi.mocked(copyApi.approvalApprove).mockResolvedValue(undefined as never)

    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByText('文案 #88'))
    expect(screen.getByTestId('copy-approval-detail-drawer')).toHaveAttribute('data-approval-id', '1')
    expect(screen.getByTestId('copy-approval-detail-drawer')).toHaveAttribute('data-unsupported-endpoint', '/copy/approval/revise')
    expect(screen.getByTestId('copy-approval-revise-degradation')).toHaveAttribute('data-no-revise-request', 'true')
    fireEvent.click(screen.getByRole('button', { name: /通过/ }))

    await waitFor(() => {
      expect(copyApi.approvalApprove).toHaveBeenCalledWith(1, '')
    })
    expect(copyApi.approvalRevise).not.toHaveBeenCalled()
  })

  it('triggers reject action through get and save helper without local card movement', async () => {
    vi.mocked(copyApi.approvalReject).mockResolvedValue(undefined as never)

    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByText('文案 #88'))
    fireEvent.change(screen.getByLabelText('审批意见'), { target: { value: '卖点不清晰' } })
    fireEvent.click(screen.getByRole('button', { name: /拒绝/ }))

    await waitFor(() => {
      expect(copyApi.approvalReject).toHaveBeenCalledWith(1, '卖点不清晰')
    })
    expect(copyApi.approvalRevise).not.toHaveBeenCalled()
  })

  it('shows retryable error when approval list fails', async () => {
    vi.mocked(copyApi.approvalSearch).mockRejectedValueOnce(new Error('approval backend down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/\/copy\/approval\/search 审批列表加载失败：approval backend down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-approval-list-error')).toHaveAttribute('data-no-local-cards', 'true')
  })

  it('shows source endpoint when approve fails and keeps detail drawer open', async () => {
    vi.mocked(copyApi.approvalApprove).mockRejectedValue(new Error('approve down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyApprovalPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByText('文案 #88'))
    fireEvent.change(screen.getByLabelText('审批意见'), { target: { value: '通过失败' } })
    fireEvent.click(screen.getByRole('button', { name: /通过/ }))

    await waitFor(() => {
      expect(screen.getAllByText(/\/copy\/approval\/get \+ \/copy\/approval\/save 审批失败：approve down/).length).toBeGreaterThan(0)
    })
    expect(screen.getByText('审批详情')).toBeInTheDocument()
    expect(screen.getByTestId('copy-approval-detail-drawer')).toHaveAttribute('data-approval-id', '1')
  })

  it('uses theme-aware kanban column surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <CopyApprovalPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect((await screen.findAllByTestId('copy-approval-kanban-column-surface'))[0]).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
  })
})
