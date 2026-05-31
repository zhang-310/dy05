import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import OperationLogPage from '../OperationLogPage'
import { logApi } from '@/api/log'

vi.mock('@/api/log', () => ({
  logApi: {
    list: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('OperationLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(logApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          username: 'admin',
          module: 'auth',
          action: '登录',
          requestMethod: 'POST',
          requestUri: '/api/v1/auth/login',
          ip: '127.0.0.1',
          durationMs: 42,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads operation logs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OperationLogPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(logApi.list).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        username: undefined,
        module: undefined,
        action: undefined,
        status: undefined,
        startTime: undefined,
        endTime: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('登录')).toBeInTheDocument()
      expect(screen.getByText('/api/v1/auth/login')).toBeInTheDocument()
      expect(screen.getByText('平均耗时')).toBeInTheDocument()
    })

    expect(screen.getByTestId('operation-log-page-workbench')).toHaveAttribute('data-contract-scope', 'log-operation-readonly-export')
    expect(screen.getByTestId('operation-log-page-workbench')).toHaveAttribute('data-ready-endpoints', '/log/operation/page|/log/operation/export')
    expect(screen.getByTestId('operation-log-source-contract')).toHaveAttribute('data-no-local-operation-log-fallback', 'true')
    expect(screen.getByTestId('operation-log-grid-contract')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('operation-log-search-contract')).toHaveAttribute('data-no-client-side-filter-only', 'true')
    expect(screen.getByTestId('operation-log-export-contract')).toHaveAttribute('data-query-rows', '2000')
  })

  it('renders wrapped operation log payloads', async () => {
    vi.mocked(logApi.list).mockResolvedValue({
      data: {
        records: [
          {
            id: 2,
            username: 'ops',
            module: 'ai',
            action: '流式对话',
            requestUri: '/api/v1/agent/chat-stream',
            requestMethod: 'POST',
            durationMs: 1800,
            status: 0,
          },
        ],
        totalElements: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <OperationLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('ops')).toBeInTheDocument()
    expect(screen.getByText('流式对话')).toBeInTheDocument()
    expect(screen.getByText('/api/v1/agent/chat-stream')).toBeInTheDocument()
    expect(screen.getByText('失败请求')).toBeInTheDocument()
  })

  it('shows operation export endpoint error and keeps current filters', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue({ ok: false, status: 500 } as Response)

    renderWithProviders(
      <MemoryRouter>
        <OperationLogPage />
      </MemoryRouter>,
    )

    await screen.findByText('admin')
    fireEvent.change(screen.getByLabelText('操作人'), { target: { value: 'admin' } })
    fireEvent.click(screen.getByRole('button', { name: '查询' }))
    fireEvent.click(screen.getAllByRole('button', { name: '导出' })[0])

    expect(await screen.findByText(/\/log\/operation\/export 导出失败：HTTP 500/)).toBeInTheDocument()
    expect(screen.getByTestId('operation-log-export-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('operation-log-export-error')).toHaveAttribute('data-query-username', 'admin')
    expect(screen.getByTestId('operation-log-export-error')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
    expect(screen.getByLabelText('操作人')).toHaveValue('admin')
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/log/operation/export', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        page: 0,
        rows: 2000,
        username: 'admin',
        module: undefined,
        action: undefined,
        status: undefined,
        startTime: undefined,
        endTime: undefined,
      }),
    }))
  })
})
