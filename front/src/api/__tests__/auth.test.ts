/**
 * auth API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { authApi } from '../auth'

vi.mock('@/utils/request', () => ({
  default: { post: vi.fn() },
}))

describe('auth API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('login calls post with credentials', async () => {
    mockPost.mockResolvedValue({ token: 'xxx', userId: 1, username: 'u', nickname: 'User', avatarUrl: '', roleCode: 'user', organizationId: 1, organizationName: 'Org' })
    await authApi.login({ username: 'user', password: 'pass' })
    expect(mockPost).toHaveBeenCalledWith('/auth/login', { username: 'user', password: 'pass' })
  })

  it('profile calls post', async () => {
    mockPost.mockResolvedValue({ userId: 1, username: 'test', nickname: 'Test', email: 'test@example.com', phone: '', avatarUrl: '' })
    await authApi.profile()
    expect(mockPost).toHaveBeenCalledWith('/auth/profile', {})
  })

  it('logout calls post', async () => {
    mockPost.mockResolvedValue(undefined)
    await authApi.logout()
    expect(mockPost).toHaveBeenCalledWith('/auth/logout')
  })

  it('roleResourcesSave posts role-resource mapping payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await authApi.roleResourcesSave(3, [11, 12, 13])
    expect(mockPost).toHaveBeenCalledWith('/auth/role/resources/save', {
      roleId: 3,
      resourceIds: [11, 12, 13],
    })
  })

  it('resourceTree posts empty payload', async () => {
    mockPost.mockResolvedValue([])
    await authApi.resourceTree()
    expect(mockPost).toHaveBeenCalledWith('/auth/resource/tree', {})
  })

  it('orgInvite posts member invitation payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await authApi.orgInvite({ username: 'new-user', role: 'talent' })
    expect(mockPost).toHaveBeenCalledWith('/organization/invite', {
      username: 'new-user',
      role: 'talent',
    })
  })
})
