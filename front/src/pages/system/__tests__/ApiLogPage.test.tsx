import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ApiLogPage from '../ApiLogPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    apiLogList: vi.fn(),
    apiLogStats: vi.fn(),
  },
}))

describe('ApiLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.apiLogList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          apiPath: '/api/v1/auth/login',
          method: 'POST',
          statusCode: 200,
          responseTime: 120,
          ip: '127.0.0.1',
          errorMsg: null,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.apiLogStats).mockResolvedValue({
      totalCalls: 100,
      successCalls: 98,
      errorCalls: 2,
      avgResponseTime: 180,
      p99ResponseTime: 650,
    } as never)
  })

  it('loads api logs and renders stats cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ApiLogPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(systemApi.apiLogList).toHaveBeenCalledWith({ page: 0, rows: 20 })
      expect(systemApi.apiLogStats).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('总调用量')).toBeInTheDocument()
      expect(screen.getByText('/api/v1/auth/login')).toBeInTheDocument()
      expect(screen.getByText('P99(ms)')).toBeInTheDocument()
      expect(screen.getByText('650')).toBeInTheDocument()
      expect(screen.getByText(/本页只展示真实接口结果/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('api-log-page-workbench')).toHaveAttribute('data-contract-scope', 'system-api-log-readonly-observability')
    expect(screen.getByTestId('api-log-page-workbench')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
    expect(screen.getByTestId('api-log-page-workbench')).toHaveAttribute('data-no-detail-fetch', 'true')
    expect(screen.getByTestId('api-log-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/api-log/export')
    expect(screen.getByTestId('api-log-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/api-log/get')
    expect(screen.getByTestId('api-log-source-contract')).toHaveAttribute('data-query-field-map', 'apiPath->apiName|statusCode->status')
    expect(screen.getByTestId('api-log-source-contract')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
    expect(screen.getByTestId('api-log-grid-contract')).toHaveAttribute('data-no-local-api-log-fallback', 'true')
    expect(screen.getByTestId('api-log-grid-contract')).toHaveAttribute('data-no-detail-fetch', 'true')
  })

  it('renders wrapped api log payloads', async () => {
    vi.mocked(systemApi.apiLogList).mockResolvedValue({
      records: [
        {
          id: 2,
          apiPath: '/api/v1/ai/chat-stream',
          method: 'POST',
          statusCode: 500,
          responseTime: 3200,
          ip: '127.0.0.1',
          errorMsg: 'stream closed',
        },
      ],
      totalElements: 1,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ApiLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('/api/v1/ai/chat-stream')).toBeInTheDocument()
    expect(screen.getByText('stream closed')).toBeInTheDocument()
    expect(screen.getByText('P99(ms)')).toBeInTheDocument()
  })

  it('applies api path and status search through backend list contract', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ApiLogPage />
      </MemoryRouter>,
    )

    fireEvent.change(await screen.findByLabelText('接口路径'), { target: { value: '/api/v1/system' } })
    fireEvent.change(screen.getByLabelText('状态码'), { target: { value: '500' } })
    fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => {
      expect(systemApi.apiLogList).toHaveBeenLastCalledWith(expect.objectContaining({
        page: 0,
        rows: 20,
        apiPath: '/api/v1/system',
        statusCode: 500,
      }))
    })
    expect(screen.getByTestId('api-log-search-contract')).toHaveAttribute('data-applied-path', '/api/v1/system')
    expect(screen.getByTestId('api-log-search-contract')).toHaveAttribute('data-applied-status', '500')
    expect(screen.getByTestId('api-log-search-contract')).toHaveAttribute('data-no-local-search-filter', 'true')
  })

  it('shows stats and list errors independently without local fallbacks', async () => {
    vi.mocked(systemApi.apiLogList).mockRejectedValue(new Error('list down') as never)
    vi.mocked(systemApi.apiLogStats).mockRejectedValue(new Error('stats down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ApiLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('api-log-list-error')).toHaveAttribute('data-no-local-api-log-fallback', 'true')
    expect(await screen.findByTestId('api-log-stats-error')).toHaveAttribute('data-no-local-stats-fallback', 'true')
    expect(screen.getByTestId('api-log-grid-contract')).toHaveAttribute('data-row-count', '0')
  })
})
