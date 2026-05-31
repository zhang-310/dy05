import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { RegeneratedScriptPreview } from '../RegeneratedScriptPreview'
import type { RegeneratedScriptVO } from '@/types/optimization'

const regenerated: RegeneratedScriptVO = {
  id: 88,
  regeneratedContent: '新版强调产品卖点和限时行动。',
  aiQualityScore: 9.2,
  approvalStatus: 'pending',
  appliedSuggestionCount: 2,
  estimatedImprovementScore: 12.4,
  contentDifference: {
    originalContent: '旧版只介绍了基础功能。',
    regeneratedContent: '新版强调产品卖点和限时行动。',
    differenceSummary: '强化利益点、补充行动召唤，并压缩冗余描述。',
    similarityScore: 82,
    changes: [
      {
        type: 'replacement',
        position: 12,
        original: '基础功能',
        modified: '产品卖点',
        reason: '提高转化表达强度',
      },
    ],
  },
}

describe('RegeneratedScriptPreview', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware regenerated script preview surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <RegeneratedScriptPreview regenerated={regenerated} onApply={vi.fn()} onReject={vi.fn()} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('regenerated-applied-count-surface')).toHaveAttribute('data-preview-color', '#e3f2fd')
    expect(screen.getByTestId('regenerated-improvement-score-surface')).toHaveAttribute('data-preview-color', '#81c784')
    expect(screen.getByTestId('regenerated-similarity-surface')).toHaveAttribute('data-similarity-tone', 'success')
    expect(screen.getByTestId('regenerated-similarity-surface')).toHaveAttribute('data-similarity-color', '#81c784')
    expect(screen.getByTestId('regenerated-summary-surface')).not.toHaveStyle({ backgroundColor: 'rgb(249, 249, 249)' })

    fireEvent.click(screen.getByRole('button', { name: '详细对比' }))

    expect(screen.getByTestId('regenerated-diff-original-surface')).toHaveAttribute('data-diff-color', '#e57373')
    expect(screen.getByTestId('regenerated-diff-modified-surface')).toHaveAttribute('data-diff-color', '#81c784')
    expect(screen.getByTestId('regenerated-change-item-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })

    const serialized = document.body.innerHTML
    for (const legacy of ['#f44336', '#ffebee', '#c62828', '#4caf50', '#e8f5e9', '#2e7d32', '#2196f3', '#fff3e0', '#e65100', '#f9f9f9', '#f5f5f5', '#999', '#666']) {
      expect(serialized).not.toContain(legacy)
    }
  })

  it('keeps the empty state when no regenerated script is available', () => {
    renderWithProviders(<RegeneratedScriptPreview regenerated={null} />)

    expect(screen.getByText('暂无重生成的话术版本')).toBeInTheDocument()
  })
})
