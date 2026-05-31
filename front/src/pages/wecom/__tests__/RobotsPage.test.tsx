import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import RobotsPage from '../RobotsPage'
import { wecomApi } from '@/api/wecom'

vi.mock('@/api/wecom', () => ({
  wecomApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    updateStatus: vi.fn(),
    push: vi.fn(),
    logList: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('RobotsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotName: '日报机器人',
          webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads robots and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RobotsPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(wecomApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('日报机器人')).toBeInTheDocument()
      expect(screen.getByText('新增机器人')).toBeInTheDocument()
      expect(screen.getByText(/qyapi\.weixin\.qq\.com\/\*\*\*abc/)).toBeInTheDocument()
    })
  })

  it('updates robot status through real update-status endpoint', async () => {
    vi.mocked(wecomApi.updateStatus).mockResolvedValue(undefined as never)

    renderWithProviders(
      <MemoryRouter>
        <RobotsPage />
      </MemoryRouter>,
    )

    const toggle = await screen.findByRole('checkbox')
    fireEvent.click(toggle)

    await waitFor(() => {
      expect(wecomApi.updateStatus).toHaveBeenCalledWith(1, 0)
    })
  })

  it('renders push log tab with explicit retry downgrade copy', async () => {
    vi.mocked(wecomApi.logList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 7,
          robotId: 1,
          robotName: '日报机器人',
          messageContent: '推送失败',
          status: 0,
          errorMessage: 'timeout',
          createTime: '2026-05-22 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <RobotsPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '推送日志' }))

    const logWorkbench = await screen.findByTestId('wecom-robot-log-workbench')
    expect(logWorkbench).toHaveAttribute('data-contract-status', 'degraded')
    expect(logWorkbench).toHaveAttribute('data-ready-endpoint', '/wecom/log/list')
    expect(logWorkbench).toHaveAttribute('data-unsupported-endpoint', '/wecom/log/retry')
    expect(screen.getByTestId('wecom-robot-log-retry-downgrade')).toHaveAttribute('data-contract-status', 'unsupported')
    expect(screen.getAllByTestId('wecom-robot-log-kpi-card').find(card => card.textContent?.includes('失败重发'))).toHaveAttribute('data-unsupported-endpoint', '/wecom/log/retry')
    expect(screen.getByText(/后端暂无 \/wecom\/log\/retry 接口/)).toBeInTheDocument()
    expect(await screen.findByText('推送失败')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '重发' })).not.toBeInTheDocument()
  })

  it('shows source endpoint when push or log list fails', async () => {
    vi.mocked(wecomApi.push).mockRejectedValue(new Error('push down'))
    vi.mocked(wecomApi.logList).mockRejectedValue(new Error('log down'))

    renderWithProviders(
      <MemoryRouter>
        <RobotsPage />
      </MemoryRouter>,
    )

    await screen.findByText('日报机器人')
    fireEvent.click(screen.getByRole('button', { name: '推送' }))
    const dialog = screen.getByRole('dialog', { name: '发送推送消息' })
    expect(screen.getByText(/robotId=1/)).toBeInTheDocument()
    expect(screen.getByText(/robotName=日报机器人/)).toBeInTheDocument()
    expect(screen.getByTestId('wecom-robot-push-contract')).toHaveAttribute('data-contract-endpoint', '/wecom/push')
    fireEvent.change(screen.getByRole('textbox', { name: '消息内容' }), { target: { value: '失败消息' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/\/wecom\/push 推送失败：push down/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/contentLength=4/).length).toBeGreaterThan(0)
    expect(dialog).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '发送推送消息' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('tab', { name: '推送日志' }))
    expect(await screen.findByText(/\/wecom\/log\/list 推送日志加载失败：log down/)).toBeInTheDocument()
  })

  it('renders capability cards with explicit retry downgrade', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RobotsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('日报机器人')).toBeInTheDocument()
    expect(screen.getByTestId('wecom-robot-page')).toHaveAttribute('data-unsupported-actions', 'log-retry')
    expect(screen.getByTestId('wecom-robot-workbench')).toHaveAttribute('data-enabled-count', '1')
    expect(screen.getAllByTestId('wecom-robot-capability-card')).toHaveLength(4)
    expect(screen.getAllByTestId('wecom-robot-capability-card').find(card => card.getAttribute('data-contract-action') === 'direct-push')).toHaveAttribute('data-contract-endpoint', '/wecom/push')
    expect(screen.getAllByTestId('wecom-robot-capability-card').find(card => card.getAttribute('data-contract-action') === 'log-retry')).toHaveAttribute('data-contract-status', 'unsupported')
    expect(screen.getAllByTestId('wecom-robot-kpi-card').find(card => card.textContent?.includes('直连推送'))).toHaveAttribute('data-source-endpoint', '/wecom/push')
    expect(screen.getAllByText('已接入').length).toBeGreaterThanOrEqual(3)
    expect(screen.getAllByText('显式降级').length).toBeGreaterThanOrEqual(1)
  })
})
