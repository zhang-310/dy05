import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import UsersPage from '../UsersPage'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('UsersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(authApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          username: 'alice',
          nickname: 'Alice',
          email: 'alice@example.com',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('renders title and loads user list', async () => {
    renderWithProviders(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('用户管理')).toBeInTheDocument()

    await waitFor(() => {
      expect(authApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, username: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument()
      expect(screen.getByText('Alice')).toBeInTheDocument()
    })
  })
})
