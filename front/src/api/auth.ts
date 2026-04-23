import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface LoginParams { username: string; password: string }
export interface LoginResult { token: string; userId: number; username: string; nickname: string; avatarUrl: string; roleCode: string; organizationId: number; organizationName: string }
export interface AuthUser {
  id: number
  username: string
  mobile?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  roleCode?: string
  status: number
  bannedAt?: string
  bannedReason?: string
  lastLoginAt?: string
  createTime?: string
}
export interface AuthUserQuery { page?: number; rows?: number; username?: string; status?: number }
export interface AuthUserSave {
  id?: number
  username: string
  nickname: string
  mobile?: string
  email?: string
  avatarUrl?: string
  roleCode?: string
  password?: string
  status?: number
}

export interface AuthRole {
  id: number
  roleName: string
  roleCode: string
  description?: string
  sortOrder?: number
  status: number
  createTime?: string
  updateTime?: string
}
export interface AuthRoleQuery { page?: number; rows?: number; roleName?: string; status?: number }
export interface AuthRoleSave {
  id?: number
  roleName: string
  roleCode: string
  description?: string
  sortOrder?: number
  status?: number
}

export interface LoginLog {
  id: number
  userId?: number
  username: string
  loginType?: string
  deviceType?: string
  ip: string
  userAgent: string
  status: number
  failReason?: string
  loginTime?: string
}
export interface LoginLogQuery { page?: number; rows?: number; username?: string; status?: number }

export interface AuthProfile { userId: number; username: string; nickname: string; email: string; phone: string; avatarUrl: string }
export interface ProfileUpdateParams { nickname?: string; email?: string; phone?: string; avatarUrl?: string }
export interface ChangePasswordParams { oldPassword: string; newPassword: string }

// AuthResourceVO: id, resourceType, resourceCode, requestMethod, module, resourceName, parentId, sortOrder, createTime, updateTime
export interface AuthResource {
  id: number
  resourceType: string
  resourceCode: string
  requestMethod?: string
  module?: string
  resourceName: string
  parentId: number
  sortOrder: number
  createTime?: string
  updateTime?: string
}

// AuthResourceSaveVO: id, resourceType, resourceCode, requestMethod, module, resourceName, parentId, sortOrder
export interface AuthResourceSave {
  id?: number
  resourceType: string
  resourceCode: string
  requestMethod?: string
  module?: string
  resourceName: string
  parentId?: number
  sortOrder?: number
}

export interface Organization { id: number; orgName: string; orgCode: string; description: string; status: number; createTime: string }
export interface OrgMember { userId: number; username: string; nickname: string; role: string; joinTime: string }

export interface MenuItem { id: number; parentId: number; name: string; path: string; icon: string; sortOrder: number; children?: MenuItem[] }

export const authApi = {
  login: (params: LoginParams) => request.post<LoginResult>('/auth/login', params),
  logout: () => request.post('/auth/logout'),
  captcha: () => request.post<{ captchaKey: string; captchaImage: string }>('/auth/captcha', {}),

  // User
  list: (params: AuthUserQuery) => request.post<PageResult<AuthUser>>('/auth/user/search', params),
  get: (id: number) => request.post<AuthUser>('/auth/user/get', { id }),
  save: (params: AuthUserSave) => request.post<void>('/auth/user/save', params),
  delete: (id: number) => request.post<void>('/auth/user/delete', { id }),
  ban: (id: number, status: number) => request.post<void>('/auth/user/ban', { id, status }),
  loginLogs: (params: LoginLogQuery) => request.post<PageResult<LoginLog>>('/auth/user/login-logs', params),
  onlineUsers: () => request.post<AuthUser[]>('/auth/user/online', {}),

  // Profile
  profile: () => request.post<AuthProfile>('/auth/profile', {}),
  profileUpdate: (params: ProfileUpdateParams) => request.post<void>('/auth/profile/update', params),
  changePassword: (params: ChangePasswordParams) => request.post<void>('/auth/profile/change-password', params),

  // Role
  roleList: (params: AuthRoleQuery) => request.post<PageResult<AuthRole>>('/auth/role/search', params),
  roleAll: () => request.post<AuthRole[]>('/auth/role/list', {}),
  roleGet: (id: number) => request.post<AuthRole>('/auth/role/get', { id }),
  roleSave: (params: Partial<AuthRoleSave>) => request.post<void>('/auth/role/save', params),
  roleDelete: (id: number) => request.post<void>('/auth/role/delete', { id }),
  roleResources: (id: number) => request.post<number[]>('/auth/role/resources', { id }),
  roleResourcesSave: (roleId: number, resourceIds: number[]) => request.post<void>('/auth/role/resources/save', { roleId, resourceIds }),

  // Resource
  resourceList: () => request.post<AuthResource[]>('/auth/resource/list', {}),
  resourceTree: () => request.post<AuthResource[]>('/auth/resource/tree', {}),
  resourceTreeFull: () => request.post<AuthResource[]>('/auth/resource/tree-full', {}),
  resourceGet: (id: number) => request.post<AuthResource>('/auth/resource/get', { id }),
  resourceSave: (params: Partial<AuthResourceSave>) => request.post<void>('/auth/resource/save', params),
  resourceDelete: (id: number) => request.post<void>('/auth/resource/delete', { id }),

  // Menu
  menuSearch: () => request.post<MenuItem[]>('/auth/menu/search', {}),

  // Organization
  orgMy: () => request.post<Organization>('/organization/my', {}),
  orgCreate: (params: Partial<Organization>) => request.post<void>('/organization/create', params),
  orgUpdate: (params: Partial<Organization>) => request.post<void>('/organization/update', params),
  orgMembers: () => request.post<OrgMember[]>('/organization/members', {}),
  orgInvite: (params: { username: string; role?: string }) => request.post<void>('/organization/invite', params),
  orgRemove: (userId: number) => request.post<void>('/organization/remove', { userId }),
  orgInvitations: () => request.post<Record<string, unknown>[]>('/organization/invitations', {}),
}
