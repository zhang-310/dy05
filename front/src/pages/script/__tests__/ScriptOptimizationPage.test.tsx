import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ScriptOptimizationPage from '../ScriptOptimizationPage'
import { scriptApi } from '@/api/script'

vi.mock('@/api/script', () => ({
  scriptApi: {
    optimize: vi.fn(),
    generate: vi.fn(),
    list: vi.fn(),
    save: vi.fn(),
  },
}))

const toast = vi.fn()
const writeText = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <ScriptOptimizationPage />
    </MemoryRouter>,
  )
}

describe('ScriptOptimizationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.optimize).mockResolvedValue({
      optimizedContent: '前三秒提出敏感肌痛点，马上引出修护方案，最后提醒点击领取。',
      originalScore: 6.2,
      optimizedScore: 8.9,
      suggestions: ['已按优化目标强化：强化行动号召'],
      generationTime: 960,
      style: 'conversion',
      goal: '强化行动号召',
    } as never)
    vi.mocked(scriptApi.generate).mockResolvedValue({ id: 0, variants: [] } as never)
    vi.mocked(scriptApi.list).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 0 } as never)
    vi.mocked(scriptApi.save).mockResolvedValue(undefined as never)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: writeText.mockResolvedValue(undefined) },
      configurable: true,
    })
  })

  it('calls real optimization endpoint and keeps local diagnosis as support', async () => {
    renderPage()

    expect(screen.getByText(/数据源：\/script\/optimize/)).toBeInTheDocument()
    const workbench = screen.getByTestId('script-optimization-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'script-optimization')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/script/optimize')
    expect(workbench).toHaveAttribute('data-related-endpoints', '/script/generate|/script/list|/script/save')
    expect(workbench).toHaveAttribute(
      'data-unsupported-endpoints',
      '/script/generate|/short-video/script/generate|/product/script/generate|/copy/ai/generate|/live/ai/generate-product-script|/script/optimize/mock',
    )
    expect(workbench).toHaveAttribute('data-no-static-optimized-content', 'true')
    expect(screen.getByTestId('script-optimization-contract-alert')).toHaveAttribute('data-no-script-generate-request', 'true')
    expect(screen.getByTestId('script-optimization-contract-alert')).toHaveAttribute('data-no-shortvideo-generate-request', 'true')
    expect(screen.getByTestId('script-optimization-form-card')).toHaveAttribute('data-ready-endpoint', '/script/optimize')
    expect(screen.getByTestId('script-optimization-form-card')).toHaveAttribute('data-contract-payload', 'originalContent|goal|style')

    fireEvent.change(screen.getByLabelText(/原始话术/), {
      target: {
        value:
          '这款面膜很温和，适合敏感肌日常护理。它主打屏障修护、清爽贴肤和换季维稳，适合在直播间先讲使用场景，再讲成分优势，最后回到用户的真实困扰。',
      },
    })
    fireEvent.change(screen.getByLabelText(/优化目标/), {
      target: { value: '强化行动号召' },
    })
    fireEvent.mouseDown(screen.getByLabelText('优化风格'))
    fireEvent.click(screen.getByRole('option', { name: '紧迫促销' }))

    fireEvent.click(screen.getByRole('button', { name: 'AI 优化话术' }))

    await waitFor(() => {
      expect(scriptApi.optimize).toHaveBeenCalledWith({
        originalContent:
          '这款面膜很温和，适合敏感肌日常护理。它主打屏障修护、清爽贴肤和换季维稳，适合在直播间先讲使用场景，再讲成分优势，最后回到用户的真实困扰。',
        goal: '强化行动号召',
        style: 'urgent',
      })
    })

    expect(await screen.findByText('AI 优化结果')).toBeInTheDocument()
    expect(screen.getByText(/马上引出修护方案/)).toBeInTheDocument()
    expect(screen.getByText('原评分 6.2')).toBeInTheDocument()
    expect(screen.getByText('优化后 8.9')).toBeInTheDocument()
    expect(screen.getByText(/缺少明确行动号召/)).toBeInTheDocument()
    expect(screen.getByText('已按优化目标强化：强化行动号召')).toBeInTheDocument()
    expect(screen.getByText(/按目标「强化行动号召」重写时/)).toBeInTheDocument()
    expect(screen.getByTestId('script-optimization-workbench')).toHaveAttribute('data-style', 'urgent')
    expect(screen.getByTestId('script-optimization-workbench')).toHaveAttribute('data-result-has-content', 'true')
    expect(screen.getByTestId('script-optimization-workbench')).toHaveAttribute('data-generation-time-ms', '960')
    expect(screen.getByTestId('script-optimization-result-card')).toHaveAttribute('data-contract-source', '/script/optimize')
    expect(screen.getByTestId('script-optimization-result-card')).toHaveAttribute('data-result-has-content', 'true')
    expect(screen.getByTestId('script-optimization-result-card')).toHaveAttribute('data-original-score', '6.2')
    expect(screen.getByTestId('script-optimization-result-card')).toHaveAttribute('data-optimized-score', '8.9')
    expect(screen.getByTestId('script-optimization-result-suggestion-0')).toHaveAttribute('data-contract-source', '/script/optimize suggestions')
    expect(screen.getByTestId('script-optimization-local-diagnosis')).toHaveAttribute('data-contract-source', 'local-structure-diagnosis')
    expect(screen.getByTestId('script-optimization-local-diagnosis')).toHaveAttribute('data-no-ai-result-fabrication', 'true')
    expect(scriptApi.generate).not.toHaveBeenCalled()
    expect(scriptApi.list).not.toHaveBeenCalled()
    expect(scriptApi.save).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: '复制优化稿' }))
    await waitFor(() => {
      expect(writeText).toHaveBeenCalledWith('前三秒提出敏感肌痛点，马上引出修护方案，最后提醒点击领取。')
    })
  })

  it('keeps backend error visible and does not fabricate optimized content', async () => {
    vi.mocked(scriptApi.optimize).mockRejectedValueOnce(new Error('AI_TASK_MODEL_NOT_CONFIGURED') as never)

    renderPage()

    fireEvent.change(screen.getByLabelText(/原始话术/), {
      target: { value: '这款面膜很温和，适合敏感肌日常护理。' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'AI 优化话术' }))

    expect(await screen.findByText(/话术优化失败：AI_TASK_MODEL_NOT_CONFIGURED/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/optimize/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/script\/optimization/)).toBeInTheDocument()
    expect(screen.getByText(/originalLength=/)).toBeInTheDocument()
    expect(screen.getByTestId('script-optimization-error')).toHaveAttribute('data-contract-source', '/script/optimize')
    expect(screen.getByTestId('script-optimization-error')).toHaveAttribute('data-no-static-optimized-content', 'true')
    expect(screen.getByTestId('script-optimization-error')).toHaveAttribute('data-style', 'conversion')
    expect(screen.getByLabelText(/原始话术/)).toHaveValue('这款面膜很温和，适合敏感肌日常护理。')
    expect(screen.queryByText('AI 优化结果')).not.toBeInTheDocument()
    expect(screen.getByTestId('script-optimization-local-diagnosis')).toBeInTheDocument()
    expect(scriptApi.generate).not.toHaveBeenCalled()
    expect(scriptApi.list).not.toHaveBeenCalled()
    expect(scriptApi.save).not.toHaveBeenCalled()
  })

  it('shows an explicit empty result warning when optimized content is missing', async () => {
    vi.mocked(scriptApi.optimize).mockResolvedValueOnce({
      optimizedContent: '',
      suggestions: ['后端未返回正文'],
    } as never)

    renderPage()

    fireEvent.change(screen.getByLabelText(/原始话术/), {
      target: { value: '这款面膜很温和，适合敏感肌日常护理。' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'AI 优化话术' }))

    expect(await screen.findByText(/缺少 optimizedContent/)).toBeInTheDocument()
    expect(screen.getByTestId('script-optimization-result-card')).toHaveAttribute('data-result-has-content', 'false')
    expect(screen.getByTestId('script-optimization-empty-result')).toHaveAttribute('data-contract-source', '/script/optimize')
    expect(screen.getByTestId('script-optimization-empty-result')).toHaveAttribute('data-no-static-optimized-content', 'true')
    expect(screen.getByText('后端未返回正文')).toBeInTheDocument()
  })
})
