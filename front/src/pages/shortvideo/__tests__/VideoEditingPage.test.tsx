import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import VideoEditingPage from '../VideoEditingPage'
import { autoCompose, shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', async () => {
  const actual = await vi.importActual<typeof import('@/api/shortvideo')>('@/api/shortvideo')
  return {
    ...actual,
    shortvideoApi: {
      get: vi.fn(),
      videoTaskList: vi.fn(),
      videoTaskCancel: vi.fn(),
      videoTaskRetry: vi.fn(),
      workflowDigitalHumanCommerceStart: vi.fn(),
      workflowStatus: vi.fn(),
    },
    autoCompose: vi.fn(),
  }
})

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, loading }: any) => (
      <div>
        {loading && <span>loading</span>}
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell ? col.renderCell({ row, value: row[col.field] }) : String(row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

describe('VideoEditingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '项目',
      projectType: 'daily',
      finalVideoUrl: '',
    } as never)
    vi.mocked(shortvideoApi.videoTaskList).mockResolvedValue({
      total: 1,
      list: [{ id: 1, taskType: 'img2video', status: 'failed', progress: 0, errorMessage: '供应商未配置' }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(autoCompose).mockResolvedValue({
      finalVideoUrl: 'https://cdn.test/final.mp4',
      duration: 30,
    } as never)
    vi.mocked(shortvideoApi.workflowDigitalHumanCommerceStart).mockResolvedValue({
      taskId: 'wf_dh_commerce_1_7',
      projectId: 7,
      status: 'processing',
      currentStep: 'pipeline:init',
      progress: 5,
    } as never)
    vi.mocked(shortvideoApi.workflowStatus).mockResolvedValue({
      taskId: 'wf_dh_commerce_1_7',
      projectId: 7,
      status: 'completed',
      currentStep: 'completed',
      progress: 100,
      scriptId: 22,
      shotListId: 33,
      productBrollKeyframeCount: 5,
      productBrollVideoCount: 5,
      voiceClipCount: 6,
      composeVideoCount: 6,
      finalVideoUrl: 'https://cdn.test/dh-commerce-final.mp4',
    } as never)
  })

  it('shows compose diagnostics and calls autoCompose with project id', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '视频剪辑与成片' })).toBeInTheDocument()
    const root = screen.getByTestId('shortvideo-video-editing-page')
    expect(root).toHaveAttribute('data-entry-mode', 'editing')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/short-video/workflow/digital-human-commerce/start'))
    expect(root).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/publish?projectId=:id'))
    expect(root).toHaveAttribute('data-supported-actions', expect.stringContaining('submit-auto-compose'))
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/edit/mock-final-video'))
    expect(root).toHaveAttribute('data-no-local-video-task-fallback', 'true')
    expect(screen.getByTestId('video-editing-refresh-button')).toHaveAttribute('data-source-endpoint', '/short-video/video-task/list')
    expect(screen.getByTestId('video-editing-open-compose-button')).toHaveAttribute('data-source-endpoint', '/short-video/edit/auto-compose')
    expect(screen.getByTestId('video-editing-contract-alert')).toHaveTextContent(/videos 不能为空/)
    expect(screen.getByText(/videos 不能为空/)).toBeInTheDocument()

    await waitFor(() => {
      expect(shortvideoApi.videoTaskList).toHaveBeenCalledWith({ projectId: 7, page: 0, rows: 20 })
    })
    expect(await screen.findByText('失败')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'AI自动合成' }))
    expect(await screen.findByText(/自动合成当前只消费项目中已有的 videoUrl/)).toBeInTheDocument()
    expect(screen.getByText(/当前项目尚未生成成片地址/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '开始合成' }))

    await waitFor(() => {
      expect(autoCompose).toHaveBeenCalledWith({
        projectId: 7,
        duration: 60,
        style: 'standard',
        subtitleEnabled: true,
      })
    })
  })

  it('starts digital human commerce pipeline and renders workflow status', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    await screen.findByText('失败')
    fireEvent.click(screen.getByRole('button', { name: 'AI自动合成' }))
    expect(await screen.findByText(/数字人口播带货会串联脚本、分镜、数字人口播、产品 B-roll/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '数字人口播带货成片' }))

    await waitFor(() => {
      expect(shortvideoApi.workflowDigitalHumanCommerceStart).toHaveBeenCalledWith(expect.objectContaining({
        projectId: 7,
        duration: 60,
        style: '数字人口播带货 产品细节展示',
      }))
    })
    expect(await screen.findByTestId('video-editing-digital-human-workflow-card')).toHaveAttribute(
      'data-source-endpoints',
      '/short-video/workflow/digital-human-commerce/start|/short-video/workflow/status',
    )
    await waitFor(() => {
      expect(shortvideoApi.workflowStatus).toHaveBeenCalledWith('wf_dh_commerce_1_7')
    })
    expect(await screen.findByText('脚本 #22')).toBeInTheDocument()
    expect(screen.getByText('B-roll 5')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'AI自动合成视频' })).not.toBeInTheDocument()
    })
    expect(screen.getByRole('link', { name: '查看数字人成片' })).toHaveAttribute('href', 'https://cdn.test/dh-commerce-final.mp4')
  })

  it('renders subtitle entry diagnostics without navigating to placeholder video id', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitles']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '视频剪辑与成片' })).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-video-editing-page')).toHaveAttribute('data-entry-mode', 'editing')
    expect(screen.getByTestId('video-editing-no-project-warning')).toBeInTheDocument()
    expect(screen.getByText(/未选择项目时，本页只能查看全局任务/)).toBeInTheDocument()

    await waitFor(() => {
      expect(shortvideoApi.videoTaskList).toHaveBeenCalledWith({ projectId: undefined, page: 0, rows: 20 })
    })
  })

  it('renders wrapped task rows and normalized compose result link', async () => {
    vi.mocked(shortvideoApi.videoTaskList).mockResolvedValueOnce({
      total: 1,
      list: [
        {
          id: 101,
          taskType: 'img2video',
          status: 'completed',
          progress: 100,
          outputUrl: 'https://cdn.test/segment.mp4',
          createTime: '2026-05-22 11:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(autoCompose).mockResolvedValueOnce({
      finalVideoUrl: 'https://cdn.test/wrapped-final.mp4',
      duration: 30,
      thumbnail: 'https://cdn.test/cover.jpg',
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('已完成')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '查看' })).toHaveAttribute('href', 'https://cdn.test/segment.mp4')
    fireEvent.click(screen.getByRole('button', { name: 'AI自动合成' }))
    fireEvent.click(await screen.findByRole('button', { name: '开始合成' }))

    expect(await screen.findByText('https://cdn.test/wrapped-final.mp4')).toBeInTheDocument()
    expect(screen.getByTestId('video-editing-back-workbench-button')).toHaveAttribute('data-target-route', expect.stringContaining('/shortvideo/workbench?projectId=7'))
    expect(screen.getByTestId('video-editing-go-publish-button')).toHaveAttribute('data-target-route', expect.stringContaining('/shortvideo/publish?projectId=7'))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'AI自动合成视频' })).not.toBeInTheDocument()
    })
    expect(await screen.findByRole('link', { name: '查看成片' })).toHaveAttribute('href', 'https://cdn.test/wrapped-final.mp4')
  })

  it('keeps compose dialog input and shows endpoint when auto compose fails', async () => {
    vi.mocked(autoCompose).mockRejectedValueOnce(new Error('videos empty') as never)
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    await screen.findByText('失败')
    fireEvent.click(screen.getByRole('button', { name: 'AI自动合成' }))
    fireEvent.change(await screen.findByLabelText(/项目 ID/), { target: { value: '88' } })
    fireEvent.click(screen.getByRole('button', { name: '开始合成' }))

    expect(await screen.findByText(/自动合成失败（POST \/short-video\/edit\/auto-compose）：videos empty/)).toBeInTheDocument()
    expect(screen.getByTestId('video-editing-compose-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('video-editing-compose-error')).toHaveAttribute('data-no-mock-final-video', 'true')
    expect(screen.getByLabelText(/项目 ID/)).toHaveValue(88)
  })

  it('shows cancel and retry endpoint errors while keeping task rows', async () => {
    vi.mocked(shortvideoApi.videoTaskList).mockResolvedValue({
      total: 2,
      list: [
        { id: 1, taskType: 'img2video', status: 'failed', progress: 0 },
        { id: 2, taskType: 'img2video', status: 'running', progress: 40 },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(shortvideoApi.videoTaskRetry).mockRejectedValueOnce(new Error('retry queue down') as never)
    vi.mocked(shortvideoApi.videoTaskCancel).mockRejectedValueOnce(new Error('cancel denied') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    await screen.findByText('失败')
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(await screen.findByText(/重试任务失败（POST \/short-video\/video-task\/retry）：retry queue down/)).toBeInTheDocument()
    expect(screen.getByTestId('video-editing-task-action-error')).toHaveAttribute('data-no-local-video-task-mutation', 'true')
    expect(screen.getAllByText('img2video').length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(await screen.findByText(/取消任务失败（POST \/short-video\/video-task\/cancel）：cancel denied/)).toBeInTheDocument()
    expect(screen.getByText('处理中')).toBeInTheDocument()
  })

  it('shows project and task load endpoint errors without fallback tasks', async () => {
    vi.mocked(shortvideoApi.get).mockRejectedValueOnce(new Error('project denied') as never)
    vi.mocked(shortvideoApi.videoTaskList).mockRejectedValueOnce(new Error('task list down') as never)
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/项目详情加载失败（POST \/short-video\/project\/get）：project denied/)).toBeInTheDocument()
    expect(await screen.findByText(/视频任务加载失败（POST \/short-video\/video-task\/list）：task list down/)).toBeInTheDocument()
    expect(screen.getByTestId('video-editing-project-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('video-editing-tasks-error')).toHaveAttribute('data-no-local-video-task-fallback', 'true')
    expect(screen.queryByText('模拟任务')).not.toBeInTheDocument()
  })

  it('renders empty task state without static task fallback', async () => {
    vi.mocked(shortvideoApi.videoTaskList).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    } as never)
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/editing?projectId=7']}>
        <VideoEditingPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('video-editing-task-empty')).toHaveAttribute('data-no-static-task', 'true')
    expect(screen.queryByText('模拟任务')).not.toBeInTheDocument()
  })
})
