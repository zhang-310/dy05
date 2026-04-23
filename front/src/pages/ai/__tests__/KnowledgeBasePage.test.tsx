import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import KnowledgeBasePage from '../KnowledgeBasePage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
    kbCreate: vi.fn(),
    kbDelete: vi.fn(),
    docDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('KnowledgeBasePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.kbList).mockResolvedValue([
      {
        id: 1,
        kbName: '护肤知识库',
        description: '护肤话术与产品知识',
        totalDocuments: 12,
        status: 1,
        createTime: '2026-04-10 10:00:00',
      },
    ] as never)
  })

  it('loads knowledge bases and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    expect(screen.getByText('知识库')).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.kbList).toHaveBeenCalledWith({ page: 0, rows: 20, name: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('护肤知识库')).toBeInTheDocument()
      expect(screen.getByText('12')).toBeInTheDocument()
    })
  })
})
