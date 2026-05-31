import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useMonitoringData } from '../useMonitoringData'
import * as monitoringApi from '@/api/monitoring'
import { ComponentStatus } from '@/types/monitoring'

vi.mock('@/api/monitoring', () => ({
  getRealtimeMetrics: vi.fn(),
  getActiveAlerts: vi.fn(),
  getHealthStatus: vi.fn(),
  getPerformanceTrend: vi.fn(),
  subscribeToRealtimeMetrics: vi.fn(),
  acknowledgeAlert: vi.fn(),
  resolveAlert: vi.fn(),
}))

describe('useMonitoringData', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(monitoringApi.getRealtimeMetrics).mockResolvedValue({ cpuUsage: 40, responseTime: 120 })
    vi.mocked(monitoringApi.getActiveAlerts).mockResolvedValue({
      alerts: [{ alertId: 1, alertType: 'metric_threshold', severity: 'high', status: 'active', message: 'cache low' }],
      total: 1,
    } as never)
    vi.mocked(monitoringApi.getHealthStatus).mockResolvedValue({
      status: ComponentStatus.DEGRADED,
      components: { cache: { status: ComponentStatus.DEGRADED, message: 'slow' } },
    } as never)
    vi.mocked(monitoringApi.getPerformanceTrend).mockResolvedValue({
      metricName: 'responseTime',
      unit: 'ms',
      timeRange: 'hour',
      dataPoints: [{ timestamp: 1, value: 120 }],
    })
    vi.mocked(monitoringApi.subscribeToRealtimeMetrics).mockReturnValue({
      addEventListener: vi.fn(),
      close: vi.fn(),
    } as unknown as EventSource)
  })

  it('keeps partial request errors visible instead of clearing them on another successful request', async () => {
    vi.mocked(monitoringApi.getActiveAlerts).mockRejectedValueOnce(new Error('alerts table down'))

    const { result } = renderHook(() => useMonitoringData({ enableAutoUpdate: false, enableWebSocket: false }))

    await result.current.refreshAll()

    await waitFor(() => {
      expect(result.current.realtimeMetrics?.cpuUsage).toBe(40)
      expect(result.current.healthStatus?.status).toBe(ComponentStatus.DEGRADED)
      expect(result.current.error).toContain('活跃告警加载失败：alerts table down')
    })
  })

  it('stores trend data by metric and clears only the matching trend error after success', async () => {
    vi.mocked(monitoringApi.getPerformanceTrend)
      .mockRejectedValueOnce(new Error('trend timeout'))
      .mockResolvedValueOnce({
        metricName: 'responseTime',
        unit: 'ms',
        timeRange: 'day',
        dataPoints: [{ timestamp: 1, value: 180 }],
      })

    const { result } = renderHook(() => useMonitoringData({ enableAutoUpdate: false, enableWebSocket: false }))

    await result.current.fetchPerformanceTrend('responseTime', 'day')

    await waitFor(() => {
      expect(result.current.error).toContain('responseTime趋势加载失败：trend timeout')
    })

    await result.current.fetchPerformanceTrend('responseTime', 'day')

    await waitFor(() => {
      expect(result.current.error).toBeNull()
      expect(result.current.performanceTrends.get('responseTime')?.dataPoints[0].value).toBe(180)
    })
  })
})
