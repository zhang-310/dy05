/** POST /dashboard/business 返回（BigDecimal 序列化为 string | number） */
export interface BusinessOngoingSessionVO {
  id: number
  userId: number
  liveTitle?: string
  status?: number
}

export interface BusinessDashboardVO {
  todayGmv?: string | number
  yesterdayGmv?: string | number
  todayLiveSessionCount?: number
  ongoingLiveSessionCount?: number
  ongoingSession?: BusinessOngoingSessionVO | null
}

export function parseMoney(v: unknown): number {
  if (v == null) return 0
  if (typeof v === 'number' && !Number.isNaN(v)) return v
  const n = parseFloat(String(v))
  return Number.isFinite(n) ? n : 0
}

/** 环比百分比（昨日为 0 时返回 null 表示无法计算） */
export function calcGmvDelta(today: number, yesterday: number): { pct: number; up: boolean } | null {
  if (yesterday <= 0) {
    if (today <= 0) return null
    return { pct: 100, up: true }
  }
  const pct = ((today - yesterday) / yesterday) * 100
  return { pct, up: pct >= 0 }
}

export function formatCurrencyYuan(n: number): string {
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

/** POST /dashboard/kpi-unified */
export interface CockpitExportResultVO {
  csv?: string
  filename?: string
  rowCount?: number
}

/** POST /dashboard/live-format-gmv */
export interface LiveFormatGmvRowVO {
  liveFormat?: string
  sessionCount?: number
  totalGmv?: string | number
}

export interface LiveFormatGmvSummaryVO {
  lookbackDays?: number
  since?: string
  rows?: LiveFormatGmvRowVO[]
}

/** POST /dashboard/product-gmv-summary — 已结束场次窗口内按商品汇总 live_product.revenue */
export interface ProductGmvRowVO {
  productId?: number
  productName?: string
  sessionCount?: number
  totalGmv?: string | number
}

export interface ProductGmvSummaryVO {
  lookbackDays?: number
  since?: string
  rows?: ProductGmvRowVO[]
}

/** POST /dashboard/profit-matrix-preview */
export interface ProfitMatrixRowVO {
  liveFormat?: string
  sessionCount?: number
  totalGmv?: string | number
  estimatedMarginRate?: string | number
  isEstimated?: boolean
  note?: string
}

export interface ProfitMatrixPreviewVO {
  lookbackDays?: number
  since?: string
  rows?: ProfitMatrixRowVO[]
}

export interface UnifiedKpiVO {
  content?: {
    shortVideoCount?: number
    productScriptCount?: number
    kbDocumentCount?: number
  }
  traffic?: {
    liveViewerSum?: number
    shortVideoViewSum?: number
  }
  conversion?: {
    saleQuantitySinceToday?: number
    revenueLinesSinceToday?: number
  }
  revenue?: {
    todayGmv?: string | number
    yesterdayGmv?: string | number
    avgOrderValueToday?: string | number
  }
}

/** POST /dashboard/profit-matrix-preview (duplicate removed) */
// ProfitMatrixPreviewVO already defined above

/** POST /dashboard/gmv-trend */
export interface GmvTrendPointVO {
  date: string
  gmv: string | number
}

export interface GmvTrendVO {
  granularity?: string
  lookbackDays?: number
  points?: GmvTrendPointVO[]
}

/** POST /dashboard/cockpit-preview（与 cockpit-export 同源筛选） */
export interface CockpitSessionRowVO {
  sessionId?: number
  liveTitle?: string
  accountId?: number
  userId?: number
  status?: number
  startTime?: string
  endTime?: string
  gmv?: string | number
  productLineCount?: number
}

export interface CockpitPreviewVO {
  rows?: CockpitSessionRowVO[]
  rowCount?: number
}

/** POST /dashboard/conversion-funnel */
export interface ConversionFunnelStepVO {
  name: string
  value: number
  rate: number
}

export interface ConversionFunnelVO {
  lookbackDays?: number
  steps?: ConversionFunnelStepVO[]
}

/** POST /dashboard/gmv-prediction */
export interface GmvPredictionVO {
  predictedGmv?: string | number
  actualGmv?: string | number
  avgGmvPerSession?: string | number
  lookbackDays?: number
  historicalSessionCount?: number
  todaySessionCount?: number
  confidence?: 'high' | 'medium' | 'low'
}
