import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { SearchResultsList } from '../SearchResultsList'
import type { HybridSearchResultVO } from '@/types/search'

const results: HybridSearchResultVO[] = [
  {
    id: 1,
    content: '高相关商品话术',
    title: '修护精华高转化话术',
    scriptType: 'product',
    style: 'natural',
    relevanceScore: 92,
    vectorSimilarity: 90,
    bm25Score: 88,
    effectivenessScore: 86,
    usageCount: 23,
    createdAt: '2026-05-22T10:00:00',
    preview: '强调屏障修护和限时福利。',
    author: '运营A',
    tags: ['修护', '直播'],
  },
  {
    id: 2,
    content: '中相关短视频脚本',
    title: '短视频开头优化',
    scriptType: 'shortvideo',
    style: 'story',
    relevanceScore: 72,
    vectorSimilarity: 70,
    bm25Score: 68,
    createdAt: '2026-05-21T10:00:00',
  },
  {
    id: 3,
    content: '低相关活动话术',
    title: '活动备用文案',
    scriptType: 'event',
    style: 'promo',
    relevanceScore: 38,
    vectorSimilarity: 30,
    bm25Score: 42,
    createdAt: '2026-05-20T10:00:00',
  },
]

describe('SearchResultsList', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware result surfaces and feedback actions in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    const onFeedback = vi.fn().mockResolvedValue(undefined)

    renderWithProviders(
      <AppThemeProvider>
        <SearchResultsList results={results} total={3} executionTimeMs={24} onFeedback={onFeedback} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('search-results-summary-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })

    const fills = screen.getAllByTestId('search-result-relevance-fill-surface')
    expect(fills.map(node => node.getAttribute('data-relevance-tone'))).toEqual(['success', 'warning', 'error'])
    expect(fills.map(node => node.getAttribute('data-relevance-color'))).toEqual(['#81c784', '#ffb74d', '#e57373'])

    const serialized = document.body.innerHTML
    for (const legacy of ['#333', '#666', '#999', '#e0e0e0', '#4caf50', '#ff9800', '#f44336', '#eee', '#f5f5f5']) {
      expect(serialized).not.toContain(legacy)
    }

    fireEvent.click(screen.getAllByTitle('有帮助')[0])
    expect(screen.getByText('反馈搜索结果质量')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '提交反馈' }))

    await waitFor(() => expect(onFeedback).toHaveBeenCalledWith(1, true, 5, ''))
  })

  it('uses a theme-aware empty state', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <SearchResultsList results={[]} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('search-results-empty-surface')).not.toHaveStyle({ backgroundColor: 'rgb(249, 249, 249)' })
    expect(screen.getByText('暂无搜索结果')).toBeInTheDocument()
  })
})
