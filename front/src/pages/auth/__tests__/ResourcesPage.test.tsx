import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ResourcesPage from '../ResourcesPage'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    resourceList: vi.fn(),
    resourceSave: vi.fn(),
    resourceDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ResourcesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(authApi.resourceList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          parentId: 0,
          resourceName: '用户管理',
          resourceCode: 'auth:user:list',
          resourceType: 'menu',
          requestMethod: 'POST',
          module: 'auth',
          sortOrder: 1,
        },
      ],
      pageNum: 0,
      pageSize: 50,
    })
    vi.mocked(authApi.resourceSave).mockResolvedValue(undefined)
    vi.mocked(authApi.resourceDelete).mockResolvedValue(undefined)
  })

  it('loads resources and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(authApi.resourceList).toHaveBeenCalledWith({
        page: 0,
        rows: 50,
        resourceName: undefined,
        module: undefined,
        resourceType: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('用户管理')).toBeInTheDocument()
      expect(screen.getByText('auth:user:list')).toBeInTheDocument()
    })
  })

  it('renders wrapped resource page payloads', async () => {
    vi.mocked(authApi.resourceList).mockResolvedValue({
      body: {
        resources: [
          {
            id: 2,
            parentId: 0,
            resourceName: '登录接口',
            resourceCode: 'auth:login',
            resourceType: 'api',
            requestMethod: 'POST',
            module: 'auth',
            sortOrder: 2,
          },
        ],
        totalRecords: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('auth-resources-workbench')).toHaveAttribute('data-contract-scope', 'auth-resources')
    expect(screen.getByTestId('auth-resources-workbench')).toHaveAttribute('data-ready-endpoints', '/auth/resource/list,/auth/resource/save,/auth/resource/delete')
    expect(screen.getByTestId('auth-resources-workbench')).toHaveAttribute('data-degraded-endpoints', '/auth/resource/tree,/auth/resource/tree-full')
    expect(screen.getByTestId('auth-resources-workbench')).toHaveAttribute('data-no-local-resource-fallback', 'true')
    expect(screen.getByTestId('auth-resources-workbench')).toHaveAttribute('data-no-paged-list-as-tree', 'true')
    expect(screen.getByTestId('auth-resources-contract-downgrade')).toHaveAttribute('data-no-paged-list-as-tree', 'true')

    expect(await screen.findByText('登录接口')).toBeInTheDocument()
    expect(screen.getByText('auth:login')).toBeInTheDocument()
    expect(screen.getByText('本页接口')).toBeInTheDocument()
  })

  it('shows endpoint error when save fails and keeps form input', async () => {
    vi.mocked(authApi.resourceSave).mockRejectedValue(new Error('resource save down'))

    renderWithProviders(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增资源' }))
    const dialog = screen.getByRole('dialog', { name: '新增资源' })
    fireEvent.change(within(dialog).getByLabelText('资源名称'), { target: { value: '订单导出' } })
    fireEvent.change(within(dialog).getByLabelText('资源编码'), { target: { value: 'payment:order:export' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByTestId('auth-resources-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(await within(dialog).findByText(/\/auth\/resource\/save 保存失败：resource save down/)).toBeInTheDocument()
    expect(within(dialog).getByText(/route=\/admin\/auth\/resources; resourceId=新增; resourceName=订单导出; resourceCode=payment:order:export/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('payment:order:export')).toBeInTheDocument()
  })

  it('shows endpoint error when delete fails and keeps resource row plus confirm context', async () => {
    vi.mocked(authApi.resourceDelete).mockRejectedValue(new Error('resource delete down'))

    renderWithProviders(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>,
    )

    await screen.findByText('用户管理')
    fireEvent.click(screen.getByText('删除'))
    expect(screen.getByText(/endpoint=\/auth\/resource\/delete; route=\/admin\/auth\/resources; resourceId=1; resourceName=用户管理; resourceCode=auth:user:list/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/\/auth\/resource\/delete 删除失败：resource delete down/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('auth-resources-action-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getAllByText(/resourceId=1; resourceName=用户管理; resourceCode=auth:user:list/).length).toBeGreaterThan(0)
    expect(screen.getByText('用户管理')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '确认操作' })).toBeInTheDocument()
  })
})
