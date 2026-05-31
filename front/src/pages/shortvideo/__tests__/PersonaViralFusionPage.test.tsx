import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import PersonaViralFusionPage from '../PersonaViralFusionPage'
import { matchPersonasForViral, personaViralFusion, shortvideoApi } from '@/api/shortvideo'
import { douyinApi } from '@/api/douyin'
import { productApi } from '@/api/product'

vi.mock('@/api/shortvideo', async () => {
  const actual = await vi.importActual<typeof import('@/api/shortvideo')>('@/api/shortvideo')
  return {
    ...actual,
    shortvideoApi: {
      viralList: vi.fn(),
    },
    matchPersonasForViral: vi.fn(),
    personaViralFusion: vi.fn(),
  }
})

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    personaList: vi.fn(),
  },
}))

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('PersonaViralFusionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
    vi.mocked(douyinApi.personaList).mockResolvedValue([
      { id: 2, personaName: '护肤专家' },
    ] as never)
    vi.mocked(shortvideoApi.viralList).mockResolvedValue([
      { id: 8, title: '爆款护肤结构', transcript: '原始口播内容' },
    ] as never)
    vi.mocked(matchPersonasForViral).mockResolvedValue([
      { personaId: 2, personaName: '护肤专家', matchScore: 0.92 },
    ] as never)
    vi.mocked(productApi.list).mockResolvedValue({
      total: 1,
      list: [{ id: 5, productName: '修护精华' }],
      pageNum: 0,
      pageSize: 100,
    } as never)
    vi.mocked(personaViralFusion).mockResolvedValue({
      scriptId: 21,
      script: '{"title":"融合脚本"}',
      remakeType: 'form_imitation',
      fusionScore: 0.92,
      constraintsApplied: {
        productId: 5,
        productName: '修护精华',
        topic: '春节修护场景',
        durationSeconds: 60,
        count: 3,
      },
    } as never)
  })

  it('shows dependency degradation and calls persona fusion with selected ids', async () => {
    renderWithProviders(
      <MemoryRouter>
        <PersonaViralFusionPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('persona-fusion-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/douyin/persona/list|/short-video/viral/list|/product/search|/short-video/persona-fusion/match-personas|/short-video/persona-fusion/generate-fused-script',
    )
    expect(screen.getByTestId('persona-fusion-boundary-contract')).toHaveAttribute('data-no-local-fusion-fallback', 'true')
    expect(screen.getByText(/productId\/topic\/duration\/count/)).toBeInTheDocument()

    fireEvent.mouseDown(await screen.findByLabelText(/选择人设/))
    fireEvent.click(await screen.findByRole('option', { name: '护肤专家' }))

    fireEvent.mouseDown(screen.getByLabelText(/爆款视频/))
    fireEvent.click(await screen.findByRole('option', { name: '爆款护肤结构' }))

    await waitFor(() => {
      expect(matchPersonasForViral).toHaveBeenCalledWith(8)
    })
    expect(await screen.findByText(/匹配建议：护肤专家 92%/)).toBeInTheDocument()
    expect(screen.getByTestId('persona-fusion-match-contract')).toHaveAttribute('data-no-local-match-fallback', 'true')

    fireEvent.mouseDown(screen.getByLabelText(/关联商品/))
    fireEvent.click(await screen.findByRole('option', { name: '修护精华' }))

    fireEvent.change(screen.getByLabelText('话题/场景（可选）'), { target: { value: '春节修护场景' } })

    expect(screen.getByText(/关联商品会提交到后端/)).toBeInTheDocument()
    expect(screen.getByTestId('persona-fusion-product-constraint')).toHaveAttribute('data-source-endpoint', '/short-video/persona-fusion/generate-fused-script')

    fireEvent.click(screen.getByRole('button', { name: '开始融合生成' }))

    await waitFor(() => {
      expect(personaViralFusion).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 2,
        viralVideoId: 8,
        productId: 5,
        fusionMode: 'hybrid',
      }))
    })

    expect(await screen.findByText('原始口播内容')).toBeInTheDocument()
    expect(screen.getByTestId('persona-fusion-result-contract')).toHaveAttribute('data-no-local-project-create', 'true')
    expect(screen.getByText('{"title":"融合脚本"}')).toBeInTheDocument()
    expect(screen.getByText('融合分 92.0%')).toBeInTheDocument()
    expect(screen.getByText('脚本 #21')).toBeInTheDocument()
    expect(screen.getByText(/商品已写入后端约束：修护精华/)).toBeInTheDocument()
    expect(screen.getByText(/话题已写入后端约束：春节修护场景/)).toBeInTheDocument()
    expect(screen.getByText('商品：修护精华')).toBeInTheDocument()
  })

  it('shows dependency endpoints and keeps selected constraints when fusion fails', async () => {
    vi.mocked(shortvideoApi.viralList).mockRejectedValueOnce(new Error('viral table down') as never)
    vi.mocked(productApi.list).mockRejectedValueOnce(new Error('product table down') as never)

    renderWithProviders(
      <MemoryRouter>
        <PersonaViralFusionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/POST \/short-video\/viral\/list 爆款=viral table down/)).toBeInTheDocument()
    expect(screen.getByTestId('persona-fusion-dependency-error')).toHaveAttribute('data-no-local-viral-fallback', 'true')
    expect(screen.getByText(/POST \/product\/search 商品=product table down/)).toBeInTheDocument()
    expect(screen.getByText(/页面不会补静态人设、爆款或商品/)).toBeInTheDocument()
  })

  it('keeps selected ids and topic when persona fusion generation fails', async () => {
    vi.mocked(personaViralFusion).mockRejectedValueOnce(new Error('llm unavailable') as never)

    renderWithProviders(
      <MemoryRouter>
        <PersonaViralFusionPage />
      </MemoryRouter>,
    )

    fireEvent.mouseDown(await screen.findByLabelText(/选择人设/))
    fireEvent.click(await screen.findByRole('option', { name: '护肤专家' }))
    fireEvent.mouseDown(screen.getByLabelText(/爆款视频/))
    fireEvent.click(await screen.findByRole('option', { name: '爆款护肤结构' }))
    fireEvent.change(screen.getByLabelText('话题/场景（可选）'), { target: { value: '失败场景' } })
    fireEvent.click(screen.getByRole('button', { name: '开始融合生成' }))

    expect(await screen.findByText(/融合生成失败（POST \/short-video\/persona-fusion\/generate-fused-script）：llm unavailable/)).toBeInTheDocument()
    expect(screen.getByTestId('persona-fusion-generate-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('失败场景')).toBeInTheDocument()
  })

  it('uses theme-aware fused script surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <PersonaViralFusionPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.mouseDown(await screen.findByLabelText(/选择人设/))
    fireEvent.click(await screen.findByRole('option', { name: '护肤专家' }))
    fireEvent.mouseDown(screen.getByLabelText(/爆款视频/))
    fireEvent.click(await screen.findByRole('option', { name: '爆款护肤结构' }))
    fireEvent.click(screen.getByRole('button', { name: '开始融合生成' }))

    expect(await screen.findByText('{"title":"融合脚本"}')).toBeInTheDocument()
    expect(screen.getByTestId('persona-fusion-fused-preview')).toHaveStyle({
      backgroundColor: 'rgba(144, 202, 249, 0.18)',
    })
  })
})
