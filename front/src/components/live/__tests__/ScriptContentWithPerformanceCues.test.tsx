import { describe, expect, it, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import ScriptContentWithPerformanceCues from '../ScriptContentWithPerformanceCues'

const content = '欢迎来到直播间，【微笑停顿】这款精华先讲修护屏障，【语速放慢】再讲限时福利。'

function renderCue(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('ScriptContentWithPerformanceCues', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('uses theme-aware fullscreen panel cue surfaces', () => {
    renderCue(<ScriptContentWithPerformanceCues content={content} variant="panel" isFullscreen />)

    const textSurface = screen.getByTestId('performance-cue-text-surface')
    const inlineCues = screen.getAllByTestId('performance-cue-inline-surface')
    const chips = screen.getAllByTestId('performance-cue-chip-surface')

    expect(textSurface).toHaveStyle({ color: 'rgb(255, 255, 255)' })
    expect(inlineCues).toHaveLength(2)
    expect(inlineCues[0]).toHaveAttribute('data-cue-fullscreen', 'true')
    expect(inlineCues[0]).not.toHaveStyle({ backgroundColor: 'rgba(255,193,7,0.22)' })
    expect(inlineCues[0]).not.toHaveStyle({ color: 'rgb(255, 224, 130)' })
    expect(chips[0]).toHaveAttribute('data-cue-fullscreen', 'true')
    expect(document.body.textContent).not.toMatch(/#ffe082|rgba\(255,193,7,0\.22\)|#fff|#000/i)
  })

  it('uses theme-aware panel text in dark mode without fullscreen fallback colors', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderCue(<ScriptContentWithPerformanceCues content={content} variant="panel" />)

    const textSurface = screen.getByTestId('performance-cue-text-surface')
    const inlineCues = screen.getAllByTestId('performance-cue-inline-surface')

    expect(textSurface).not.toHaveStyle({ color: 'rgb(0, 0, 0)' })
    expect(inlineCues[0]).toHaveAttribute('data-cue-variant', 'panel')
    expect(inlineCues[0]).toHaveAttribute('data-cue-fullscreen', 'false')
    expect(inlineCues[0]).not.toHaveStyle({ backgroundColor: 'rgba(255,193,7,0.22)' })
  })
})
