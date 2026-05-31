import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { QualityCheckDialogs, type QualityCheckDialogsProps } from './QualityCheckDialogs'

const similarityList = [
  {
    scriptId1: 101,
    scriptId2: 102,
    type1: 'opening',
    type2: 'product',
    similarityLevel: '高',
    suggestion: '两段都在重复价格权益，建议拆分利益点。',
  },
]

const skeletonList = [
  { scriptId: 101, scriptType: 'opening', summary: '先放大敏感肌痛点', suggestedDurationSec: 30 },
]

const products = [
  { id: 1, productId: 201, productName: '屏障修护精华' },
  { id: 2, productId: 202, productName: '轻盈防晒乳' },
]

function props(overrides?: Partial<QualityCheckDialogsProps>): QualityCheckDialogsProps {
  return {
    deleteConfirm: null,
    onDeleteConfirmClose: vi.fn(),
    onDeleteConfirm: vi.fn(),
    similarityOpen: false,
    similarityList: [],
    similarityLoading: false,
    onSimilarityClose: vi.fn(),
    skeletonOpen: false,
    skeletonList: [],
    skeletonLoading: false,
    onSkeletonClose: vi.fn(),
    onSkeletonExpand: vi.fn(),
    batchOpen: false,
    batchMessage: '',
    batchLoading: false,
    selectedCount: 0,
    onBatchClose: vi.fn(),
    onBatchMessageChange: vi.fn(),
    onBatchApply: vi.fn(),
    refineOpen: null,
    refineQuestion: '',
    refineLoading: false,
    onRefineClose: vi.fn(),
    onRefineQuestionChange: vi.fn(),
    onRefineApply: vi.fn(),
    emotionalOpen: false,
    emotionalCategory: 'urgency',
    emotionalSubCategory: '',
    emotionalLoading: false,
    onEmotionalClose: vi.fn(),
    onEmotionalCategoryChange: vi.fn(),
    onEmotionalSubCategoryChange: vi.fn(),
    onEmotionalGenerate: vi.fn(),
    productGenOpen: false,
    sortedProducts: products,
    onProductGenClose: vi.fn(),
    onProductGenSelect: vi.fn(),
    scriptTypeLabel: { opening: '开场', product: '产品' },
    ...overrides,
  }
}

function renderDialogs(overrides?: Partial<QualityCheckDialogsProps>) {
  const finalProps = props(overrides)
  renderWithProviders(<QualityCheckDialogs {...finalProps} />)
  return finalProps
}

describe('QualityCheckDialogs contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks quality dialogs as a props-only suite and confirms delete through parent callback', () => {
    const finalProps = renderDialogs({ deleteConfirm: { id: 101 } })

    const root = screen.getByTestId('quality-check-dialogs-contract-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-quality-check-dialogs-props-suite')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/check-similarity'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('direct-api-request'))
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(root).toHaveAttribute('data-dialog-count', '1')

    expect(screen.getByTestId('quality-delete-dialog')).toHaveAttribute('data-contract-source', '/live/script/delete|onDeleteConfirm-prop')
    fireEvent.click(screen.getByTestId('quality-delete-confirm-button'))
    expect(finalProps.onDeleteConfirm).toHaveBeenCalled()
  })

  it('renders similarity loading, empty, and result states without local fallback', () => {
    const { rerender } = renderWithProviders(<QualityCheckDialogs {...props({ similarityOpen: true, similarityLoading: true })} />)
    expect(screen.getByTestId('quality-similarity-dialog')).toHaveAttribute('data-loading', 'true')
    expect(screen.getByTestId('quality-similarity-loading')).toHaveAttribute('data-contract-source', '/live/ai/check-similarity')

    rerender(<QualityCheckDialogs {...props({ similarityOpen: true, similarityList: [] })} />)
    expect(screen.getByTestId('quality-similarity-empty-state')).toHaveAttribute('data-no-local-similarity-fallback', 'true')

    rerender(<QualityCheckDialogs {...props({ similarityOpen: true, similarityList })} />)
    expect(screen.getByTestId('quality-similarity-dialog')).toHaveAttribute('data-similarity-count', '1')
    expect(screen.getByTestId('quality-similarity-item')).toHaveAttribute('data-script-id-1', '101')
  })

  it('renders skeleton states and expands selected skeleton through props', () => {
    const finalProps = renderDialogs({ skeletonOpen: true, skeletonList })

    expect(screen.getByTestId('quality-skeleton-dialog')).toHaveAttribute('data-contract-scope', 'live-quality-skeleton-result-props')
    expect(screen.getByTestId('quality-skeleton-dialog')).toHaveAttribute('data-skeleton-count', '1')
    fireEvent.click(screen.getByTestId('quality-skeleton-item'))
    expect(finalProps.onSkeletonExpand).toHaveBeenCalledWith(101, '先放大敏感肌痛点', 30)
  })

  it('keeps skeleton true empty visible without injecting local skeletons', () => {
    renderDialogs({ skeletonOpen: true, skeletonList: [] })

    expect(screen.getByTestId('quality-skeleton-empty-state')).toHaveAttribute('data-no-local-skeleton-fallback', 'true')
    expect(screen.queryByTestId('quality-skeleton-item')).not.toBeInTheDocument()
  })

  it('marks batch AI command disabled reasons and delegates input/apply to props', () => {
    const finalProps = renderDialogs({ batchOpen: true, batchMessage: '', selectedCount: 2 })

    expect(screen.getByTestId('quality-batch-dialog')).toHaveAttribute('data-selected-count', '2')
    expect(screen.getByTestId('quality-batch-apply-button')).toHaveAttribute('data-disabled-reason', 'empty-message')
    fireEvent.change(screen.getByTestId('quality-batch-message-input'), { target: { value: '统一改成促销风格' } })
    expect(finalProps.onBatchMessageChange).toHaveBeenCalledWith('统一改成促销风格')

    const readyProps = renderDialogs({ batchOpen: true, batchMessage: '统一改成促销风格', selectedCount: 2 })
    fireEvent.click(screen.getAllByTestId('quality-batch-apply-button').at(-1)!)
    expect(readyProps.onBatchApply).toHaveBeenCalled()
  })

  it('marks refine command suggestions, disabled reasons, and apply callback', () => {
    const finalProps = renderDialogs({ refineOpen: { scriptId: 101 }, refineQuestion: '' })

    expect(screen.getByTestId('quality-refine-dialog')).toHaveAttribute('data-script-id', '101')
    expect(screen.getByTestId('quality-refine-apply-button')).toHaveAttribute('data-disabled-reason', 'empty-question')
    fireEvent.click(screen.getAllByTestId('quality-refine-suggestion-chip')[0])
    expect(finalProps.onRefineQuestionChange).toHaveBeenCalledWith('改成30秒以内')

    const readyProps = renderDialogs({ refineOpen: { scriptId: 101 }, refineQuestion: '改短' })
    fireEvent.click(screen.getAllByTestId('quality-refine-apply-button').at(-1)!)
    expect(readyProps.onRefineApply).toHaveBeenCalled()
  })

  it('marks emotional generation as props-owned and exposes loading disabled reason', () => {
    const finalProps = renderDialogs({ emotionalOpen: true, emotionalCategory: 'urgency', emotionalSubCategory: 'time_limit' })

    expect(screen.getByTestId('quality-emotional-dialog')).toHaveAttribute('data-category', 'urgency')
    expect(screen.getByTestId('quality-emotional-dialog')).toHaveAttribute('data-no-local-emotional-fallback', 'true')
    fireEvent.click(screen.getByTestId('quality-emotional-generate-button'))
    expect(finalProps.onEmotionalGenerate).toHaveBeenCalled()
  })

  it('renders product choices and true empty state without local product fallback', () => {
    const finalProps = renderDialogs({ productGenOpen: true, sortedProducts: products })

    expect(screen.getByTestId('quality-product-dialog')).toHaveAttribute('data-product-count', '2')
    fireEvent.click(screen.getAllByTestId('quality-product-item')[0])
    expect(finalProps.onProductGenSelect).toHaveBeenCalledWith(201)
  })

  it('keeps product generator empty state visible without fallback products', () => {
    renderDialogs({ productGenOpen: true, sortedProducts: [] })

    expect(screen.getByTestId('quality-product-empty-state')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.queryByTestId('quality-product-item')).not.toBeInTheDocument()
  })
})
