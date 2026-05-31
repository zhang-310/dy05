import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { AnomalyAlerts } from '../AnomalyAlerts'
import { AlertSeverity, AlertStatus, type AnomalyAlert } from '@/types/monitoring'
import * as monitoringApi from '@/api/monitoring'

vi.mock('@/api/monitoring', () => ({
  acknowledgeAlert: vi.fn(),
  resolveAlert: vi.fn(),
}))

const alerts: AnomalyAlert[] = [
  {
    alertId: 1,
    alertType: 'metric_threshold',
    severity: AlertSeverity.CRITICAL,
    status: AlertStatus.ACTIVE,
    metric: 'redis.cacheHitRate',
    currentValue: 22.28,
    threshold: 60,
    message: '缓存命中率偏低',
    firstDetectedAt: '2026-05-22T10:00:00',
    lastDetectedAt: '2026-05-22T10:30:00',
  },
  {
    alertId: 2,
    alertType: 'anomaly_detection',
    severity: AlertSeverity.HIGH,
    status: AlertStatus.ACKNOWLEDGED,
    metric: 'api.errorRate',
    currentValue: 8.6,
    threshold: 3,
    message: '错误率异常',
    firstDetectedAt: '2026-05-22T08:00:00',
    lastDetectedAt: '2026-05-22T10:30:00',
  },
  {
    alertId: 3,
    alertType: 'health_check',
    severity: AlertSeverity.LOW,
    status: AlertStatus.RESOLVED,
    metric: 'storage.latency',
    currentValue: 1.2,
    threshold: 5,
    message: '存储延迟恢复',
  },
]

describe('AnomalyAlerts', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
    vi.mocked(monitoringApi.acknowledgeAlert).mockResolvedValue(undefined)
    vi.mocked(monitoringApi.resolveAlert).mockResolvedValue(undefined)
  })

  it('uses theme-aware alert surfaces and submits alert actions in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <AnomalyAlerts alerts={alerts} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('anomaly-alert-summary-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })
    expect(screen.getByTestId('anomaly-alert-table-head-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })
    expect(screen.getAllByTestId('anomaly-alert-row-surface')[0]).toHaveAttribute('data-critical-border-color', '#e57373')
    expect(screen.getAllByTestId('anomaly-alert-metric-value-surface')[1]).toHaveAttribute('data-metric-color', '#e57373')
    expect(screen.getByTestId('anomaly-alert-resolve-action-surface')).toHaveAttribute('data-action-color', '#81c784')

    const serialized = document.body.innerHTML
    for (const legacy of ['#f5f5f5', '#fafafa', '#f44336', '#666', '#4caf50']) {
      expect(serialized).not.toContain(legacy)
    }

    fireEvent.click(screen.getByRole('button', { name: '查看详情' }))
    expect(screen.getByRole('heading', { name: '确认告警' })).toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('输入确认备注信息...'), { target: { value: '已通知负责人' } })
    fireEvent.click(screen.getByRole('button', { name: '确认告警' }))
    await waitFor(() => expect(monitoringApi.acknowledgeAlert).toHaveBeenCalledWith(1, '已通知负责人'))

    fireEvent.click(screen.getByTestId('anomaly-alert-resolve-action-surface'))
    expect(screen.getByText('解决告警')).toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('输入解决方案备注...'), { target: { value: '已扩容缓存' } })
    fireEvent.click(screen.getByRole('button', { name: '标记为已解决' }))
    await waitFor(() => expect(monitoringApi.resolveAlert).toHaveBeenCalledWith(2, '已扩容缓存'))
  })

  it('renders a theme-aware empty state', () => {
    renderWithProviders(
      <AppThemeProvider>
        <AnomalyAlerts alerts={[]} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('anomaly-alert-empty-surface')).toBeInTheDocument()
    expect(screen.getByText('暂无活跃告警 ✅')).toBeInTheDocument()
  })
})
