import {
  AppBar,
  Toolbar,
  IconButton,
  Typography,
  Tooltip,
  Box,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import LightModeIcon from '@mui/icons-material/LightMode'
import { LayoutBreadcrumbs } from './LayoutBreadcrumbs'
import { LAYOUT_APPBAR_HEIGHT_PX } from '@/utils/layoutViewport'

export interface LayoutTopBarProps {
  pathname: string
  themeMode: 'light' | 'dark'
  onToggleTheme: () => void
  onToggleSidebar: () => void
  useTopModules: boolean
  userDisplayName: string
  roleName: string
  onUserMenuClick: (e: React.MouseEvent<HTMLElement>) => void
}

export function LayoutTopBar({
  pathname,
  themeMode,
  onToggleTheme,
  onToggleSidebar,
  useTopModules,
  userDisplayName,
  roleName,
  onUserMenuClick,
}: LayoutTopBarProps) {
  return (
    <AppBar
      position="static"
      elevation={0}
      sx={{
        bgcolor: 'background.paper',
        color: 'text.primary',
        borderBottom: '1px solid',
        borderColor: 'divider',
      }}
    >
      <Toolbar sx={{ minHeight: LAYOUT_APPBAR_HEIGHT_PX, px: 2 }}>
        <IconButton edge="start" onClick={onToggleSidebar} sx={{ mr: 1.5 }} size="small">
          <MenuIcon />
        </IconButton>
        <LayoutBreadcrumbs pathname={pathname} />
        <Box sx={{ flexGrow: 1 }} />
        {!useTopModules && (
          <>
            <Tooltip title={themeMode === 'dark' ? '切换亮色' : '切换暗色'}>
              <IconButton size="small" onClick={onToggleTheme} sx={{ mr: 1 }}>
                {themeMode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
              </IconButton>
            </Tooltip>
            <Typography
              variant="body2"
              sx={{ cursor: 'pointer', py: 1, px: 1.5, '&:hover': { bgcolor: 'action.hover' } }}
              onClick={onUserMenuClick}
            >
              {userDisplayName} · {roleName}
            </Typography>
          </>
        )}
        {useTopModules && (
          <Typography variant="body2" color="text.secondary" sx={{ mr: 1 }}>
            {userDisplayName} · {roleName}
          </Typography>
        )}
      </Toolbar>
    </AppBar>
  )
}
