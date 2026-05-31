import { beforeEach, describe, expect, it } from 'vitest'
import { Button, useTheme } from '@mui/material'
import { fireEvent, render, screen } from '@testing-library/react'
import { AppThemeProvider, useThemeMode } from './AppThemeProvider'

function ThemeProbe() {
  const theme = useTheme()
  const { mode, toggleMode } = useThemeMode()

  return (
    <div>
      <span data-testid="context-mode">{mode}</span>
      <span data-testid="palette-mode">{theme.palette.mode}</span>
      <Button onClick={toggleMode}>切换主题</Button>
    </div>
  )
}

describe('AppThemeProvider', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('uses light mode by default and persists dark mode after toggling', () => {
    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('context-mode')).toHaveTextContent('light')
    expect(screen.getByTestId('palette-mode')).toHaveTextContent('light')

    fireEvent.click(screen.getByRole('button', { name: '切换主题' }))

    expect(screen.getByTestId('context-mode')).toHaveTextContent('dark')
    expect(screen.getByTestId('palette-mode')).toHaveTextContent('dark')
    expect(window.localStorage.getItem('dy-theme-mode')).toBe('dark')
  })

  it('reads a stored valid theme mode and ignores invalid values', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    const { unmount } = render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('context-mode')).toHaveTextContent('dark')
    expect(screen.getByTestId('palette-mode')).toHaveTextContent('dark')
    unmount()

    window.localStorage.setItem('dy-theme-mode', 'system')
    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('context-mode')).toHaveTextContent('light')
    expect(screen.getByTestId('palette-mode')).toHaveTextContent('light')
  })
})
