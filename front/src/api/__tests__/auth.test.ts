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

  it('roleResources posts backend roleId payload', async () => {
    mockPost.mockResolvedValue([11, 12])

    const result = await authApi.roleResources(3)

    expect(mockPost).toHaveBeenCalledWith('/auth/role/resources', { roleId: 3 })
    expect(result).toEqual([11, 12])
  })

  it('ban posts AuthUserBanVO payload', async () => {
    mockPost.mockResolvedValue(undefined)

    await authApi.ban({ userId: 9, ban: true, reason: '违规登录' })

    expect(mockPost).toHaveBeenCalledWith('/auth/user/ban', {
      userId: 9,
      ban: true,
      reason: '违规登录',
    })
  })

  it('resourceTree posts empty payload', async () => {
    mockPost.mockResolvedValue([])
    await authApi.resourceTree()
    expect(mockPost).toHaveBeenCalledWith('/auth/resource/tree', {})
  })

  it('orgInvite posts member invitation payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await authApi.orgInvite({ userId: 9 })
    expect(mockPost).toHaveBeenCalledWith('/organization/invite', {
      userId: 9,
    })
  })

  it('loginLogs maps frontend rows to backend size and normalizes page result', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [{ id: 1, username: 'admin', status: 1 }],
      pageNum: 0,
      pageSize: 20,
    })

    const result = await authApi.loginLogs({ page: 0, rows: 20, userId: 1, username: 'admin', status: 1 })

    expect(mockPost).toHaveBeenCalledWith('/auth/user/login-logs', {
      userId: 1,
      username: 'admin',
      status: 1,
      page: 0,
      size: 20,
    })
    expect(result.total).toBe(1)
    expect(result.list[0].username).toBe('admin')
  })

  it('normalizes wrapped auth pages and resource arrays', async () => {
    mockPost
      .mockResolvedValueOnce({ records: [{ id: 1, username: 'alice', status: 1 }], totalElements: 1 })
      .mockResolvedValueOnce({ data: { items: [{ id: 2, roleName: '运营', roleCode: 'operator', status: 1 }], totalCount: 1 } })
      .mockResolvedValueOnce({ rows: [{ id: 3, resourceName: '资源', resourceCode: 'auth:resource', resourceType: 'api', parentId: 0, sortOrder: 1 }], totalRecords: 1 })
      .mockResolvedValueOnce({ data: { list: [3, '4'] } })

    const users = await authApi.list({ page: 0, rows: 20 })
    const roles = await authApi.roleList({ page: 0, rows: 20 })
    const resources = await authApi.resourceList({ page: 0, rows: 50 })
    const roleResources = await authApi.roleResources(2)

    expect(users).toMatchObject({ total: 1, list: [{ username: 'alice' }] })
    expect(roles.list[0].roleCode).toBe('operator')
    expect(resources.list[0].resourceCode).toBe('auth:resource')
    expect(roleResources).toEqual([3, 4])
    expect(mockPost).toHaveBeenNthCalledWith(4, '/auth/role/resources', { roleId: 2 })
  })

  it('resourceList posts server-side search params and normalizes legacy arrays', async () => {
    mockPost.mockResolvedValue([{ id: 1, resourceName: '用户管理', resourceCode: 'auth:user:list', resourceType: 'menu', parentId: 0, sortOrder: 1 }])

    const result = await authApi.resourceList({ page: 0, rows: 50, resourceType: 'menu', module: 'auth' })

    expect(mockPost).toHaveBeenCalledWith('/auth/resource/list', {
      page: 0,
      rows: 50,
      resourceType: 'menu',
      module: 'auth',
    })
    expect(result.total).toBe(1)
    expect(result.list[0].resourceCode).toBe('auth:user:list')
  })
})
