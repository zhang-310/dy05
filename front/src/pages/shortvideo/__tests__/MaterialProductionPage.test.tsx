import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import MaterialProductionPage from '../MaterialProductionPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', async () => {
  const actual = await vi.importActual<typeof import('@/api/shortvideo')>('@/api/shortvideo')
  return {
    ...actual,
    shortvideoApi: {
      get: vi.fn(),
      shotListGet: vi.fn(),
      shotListGetByScript: vi.fn(),
      materialGenerateKeyframes: vi.fn(),
      videoTaskSubmit: vi.fn(),
      videoTaskList: vi.fn(),
      videoTaskCancel: vi.fn(),
    },
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
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({ row, value: row[col.field] })
                      : String(row[col.field] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  }
})

describe('MaterialProductionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.videoTaskList).mockResolvedValue({
      total: 2,
      list: [
        { id: 1, taskType: 'img2video', status: 'running', progress: 40 },
        { id: 2, taskType: 'img2video', status: 'failed', progress: 0, errorMessage: '供应商未配置' },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('shows keyframe readiness and task degradation diagnostics', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '项目',
      projectType: 'daily',
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 30,
      shots: [
        { id: 1, shotNumber: 1, keyframeUrl: 'https://cdn.test/k1.jpg' },
        { id: 2, shotNumber: 2 },
      ],
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/material-production?projectId=7']}>
        <MaterialProductionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '素材生产' })).toBeInTheDocument()
    expect(screen.getByTestId('material-production-page')).toHaveAttribute('data-no-local-task-fallback', 'true')
    expect(screen.getByTestId('material-production-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/video-task/local-submit'),
    )
    await waitFor(() => {
      expect(shortvideoApi.shotListGet).toHaveBeenCalledWith(30)
    })
    expect(screen.getByTestId('material-production-summary')).toHaveAttribute('data-no-local-keyframe-fallback', 'true')
    expect(screen.getByText('分镜总数')).toBeInTheDocument()
    expect(screen.getByText('可提交关键帧')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText((_, node) => node?.textContent === '缺 1 条')).toBeInTheDocument()
    })
    expect(screen.getByText(/存在失败的视频生成任务/)).toBeInTheDocument()
  })

  it('renders wrapped video task rows with snake-case fields', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '项目',
      projectType: 'daily',
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 30,
      shots: [
        { id: 1, shotNumber: 1, keyframeUrl: 'https://cdn.test/k1.jpg' },
      ],
    } as never)
    vi.mocked(shortvideoApi.videoTaskList).mockResolvedValueOnce({
      total: 1,
      list: [
        {
          id: 101,
          taskType: 'img2video',
          status: 'failed',
          progress: 40,
          projectId: 7,
          shotListId: 30,
          errorMessage: '包装供应商未配置',
          outputUrl: 'https://cdn.test/out.mp4',
          createTime: '2026-05-22 11:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/material-production?projectId=7']}>
        <MaterialProductionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装供应商未配置')).toBeInTheDocument()
    expect(screen.getByText('40%')).toBeInTheDocument()
    expect(screen.getByText(/存在失败的视频生成任务/)).toBeInTheDocument()
  })

  it('shows shot-list loading errors with retry context', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '项目',
      projectType: 'daily',
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockRejectedValueOnce(new Error('分镜不存在') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/material-production?projectId=7']}>
        <MaterialProductionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/分镜加载失败（POST \/short-video\/shot-list\/get）：分镜不存在/)).toBeInTheDocument()
    expect(screen.getByTestId('material-production-shotlist-error')).toHaveAttribute('data-no-local-keyframe-fallback', 'true')
    expect(screen.getByRole('button', { name: '重试' })).toBeInTheDocument()
  })

  it('shows keyframe generation, submit, cancel and task-list failures inline', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '项目',
      projectType: 'daily',
      shotListId: 30,
      characterReferenceUrl: 'https://cdn.test/character.png',
      sceneReferenceUrl: 'https://cdn.test/scene.png',
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 30,
      shots: [
        { id: 1, shotNumber: 1, keyframeUrl: 'https://cdn.test/k1.jpg', sceneDescription: '痛点开场' },
      ],
    } as never)
    vi.mocked(shortvideoApi.materialGenerateKeyframes).mockRejectedValueOnce(new Error('SD 未启动') as never)
    vi.mocked(shortvideoApi.videoTaskSubmit).mockRejectedValueOnce(new Error('RabbitMQ 未配置') as never)
    vi.mocked(shortvideoApi.videoTaskCancel).mockRejectedValueOnce(new Error('任务已锁定') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/material-production?projectId=7']}>
        <MaterialProductionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '素材生产' })).toBeInTheDocument()
    await waitFor(() => {
      expect(shortvideoApi.shotListGet).toHaveBeenCalledWith(30)
    })
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '生成关键帧' })).toBeEnabled()
    })

    fireEvent.click(screen.getByRole('button', { name: '生成关键帧' }))
    expect(await screen.findByText(/关键帧生成失败（POST \/short-video\/material\/generate-keyframes）：SD 未启动/)).toBeInTheDocument()
    expect(screen.getByTestId('material-production-operation-error')).toHaveAttribute('data-no-local-keyframe-fallback', 'true')
    expect(screen.getByText(/不会写入占位关键帧/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /一键提交图生视频/ }))
    expect(await screen.findByText(/图生视频任务提交失败（POST \/short-video\/video-task\/submit）：RabbitMQ 未配置/)).toBeInTheDocument()
    expect(screen.getByTestId('material-production-operation-error')).toHaveAttribute('data-no-local-task-fallback', 'true')
    expect(screen.getByText(/当前分镜和 keyframes 仍保留/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(await screen.findByText(/取消图生视频任务失败（POST \/short-video\/video-task\/cancel）：任务已锁定/)).toBeInTheDocument()
    expect(screen.getByText(/保留任务原状态/)).toBeInTheDocument()
  })

  it('shows task-list loading errors with source endpoint', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '项目',
      projectType: 'daily',
      shotListId: 30,
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({ id: 30, shots: [] } as never)
    vi.mocked(shortvideoApi.videoTaskList).mockRejectedValueOnce(new Error('任务表不可用') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/material-production?projectId=7']}>
        <MaterialProductionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/图生视频任务列表加载失败（POST \/short-video\/video-task\/list）：任务表不可用/)).toBeInTheDocument()
    expect(screen.getByTestId('material-production-tasklist-error')).toHaveAttribute('data-no-local-task-fallback', 'true')
    expect(screen.getByText(/页面不会补静态任务/)).toBeInTheDocument()
  })
})
