/**
 * AI 话术自动优化建议系统 - 类型定义
 * W-05: 优化分析、建议、重生成、历史
 * @author Claude Code
 * @since 2026-03-06
 */

export interface ScriptWeakPointVO {
  type?: string;
  timeRange?: string;
  severity?: string;
  description?: string;
}

/**
 * 话术分析结果。当前真实后端为 /product/script/analyze 返回的 ScriptAnalysisResultVO。
 */
export interface ScriptAnalysisResultVO {
  id?: number;
  scriptVersionId?: number;
  overallScore?: number;
  effectivenessMetrics?: {
    interactionRate?: number;
    conversionRate?: number;
    fanGrowth?: number;
    commentSentiment?: number;
  };
  weakPoints?: ScriptWeakPointVO[];
  styleProfile?: {
    dominantStyle?: string;
    styleScores?: Record<string, number>;
  };
  analysisType?: string;
  dataSource?: string;
  createdAt?: string;

  /** @deprecated 旧前端字段，保留给遗留组件兼容 */
  versionId?: number;
  scriptId?: number;
  productId?: number;
  analysisTime?: string;
  contentQuality?: number;
  persuasivenessScore?: number;
  claritScore?: number;
  emotionalResonance?: number;
  callToActionStrength?: number;
  weaknessCount?: number;
  suggestionsCount?: number;
  estimatedEffectivenessImprovement?: number;
  keyWeaknesses?: string[];
  detailedMetrics?: Record<string, number>;
}

/**
 * 优化建议
 */
export interface OptimizationSuggestionVO {
  id: number;
  scriptVersionId?: number;
  analysisResultId?: number;
  category: string;
  priority: string;
  suggestionContent?: string;
  relatedWeakPoint?: string;
  expectedImprovement?: number | {
    interactionRateIncrease?: number;
    conversionRateIncrease?: number;
    confidence?: number;
  };
  adoptionStatus?: string;
  adoptedAt?: string;
  adoptionNotes?: string;
  createdAt?: string;

  /** @deprecated 旧前端字段，保留给遗留组件兼容 */
  analysisId?: number;
  versionId?: number;
  title?: string;
  description?: string;
  affectedSegment?: string;
  suggestedImprovement?: string;
  confidence?: number;
  acceptanceCount?: number;
  rejectionCount?: number;
  status?: 'pending' | 'accepted' | 'rejected' | 'applied';
  updatedAt?: string;
}

/**
 * 重新生成的话术版本
 */
export interface RegeneratedScriptVO {
  id: number;
  scriptVersionId?: number;
  suggestionId?: number;
  generationStyle?: string;
  regeneratedContent: string;
  aiQualityScore?: number;
  estimatedMetrics?: {
    interactionRate?: number;
    conversionRate?: number;
    estimatedFanGrowth?: number;
  };
  isApplied?: boolean;
  appliedAt?: string;
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'pending' | 'approved' | 'rejected';
  approvedBy?: number;
  approvedAt?: string;
  approvalNotes?: string;
  createdAt?: string;

  /** @deprecated 旧前端字段，保留给遗留组件兼容 */
  originalVersionId?: number;
  suggestionsApplied?: number[];
  appliedSuggestionCount?: number;
  estimatedImprovementScore?: number;
  contentDifference?: ContentDifferenceVO;
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
