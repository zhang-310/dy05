import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeArray,
  normalizePage as normalizeResponsePage,
} from '@/utils/response-normalize'

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
export interface LoginLogQuery { page?: number; rows?: number; userId?: number; username?: string; status?: number }

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
export interface OrgMember {
  id?: number
  orgId?: number
  userId: number
  username: string
  nickname?: string
  role?: string
  roleInOrg?: string
  status?: number
  joinTime?: string
  joinedAt?: string
  invitedAt?: string
}
export interface AuthUserBanParams {
  userId: number
  ban: boolean
  reason?: string
}

export interface AuthResourceQuery {
  page?: number
  rows?: number
  resourceType?: string
  module?: string
  resourceCode?: string
  resourceName?: string
  parentId?: number
}

export interface MenuItem { id: number; parentId: number; name: string; path: string; icon: string; sortOrder: number; children?: MenuItem[] }

function readPage<T>(data: unknown, page = 0, rows = 20): PageResult<T> {
  return normalizeResponsePage<T, T>(data, item => item, page, rows)
}

function normalizeNumberArray(raw: unknown): number[] {
  return normalizeArray<unknown>(raw)
    .map(value => Number(value))
    .filter(Number.isFinite)
}

function normalizeMenuItem(raw: unknown): MenuItem {
  const item = isRecord(raw) ? raw : {}
  const children = normalizeArray<unknown>(item.children).map(normalizeMenuItem)
  const resourceName = String(item.resourceName ?? item.name ?? '')
  const resourceCode = String(item.resourceCode ?? '')
  return {
    id: Number(item.id ?? 0),
    parentId: Number(item.parentId ?? 0),
    name: String(item.name ?? resourceName),
    path: String(item.path ?? resourceCode),
    icon: String(item.icon ?? ''),
    sortOrder: Number(item.sortOrder ?? 0),
    children,
  }
}

function normalizeMenuArray(raw: unknown): MenuItem[] {
  return normalizeArray<unknown>(raw).map(normalizeMenuItem)
}

function mapLoginLogQuery(params: LoginLogQuery): Record<string, unknown> {
  return {
    userId: params.userId,
    username: params.username,
    status: params.status,
    page: params.page,
    size: params.rows ?? 20,
  }
}

export const authApi = {
  login: (params: LoginParams) => request.post<LoginResult>('/auth/login', params),
  logout: () => request.post('/auth/logout'),
  captcha: () => request.post<{ captchaKey: string; captchaImage: string }>('/auth/captcha', {}),

  // User
  list: (params: AuthUserQuery) =>
    request.post<unknown>('/auth/user/search', params)
      .then((data) => readPage<AuthUser>(data, params.page ?? 0, params.rows ?? 20)),
  get: (id: number) => request.post<AuthUser>('/auth/user/get', { id }),
  save: (params: AuthUserSave) => request.post<void>('/auth/user/save', params),
  delete: (id: number) => request.post<void>('/auth/user/delete', { id }),
  ban: (params: AuthUserBanParams) => request.post<void>('/auth/user/ban', params),
  loginLogs: (params: LoginLogQuery) =>
    request.post<unknown>('/auth/user/login-logs', mapLoginLogQuery(params))
      .then((data) => readPage(data, params.page ?? 0, params.rows ?? 20)),
  onlineUsers: () => request.post<unknown>('/auth/user/online', {}).then(normalizeArray<AuthUser>),

  // Profile
  profile: () => request.post<AuthProfile>('/auth/profile', {}),
  profileUpdate: (params: ProfileUpdateParams) => request.post<void>('/auth/profile/update', params),
  changePassword: (params: ChangePasswordParams) => request.post<void>('/auth/profile/change-password', params),

  // Role
  roleList: (params: AuthRoleQuery) =>
    request.post<unknown>('/auth/role/search', params)
      .then((data) => readPage<AuthRole>(data, params.page ?? 0, params.rows ?? 20)),
  roleAll: () => request.post<unknown>('/auth/role/list', {}).then(normalizeArray<AuthRole>),
  roleGet: (id: number) => request.post<AuthRole>('/auth/role/get', { id }),
  roleSave: (params: Partial<AuthRoleSave>) => request.post<void>('/auth/role/save', params),
  roleDelete: (id: number) => request.post<void>('/auth/role/delete', { id }),
  roleResources: (roleId: number) => request.post<unknown>('/auth/role/resources', { roleId }).then(normalizeNumberArray),
  roleResourcesSave: (roleId: number, resourceIds: number[]) => request.post<void>('/auth/role/resources/save', { roleId, resourceIds }),

  // Resource
  resourceList: (params: AuthResourceQuery = {}) =>
    request.post<unknown>('/auth/resource/list', params)
      .then((data) => readPage(data, params.page ?? 0, params.rows ?? 50)),
  resourceTree: () => request.post<unknown>('/auth/resource/tree', {}).then(normalizeArray<AuthResource>),
  resourceTreeFull: () => request.post<unknown>('/auth/resource/tree-full', {}).then(normalizeArray<AuthResource>),
  resourceGet: (id: number) => request.post<AuthResource>('/auth/resource/get', { id }),
  resourceSave: (params: Partial<AuthResourceSave>) => request.post<void>('/auth/resource/save', params),
  resourceDelete: (id: number) => request.post<void>('/auth/resource/delete', { id }),

  // Menu
  menuSearch: () => request.post<unknown>('/auth/menu/search', {}).then(normalizeMenuArray),

  // Organization
  orgMy: () => request.post<Organization>('/organization/my', {}),
  orgCreate: (params: Partial<Organization>) => request.post<void>('/organization/create', params),
  orgUpdate: (params: Partial<Organization>) => request.post<void>('/organization/update', params),
  orgMembers: () => request.post<unknown>('/organization/members', {}).then(normalizeArray<OrgMember>),
  orgInvite: (params: { userId: number }) => request.post<void>('/organization/invite', params),
  orgRemove: (userId: number) => request.post<void>('/organization/remove', { userId }),
  orgInvitations: () => request.post<unknown>('/organization/invitations', {}).then(normalizeArray<Record<string, unknown>>),
  orgSearchTalents: (keyword: string) => request.post<unknown>('/organization/search-talents', { keyword }).then(normalizeArray<Record<string, unknown>>),
}
