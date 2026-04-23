/**
 * 向量化搜索与混合检索系统 - 类型定义
 * W-06: 混合搜索、建议、分析
 * @author Claude Code
 * @since 2026-03-06
 */

/**
 * 混合搜索结果
 */
export interface HybridSearchResultVO {
  id: number;
  scriptId?: number;
  versionId?: number;
  productId?: number;
  content: string; // 脚本内容
  title?: string; // 脚本标题
  scriptType: 'product' | 'shortvideo' | 'live' | 'event'; // 脚本类型
  style: string; // 脚本风格
  relevanceScore: number; // 相关性评分 0-100（融合后）
  vectorSimilarity: number; // 向量相似度 0-100
  bm25Score: number; // BM25 关键词相似度 0-100
  rerankScore?: number; // 重排评分 0-100
  effectivenessScore?: number; // 效果评分
  usageCount?: number; // 使用次数
  lastUsedAt?: string; // 最后使用时间
  createdAt: string;
  tags?: string[]; // 标签
  preview?: string; // 内容预览
  author?: string; // 作者
  rank?: number; // 搜索结果排名
}

/**
 * 搜索建议
 */
export interface SearchSuggestionVO {
  id: number;
  query: string; // 建议的查询词
  frequency: number; // 频率
  clickThroughRate: number; // 点击率
  popularityScore: number; // 热度评分
  category?: string; // 建议分类
  relatedQueries?: string[]; // 相关查询
  source: 'user_search' | 'trending' | 'ai_generated'; // 来源
  createdAt: string;
}

/**
 * 搜索分析数据
 */
export interface SearchAnalyticsVO {
  period: string; // 时间周期（day/week/month）
  totalSearches: number; // 总搜索次数
  uniqueUsers: number; // 独立用户数
  averageResultsReturned: number; // 平均返回结果数
  averageClickThroughRate: number; // 平均点击率
  topSearchQueries: SearchQueryStats[]; // 热门搜索词
  searchTrend: TrendPoint[]; // 搜索趋势
  resultQualityScores: {
    excellent: number; // 优秀（>80分）
    good: number; // 良好（60-80分）
    fair: number; // 一般（40-60分）
    poor: number; // 差（<40分）
  };
}

/**
 * 搜索查询统计
 */
export interface SearchQueryStats {
  query: string;
  count: number; // 搜索次数
  clickThroughRate: number; // 点击率
  averageResultsViewed: number; // 平均查看结果数
  conversionRate?: number; // 转化率
}

/**
 * 趋势点
 */
export interface TrendPoint {
  timestamp: string;
  count: number;
  value?: number;
}

/**
 * 搜索筛选器
 */
export interface SearchFiltersVO {
  scriptType?: 'product' | 'shortvideo' | 'live' | 'event';
  style?: string[];
  scoreRange?: [number, number]; // [最小分, 最大分]
  dateRange?: [string, string]; // [起始日期, 结束日期]
  tags?: string[];
  minUsageCount?: number;
  author?: string[];
}

/**
 * 混合搜索请求参数
 */
export interface HybridSearchRequest {
  query: string; // 搜索查询
  filters?: SearchFiltersVO; // 筛选条件
  topK?: number; // 返回结果数（默认 20）
  useVectorSearch?: boolean; // 是否使用向量搜索（默认 true）
  useBM25Search?: boolean; // 是否使用 BM25 搜索（默认 true）
  useReranking?: boolean; // 是否使用重排（默认 true）
  similarityThreshold?: number; // 相似度阈值（0-1，默认 0.3）
  page?: number; // 分页页码
  pageSize?: number; // 分页大小
}

/**
 * 搜索反馈
 */
export interface SearchFeedbackVO {
  id: number;
  searchId: string; // 搜索会话 ID
  resultId: number; // 结果 ID
  query: string; // 搜索词
  isHelpful: boolean; // 是否有帮助
  rating?: number; // 评分（1-5）
  feedback?: string; // 反馈文本
  clickPosition?: number; // 点击的结果位置
  timeSpentMs?: number; // 停留时间（ms）
  createdAt: string;
}

/**
 * 搜索会话
 */
export interface SearchSessionVO {
  id: string; // 会话 ID
  userId: number;
  queries: string[]; // 查询列表
  sessionStartTime: string;
  sessionEndTime?: string;
  totalQueries: number;
  clickedResults: number;
  conversionStatus: boolean; // 是否转化
}

/**
 * 自动完成建议
 */
export interface AutocompleteResultVO {
  query: string;
  suggestions: {
    text: string;
    type: 'query' | 'script' | 'tag'; // 建议类型
    score: number; // 相关性评分
    metadata?: Record<string, any>;
  }[];
  executionTimeMs: number;
}

/**
 * 搜索统计信息
 */
export interface SearchStatsVO {
  q: string;
  totalMatches: number;
  vectorMatches: number;
  bm25Matches: number;
  rerankMatches: number;
  executionTimeMs: number;
  cacheHit: boolean;
}

/**
 * 搜索历史项
 */
export interface SearchHistoryItemVO {
  id: number;
  userId: number;
  query: string;
  resultsCount: number;
  clickCount: number;
  lastSearchAt: string;
  searchCount: number; // 相同查询的总搜索次数
}
