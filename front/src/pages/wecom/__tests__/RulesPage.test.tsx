import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import RulesPage from '../RulesPage'
import { wecomApi } from '@/api/wecom'

vi.mock('@/api/wecom', () => ({
  wecomApi: {
    list: vi.fn(),
    ruleList: vi.fn(),
    ruleSave: vi.fn(),
    ruleDelete: vi.fn(),
    ruleUpdateStatus: vi.fn(),
    manualPush: vi.fn(),
    templateList: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('RulesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [{ id: 1, robotName: '告警机器人' }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(wecomApi.ruleList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotId: 1,
          ruleName: '开播提醒',
          triggerType: 'live_start',
          triggerConfig: '',
          messageTemplate: '直播开始啦',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 100,
    } as never)
    vi.mocked(wecomApi.templateList).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 100,
    } as never)
  })

  it('loads rules and renders rule card content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RulesPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(wecomApi.ruleList).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getAllByText('开播提醒').length).toBeGreaterThan(0)
      expect(screen.getByText('新增规则')).toBeInTheDocument()
    })
  })
})
