import { beforeEach, describe, expect, it, vi } from 'vitest'
import { abtestApi } from '@/api/abtest'

vi.mock('@/api/abtest', () => ({
  abtestApi: {
    eventRecord: vi.fn(),
  },
}))

describe('abTestLogger', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('uploads queued events through real /abtest/event/record contract', async () => {
    const { abTestLogger } = await import('../abtest-logger')
    vi.mocked(abtestApi.eventRecord).mockResolvedValue(undefined as never)

    abTestLogger.logEvent({
      experimentId: '7',
      variant: '2',
      eventName: 'conversion',
      data: { userFingerprint: 'u1', sessionId: 's1' },
      timestamp: Date.now(),
    })

    await abTestLogger.uploadEvents('7')

    expect(abtestApi.eventRecord).toHaveBeenCalledWith({
      experimentId: 7,
      variantId: 2,
      eventType: 'conversion',
      userFingerprint: 'u1',
      sessionId: 's1',
    })
    expect(abTestLogger.getLocalEvents('7')).toEqual([])
  })

  it('keeps failed events in local queue for retry', async () => {
    const { abTestLogger } = await import('../abtest-logger')
    vi.mocked(abtestApi.eventRecord).mockRejectedValueOnce(new Error('record down') as never)

    abTestLogger.logEvent({
      experimentId: '7',
      variant: '2',
      eventName: 'click',
      timestamp: Date.now(),
    })

    await abTestLogger.uploadEvents('7')

    expect(abtestApi.eventRecord).toHaveBeenCalledWith(expect.objectContaining({
      experimentId: 7,
      variantId: 2,
      eventType: 'click',
    }))
    expect(abTestLogger.getLocalEvents('7')).toHaveLength(1)
  })

  it('rejects non-numeric variant data instead of calling a mock analytics endpoint', async () => {
    const { abTestLogger } = await import('../abtest-logger')

    abTestLogger.logEvent({
      experimentId: '7',
      variant: 'B',
      eventName: 'view',
      timestamp: Date.now(),
    })

    await abTestLogger.uploadEvents('7')

    expect(abtestApi.eventRecord).not.toHaveBeenCalled()
    expect(abTestLogger.getLocalEvents('7')).toHaveLength(1)
  })
})
