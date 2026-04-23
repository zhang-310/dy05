export const TASK_STATUS_MAP: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  0: { label: '待执行', color: 'default' },
  1: { label: '执行中', color: 'warning' },
  2: { label: '已完成', color: 'success' },
  3: { label: '失败', color: 'error' },
  4: { label: '已取消', color: 'warning' },
}

/** 进化页「知识库范围」：下拉 value 为 id 字符串；非法时返回 undefined，避免 NaN 导致后端走默认 KB 与列表筛选不一致 */
export function parseScopeKbId(scopeKbId: string): number | undefined {
  if (scopeKbId == null || scopeKbId === '') return undefined
  const n = Number(scopeKbId)
  return Number.isFinite(n) ? n : undefined
}

export const AGENT_TYPES = [
  { code: 'gap', label: '知识缺口Agent' },
  { code: 'timeliness', label: '时效性Agent' },
  { code: 'quality', label: '质量评分Agent' },
  { code: 'classify', label: '自动分类Agent' },
  { code: 'share', label: '跨域共享Agent' },
  { code: 'deepen', label: '深度进化Agent' },
] as const

export const REVIEW_STATUS_LABEL: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  REVISED: '已修订',
}
