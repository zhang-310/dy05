import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import SyncLogPage from '../SyncLogPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    syncLogList: vi.fn(),
  },
}))

describe('SyncLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.syncLogList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          syncType: 'douyin-video',
          status: 'SUCCESS',
          totalCount: 100,
          successCount: 98,
          failCount: 2,
          errorMessage: null,
          startTime: '2026-04-10 09:00:00',
          endTime: '2026-04-10 09:05:00',
          createTime: '2026-04-10 09:00:00',
          userId: 9,
          accountId: 18,
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads sync logs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SyncLogPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('同步日志')).toBeInTheDocument()

    await waitFor(() => {
      expect(systemApi.syncLogList).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('douyin-video')).toBeInTheDocument()
      expect(screen.getAllByText('成功').length).toBeGreaterThan(0)
      expect(screen.getByText(/详情抽屉展示的是落库字段/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('sync-log-page-workbench')).toHaveAttribute('data-contract-scope', 'system-sync-log-readonly-ledger')
    expect(screen.getByTestId('sync-log-page-workbench')).toHaveAttribute('data-no-client-side-sync-retry', 'true')
    expect(screen.getByTestId('sync-log-page-workbench')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
    expect(screen.getByTestId('sync-log-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/sync-log/retry')
    expect(screen.getByTestId('sync-log-source-contract')).toHaveAttribute('data-no-synthetic-detail-fetch', 'true')
    expect(screen.getByTestId('sync-log-source-contract')).toHaveAttribute('data-no-client-side-sync-retry', 'true')
    expect(screen.getByTestId('sync-log-grid-contract')).toHaveAttribute('data-no-local-sync-log-fallback', 'true')
    expect(screen.getByTestId('sync-log-grid-contract')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
  })

  it('opens drawer with persisted sync log details', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SyncLogPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByText('douyin-video'))

    expect(await screen.findByText('同步详情')).toBeInTheDocument()
    expect(screen.getByTestId('sync-log-detail-contract')).toHaveAttribute('data-contract-source', '/system/sync-log/list')
    expect(screen.getByTestId('sync-log-detail-contract')).toHaveAttribute('data-no-synthetic-detail-fetch', 'true')
    expect(screen.getByTestId('sync-log-detail-contract')).toHaveAttribute('data-no-client-side-sync-retry', 'true')
    expect(screen.getByText('用户 ID')).toBeInTheDocument()
    expect(screen.getByText('账号 ID')).toBeInTheDocument()
    expect(screen.getByText('9')).toBeInTheDocument()
    expect(screen.getByText('18')).toBeInTheDocument()
  })

  it('renders wrapped sync log payloads', async () => {
    vi.mocked(systemApi.syncLogList).mockResolvedValue({
      data: {
        rows: [
          {
            id: 2,
            syncType: 'douyin-account',
            status: 'FAILED',
            totalCount: 10,
            successCount: 7,
            failCount: 3,
            errorMessage: 'token expired',
          },
        ],
        totalRecords: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <SyncLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('douyin-account')).toBeInTheDocument()
    expect(screen.getAllByText('失败').length).toBeGreaterThan(0)
  })

  it('shows load error and empty state without local sync log fallback', async () => {
    vi.mocked(systemApi.syncLogList).mockRejectedValueOnce(new Error('sync down') as never)

    const { unmount } = renderWithProviders(
      <MemoryRouter>
        <SyncLogPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('sync-log-load-error')).toHaveAttribute('data-no-local-sync-log-fallback', 'true')
    expect(screen.getByTestId('sync-log-grid-contract')).toHaveAttribute('data-row-count', '0')
    unmount()

    vi.mocked(systemApi.syncLogList).mockResolvedValueOnce({ total: 0, list: [], pageNum: 0, pageSize: 20 } as never)
    renderWithProviders(
      <MemoryRouter>
        <SyncLogPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('sync-log-grid-contract')).toHaveAttribute('data-row-count', '0')
    })
    expect(screen.getByTestId('sync-log-page-workbench')).toHaveAttribute('data-no-local-sync-log-fallback', 'true')
  })
})
