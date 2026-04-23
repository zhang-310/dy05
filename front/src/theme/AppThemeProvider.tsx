import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'
import { zhCN } from '@mui/material/locale'

/** MUI 默认主题 +中文组件文案，无 palette/typography/components 自定义覆盖 */
const theme = createTheme({}, zhCN)

export function AppThemeProvider({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  )
}
