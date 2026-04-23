/**
 * 效果评分 API 客户端
 * @author Claude Code
 * @since 2026-03-06
 */

import request from '@/utils/request'
import type {
  ScriptRankingResponse,
  ComparisonResponse,
  TrendResponse,
  StyleComparisonResponse,
  RecalculateResponse,
  AnalysisResponse,
  SnapshotResponse,
  ClearCacheResponse,
} from '@/types/effectiveness'

/**
 * 获取话术排行榜
 */
export function getRanking(
  productIdOrParams: number | { sessionId: number; page: number; pageSize: number },
  topN: number = 20,
  sortBy: string = 'score',
  page: number = 0,
  rows: number = 20
): Promise<ScriptRankingResponse | unknown> {
  // 支持两种调用方式：旧版本直播 ({sessionId, page, pageSize}) 和新版本产品 (productId, ...)
  if (typeof productIdOrParams === 'object' && 'sessionId' in productIdOrParams) {
    // 直播版本调用
    return request.post<unknown>('/live/effectiveness/ranking', productIdOrParams)
  }
  // 产品版本调用
  return request.post<ScriptRankingResponse>('/product/script-version/ranking', {
    productId: productIdOrParams,
    topN,
    sortBy,
    page,
    rows,
  })
}

/**
 * 获取单个话术效果详情（用于直播场景）
 */
export function getScriptEffectiveness(scriptId: number): Promise<unknown> {
  return request.post<unknown>('/live/effectiveness/script-effectiveness', {
    scriptId,
  })
}

/**
 * 对比两个版本（直播场景兼容）
 */
export function compareVersions(
  data: { versionA: number; versionB: number } | number,
  versionIds?: number[],
  metrics?: string[]
): Promise<ComparisonResponse> {
  // 支持两种调用方式：旧版本 (versionA, versionB) 和新版本 (productId, versionIds, metrics)
  if (typeof data === 'object' && 'versionA' in data) {
    // 直播版本调用
    return request.post<ComparisonResponse>('/live/effectiveness/compare', data)
  }
  // 产品版本调用
  return request.post<ComparisonResponse>('/product/script-version/compare', {
    productId: data,
    versionIds,
    metrics,
  })
}

/**
 * 获取效果趋势
 */
export function getTrend(
  productId: number,
  versionId?: number,
  timePeriod: number = 30,
  groupBy: string = 'day'
): Promise<TrendResponse> {
  return request.post<TrendResponse>('/product/script-version/trend', {
    productId,
    versionId,
    timePeriod,
    groupBy,
  })
}

/**
 * 重新计算效果评分
 */
export function recalculateScores(
  productId: number,
  includeHistorical: boolean = false
): Promise<RecalculateResponse> {
  return request.post<RecalculateResponse>('/product/script-version/recalculate-scores', {
    productId,
    includeHistorical,
  })
}

/**
 * 获取风格对比
 */
export function getStyleComparison(productId: number): Promise<StyleComparisonResponse> {
  return request.post<StyleComparisonResponse>('/product/script-version/style-comparison', {
    productId,
  })
}

/**
 * 获取分析洞察
 */
export function getAnalysis(productId: number, versionId?: number): Promise<AnalysisResponse> {
  return request.post<AnalysisResponse>('/product/script-version/analysis', {
    productId,
    versionId,
  })
}

/**
 * 记录版本快照
 */
export function recordSnapshot(versionId: number): Promise<SnapshotResponse> {
  return request.post<SnapshotResponse>('/product/script-version/snapshot', {
    versionId,
  })
}

/**
 * 清除评分缓存
 */
export function clearCache(productId: number): Promise<ClearCacheResponse> {
  return request.post<ClearCacheResponse>('/product/script-version/clear-cache', {
    productId,
  })
}
