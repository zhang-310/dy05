import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders } from '@/test/utils'
import { SessionInfoTab } from './SessionInfoTab'
import { ReadinessSteps, StatusChip, TimeTip } from './LiveSessionShared'
import {
  batchSortProducts,
  deleteLiveProduct,
  saveLiveProduct,
} from '@/api/live'
import { searchProducts } from '@/api/product'

vi.mock('@/api/live', () => ({
  saveLiveProduct: vi.fn(),
  deleteLiveProduct: vi.fn(),
  batchSortProducts: vi.fn(),
}))

vi.mock('@/api/product', () => ({
  searchProducts: vi.fn(),
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(<AppThemeProvider>{ui}</AppThemeProvider>)
}

const products = [
  {
    id: 501,
    productId: 101,
    productName: '屏障修护精华',
    price: 129,
    position: 1,
    scriptSource: 'product',
    saleQuantity: 16,
    revenue: 2064,
    createTime: '2026-05-22T20:00:00',
  },
  {
    id: 502,
    productId: 102,
    productName: '敏感肌面霜',
    price: 169,
    position: 2,
    scriptSource: 'session',
    saleQuantity: 9,
    revenue: 1521,
    createTime: '2026-05-22T20:30:00',
  },
]

describe('SessionInfoTab and LiveSessionShared contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(saveLiveProduct).mockResolvedValue(888 as never)
    vi.mocked(deleteLiveProduct).mockResolvedValue(undefined as never)
    vi.mocked(batchSortProducts).mockResolvedValue(undefined as never)
    vi.mocked(searchProducts).mockResolvedValue({
      list: [
        { id: 101, productName: '屏障修护精华', price: 129 },
        { id: 103, productName: '舒缓喷雾', price: 89 },
      ],
      total: 2,
      page: 0,
      rows: 50,
    } as never)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('marks shared session derived surfaces without direct APIs', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-22T20:00:00+08:00'))

    renderDark(
      <>
        <StatusChip status={1} />
        <ReadinessSteps row={{ productCount: 2, scriptCount: 0 }} />
        <TimeTip scheduledTime="2026-05-22T20:20:00+08:00" />
      </>,
    )

    const chip = screen.getByTestId('live-session-status-chip')
    expect(chip).toHaveAttribute('data-contract-scope', 'live-session-status-chip-derived')
    expect(chip).toHaveAttribute('data-status', '1')
    expect(chip).toHaveAttribute('data-status-label', '直播中')
    expect(chip).toHaveAttribute('data-live-pulse', 'true')
    expect(chip).toHaveAttribute('data-no-direct-api', 'true')

    const steps = screen.getByTestId('live-session-readiness-steps')
    expect(steps).toHaveAttribute('data-contract-scope', 'live-session-readiness-steps-derived')
    expect(steps).toHaveAttribute('data-product-count', '2')
    expect(steps).toHaveAttribute('data-script-count', '0')
    expect(steps).toHaveAttribute('data-progress', '1')
    expect(screen.getAllByTestId('live-session-readiness-step').map(step => step.getAttribute('data-step-state'))).toEqual(['done', 'current', 'pending'])

    const tip = screen.getByTestId('live-session-time-tip')
    expect(tip).toHaveAttribute('data-contract-scope', 'live-session-time-tip-derived')
    expect(tip).toHaveAttribute('data-minutes-left', '20')
    expect(tip).toHaveAttribute('data-no-direct-api', 'true')
  })

  it('marks product maintenance table contract and moves products through the live sort endpoint', async () => {
    const toast = vi.fn()
    const onRefresh = vi.fn()

    renderDark(<SessionInfoTab products={products} sessionId={18} onRefresh={onRefresh} toast={toast} />)

    const root = screen.getByTestId('session-info-tab-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-session-info-product-maintenance')
    expect(root).toHaveAttribute('data-ready-endpoints', '/live/product/save|/live/product/delete|/live/product/batch-sort|/product/search')
    expect(root).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(root).toHaveAttribute('data-product-count', '2')

    expect(screen.getByTestId('session-info-product-table')).toHaveAttribute('data-row-count', '2')
    const rows = screen.getAllByTestId('session-info-product-row')
    expect(rows[0]).toHaveAttribute('data-product-id', '101')
    expect(rows[0]).toHaveAttribute('data-script-source', 'product')

    const moveDown = screen.getAllByTestId('session-info-product-move-down-button')[0]
    expect(moveDown).toHaveAttribute('data-contract-source', '/live/product/batch-sort')
    expect(moveDown).toHaveAttribute('data-disabled-reason', 'ready')
    fireEvent.click(moveDown)

    await waitFor(() => {
      expect(batchSortProducts).toHaveBeenCalledWith({ sessionId: 18, productIds: [502, 501] })
    })
    expect(toast).toHaveBeenCalledWith('排序已更新', 'success')
    expect(onRefresh).toHaveBeenCalledTimes(1)
  })

  it('loads product library, filters already-added products and delegates add to live save', async () => {
    const toast = vi.fn()
    const onRefresh = vi.fn()

    renderDark(<SessionInfoTab products={[products[0]]} sessionId={18} onRefresh={onRefresh} toast={toast} />)

    fireEvent.click(screen.getByTestId('session-info-add-open-button'))

    const dialog = await screen.findByTestId('session-info-add-product-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-session-info-add-product-dialog')
    expect(dialog).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(searchProducts).toHaveBeenCalledWith({ page: 0, rows: 50, productName: undefined })

    await waitFor(() => {
      expect(screen.getByTestId('session-info-product-library-list')).toHaveAttribute('data-row-count', '1')
    })
    expect(within(screen.getByTestId('session-info-product-library-list')).queryByText('屏障修护精华')).not.toBeInTheDocument()

    fireEvent.change(screen.getByTestId('session-info-product-search-input'), { target: { value: '喷雾' } })
    fireEvent.click(screen.getByTestId('session-info-product-search-button'))
    await waitFor(() => {
      expect(searchProducts).toHaveBeenLastCalledWith({ page: 0, rows: 50, productName: '喷雾' })
    })

    fireEvent.click(screen.getByTestId('session-info-product-library-row'))
    await waitFor(() => {
      expect(saveLiveProduct).toHaveBeenCalledWith({
        sessionId: 18,
        productId: 103,
        productName: '舒缓喷雾',
        position: 2,
      })
    })
    expect(toast).toHaveBeenCalledWith('添加成功', 'success')
    expect(onRefresh).toHaveBeenCalledTimes(1)
  })

  it('shows product search failures as endpoint errors instead of fake empty state', async () => {
    vi.mocked(searchProducts).mockRejectedValueOnce(new Error('ConnectException'))

    renderDark(<SessionInfoTab products={[]} sessionId={18} onRefresh={vi.fn()} toast={vi.fn()} />)

    expect(screen.getByTestId('session-info-product-empty-state')).toHaveAttribute('data-no-local-product-fallback', 'true')
    fireEvent.click(screen.getByTestId('session-info-add-open-button'))

    const error = await screen.findByTestId('session-info-product-search-error')
    expect(error).toHaveAttribute('data-contract-source', '/product/search')
    expect(error).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(error).toHaveTextContent('ConnectException')
    expect(screen.getByTestId('session-info-product-library-empty')).toHaveTextContent('商品库搜索失败，请重试')
  })

  it('delegates delete to live product delete and exposes loading contract', async () => {
    const toast = vi.fn()
    const onRefresh = vi.fn()

    renderDark(<SessionInfoTab products={products} sessionId={18} onRefresh={onRefresh} toast={toast} />)

    fireEvent.click(screen.getAllByTestId('session-info-product-delete-open-button')[0])

    const dialog = await screen.findByTestId('session-info-delete-product-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-session-info-delete-product-dialog')
    expect(dialog).toHaveAttribute('data-product-id', '501')

    const confirm = screen.getByTestId('session-info-delete-confirm-button')
    expect(confirm).toHaveAttribute('data-contract-source', '/live/product/delete')
    expect(confirm).toHaveAttribute('data-disabled-reason', 'ready')
    fireEvent.click(confirm)

    await waitFor(() => {
      expect(deleteLiveProduct).toHaveBeenCalledWith(501)
    })
    expect(toast).toHaveBeenCalledWith('移除成功', 'success')
    expect(onRefresh).toHaveBeenCalledTimes(1)
  })
})
