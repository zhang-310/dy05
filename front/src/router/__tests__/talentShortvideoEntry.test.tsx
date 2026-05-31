import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import { TalentShortvideoEntry } from '../index'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/utils/auth', () => ({
  isAuthenticated: () => true,
}))

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    list: vi.fn(),
    get: vi.fn(),
    shotListGet: vi.fn(),
    shotListGetByScript: vi.fn(),
  },
}))

describe('TalentShortvideoEntry', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.list).mockResolvedValue({
      total: 1,
      list: [{ id: 7, title: '达人列表项目', projectType: 'daily', status: 'processing', createTime: '2026-05-22 10:00:00' }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '达人工作台项目',
      projectType: 'daily',
      status: 'processing',
      scriptId: 20,
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 30,
      scriptId: 20,
      shots: [{ id: 1, shotNumber: 1, sceneDescription: '开场', keyframeUrl: 'https://cdn.test/k1.jpg' }],
    } as never)
  })

  it('renders talent project list when projectId is absent', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/talent/shortvideo']}>
        <TalentShortvideoEntry />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '达人短视频项目' }, { timeout: 15000 })).toBeInTheDocument()
    expect(await screen.findByText('达人列表项目', {}, { timeout: 15000 })).toBeInTheDocument()
    expect(shortvideoApi.list).toHaveBeenCalled()
    expect(shortvideoApi.get).not.toHaveBeenCalled()
  })

  it('renders talent project workbench when projectId is present', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/talent/shortvideo?projectId=7']}>
        <TalentShortvideoEntry />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '项目 #7' }, { timeout: 15000 })).toBeInTheDocument()
    expect(await screen.findByText(/项目列表/, {}, { timeout: 15000 })).toBeInTheDocument()
    expect(shortvideoApi.get).toHaveBeenCalledWith(7)
    await waitFor(() => {
      expect(shortvideoApi.list).not.toHaveBeenCalled()
    })
  })
})
