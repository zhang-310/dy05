import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { FlowStepList } from './FlowStepList'
import type { FlowStep, GroupedSteps } from './types'

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

const baseProps = {
  currentLabel: undefined,
  justCompleted: false,
  expandedKeys: new Set<string>(),
  groupAccordionExpanded: {},
  onToggleExpand: vi.fn(),
  onCopy: vi.fn(),
  onRetryStep: vi.fn(),
  onGroupAccordionChange: vi.fn(),
  lastStepRef: { current: null },
}

describe('FlowStepList', () => {
  it('uses theme-aware failed state in flat timeline view', () => {
    const steps: FlowStep[] = [
      { label: '生成开场中', status: 'done', content: '开场完成', stepKey: 'done' },
      { label: '生成产品1中', status: 'failed', errorMsg: '模型超时', stepKey: 'failed' },
    ]

    renderDark(<FlowStepList {...baseProps} steps={steps} />)

    const failed = screen.getByTestId('flow-step-flat-failed-surface')
    expect(failed).toHaveAttribute('data-step-status', 'failed')
    expect(failed).not.toHaveStyle({ backgroundColor: 'rgba(211, 47, 47, 0.04)' })

    for (const legacy of ['rgba(211, 47, 47, 0.04)', 'rgba(211, 47, 47, 0.06)']) {
      expect(document.body.textContent).not.toContain(legacy)
    }
  })

  it('uses theme-aware failed state in grouped accordion view', () => {
    const groupedSteps: GroupedSteps[] = [
      {
        groupLabel: '产品生成',
        steps: [
          { label: '生成产品1中', status: 'done', content: '完成', stepKey: 'g1' },
          { label: '生成产品2中', status: 'failed', errorMsg: '供应商限流', stepKey: 'g2' },
        ],
      },
    ]

    renderDark(<FlowStepList {...baseProps} steps={[]} groupedSteps={groupedSteps} />)

    const failed = screen.getByTestId('flow-step-grouped-failed-surface')
    expect(failed).toHaveAttribute('data-step-status', 'failed')
    expect(failed).not.toHaveStyle({ backgroundColor: 'rgba(211, 47, 47, 0.06)' })
  })
})
