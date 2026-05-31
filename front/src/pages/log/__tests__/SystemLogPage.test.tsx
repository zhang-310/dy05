import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import SystemLogPage from '../SystemLogPage'
import { logApi } from '@/api/log'

vi.mock('@/api/log', () => ({
  logApi: {
    systemList: vi.fn(),
  },
}))

describe('SystemLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(logApi.systemList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          module: 'system',
          eventType: 'startup',
          summary: '系统启动完成',
          detail: 'application started',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads system logs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SystemLogPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('系统日志')).toBeInTheDocument()

    await waitFor(() => {
      expect(logApi.systemList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        module: undefined,
        eventType: undefined,
        status: undefined,
        startTime: undefined,
        endTime: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('system')).toBeInTheDocument()
      expect(screen.getByText('startup')).toBeInTheDocument()
      expect(screen.getByText('系统启动完成')).toBeInTheDocument()
    })

    expect(screen.getByTestId('system-log-page-workbench')).toHaveAttribute('data-contract-scope', 'log-system-readonly-export')
    expect(screen.getByTestId('system-log-page-workbench')).toHaveAttribute('data-ready-endpoints', '/log/system/page|/log/system/export')
    expect(screen.getByTestId('system-log-source-contract')).toHaveAttribute('data-no-username-filter-query', 'true')
    expect(screen.getByTestId('system-log-filter-contract')).toHaveAttribute('data-no-client-side-filter-only', 'true')
    expect(screen.getByTestId('system-log-grid-contract')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('system-log-export-contract')).toHaveAttribute('data-query-rows', '2000')
  })

  it('renders wrapped system log payloads', async () => {
    vi.mocked(logApi.systemList).mockResolvedValue({
      items: [
        {
          id: 2,
          module: 'redis',
          eventType: 'warn',
          summary: '缓存命中率偏低',
          detail: 'hit rate 22%',
          status: 0,
        },
      ],
      totalCount: 1,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <SystemLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('redis')).toBeInTheDocument()
    expect(screen.getByText('warn')).toBeInTheDocument()
    expect(screen.getByText('缓存命中率偏低')).toBeInTheDocument()
    expect(screen.getByText('失败事件')).toBeInTheDocument()
  })

  it('exports through real system log endpoint and shows error in page', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue({ ok: false, status: 403 } as Response)

    renderWithProviders(
      <MemoryRouter>
        <SystemLogPage />
      </MemoryRouter>,
    )

    await screen.findByText('系统启动完成')
    fireEvent.mouseDown(screen.getByLabelText('模块'))
    fireEvent.click(await screen.findByRole('option', { name: '系统' }))
    fireEvent.mouseDown(screen.getByLabelText('事件类型'))
    fireEvent.click(await screen.findByRole('option', { name: '告警' }))
    fireEvent.mouseDown(screen.getByLabelText('结果'))
    fireEvent.click(await screen.findByRole('option', { name: '失败' }))
    fireEvent.click(screen.getAllByRole('button', { name: '导出' })[0])

    expect(await screen.findByText(/\/log\/system\/export 导出失败：HTTP 403/)).toBeInTheDocument()
    expect(screen.getByTestId('system-log-export-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('system-log-export-error')).toHaveAttribute('data-query-module', 'system')
    expect(screen.getByTestId('system-log-export-error')).toHaveAttribute('data-query-event-type', 'warn')
    expect(screen.getByTestId('system-log-export-error')).toHaveAttribute('data-query-status', '0')
    expect(screen.getByTestId('system-log-export-error')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/log/system/export', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        page: 0,
        rows: 2000,
        module: 'system',
        eventType: 'warn',
        status: 0,
        startTime: undefined,
        endTime: undefined,
      }),
    }))
    expect(JSON.parse(String(vi.mocked(globalThis.fetch).mock.calls[0][1]?.body))).not.toHaveProperty('username')
  })

  it('shows list endpoint errors without local system log fallback', async () => {
    vi.mocked(logApi.systemList).mockRejectedValue(new Error('/log/system/page down'))

    renderWithProviders(
      <MemoryRouter>
        <SystemLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('system-log-list-error')).toHaveAttribute('data-no-local-system-log-fallback', 'true')
    expect(screen.getByText('/log/system/page down')).toBeInTheDocument()
  })
})
