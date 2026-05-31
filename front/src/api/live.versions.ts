import request from '@/utils/request'
import type { PageResult } from '@/types/common'

/**
 * 查询话术版本列表（分页）
 * API: POST /api/v1/live/script-version/search
 *
 * @param data - 查询参数，包含 scriptId, page, rows 等
 * @returns 版本列表及分页信息
 *
 * @example
 * const result = await searchScriptVersions({
 *   scriptId: 123,
 *   page: 0,
 *   rows: 10,
 *   sortName: 'versionNumber',
 *   sortOrder: 'desc'
 * })
 * renderVersionRows(result.data)
 */
export function searchScriptVersions<T = Record<string, unknown>>(data?: Record<string, unknown>) {
  return request.post<PageResult<T>>('/live/script-version/search', data || {})
}

/**
 * 创建话术新版本
 * API: POST /api/v1/live/script-version/save
 *
 * @param data - 版本保存参数
 * @returns 新版本的 ID
 *
 * @example
 * const versionId = await saveScriptVersion({
 *   scriptId: 123,
 *   content: '修改后的话术内容',
 *   changeReason: '优化开场白'
 * })
 */
export function saveScriptVersion(data: Record<string, unknown>) {
  return request.post<number>('/live/script-version/save', data)
}

/**
 * 获取单个版本详情
 * API: POST /api/v1/live/script-version/get
 *
 * @param scriptId - 话术 ID
 * @param versionNumber - 版本号
 * @returns 版本详细信息
 */
export function getScriptVersion(scriptId: number, versionNumber: number) {
  return request.post<Record<string, unknown>>('/live/script-version/get', {
    scriptId,
    versionNumber,
  })
}

/**
 * 对比两个版本
 * API: POST /api/v1/live/script-version/diff
 *
 * @param data - 对比参数 { scriptId, version1, version2 }
 * @returns 版本差异信息
 *
 * @example
 * const diff = await diffScriptVersions({
 *   scriptId: 123,
 *   version1: 1,
 *   version2: 2
 * })
 * renderDiff(diff.data.added, diff.data.removed)
 */
export function diffScriptVersions(data: Record<string, unknown>) {
  return request.post<Record<string, unknown>>('/live/script-version/diff', data)
}

/**
 * 回滚话术到指定版本
 * API: POST /api/v1/live/script-version/rollback
 *
 * @param data - 回滚参数 { scriptId, targetVersion }
 * @returns void
 *
 * @example
 * await rollbackScriptVersion({
 *   scriptId: 123,
 *   targetVersion: 1
 * })
 * refreshVersionHistory()
 */
export function rollbackScriptVersion(data: Record<string, unknown>) {
  return request.post<void>('/live/script-version/rollback', data)
}
