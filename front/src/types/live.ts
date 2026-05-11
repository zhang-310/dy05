/**
 * 直播模块 - TypeScript 类型定义
 * 与后端 module/live/ VO 一一对应
 */

import type { BasicQuery } from './common'

// ─── 直播场次 ──────────────────────────────────────────────────────────────

/** 场次查询参数（对应后端 LiveSessionSearchVO） */
export interface LiveSessionSearchVO extends BasicQuery {
  userId?: number
  userIds?: number[]
  accountId?: number
  status?: number
  keyword?: string
  liveTitle?: string
  startTimeFrom?: string
  startTimeTo?: string
}

/** 场次保存参数（对应后端 LiveSessionSaveVO） */
export interface LiveSessionSaveVO {
  id?: number
  userId?: number
  accountId?: number
  personaId?: number
  liveTitle: string
  sessionCover?: string
  scriptStyle?: string
  liveDescription?: string
  liveUrl?: string
  scheduledTime?: string
  scheduledEndTime?: string
  status?: number
  sessionType?: string
  /** 直播形式 LF-01：pure_entertainment | content_commerce | content_led | organic_micro_paid | heavy_paid_category | multi_sku_speed | warehouse | single_sku */
  liveFormat?: string
  /** 场次槽位结构模板 ID（M-1/M-2） */
  templateId?: number
  /** M-3 none|hourly_block|manual */
  slotRotationMode?: string
  /** M-4 热度重排 */
  heatReorderEnabled?: boolean
  /** M-6 模板 A/B 标签 */
  templateAbTag?: string
}

/** 场次返回值（对应后端 LiveSessionVO） */
export interface LiveSessionVO {
  id: number
  userId: number
  accountId?: number
  liveTitle: string
  liveDescription?: string
  scheduledTime?: string
  startTime?: string
  endTime?: string
  liveUrl?: string
  viewers: number
  likes: number
  status: number
  recordingUrl?: string
  recordingDuration?: number
  createTime: string
  updateTime: string
  productCount?: number
  scriptCount?: number
  /** 已结束场次复盘 GMV（live_session_data.total_revenue） */
  totalRevenue?: number
  /** 场次累计 GMV（live_product.revenue 汇总），与实时 SSE cumulativeGmv 口径一致；直播中用于展示进行中累计 */
  cumulativeGmv?: number | string
  sessionType?: string
  liveFormat?: string
  scriptStyle?: string
  templateId?: number
  slotRotationMode?: string
  heatReorderEnabled?: boolean
  templateAbTag?: string
  [key: string]: unknown
}

/** 场次数据概览（对应后端 LiveSessionOverviewVO） */
export interface LiveSessionOverviewVO {
  session: LiveSessionVO
  sessionData?: LiveSessionDataVO
  productDataList?: LiveProductDataVO[]
  scriptTop5?: LiveScriptVO[]
}

/** 场次数据统计（对应后端 LiveSessionDataVO） */
export interface LiveSessionDataVO {
  id: number
  sessionId: number
  totalRevenue?: number
  totalOrders?: number
  avgOrderValue?: number
  conversionRate?: number
  peakViewers?: number
  avgViewers?: number
  totalInteractions?: number
  createTime?: string
  updateTime?: string
}

/** 产品数据统计（对应后端 LiveProductDataVO） */
export interface LiveProductDataVO {
  id: number
  sessionId: number
  productId: number
  productName?: string
  saleQuantity?: number
  revenue?: number
  conversionRate?: number
  createTime?: string
}

/** 开播准备清单（对应后端 LiveReadinessVO） */
export interface LiveReadinessVO {
  ready: boolean
  checks: {
    products: LiveReadinessCheckItem
    persona: LiveReadinessCheckItem
    scripts: LiveReadinessCheckItem
    compliance: LiveReadinessCheckItem
  }
  [key: string]: unknown
}

/** 开播准备检查项 */
export interface LiveReadinessCheckItem {
  passed: boolean
  count?: number
  personaName?: string
  message?: string
}

// ─── 竞品商业洞察 C-2/C-3/C-5 ───────────────────────────────────────────────

export interface LiveCompetitiveInsightSearchVO extends BasicQuery {
  keyword?: string
  sessionId?: number
}

export interface LiveCompetitiveInsightSaveVO {
  id?: number
  sessionId?: number
  competitorLabel: string
  productPrice?: number
  marketSharePercent?: number
  gmvEstimate?: number
  winLossNotes?: string
}

export interface LiveCompetitiveInsightVO {
  id: number
  ownerId: number
  sessionId?: number
  competitorLabel: string
  productPrice?: number
  marketSharePercent?: number
  gmvEstimate?: number
  winLossNotes?: string
  createTime?: string
  updateTime?: string
}

// ─── 直播话术 ──────────────────────────────────────────────────────────────

/** 话术查询参数（对应后端 LiveScriptSearchVO） */
export interface LiveScriptSearchVO extends BasicQuery {
  sessionId?: number
  sessionIds?: number[]
  executed?: number
}

/** 话术保存参数（对应后端 LiveScriptSaveVO，更新时 sessionId 可省略） */
export interface LiveScriptSaveVO {
  id?: number
  sessionId?: number
  scriptContent?: string
  scriptType?: string
  style?: string
  requirement?: string
  durationLimitSec?: number
  productId?: number
  sequenceNo?: number
  executionTime?: number
  executed?: number
  aiGenerated?: number
  referencedScriptId?: number
  referencedScriptSnapshot?: string
  promptTemplateId?: number
  platform?: string
  /** R-4 提词备注（不写入正文） */
  presenterNotes?: string
}

/** 话术返回值（对应后端 LiveScriptVO） */
export interface LiveScriptVO {
  id: number
  sessionId: number
  scriptContent: string
  scriptType?: string
  style?: string
  requirement?: string
  durationLimitSec?: number
  productId?: number
  aiGenerated?: boolean           // P0-2: 改为 boolean（后端已转换）
  aiCallLogId?: number
  generationStatus?: string
  violationChecked?: boolean      // P0-2: 改为 boolean（后端已转换）
  violationResult?: string
  viewerDelta?: number
  interactionDelta?: number
  conversionDelta?: number
  effectivenessScore?: number
  sequenceNo?: number
  executionTime?: number
  executed?: number
  referencedScriptId?: number
  referencedScriptSnapshot?: string
  approvalStatus?: string         // P0-2: 改为 string（后端已转换）
  abExperimentId?: number
  abVariantId?: number
  /** 末次 AI 生成的 prompt 指纹（SHA-256 hex）；手工保存改文后清空 */
  generationPromptHash?: string
  actualExecutionTime?: string
  presenterNotes?: string
  // P0-2: 补充缺失字段（已全部补齐）
  userId?: number                 // 所属用户 ID
  aiSuggestion?: string           // AI 建议
  promptTemplateId?: number       // 提示词模板 ID
  createTime: string
  updateTime: string
  [key: string]: unknown
}

// ─── 直播产品 ──────────────────────────────────────────────────────────────

/** 产品查询参数（对应后端 LiveProductSearchVO） */
export interface LiveProductSearchVO extends BasicQuery {
  sessionId?: number
  sessionIds?: number[]
}

/** 产品保存参数（对应后端 LiveProductSaveVO） */
export interface LiveProductSaveVO {
  id?: number
  sessionId: number
  productId: number
  productName?: string
  saleQuantity?: number
  position?: number
  productType?: string
  productScriptId?: number
}

/** 产品返回值（对应后端 LiveProductVO，含 DyProduct 富字段） */
export interface LiveProductVO {
  id: number
  sessionId: number
  productId: number
  productName?: string
  saleQuantity?: number
  revenue?: number
  position?: number
  productType?: string
  createTime: string
  productScriptId?: number
  price?: number
  imageUrl?: string
  productCategory?: string
  description?: string
  aiSellingPoints?: string
  profitMarginPct?: number
  lossPerUnit?: number
  controlStrategy?: string
  [key: string]: unknown
}

// ─── 直播场次槽位模板（M-1/M-2）────────────────────────────────────────────

export interface LiveSessionTemplateSearchVO extends BasicQuery {
  keyword?: string
}

export interface LiveSessionTemplateSaveVO {
  id?: number
  ownerId?: number
  name: string
  code: string
  description?: string
  structureJson: string
}

export interface LiveSessionTemplateVO {
  id: number
  ownerId: number
  name: string
  code: string
  description?: string
  structureJson: string
  createTime: string
  updateTime: string
}

// ─── 直播竞品话术库（C-4）──────────────────────────────────────────────────

export interface LiveCompetitorScriptSearchVO extends BasicQuery {
  keyword?: string
  platform?: string
}

export interface LiveCompetitorScriptSaveVO {
  id?: number
  ownerId?: number
  title: string
  competitorName?: string
  platform?: string
  scriptContent: string
  sourceUrl?: string
  tags?: string
  notes?: string
}

export interface LiveCompetitorScriptVO {
  id: number
  ownerId: number
  title: string
  competitorName?: string
  platform?: string
  scriptContent: string
  sourceUrl?: string
  tags?: string
  notes?: string
  createTime: string
  updateTime: string
}
