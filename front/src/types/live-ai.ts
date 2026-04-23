/**
 * 直播 AI 模块 - TypeScript 类型定义
 * 与后端 module/live/ AI 相关 VO 对齐
 */

import type { BasicQuery } from './common'

// ─── 直播场次数据（live_session_data）───────────────────────────────────

export interface LiveSessionDataVO {
  id: number
  sessionId: number
  totalViewers?: number
  peakViewers?: number
  totalLikes?: number
  totalComments?: number
  totalShares?: number
  totalRevenue?: number | string
  totalOrders?: number
  avgStayTime?: number
  newFollowers?: number
  syncTime?: string
  aiAnalysis?: string
  aiReviewId?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 直播产品数据（live_product_data）──────────────────────────────────

export interface LiveProductDataVO {
  id: number
  sessionId: number
  productId: number
  productName?: string
  impressions?: number
  clicks?: number
  orders?: number
  saleQuantity?: number
  revenue?: number | string
  refundQuantity?: number
  conversionRate?: number
  syncTime?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 直播监控快照（live_monitor）──────────────────────────────────────

export interface LiveMonitorVO {
  id: number
  sessionId: number
  timestamp?: string | number
  viewers?: number
  likes?: number
  comments?: number
  shares?: number
  productImpressions?: number
  totalViewers?: number
  newFollowers?: number
  onlineCount?: number
  gmv?: number | string
  orders?: number
  createTime?: string
  [key: string]: unknown
}

export interface LiveMonitorSearchVO extends BasicQuery {
  sessionId?: number
}

// ─── AI 复盘报告（ai_live_review）────────────────────────────────────

export interface LiveReviewVO {
  id: number
  sessionId: number
  totalViewers?: number
  totalGmv?: number | string
  conversionRate?: number
  peakViewers?: number
  reportContent?: string
  topScripts?: string
  weakPoints?: string
  status?: number
  createTime?: string
  [key: string]: unknown
}

// ─── 话术效果排行 ────────────────────────────────────────────────────

export interface LiveScriptEffectivenessVO {
  scriptId: number
  scriptType?: string
  scriptContent?: string
  viewerDelta?: number
  interactionDelta?: number
  conversionDelta?: number
  effectivenessScore?: number
  sequenceNo?: number
  [key: string]: unknown
}

// ─── 直播场次保存参数 ────────────────────────────────────────────────

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
  liveFormat?: string
  templateId?: number
  slotRotationMode?: string
  heatReorderEnabled?: boolean
  templateAbTag?: string
}

// ─── 话术保存参数 ────────────────────────────────────────────────────

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
  presenterNotes?: string
}
