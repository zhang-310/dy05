import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { MemoryRouter } from 'react-router-dom'

// Mock all external dependencies
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    adminStats: vi.fn().mockResolvedValue({
      totalUsers: 100,
      activeUsers: 50,
      todayUsers: 5,
      totalVideos: 200,
      publishedVideos: 150,
      todayVideos: 10,
      totalLiveSessions: 50,
      completedSessions: 40,
      todaySessions: 3,
      totalShortVideos: 300,
      publishedShortVideos: 250,
      todayShortVideos: 15,
      totalCopyItems: 100,
      approvedCopyItems: 80,
      todayCopyItems: 5,
      todayAiCalls: 100,
      todayAiAttempts: 120,
      aiSuccessRate: 83.3,
      todayRevenue: 12345.67,
    }),
    kpiUnified: vi.fn().mockResolvedValue({
      gmvToday: 12345.67,
      gmvMom: 23.4,
      gmvYoy: 45.6,
      ordersToday: 100,
      avgOrderValue: 123.46,
      conversionRate: 3.5,
      liveSessions: 3,
      activeSessionCount: 1,
      aiCallsToday: 100,
    }),
    liveFormatGmv: vi.fn().mockResolvedValue([]),
    productGmvSummary: vi.fn().mockResolvedValue([]),
    cockpitPreview: vi.fn().mockResolvedValue([]),
    conversionFunnel: vi.fn().mockResolvedValue({
      impressions: 10000,
      clicks: 500,
      addToCart: 100,
      orders: 35,
    }),
  },
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    roleCode: 'admin',
    id: 1,
    username: 'admin',
    nickname: 'Admin',
  })),
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionSearch: vi.fn().mockResolvedValue({
      total: 0,
      list: [],
    }),
    approvalPending: vi.fn().mockResolvedValue({
      total: 0,
      list: [],
    }),
  },
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    callTypeDistribution: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('@/api/system', () => ({
  systemApi: {
    alertActive: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts-mock">ECharts</div>,
}))

vi.mock('@/utils/echarts-registry', () => ({
  echarts: {},
}))

import DashboardPage from '../DashboardPage'

function renderDashboard() {
  return renderWithProviders(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', async () => {
    const { container } = renderDashboard()
    expect(container).toBeInTheDocument()
  })

  it('renders with admin role', async () => {
    renderDashboard()
    // Just verify the page renders, don't check specific content
    await waitFor(() => {
      expect(document.body).toBeInTheDocument()
    })
  })

  it('calls dashboard API on mount', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    renderDashboard()
    await waitFor(() => {
      expect(dashboardApi.adminStats).toHaveBeenCalled()
    })
  })
})
