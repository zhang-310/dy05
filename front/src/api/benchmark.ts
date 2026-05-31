/**
 * 对标账号分析系统 API
 */

import request from '@/utils/request';
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
import {
  isRecord,
  normalizeArray as normalizeResponseArray,
  normalizePage as normalizeResponsePage,
  normalizeRecord,
  parseJsonValue,
  readNumber,
} from '@/utils/response-normalize';

function asRecord(raw: unknown): Record<string, unknown> {
  return normalizeRecord(raw, ['account', 'video', 'analysis', 'cookie', 'script']);
}

function readString(record: Record<string, unknown>, keys: string[], fallback = ''): string {
  for (const key of keys) {
    const value = record[key];
    if (value != null && String(value).trim()) return String(value);
  }
  return fallback;
}

function readOptionalString(record: Record<string, unknown>, keys: string[]): string | undefined {
  const value = readString(record, keys);
  return value || undefined;
}

function readOptionalNumber(record: Record<string, unknown>, keys: string[]): number | undefined {
  for (const key of keys) {
    if (record[key] == null) continue;
    const value = Number(record[key]);
    if (Number.isFinite(value)) return value;
  }
  return undefined;
}

function readOptionalBoolean(record: Record<string, unknown>, keys: string[]): boolean | undefined {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === 'boolean') return value;
    if (typeof value === 'number') return value !== 0;
    if (typeof value === 'string') {
      const normalized = value.trim().toLowerCase();
      if (['true', '1', 'yes', 'y'].includes(normalized)) return true;
      if (['false', '0', 'no', 'n'].includes(normalized)) return false;
    }
  }
  return undefined;
}

function normalizeDateString(record: Record<string, unknown>, keys: string[]): string | undefined {
  return readOptionalString(record, keys);
}

function normalizeJsonField(record: Record<string, unknown>, keys: string[]): string | undefined {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === 'string' && value.trim()) return value;
    if (value != null && (Array.isArray(value) || isRecord(value))) return JSON.stringify(value);
  }
  return undefined;
}

function normalizeCookie(raw: unknown): DouyinCookieVO {
  const record = asRecord(raw);
  return {
    ...(record as Partial<DouyinCookieVO>),
    id: readNumber(record, ['id', 'cookieId'], 0),
    ownerId: readNumber(record, ['ownerId', 'userId'], 0),
    cookieName: readString(record, ['cookieName', 'name', 'label']),
    cookieValue: readString(record, ['cookieValue', 'value', 'header', 'cookieHeader']),
    isValid: readOptionalBoolean(record, ['isValid', 'valid', 'enabled', 'active']) ?? readStatusAsValid(record),
    lastValidateTime: normalizeDateString(record, ['lastValidateTime', 'last_validate_time', 'validatedAt', 'lastCheckedAt']),
    useCount: readOptionalNumber(record, ['useCount', 'use_count', 'usedCount', 'usageCount']) ?? 0,
    createTime: readString(record, ['createTime', 'createdAt', 'create_time']),
    updateTime: readString(record, ['updateTime', 'updatedAt', 'update_time']),
  };
}

function readStatusAsValid(record: Record<string, unknown>): boolean {
  const status = readOptionalString(record, ['status', 'state']);
  if (!status) return false;
  return ['valid', 'active', 'enabled', 'success', 'ok', '1', 'true'].includes(status.trim().toLowerCase());
}

function normalizeBooleanResult(raw: unknown): boolean {
  const value = parseJsonValue(raw);
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    if (['true', '1', 'yes', 'y', 'valid', 'success', 'ok'].includes(normalized)) return true;
    if (['false', '0', 'no', 'n', 'invalid', 'failed', 'error'].includes(normalized)) return false;
  }
  const record = asRecord(value);
  return readOptionalBoolean(record, ['valid', 'isValid', 'success', 'ok', 'data', 'result']) ?? readStatusAsValid(record);
}

function normalizeQrLoginStart(raw: unknown): DouyinQrLoginStartResultVO {
  const record = asRecord(raw);
  return {
    sessionId: readString(record, ['sessionId', 'session_id', 'id', 'token']),
    qrImageBase64: readString(record, ['qrImageBase64', 'qr_image_base64', 'qrImage', 'imageBase64', 'screenshotBase64', 'base64']),
    message: readOptionalString(record, ['message', 'msg', 'hint']),
  };
}

function normalizeQrLoginPoll(raw: unknown): DouyinQrLoginPollResultVO {
  const record = asRecord(raw);
  const rawStatus = readString(record, ['status', 'state'], 'waiting').trim().toLowerCase();
  const status = (['waiting', 'success', 'expired', 'error'].includes(rawStatus) ? rawStatus : 'waiting') as DouyinQrLoginPollResultVO['status'];
  return {
    status,
    cookieValue: readOptionalString(record, ['cookieValue', 'cookie_value', 'value', 'cookie', 'header', 'cookieHeader']),
    message: readOptionalString(record, ['message', 'msg', 'hint']),
  };
}

function compactPayload<T extends Record<string, unknown>>(payload: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) => value !== undefined && value !== '')
  ) as Partial<T>;
}

function normalizeAccount(raw: unknown): BenchmarkAccountVO {
  const record = asRecord(raw);
  const fanCount = readOptionalNumber(record, ['fanCount', 'followerCount', 'fansCount']);
  return {
    ...(record as Partial<BenchmarkAccountVO>),
    id: readNumber(record, ['id', 'accountId'], 0),
    ownerId: readOptionalNumber(record, ['ownerId', 'userId']),
    accountName: readString(record, ['accountName', 'nickname', 'name']),
    platform: readOptionalString(record, ['platform']) ?? 'douyin',
    accountUrl: readString(record, ['accountUrl', 'profileUrl', 'url']),
    secUid: readString(record, ['secUid', 'sec_uid']),
    douyinId: readOptionalString(record, ['douyinId', 'uniqueId']),
    category: readOptionalString(record, ['category', 'industry', 'tags']),
    fanCount,
    followerCount: fanCount,
    videoCount: readOptionalNumber(record, ['videoCount', 'awemeCount']),
    avgViewCount: readOptionalNumber(record, ['avgViewCount', 'averageViewCount']),
    avgLikeCount: readOptionalNumber(record, ['avgLikeCount', 'averageLikeCount']),
    notes: readOptionalString(record, ['notes', 'description']),
    isActive: readOptionalBoolean(record, ['isActive', 'active']),
    avatarUrl: readOptionalString(record, ['avatarUrl', 'avatar', 'avatarThumb']),
    likeCount: readOptionalNumber(record, ['likeCount', 'totalLikeCount']),
    description: readOptionalString(record, ['description', 'notes']),
    tags: readOptionalString(record, ['tags', 'category']),
    lastCollectTime: normalizeDateString(record, ['lastCollectTime']),
    createTime: normalizeDateString(record, ['createTime', 'createdAt']),
    updateTime: normalizeDateString(record, ['updateTime', 'updatedAt']),
  };
}

function normalizeAccountSearchParams(params: BenchmarkAccountSearchVO): Record<string, unknown> {
  return compactPayload({
    ...params,
    minFanCount: params.minFanCount ?? params.minFollowerCount,
    minFollowerCount: undefined,
  });
}

function normalizeAccountSavePayload(data: BenchmarkAccountSaveVO): Record<string, unknown> {
  return compactPayload({
    ...data,
    platform: data.platform ?? 'douyin',
    fanCount: data.fanCount ?? data.followerCount,
    followerCount: undefined,
    avatarUrl: undefined,
    likeCount: undefined,
    description: undefined,
    tags: undefined,
  });
}

function normalizeKeywordSearchPayload(params: SearchAccountByKeywordVO): Record<string, unknown> {
  return compactPayload({
    ...params,
    minFanCount: params.minFanCount ?? params.minFollowerCount,
    minFollowerCount: undefined,
  });
}

function normalizeVideo(raw: unknown): BenchmarkVideoVO {
  const record = asRecord(raw);
  const localVideoPath = readOptionalString(record, ['localVideoPath', 'localPath']);
  const bosVideoUrl = readOptionalString(record, ['bosVideoUrl', 'bosUrl']);
  return {
    ...(record as Partial<BenchmarkVideoVO>),
    id: readNumber(record, ['id', 'videoPk'], 0),
    ownerId: readOptionalNumber(record, ['ownerId', 'userId']),
    benchmarkAccountId: readNumber(record, ['benchmarkAccountId', 'accountId'], 0),
    videoId: readString(record, ['videoId', 'awemeId']),
    videoUrl: readString(record, ['videoUrl', 'awemeUrl', 'url']),
    title: readOptionalString(record, ['title']),
    description: readOptionalString(record, ['description', 'desc']),
    coverUrl: readOptionalString(record, ['coverUrl', 'cover']),
    duration: readOptionalNumber(record, ['duration']),
    viewCount: readOptionalNumber(record, ['viewCount', 'playCount']),
    likeCount: readOptionalNumber(record, ['likeCount', 'diggCount']),
    commentCount: readOptionalNumber(record, ['commentCount']),
    shareCount: readOptionalNumber(record, ['shareCount']),
    favoriteCount: readOptionalNumber(record, ['favoriteCount', 'collectCount']),
    collectCount: readOptionalNumber(record, ['collectCount', 'favoriteCount']),
    publishTime: normalizeDateString(record, ['publishTime']),
    isQualified: readOptionalBoolean(record, ['isQualified', 'qualified']),
    localVideoPath,
    bosVideoUrl,
    localPath: localVideoPath,
    bosUrl: bosVideoUrl,
    analysisStatus: (readOptionalString(record, ['analysisStatus']) ?? 'pending') as BenchmarkVideoVO['analysisStatus'],
    analysisErrorMsg: readOptionalString(record, ['analysisErrorMsg', 'errorMsg']),
    createTime: normalizeDateString(record, ['createTime', 'createdAt']),
    updateTime: normalizeDateString(record, ['updateTime', 'updatedAt']),
  };
}

function normalizeAnalysis(raw: unknown): BenchmarkAnalysisVO {
  const record = asRecord(raw);
  return {
    ...(record as Partial<BenchmarkAnalysisVO>),
    id: readNumber(record, ['id', 'analysisId'], 0),
    ownerId: readOptionalNumber(record, ['ownerId', 'userId']),
    benchmarkVideoId: readNumber(record, ['benchmarkVideoId', 'videoId'], 0),
    transcriptText: readOptionalString(record, ['transcriptText']),
    ocrText: readOptionalString(record, ['ocrText']),
    apiDescription: readOptionalString(record, ['apiDescription']),
    mergedContent: readOptionalString(record, ['mergedContent']),
    keyFramesJson: normalizeJsonField(record, ['keyFramesJson', 'key_frames_json', 'keyFrames', 'frames']),
    sceneDescription: readOptionalString(record, ['sceneDescription']),
    sceneCount: readOptionalNumber(record, ['sceneCount']),
    creativeType: readOptionalString(record, ['creativeType']),
    hookStrategy: readOptionalString(record, ['hookStrategy']),
    contentStructure: readOptionalString(record, ['contentStructure']),
    emotionalCurve: readOptionalString(record, ['emotionalCurve']),
    pacingAnalysis: readOptionalString(record, ['pacingAnalysis']),
    viralFactors: readOptionalString(record, ['viralFactors']),
    strengths: readOptionalString(record, ['strengths']),
    weaknesses: readOptionalString(record, ['weaknesses']),
    replicableElements: readOptionalString(record, ['replicableElements']),
    aiSummary: readOptionalString(record, ['aiSummary', 'summary']),
    scriptBreakdown: readOptionalString(record, ['scriptBreakdown']),
    improvementSuggestions: readOptionalString(record, ['improvementSuggestions']),
    targetAudience: readOptionalString(record, ['targetAudience']),
    comparisonReport: readOptionalString(record, ['comparisonReport']),
    differentiationPoints: readOptionalString(record, ['differentiationPoints']),
    aiModelUsed: readOptionalString(record, ['aiModelUsed']),
    tokensUsed: readOptionalNumber(record, ['tokensUsed']),
    analysisDurationMs: readOptionalNumber(record, ['analysisDurationMs']),
    createTime: normalizeDateString(record, ['createTime', 'createdAt']),
    updateTime: normalizeDateString(record, ['updateTime', 'updatedAt']),
  };
}

function normalizeQualityScript(raw: unknown): BenchmarkQualityScriptVO {
  const record = asRecord(raw);
  return {
    ...(record as Partial<BenchmarkQualityScriptVO>),
    id: readNumber(record, ['id', 'scriptId'], 0),
    ownerId: readNumber(record, ['ownerId', 'userId'], 0),
    videoId: readNumber(record, ['videoId', 'benchmarkVideoId'], 0),
    analysisId: readNumber(record, ['analysisId', 'benchmarkAnalysisId'], 0),
    scriptContent: readString(record, ['scriptContent', 'content', 'text']),
    scriptType: readOptionalString(record, ['scriptType', 'type']),
    industry: readOptionalString(record, ['industry', 'category']),
    sceneType: readOptionalString(record, ['sceneType', 'scene']),
    qualityScore: readOptionalNumber(record, ['qualityScore', 'score']) ?? 0,
    engagementRate: readOptionalNumber(record, ['engagementRate', 'interactionRate']),
    viralScore: readOptionalNumber(record, ['viralScore', 'spreadScore']),
    completionRate: readOptionalNumber(record, ['completionRate', 'finishRate']),
    aiRating: readOptionalNumber(record, ['aiRating', 'aiScore']),
    likesCount: readOptionalNumber(record, ['likesCount', 'likeCount']),
    commentsCount: readOptionalNumber(record, ['commentsCount', 'commentCount']),
    sharesCount: readOptionalNumber(record, ['sharesCount', 'shareCount']),
    collectionsCount: readOptionalNumber(record, ['collectionsCount', 'collectCount']),
    viewsCount: readOptionalNumber(record, ['viewsCount', 'viewCount', 'playCount']),
    videoDuration: readOptionalNumber(record, ['videoDuration', 'duration']),
    keyFeatures: readOptionalString(record, ['keyFeatures']),
    creativeElements: readOptionalString(record, ['creativeElements']),
    hookStrategy: readOptionalString(record, ['hookStrategy']),
    contentStructure: readOptionalString(record, ['contentStructure']),
    embeddingVector: readOptionalString(record, ['embeddingVector']),
    referenceCount: readOptionalNumber(record, ['referenceCount']) ?? 0,
    lastReferencedAt: readOptionalString(record, ['lastReferencedAt']),
    createTime: readString(record, ['createTime', 'createdAt']),
    updateTime: readString(record, ['updateTime', 'updatedAt']),
  };
}

function normalizeQualityScore(raw: unknown): number {
  const value = parseJsonValue(raw);
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string') {
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
  }
  if (isRecord(value)) {
    return readNumber(value, ['score', 'qualityScore', 'value', 'data'], 0);
  }
  return 0;
}

function normalizeScriptSimilarity(raw: unknown): BenchmarkScriptSimilarityVO {
  const record = asRecord(raw);
  return {
    ...(record as Partial<BenchmarkScriptSimilarityVO>),
    scriptId: readNumber(record, ['scriptId', 'id', 'qualityScriptId'], 0),
    videoId: readNumber(record, ['videoId', 'benchmarkVideoId'], 0),
    scriptContent: readString(record, ['scriptContent', 'content', 'text']),
    scriptType: readOptionalString(record, ['scriptType', 'type']),
    industry: readOptionalString(record, ['industry', 'category']),
    sceneType: readOptionalString(record, ['sceneType', 'scene']),
    qualityScore: readOptionalNumber(record, ['qualityScore', 'scriptQualityScore', 'score']) ?? 0,
    similarityScore: readOptionalNumber(record, ['similarityScore', 'similarity', 'matchScore']),
    engagementRate: readOptionalNumber(record, ['engagementRate', 'interactionRate']),
    viralScore: readOptionalNumber(record, ['viralScore', 'spreadScore']),
    likesCount: readOptionalNumber(record, ['likesCount', 'likeCount']),
    commentsCount: readOptionalNumber(record, ['commentsCount', 'commentCount']),
    sharesCount: readOptionalNumber(record, ['sharesCount', 'shareCount']),
    collectionsCount: readOptionalNumber(record, ['collectionsCount', 'collectCount']),
    viewsCount: readOptionalNumber(record, ['viewsCount', 'viewCount', 'playCount']),
  };
}

function normalizeSimilarityResults(raw: unknown): BenchmarkScriptSimilarityVO[] {
  return normalizeResponseArray<unknown>(raw).map(normalizeScriptSimilarity);
}

function normalizeNumberArray(raw: unknown): number[] {
  return normalizeResponseArray<unknown>(raw)
    .map(value => Number(value))
    .filter(value => Number.isFinite(value));
}

// ==================== 对标账号 API ====================

export const benchmarkAccountApi = {
  /**
   * 分页查询账号
   */
  list: (params: BenchmarkAccountSearchVO) =>
    request.post<unknown>('/benchmark/account/list', normalizeAccountSearchParams(params))
      .then(raw => normalizeResponsePage<unknown, BenchmarkAccountVO>(raw, normalizeAccount, params.page ?? 0, params.rows ?? 20)),

  /**
   * 获取账号详情
   */
  get: (id: number) =>
    request.post<unknown>('/benchmark/account/get', id).then(normalizeAccount),

  /**
   * 保存账号
   */
  save: (data: BenchmarkAccountSaveVO) =>
    request.post<unknown>('/benchmark/account/save', normalizeAccountSavePayload(data)).then(normalizeAccount),

  /**
   * 删除账号
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/account/delete', id),

  /**
   * 按关键词搜索账号
   */
  searchByKeyword: (params: SearchAccountByKeywordVO) =>
    request.post<unknown>('/benchmark/account/search-by-keyword', normalizeKeywordSearchPayload(params))
      .then(raw => normalizeResponseArray<unknown>(raw).map(normalizeAccount)),

  /**
   * 按URL分析账号
   */
  analyzeByUrl: (params: AnalyzeAccountByUrlVO) =>
    request.post<unknown>('/benchmark/account/analyze-by-url', compactPayload(params as unknown as Record<string, unknown>))
      .then(normalizeAccount),
};

// ==================== 对标视频 API ====================

export const benchmarkVideoApi = {
  /**
   * 分页查询视频
   */
  list: (params: BenchmarkVideoSearchVO) =>
    request.post<unknown>('/benchmark/video/list', compactPayload(params as unknown as Record<string, unknown>))
      .then(raw => normalizeResponsePage<unknown, BenchmarkVideoVO>(raw, normalizeVideo, params.page ?? 0, params.rows ?? 20)),

  /**
   * 获取视频详情
   */
  get: (id: number) =>
    request.post<unknown>('/benchmark/video/get', id).then(normalizeVideo),

  /**
   * 保存视频
   */
  save: (data: BenchmarkVideoSaveVO) =>
    request.post<unknown>('/benchmark/video/save', compactPayload(data as unknown as Record<string, unknown>)).then(normalizeVideo),

  /**
   * 删除视频
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/video/delete', id),

  /**
   * 采集账号视频
   */
  collect: (params: CollectAccountVideosVO) =>
    request.post<unknown>('/benchmark/video/collect', compactPayload(params as unknown as Record<string, unknown>))
      .then(raw => normalizeResponseArray<unknown>(raw).map(normalizeVideo)),
};

// ==================== 深度分析 API ====================

export const benchmarkAnalysisApi = {
  /**
   * 分析单个视频
   */
  analyze: (params: AnalyzeVideoVO) =>
    request.post<unknown>('/benchmark/analysis/analyze', compactPayload(params as unknown as Record<string, unknown>)).then(normalizeAnalysis),

  /**
   * 批量分析视频
   */
  batchAnalyze: (params: BatchAnalyzeVideosVO) =>
    request.post<number>('/benchmark/analysis/batch-analyze', params),

  /**
   * 获取分析结果
   */
  getByVideo: (videoId: number) =>
    request.post<unknown>('/benchmark/analysis/get-by-video', videoId).then(normalizeAnalysis),
};

// ==================== Cookie 管理 API ====================

export const douyinCookieApi = {
  /**
   * 分页查询Cookie
   */
  list: (params: DouyinCookieSearchVO) =>
    request.post<unknown>('/benchmark/cookie/list', params)
      .then(raw => normalizeResponsePage<unknown, DouyinCookieVO>(raw, normalizeCookie, params.page ?? 0, params.rows ?? 20)),

  /**
   * 获取Cookie详情
   */
  get: (id: number) =>
    request.post<unknown>('/benchmark/cookie/get', id).then(normalizeCookie),

  /**
   * 保存Cookie
   */
  save: (data: DouyinCookieSaveVO) =>
    request.post<unknown>('/benchmark/cookie/save', data).then(normalizeCookie),

  /**
   * 删除Cookie
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/cookie/delete', id),

  /**
   * 验证Cookie
   */
  validate: (params: ValidateCookieVO) =>
    request.post<unknown>('/benchmark/cookie/validate', params).then(normalizeBooleanResult),

  /** 服务端 Playwright 打开抖音页并返回整页截图（含二维码） */
  qrLoginStart: () =>
    request.post<unknown>('/benchmark/cookie/qr-login/start', {}).then(normalizeQrLoginStart),

  qrLoginPoll: (sessionId: string) =>
    request.post<unknown>('/benchmark/cookie/qr-login/poll', { sessionId }).then(normalizeQrLoginPoll),

  qrLoginCancel: (sessionId: string) =>
    request.post<void>('/benchmark/cookie/qr-login/cancel', { sessionId }),
};

// ==================== 质量脚本知识库 API ====================

export const benchmarkQualityScriptApi = {
  /**
   * 分页查询质量脚本
   */
  search: (params: BenchmarkQualityScriptSearchVO) =>
    request.post<unknown>('/benchmark/quality-script/search', params)
      .then(raw => normalizeResponsePage<unknown, BenchmarkQualityScriptVO>(raw, normalizeQualityScript, params.page ?? 0, params.rows ?? 20)),

  /**
   * 获取质量脚本详情
   */
  get: (id: number) =>
    request.post<unknown>('/benchmark/quality-script/get', { id }).then(normalizeQualityScript),

  /**
   * 保存质量脚本
   */
  save: (data: BenchmarkQualityScriptSaveVO) =>
    request.post<unknown>('/benchmark/quality-script/save', data).then(normalizeQualityScript),

  /**
   * 删除质量脚本
   */
  delete: (id: number) =>
    request.post<void>('/benchmark/quality-script/delete', { id }),

  /**
   * 根据视频ID查询质量脚本
   */
  getByVideo: (videoId: number) =>
    request.post<unknown>('/benchmark/quality-script/get-by-video', { videoId }).then(normalizeQualityScript),

  /**
   * 根据分析ID查询质量脚本
   */
  getByAnalysis: (analysisId: number) =>
    request.post<unknown>('/benchmark/quality-script/get-by-analysis', { analysisId }).then(normalizeQualityScript),

  /**
   * 获取指定行业的高质量脚本
   */
  getHighQualityByIndustry: (industry: string, minScore: number) =>
    request.post<unknown>('/benchmark/quality-script/get-high-quality-by-industry', {
      industry,
      minScore,
    }).then(raw => normalizeResponseArray<unknown>(raw).map(normalizeQualityScript)),

  /**
   * 获取指定场景的高质量脚本
   */
  getHighQualityByScene: (sceneType: string, minScore: number) =>
    request.post<unknown>('/benchmark/quality-script/get-high-quality-by-scene', {
      sceneType,
      minScore,
    }).then(raw => normalizeResponseArray<unknown>(raw).map(normalizeQualityScript)),

  /**
   * 获取互动率最高的脚本
   */
  getTopEngagement: (limit: number = 10) =>
    request.post<unknown>('/benchmark/quality-script/get-top-engagement', { limit })
      .then(raw => normalizeResponseArray<unknown>(raw).map(normalizeQualityScript)),

  /**
   * 获取传播力最高的脚本
   */
  getTopViral: (limit: number = 10) =>
    request.post<unknown>('/benchmark/quality-script/get-top-viral', { limit })
      .then(raw => normalizeResponseArray<unknown>(raw).map(normalizeQualityScript)),

  /**
   * 计算质量评分
   */
  calculateQualityScore: (data: BenchmarkQualityScriptSaveVO) =>
    request.post<unknown>('/benchmark/quality-script/calculate-quality-score', data).then(normalizeQualityScore),
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
    request.post<unknown>('/benchmark/script-similarity/find-similar', params).then(normalizeSimilarityResults),

  /**
   * 根据文本查找相似脚本
   */
  findSimilarByText: (params: FindSimilarByTextVO) =>
    request.post<unknown>('/benchmark/script-similarity/find-similar-by-text', params).then(normalizeSimilarityResults),

  /**
   * 计算两个脚本的相似度
   */
  calculateSimilarity: (scriptId1: number, scriptId2: number) =>
    request.post<unknown>('/benchmark/script-similarity/calculate-similarity', { scriptId1, scriptId2 }).then(normalizeQualityScore),

  /**
   * 获取未生成向量的脚本
   */
  getUnembeddedScripts: (limit: number = 100) =>
    request.post<unknown>('/benchmark/script-similarity/get-unembedded-scripts', { limit }).then(normalizeNumberArray),

  /**
   * 获取未索引的脚本
   */
  getUnindexedScripts: (limit: number = 100) =>
    request.post<unknown>('/benchmark/script-similarity/get-unindexed-scripts', { limit }).then(normalizeNumberArray),
};

// ==================== 脚本推荐 API ====================

export const benchmarkScriptRecommendationApi = {
  /**
   * 根据需求推荐脚本
   */
  recommendByRequirement: (params: RecommendByRequirementVO) =>
    request.post<unknown>('/benchmark/script-recommendation/recommend-by-requirement', params).then(normalizeSimilarityResults),

  /**
   * 根据行业场景推荐脚本
   */
  recommendByIndustryScene: (params: RecommendByIndustrySceneVO) =>
    request.post<unknown>('/benchmark/script-recommendation/recommend-by-industry-scene', params).then(normalizeSimilarityResults),

  /**
   * 根据脚本类型推荐脚本
   */
  recommendByScriptType: (params: RecommendByScriptTypeVO) =>
    request.post<unknown>('/benchmark/script-recommendation/recommend-by-script-type', params).then(normalizeSimilarityResults),

  /**
   * 智能推荐
   */
  smartRecommend: (params: SmartRecommendVO) =>
    request.post<unknown>('/benchmark/script-recommendation/smart-recommend', params).then(normalizeSimilarityResults),

  /**
   * 获取热门脚本
   */
  getPopularScripts: (topK: number = 10) =>
    request.post<unknown>('/benchmark/script-recommendation/get-popular-scripts', { topK }).then(normalizeSimilarityResults),

  /**
   * 获取最新高质量脚本
   */
  getLatestQualityScripts: (topK: number = 10, minQualityScore?: number) =>
    request.post<unknown>('/benchmark/script-recommendation/get-latest-quality-scripts', {
      topK,
      minQualityScore,
    }).then(normalizeSimilarityResults),

  /**
   * 推荐改进脚本
   */
  recommendImprovementScripts: (params: RecommendImprovementVO) =>
    request.post<unknown>('/benchmark/script-recommendation/recommend-improvement-scripts', params).then(normalizeSimilarityResults),
};
