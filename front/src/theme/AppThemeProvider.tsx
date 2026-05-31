import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'
import { zhCN } from '@mui/material/locale'

export type ThemeMode = 'light' | 'dark'

interface ThemeModeContextValue {
  mode: ThemeMode
  toggleMode: () => void
  setMode: (mode: ThemeMode) => void
}

const THEME_MODE_STORAGE_KEY = 'dy-theme-mode'

const ThemeModeContext = createContext<ThemeModeContextValue>({
  mode: 'light',
  toggleMode: () => undefined,
  setMode: () => undefined,
})

function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'light' || value === 'dark'
}

function readInitialThemeMode(): ThemeMode {
  if (typeof window === 'undefined') {
    return 'light'
  }

  const storedMode = window.localStorage.getItem(THEME_MODE_STORAGE_KEY)
  return isThemeMode(storedMode) ? storedMode : 'light'
}

export function useThemeMode() {
  return useContext(ThemeModeContext)
}

export function AppThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(() => readInitialThemeMode())

  const setMode = useCallback((nextMode: ThemeMode) => {
    setModeState(nextMode)
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(THEME_MODE_STORAGE_KEY, nextMode)
    }
  }, [])

  const toggleMode = useCallback(() => {
    setModeState((currentMode) => {
      const nextMode = currentMode === 'light' ? 'dark' : 'light'
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(THEME_MODE_STORAGE_KEY, nextMode)
      }
      return nextMode
    })
  }, [])

  const contextValue = useMemo(
    () => ({ mode, toggleMode, setMode }),
    [mode, setMode, toggleMode],
  )

  const theme = useMemo(
    () => createTheme({ palette: { mode } }, zhCN),
    [mode],
  )

  return (
    <ThemeModeContext.Provider value={contextValue}>
      <ThemeProvider theme={theme}>
        <CssBaseline enableColorScheme />
        {children}
      </ThemeProvider>
    </ThemeModeContext.Provider>
  )
}
