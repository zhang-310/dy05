import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ScriptTemplatePage from '../ScriptTemplatePage'
import { scriptApi } from '@/api/script'

vi.mock('@/api/script', () => ({
  scriptApi: {
    templateSearch: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ScriptTemplatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.templateSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          templateName: '直播开场模板',
          templateContent: '欢迎来到直播间',
          scene: '开场',
          industry: '护肤',
          tags: '直播,开场',
          useCount: 9,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('renders title and loads script templates', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ScriptTemplatePage />
      </MemoryRouter>,
    )

    expect(screen.getByText('话术模板')).toBeInTheDocument()

    await waitFor(() => {
      expect(scriptApi.templateSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('直播开场模板')).toBeInTheDocument()
      expect(screen.getByText('开场')).toBeInTheDocument()
    })
  })
})
