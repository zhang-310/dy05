import { describe, it, expect, vi } from 'vitest'
import { authApi } from '@/api/auth'
import type { AuthUserVO, AuthRoleVO, LoginLogVO } from '@/types/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    captcha: vi.fn(),
    list: vi.fn(),
    get: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    ban: vi.fn(),
    loginLogs: vi.fn(),
    onlineUsers: vi.fn(),
    profile: vi.fn(),
    profileUpdate: vi.fn(),
    changePassword: vi.fn(),
    roleList: vi.fn(),
    roleAll: vi.fn(),
    roleGet: vi.fn(),
    roleSave: vi.fn(),
    roleDelete: vi.fn(),
    roleResources: vi.fn(),
    roleResourcesSave: vi.fn(),
    resourceList: vi.fn(),
    resourceTree: vi.fn(),
    resourceTreeFull: vi.fn(),
    resourceGet: vi.fn(),
    resourceSave: vi.fn(),
    resourceDelete: vi.fn(),
    menuSearch: vi.fn(),
    orgMy: vi.fn(),
    orgCreate: vi.fn(),
    orgUpdate: vi.fn(),
    orgMembers: vi.fn(),
    orgInvite: vi.fn(),
    orgRemove: vi.fn(),
    orgInvitations: vi.fn(),
    orgSearchTalents: vi.fn(),
  },
}))

interface LoginResponse {
  token: string
  userId: number
  username: string
  nickname: string
  avatarUrl: string
  roleCode: string
  organizationId: number
  organizationName: string
}

interface CaptchaResponse {
  captchaKey: string
  captchaImage: string
}

interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

describe('Auth API 集成测试', () => {
  it('应该正确调用登录 API', async () => {
    const mockResponse: LoginResponse = {
      token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
      userId: 1,
      username: 'testuser',
      nickname: '测试用户',
      avatarUrl: '/avatar.jpg',
      roleCode: 'admin',
      organizationId: 1,
      organizationName: '测试组织',
    }
    vi.mocked(authApi.login).mockResolvedValue(mockResponse)

    const result = await authApi.login({
      username: 'testuser',
      password: 'password123',
    })

    expect(result.token).toBeTruthy()
    expect(result.userId).toBe(1)
    expect(result.username).toBe('testuser')
    expect(authApi.login).toHaveBeenCalledWith({
      username: 'testuser',
      password: 'password123',
    })
  })

  it('应该正确调用登出 API', async () => {
    vi.mocked(authApi.logout).mockResolvedValue(undefined)

    await authApi.logout()

    expect(authApi.logout).toHaveBeenCalled()
  })

  it('应该正确调用验证码 API', async () => {
    const mockResponse: CaptchaResponse = {
      captchaKey: 'abc123',
      captchaImage: 'data:image/png;base64,...',
    }
    vi.mocked(authApi.captcha).mockResolvedValue(mockResponse)

    const result = await authApi.captcha()

    expect(result.captchaKey).toBe('abc123')
    expect(result.captchaImage).toContain('data:image/png')
    expect(authApi.captcha).toHaveBeenCalled()
  })

  it('应该正确调用用户列表 API', async () => {
    const mockResponse: PageResult<AuthUserVO> = {
      list: [
        {
          id: 1,
          username: 'testuser',
          nickname: '测试用户',
          roleCode: 'admin',
          status: 1,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    }
    vi.mocked(authApi.list).mockResolvedValue(mockResponse)

    const result = await authApi.list({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].username).toBe('testuser')
    expect(authApi.list).toHaveBeenCalled()
  })

  it('应该支持用户名筛选', async () => {
    const mockResponse: PageResult<AuthUserVO> = {
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    }
    vi.mocked(authApi.list).mockResolvedValue(mockResponse)

    await authApi.list({ page: 0, rows: 20, username: 'test' })

    expect(authApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        username: 'test',
      })
    )
  })

  it('应该支持状态筛选', async () => {
    const mockResponse: PageResult<AuthUserVO> = {
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    }
    vi.mocked(authApi.list).mockResolvedValue(mockResponse)

    await authApi.list({ page: 0, rows: 20, status: 1 })

    expect(authApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        status: 1,
      })
    )
  })

  it('应该正确调用用户详情 API', async () => {
    const mockResponse: AuthUserVO = {
      id: 1,
      username: 'testuser',
      nickname: '测试用户',
      email: 'test@example.com',
      roleCode: 'admin',
      status: 1,
      createTime: '2026-05-10T10:00:00',
    }
    vi.mocked(authApi.get).mockResolvedValue(mockResponse)

    const result = await authApi.get(1)

    expect(result.username).toBe('testuser')
    expect(authApi.get).toHaveBeenCalledWith(1)
  })

  it('应该正确调用用户保存 API', async () => {
    vi.mocked(authApi.save).mockResolvedValue(undefined)

    await authApi.save({
      username: 'newuser',
      nickname: '新用户',
      roleCode: 'user',
      password: 'password123',
    })

    expect(authApi.save).toHaveBeenCalledWith(
      expect.objectContaining({
        username: 'newuser',
        nickname: '新用户',
      })
    )
  })

  it('应该正确调用用户删除 API', async () => {
    vi.mocked(authApi.delete).mockResolvedValue(undefined)

    await authApi.delete(1)

    expect(authApi.delete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用用户封禁 API', async () => {
    vi.mocked(authApi.ban).mockResolvedValue(undefined)

    await authApi.ban({ userId: 1, ban: true, reason: '违规登录' })

    expect(authApi.ban).toHaveBeenCalledWith({ userId: 1, ban: true, reason: '违规登录' })
  })

  it('应该正确调用登录日志 API', async () => {
    const mockResponse: PageResult<LoginLogVO> = {
      list: [
        {
          id: 1,
          userId: 1,
          username: 'testuser',
          ip: '192.168.1.1',
          userAgent: 'Mozilla/5.0',
          status: 1,
          loginTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    }
    vi.mocked(authApi.loginLogs).mockResolvedValue(mockResponse)

    const result = await authApi.loginLogs({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].username).toBe('testuser')
    expect(authApi.loginLogs).toHaveBeenCalled()
  })

  it('应该正确调用在线用户 API', async () => {
    const mockResponse: AuthUserVO[] = [
      {
        id: 1,
        username: 'testuser',
        nickname: '测试用户',
        roleCode: 'admin',
        status: 1,
        lastLoginAt: '2026-05-10T10:00:00',
      },
    ]
    vi.mocked(authApi.onlineUsers).mockResolvedValue(mockResponse)

    const result = await authApi.onlineUsers()

    expect(result).toHaveLength(1)
    expect(result[0].username).toBe('testuser')
    expect(authApi.onlineUsers).toHaveBeenCalled()
  })

  it('应该正确调用个人资料 API', async () => {
    interface ProfileResponse {
      userId: number
      username: string
      nickname: string
      email: string
      phone: string
      avatarUrl: string
    }

    const mockResponse: ProfileResponse = {
      userId: 1,
      username: 'testuser',
      nickname: '测试用户',
      email: 'test@example.com',
      phone: '13800138000',
      avatarUrl: '/avatar.jpg',
    }
    vi.mocked(authApi.profile).mockResolvedValue(mockResponse)

    const result = await authApi.profile()

    expect(result.username).toBe('testuser')
    expect(result.email).toBe('test@example.com')
    expect(authApi.profile).toHaveBeenCalled()
  })

  it('应该正确调用更新个人资料 API', async () => {
    vi.mocked(authApi.profileUpdate).mockResolvedValue(undefined)

    await authApi.profileUpdate({
      nickname: '新昵称',
      email: 'newemail@example.com',
    })

    expect(authApi.profileUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        nickname: '新昵称',
        email: 'newemail@example.com',
      })
    )
  })

  it('应该正确调用修改密码 API', async () => {
    vi.mocked(authApi.changePassword).mockResolvedValue(undefined)

    await authApi.changePassword({
      oldPassword: 'oldpass123',
      newPassword: 'newpass456',
    })

    expect(authApi.changePassword).toHaveBeenCalledWith({
      oldPassword: 'oldpass123',
      newPassword: 'newpass456',
    })
  })

  it('应该正确调用角色列表 API', async () => {
    const mockResponse: PageResult<AuthRoleVO> = {
      list: [
        {
          id: 1,
          roleName: '管理员',
          roleCode: 'admin',
          sortOrder: 1,
          status: 1,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    }
    vi.mocked(authApi.roleList).mockResolvedValue(mockResponse)

    const result = await authApi.roleList({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].roleName).toBe('管理员')
    expect(authApi.roleList).toHaveBeenCalled()
  })

  it('应该正确调用所有角色 API', async () => {
    const mockResponse: AuthRoleVO[] = [
      {
        id: 1,
        roleName: '管理员',
        roleCode: 'admin',
        sortOrder: 1,
        status: 1,
      },
      {
        id: 2,
        roleName: '普通用户',
        roleCode: 'user',
        sortOrder: 2,
        status: 1,
      },
    ]
    vi.mocked(authApi.roleAll).mockResolvedValue(mockResponse)

    const result = await authApi.roleAll()

    expect(result).toHaveLength(2)
    expect(result[0].roleCode).toBe('admin')
    expect(authApi.roleAll).toHaveBeenCalled()
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('用户名或密码错误'))

    await expect(
      authApi.login({ username: 'wrong', password: 'wrong' })
    ).rejects.toThrow('用户名或密码错误')
  })
})
