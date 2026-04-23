import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ExperimentsPage from '../ExperimentsPage'
import { abtestApi } from '@/api/abtest'

vi.mock('@/api/abtest', () => ({
  abtestApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    start: vi.fn(),
    pause: vi.fn(),
    stop: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ExperimentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(abtestApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          experimentName: '直播开场 A/B',
          description: '测试两种开场风格',
          status: 1,
          trafficSplit: 50,
          winnerVariantId: null,
          startTime: '2026-04-10 10:00:00',
          endTime: '',
          createTime: '2026-04-10 09:00:00',
          variants: [
            { id: 1, experimentId: 1, variantName: 'A', trafficRatio: 50, scriptStyle: 'warm', conversions: 12, exposures: 300, conversionRate: 0.04, isWinner: false },
            { id: 2, experimentId: 1, variantName: 'B', trafficRatio: 50, scriptStyle: 'direct', conversions: 18, exposures: 300, conversionRate: 0.06, isWinner: false },
          ],
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads experiment list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExperimentsPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'A/B 实验管理' })).toBeInTheDocument()

    await waitFor(() => {
      expect(abtestApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, experimentName: undefined, status: undefined })
    })

    await waitFor(() => {
      expect(screen.getByText('直播开场 A/B')).toBeInTheDocument()
      expect(screen.getByText('进行中')).toBeInTheDocument()
    })
  })
})
