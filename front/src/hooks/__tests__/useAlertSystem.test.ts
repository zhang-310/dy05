import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAlertSystem } from '../useAlertSystem'
import * as monitoringApi from '@/api/monitoring'
import { AlertStatus } from '@/types/monitoring'
import type { AlertRule, AnomalyAlert } from '@/types/monitoring'

vi.mock('@/api/monitoring', () => ({
  searchAlertRules: vi.fn(),
  createAlertRule: vi.fn(),
  updateAlertRule: vi.fn(),
  deleteAlertRule: vi.fn(),
  enableAlertRule: vi.fn(),
  disableAlertRule: vi.fn(),
  searchAlerts: vi.fn(),
  acknowledgeAlert: vi.fn(),
  resolveAlert: vi.fn(),
  getAlertStatistics: vi.fn(),
}))

describe('useAlertSystem', () => {
  const mockAlertRule: AlertRule = {
    ruleId: 1,
    ruleName: 'Test Rule',
    enabled: true,
    metricType: 'cpu',
    threshold: 80,
  } as AlertRule

  const mockAlert: AnomalyAlert = {
    alertId: 1,
    ruleId: 1,
    status: AlertStatus.ACTIVE,
    message: 'CPU usage high',
  } as AnomalyAlert

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [],
      total: 0,
    })
    vi.mocked(monitoringApi.searchAlerts).mockResolvedValue({
      list: [],
      total: 0,
    })
    vi.mocked(monitoringApi.getAlertStatistics).mockResolvedValue({})
  })

  it('initializes with default state', () => {
    const { result } = renderHook(() => useAlertSystem())

    expect(result.current.alertRules).toEqual([])
    expect(result.current.alerts).toEqual([])
    expect(result.current.selectedRule).toBeNull()
    expect(result.current.selectedAlert).toBeNull()
    expect(result.current.isLoading).toBe(true) // Loading starts immediately on mount
    expect(result.current.isSaving).toBe(false)
    expect(result.current.error).toBeNull()
    expect(result.current.totalRules).toBe(0)
    expect(result.current.totalAlerts).toBe(0)
    expect(result.current.pageSize).toBe(20)
  })

  it('loads alert rules and alerts on mount', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [mockAlertRule],
      total: 1,
    })
    vi.mocked(monitoringApi.searchAlerts).mockResolvedValue({
      list: [mockAlert],
      total: 1,
    })

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alertRules).toHaveLength(1)
      expect(result.current.alerts).toHaveLength(1)
      expect(result.current.totalRules).toBe(1)
      expect(result.current.totalAlerts).toBe(1)
    })
  })

  it('loads statistics on mount when autoLoadStatistics is true', async () => {
    const mockStats = { active: 5, resolved: 10 }
    vi.mocked(monitoringApi.getAlertStatistics).mockResolvedValue(mockStats)

    const { result } = renderHook(() => useAlertSystem({ autoLoadStatistics: true }))

    await waitFor(() => {
      expect(result.current.alertStatistics).toEqual(mockStats)
    })
  })

  it('does not load statistics when autoLoadStatistics is false', async () => {
    renderHook(() => useAlertSystem({ autoLoadStatistics: false }))

    await waitFor(() => {
      expect(monitoringApi.getAlertStatistics).not.toHaveBeenCalled()
    })
  })

  it('uses custom initial page size', () => {
    const { result } = renderHook(() => useAlertSystem({ initialPageSize: 50 }))

    expect(result.current.pageSize).toBe(50)
  })

  it('loads alert rules with custom params', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [mockAlertRule],
      total: 1,
    })

    const { result } = renderHook(() => useAlertSystem())

    await act(async () => {
      await result.current.loadAlertRules({ page: 1, rows: 10, keyword: 'test' })
    })

    expect(monitoringApi.searchAlertRules).toHaveBeenCalledWith({
      page: 1,
      rows: 10,
      keyword: 'test',
    })
  })

  it('handles error when loading alert rules fails', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockRejectedValue(new Error('Network error'))

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.error).toBe('Network error')
      expect(result.current.isLoading).toBe(false)
    })
  })

  it('creates new alert rule', async () => {
    const newRule = { ...mockAlertRule, ruleId: 2 }
    vi.mocked(monitoringApi.createAlertRule).mockResolvedValue(newRule)

    const { result } = renderHook(() => useAlertSystem())

    await act(async () => {
      const created = await result.current.createNewAlertRule(newRule)
      expect(created).toEqual(newRule)
    })

    await waitFor(() => {
      expect(result.current.alertRules).toContainEqual(newRule)
      expect(result.current.totalRules).toBe(1)
    })
  })

  it('handles error when creating alert rule fails', async () => {
    vi.mocked(monitoringApi.createAlertRule).mockRejectedValue(new Error('Create failed'))

    const { result } = renderHook(() => useAlertSystem())

    await act(async () => {
      try {
        await result.current.createNewAlertRule(mockAlertRule)
      } catch (err) {
        expect(err).toBeInstanceOf(Error)
      }
    })

    await waitFor(() => {
      expect(result.current.error).toBe('Create failed')
      expect(result.current.isSaving).toBe(false)
    })
  })

  it('updates existing alert rule', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [mockAlertRule],
      total: 1,
    })
    vi.mocked(monitoringApi.updateAlertRule).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alertRules).toHaveLength(1)
    })

    const updatedRule = { ...mockAlertRule, threshold: 90 }

    await act(async () => {
      await result.current.updateExistingAlertRule(updatedRule)
    })

    expect(result.current.alertRules[0].threshold).toBe(90)
  })

  it('deletes alert rule', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [mockAlertRule],
      total: 1,
    })
    vi.mocked(monitoringApi.deleteAlertRule).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alertRules).toHaveLength(1)
    })

    await act(async () => {
      await result.current.deleteExistingAlertRule(1)
    })

    expect(result.current.alertRules).toHaveLength(0)
    expect(result.current.totalRules).toBe(0)
  })

  it('enables alert rule', async () => {
    const disabledRule = { ...mockAlertRule, enabled: false }
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [disabledRule],
      total: 1,
    })
    vi.mocked(monitoringApi.enableAlertRule).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alertRules[0].enabled).toBe(false)
    })

    await act(async () => {
      await result.current.enableRule(1)
    })

    expect(result.current.alertRules[0].enabled).toBe(true)
  })

  it('disables alert rule', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [mockAlertRule],
      total: 1,
    })
    vi.mocked(monitoringApi.disableAlertRule).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alertRules[0].enabled).toBe(true)
    })

    await act(async () => {
      await result.current.disableRule(1)
    })

    expect(result.current.alertRules[0].enabled).toBe(false)
  })

  it('acknowledges alert', async () => {
    vi.mocked(monitoringApi.searchAlerts).mockResolvedValue({
      list: [mockAlert],
      total: 1,
    })
    vi.mocked(monitoringApi.acknowledgeAlert).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alerts[0].status).toBe(AlertStatus.ACTIVE)
    })

    await act(async () => {
      await result.current.acknowledgeExistingAlert(1, 'Investigating')
    })

    expect(result.current.alerts[0].status).toBe(AlertStatus.ACKNOWLEDGED)
  })

  it('resolves alert', async () => {
    vi.mocked(monitoringApi.searchAlerts).mockResolvedValue({
      list: [mockAlert],
      total: 1,
    })
    vi.mocked(monitoringApi.resolveAlert).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alerts[0].status).toBe(AlertStatus.ACTIVE)
    })

    await act(async () => {
      await result.current.resolveExistingAlert(1, 'Fixed')
    })

    expect(result.current.alerts[0].status).toBe(AlertStatus.RESOLVED)
  })

  it('loads statistics with date range', async () => {
    const mockStats = { active: 3, resolved: 7 }
    vi.mocked(monitoringApi.getAlertStatistics).mockResolvedValue(mockStats)

    const { result } = renderHook(() => useAlertSystem({ autoLoadStatistics: false }))

    await act(async () => {
      const stats = await result.current.loadStatistics('2024-01-01', '2024-01-31')
      expect(stats).toEqual(mockStats)
    })

    expect(monitoringApi.getAlertStatistics).toHaveBeenCalledWith('2024-01-01', '2024-01-31')
  })

  it('selects rule', () => {
    const { result } = renderHook(() => useAlertSystem())

    act(() => {
      result.current.selectRule(mockAlertRule)
    })

    expect(result.current.selectedRule).toEqual(mockAlertRule)
  })

  it('selects alert', () => {
    const { result } = renderHook(() => useAlertSystem())

    act(() => {
      result.current.selectAlert(mockAlert)
    })

    expect(result.current.selectedAlert).toEqual(mockAlert)
  })

  it('clears error', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockRejectedValue(new Error('Test error'))

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.error).toBe('Test error')
    })

    act(() => {
      result.current.clearError()
    })

    expect(result.current.error).toBeNull()
  })

  it('updates selected rule when rule is updated', async () => {
    vi.mocked(monitoringApi.searchAlertRules).mockResolvedValue({
      list: [mockAlertRule],
      total: 1,
    })
    vi.mocked(monitoringApi.updateAlertRule).mockResolvedValue(undefined)

    const { result } = renderHook(() => useAlertSystem())

    await waitFor(() => {
      expect(result.current.alertRules).toHaveLength(1)
    })

    act(() => {
      result.current.selectRule(mockAlertRule)
    })

    const updatedRule = { ...mockAlertRule, threshold: 95 }

    await act(async () => {
      await result.current.updateExistingAlertRule(updatedRule)
    })

    expect(result.current.selectedRule?.threshold).toBe(95)
  })
})
