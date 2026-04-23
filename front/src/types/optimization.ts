/**
 * AI 话术自动优化建议系统 - 类型定义
 * W-05: 优化分析、建议、重生成、历史
 * @author Claude Code
 * @since 2026-03-06
 */

/**
 * 话术分析结果
 */
export interface ScriptAnalysisResultVO {
  versionId: number;
  scriptId: number;
  productId: number;
  analysisTime: string;
  overallScore: number; // 0-100
  contentQuality: number; // 内容质量评分
  persuasivenessScore: number; // 说服力评分
  claritScore: number; // 清晰度评分
  emotionalResonance: number; // 情感共鸣评分
  callToActionStrength: number; // 行动号召力
  weaknessCount: number; // 弱点数量
  suggestionsCount: number; // 建议数量
  estimatedEffectivenessImprovement: number; // 预期效果提升百分比
  keyWeaknesses: string[]; // 主要弱点描述
  detailedMetrics: Record<string, number>; // 详细指标
}

/**
 * 优化建议
 */
export interface OptimizationSuggestionVO {
  id: number;
  analysisId: number;
  versionId: number;
  category: 'phrasing' | 'structure' | 'emotion' | 'cta' | 'timing' | 'audience'; // 建议分类
  priority: 'high' | 'medium' | 'low'; // 优先级
  title: string; // 建议标题
  description: string; // 详细说明
  affectedSegment: string; // 影响的文本片段
  suggestedImprovement: string; // 建议改进文本
  expectedImprovement: number; // 预期改进百分比
  confidence: number; // 置信度 0-100
  acceptanceCount: number; // 被接受的次数
  rejectionCount: number; // 被拒绝的次数
  status: 'pending' | 'accepted' | 'rejected' | 'applied'; // 建议状态
  createdAt: string;
  updatedAt: string;
}

/**
 * 重新生成的话术版本
 */
export interface RegeneratedScriptVO {
  id: number;
  originalVersionId: number;
  suggestionsApplied: number[]; // 应用的建议 ID 列表
  regeneratedContent: string; // 重生成的内容
  appliedSuggestionCount: number; // 应用的建议数
  estimatedImprovementScore: number; // 预期改进分数
  contentDifference: ContentDifferenceVO; // 内容差异对比
  createdAt: string;
  approvalStatus: 'pending' | 'approved' | 'rejected'; // 审批状态
  appliedAt?: string; // 应用时间
}

/**
 * 内容差异对比
 */
export interface ContentDifferenceVO {
  originalContent: string; // 原内容
  regeneratedContent: string; // 重生成内容
  differenceSummary: string; // 差异摘要
  changes: TextChange[]; // 具体改变
  similarityScore: number; // 相似度 0-100
}

/**
 * 文本改变项
 */
export interface TextChange {
  type: 'insertion' | 'deletion' | 'replacement'; // 改变类型
  position: number; // 位置
  original: string; // 原文本
  modified: string; // 修改后文本
  reason: string; // 改变原因
}

/**
 * 优化历史记录
 */
export interface OptimizationHistoryVO {
  versionId: number;
  timestamp: string;
  action: 'analyzed' | 'regenerated' | 'applied' | 'reverted'; // 操作类型
  details: string;
  resultScore?: number; // 结果评分
  effectivenessChange?: number; // 效果变化
}

/**
 * 优化进化指标
 */
export interface EvolutionMetricsVO {
  versionId: number;
  totalAnalysis: number; // 总分析次数
  totalSuggestions: number; // 总建议数
  acceptedSuggestions: number; // 已接受建议数
  rejectedSuggestions: number; // 已拒绝建议数
  appliedRegenerations: number; // 应用的重生成次数
  averageScoreImprovement: number; // 平均分数改进
  latestEffectivenessScore: number; // 最新效果评分
  evolutionTrend: EvolutionTrendPoint[]; // 进化趋势点
  optimizationSuccessRate: number; // 优化成功率
  lastOptimizedAt: string;
}

/**
 * 进化趋势点
 */
export interface EvolutionTrendPoint {
  timestamp: string;
  overallScore: number;
  suggestionsApplied: number;
  averageImprovement: number;
}

/**
 * 分析搜索参数
 */
export interface AnalysisSearchVO {
  page?: number;
  rows?: number;
  sortName?: 'createdAt' | 'overallScore' | 'suggestionsCount';
  sortOrder?: 'asc' | 'desc';
  versionIdList?: number[]; // 版本 ID 列表
  scoreRange?: [number, number]; // 分数范围
  dateFrom?: string; // 起始日期
  dateTo?: string; // 结束日期
}

/**
 * 建议搜索参数
 */
export interface SuggestionSearchVO {
  page?: number;
  rows?: number;
  sortName?: 'priority' | 'confidence' | 'createdAt';
  sortOrder?: 'asc' | 'desc';
  versionId?: number;
  category?: string;
  status?: string;
  priority?: string;
}
