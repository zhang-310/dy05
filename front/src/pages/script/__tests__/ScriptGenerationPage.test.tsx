import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ScriptGenerationPage from '../ScriptGenerationPage'
import { scriptApi } from '@/api/script'

vi.mock('@/api/script', () => ({
  scriptApi: {
    generate: vi.fn(),
    optimize: vi.fn(),
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
      <ScriptGenerationPage />
    </MemoryRouter>,
  )
}

describe('ScriptGenerationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.generate).mockResolvedValue({
      id: 101,
      generationTime: 1280,
      variants: [
        {
          id: 'v1',
          content: '前三秒提出敏感肌痛点，然后给出修护方案。',
          score: 91.5,
          keyPoints: '痛点开场',
        },
        {
          id: 'v2',
          content: '用真实使用场景切入，强化温和修护。',
          score: 86,
          keyPoints: '场景切入',
        },
      ],
    } as never)
    vi.mocked(scriptApi.optimize).mockResolvedValue({ optimizedContent: '', suggestions: [] } as never)
    vi.mocked(scriptApi.list).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 0 } as never)
    vi.mocked(scriptApi.save).mockResolvedValue(undefined as never)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: writeText.mockResolvedValue(undefined) },
      configurable: true,
    })
  })

  it('posts the backend generation contract and renders returned variants', async () => {
    renderPage()

    expect(screen.getByText(/数据源：\/script\/generate/)).toBeInTheDocument()
    const workbench = screen.getByTestId('script-generation-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'script-generation')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/script/generate')
    expect(workbench).toHaveAttribute('data-related-endpoints', '/script/optimize|/script/list|/script/save')
    expect(workbench).toHaveAttribute(
      'data-unsupported-endpoints',
      '/short-video/script/generate|/product/script/generate|/copy/ai/generate|/live/ai/generate-product-script|/script/template/by-scene|/script/generate/mock',
    )
    expect(workbench).toHaveAttribute('data-no-mock-variants', 'true')
    expect(screen.getByTestId('script-generation-contract-alert')).toHaveAttribute('data-no-shortvideo-generate-request', 'true')
    expect(screen.getByTestId('script-generation-contract-alert')).toHaveAttribute('data-no-product-script-generate-request', 'true')
    expect(screen.getByTestId('script-generation-contract-alert')).toHaveAttribute('data-no-copy-ai-generate-request', 'true')
    expect(screen.getByTestId('script-generation-form-card')).toHaveAttribute('data-ready-endpoint', '/script/generate')
    expect(screen.getByTestId('script-generation-payload-alert')).toHaveAttribute('data-key-features-shape', 'array')

    fireEvent.change(screen.getByLabelText(/商品名称/), { target: { value: '屏障修护面膜' } })
    fireEvent.change(screen.getByLabelText('价格'), { target: { value: '99' } })
    fireEvent.change(screen.getByLabelText(/核心卖点/), { target: { value: '敏感肌可用\n修护屏障' } })
    fireEvent.change(screen.getByLabelText(/时长/), { target: { value: '45' } })
    fireEvent.change(screen.getByLabelText(/版本数/), { target: { value: '2' } })
    fireEvent.mouseDown(screen.getByLabelText('话术风格'))
    fireEvent.click(screen.getByRole('option', { name: '故事叙述' }))

    fireEvent.click(screen.getByRole('button', { name: 'AI 生成话术' }))

    await waitFor(() => {
      expect(scriptApi.generate).toHaveBeenCalledWith({
        productName: '屏障修护面膜',
        productPrice: 99,
        keyFeatures: ['敏感肌可用', '修护屏障'],
        duration: 45,
        style: 'storytelling',
        variants: 2,
      })
    })

    expect(await screen.findByText('版本 1')).toBeInTheDocument()
    expect(screen.getByText('前三秒提出敏感肌痛点，然后给出修护方案。')).toBeInTheDocument()
    expect(screen.getByText('版本 2')).toBeInTheDocument()
    expect(screen.getByText('用真实使用场景切入，强化温和修护。')).toBeInTheDocument()
    expect(screen.getByText('1280ms')).toBeInTheDocument()
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-feature-count', '2')
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-duration', '45')
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-variants-requested', '2')
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-style', 'storytelling')
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-result-id', '101')
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-result-variant-count', '2')
    expect(screen.getByTestId('script-generation-summary-card')).toHaveAttribute('data-contract-source', 'local-form-state|/script/generate')
    expect(screen.getByTestId('script-generation-best-variant')).toHaveAttribute('data-contract-source', '/script/generate variants[0]')
    expect(screen.getByTestId('script-generation-variant-0')).toHaveAttribute('data-contract-source', '/script/generate variants')
    expect(screen.getByTestId('script-generation-variant-0')).toHaveAttribute('data-variant-id', 'v1')
    expect(screen.getByTestId('script-generation-variant-0')).toHaveAttribute('data-no-mock-variant', 'true')
    expect(scriptApi.optimize).not.toHaveBeenCalled()
    expect(scriptApi.list).not.toHaveBeenCalled()
    expect(scriptApi.save).not.toHaveBeenCalled()

    fireEvent.click(screen.getAllByRole('button', { name: '复制' })[0])
    await waitFor(() => {
      expect(writeText).toHaveBeenCalledWith('前三秒提出敏感肌痛点，然后给出修护方案。')
      expect(toast).toHaveBeenCalledWith('已复制', 'success')
    })
  })

  it('keeps backend error source visible on generation failure', async () => {
    vi.mocked(scriptApi.generate).mockRejectedValueOnce(new Error('DeepSeek AI 未启用') as never)

    renderPage()

    fireEvent.change(screen.getByLabelText(/商品名称/), { target: { value: '屏障修护面膜' } })
    fireEvent.change(screen.getByLabelText(/核心卖点/), { target: { value: '敏感肌可用' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI 生成话术' }))

    expect(await screen.findByText(/话术生成失败：DeepSeek AI 未启用/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/generate/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/script\/generation; productName=屏障修护面膜/)).toBeInTheDocument()
    expect(screen.getByText(/featureCount=1/)).toBeInTheDocument()
    expect(screen.getByTestId('script-generation-error')).toHaveAttribute('data-contract-source', '/script/generate')
    expect(screen.getByTestId('script-generation-error')).toHaveAttribute('data-no-mock-variants', 'true')
    expect(screen.getByTestId('script-generation-error')).toHaveAttribute('data-product-name', '屏障修护面膜')
    expect(screen.getByLabelText(/商品名称/)).toHaveValue('屏障修护面膜')
    expect(scriptApi.optimize).not.toHaveBeenCalled()
    expect(scriptApi.list).not.toHaveBeenCalled()
    expect(scriptApi.save).not.toHaveBeenCalled()
  })

  it('does not fabricate variants when backend returns an empty variant list', async () => {
    vi.mocked(scriptApi.generate).mockResolvedValueOnce({
      id: 102,
      generationTime: 320,
      variants: [],
    } as never)

    renderPage()

    fireEvent.change(screen.getByLabelText(/商品名称/), { target: { value: '屏障修护面膜' } })
    fireEvent.change(screen.getByLabelText(/核心卖点/), { target: { value: '敏感肌可用' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI 生成话术' }))

    expect(await screen.findByText(/未返回 variants/)).toBeInTheDocument()
    expect(screen.getByTestId('script-generation-empty-variants')).toHaveAttribute('data-contract-source', '/script/generate')
    expect(screen.getByTestId('script-generation-empty-variants')).toHaveAttribute('data-no-mock-variants', 'true')
    expect(screen.getByTestId('script-generation-workbench')).toHaveAttribute('data-result-variant-count', '0')
    expect(screen.queryByText('版本 1')).not.toBeInTheDocument()
  })
})
