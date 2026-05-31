import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ShotListPage from '../ShotListPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    get: vi.fn(),
    svScriptGet: vi.fn(),
    shotListGet: vi.fn(),
    shotListGetByScript: vi.fn(),
    shotListGenerate: vi.fn(),
    shotListSave: vi.fn(),
    shotDelete: vi.fn(),
    save: vi.fn(),
  },
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

function renderPage() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/admin/shortvideo/shot-list?projectId=7']}>
      <ShotListPage />
    </MemoryRouter>,
  )
}

describe('ShotListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      ownerId: 1,
      accountId: 3,
      title: '短视频项目',
      projectType: 'daily',
      status: 'processing',
      scriptId: 12,
      shotListId: 20,
      duration: 30,
      characterReferenceUrl: 'https://cdn.example.com/character.png',
    } as never)
    vi.mocked(shortvideoApi.svScriptGet).mockResolvedValue({
      id: 12,
      title: '脚本',
      content: '开场介绍产品，展示使用方法，最后引导评论。',
      scriptType: 'daily',
      style: 'professional',
    } as never)
    vi.mocked(shortvideoApi.shotListGet).mockResolvedValue({
      id: 20,
      scriptId: 12,
      shotCount: 2,
      shots: [
        {
          id: 101,
          shotNumber: 1,
          sceneDescription: '产品特写',
          dialogue: '先看质地',
          cameraAngle: '特写',
          cameraType: 'zoom-in',
          keyframeUrl: 'https://cdn.example.com/k1.png',
          keyframeBosKey: 'bos/k1.png',
          videoUrl: 'https://cdn.example.com/v1.mp4',
          videoBosKey: 'bos/v1.mp4',
          duration: 5,
        },
        {
          id: 102,
          shotNumber: 2,
          sceneDescription: '使用场景',
          cameraAngle: '中景',
          duration: 6,
        },
      ],
    } as never)
    vi.mocked(shortvideoApi.shotListSave).mockResolvedValue(20 as never)
    vi.mocked(shortvideoApi.shotListGenerate).mockResolvedValue({
      shotListId: 30,
      shots: [{ shotNumber: 1, sceneDescription: '新分镜' }],
    } as never)
    vi.mocked(shortvideoApi.save).mockResolvedValue(7 as never)
  })

  it('renders shot diagnostics and preserves BOS keys when saving', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: '分镜设计' })).toBeInTheDocument()
    expect(await screen.findByText('产品特写')).toBeInTheDocument()
    expect(screen.getByTestId('shot-list-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/shot-list/generate'),
    )
    expect(screen.getByTestId('shot-list-page')).toHaveAttribute(
      'data-ready-routes',
      expect.stringContaining('/shortvideo/material-production?projectId=:id'),
    )
    expect(screen.getByTestId('shot-list-page')).toHaveAttribute(
      'data-supported-actions',
      expect.stringContaining('navigate-material-production'),
    )
    expect(screen.getByTestId('shot-list-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/shot-list/local-generate'),
    )
    expect(screen.getByTestId('shot-list-boundary-contract')).toHaveAttribute('data-no-local-project-mutation', 'true')
    expect(screen.getByTestId('shot-list-load-project-button')).toHaveAttribute('data-source-endpoint', '/short-video/project/get')
    expect(screen.getAllByTestId('shot-list-row')[0]).toHaveAttribute('data-no-local-shot-row-synthesis', 'true')
    expect(screen.getByText(/BOS key 会随 URL 一起保留/)).toBeInTheDocument()
    expect(screen.getByText('1 / 1')).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: '编辑' })[0])
    fireEvent.change(await screen.findByLabelText('关键帧 BOS Key'), { target: { value: 'bos/k1-new.png' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.shotListSave).toHaveBeenCalledWith(expect.objectContaining({
        shotListId: 20,
        shots: expect.arrayContaining([
          expect.objectContaining({
            id: 101,
            keyframeBosKey: 'bos/k1-new.png',
            videoBosKey: 'bos/v1.mp4',
          }),
        ]),
      }))
    })
  })

  it('generates shot list from script and keeps project fields while binding shotListId', async () => {
    renderPage()

    await screen.findByText('产品特写')
    expect(screen.getByTestId('shot-list-generate-button')).toHaveAttribute('data-source-endpoint', '/short-video/shot-list/generate')
    fireEvent.click(screen.getByRole('button', { name: '从脚本生成分镜' }))

    await waitFor(() => {
      expect(shortvideoApi.shotListGenerate).toHaveBeenCalledWith(expect.objectContaining({
        scriptId: 12,
        scriptContent: '开场介绍产品，展示使用方法，最后引导评论。',
        style: 'professional',
      }))
    })
    expect(shortvideoApi.save).toHaveBeenCalledWith(expect.objectContaining({
      id: 7,
      accountId: 3,
      title: '短视频项目',
      projectType: 'daily',
      scriptId: 12,
      shotListId: 30,
      characterReferenceUrl: 'https://cdn.example.com/character.png',
    }))
  })

  it('uses in-page confirmation and shows delete failure inline', async () => {
    vi.mocked(shortvideoApi.shotDelete).mockRejectedValueOnce(new Error('shot locked') as never)
    renderPage()

    await screen.findByText('产品特写')
    fireEvent.click(screen.getByLabelText('删除分镜 1'))

    expect(await screen.findByRole('dialog', { name: '删除分镜' })).toBeInTheDocument()
    expect(screen.getByText(/素材生产和成片合成将不再包含该镜头/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(shortvideoApi.shotDelete).toHaveBeenCalledWith(101)
    })
    expect((await screen.findAllByText(/删除分镜失败（POST \/short-video\/shot-list\/delete-shot）：shot locked/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('shot-list-delete-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByText('产品特写')).toBeInTheDocument()
  })

  it('keeps edit dialog open and shows endpoint when shot save fails', async () => {
    vi.mocked(shortvideoApi.shotListSave).mockRejectedValueOnce(new Error('save denied') as never)
    renderPage()

    await screen.findByText('产品特写')
    fireEvent.click(screen.getAllByRole('button', { name: '编辑' })[0])
    fireEvent.change(await screen.findByLabelText('画面描述'), { target: { value: '产品质地近景' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/分镜保存失败（POST \/short-video\/shot-list\/save）：save denied/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('shot-list-save-error')).toHaveAttribute('data-no-local-shot-mutation', 'true')
    expect(screen.getByLabelText('画面描述')).toHaveValue('产品质地近景')
  })

  it('shows shot generation endpoint and keeps current project context on failure', async () => {
    vi.mocked(shortvideoApi.shotListGenerate).mockRejectedValueOnce(new Error('llm offline') as never)
    renderPage()

    await screen.findByText('产品特写')
    fireEvent.click(screen.getByRole('button', { name: '从脚本生成分镜' }))

    expect(await screen.findByText(/分镜生成或项目回写失败（POST \/short-video\/shot-list\/generate 或 POST \/short-video\/project\/save）：llm offline/)).toBeInTheDocument()
    expect(screen.getByTestId('shot-list-generate-error')).toHaveAttribute('data-no-local-shot-generation', 'true')
    expect(screen.getByTestId('shot-list-generate-error')).toHaveAttribute('data-no-local-project-mutation', 'true')
    expect(screen.getByText('产品特写')).toBeInTheDocument()
    expect(screen.getByLabelText('项目 ID')).toHaveValue('7')
  })

  it('shows shot-list loading endpoint instead of mocked fallback rows', async () => {
    vi.mocked(shortvideoApi.shotListGet).mockRejectedValueOnce(new Error('shot table locked') as never)
    renderPage()

    expect(await screen.findByText(/分镜加载失败（POST \/short-video\/shot-list\/get）：shot table locked/)).toBeInTheDocument()
    expect(screen.getByTestId('shot-list-load-error')).toHaveAttribute('data-no-local-shot-fallback', 'true')
    expect(screen.queryByText('模拟分镜')).not.toBeInTheDocument()
  })
})
