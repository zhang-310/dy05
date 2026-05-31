import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import userEvent from '@testing-library/user-event'
import ScriptsPage from '../ScriptsPage'
import { liveApi } from '@/api/live'

vi.mock('@/api/live', () => ({
  liveApi: {
    scriptSearch: vi.fn(),
    scriptSave: vi.fn(),
    scriptDelete: vi.fn(),
    scriptUpdateExecuted: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

const scripts = [
  {
    id: 21,
    sessionId: 18,
    scriptTitle: '修护精华槽位',
    requirement: '修护精华槽位',
    scriptContent: '今晚主推修护精华',
    scriptType: '产品介绍',
    sequenceNo: 2,
    sortOrder: 2,
    durationLimitSec: 90,
    duration: 90,
    executed: 0,
    aiGenerated: true,
    violationChecked: false,
    createTime: '2026-05-22T20:00:00',
  },
  {
    id: 22,
    sessionId: 18,
    scriptTitle: '成交提醒',
    requirement: '成交提醒',
    scriptContent: '最后一轮库存提醒',
    scriptType: '促单',
    sequenceNo: 3,
    sortOrder: 3,
    durationLimitSec: 60,
    duration: 60,
    executed: 1,
    aiGenerated: false,
    violationChecked: true,
    createTime: '2026-05-22T20:05:00',
  },
]

describe('ScriptsPage', () => {
  async function waitForScriptsLoaded() {
    await waitFor(() => {
      expect(screen.getByTestId('live-scripts-workbench')).toHaveAttribute('data-row-count', '2')
      expect(screen.getByTestId('live-scripts-workbench')).toHaveAttribute('data-total', '2')
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(liveApi.scriptSearch).mockResolvedValue({
      total: 2,
      list: scripts,
      pageNum: 0,
      pageSize: 20,
    })
    vi.mocked(liveApi.scriptSave).mockResolvedValue(21)
    vi.mocked(liveApi.scriptDelete).mockResolvedValue(undefined)
    vi.mocked(liveApi.scriptUpdateExecuted).mockResolvedValue(undefined)
  })

  it('renders normalized real scripts and data source diagnostics', async () => {
    renderWithProviders(<ScriptsPage />)

    expect(await screen.findByText('修护精华槽位')).toBeInTheDocument()
    expect(screen.getByText('今晚主推修护精华')).toBeInTheDocument()
    expect(screen.getByTestId('live-scripts-workbench')).toHaveAttribute('data-contract-scope', 'live-script-management')
    expect(screen.getByTestId('live-scripts-workbench')).toHaveAttribute('data-ready-endpoints', '/live/script/search|/live/script/save|/live/script/delete|/live/script/executed')
    expect(screen.getByTestId('live-scripts-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('sse-generate-full'))
    expect(screen.getByTestId('live-scripts-contract-alert')).toHaveAttribute('data-no-single-segment-ai-generate', 'true')
    expect(screen.getByTestId('live-scripts-contract-alert')).toHaveAttribute('data-no-sse-generate', 'true')
    expect(screen.getByTestId('live-scripts-contract-alert')).toHaveAttribute('data-no-shortvideo-export', 'true')
    expect(screen.getByTestId('live-scripts-contract-alert')).toHaveAttribute('data-no-product-selection', 'true')
    expect(screen.getByTestId('live-scripts-table-surface')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.queryByRole('button', { name: /导出/ })).not.toBeInTheDocument()
    expect(screen.getByText(/\/live\/script\/search 支持 sessionId、scriptType、keyword、executed 真实筛选/)).toBeInTheDocument()
    expect(screen.getByText('筛选命中话术')).toBeInTheDocument()
    expect(screen.getByText('当前页已执行')).toBeInTheDocument()
  })

  it('sends supported search filters without stale page reload', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ScriptsPage />)

    await waitForScriptsLoaded()
    await user.type(screen.getByLabelText('场次ID'), '18')
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '话术类型' }))
    fireEvent.click(screen.getByRole('option', { name: '产品介绍' }))
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '执行状态' }))
    fireEvent.click(screen.getByRole('option', { name: '已执行' }))
    await user.type(screen.getByLabelText('关键词'), '修护')
    await user.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => {
      expect(liveApi.scriptSearch).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        sessionId: 18,
        scriptType: '产品介绍',
        keyword: '修护',
        executed: 1,
      })
    })
  })

  it('saves through real backend payload and keeps title as requirement', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ScriptsPage />)

    await waitForScriptsLoaded()
    await user.click(screen.getByRole('button', { name: '新建话术' }))
    const dialog = screen.getByRole('dialog')

    await user.type(within(dialog).getByRole('textbox', { name: /场次ID/ }), '18')
    await user.type(within(dialog).getByRole('textbox', { name: /槽位标题\/需求/ }), '福利提醒')
    await user.type(within(dialog).getByRole('textbox', { name: /话术内容/ }), '现在下单送旅行装')
    await user.clear(within(dialog).getByLabelText('排序'))
    await user.type(within(dialog).getByLabelText('排序'), '5')
    await user.clear(within(dialog).getByLabelText('时长(秒)'))
    await user.type(within(dialog).getByLabelText('时长(秒)'), '45')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(liveApi.scriptSave).toHaveBeenCalledWith({
        sessionId: 18,
        scriptTitle: '福利提醒',
        scriptContent: '现在下单送旅行装',
        scriptType: '产品介绍',
        requirement: '福利提醒',
        sequenceNo: 5,
        durationLimitSec: 45,
      })
    })
  }, 20000)

  it('shows save error inside dialog and keeps it open', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.scriptSave).mockRejectedValue(new Error('save down'))
    renderWithProviders(<ScriptsPage />)

    await screen.findByText('修护精华槽位')
    await user.click(screen.getAllByLabelText('编辑')[0])
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByText(/\/live\/script\/save：save down/)).toBeInTheDocument()
    expect(within(dialog).getByTestId('live-scripts-save-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getByTestId('live-scripts-save-dialog')).toHaveAttribute('data-no-ai-generate', 'true')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  }, 20000)

  it('updates executed status with explicit executed value and shows failures', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.scriptUpdateExecuted).mockRejectedValueOnce(new Error('executed down'))
    renderWithProviders(<ScriptsPage />)

    await screen.findByText('修护精华槽位')
    await user.click(screen.getAllByRole('button', { name: '标记已执行' })[0])

    await waitFor(() => {
      expect(liveApi.scriptUpdateExecuted).toHaveBeenCalledWith({ id: 21, executed: 1 })
    })
    expect(await screen.findByText(/\/live\/script\/executed：executed down/)).toBeInTheDocument()
    expect(screen.getByTestId('live-scripts-executed-error')).toHaveAttribute('data-no-local-executed-on-error', 'true')
  }, 20000)

  it('confirms delete and keeps page row on delete failure', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.scriptDelete).mockRejectedValue(new Error('delete down'))
    renderWithProviders(<ScriptsPage />)

    await screen.findByText('修护精华槽位')
    await user.click(screen.getAllByLabelText('删除')[0])
    await user.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => expect(liveApi.scriptDelete).toHaveBeenCalledWith(21))
    expect(await screen.findByText(/\/live\/script\/delete：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('live-scripts-delete-error')).toHaveAttribute('data-no-local-delete-on-error', 'true')
    expect(screen.getByText('修护精华槽位')).toBeInTheDocument()
  })

  it('shows empty state without mock fallback', async () => {
    vi.mocked(liveApi.scriptSearch).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    })

    renderWithProviders(<ScriptsPage />)

    expect(await screen.findByText('还没有直播话术')).toBeInTheDocument()
    expect(screen.getByTestId('live-scripts-empty')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.getByText('可以先在场次工作台生成完整话术，也可以手工新建单条话术。')).toBeInTheDocument()
  })

  it('shows load failure source without static script fallback', async () => {
    vi.mocked(liveApi.scriptSearch).mockRejectedValue(new Error('search down'))

    renderWithProviders(<ScriptsPage />)

    expect(await screen.findByTestId('live-scripts-load-error')).toHaveTextContent('/live/script/search：search down')
    expect(screen.getByTestId('live-scripts-load-error')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.queryByText('修护精华槽位')).not.toBeInTheDocument()
  })
})
