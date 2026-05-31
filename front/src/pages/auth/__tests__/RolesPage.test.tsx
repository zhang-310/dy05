import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
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
    vi.mocked(authApi.roleSave).mockResolvedValue(undefined)
    vi.mocked(authApi.roleDelete).mockResolvedValue(undefined)
  })

  it('loads roles and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RolesPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('角色管理')).toBeInTheDocument()
    expect(screen.getByTestId('auth-roles-workbench')).toHaveAttribute('data-contract-scope', 'auth-roles')
    expect(screen.getByTestId('auth-roles-workbench')).toHaveAttribute('data-ready-endpoints', '/auth/role/search,/auth/role/save,/auth/role/delete')
    expect(screen.getByTestId('auth-roles-workbench')).toHaveAttribute('data-degraded-endpoints', '/auth/role/resources,/auth/role/resources/save')
    expect(screen.getByTestId('auth-roles-workbench')).toHaveAttribute('data-no-local-role-fallback', 'true')
    expect(screen.getByTestId('auth-roles-workbench')).toHaveAttribute('data-no-inline-resource-assignment', 'true')
    expect(screen.getByTestId('auth-roles-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')

    await waitFor(() => {
      expect(authApi.roleList).toHaveBeenCalledWith({ page: 0, rows: 20, roleName: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument()
      expect(screen.getByText('admin')).toBeInTheDocument()
    })
  })

  it('renders wrapped role page payloads', async () => {
    vi.mocked(authApi.roleList).mockResolvedValue({
      data: {
        roles: [
          {
            id: 2,
            roleName: '运营',
            roleCode: 'operator',
            status: 0,
            createTime: '2026-04-11 10:00:00',
          },
        ],
        totalCount: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <RolesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('运营')).toBeInTheDocument()
    expect(screen.getByText('operator')).toBeInTheDocument()
    expect(screen.getByText('本页禁用')).toBeInTheDocument()
  })

  it('shows endpoint error when save fails and keeps form input', async () => {
    vi.mocked(authApi.roleSave).mockRejectedValue(new Error('role save down'))

    renderWithProviders(
      <MemoryRouter>
        <RolesPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新建角色' }))
    const dialog = screen.getByRole('dialog', { name: '新建角色' })
    fireEvent.change(within(dialog).getByRole('textbox', { name: /角色名称/ }), { target: { value: '内容运营' } })
    fireEvent.change(within(dialog).getByRole('textbox', { name: /角色编码/ }), { target: { value: 'content_operator' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByTestId('auth-roles-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(await within(dialog).findByText(/\/auth\/role\/save 保存失败：role save down/)).toBeInTheDocument()
    expect(within(dialog).getByText(/route=\/admin\/auth\/roles; roleId=新增; roleName=内容运营; roleCode=content_operator/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('content_operator')).toBeInTheDocument()
  })

  it('shows endpoint error when delete fails and keeps role row plus confirm context', async () => {
    vi.mocked(authApi.roleDelete).mockRejectedValue(new Error('role delete down'))

    renderWithProviders(
      <MemoryRouter>
        <RolesPage />
      </MemoryRouter>,
    )

    await screen.findByText('管理员')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(screen.getByText(/endpoint=\/auth\/role\/delete; route=\/admin\/auth\/roles; roleId=1; roleName=管理员; roleCode=admin/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/\/auth\/role\/delete 删除失败：role delete down/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('auth-roles-action-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getAllByText(/roleId=1; roleName=管理员; roleCode=admin/).length).toBeGreaterThan(0)
    expect(screen.getByText('管理员')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '确认操作' })).toBeInTheDocument()
  })
})
