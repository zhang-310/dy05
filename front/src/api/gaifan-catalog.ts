import request from '@/utils/request'

export type GaifanProductSummary = {
  code: string
  name: string
  stage?: string
  enabled?: boolean
  featureCount?: number
}

export async function listGaifanProducts(): Promise<GaifanProductSummary[]> {
  const res = await request.post<GaifanProductSummary[]>('/product/list', {})
  return Array.isArray(res) ? res : []
}

export type GaifanEntitlementDecision = {
  granted: boolean
  productCode?: string
  featureCode?: string
  reason?: string | null
  quotaRemaining?: number
}

export async function checkGaifanEntitlement(productCode: string, featureCode: string) {
  return request.post<GaifanEntitlementDecision>('/product/check', {
    productCode,
    featureCode,
  })
}

/** 可售产品 → 管理端入口（按 productCode 显隐导航） */
export const GAIFAN_PRODUCT_ROUTES: Record<string, string> = {
  'douyin-ops': '/admin/gaifan/douyin-ops-commander',
  'video-insight': '/admin/shortvideo/insights',
  'shortvideo-maker': '/admin/shortvideo/projects',
  'digital-human': '/admin/ai/digital-human',
  'photo-avatar-video': '/talent/shortvideo/photo-avatar',
  'drama-ai': '/admin/shortvideo/drama',
  'knowledge-base': '/admin/ai/knowledge',
}
