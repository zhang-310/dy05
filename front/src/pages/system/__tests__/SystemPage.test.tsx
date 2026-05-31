import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import SystemPage from '../SystemPage'
import { systemApi } from '@/api/system'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('@/components/base', () => ({
  PageHeader: ({ title, subtitle, actions }: any) => (
    <div>
      <h1>{title}</h1>
      {subtitle && <p>{subtitle}</p>}
      {actions}
    </div>
  ),
  ErrorAlert: ({ title, message, onRetry }: any) => (
    <div role="alert">
      {title && <strong>{title}</strong>}
      <span>{message}</span>
      {onRetry && <button type="button" onClick={onRetry}>重试</button>}
    </div>
  ),
  StandardDataGrid: ({
    rows,
    columns,
    onPaginationModelChange,
    getRowId,
  }: any) => (
    <div>
      <button type="button" onClick={() => onPaginationModelChange?.({ page: 1, pageSize: 30 })}>
        mock-next-page
      </button>
      <table>
        <tbody>
          {rows.map((row: any, rowIndex: number) => (
            <tr key={getRowId?.(row) ?? row.id ?? rowIndex}>
              {columns.map((col: any) => (
                <td key={col.field}>
                  {col.renderCell
                    ? col.renderCell({ row, value: row[col.field], field: col.field })
                    : col.valueFormatter
                      ? col.valueFormatter({ value: row[col.field], row, field: col.field })
                      : String(row[col.field] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ),
}))

vi.mock('@/api/system', () => ({
  systemApi: {
    info: vi.fn(),
    health: vi.fn(),
    apiLogList: vi.fn(),
    apiLogStats: vi.fn(),
    syncLogList: vi.fn(),
    alertSearch: vi.fn(),
    alertActive: vi.fn(),
    alertAcknowledge: vi.fn(),
    alertResolve: vi.fn(),
    externalApiList: vi.fn(),
    externalApiHealthStatus: vi.fn(),
    diagnosticReport: vi.fn(),
    performanceTimeseries: vi.fn(),
    performanceSlowQuery: vi.fn(),
    performanceAnalysis: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <SystemPage />
    </MemoryRouter>,
  )
}

function renderPageWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <SystemPage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('SystemPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:system-diagnose'),
      revokeObjectURL: vi.fn(),
    })

    vi.mocked(systemApi.health).mockResolvedValue({
      postgres: { status: 'UP', responseTime: 21 },
      redis: { status: 'UP', responseTime: 8 },
      elasticsearch: { status: 'UP', responseTime: 33 },
    } as never)
    vi.mocked(systemApi.info).mockResolvedValue({
      jvmMemory: 512,
      cpuUsage: 35,
      dbConnections: 12,
      uptime: 7380,
    } as never)
    vi.mocked(systemApi.apiLogStats).mockResolvedValue({
      totalCalls: 1200,
      successCalls: 1170,
      errorCalls: 30,
      avgResponseTime: 180,
      p99ResponseTime: 820,
    } as never)
    vi.mocked(systemApi.alertActive).mockResolvedValue({
      total: 1,
      alerts: [
        {
          id: 8,
          ruleId: 3,
          ruleName: 'P99 延迟超限',
          metric: 'p99',
          value: 4200,
          severity: 'HIGH',
          status: 'ACTIVE',
          createTime: '2026-05-15 09:00:00',
        },
      ],
    } as never)
    vi.mocked(systemApi.apiLogList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          apiPath: '/api/system/health',
          method: 'POST',
          statusCode: 200,
          responseTime: 92,
          userId: null,
          ip: '127.0.0.1',
          errorMsg: null,
          createTime: '2026-05-15 09:10:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.syncLogList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 2,
          syncType: 'DOUYIN_VIDEO',
          status: 'FAILED',
          totalCount: 30,
          successCount: 24,
          failCount: 6,
          errorMessage: 'token expired',
          startTime: '2026-05-15 08:00:00',
          endTime: '2026-05-15 08:03:00',
          createTime: '2026-05-15 08:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.alertSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 8,
          ruleId: 3,
          ruleName: 'P99 延迟超限',
          metric: 'p99',
          value: 4200,
          severity: 'HIGH',
          status: 'ACTIVE',
          createTime: '2026-05-15 09:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.alertAcknowledge).mockResolvedValue(undefined as never)
    vi.mocked(systemApi.alertResolve).mockResolvedValue(undefined as never)
    vi.mocked(systemApi.externalApiList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 7,
          providerCode: 'openai',
          providerName: 'OpenAI',
          category: 'ai',
          baseUrl: 'https://api.openai.com',
          isEnabled: true,
          healthStatus: 'healthy',
          createTime: '2026-05-15 07:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.externalApiHealthStatus).mockResolvedValue(undefined as never)
    vi.mocked(systemApi.diagnosticReport).mockResolvedValue({
      generatedAt: '2026-05-23T09:00:00',
      source: '/system/diagnostic/report',
      degraded: false,
      failures: [],
      health: { _overall: 'UP' },
      info: { runtime: { javaVersion: '17' } },
      apiStats: {
        totalCalls: 1200,
        successCalls: 1170,
        errorCalls: 30,
        avgResponseTime: 180,
        p99ResponseTime: 820,
      },
    } as never)
    vi.mocked(systemApi.performanceTimeseries).mockResolvedValue([
      { time: '09:00', p50: 80, p95: 320, p99: 780 },
    ] as never)
    vi.mocked(systemApi.performanceSlowQuery).mockResolvedValue({
      total: 1,
      list: [{ path: '/api/slow', avgMs: 640, callCount: 12, maxMs: 1500 }],
      pageNum: 0,
      pageSize: 10,
    } as never)
    vi.mocked(systemApi.performanceAnalysis).mockResolvedValue({
      status: 'DEGRADED',
      message: 'P99 latency is elevated',
    } as never)
  })

  it('renders health summary, system info, and downloads a diagnosis report', async () => {
    renderPage()

    expect(screen.getByText('系统监控')).toBeInTheDocument()
    const workbench = screen.getByTestId('system-page-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'system-monitoring-consolidated-tabs')
    expect(workbench).toHaveAttribute('data-no-local-health-fallback', 'true')

    await waitFor(() => {
      expect(systemApi.health).toHaveBeenCalled()
      expect(systemApi.info).toHaveBeenCalled()
      expect(systemApi.apiLogStats).toHaveBeenCalled()
      expect(systemApi.alertActive).toHaveBeenCalled()
    })

    expect(await screen.findByText('512 MB')).toBeInTheDocument()
    expect(screen.getByText('35%')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByTestId('system-info-tab-contract')).toHaveAttribute('data-contract-source', '/system/info|/system/health')
    expect(screen.getByTestId('system-health-banner-surface')).toHaveAttribute('data-contract-source', '/system/health|/system/api-log/stats|/monitoring/alerts/active')

    fireEvent.click(screen.getByRole('button', { name: '一键诊断报告' }))
    expect(await screen.findByRole('dialog', { name: '一键诊断报告' })).toBeInTheDocument()
    expect(screen.getByTestId('system-diagnose-dialog-contract')).toHaveAttribute('data-contract-source', '/system/diagnostic/report|loaded-page-state-fallback')
    expect(await screen.findByText('后端诊断报告')).toBeInTheDocument()
    await waitFor(() => {
      expect(systemApi.diagnosticReport).toHaveBeenCalled()
    })

    fireEvent.click(screen.getByRole('button', { name: '下载后端报告' }))
    expect(URL.createObjectURL).toHaveBeenCalled()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:system-diagnose')
  })

  it('filters api logs and paginates through the embedded data grid', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '接口日志' }))

    await waitFor(() => {
      expect(systemApi.apiLogList).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })
    expect(await screen.findByText('/api/system/health')).toBeInTheDocument()
    expect(screen.getByTestId('system-page-workbench')).toHaveAttribute('data-active-tab', '接口日志')
    expect(screen.getByTestId('system-api-log-tab-contract')).toHaveAttribute('data-contract-source', '/system/api-log/stats|/system/api-log/list')

    fireEvent.change(screen.getByPlaceholderText('接口路径筛选'), {
      target: { value: '/api/system' },
    })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))
    await waitFor(() => {
      expect(systemApi.apiLogList).toHaveBeenLastCalledWith({ page: 0, rows: 20, apiPath: '/api/system' })
    })

    fireEvent.click(screen.getByRole('button', { name: 'mock-next-page' }))
    await waitFor(() => {
      expect(systemApi.apiLogList).toHaveBeenLastCalledWith({ page: 1, rows: 30, apiPath: '/api/system' })
    })
  })

  it('handles sync logs, alert actions, external api checks, and performance panels', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '同步日志' }))
    await waitFor(() => {
      expect(systemApi.syncLogList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        syncType: undefined,
        status: undefined,
      })
    })
    expect(await screen.findByText('DOUYIN_VIDEO')).toBeInTheDocument()
    expect(screen.getByText('token expired')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '告警记录' }))
    await waitFor(() => {
      expect(systemApi.alertSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })
    expect(await screen.findByText('P99 延迟超限')).toBeInTheDocument()
    expect(screen.getByTestId('system-alerts-tab-contract')).toHaveAttribute('data-no-alert-rule-crud', 'true')
    expect(screen.getByTestId('system-alerts-tab-contract')).toHaveAttribute('data-no-legacy-system-alert-rule-endpoints', 'true')
    expect(screen.getByTestId('system-alerts-tab-contract')).toHaveAttribute('data-no-optimistic-alert-status-mutation', 'true')
    expect(screen.getByTestId('system-alerts-filter-contract')).toHaveAttribute('data-contract-source', '/monitoring/alerts/search')
    expect(screen.getByTestId('system-alerts-grid-contract')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getByTestId('system-alert-ack-button')).toHaveAttribute('data-contract-source', '/monitoring/alerts/acknowledge')
    expect(screen.getByTestId('system-alert-resolve-button')).toHaveAttribute('data-contract-source', '/monitoring/alerts/resolve')
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(vi.mocked(systemApi.alertAcknowledge).mock.calls[0][0]).toBe(8)
      expect(toast).toHaveBeenCalledWith('已确认', 'success')
    })
    fireEvent.click(screen.getByRole('button', { name: '处理' }))
    await waitFor(() => {
      expect(vi.mocked(systemApi.alertResolve).mock.calls[0][0]).toBe(8)
      expect(toast).toHaveBeenCalledWith('已处理', 'success')
    })

    fireEvent.click(screen.getByRole('tab', { name: '外部API' }))
    await waitFor(() => {
      expect(systemApi.externalApiList).toHaveBeenCalledWith({})
    })
    expect(await screen.findByText('OpenAI')).toBeInTheDocument()
    expect(screen.getByText('https://api.openai.com')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '标记待探测' }))
    await waitFor(() => {
      expect(vi.mocked(systemApi.externalApiHealthStatus).mock.calls[0][0]).toEqual({
        providerCode: 'openai',
        status: 'unknown',
      })
      expect(toast).toHaveBeenCalledWith('已标记为待探测', 'success')
    })

    fireEvent.click(screen.getByRole('tab', { name: '性能监控' }))
    await waitFor(() => {
      expect(systemApi.performanceTimeseries).toHaveBeenCalledWith({ metric: 'response_time', hours: 24 })
      expect(systemApi.performanceSlowQuery).toHaveBeenCalledWith({ rows: 10, minMs: 500 })
      expect(systemApi.performanceAnalysis).toHaveBeenCalled()
    })
    expect(await screen.findByText('/api/slow')).toBeInTheDocument()
    expect(screen.getByText('DEGRADED')).toBeInTheDocument()
  }, 20000)

  it('keeps embedded system action errors visible without mutating rows', async () => {
    vi.mocked(systemApi.alertAcknowledge).mockRejectedValue(new Error('ack denied') as never)
    vi.mocked(systemApi.externalApiHealthStatus).mockRejectedValue(new Error('health write denied') as never)

    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '告警记录' }))
    expect(await screen.findByText('P99 延迟超限')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/\/monitoring\/alerts\/acknowledge 处理失败：ack denied/)).toBeInTheDocument()
    expect(screen.getByText(/告警 ID 8 的当前状态已保留/)).toBeInTheDocument()
    expect(screen.getByTestId('system-alert-action-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByTestId('system-alerts-tab-contract')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getByTestId('system-alerts-grid-contract')).toHaveAttribute('data-no-local-alert-fallback', 'true')
    expect(screen.getByText('P99 延迟超限')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '外部API' }))
    expect(await screen.findByText('OpenAI')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '标记待探测' }))
    expect(await screen.findByText(/\/system\/external-api\/health-status 写入失败：health write denied/)).toBeInTheDocument()
    expect(screen.getByTestId('system-external-api-action-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByText(/供应商 openai 的原健康状态已保留/)).toBeInTheDocument()
    expect(screen.getByText('OpenAI')).toBeInTheDocument()
  }, 20000)

  it('falls back to a local diagnosis snapshot when backend report fails', async () => {
    vi.mocked(systemApi.diagnosticReport).mockRejectedValue(new Error('report endpoint missing') as never)

    renderPage()

    fireEvent.click(screen.getByRole('button', { name: '一键诊断报告' }))

    expect(await screen.findByText(/后端诊断报告加载失败：report endpoint missing/)).toBeInTheDocument()
    expect(screen.getByTestId('system-diagnostic-report-fallback')).toHaveAttribute('data-fallback-source', 'loaded-page-state')
    fireEvent.click(screen.getByRole('button', { name: '下载降级快照' }))
    expect(URL.createObjectURL).toHaveBeenCalled()
  })

  it('tolerates wrapped data in embedded system tabs', async () => {
    vi.mocked(systemApi.apiLogList).mockResolvedValue({
      records: [
        {
          id: 11,
          apiPath: '/api/wrapped',
          method: 'POST',
          statusCode: 200,
          responseTime: 100,
          userId: null,
          ip: '127.0.0.1',
          errorMsg: null,
          createTime: '2026-05-15 09:10:00',
        },
      ],
      totalElements: 1,
    } as never)
    vi.mocked(systemApi.externalApiList).mockResolvedValue({
      items: [
        {
          id: 17,
          providerCode: 'wrapped',
          providerName: 'Wrapped API',
          category: 'ai',
          baseUrl: 'https://wrapped.example.com',
          isEnabled: true,
          healthStatus: 'degraded',
        },
      ],
    } as never)
    vi.mocked(systemApi.performanceTimeseries).mockResolvedValue({
      data: { records: [{ time: '10:00', p50: 90, p95: 300, p99: 700 }] },
    } as never)
    vi.mocked(systemApi.performanceSlowQuery).mockResolvedValue({
      data: { rows: [{ path: '/api/wrapped-slow', avgMs: 900, callCount: 3, maxMs: 1200 }] },
    } as never)

    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '接口日志' }))
    expect(await screen.findByText('/api/wrapped')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '外部API' }))
    expect(await screen.findByText('Wrapped API')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '性能监控' }))
    expect(await screen.findByText('/api/wrapped-slow')).toBeInTheDocument()
  })

  it('uses theme-aware health banner and service chip colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    expect(await screen.findByText('系统健康评分')).toBeInTheDocument()
    await waitFor(() => {
      expect(systemApi.health).toHaveBeenCalled()
      expect(systemApi.alertActive).toHaveBeenCalled()
    })

    expect(screen.getByTestId('system-health-score-surface')).not.toHaveStyle({
      color: 'rgb(244, 67, 54)',
    })
    expect(screen.getByTestId('system-health-active-alert-icon-surface')).not.toHaveStyle({
      color: 'rgb(244, 67, 54)',
    })
    expect(screen.getAllByTestId('system-service-health-chip-surface')[0]).not.toHaveStyle({
      borderColor: 'rgb(76, 175, 80)',
    })
  })

  it('uses theme-aware embedded chart and diagnosis colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    fireEvent.click(screen.getByRole('tab', { name: '接口日志' }))
    const apiLogChart = await screen.findByTestId('system-api-log-pie-surface')
    await waitFor(() => expect(apiLogChart.textContent).toContain('成功'))
    expect(apiLogChart.textContent).not.toContain('#4caf50')
    expect(apiLogChart.textContent).not.toContain('#f44336')

    fireEvent.click(screen.getByRole('tab', { name: '性能监控' }))
    const performanceChart = await screen.findByTestId('system-performance-timeseries-surface')
    await waitFor(() => expect(performanceChart.textContent).toContain('P99'))
    expect(performanceChart.textContent).not.toContain('#4caf50')
    expect(performanceChart.textContent).not.toContain('#ff9800')
    expect(performanceChart.textContent).not.toContain('#f44336')

    fireEvent.click(screen.getByRole('button', { name: '一键诊断报告' }))
    expect(await screen.findByRole('dialog', { name: '一键诊断报告' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: '下载后端报告' })).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getAllByTestId('system-diagnose-status-icon-surface').length).toBeGreaterThan(0)
    })
    screen.getAllByTestId('system-diagnose-status-icon-surface').forEach((icon) => {
      expect(icon).not.toHaveStyle({ color: 'rgb(76, 175, 80)' })
      expect(icon).not.toHaveStyle({ color: 'rgb(255, 152, 0)' })
    })
  }, 20000)
})
