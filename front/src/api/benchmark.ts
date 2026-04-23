/**
 * 对标账号分析系统 API
 */

import request from '@/utils/request';
import type { PageResult } from '@/types/common';
import type {
  BenchmarkAccountVO,
  BenchmarkAccountSearchVO,
  BenchmarkAccountSaveVO,
  SearchAccountByKeywordVO,
  AnalyzeAccountByUrlVO,
  BenchmarkVideoVO,
  BenchmarkVideoSearchVO,
  BenchmarkVideoSaveVO,
  CollectAccountVideosVO,
  BenchmarkAnalysisVO,
  AnalyzeVideoVO,
  BatchAnalyzeVideosVO,
  DouyinCookieVO,
  DouyinCookieSearchVO,
  DouyinCookieSaveVO,
  ValidateCookieVO,
  DouyinQrLoginStartResultVO,
  DouyinQrLoginPollResultVO,
  BenchmarkQualityScriptVO,
  BenchmarkQualityScriptSearchVO,
  BenchmarkQualityScriptSaveVO,
  BenchmarkScriptSimilarityVO,
  FindSimilarScriptsVO,
  FindSimilarByTextVO,
  RecommendByRequirementVO,
  RecommendByIndustrySceneVO,
  RecommendByScriptTypeVO,
  SmartRecommendVO,
  RecommendImprovementVO,
} from '@/types/benchmark';

// ==================== 对标账号 API ====================

export const benchmarkAccountApi = {
  /**
   * 分页查询账号
   */
  list: (params: BenchmarkAccountSearchVO) =>
    request.post<PageResult<BenchmarkAccountVO>>('/benchmark/account/list', params),

  /**
   * 获取账号详情
   */
  get: (id: number) =>
    request.post<BenchmarkAccountVO>('/benchmark/account/get', id),

  /**
   * 保存账号
   */
  save: (data: BenchmarkAccountSaveVO) =>
    request.post<BenchmarkAccountVO>('/benchmark/account/save', data),

  /**
   * 删除账号
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/account/delete', id),

  /**
   * 按关键词搜索账号
   */
  searchByKeyword: (params: SearchAccountByKeywordVO) =>
    request.post<BenchmarkAccountVO[]>('/benchmark/account/search-by-keyword', params),

  /**
   * 按URL分析账号
   */
  analyzeByUrl: (params: AnalyzeAccountByUrlVO) =>
    request.post<BenchmarkAccountVO>('/benchmark/account/analyze-by-url', params),
};

// ==================== 对标视频 API ====================

export const benchmarkVideoApi = {
  /**
   * 分页查询视频
   */
  list: (params: BenchmarkVideoSearchVO) =>
    request.post<PageResult<BenchmarkVideoVO>>('/benchmark/video/list', params),

  /**
   * 获取视频详情
   */
  get: (id: number) =>
    request.post<BenchmarkVideoVO>('/benchmark/video/get', id),

  /**
   * 保存视频
   */
  save: (data: BenchmarkVideoSaveVO) =>
    request.post<BenchmarkVideoVO>('/benchmark/video/save', data),

  /**
   * 删除视频
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/video/delete', id),

  /**
   * 采集账号视频
   */
  collect: (params: CollectAccountVideosVO) =>
    request.post<BenchmarkVideoVO[]>('/benchmark/video/collect', params),
};

// ==================== 深度分析 API ====================

export const benchmarkAnalysisApi = {
  /**
   * 分析单个视频
   */
  analyze: (params: AnalyzeVideoVO) =>
    request.post<BenchmarkAnalysisVO>('/benchmark/analysis/analyze', params),

  /**
   * 批量分析视频
   */
  batchAnalyze: (params: BatchAnalyzeVideosVO) =>
    request.post<number>('/benchmark/analysis/batch-analyze', params),

  /**
   * 获取分析结果
   */
  getByVideo: (videoId: number) =>
    request.post<BenchmarkAnalysisVO>('/benchmark/analysis/get-by-video', videoId),
};

// ==================== Cookie 管理 API ====================

export const douyinCookieApi = {
  /**
   * 分页查询Cookie
   */
  list: (params: DouyinCookieSearchVO) =>
    request.post<PageResult<DouyinCookieVO>>('/benchmark/cookie/list', params),

  /**
   * 获取Cookie详情
   */
  get: (id: number) =>
    request.post<DouyinCookieVO>('/benchmark/cookie/get', id),

  /**
   * 保存Cookie
   */
  save: (data: DouyinCookieSaveVO) =>
    request.post<DouyinCookieVO>('/benchmark/cookie/save', data),

  /**
   * 删除Cookie
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/cookie/delete', id),

  /**
   * 验证Cookie
   */
  validate: (params: ValidateCookieVO) =>
    request.post<boolean>('/benchmark/cookie/validate', params),

  /** 服务端 Playwright 打开抖音页并返回整页截图（含二维码） */
  qrLoginStart: () =>
    request.post<DouyinQrLoginStartResultVO>('/benchmark/cookie/qr-login/start', {}),

  qrLoginPoll: (sessionId: string) =>
    request.post<DouyinQrLoginPollResultVO>('/benchmark/cookie/qr-login/poll', { sessionId }),

  qrLoginCancel: (sessionId: string) =>
    request.post<void>('/benchmark/cookie/qr-login/cancel', { sessionId }),
};

// ==================== 质量脚本知识库 API ====================

export const benchmarkQualityScriptApi = {
  /**
   * 分页查询质量脚本
   */
  search: (params: BenchmarkQualityScriptSearchVO) =>
    request.post<PageResult<BenchmarkQualityScriptVO>>('/benchmark/quality-script/search', params),

  /**
   * 获取质量脚本详情
   */
  get: (id: number) =>
    request.post<BenchmarkQualityScriptVO>('/benchmark/quality-script/get', { id }),

  /**
   * 保存质量脚本
   */
  save: (data: BenchmarkQualityScriptSaveVO) =>
    request.post<BenchmarkQualityScriptVO>('/benchmark/quality-script/save', data),

  /**
   * 删除质量脚本
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/quality-script/delete', { id }),

  /**
   * 根据视频ID查询质量脚本
   */
  getByVideo: (videoId: number) =>
    request.post<BenchmarkQualityScriptVO>('/benchmark/quality-script/get-by-video', { videoId }),

  /**
   * 根据分析ID查询质量脚本
   */
  getByAnalysis: (analysisId: number) =>
    request.post<BenchmarkQualityScriptVO>('/benchmark/quality-script/get-by-analysis', { analysisId }),

  /**
   * 获取指定行业的高质量脚本
   */
  getHighQualityByIndustry: (industry: string, minScore: number) =>
    request.post<BenchmarkQualityScriptVO[]>('/benchmark/quality-script/get-high-quality-by-industry', {
      industry,
      minScore,
    }),

  /**
   * 获取指定场景的高质量脚本
   */
  getHighQualityByScene: (sceneType: string, minScore: number) =>
    request.post<BenchmarkQualityScriptVO[]>('/benchmark/quality-script/get-high-quality-by-scene', {
      sceneType,
      minScore,
    }),

  /**
   * 获取互动率最高的脚本
   */
  getTopEngagement: (limit: number = 10) =>
    request.post<BenchmarkQualityScriptVO[]>('/benchmark/quality-script/get-top-engagement', { limit }),

  /**
   * 获取传播力最高的脚本
   */
  getTopViral: (limit: number = 10) =>
    request.post<BenchmarkQualityScriptVO[]>('/benchmark/quality-script/get-top-viral', { limit }),

  /**
   * 计算质量评分
   */
  calculateQualityScore: (data: BenchmarkQualityScriptSaveVO) =>
    request.post<number>('/benchmark/quality-script/calculate-quality-score', data),
};

// ==================== 脚本相似度 API ====================

export const benchmarkScriptSimilarityApi = {
  /**
   * 生成向量嵌入
   */
  generateEmbedding: (scriptId: number) =>
    request.post<void>('/benchmark/script-similarity/generate-embedding', { scriptId }),

  /**
   * 批量生成向量嵌入
   */
  batchGenerateEmbeddings: (scriptIds: number[]) =>
    request.post<number>('/benchmark/script-similarity/batch-generate-embeddings', { scriptIds }),

  /**
   * 索引到Milvus
   */
  indexToMilvus: (scriptId: number) =>
    request.post<void>('/benchmark/script-similarity/index-to-milvus', { scriptId }),

  /**
   * 批量索引到Milvus
   */
  batchIndexToMilvus: (scriptIds: number[]) =>
    request.post<number>('/benchmark/script-similarity/batch-index-to-milvus', { scriptIds }),

  /**
   * 查找相似脚本
   */
  findSimilar: (params: FindSimilarScriptsVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-similarity/find-similar', params),

  /**
   * 根据文本查找相似脚本
   */
  findSimilarByText: (params: FindSimilarByTextVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-similarity/find-similar-by-text', params),

  /**
   * 计算两个脚本的相似度
   */
  calculateSimilarity: (scriptId1: number, scriptId2: number) =>
    request.post<number>('/benchmark/script-similarity/calculate-similarity', { scriptId1, scriptId2 }),

  /**
   * 获取未生成向量的脚本
   */
  getUnembeddedScripts: (limit: number = 100) =>
    request.post<number[]>('/benchmark/script-similarity/get-unembedded-scripts', { limit }),

  /**
   * 获取未索引的脚本
   */
  getUnindexedScripts: (limit: number = 100) =>
    request.post<number[]>('/benchmark/script-similarity/get-unindexed-scripts', { limit }),
};

// ==================== 脚本推荐 API ====================

export const benchmarkScriptRecommendationApi = {
  /**
   * 根据需求推荐脚本
   */
  recommendByRequirement: (params: RecommendByRequirementVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/recommend-by-requirement', params),

  /**
   * 根据行业场景推荐脚本
   */
  recommendByIndustryScene: (params: RecommendByIndustrySceneVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/recommend-by-industry-scene', params),

  /**
   * 根据脚本类型推荐脚本
   */
  recommendByScriptType: (params: RecommendByScriptTypeVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/recommend-by-script-type', params),

  /**
   * 智能推荐
   */
  smartRecommend: (params: SmartRecommendVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/smart-recommend', params),

  /**
   * 获取热门脚本
   */
  getPopularScripts: (topK: number = 10) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/get-popular-scripts', { topK }),

  /**
   * 获取最新高质量脚本
   */
  getLatestQualityScripts: (topK: number = 10, minQualityScore?: number) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/get-latest-quality-scripts', {
      topK,
      minQualityScore,
    }),

  /**
   * 推荐改进脚本
   */
  recommendImprovementScripts: (params: RecommendImprovementVO) =>
    request.post<BenchmarkScriptSimilarityVO[]>('/benchmark/script-recommendation/recommend-improvement-scripts', params),
};
