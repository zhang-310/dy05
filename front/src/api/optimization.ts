/**
 * AI 话术自动优化建议系统 - API 客户端
 * W-05: 优化分析、建议、重生成、历史
 * @author Claude Code
 * @since 2026-03-06
 */

import request from '@/utils/request';
import type {
  ScriptAnalysisResultVO,
  OptimizationSuggestionVO,
  RegeneratedScriptVO,
  OptimizationHistoryVO,
  EvolutionMetricsVO,
  AnalysisSearchVO,
  SuggestionSearchVO,
} from '@/types/optimization';
import type { PageResult } from '@/types/common';

/**
 * 分析话术内容，获取优化建议
 */
export function analyzeScript(versionId: number): Promise<ScriptAnalysisResultVO> {
  return request.post<ScriptAnalysisResultVO>('/product/script-version/analyze', {
    versionId,
  });
}

/**
 * 获取优化建议列表
 */
export function getOptimizationSuggestions(
  params: SuggestionSearchVO
): Promise<PageResult<OptimizationSuggestionVO>> {
  return request.post<PageResult<OptimizationSuggestionVO>>(
    '/product/script-version/suggestions/list',
    params
  );
}

/**
 * 获取单个版本的所有建议
 */
export function getVersionSuggestions(
  versionId: number,
  topN: number = 10,
  category?: string
): Promise<OptimizationSuggestionVO[]> {
  return request.post<OptimizationSuggestionVO[]>(
    '/product/script-version/suggestions/by-version',
    {
      versionId,
      topN,
      category,
    }
  );
}

/**
 * 根据优化建议重新生成话术
 */
export function regenerateScript(
  versionId: number,
  suggestionIds: number[],
  useAiEnhance: boolean = true
): Promise<RegeneratedScriptVO> {
  return request.post<RegeneratedScriptVO>(
    '/product/script-version/regenerate',
    {
      versionId,
      suggestionIds,
      useAiEnhance,
    }
  );
}

/**
 * 获取优化历史记录
 */
export function getOptimizationHistory(
  versionId: number,
  limit: number = 50
): Promise<OptimizationHistoryVO[]> {
  return request.post<OptimizationHistoryVO[]>(
    '/product/script-version/optimization-history',
    {
      versionId,
      limit,
    }
  );
}

/**
 * 获取进化指标
 */
export function getEvolutionMetrics(versionId: number): Promise<EvolutionMetricsVO> {
  return request.post<EvolutionMetricsVO>(
    '/product/script-version/evolution-metrics',
    {
      versionId,
    }
  );
}

/**
 * 接受优化建议
 */
export function acceptSuggestion(suggestionId: number): Promise<OptimizationSuggestionVO> {
  return request.post<OptimizationSuggestionVO>(
    '/product/script-version/suggestions/accept',
    {
      suggestionId,
    }
  );
}

/**
 * 拒绝优化建议
 */
export function rejectSuggestion(
  suggestionId: number,
  reason?: string
): Promise<OptimizationSuggestionVO> {
  return request.post<OptimizationSuggestionVO>(
    '/product/script-version/suggestions/reject',
    {
      suggestionId,
      reason,
    }
  );
}

/**
 * 应用重新生成的话术版本
 */
export function applyRegeneratedScript(regeneratedId: number): Promise<RegeneratedScriptVO> {
  return request.post<RegeneratedScriptVO>(
    '/product/script-version/regenerated/apply',
    {
      regeneratedId,
    }
  );
}

/**
 * 拒绝重新生成的话术版本
 */
export function rejectRegeneratedScript(
  regeneratedId: number,
  reason?: string
): Promise<void> {
  return request.post<void>('/product/script-version/regenerated/reject', {
    regeneratedId,
    reason,
  });
}

/**
 * 获取分析历史列表
 */
export function getAnalysisHistory(
  params: AnalysisSearchVO
): Promise<PageResult<ScriptAnalysisResultVO>> {
  return request.post<PageResult<ScriptAnalysisResultVO>>(
    '/product/script-version/analysis-history',
    params
  );
}

/**
 * 批量分析多个版本
 */
export function batchAnalyzeScripts(versionIds: number[]): Promise<ScriptAnalysisResultVO[]> {
  return request.post<ScriptAnalysisResultVO[]>(
    '/product/script-version/batch-analyze',
    {
      versionIds,
    }
  );
}

/**
 * 获取优化建议的统计信息
 */
export function getSuggestionStatistics(versionId?: number): Promise<{
  totalSuggestions: number;
  byCategoryCount: Record<string, number>;
  byPriorityCount: Record<string, number>;
  acceptanceRate: number;
  averageConfidence: number;
}> {
  return request.post<any>(
    '/product/script-version/suggestions/statistics',
    { versionId }
  );
}

/**
 * 导出优化报告
 */
export function exportOptimizationReport(
  versionId: number,
  format: 'pdf' | 'xlsx' = 'pdf'
): Promise<Blob> {
  return request.post<Blob>(
    '/product/script-version/optimization-report/export',
    {
      versionId,
      format,
    },
    {
      responseType: 'blob',
    }
  );
}
