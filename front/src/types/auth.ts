import type { BasicQuery } from './common'

// ─── 用户 ───────────────────────────────────────
export interface AuthUserSearchVO extends BasicQuery {
  keyword?: string
  username?: string
  nickname?: string
  mobile?: string
  roleCode?: string
  status?: number
}

export interface AuthUserSaveVO {
  id?: number
  username: string
  nickname?: string
  mobile?: string
  email?: string
  avatarUrl?: string
  roleCode: string
  password?: string
}

export interface AuthUserVO {
  id: number
  username: string
  mobile?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  roleCode: string
  status: number
  bannedAt?: string
  bannedReason?: string
  lastLoginAt?: string
  createTime?: string
}

export interface AuthUserBanVO {
  userId: number
  ban: boolean
  reason?: string
}

// ─── 角色 ───────────────────────────────────────
export interface AuthRoleSearchVO extends BasicQuery {
  roleCode?: string
  roleName?: string
  status?: number
}

export interface AuthRoleSaveVO {
  id?: number
  roleCode: string
  roleName: string
  sortOrder?: number
  status?: number
}

export interface AuthRoleVO {
  id: number
  roleCode: string
  roleName: string
  sortOrder: number
  status: number
  createTime?: string
  updateTime?: string
}

// ─── 登录日志 ─────────────────────────────────────
export interface LoginLogQueryVO {
  userId?: number
  page?: number
  size?: number
}

export interface LoginLogVO {
  id: number
  userId?: number
  username?: string
  loginType?: string
  deviceType?: string
  ip?: string
  userAgent?: string
  status?: number
  failReason?: string
  loginTime?: string
}
