import request from '@/utils/request'
import { isRecord, normalizeArray, normalizeRecord } from '@/utils/response-normalize'

export type RoleHomeKind = 'admin' | 'org' | 'talent' | 'user'

export interface RoleHomeBoundary {
  role?: string
  roleCode?: string
  ownerId?: number
  organizationId?: number | null
  unrestricted?: boolean
  scopeType?: string
  visibleOwnerIds?: number[] | null
  requiresOwnerFilter?: boolean
}

export interface RoleHomeSection {
  key: string
  title: string
  items: Record<string, unknown>[]
}

export interface RoleHomeBusinessChain {
  key: string
  title: string
  path: string
  requiredKnowledge: string
  guardRequired: boolean
  realTime?: boolean
  status?: string
  metrics?: Record<string, unknown>
}

export interface RoleHomeKnowledgeGuard {
  requiredKbCodes: string[]
  officialReferenceRequired: boolean
  blockWithoutOfficialReference: boolean
  appliesTo: string[]
  status?: string
  realTime?: boolean
  metrics?: Record<string, unknown>
  message?: string
}

export interface RoleHomeLearningLoop {
  key: string
  title: string
  targetKb: string
  source: string
  status?: string
  realTime?: boolean
  metrics?: Record<string, unknown>
}

export interface RoleHomeData {
  role: RoleHomeKind
  userId?: number
  roleCode?: string
  organizationId?: number | null
  visibleOwnerIds?: number[] | null
  boundary?: RoleHomeBoundary
  metrics: Record<string, unknown>
  sections: RoleHomeSection[]
  businessChains: RoleHomeBusinessChain[]
  knowledgeGuard?: RoleHomeKnowledgeGuard
  learningLoops: RoleHomeLearningLoop[]
  readyEndpoints: string[]
  generatedAt?: string
}

const endpointByRole: Record<RoleHomeKind, string> = {
  admin: '/admin/home',
  org: '/org/home',
  talent: '/talent/home',
  user: '/user/home',
}

function toNumberArray(value: unknown): number[] | null | undefined {
  if (value == null) return value as null | undefined
  if (!Array.isArray(value)) return undefined
  return value.map((item) => Number(item)).filter(Number.isFinite)
}

function normalizeBoundary(value: unknown): RoleHomeBoundary {
  const record = normalizeRecord(value)
  return {
    role: typeof record.role === 'string' ? record.role : undefined,
    roleCode: typeof record.roleCode === 'string' ? record.roleCode : undefined,
    ownerId: typeof record.ownerId === 'number' ? record.ownerId : undefined,
    organizationId: typeof record.organizationId === 'number' ? record.organizationId : null,
    unrestricted: Boolean(record.unrestricted),
    scopeType: typeof record.scopeType === 'string' ? record.scopeType : undefined,
    visibleOwnerIds: toNumberArray(record.visibleOwnerIds),
    requiresOwnerFilter: Boolean(record.requiresOwnerFilter),
  }
}

function normalizeRoleHome(raw: unknown, role: RoleHomeKind): RoleHomeData {
  const record = normalizeRecord(raw)
  const guard = normalizeRecord(record.knowledgeGuard)
  return {
    role,
    userId: typeof record.userId === 'number' ? record.userId : undefined,
    roleCode: typeof record.roleCode === 'string' ? record.roleCode : undefined,
    organizationId: typeof record.organizationId === 'number' ? record.organizationId : null,
    visibleOwnerIds: toNumberArray(record.visibleOwnerIds),
    boundary: normalizeBoundary(record.boundary),
    metrics: normalizeRecord(record.metrics),
    sections: normalizeArray(record.sections).filter(isRecord).map((section) => ({
      key: String(section.key ?? ''),
      title: String(section.title ?? ''),
      items: normalizeArray(section.items).filter(isRecord),
    })),
    businessChains: normalizeArray(record.businessChains).filter(isRecord).map((chain) => ({
      key: String(chain.key ?? ''),
      title: String(chain.title ?? ''),
      path: String(chain.path ?? ''),
      requiredKnowledge: String(chain.requiredKnowledge ?? ''),
      guardRequired: Boolean(chain.guardRequired),
      realTime: Boolean(chain.realTime),
      status: typeof chain.status === 'string' ? chain.status : undefined,
      metrics: normalizeRecord(chain.metrics),
    })),
    knowledgeGuard: {
      requiredKbCodes: normalizeArray(guard.requiredKbCodes).map(String),
      officialReferenceRequired: Boolean(guard.officialReferenceRequired),
      blockWithoutOfficialReference: Boolean(guard.blockWithoutOfficialReference),
      appliesTo: normalizeArray(guard.appliesTo).map(String),
      status: typeof guard.status === 'string' ? guard.status : undefined,
      realTime: Boolean(guard.realTime),
      metrics: normalizeRecord(guard.metrics),
      message: typeof guard.message === 'string' ? guard.message : undefined,
    },
    learningLoops: normalizeArray(record.learningLoops).filter(isRecord).map((loop) => ({
      key: String(loop.key ?? ''),
      title: String(loop.title ?? ''),
      targetKb: String(loop.targetKb ?? ''),
      source: String(loop.source ?? ''),
      status: typeof loop.status === 'string' ? loop.status : undefined,
      realTime: Boolean(loop.realTime),
      metrics: normalizeRecord(loop.metrics),
    })),
    readyEndpoints: normalizeArray(record.readyEndpoints).map(String),
    generatedAt: typeof record.generatedAt === 'string' ? record.generatedAt : undefined,
  }
}

export const roleHomeApi = {
  getHome: (role: RoleHomeKind) =>
    request.post<unknown>(endpointByRole[role], {}).then((data) => normalizeRoleHome(data, role)),
}
