import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ExternalApiHealthPage from '../ExternalApiHealthPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    externalApiList: vi.fn(),
    externalApiHealthStatus: vi.fn(),
    externalApiProbe: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('ExternalApiHealthPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.externalApiList).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 1,
          providerCode: 'openai',
          providerName: 'OpenAI',
          category: 'llm',
          baseUrl: 'https://api.openai.com',
          isEnabled: true,
          healthStatus: 'healthy',
          avgLatencyMs: 120,
          successRatePct: 0.995,
          lastHealthCheck: '2026-05-20 12:00:00',
        },
        {
          id: 2,
          providerCode: 'douyin',
          providerName: 'Douyin',
          category: 'data',
          baseUrl: 'https://open.douyin.com',
          isEnabled: true,
          healthStatus: 'down',
          avgLatencyMs: 1500,
          successRatePct: 0,
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.externalApiHealthStatus).mockResolvedValue(undefined as never)
    vi.mocked(systemApi.externalApiProbe).mockResolvedValue({ providerCode: 'openai', status: 'healthy' } as never)
  })

  it('renders scheduler-based health status and normalizes success rate ratio', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiHealthPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '外部 API 健康监控' })).toBeInTheDocument()
    expect(await screen.findByText('OpenAI')).toBeInTheDocument()
    expect(screen.getByText('Douyin')).toBeInTheDocument()
    expect(screen.getByText(/后端定时任务默认每 5 分钟/)).toBeInTheDocument()
    expect(screen.getByText('99.5%')).toBeInTheDocument()
    expect(screen.getByText('有 1 个外部 API 连接异常，请及时处理')).toBeInTheDocument()
    expect(screen.getByTestId('external-api-health-page-workbench')).toHaveAttribute('data-contract-scope', 'system-external-api-health-monitoring')
    expect(screen.getByTestId('external-api-health-page-workbench')).toHaveAttribute('data-no-local-health-fallback', 'true')
    expect(screen.getByTestId('external-api-health-page-workbench')).toHaveAttribute('data-no-local-health-mutation', 'true')
    expect(screen.getByTestId('external-api-health-page-workbench').getAttribute('data-ready-endpoints')).toContain('/system/external-api/probe')
    expect(screen.getByTestId('external-api-health-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/external-api/get-secret')
    expect(screen.getByTestId('external-api-health-page-workbench').getAttribute('data-unsupported-endpoints')).not.toContain('/system/external-api/probe')
    expect(screen.getByTestId('external-api-health-source-contract')).toHaveAttribute('data-no-browser-direct-provider-ping', 'true')
    expect(screen.getByTestId('external-api-health-source-contract')).toHaveAttribute('data-no-local-health-mutation', 'true')
  })

  it('triggers backend probe instead of pretending to ping from browser', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiHealthPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByLabelText('立即探测 OpenAI'))

    await waitFor(() => {
      expect(systemApi.externalApiProbe).toHaveBeenCalledWith('openai')
      expect(toast).toHaveBeenCalledWith('后端探测已完成', 'success')
    })
    expect(screen.getAllByTestId('external-api-health-mark-unknown')[0]).toHaveAttribute('data-no-local-health-mutation', 'true')
    expect(screen.getAllByTestId('external-api-health-card')[0]).toHaveAttribute('data-no-local-health-mutation', 'true')
  })

  it('renders wrapped external api config payloads', async () => {
    vi.mocked(systemApi.externalApiList).mockResolvedValue({
      data: {
        rows: [
          {
            id: 3,
            providerCode: 'ollama',
            providerName: 'Ollama',
            isEnabled: true,
            healthStatus: 'degraded',
            avgLatencyMs: 1800,
            successRatePct: 0.75,
          },
        ],
        totalElements: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ExternalApiHealthPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Ollama')).toBeInTheDocument()
    expect(screen.getByText('75.0%')).toBeInTheDocument()
    expect(screen.getByText('有 1 个外部 API 连接异常，请及时处理')).toBeInTheDocument()
  })

  it('keeps row context when health mark fails', async () => {
    vi.mocked(systemApi.externalApiProbe).mockRejectedValue(new Error('probe denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <ExternalApiHealthPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByLabelText('立即探测 OpenAI'))

    expect(await screen.findByTestId('external-api-health-operation-error')).toHaveAttribute('data-provider-code', 'openai')
    expect(screen.getByTestId('external-api-health-operation-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getAllByTestId('external-api-health-card')[0]).toHaveAttribute('data-health-status', 'healthy')
  })

  it('shows load error and empty state without synthetic provider cards', async () => {
    vi.mocked(systemApi.externalApiList).mockRejectedValueOnce(new Error('list down') as never)

    const { unmount } = renderWithProviders(
      <MemoryRouter>
        <ExternalApiHealthPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('external-api-health-load-error')).toHaveAttribute('data-no-local-health-fallback', 'true')
    unmount()

    vi.mocked(systemApi.externalApiList).mockResolvedValueOnce({ total: 0, list: [], pageNum: 0, pageSize: 20 } as never)
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiHealthPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('external-api-health-empty')).toHaveAttribute('data-no-local-health-fallback', 'true')
    expect(screen.getByTestId('external-api-health-empty')).toHaveAttribute('data-no-synthetic-provider-card', 'true')
    expect(screen.queryAllByTestId('external-api-health-card')).toHaveLength(0)
  })
})
