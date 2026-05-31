import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  getActiveAlerts,
  getHealthStatus,
  getMonitoringStatistics,
  getPerformanceTrend,
  getRealtimeMetrics,
  getTopErrorEndpoints,
  searchAlertRules,
  searchLogs,
} from '../monitoring'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('monitoring API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('normalizes wrapped realtime, health, alerts and stats payloads', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { cpu: 28, memory: 55, responseTime: 120, cacheHitRate: 77.8 } })
      .mockResolvedValueOnce({ data: { records: [{ timestamp: '10:00', value: '300' }], metricName: 'latency', timeRange: 'hour' } })
      .mockResolvedValueOnce({ data: { alerts: [{ id: 1, ruleName: 'Redis', severity: 'critical', status: 'triggered' }], total: 1 } })
      .mockResolvedValueOnce({ data: { status: 'UP', components: { cache: { status: 'DEGRADED', message: '慢' } } } })
      .mockResolvedValueOnce({ data: { totalAlerts: '3', activeAlerts: '1', criticalAlerts: '1' } })

    await expect(getRealtimeMetrics()).resolves.toMatchObject({ cpuUsage: 28, responseTime: 120, cacheHitRate: 77.8 })
    await expect(getPerformanceTrend('latency')).resolves.toMatchObject({
      metricName: 'latency',
      dataPoints: [{ timestamp: 0, value: 300, label: undefined }],
    })
    await expect(getActiveAlerts()).resolves.toMatchObject({ total: 1, alerts: [expect.objectContaining({ ruleName: 'Redis' })] })
    await expect(getHealthStatus()).resolves.toMatchObject({ status: 'UP', components: { cache: expect.objectContaining({ status: 'DEGRADED' }) } })
    await expect(getMonitoringStatistics()).resolves.toMatchObject({ totalAlerts: 3, activeAlerts: 1, criticalAlerts: 1 })
  })

  it('preserves missing realtime metric fields instead of fabricating zero values', async () => {
    mockPost.mockResolvedValueOnce({ data: {} })

    await expect(getRealtimeMetrics()).resolves.toEqual({
      timestamp: undefined,
      cpuUsage: undefined,
      memoryUsage: undefined,
      diskUsage: undefined,
      networkIn: undefined,
      networkOut: undefined,
      activeConnections: undefined,
      requestsPerSecond: undefined,
      errorRate: undefined,
      responseTime: undefined,
      queueDepth: undefined,
      cacheHitRate: undefined,
      databaseConnections: undefined,
    })
  })

  it('normalizes wrapped monitoring pages and arrays', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ id: 2, name: 'CPU', metric: 'cpu', status: 1 }], totalElements: 1 } })
      .mockResolvedValueOnce({ rows: [{ id: 3, level: 'ERROR', module: 'ai', summary: 'stream closed' }], totalRecords: 1 })
      .mockResolvedValueOnce({ data: { items: [{ endpoint: '/api/test', errorCount: '4', errorRate: '0.5' }] } })

    await expect(searchAlertRules({ page: 0, rows: 20 })).resolves.toMatchObject({
      total: 1,
      list: [expect.objectContaining({ ruleName: 'CPU', metricName: 'cpu', enabled: true })],
    })
    await expect(searchLogs({ page: 0, rows: 20 })).resolves.toMatchObject({
      total: 1,
      list: [expect.objectContaining({ logger: 'ai', message: 'stream closed' })],
    })
    await expect(getTopErrorEndpoints()).resolves.toEqual([{ endpoint: '/api/test', errorCount: '4', errorRate: '0.5' }])
  })
})
