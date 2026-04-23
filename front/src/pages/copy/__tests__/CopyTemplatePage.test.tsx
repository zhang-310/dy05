import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import CopyTemplatePage from '../CopyTemplatePage'
import { copyApi } from '@/api/copy'

vi.mock('@/api/copy', () => ({
  copyApi: {
    templateList: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('CopyTemplatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(copyApi.templateList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          templateName: '开场模板',
          content: '大家好，欢迎来到直播间',
          category: '开场白',
          variables: 'productName',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('renders title and loads copy templates', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    expect(screen.getByText('文案模板')).toBeInTheDocument()

    await waitFor(() => {
      expect(copyApi.templateList).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('开场模板')).toBeInTheDocument()
      expect(screen.getByText('开场白')).toBeInTheDocument()
    })
  })
})
