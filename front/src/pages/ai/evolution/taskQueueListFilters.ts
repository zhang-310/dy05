import { TASK_STATUS_MAP } from '@/pages/ai/evolution/engineConstants'

/** 与 TaskQueueTab 一致的任务状态筛选项 */
export const EVOLVE_TASK_STATUS_FILTER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '全部状态' },
  { value: '0', label: '待执行' },
  { value: '1', label: '执行中' },
  { value: '2', label: '已完成' },
  { value: '3', label: '失败/阻塞' },
  { value: '4', label: '已取消' },
]

/** 将后端 status（数字或字符串）映射为 TASK_STATUS_MAP 使用的 UI 类别 0–4 */
export function statusToUi(s: unknown): number {
  if (typeof s === 'number' && s in TASK_STATUS_MAP) return s
  const str = String(s ?? '').toLowerCase()
  if (str === 'pending' || str === 'queued') return 0
  if (str === 'running' || str === 'gathering' || str === 'generating' || str === 'scoring') return 1
  if (str === 'completed' || str === 'success') return 2
  if (str === 'failed' || str === 'error') return 3
  if (str === 'canceled' || str === 'cancelled') return 4
  return 0
}

/** 当前页任务是否存在「执行中」 */
export function pageHasRunningTask(
  list: Array<{ status?: unknown }> | undefined,
): boolean {
  if (!list?.length) return false
  return list.some(row => statusToUi(row.status) === 1)
}
