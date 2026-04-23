import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
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
    vi.mocked(logApi.auditLogPage).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          userId: 1,
          username: 'admin',
          module: 'product',
          action: 'update',
          targetId: 5,
          targetType: 'product',
          beforeValue: '{"name":"旧"}',
          afterValue: '{"name":"新"}',
          ip: '127.0.0.1',
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
      expect(logApi.auditLogPage).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getAllByText('product').length).toBeGreaterThan(0)
    })
  })
})
