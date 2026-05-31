import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import UsersPage from '../UsersPage'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    ban: vi.fn(),
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
    vi.mocked(authApi.save).mockResolvedValue(undefined)
    vi.mocked(authApi.delete).mockResolvedValue(undefined)
    vi.mocked(authApi.ban).mockResolvedValue(undefined)
  })

  it('renders title and loads user list', async () => {
    renderWithProviders(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('用户管理')).toBeInTheDocument()
    expect(screen.getByTestId('auth-users-workbench')).toHaveAttribute('data-contract-scope', 'auth-users')
    expect(screen.getByTestId('auth-users-workbench')).toHaveAttribute('data-ready-endpoints', '/auth/user/search,/auth/user/save,/auth/user/delete,/auth/user/ban')
    expect(screen.getByTestId('auth-users-workbench')).toHaveAttribute('data-no-local-user-fallback', 'true')
    expect(screen.getByTestId('auth-users-workbench')).toHaveAttribute('data-no-local-status-mutation', 'true')
    expect(screen.getByTestId('auth-users-contract-downgrade')).toHaveAttribute('data-no-password-echo', 'true')

    await waitFor(() => {
      expect(authApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, username: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument()
      expect(screen.getByText('Alice')).toBeInTheDocument()
    })
  })

  it('renders wrapped user page payloads', async () => {
    vi.mocked(authApi.list).mockResolvedValue({
      payload: {
        users: [
          {
            id: 2,
            username: 'bob',
            nickname: 'Bob',
            email: 'bob@example.com',
            status: 0,
            createTime: '2026-04-11 10:00:00',
          },
        ],
        totalElements: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('bob')).toBeInTheDocument()
    expect(screen.getByText('Bob')).toBeInTheDocument()
    expect(screen.getByText('本页禁用')).toBeInTheDocument()
  })

  it('shows endpoint error when save fails and keeps form input', async () => {
    vi.mocked(authApi.save).mockRejectedValue(new Error('save down'))

    renderWithProviders(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增用户' }))
    const dialog = screen.getByRole('dialog', { name: '新增用户' })
    fireEvent.change(within(dialog).getByLabelText('用户名'), { target: { value: 'charlie' } })
    fireEvent.change(within(dialog).getByLabelText('昵称'), { target: { value: 'Charlie' } })
    fireEvent.change(within(dialog).getByLabelText('角色编码'), { target: { value: 'operator' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByTestId('auth-users-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(within(dialog).getByTestId('auth-users-save-error')).toHaveAttribute('data-no-password-echo', 'true')
    expect(await within(dialog).findByText(/\/auth\/user\/save 保存失败：save down/)).toBeInTheDocument()
    expect(within(dialog).getByText(/route=\/admin\/auth\/users; userId=新增; username=charlie; nickname=Charlie; roleCode=operator/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('charlie')).toBeInTheDocument()
  })

  it('shows endpoint error when delete fails and keeps user row plus confirm context', async () => {
    vi.mocked(authApi.delete).mockRejectedValue(new Error('delete down'))

    renderWithProviders(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    await screen.findByText('alice')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(screen.getByText(/endpoint=\/auth\/user\/delete; route=\/admin\/auth\/users; userId=1; username=alice; nickname=Alice/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/\/auth\/user\/delete 删除失败：delete down/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('auth-users-action-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getAllByText(/userId=1; username=alice; nickname=Alice/).length).toBeGreaterThan(0)
    expect(screen.getByText('alice')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '确认操作' })).toBeInTheDocument()
  })

  it('calls real ban endpoint and keeps row when status update fails', async () => {
    vi.mocked(authApi.ban).mockRejectedValue(new Error('ban down'))

    renderWithProviders(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    await screen.findByText('alice')
    fireEvent.click(screen.getByRole('button', { name: '禁用' }))
    expect(screen.getByText(/endpoint=\/auth\/user\/ban; route=\/admin\/auth\/users; userId=1; username=alice; nickname=Alice/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(authApi.ban).toHaveBeenCalledWith({
        userId: 1,
        ban: true,
        reason: '管理员在用户管理页禁用',
      })
    })
    expect((await screen.findAllByText(/\/auth\/user\/ban 启停失败：ban down/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('auth-users-action-error')).toHaveAttribute('data-no-local-status-mutation', 'true')
    expect(screen.getAllByText(/targetStatus=0\/禁用/).length).toBeGreaterThan(0)
    expect(screen.getByText('alice')).toBeInTheDocument()
  })
})
