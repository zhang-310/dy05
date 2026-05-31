import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import QuickGeneratePage from '../QuickGeneratePage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    quickGenerate: vi.fn(),
  },
}))

const navigate = vi.fn()
const toast = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('QuickGeneratePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.quickGenerate).mockResolvedValue({
      projectId: 9,
      scriptId: 10,
      shotListId: 11,
      title: '屏障修护',
      scriptContent: '前三秒提出敏感肌痛点。',
    } as never)
  })

  it('calls real quick-generate endpoint and renders generated chain result', async () => {
    renderWithProviders(
      <MemoryRouter>
        <QuickGeneratePage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('quick-generate-page')).toHaveAttribute('data-ready-endpoints', '/short-video/quick/generate')
    expect(screen.getByTestId('quick-generate-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/quick/local-project'),
    )
    expect(screen.getByTestId('quick-generate-boundary-contract')).toHaveAttribute('data-no-local-quick-generate', 'true')
    fireEvent.change(screen.getByLabelText(/视频主题/), { target: { value: '护肤测评' } })
    fireEvent.change(screen.getByLabelText(/关键词/), { target: { value: '屏障修护' } })
    fireEvent.mouseDown(screen.getByLabelText('视频风格'))
    fireEvent.click(screen.getByRole('option', { name: '专业干货' }))
    fireEvent.click(screen.getByRole('button', { name: '一键生成脚本与分镜' }))

    await waitFor(() => {
      expect(shortvideoApi.quickGenerate).toHaveBeenCalledWith({
        theme: '护肤测评',
        keywords: '屏障修护',
        style: '专业',
      })
    })

    expect(await screen.findByText('项目 #9')).toBeInTheDocument()
    expect(screen.getByTestId('quick-generate-result-card')).toHaveAttribute('data-no-client-id-synthesis', 'true')
    expect(screen.getByText('脚本 #10')).toBeInTheDocument()
    expect(screen.getByText('分镜 #11')).toBeInTheDocument()
    expect(screen.getByText('前三秒提出敏感肌痛点。')).toBeInTheDocument()
  })

  it('keeps generation failure visible on the page', async () => {
    vi.mocked(shortvideoApi.quickGenerate).mockRejectedValueOnce(new Error('LLM 未配置') as never)

    renderWithProviders(
      <MemoryRouter>
        <QuickGeneratePage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText(/视频主题/), { target: { value: '护肤测评' } })
    fireEvent.click(screen.getByRole('button', { name: '一键生成脚本与分镜' }))

    expect(await screen.findByText(/一键生成失败（POST \/short-video\/quick\/generate）：LLM 未配置/)).toBeInTheDocument()
    expect(screen.getByTestId('quick-generate-error')).toHaveAttribute('data-no-local-quick-generate', 'true')
    expect(screen.getByTestId('quick-generate-error')).toHaveAttribute('data-no-client-id-synthesis', 'true')
    expect(screen.getAllByText(/POST \/short-video\/quick\/generate/).length).toBeGreaterThan(0)
    expect(screen.getByLabelText(/视频主题/)).toHaveValue('护肤测评')
  })

  it('shows explicit degraded state when quick-generate response is incomplete', async () => {
    vi.mocked(shortvideoApi.quickGenerate).mockResolvedValueOnce({
      projectId: 9,
      title: '缺少脚本和分镜',
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <QuickGeneratePage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText(/视频主题/), { target: { value: '护肤测评' } })
    fireEvent.click(screen.getByRole('button', { name: '一键生成脚本与分镜' }))

    expect(await screen.findByText(/项目、脚本或分镜 ID 不完整/)).toBeInTheDocument()
    expect(screen.getByTestId('quick-generate-degraded')).toHaveAttribute('data-no-client-id-synthesis', 'true')
    expect(screen.getByText(/接口来源：POST \/short-video\/quick\/generate/)).toBeInTheDocument()
  })

  it('pre-fills theme and keywords from hot topic route params', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/quick-generate?keyword=屏障修护&hotTopicId=5']}>
        <QuickGeneratePage />
      </MemoryRouter>,
    )

    expect(screen.getAllByDisplayValue('屏障修护')).toHaveLength(2)
    expect(screen.getByTestId('quick-generate-hot-topic-prefill')).toHaveAttribute('data-navigation-param-only', 'true')
    expect(screen.getByText(/来自热点 #5：屏障修护/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '一键生成脚本与分镜' }))

    await waitFor(() => {
      expect(shortvideoApi.quickGenerate).toHaveBeenCalledWith({
        theme: '屏障修护',
        keywords: '屏障修护',
        style: '温馨',
      })
    })
  })
})
