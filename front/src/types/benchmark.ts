/**
 * 对标账号分析系统类型定义
 */

import { BasicQuery } from './common';

// ==================== 对标账号 ====================

export interface BenchmarkAccount {
  id: number;
  ownerId: number;
  accountName: string;
  accountUrl: string;
  secUid: string;
  avatarUrl?: string;
  followerCount?: number;
  videoCount?: number;
  likeCount?: number;
  description?: string;
  tags?: string;
  lastCollectTime?: string;
  createTime: string;
  updateTime: string;
}

export interface BenchmarkAccountSearchVO extends BasicQuery {
  keyword?: string;
  minFollowerCount?: number;
  tags?: string;
}

export interface BenchmarkAccountSaveVO {
  id?: number;
  accountName: string;
  accountUrl: string;
  secUid: string;
  avatarUrl?: string;
  followerCount?: number;
  videoCount?: number;
  likeCount?: number;
  description?: string;
  tags?: string;
}

export interface SearchAccountByKeywordVO {
  keyword: string;
  minFollowerCount?: number;
  maxResults?: number;
}

export interface AnalyzeAccountByUrlVO {
  accountUrl: string;
}

// ==================== 对标视频 ====================

export interface BenchmarkVideo {
  id: number;
  ownerId: number;
  benchmarkAccountId: number;
  videoId: string;
  videoUrl: string;
  title?: string;
  description?: string;
  coverUrl?: string;
  duration?: number;
  likeCount?: number;
  commentCount?: number;
  shareCount?: number;
  collectCount?: number;
  publishTime?: string;
  localPath?: string;
  bosUrl?: string;
  analysisStatus: 'pending' | 'processing' | 'completed' | 'failed';
  analysisErrorMsg?: string;
  createTime: string;
  updateTime: string;
}

export interface BenchmarkVideoSearchVO extends BasicQuery {
  benchmarkAccountId?: number;
  keyword?: string;
  minLikeCount?: number;
  analysisStatus?: string;
}

export interface BenchmarkVideoSaveVO {
  id?: number;
  benchmarkAccountId: number;
  videoId: string;
  videoUrl: string;
  title?: string;
  description?: string;
  coverUrl?: string;
  duration?: number;
  likeCount?: number;
  commentCount?: number;
  shareCount?: number;
  collectCount?: number;
  publishTime?: string;
}

export interface CollectAccountVideosVO {
  benchmarkAccountId: number;
  minLikeCount?: number;
  maxVideos?: number;
}

// ==================== 深度分析 ====================

export interface BenchmarkAnalysis {
  id: number;
  ownerId: number;
  benchmarkVideoId: number;

  // 文案内容
  transcriptText?: string;
  ocrText?: string;
  apiDescription?: string;
  mergedContent?: string;

  // 场景分析
  keyFramesJson?: string;
  sceneDescription?: string;
  sceneCount?: number;

  // 创意分析
  creativeType?: string;
  hookStrategy?: string;
  contentStructure?: string;
  emotionalCurve?: string;
  pacingAnalysis?: string;

  // 爆款因素
  viralFactors?: string;
  strengths?: string;
  weaknesses?: string;
  replicableElements?: string;

  // AI 深度分析
  aiSummary?: string;
  scriptBreakdown?: string;
  improvementSuggestions?: string;
  targetAudience?: string;

  // 竞品对比
  comparisonReport?: string;
  differentiationPoints?: string;

  // 元数据
  aiModelUsed?: string;
  tokensUsed?: number;
  analysisDurationMs?: number;

  createTime: string;
  updateTime: string;
}

export interface AnalyzeVideoVO {
  benchmarkVideoId: number;
  forceReanalyze?: boolean;
  enableAsr?: boolean;
  enableOcr?: boolean;
  enableApi?: boolean;
  aiModel?: string;
  cookieId?: number;
}

export interface BatchAnalyzeVideosVO {
  videoIds: number[];
  forceReanalyze?: boolean;
  aiModel?: string;
}

// ==================== Cookie 管理 ====================

export interface DouyinCookie {
  id: number;
  ownerId: number;
  cookieName: string;
  cookieValue: string;
  isValid: boolean;
  lastValidateTime?: string;
  useCount: number;
  createTime: string;
  updateTime: string;
}

export interface DouyinCookieSearchVO extends BasicQuery {
  keyword?: string;
  isValid?: boolean;
}

export interface DouyinCookieSaveVO {
  id?: number;
  cookieName: string;
  cookieValue: string;
}

export interface ValidateCookieVO {
  cookieId: number;
}

export interface DouyinQrLoginStartResultVO {
  sessionId: string;
  qrImageBase64: string;
  message?: string;
}

export type DouyinQrLoginPollStatus = 'waiting' | 'success' | 'expired' | 'error';

export interface DouyinQrLoginPollResultVO {
  status: DouyinQrLoginPollStatus;
  cookieValue?: string;
  message?: string;
}

// ==================== 任务管理 ====================

export interface BenchmarkTask {
  id: number;
  ownerId: number;
  taskType: 'single_analysis' | 'batch_analysis';
  taskStatus: 'pending' | 'running' | 'completed' | 'failed';
  totalCount: number;
  successCount: number;
  failedCount: number;
  errorMsg?: string;
  startTime?: string;
  endTime?: string;
  createTime: string;
  updateTime: string;
}

// ==================== 质量脚本知识库 ====================

export interface BenchmarkQualityScript {
  id: number;
  ownerId: number;
  videoId: number;
  analysisId: number;
  scriptContent: string;
  scriptType?: string;
  industry?: string;
  sceneType?: string;
  qualityScore: number;
  engagementRate?: number;
  viralScore?: number;
  completionRate?: number;
  aiRating?: number;
  likesCount?: number;
  commentsCount?: number;
  sharesCount?: number;
  collectionsCount?: number;
  viewsCount?: number;
  videoDuration?: number;
  keyFeatures?: string;
  creativeElements?: string;
  hookStrategy?: string;
  contentStructure?: string;
  embeddingVector?: string;
  referenceCount?: number;
  lastReferencedAt?: string;
  createTime: string;
  updateTime: string;
}

export interface BenchmarkQualityScriptSearchVO extends BasicQuery {
  keyword?: string;
  industry?: string;
  sceneType?: string;
  scriptType?: string;
  minQualityScore?: number;
  minEngagementRate?: number;
}

export interface BenchmarkQualityScriptSaveVO {
  id?: number;
  videoId: number;
  analysisId: number;
  scriptContent: string;
  scriptType?: string;
  industry?: string;
  sceneType?: string;
  qualityScore: number;
  engagementRate?: number;
  viralScore?: number;
  completionRate?: number;
  aiRating?: number;
  likesCount?: number;
  commentsCount?: number;
  sharesCount?: number;
  collectionsCount?: number;
  viewsCount?: number;
  videoDuration?: number;
  keyFeatures?: string;
  creativeElements?: string;
  hookStrategy?: string;
  contentStructure?: string;
}

// ==================== 脚本相似度 ====================

export interface BenchmarkScriptSimilarityVO {
  scriptId: number;
  videoId: number;
  scriptContent: string;
  scriptType?: string;
  industry?: string;
  sceneType?: string;
  qualityScore: number;
  similarityScore?: number;
  engagementRate?: number;
  viralScore?: number;
  likesCount?: number;
  commentsCount?: number;
  sharesCount?: number;
  collectionsCount?: number;
  viewsCount?: number;
}

export interface FindSimilarScriptsVO {
  scriptId: number;
  topK?: number;
  minScore?: number;
}

export interface FindSimilarByTextVO {
  text: string;
  topK?: number;
  minScore?: number;
}

// ==================== 脚本推荐 ====================

export interface RecommendByRequirementVO {
  requirement: string;
  topK?: number;
}

export interface RecommendByIndustrySceneVO {
  industry?: string;
  sceneType?: string;
  topK?: number;
}

export interface RecommendByScriptTypeVO {
  scriptType: string;
  referenceScriptId?: number;
  topK?: number;
}

export interface SmartRecommendVO {
  filters?: {
    industry?: string;
    sceneType?: string;
    scriptType?: string;
    minQualityScore?: number;
  };
  referenceText?: string;
  topK?: number;
}

export interface RecommendImprovementVO {
  analysisId: number;
  topK?: number;
}

// ==================== 返回类型 ====================

export type BenchmarkAccountVO = BenchmarkAccount;
export type BenchmarkVideoVO = BenchmarkVideo;
export type BenchmarkAnalysisVO = BenchmarkAnalysis;
export type DouyinCookieVO = DouyinCookie;
export type BenchmarkTaskVO = BenchmarkTask;
export type BenchmarkQualityScriptVO = BenchmarkQualityScript;
