import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import MaterialPreparationPage from '../MaterialPreparationPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    get: vi.fn(),
    uploadReferenceCharacter: vi.fn(),
    uploadReferenceScene: vi.fn(),
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
    <MemoryRouter initialEntries={['/admin/shortvideo/material-prepare?projectId=7']}>
      <MaterialPreparationPage />
    </MemoryRouter>,
  )
}

describe('MaterialPreparationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '屏障修护短视频',
      projectType: 'daily',
      shotListId: 31,
      characterReferenceUrl: 'https://cdn.test/character.png',
      sceneReferenceUrl: 'https://cdn.test/scene.png',
    } as never)
    vi.mocked(shortvideoApi.uploadReferenceCharacter).mockResolvedValue({
      url: 'https://cdn.test/new-character.png',
    } as never)
    vi.mocked(shortvideoApi.uploadReferenceScene).mockResolvedValue({
      url: 'https://cdn.test/new-scene.png',
    } as never)
  })

  it('loads project reference assets and uploads replacements with project binding', async () => {
    const { container } = renderPage()

    expect(await screen.findByText(/项目 #7 · 屏障修护短视频 · POST \/short-video\/project\/get/)).toBeInTheDocument()
    expect(screen.getByTestId('material-preparation-page')).toHaveAttribute('data-no-local-project-fallback', 'true')
    expect(screen.getByTestId('material-preparation-boundary-contract')).toHaveAttribute(
      'data-source-endpoints',
      expect.stringContaining('/short-video/upload/reference/character'),
    )
    expect(screen.getByText('角色参考图已准备')).toBeInTheDocument()
    expect(screen.getByText('场景参考图已准备')).toBeInTheDocument()
    expect(screen.getByText('分镜 #31')).toBeInTheDocument()
    expect(screen.getByTestId('material-preparation-status-card')).toHaveAttribute('data-no-local-project-fallback', 'true')

    const images = container.querySelectorAll('img')
    expect(images[0]).toHaveAttribute('src', 'https://cdn.test/character.png')
    expect(images[1]).toHaveAttribute('src', 'https://cdn.test/scene.png')

    const [characterInput] = Array.from(container.querySelectorAll('input[type="file"]'))
    const characterFile = new File(['avatar'], 'avatar.png', { type: 'image/png' })
    const sceneFile = new File(['scene'], 'scene.png', { type: 'image/png' })

    fireEvent.change(characterInput, { target: { files: [characterFile] } })
    await waitFor(() => {
      expect(shortvideoApi.uploadReferenceCharacter).toHaveBeenCalledWith({
        characterId: 'p7-default',
        projectId: 7,
        file: characterFile,
      })
      expect(toast).toHaveBeenCalledWith('上传成功', 'success')
    })

    const [, currentSceneInput] = Array.from(container.querySelectorAll('input[type="file"]'))
    fireEvent.change(currentSceneInput, { target: { files: [sceneFile] } })
    await waitFor(() => {
      expect(shortvideoApi.uploadReferenceScene).toHaveBeenCalledWith({
        sceneId: 'p7-default',
        projectId: 7,
        file: sceneFile,
      })
    })

    fireEvent.click(screen.getByRole('button', { name: '去素材生产' }))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/material-production?projectId=7'))
  })

  it('shows upload errors inline when BOS upload fails', async () => {
    vi.mocked(shortvideoApi.uploadReferenceCharacter).mockRejectedValueOnce(new Error('BOS 未配置') as never)

    const { container } = renderPage()
    expect(await screen.findByText(/项目 #7 · 屏障修护短视频 · POST \/short-video\/project\/get/)).toBeInTheDocument()

    const [characterInput] = Array.from(container.querySelectorAll('input[type="file"]'))
    const characterFile = new File(['avatar'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(characterInput, { target: { files: [characterFile] } })

    expect(await screen.findByText(/角色参考图上传失败（POST \/short-video\/upload\/reference\/character）：BOS 未配置/)).toBeInTheDocument()
    expect(screen.getByTestId('material-preparation-upload-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/已上传的参考图 URL 会保留/)).toBeInTheDocument()
  })

  it('treats missing upload url as an explicit backend contract failure', async () => {
    vi.mocked(shortvideoApi.uploadReferenceCharacter).mockResolvedValueOnce({} as never)

    const { container } = renderPage()
    expect(await screen.findByText(/项目 #7 · 屏障修护短视频 · POST \/short-video\/project\/get/)).toBeInTheDocument()

    const [characterInput] = Array.from(container.querySelectorAll('input[type="file"]'))
    const characterFile = new File(['avatar'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(characterInput, { target: { files: [characterFile] } })

    expect(await screen.findByText(/角色参考图上传失败（POST \/short-video\/upload\/reference\/character）：上传接口未返回 url/)).toBeInTheDocument()
    expect(screen.getByText(/不回写空地址/)).toBeInTheDocument()
  })

  it('shows project load endpoint and avoids placeholder images when project loading fails', async () => {
    vi.mocked(shortvideoApi.get).mockRejectedValueOnce(new Error('项目不可见') as never)

    const { container } = renderPage()

    expect(await screen.findByText(/项目加载失败（POST \/short-video\/project\/get）：项目不可见/)).toBeInTheDocument()
    expect(screen.getByTestId('material-preparation-project-error')).toHaveAttribute('data-no-local-project-fallback', 'true')
    expect(screen.getByText(/页面不会填充占位图/)).toBeInTheDocument()
    expect(container.querySelectorAll('img')).toHaveLength(0)
  })
})
