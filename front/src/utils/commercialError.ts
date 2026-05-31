/** Gaifan 商业化错误码：与后端 ErrorCode 对齐 */
export const INSUFFICIENT_CREDITS = 4420
export const ENTITLEMENT_DENIED = 4421

export function isCommercialDenial(status: unknown): boolean {
  return status === INSUFFICIENT_CREDITS || status === ENTITLEMENT_DENIED
}

export function commercialDenialMessage(status: unknown, fallback?: string): string {
  if (status === ENTITLEMENT_DENIED) {
    return fallback ?? '当前租户未开通该产品功能，请联系管理员或前往积分治理购买权益。'
  }
  if (status === INSUFFICIENT_CREDITS) {
    return fallback ?? '积分不足，请前往积分治理充值后再试。'
  }
  return fallback ?? '请求失败'
}

export const CREDITS_GOVERNANCE_PATH = '/admin/gaifan/credits'
