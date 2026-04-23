import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ConfigPage from '../ConfigPage'
import { configApi } from '@/api/config'

vi.mock('@/api/config', () => ({
  configApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(configApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          configKey: 'feature.enabled',
          configValue: 'true',
          description: '功能开关',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads config list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('系统配置')).toBeInTheDocument()

    await waitFor(() => {
      expect(configApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, configKey: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('feature.enabled')).toBeInTheDocument()
      expect(screen.getByText('功能开关')).toBeInTheDocument()
    })
  })
})
