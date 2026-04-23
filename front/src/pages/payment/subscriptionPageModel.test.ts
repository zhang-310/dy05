import { describe, expect, it } from 'vitest'
import {
  buildQuotaCards,
  getAiQuotaPrediction,
  getSubscriptionProgress,
  getUpgradeSummary,
  getUsageQuotaMetrics,
  normalizePaymentPlans,
} from './subscriptionPageModel'

describe('subscriptionPageModel', () => {
  it('normalizes plan data for page rendering', () => {
    expect(
      normalizePaymentPlans([
        { planCode: 'pro', planName: '专业版', price: '699', features: ['AI 生成', 99] },
      ]),
    ).toEqual([
      { planCode: 'pro', planName: '专业版', price: 699, features: ['AI 生成', '99'] },
    ])
  })

  it('computes subscription progress with a fixed current date', () => {
    expect(
      getSubscriptionProgress(
        {
          createTime: '2026-01-01 00:00:00',
          expireTime: '2026-01-11 00:00:00',
        },
        new Date('2026-01-06T00:00:00'),
      ),
    ).toEqual({
      totalDays: 10,
      remainDays: 5,
      remainPct: 50,
    })
  })

  it('builds quota metrics and prediction deterministically', () => {
    const quota = {
      aiCallUsed: 120,
      aiCallLimit: 1000,
      aiCallLast7d: 70,
    }

    expect(getUsageQuotaMetrics(quota)).toEqual({
      aiUsed: 120,
      aiLimit: 1000,
      usedLast7d: 70,
      dailyRate: 10,
      aiDaysLeft: 88,
    })

    expect(getAiQuotaPrediction(quota, new Date('2026-04-10T00:00:00'))).toEqual({
      daysLeft: 88,
      dateStr: '7月7日',
      showWarning: false,
    })
  })

  it('computes deterministic upgrade pricing from remaining time', () => {
    const plans = normalizePaymentPlans([
      { planCode: 'pro', planName: '专业版', price: 699, features: [] },
      { planCode: 'ultra', planName: '企业版 Ultra', price: 1299, features: [] },
    ])

    expect(getUpgradeSummary(plans, 'pro', 'ultra', 50)).toEqual({
      currentPlanPrice: 699,
      targetPlanPrice: 1299,
      targetPlanName: '企业版 Ultra',
      creditAmount: 349.5,
      payableAmount: 949.5,
    })
  })

  it('adds warning text only for near-term quota depletion', () => {
    const cards = buildQuotaCards(
      {
        aiCallUsed: 930,
        aiCallLimit: 1000,
        aiCallLast7d: 35,
        liveSessionUsed: 4,
        liveSessionLimit: 10,
      },
      new Date('2026-04-10T00:00:00'),
    )

    expect(cards[0]).toMatchObject({
      key: 'ai',
      prediction: '预计 14 天后耗尽 (4月24日)',
    })
    expect(cards[1]).toMatchObject({
      key: 'live',
      prediction: null,
    })
  })
})
