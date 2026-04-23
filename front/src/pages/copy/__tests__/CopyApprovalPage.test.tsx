import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import CopyApprovalPage from '../CopyApprovalPage'
import { copyApi } from '@/api/copy'

vi.mock('@/api/copy', () => ({
  copyApi: {
    approvalSearch: vi.fn(),
    approvalApprove: vi.fn(),
    approvalReject: vi.fn(),
    approvalRevise: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('CopyApprovalPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(copyApi.approvalSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          copyId: 88,
          approvalStatus: 0,
          comments: '待审核',
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
      expect(screen.getAllByText('待审批').length).toBeGreaterThan(0)
      expect(screen.getByText('文案 #88')).toBeInTheDocument()
      expect(screen.getByText('待审核')).toBeInTheDocument()
    })
  })
})
