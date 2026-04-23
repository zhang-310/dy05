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

  it('apiLogList posts log query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await systemApi.apiLogList({ page: 0, rows: 10, apiPath: '/api/v1/auth/login' })
    expect(mockPost).toHaveBeenCalledWith('/system/api-log/list', {
      page: 0,
      rows: 10,
      apiPath: '/api/v1/auth/login',
    })
  })

  it('alertRuleCreate posts alert rule payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await systemApi.alertRuleCreate({ ruleName: 'CPU 超限', metric: 'cpu', threshold: 90 })
    expect(mockPost).toHaveBeenCalledWith('/monitoring/alert-rules/create', {
      ruleName: 'CPU 超限',
      metric: 'cpu',
      threshold: 90,
    })
  })

  it('metricsRealtime posts empty object payload', async () => {
    mockPost.mockResolvedValue({})
    await systemApi.metricsRealtime()
    expect(mockPost).toHaveBeenCalledWith('/monitoring/metrics/realtime', {})
  })

  it('externalApiHealthStatus posts target id', async () => {
    mockPost.mockResolvedValue({ status: 'up' })
    await systemApi.externalApiHealthStatus(7)
    expect(mockPost).toHaveBeenCalledWith('/system/external-api/health-status', { id: 7 })
  })

  it('performanceApiList posts params object', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await systemApi.performanceApiList({ page: 1, rows: 20, path: '/api/v1/live/session/search' })
    expect(mockPost).toHaveBeenCalledWith('/system/performance/api/list', {
      page: 1,
      rows: 20,
      path: '/api/v1/live/session/search',
    })
  })

  it('metricsPrometheus posts empty payload', async () => {
    mockPost.mockResolvedValue('# HELP')
    await systemApi.metricsPrometheus()
    expect(mockPost).toHaveBeenCalledWith('/system/metrics/prometheus', {})
  })
})
