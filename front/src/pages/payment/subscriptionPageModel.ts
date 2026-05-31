import type { Subscription } from '@/api/payment'

type UnknownRecord = Record<string, unknown>

export interface PaymentPlanView {
  planCode: string
  planName: string
  price: number
  features: string[]
}

export interface UsageQuotaResponse {
  aiCallUsed?: number
  aiCallLimit?: number
  aiCallLast7d?: number
  liveSessionUsed?: number
  liveSessionLimit?: number
  storageUsed?: number
  storageLimit?: number
  kbDocUsed?: number
  kbDocLimit?: number
  [key: string]: unknown
}

export interface UsageQuotaMetrics {
  aiUsed: number
  aiLimit: number
  usedLast7d: number
  dailyRate: number
  aiDaysLeft: number
}

export interface AiQuotaPrediction {
  daysLeft: number
  dateStr: string
  showWarning: boolean
}

export interface SubscriptionProgress {
  totalDays: number
  remainDays: number
  remainPct: number
}

export interface UpgradeSummary {
  currentPlanPrice: number
  targetPlanPrice: number
  targetPlanName: string
  creditAmount: number
  payableAmount: number
}

function parseDateValue(value?: string | null): Date | null {
  if (!value) return null
  const normalized = value.includes(' ') ? value.replace(' ', 'T') : value
  const parsed = new Date(normalized)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function roundMoney(value: number): number {
  return Math.round(value * 100) / 100
}

function normalizePlanCode(plan: UnknownRecord): string {
  return String(plan.planCode ?? plan.plan ?? '')
}

function normalizePlanName(planCode: string, plan: UnknownRecord): string {
  const explicitName = String(plan.planName ?? '')
  if (explicitName) return explicitName
  return ({ free: '免费版', pro: '专业版', enterprise: '企业版' } as Record<string, string>)[planCode] ?? planCode
}

function normalizePlanPrice(planCode: string, plan: UnknownRecord): number {
  const rawPrice = plan.price
  if (typeof rawPrice === 'string') {
    const parsed = Number(rawPrice.replace(/[^\d.]/g, ''))
    if (Number.isFinite(parsed)) return parsed
  }
  const numericPrice = Number(rawPrice)
  if (Number.isFinite(numericPrice)) return numericPrice
  return ({ free: 0, pro: 299, enterprise: 999 } as Record<string, number>)[planCode] ?? 0
}

export function normalizePaymentPlans(plans: unknown): PaymentPlanView[] {
  if (!Array.isArray(plans)) return []
  return plans.map((plan) => {
    const item = plan as UnknownRecord
    const planCode = normalizePlanCode(item)
    return {
      planCode,
      planName: normalizePlanName(planCode, item),
      price: normalizePlanPrice(planCode, item),
      features: Array.isArray(item.features) ? item.features.map((feature) => String(feature)) : [],
    }
  })
}

export function getUsageQuotaMetrics(quota?: UsageQuotaResponse): UsageQuotaMetrics {
  const aiUsed = Number(quota?.aiCallUsed ?? 0)
  const aiLimit = Number(quota?.aiCallLimit ?? 1)
  const usedLast7d = Number(quota?.aiCallLast7d ?? aiUsed * 0.7)
  const dailyRate = usedLast7d / 7
  const remainingQuota = Math.max(0, aiLimit - aiUsed)

  return {
    aiUsed,
    aiLimit,
    usedLast7d,
    dailyRate,
    aiDaysLeft: dailyRate > 0 ? Math.floor(remainingQuota / dailyRate) : 999,
  }
}

export function getAiQuotaPrediction(quota?: UsageQuotaResponse, now = new Date()): AiQuotaPrediction | null {
  if (!quota) return null

  const metrics = getUsageQuotaMetrics(quota)
  const predictDate = new Date(now)
  predictDate.setDate(predictDate.getDate() + metrics.aiDaysLeft)

  return {
    daysLeft: metrics.aiDaysLeft,
    dateStr: predictDate.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' }),
    showWarning: metrics.aiDaysLeft < 30,
  }
}

export function getSubscriptionProgress(
  subscription?: Pick<Subscription, 'createTime' | 'expireTime'> | null,
  now = new Date(),
): SubscriptionProgress {
  const expireDate = parseDateValue(subscription?.expireTime)
  const startDate = parseDateValue(subscription?.createTime)
  const totalDays =
    expireDate && startDate ? Math.ceil((expireDate.getTime() - startDate.getTime()) / 86400000) : 365
  const remainDays =
    expireDate ? Math.max(0, Math.ceil((expireDate.getTime() - now.getTime()) / 86400000)) : 0
  const remainPct = totalDays > 0 ? (remainDays / totalDays) * 100 : 0

  return { totalDays, remainDays, remainPct }
}

export function getUpgradeSummary(
  planList: PaymentPlanView[],
  currentPlanCode?: string | null,
  selectedPlanCode?: string | null,
  remainPct = 0,
): UpgradeSummary {
  const currentPlan = planList.find((plan) => plan.planCode === currentPlanCode)
  const targetPlan = planList.find((plan) => plan.planCode === selectedPlanCode)
  const currentPlanPrice = Number(currentPlan?.price ?? 0)
  const targetPlanPrice = Number(targetPlan?.price ?? 0)
  const safeRemainPct = Math.min(Math.max(remainPct, 0), 100)
  const creditAmount = roundMoney(currentPlanPrice * (safeRemainPct / 100))
  const payableAmount = roundMoney(Math.max(0, targetPlanPrice - creditAmount))

  return {
    currentPlanPrice,
    targetPlanPrice,
    targetPlanName: targetPlan?.planName ?? '',
    creditAmount,
    payableAmount,
  }
}

export function buildQuotaCards(quota?: UsageQuotaResponse, now = new Date()) {
  const prediction = getAiQuotaPrediction(quota, now)
  const metrics = getUsageQuotaMetrics(quota)

  return [
    {
      key: 'ai',
      label: 'AI 调用次数',
      used: metrics.aiUsed,
      limit: metrics.aiLimit,
      unit: '次',
      prediction:
        prediction?.showWarning ? `预计 ${prediction.daysLeft} 天后耗尽 (${prediction.dateStr})` : null,
    },
    {
      key: 'live',
      label: '直播场次',
      used: Number(quota?.liveSessionUsed ?? 0),
      limit: Number(quota?.liveSessionLimit ?? 0),
      unit: '场',
      prediction: null,
    },
    {
      key: 'storage',
      label: '存储空间',
      used: Number(quota?.storageUsed ?? 0),
      limit: Number(quota?.storageLimit ?? 0),
      unit: 'GB',
      prediction: null,
    },
    {
      key: 'kb',
      label: '知识库文档',
      used: Number(quota?.kbDocUsed ?? 0),
      limit: Number(quota?.kbDocLimit ?? 0),
      unit: '篇',
      prediction: null,
    },
  ]
}
