import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import SvProjectWorkbenchPage from '../SvProjectWorkbenchPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    get: vi.fn(),
    shotListGet: vi.fn(),
    shotListGetByScript: vi.fn(),
  },
}))

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

describe('SvProjectWorkbenchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders project chain diagnostics and next blocker', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '精华液短视频',
      projectType: 'daily',
      status: 'processing',
      scriptId: 20,
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 30,
      scriptId: 20,
      shots: [
        { id: 1, shotNumber: 1, sceneDescription: '开场', keyframeUrl: 'https://cdn.test/k1.jpg' },
        { id: 2, shotNumber: 2, sceneDescription: '展示' },
      ],
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workbench?projectId=7']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('精华液短视频')).toBeInTheDocument()
    const root = screen.getByTestId('sv-project-workbench-page')
    expect(root).toHaveAttribute('data-ready-endpoints', '/short-video/project/get|/short-video/shot-list/get|/short-video/shot-list/get-by-script')
    expect(root).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/publish?projectId=:id'))
    expect(root).toHaveAttribute('data-supported-actions', expect.stringContaining('navigate-video-editing'))
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/workbench/static-progress'))
    expect(root).toHaveAttribute('data-no-local-project-fallback', 'true')
    expect(root).toHaveAttribute('data-no-static-progress', 'true')
    expect(screen.getByTestId('sv-workbench-refresh-button')).toHaveAttribute('data-source-endpoint', '/short-video/project/get')
    await waitFor(() => {
      expect(shortvideoApi.shotListGet).toHaveBeenCalledWith(30)
    })
    expect(screen.getByText('脚本 1')).toBeInTheDocument()
    expect(screen.getByText('分镜 2')).toBeInTheDocument()
    expect(screen.getByText('关键帧 1')).toBeInTheDocument()
    expect(screen.getByText(/缺少视频片段/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-workbench-progress-summary')).toHaveAttribute('data-no-static-progress', 'true')
    expect(screen.getByTestId('sv-workbench-blockers')).toBeInTheDocument()
    expect(screen.getAllByTestId('sv-workbench-flow-step-button').length).toBeGreaterThan(0)
  })

  it('keeps talent workbench in talent shell and step routes stay inside talent shell', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '达人短视频项目',
      projectType: 'daily',
      status: 'processing',
      scriptId: 20,
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 30,
      scriptId: 20,
      shots: [
        { id: 1, shotNumber: 1, sceneDescription: '开场', keyframeUrl: 'https://cdn.test/k1.jpg' },
      ],
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/talent/shortvideo?projectId=7']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('达人短视频项目')).toBeInTheDocument()
    expect(screen.getByTestId('sv-workbench-talent-shell-contract')).toHaveTextContent(/当前为 达人端 项目工作台/)
    expect(screen.getByRole('link', { name: '项目列表' })).toHaveAttribute('href', '/talent/shortvideo')
    expect(screen.getAllByTestId('sv-workbench-flow-step-button')[0]).toHaveAttribute('data-target-route', '/talent/shortvideo/script-planning?projectId=7')

    screen.getByRole('button', { name: '进入视频剪辑' }).click()
    expect(navigate).toHaveBeenCalledWith('/talent/shortvideo/editing?projectId=7')
  })

  it('shows project load errors inline', async () => {
    vi.mocked(shortvideoApi.get).mockRejectedValueOnce(new Error('项目不存在') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workbench?projectId=7']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/项目加载失败（POST \/short-video\/project\/get）：项目不存在/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-workbench-project-error')).toHaveAttribute('data-no-local-project-fallback', 'true')
  })

  it('shows shot-list endpoint when project loads but shot list fails', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '精华液短视频',
      projectType: 'daily',
      status: 'processing',
      scriptId: 20,
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockRejectedValueOnce(new Error('shot table down') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workbench?projectId=7']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('精华液短视频')).toBeInTheDocument()
    expect(await screen.findByText(/分镜详情加载失败（POST \/short-video\/shot-list\/get）：shot table down/)).toBeInTheDocument()
    expect(screen.getByText(/不会补静态分镜/)).toBeInTheDocument()
    expect(screen.getByTestId('sv-workbench-shot-list-error')).toHaveAttribute('data-no-static-progress', 'true')
  })

  it('uses get-by-script endpoint when project has script but no shotListId', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '仅脚本项目',
      projectType: 'daily',
      status: 'processing',
      scriptId: 20,
    } as never)
    vi.mocked(shortvideoApi.shotListGetByScript).mockRejectedValueOnce(new Error('script shot missing') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workbench?projectId=7']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('仅脚本项目')).toBeInTheDocument()
    expect(await screen.findByText(/分镜详情加载失败（POST \/short-video\/shot-list\/get-by-script）：script shot missing/)).toBeInTheDocument()
  })

  it('renders no project and no shot-source states without local fallback', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workbench']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('sv-project-workbench-page')).toHaveAttribute('data-no-local-project-fallback', 'true')
    expect(screen.getByText('请从项目列表进入工作台')).toBeInTheDocument()
    expect(shortvideoApi.get).not.toHaveBeenCalled()

    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 8,
      title: '未关联脚本项目',
      projectType: 'daily',
      status: 'draft',
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workbench?projectId=8']}>
        <SvProjectWorkbenchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('未关联脚本项目')).toBeInTheDocument()
    expect(screen.getByTestId('sv-workbench-no-shot-source')).toHaveAttribute('data-no-local-shot-list-fallback', 'true')
    expect(shortvideoApi.shotListGet).not.toHaveBeenCalled()
    expect(shortvideoApi.shotListGetByScript).not.toHaveBeenCalled()
  })
})
