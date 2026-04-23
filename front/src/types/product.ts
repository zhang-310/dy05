/**
 * 商品话术版本 - TypeScript 类型定义
 * @author Claude Code
 * @since 2026-03-06
 */

/** 商品库搜索结果在直播选品面板中的最小字段集 */
export interface ProductLibraryItemVO {
  id: number
  productName: string
  price?: number
  mainImage?: string
  imageUrl?: string
  category?: string
  productCategory?: string
  [key: string]: unknown
}

// 基础返回 VO
export interface ProductScriptVersionVO {
  id: number;
  productId: number;
  scriptId?: number;
  versionNumber: number;
  content: string;
  style?: string;
  effectivenessScore: number;
  usageCount: number;
  conversionRate: number;
  likesCount: number;
  commentsCount: number;
  isActive: boolean;
  isRecommended: boolean;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

// 查询参数 VO
export interface ProductScriptVersionSearchVO {
  productId: number;
  style?: string;
  isActive?: boolean;
  page: number;
  rows: number;
  sortName?: string;
  sortOrder?: 'asc' | 'desc';
}

// 保存参数 VO
export interface ProductScriptVersionSaveVO {
  id?: number;
  productId: number;
  content: string;
  style?: string;
  effectivenessScore?: number;
  conversionRate?: number;
  remark?: string;
}

// 推荐版本 VO
export interface ProductScriptRecommendVO {
  versions: ProductScriptRecommendItem[];
  scores: number[];
}

export interface ProductScriptRecommendItem {
  id: number;
  versionNumber: number;
  style?: string;
  effectivenessScore: number;
  usageCount: number;
  recommendScore: number;
}

// 快照 VO
export interface ProductScriptSnapshotVO {
  id: number;
  liveSessionId: number;
  productScriptVersionId: number;
  contentSnapshot: string;
  referencedAt: string;
}

// 页面参数类型
export interface PageParams {
  page: number;
  rows: number;
  sortName?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface SearchParams extends PageParams {
  keyword?: string;
  style?: string;
  minScore?: number;
}

// API 响应类型
export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  traceId?: string;
  timestamp?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}
