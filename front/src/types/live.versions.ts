/**
 * 直播话术版本管理 - TypeScript 类型定义
 */

/**
 * 直播话术版本 VO
 */
export interface LiveScriptVersionVO {
  id: number;
  scriptId: number;
  sessionId: number;
  versionNo: number;
  versionLabel: string;
  scriptContent: string;
  scriptType: string;
  remark?: string;
  versionStatus: 'draft' | 'active' | 'archived';
  effectivenessScore?: number;
  likedCount: number;
  usageCount: number;
  lastUsedTime?: string;
  ownerId: number;
  isRecommended: number;
  recommendReason?: string;
  recommendScore?: number;
  basedOnVersionId?: number;
  changeSummary?: string;
  createTime: string;
  updateTime: string;
}

/**
 * 直播话术版本保存 VO
 */
export interface LiveScriptVersionSaveVO {
  scriptId?: number;
  versionNo?: number;
  versionLabel?: string;
  scriptContent: string;
  scriptType?: string;
  remark?: string;
  versionStatus?: 'draft' | 'active' | 'archived';
  effectivenessScore?: number;
  basedOnVersionId?: number;
  recommendReason?: string;
  isRecommended?: number;
}

/**
 * 直播话术版本搜索 VO
 */
export interface LiveScriptVersionSearchVO {
  scriptId?: number;
  sessionId?: number;
  versionStatus?: 'draft' | 'active' | 'archived';
  effectivenessScoreMin?: number;
  effectivenessScoreMax?: number;
  isRecommended?: number;
  ownerId?: number;
  scriptType?: string;
  page?: number;
  rows?: number;
  sortName?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * 版本差异展示 VO
 */
export interface VersionDiffVO {
  oldVersionId: number;
  newVersionId: number;
  oldVersionNo: number;
  newVersionNo: number;
  oldContent: string;
  newContent: string;
  changedFields: string[];
  diffHtml?: string;
  similarity: number;
  recommendNew: boolean;
  recommendation: string;
}

/**
 * 版本对比请求 VO
 */
export interface VersionDiffRequestVO {
  oldVersionId: number;
  newVersionId: number;
}

/**
 * 设置推荐版本请求 VO
 */
export interface SetRecommendedVO {
  versionId: number;
  recommendReason?: string;
}

/**
 * 更新版本状态请求 VO
 */
export interface UpdateVersionStatusVO {
  versionId: number;
  versionStatus: 'draft' | 'active' | 'archived';
}

/**
 * 用户场次版本查询请求 VO
 */
export interface UserSessionVersionsVO {
  sessionId: number;
  ownerId: number;
}

/**
 * 基于现有版本创建新版本请求 VO
 */
export interface CreateFromExistingVO {
  sourceVersionId: number;
  versionData: LiveScriptVersionSaveVO;
}

/**
 * 分页结果
 */
export interface PageResultVO<T> {
  total: number;
  list: T[];
  pageNum: number;
  pageSize: number;
}
