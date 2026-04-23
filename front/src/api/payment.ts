import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface PaymentOrder {
  id: number
  orderNo: string
  userId: number
  amount: number
  status: number
  payType: string
  payTime: string | null
  trackingNo: string | null
  remark: string | null
  createTime: string
}
export interface OrderQuery { page?: number; rows?: number; orderNo?: string; status?: number; payType?: string; startTime?: string; endTime?: string }
export interface OrderSave {
  productId?: number; productType?: string; amount: number; payType: string; remark?: string
}

export interface PaymentRefund {
  id: number; orderId: number; orderNo: string; amount: number
  reason: string; status: number; createTime: string
}
export interface RefundSave {
  orderId: number; amount: number; reason: string
}

export interface Subscription {
  id: number; userId: number; planCode: string; planName: string
  status: number; expireTime: string; createTime: string
}

export interface PaymentInvoice {
  id: number
  orderId: number
  orderNo: string
  type: string          // 'personal' | 'company'
  title: string
  taxNo?: string
  email: string
  amount: number
  status: number        // 0=申请中 1=已开具 2=已拒绝
  downloadUrl?: string
  createTime: string
}

export const paymentApi = {
  // Order
  list: (params: OrderQuery) => request.post<PageResult<PaymentOrder>>('/payment/order/list', params),
  detail: (id: number) => request.post<PaymentOrder>('/payment/order/get', { id }),
  create: (params: Partial<OrderSave>) => request.post<PaymentOrder>('/payment/order/create', params),
  getByOrderNo: (orderNo: string) => request.post<PaymentOrder>('/payment/order/getByOrderNo', { orderNo }),
  confirmPayment: (id: number) => request.post<void>('/payment/order/confirmPayment', { id }),
  ship: (id: number, trackingNo: string) => request.post<void>('/payment/order/ship', { id, trackingNo }),
  complete: (id: number) => request.post<void>('/payment/order/complete', { id }),
  cancel: (id: number, reason?: string) => request.post<void>('/payment/order/cancel', { id, reason }),

  // Refund
  refundCreate: (params: RefundSave) => request.post<PaymentRefund>('/payment/refund/create', params),
  refundGet: (id: number) => request.post<PaymentRefund>('/payment/refund/get', { id }),
  refundListByOrder: (orderId: number) => request.post<PaymentRefund[]>('/payment/refund/listByOrder', { orderId }),
  refundApprove: (id: number) => request.post<void>('/payment/refund/approve', { id }),
  refundReject: (id: number, reason: string) => request.post<void>('/payment/refund/reject', { id, reason }),
  refundComplete: (id: number) => request.post<void>('/payment/refund/complete', { id }),

  // Subscription
  subscriptionCurrent: () => request.post<Subscription>('/payment/subscription/current', {}),
  subscriptionUpgrade: (planCode: string) => request.post<Subscription>('/payment/subscription/upgrade', { planCode }),
  subscriptionCheckQuota: (feature: string) => request.post<Record<string, unknown>>('/payment/subscription/check-quota', { feature }),
  subscriptionPlans: () => request.post<Record<string, unknown>>('/payment/subscription/plans', {}),

  // Usage Quota
  usageQuota: () => request.post<Record<string, unknown>>('/payment/usage/quota', {}),

  // P1: 配额告警配置
  quotaAlertConfigGet: () =>
    request.post<Record<string, unknown>>('/payment/quota/alert-config', {}),
  quotaAlertConfigSave: (config: Record<string, unknown>) =>
    request.post<void>('/payment/quota/alert-config/save', config),

  // P1: 发票管理
  invoiceApply: (orderId: number, params: { type: string; title: string; taxNo?: string; email: string }) =>
    request.post<{ invoiceId: number }>('/payment/invoice/apply', { orderId, ...params }),
  invoiceList: (params: { page?: number; rows?: number; status?: number; startTime?: string; endTime?: string }) =>
    request.post<import('@/types/common').PageResult<PaymentInvoice>>('/payment/invoice/list', params),
  invoiceGet: (id: number) =>
    request.post<PaymentInvoice>('/payment/invoice/get', { id }),
  invoiceDownload: (id: number) =>
    request.post<{ downloadUrl: string }>('/payment/invoice/download', { id }),

  // P2: 订单导出
  exportOrders: (params: OrderQuery) =>
    request.post<{ downloadUrl: string }>('/payment/order/export', params),
}
