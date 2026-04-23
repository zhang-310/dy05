import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import AttributionPage from '../AttributionPage'
import request from '@/utils/request'

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts-mock">ECharts</div>,
}))

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('AttributionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(request.post).mockImplementation((url: string) => {
      if (url === '/attribution/summary') {
        return Promise.resolve([
          { channel: 'live', gmv: 100000, orders: 120, conversionRate: 0.08, contribution: 1, momChange: 12 },
        ] as never)
      }
      if (url === '/attribution/trend') {
        return Promise.resolve([
          { date: '2026-04-10', channel: 'live', gmv: 100000 },
        ] as never)
      }
      return Promise.resolve([] as never)
    })
  })

  it('renders title and channel attribution section', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AttributionPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '归因分析 — 增长决策中心' })).toBeInTheDocument()

    await waitFor(() => {
      expect(request.post).toHaveBeenCalledWith('/attribution/summary', { days: 30 })
      expect(request.post).toHaveBeenCalledWith('/attribution/trend', { days: 30 })
    })

    await waitFor(() => {
      expect(screen.getByText('GMV 渠道漏斗')).toBeInTheDocument()
      expect(screen.getByText('渠道详情')).toBeInTheDocument()
      expect(screen.getByText('直播带货')).toBeInTheDocument()
    })
  })
})
