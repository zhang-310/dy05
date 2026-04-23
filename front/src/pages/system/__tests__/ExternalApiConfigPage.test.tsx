import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ExternalApiConfigPage from '../ExternalApiConfigPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    externalApiList: vi.fn(),
    externalApiSave: vi.fn(),
    externalApiDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ExternalApiConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.externalApiList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          apiName: 'OpenAI',
          category: 'llm',
          baseUrl: 'https://api.openai.com',
          authType: 'api_key',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads external api configs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(systemApi.externalApiList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        category: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('OpenAI')).toBeInTheDocument()
      expect(screen.getByText('https://api.openai.com')).toBeInTheDocument()
      expect(screen.getByText('LLM')).toBeInTheDocument()
    })
  })
})
