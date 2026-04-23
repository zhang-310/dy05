/**
 * 商品话术版本效果评分系统 - TypeScript 类型定义
 * @author Claude Code
 * @since 2026-03-06
 */

import type { PageResult } from '@/types/common'

/** 排行榜单项 */
export interface ScriptRankingItem {
  versionId: number
  versionNumber: number
  style: string
  score: number
  scoreLevel: 'A' | 'B' | 'C' | 'D' | 'F'
  usageCount: number
  conversionRate: number
  likesCount: number
  commentsCount: number
  isRecommended: boolean
  lastUpdated: string
}

/** 排行榜响应 */
export interface ScriptRankingResponse extends PageResult<ScriptRankingItem> {
  productId: number
}

/** 对比指标 */
export interface ComparisonMetrics {
  score: number
  usageCount: number
  conversionRate: number
  totalInteraction: number
}

/** 对比版本 */
export interface ComparisonVersion {
  versionId: number
  versionNumber: number
  style: string
  metrics: ComparisonMetrics
}

/** 对比响应 */
export interface ComparisonResponse {
  versions: ComparisonVersion[]
  comparison: {
    [key: string]: {
      highest: number
      lowest: number
      diff: number
    }
  }
}

/** 趋势数据点 */
export interface TrendPoint {
  date: string
  score: number
  scoreLevel: 'A' | 'B' | 'C' | 'D' | 'F'
  usageCount: number
  conversionRate: number
  likesCount: number
}

/** 趋势分析数据 */
export interface TrendAnalysis {
  overallTrend: 'up' | 'down' | 'stable'
  scoreChange: number
  highestDate: string
  lowestDate: string
  volatility: number
}

/** 趋势响应 */
export interface TrendResponse {
  timeline: TrendPoint[]
  analysis: TrendAnalysis
}

/** 风格指标 */
export interface StyleMetrics {
  style: string
  versionCount: number
  avgScore: number
  avgConversion: number
  topVersion: {
    versionNumber: number
    score: number
  }
}

/** 风格对比响应 */
export interface StyleComparisonResponse {
  byStyle: {
    [style: string]: StyleMetrics
  }
}

/** 重新计算响应 */
export interface RecalculateResponse {
  success: boolean
  message: string
  updatedCount: number
}

/** 分析响应 */
export interface AnalysisResponse {
  insights: string[]
  recommendations: string[]
  anomalies: string[]
}

/** 快照响应 */
export interface SnapshotResponse {
  snapshotId: number
  versionId: number
  timestamp: string
}

/** 缓存清除响应 */
export interface ClearCacheResponse {
  success: boolean
  message: string
}
