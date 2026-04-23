import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
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
    vi.mocked(authApi.resourceList).mockResolvedValue([
      {
        id: 1,
        parentId: 0,
        resourceName: '用户管理',
        resourceCode: 'auth:user:list',
        resourceType: 'menu',
        path: '/admin/auth/users',
        icon: 'people',
        sortOrder: 1,
        status: 1,
      },
    ] as never)
  })

  it('loads resources and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ResourcesPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(authApi.resourceList).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('用户管理')).toBeInTheDocument()
      expect(screen.getByText('auth:user:list')).toBeInTheDocument()
    })
  })
})
