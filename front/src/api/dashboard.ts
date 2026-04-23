import request from '@/utils/request'

export interface AdminStats {
  // 用户
  totalUsers: number
  activeUsers: number
  todayUsers: number
  // 抖音视频
  totalVideos: number
  publishedVideos: number
  todayVideos: number
  // 直播
  totalLiveSessions: number
  completedSessions: number
  todaySessions: number
  // 短视频
  totalShortVideos: number
  publishedShortVideos: number
  todayShortVideos: number
  // 文案库
  totalCopyItems: number
  approvedCopyItems: number
  todayCopyItems: number
  // AI
  todayAiCalls: number
  todayAiAttempts: number
  aiSuccessRate: number
  // 收入
  todayRevenue: number
}

export interface KpiUnified {
  gmvToday: number
  gmvMom: number           // 环比 %
  gmvYoy: number           // 同比 %
  gmvTarget?: number       // 今日目标
  gmvForecast?: number[]   // 未来3天AI预测（v3.0）
  ordersToday: number
  avgOrderValue: number
  conversionRate: number
  liveSessions: number
  activeSessionCount?: number
  aiCallsToday: number
  aiCallCount?: number
  publishedVideos?: number
  docCount?: number
}

export interface LiveFormatGmv {
  format: string
  gmv: number
  sessions: number
  avgGmv: number
}

export interface ProductGmvSummary {
  productId: number
  productName: string
  totalGmv: number
  totalOrders: number
  avgPrice: number
}

export interface CockpitPreview {
  date: string
  gmv: number
  orders: number
  sessions: number
}

export interface ConversionFunnel {
  exposure: number
  clicks: number
  addToCart: number
  orders: number
  payments: number
}

export const dashboardApi = {
  adminStats: () =>
    request.post<AdminStats>('/dashboard/admin/stats', {}),

  // Alias used by v3.0 prototype (01-dashboard)
  adminDashboard: (params?: { orgId?: number; lookbackDays?: number }) =>
    request.post<AdminStats>('/dashboard/admin/stats', params ?? {}),

  kpiUnified: (lookbackDays = 30) =>
    request.post<KpiUnified>('/dashboard/kpi-unified', { lookbackDays }),

  liveFormatGmv: (lookbackDays = 30) =>
    request.post<LiveFormatGmv[]>('/dashboard/live-format-gmv', { lookbackDays }),

  productGmvSummary: (lookbackDays = 30) =>
    request.post<ProductGmvSummary[]>('/dashboard/product-gmv-summary', { lookbackDays }),

  cockpitPreview: (lookbackDays = 30) =>
    request.post<CockpitPreview[]>('/dashboard/cockpit-preview', { lookbackDays }),

  conversionFunnel: (lookbackDays = 30) =>
    request.post<ConversionFunnel>('/dashboard/conversion-funnel', { lookbackDays }),
}
