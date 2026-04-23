import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import RolesPage from '../RolesPage'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    roleList: vi.fn(),
    roleSave: vi.fn(),
    roleDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('RolesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(authApi.roleList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          roleName: '管理员',
          roleCode: 'admin',
          description: '平台管理员',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads roles and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RolesPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('角色管理')).toBeInTheDocument()

    await waitFor(() => {
      expect(authApi.roleList).toHaveBeenCalledWith({ page: 0, rows: 20, roleName: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument()
      expect(screen.getByText('admin')).toBeInTheDocument()
    })
  })
})
