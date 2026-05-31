import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { systemApi } from '../system'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('system API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('apiLogList maps path filter to backend apiName payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await systemApi.apiLogList({ page: 0, rows: 10, apiPath: '/api/v1/auth/login' })
    expect(mockPost).toHaveBeenCalledWith('/system/api-log/list', {
      page: 0,
      rows: 10,
      apiName: '/api/v1/auth/login',
    })
  })

  it('alertRuleCreate posts backend alert rule payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await systemApi.alertRuleCreate({ ruleName: 'CPU 超限', metric: 'cpu', threshold: 90 })
    expect(mockPost).toHaveBeenCalledWith('/monitoring/alert-rules/create', {
      name: 'CPU 超限',
      metricName: 'cpu',
      threshold: 90,
      type: 'threshold',
      operator: '>',
      duration: 60,
      severity: 'warning',
      enabled: true,
    })
  })

  it('alertRuleSearch posts pagination payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 })
    await systemApi.alertRuleSearch({ page: 0, rows: 20 })
    expect(mockPost).toHaveBeenCalledWith('/monitoring/alert-rules/search', {
      page: 0,
      rows: 20,
    })
  })

  it('syncLogList posts pagination payload', async () => {
    mockPost.mockResolvedValue({ data: { records: [{ id: 7, syncType: 'douyin-video', status: 'success' }], totalElements: 5 } })
    const res = await systemApi.syncLogList({ page: 1, rows: 50, syncType: 'douyin-video' })
    expect(mockPost).toHaveBeenCalledWith('/system/sync-log/list', {
      page: 1,
      rows: 50,
      syncType: 'douyin-video',
    })
    expect(res.total).toBe(5)
    expect(res.list[0]).toEqual(expect.objectContaining({ id: 7, status: 'SUCCESS' }))
  })

  it('metricsRealtime posts empty object payload', async () => {
    mockPost.mockResolvedValue({})
    await systemApi.metricsRealtime()
    expect(mockPost).toHaveBeenCalledWith('/monitoring/metrics/realtime', {})
  })

  it('externalApiHealthStatus posts providerCode payload', async () => {
    mockPost.mockResolvedValue({ status: 'up' })
    await systemApi.externalApiHealthStatus('deepseek')
    expect(mockPost).toHaveBeenCalledWith('/system/external-api/health-status', {
      providerCode: 'deepseek',
      status: 'unknown',
    })
  })

  it('externalApiHealthStatus posts explicit status update payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await systemApi.externalApiHealthStatus({
      providerCode: 'deepseek',
      status: 'healthy',
      latencyMs: 120,
      successRate: 99.5,
    })
    expect(mockPost).toHaveBeenCalledWith('/system/external-api/health-status', {
      providerCode: 'deepseek',
      status: 'healthy',
      latencyMs: 120,
      successRate: 99.5,
    })
  })

  it('externalApiProbe posts backend provider probe payload', async () => {
    mockPost.mockResolvedValue({ providerCode: 'tianapi', status: 'healthy' })
    await systemApi.externalApiProbe('tianapi')
    expect(mockPost).toHaveBeenCalledWith('/system/external-api/probe', {
      providerCode: 'tianapi',
    })
  })

  it('diagnosticReport posts backend endpoint and normalizes snapshot payload', async () => {
    mockPost.mockResolvedValue({
      data: {
        generatedAt: '2026-05-23T09:00:00',
        source: '/system/diagnostic/report',
        degraded: false,
        failures: [],
        health: { _overall: 'UP' },
        info: { runtime: { javaVersion: '17' } },
        apiStats: {
          totalCalls: 100,
          successCount: 98,
          failCount: 2,
          avgDurationMs: 120,
        },
      },
    })

    await expect(systemApi.diagnosticReport()).resolves.toEqual(expect.objectContaining({
      generatedAt: '2026-05-23T09:00:00',
      source: '/system/diagnostic/report',
      degraded: false,
      failures: [],
      health: { _overall: 'UP' },
      apiStats: expect.objectContaining({
        totalCalls: 100,
        successCalls: 98,
        errorCalls: 2,
        avgResponseTime: 120,
      }),
    }))
    expect(mockPost).toHaveBeenCalledWith('/system/diagnostic/report', {})
  })

  it('performanceApiList posts params object', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await systemApi.performanceApiList({ page: 1, rows: 20, path: '/api/v1/live/session/search' })
    expect(mockPost).toHaveBeenCalledWith('/system/performance/metrics/search', {
      page: 1,
      rows: 20,
      path: '/api/v1/live/session/search',
    })
  })

  it('normalizes wrapped system pages and performance rows', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ id: 1, apiName: '/api/test', status: 1 }], totalElements: 2 } })
      .mockResolvedValueOnce({ data: { items: [{ time: '10:00', p95: 300 }] } })
      .mockResolvedValueOnce({ data: { rows: [{ path: '/api/slow', avgMs: 700 }], count: 1 } })
      .mockResolvedValueOnce({ data: { status: 'DEGRADED' } })

    await expect(systemApi.apiLogList({ page: 0, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 2,
      list: [expect.objectContaining({ apiPath: '/api/test', statusCode: 200 })],
    }))
    await expect(systemApi.performanceTimeseries()).resolves.toEqual([{ time: '10:00', p95: 300 }])
    await expect(systemApi.performanceSlowQuery({ rows: 10 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [{ path: '/api/slow', avgMs: 700 }],
    }))
    await expect(systemApi.performanceAnalysis()).resolves.toEqual({ status: 'DEGRADED' })
  })

  it('metricsPrometheus posts empty payload', async () => {
    mockPost.mockResolvedValue('# HELP')
    await systemApi.metricsPrometheus()
    expect(mockPost).toHaveBeenCalledWith('/system/metrics/prometheus', {})
  })
})
