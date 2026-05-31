import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeArray as normalizeResponseArray,
  normalizePage as normalizeResponsePage,
  parseJsonValue,
} from '@/utils/response-normalize'

export type PaymentOrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'REFUNDED'
  | 'CANCELLED'

export interface PaymentOrder {
  id: number
  orderNo: string
  userId?: number
  productId?: number
  quantity?: number
  amount: number
  actualAmount?: number
  status: PaymentOrderStatus
  payType: string
  paymentMethod?: string
  transactionId?: string
  payTime: string | null
  paidAt?: string | null
  trackingNo: string | null
  trackingNumber?: string | null
  remark: string | null
  createTime: string
  createdAt?: string
}

export interface OrderQuery {
  page?: number
  rows?: number
  orderNo?: string
  status?: PaymentOrderStatus | ''
  payType?: string
  startTime?: string
  endTime?: string
}

export interface OrderSave {
  orderNo: string
  productId: number
  quantity: number
  amount: number
  actualAmount?: number
  remark?: string
}

export interface PaymentRefund {
  id: number
  orderId?: number
  orderNo?: string
  refundNo?: string
  transactionNo?: string
  amount: number
  reason: string
  status: string
  createTime: string
}

export interface RefundSave {
  orderId: number
  amount: number
  reason: string
}

export interface ConfirmPaymentPayload {
  transactionId: string
  paymentMethod: string
}

export interface Subscription {
  id: number
  userId: number
  plan: string
  planCode: string
  planName: string
  status: string
  startedAt?: string
  expiresAt?: string
  createTime: string
  expireTime: string
  maxLiveSessions?: number
  maxSvProjects?: number
  maxAiGenerations?: number
  maxStorageMb?: number
}

export interface PaymentPlan {
  planCode: string
  planName: string
  price: number
  features: string[]
  limits?: Record<string, number>
}

export interface PaymentInvoice {
  id: number
  orderId: number
  orderNo: string
  type: string
  title: string
  taxNo?: string
  email: string
  amount: number
  status: number
  downloadUrl?: string
  createTime: string
}

type UnknownRecord = Record<string, unknown>

const PLAN_NAME_MAP: Record<string, string> = {
  free: '免费版',
  pro: '专业版',
  enterprise: '企业版',
}

const PLAN_PRICE_MAP: Record<string, number> = {
  free: 0,
  pro: 299,
  enterprise: 999,
}

function asRecord(value: unknown): UnknownRecord {
  const parsed = parseJsonValue(value)
  return isRecord(parsed) ? parsed : {}
}

function unwrapDataRecord(value: unknown): UnknownRecord {
  const record = asRecord(value)
  for (const key of ['data', 'result', 'payload', 'detail', 'body', 'record', 'item', 'order'] as const) {
    if (isRecord(record[key])) return unwrapDataRecord(record[key])
  }
  return record
}

function toNumber(value: unknown, fallback = 0): number {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function toStringValue(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function normalizeStatus(status: unknown): PaymentOrderStatus {
  if (typeof status === 'string') {
    const upper = status.toUpperCase()
    if (upper in ORDER_STATUS_REQUEST_MAP) return upper as PaymentOrderStatus
  }
  if (typeof status === 'number') {
    return ([
      'PENDING_PAYMENT',
      'PAID',
      'SHIPPED',
      'COMPLETED',
      'CANCELLED',
      'REFUNDED',
    ][status] ?? 'PENDING_PAYMENT') as PaymentOrderStatus
  }
  return 'PENDING_PAYMENT'
}

function normalizeOrder(raw: unknown): PaymentOrder {
  const item = unwrapDataRecord(raw)
  const status = normalizeStatus(item.status)
  const createdAt = toStringValue(item.createdAt, toStringValue(item.createTime))
  const paidAt = toStringValue(item.paidAt, toStringValue(item.payTime))
  const trackingNumber = toStringValue(item.trackingNumber, toStringValue(item.trackingNo))
  return {
    id: toNumber(item.id),
    orderNo: toStringValue(item.orderNo),
    userId: item.userId == null ? undefined : toNumber(item.userId),
    productId: item.productId == null ? undefined : toNumber(item.productId),
    quantity: item.quantity == null ? undefined : toNumber(item.quantity),
    amount: toNumber(item.amount),
    actualAmount: item.actualAmount == null ? undefined : toNumber(item.actualAmount),
    status,
    payType: toStringValue(item.paymentMethod, toStringValue(item.payType, '-')),
    paymentMethod: toStringValue(item.paymentMethod, toStringValue(item.payType, '-')),
    transactionId: toStringValue(item.transactionId),
    payTime: paidAt || null,
    paidAt: paidAt || null,
    trackingNo: trackingNumber || null,
    trackingNumber: trackingNumber || null,
    remark: item.remark == null ? null : String(item.remark),
    createTime: createdAt,
    createdAt,
  }
}

function normalizePaymentPage<T>(
  raw: unknown,
  mapper: (item: unknown) => T,
  fallback: { page?: number; rows?: number } = {},
): PageResult<T> {
  return normalizeResponsePage<unknown, T>(raw, mapper, fallback.page ?? 0, fallback.rows ?? 20)
}

const ORDER_STATUS_REQUEST_MAP: Record<PaymentOrderStatus, PaymentOrderStatus> = {
  PENDING_PAYMENT: 'PENDING_PAYMENT',
  PAID: 'PAID',
  SHIPPED: 'SHIPPED',
  COMPLETED: 'COMPLETED',
  REFUNDED: 'REFUNDED',
  CANCELLED: 'CANCELLED',
}

function normalizeOrderQuery(params: OrderQuery): UnknownRecord {
  const payload: UnknownRecord = {
    page: params.page ?? 0,
    rows: params.rows ?? 20,
  }
  if (params.status) payload.status = ORDER_STATUS_REQUEST_MAP[params.status]
  if (params.startTime) payload.startDate = params.startTime.slice(0, 10)
  if (params.endTime) payload.endDate = params.endTime.slice(0, 10)
  return payload
}

function normalizeSubscription(raw: unknown): Subscription {
  const item = unwrapDataRecord(raw)
  const plan = toStringValue(item.plan, toStringValue(item.planCode, 'free'))
  const startedAt = toStringValue(item.startedAt, toStringValue(item.createTime))
  const expiresAt = toStringValue(item.expiresAt, toStringValue(item.expireTime))
  return {
    id: toNumber(item.id),
    userId: toNumber(item.userId),
    plan,
    planCode: plan,
    planName: PLAN_NAME_MAP[plan] ?? plan,
    status: toStringValue(item.status, 'active'),
    startedAt,
    expiresAt,
    createTime: startedAt,
    expireTime: expiresAt,
    maxLiveSessions: item.maxLiveSessions == null ? undefined : toNumber(item.maxLiveSessions),
    maxSvProjects: item.maxSvProjects == null ? undefined : toNumber(item.maxSvProjects),
    maxAiGenerations: item.maxAiGenerations == null ? undefined : toNumber(item.maxAiGenerations),
    maxStorageMb: item.maxStorageMb == null ? undefined : toNumber(item.maxStorageMb),
  }
}

function normalizePlan(planCode: string, raw: unknown): PaymentPlan {
  const data = unwrapDataRecord(raw)
  const limits = asRecord(data.limits) as Record<string, number>
  const features = Array.isArray(data.features) ? data.features.map(String) : []
  return {
    planCode,
    planName: PLAN_NAME_MAP[planCode] ?? planCode,
    price: PLAN_PRICE_MAP[planCode] ?? 0,
    features,
    limits,
  }
}

function normalizeQuota(raw: unknown): Record<string, unknown> {
  const data = unwrapDataRecord(raw)
  const quotaItem = (key: string, fallbackLabel: string) => {
    const item = isRecord(data[key]) ? asRecord(data[key]) : {}
    const total = toNumber(item.total ?? item.limit, 0)
    const used = toNumber(item.used ?? item.currentUsage, 0)
    return {
      label: toStringValue(item.label, fallbackLabel),
      metric: toStringValue(item.metric, key),
      used,
      total: total < 0 ? 0 : total,
      unlimited: Boolean(item.unlimited) || total < 0,
      allowed: item.allowed,
      resetTime: toStringValue(item.resetTime, toStringValue(data.resetTime, '按自然月')),
    }
  }
  if (isRecord(data.aiCall) || isRecord(data.liveSession) || isRecord(data.storage) || isRecord(data.svProject)) {
    const aiCall = quotaItem('aiCall', 'AI 调用额度')
    const liveSession = quotaItem('liveSession', '直播场次')
    const svProject = quotaItem('svProject', '短视频项目')
    const storage = quotaItem('storage', '存储空间')
    return {
      ...data,
      aiCall,
      liveSession,
      svProject,
      storage,
      aiCallUsed: toNumber(data.aiCallUsed, Number(aiCall.used)),
      aiCallLimit: toNumber(data.aiCallLimit, Number(aiCall.total)),
      aiCallLast7d: toNumber(data.aiCallLast7d, Math.round(Number(aiCall.used) * 0.7)),
      liveSessionUsed: toNumber(data.liveSessionUsed, Number(liveSession.used)),
      liveSessionLimit: toNumber(data.liveSessionLimit, Number(liveSession.total)),
      storageUsed: toNumber(data.storageUsed, Number(storage.used)),
      storageLimit: toNumber(data.storageLimit, Number(storage.total)),
    }
  }
  const hasQuotaSignal = [
    'limit',
    'total',
    'aiCallLimit',
    'currentUsage',
    'used',
    'aiCallUsed',
    'metric',
    'allowed',
    'unlimited',
  ].some(key => Object.prototype.hasOwnProperty.call(data, key))
  if (!hasQuotaSignal) return {}
  const limit = toNumber(data.limit ?? data.total ?? data.aiCallLimit, -1)
  const used = toNumber(data.currentUsage ?? data.used ?? data.aiCallUsed, 0)
  const last7d = toNumber(data.last7d ?? data.aiCallLast7d, Math.round(used * 0.7))
  return {
    aiCallUsed: used,
    aiCallLimit: limit < 0 ? 0 : limit,
    aiCallLast7d: last7d,
    aiCall: {
      label: 'AI 调用额度',
      used,
      total: limit < 0 ? 0 : limit,
      resetTime: '按当前订阅周期',
    },
    metric: data.metric,
    plan: data.plan,
    allowed: data.allowed,
    unlimited: data.unlimited,
  }
}

function normalizeRefund(raw: unknown): PaymentRefund {
  const item = unwrapDataRecord(raw)
  return {
    id: toNumber(item.id),
    orderId: item.orderId == null ? undefined : toNumber(item.orderId),
    orderNo: toStringValue(item.orderNo),
    refundNo: toStringValue(item.refundNo),
    transactionNo: toStringValue(item.transactionNo),
    amount: toNumber(item.amount),
    reason: toStringValue(item.reason),
    status: toStringValue(item.status, 'PENDING'),
    createTime: toStringValue(item.createTime),
  }
}

function notImplemented(feature: string): Promise<never> {
  return Promise.reject(new Error(`${feature} 后端接口尚未接入`))
}

export const paymentApi = {
  // Order
  async list(params: OrderQuery): Promise<PageResult<PaymentOrder>> {
    const result = await request.post<PageResult<PaymentOrder>>('/payment/order/list', normalizeOrderQuery(params))
    return normalizePaymentPage(result, normalizeOrder, params)
  },
  async detail(id: number): Promise<PaymentOrder> {
    const result = await request.post<PaymentOrder>('/payment/order/get', { orderId: id })
    return normalizeOrder(result)
  },
  async create(params: OrderSave): Promise<number> {
    return request.post<number>('/payment/order/create', {
      ...params,
      actualAmount: params.actualAmount ?? params.amount,
    })
  },
  async getByOrderNo(orderNo: string): Promise<PaymentOrder> {
    const result = await request.post<PaymentOrder>('/payment/order/getByOrderNo', { orderNo })
    return normalizeOrder(result)
  },
  confirmPayment: (id: number, params: ConfirmPaymentPayload) =>
    request.post<void>('/payment/order/confirmPayment', {
      orderId: String(id),
      transactionId: params.transactionId.trim(),
      paymentMethod: params.paymentMethod.trim(),
    }),
  ship: (id: number, trackingNo: string) =>
    request.post<void>('/payment/order/ship', { orderId: String(id), trackingNumber: trackingNo }),
  complete: (id: number) => request.post<void>('/payment/order/complete', { orderId: id }),
  cancel: (id: number) => request.post<void>('/payment/order/cancel', { orderId: id }),

  // Refund
  async refundCreate(params: RefundSave): Promise<number> {
    return request.post<number>('/payment/refund/create', params)
  },
  async refundGet(id: number): Promise<PaymentRefund> {
    const result = await request.post<PaymentRefund>('/payment/refund/get', { refundId: id })
    return normalizeRefund(result)
  },
  async refundListByOrder(orderId: number): Promise<PaymentRefund[]> {
    const result = await request.post<PaymentRefund[]>('/payment/refund/listByOrder', { orderId })
    return normalizeResponseArray<unknown>(result).map(normalizeRefund)
  },
  async refundSearch(params: { page?: number; rows?: number; keyword?: string; status?: string }) {
    const result = await request.post<PageResult<PaymentRefund>>('/payment/refund/search', params)
    return normalizePaymentPage(result, normalizeRefund, params)
  },
  refundApprove: (id: number) => request.post<void>('/payment/refund/approve', { refundId: id }),
  refundReject: (id: number, reason: string) => request.post<void>('/payment/refund/reject', { refundId: String(id), reason }),
  refundComplete: (id: number) => request.post<void>('/payment/refund/complete', { refundId: id }),

  // Subscription
  async subscriptionCurrent(): Promise<Subscription | null> {
    const result = await request.post<Subscription | null>('/payment/subscription/current', {})
    return result ? normalizeSubscription(result) : null
  },
  async subscriptionUpgrade(planCode: string): Promise<Subscription> {
    const result = await request.post<Subscription>('/payment/subscription/upgrade', { plan: planCode })
    return normalizeSubscription(result)
  },
  subscriptionCheckQuota: async (metric: string) => normalizeQuota(
    await request.post<Record<string, unknown>>('/payment/subscription/check-quota', { metric }),
  ),
  async subscriptionPlans(): Promise<PaymentPlan[]> {
    const plans = await Promise.all(
      ['free', 'pro', 'enterprise'].map(async plan => normalizePlan(
        plan,
        await request.post<Record<string, unknown>>('/payment/subscription/plans', { plan }),
      )),
    )
    return plans
  },

  usageQuota: async () => normalizeQuota(await request.post<Record<string, unknown>>('/payment/usage/quota', {})),

  // Not implemented in current backend.
  quotaAlertConfigGet: () => notImplemented('配额告警配置'),
  quotaAlertConfigSave: () => notImplemented('配额告警配置保存'),
  invoiceApply: () => notImplemented('发票申请'),
  invoiceList: () => notImplemented('发票列表'),
  invoiceGet: () => notImplemented('发票详情'),
  invoiceDownload: () => notImplemented('发票下载'),
  exportOrders: () => notImplemented('订单导出'),
}
