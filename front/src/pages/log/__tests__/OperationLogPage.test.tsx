import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import OperationLogPage from '../OperationLogPage'
import { logApi } from '@/api/log'

vi.mock('@/api/log', () => ({
  logApi: {
    list: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('OperationLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(logApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          username: 'admin',
          module: 'auth',
          action: '登录',
          ip: '127.0.0.1',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads operation logs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OperationLogPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(logApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, username: '', module: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('登录')).toBeInTheDocument()
    })
  })
})
