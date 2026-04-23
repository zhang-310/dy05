import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import KnowledgeEvolutionPage from '../KnowledgeEvolutionPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
    evolveTaskList: vi.fn(),
    evolveRoi: vi.fn(),
    scoreTrend: vi.fn(),
    evolveTaskTrigger: vi.fn(),
    knowledgeEvolutionAnalyze: vi.fn(),
    knowledgeEvolutionAutoOptimize: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('KnowledgeEvolutionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.kbList).mockResolvedValue([
      { id: 7, kbName: '范围测试库', description: '', totalDocuments: 0, status: 1, createTime: '' },
    ] as never)
    vi.mocked(aiApi.evolveTaskList).mockResolvedValue({ list: [], total: 0 } as never)
    vi.mocked(aiApi.evolveRoi).mockResolvedValue({
      newKnowledge: 1,
      avgScore: 80,
      totalRuns: 2,
      coveredDocs: 1,
    } as never)
    vi.mocked(aiApi.scoreTrend).mockResolvedValue([] as never)
    vi.mocked(aiApi.knowledgeEvolutionAnalyze).mockResolvedValue({
      analysisId: 'evol_test',
      degraded: true,
      readyForInclusion: [],
      needsOptimization: [],
      duplicatesDetected: [],
      readyForArchival: [],
    } as never)
    vi.mocked(aiApi.knowledgeEvolutionAutoOptimize).mockResolvedValue({ status: 'COMPLETED' } as never)
  })

  it('renders dashboard title and loads data for all KBs by default', async () => {
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('进化监控看板')).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.kbList).toHaveBeenCalledWith({ page: 0, rows: 200 })
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        kbId: undefined,
        taskType: undefined,
        status: undefined,
      })
      expect(aiApi.evolveRoi).toHaveBeenCalledWith({})
      expect(aiApi.scoreTrend).toHaveBeenCalledWith({ days: 7, kbId: undefined })
    })
  })

  it('refetches with kbId when knowledge base scope changes', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByLabelText('知识库范围')).toBeInTheDocument()
    })

    await user.click(screen.getByLabelText('知识库范围'))
    await user.click(await screen.findByRole('option', { name: '范围测试库' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith(expect.objectContaining({ page: 0, rows: 20, kbId: 7 }))
      expect(aiApi.evolveRoi).toHaveBeenCalledWith({ kbId: 7 })
      expect(aiApi.scoreTrend).toHaveBeenCalledWith({ days: 7, kbId: 7 })
    })
  })

  it('requests score trend with selected day window', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getAllByLabelText('趋势天数').length).toBeGreaterThan(0)
    })

    await user.click(screen.getAllByLabelText('趋势天数')[0])
    await user.click(await screen.findByRole('option', { name: '14 天' }))

    await waitFor(() => {
      expect(aiApi.scoreTrend).toHaveBeenCalledWith(expect.objectContaining({ days: 14, kbId: undefined }))
    })
  })

  it('filters task list by agent type', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByLabelText('Agent 类型')).toBeInTheDocument()
    })

    vi.mocked(aiApi.evolveTaskList).mockClear()

    await user.click(screen.getByLabelText('Agent 类型'))
    await user.click(await screen.findByRole('option', { name: '质量评分Agent' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith(expect.objectContaining({
        page: 0,
        taskType: 'quality',
      }))
    })
  })
})
