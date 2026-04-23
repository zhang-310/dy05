import type { ChipProps } from '@mui/material'

export type StatusChipDef = { label: string; color: ChipProps['color'] }

/** 通用启用/停用状态 */
export const COMMON_STATUS: Record<string, StatusChipDef> = {
  active:    { label: '启用', color: 'success' },
  inactive:  { label: '停用', color: 'default' },
  pending:   { label: '待处理', color: 'warning' },
  error:     { label: '异常', color: 'error' },
  draft:     { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'success' },
  archived:  { label: '已归档', color: 'default' },
}

/** 直播场次状态 */
export const LIVE_STATUS: Record<string, StatusChipDef> = {
  prepare:   { label: '准备中', color: 'default' },
  live:      { label: '直播中', color: 'error' },
  ended:     { label: '已结束', color: 'default' },
  cancelled: { label: '已取消', color: 'warning' },
}

/** 审批状态 */
export const APPROVAL_STATUS: Record<string, StatusChipDef> = {
  pending:  { label: '待审批', color: 'warning' },
  approved: { label: '已通过', color: 'success' },
  rejected: { label: '已拒绝', color: 'error' },
  revising: { label: '修改中', color: 'info' },
}

/** 任务状态 */
export const TASK_STATUS: Record<string, StatusChipDef> = {
  pending:    { label: '等待中', color: 'default' },
  processing: { label: '处理中', color: 'info' },
  completed:  { label: '已完成', color: 'success' },
  failed:     { label: '失败', color: 'error' },
  cancelled:  { label: '已取消', color: 'warning' },
}
