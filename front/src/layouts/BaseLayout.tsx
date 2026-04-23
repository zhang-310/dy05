import { useState } from 'react'
import {
  Box,
  AppBar,
  Toolbar,
  IconButton,
  Typography,
  Drawer,
  Avatar,
  Menu,
  MenuItem,
  Divider,
  Breadcrumbs,
  Link,
  useTheme,
  useMediaQuery,
  FormControl,
  Select,
  type SelectChangeEvent,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import { Outlet, useNavigate, useLocation, Link as RouterLink } from 'react-router-dom'
import { useUserStore } from '@/stores'
import { getBreadcrumbs } from '@/utils/breadcrumbs'
import { buildLoginHref } from '@/utils/login-redirect'
import { ADMIN_SHORTVIDEO_BASE } from '@/constants/shortvideoRoutes'
import { SHORTVIDEO_WIDTH_OPTIONS, useShortvideoMainWidth, type ShortvideoMainWidthToken } from '@/hooks/useShortvideoMainWidth'

const TOPBAR_HEIGHT = 48

interface BaseLayoutProps {
  drawerContent: React.ReactNode
}

export function BaseLayout({ drawerContent }: BaseLayoutProps) {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))
  const [mobileOpen, setMobileOpen] = useState(false)
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const { userInfo, logout } = useUserStore()
  const navigate = useNavigate()
  const location = useLocation()
  const crumbs = getBreadcrumbs(location.pathname)
  const isShortvideoRoute =
    location.pathname === ADMIN_SHORTVIDEO_BASE ||
    location.pathname.startsWith(`${ADMIN_SHORTVIDEO_BASE}/`)
  const { maxWidth: svMaxWidth, selectValue: svWidthSelect, setWidth: setSvWidth } = useShortvideoMainWidth()

  const handleLogout = () => {
    const returnPath = location.pathname + location.search + location.hash
    logout()
    navigate(buildLoginHref(returnPath))
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      {/* Full-width top bar */}
      <AppBar
        position="static"
        elevation={0}
        sx={{
          bgcolor: 'background.paper',
          borderBottom: '1px solid',
          borderColor: 'divider',
          color: 'text.primary',
          flexShrink: 0,
          height: TOPBAR_HEIGHT,
        }}
      >
        <Toolbar
          disableGutters
          sx={{ height: TOPBAR_HEIGHT, minHeight: `${TOPBAR_HEIGHT}px !important`, px: 2, gap: 1 }}
        >
          {isMobile && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} size="small">
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="subtitle2" fontWeight={700} color="primary"
            sx={{ mr: 2, display: { xs: 'none', md: 'block' }, flexShrink: 0, whiteSpace: 'nowrap' }}
          >
            抖音运营系统V2.0
          </Typography>
          <Breadcrumbs
            separator={<NavigateNextIcon sx={{ fontSize: 14 }} />}
            sx={{ flex: 1, '& .MuiBreadcrumbs-ol': { flexWrap: 'nowrap', alignItems: 'center' } }}
          >
            {crumbs.map((crumb, i) =>
              crumb.path ? (
                <Link key={i} component={RouterLink} to={crumb.path} underline="hover"
                  color="text.secondary" sx={{ fontSize: 13 }}>
                  {crumb.label}
                </Link>
              ) : (
                <Typography key={i} sx={{ fontSize: 13, fontWeight: i === crumbs.length - 1 ? 600 : 400 }}
                  color={i === crumbs.length - 1 ? 'text.primary' : 'text.secondary'}>
                  {crumb.label}
                </Typography>
              )
            )}
          </Breadcrumbs>
          {isShortvideoRoute && (
            <FormControl
              size="small"
              sx={{
                minWidth: 100,
                mr: 1,
                flexShrink: 0,
                display: { xs: 'none', sm: 'inline-flex' },
              }}
            >
              <Select<ShortvideoMainWidthToken>
                value={svWidthSelect}
                onChange={(e: SelectChangeEvent<ShortvideoMainWidthToken>) =>
                  setSvWidth(e.target.value as ShortvideoMainWidthToken)
                }
                displayEmpty
                aria-label="短视频内容区宽度"
                sx={{ fontSize: 13, '& .MuiSelect-select': { py: 0.5 } }}
              >
                {SHORTVIDEO_WIDTH_OPTIONS.map((opt) => (
                  <MenuItem key={opt.value} value={opt.value} sx={{ fontSize: 13 }}>
                    {opt.label === '全宽' ? '全宽' : `${opt.label}px`}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ ml: 1 }}>
            <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: 13 }}>
              {userInfo?.nickname?.[0] ?? <AccountCircleIcon fontSize="small" />}
            </Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled sx={{ fontSize: 13 }}>{userInfo?.username ?? '未登录'}</MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout} sx={{ fontSize: 13 }}>退出登录</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* Body: sidebar + content */}
      <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Desktop sidebar */}
        <Box sx={{
          display: { xs: 'none', md: 'flex' },
          flexShrink: 0,
          height: '100%',
          boxShadow: '2px 0 8px rgba(0,0,0,0.06)',
          bgcolor: 'white',
          overflow: 'hidden',
        }}>
          {drawerContent}
        </Box>

        {/* Mobile drawer */}
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: 'auto' } }}
        >
          {drawerContent}
        </Drawer>

        {/* Main content：允许纵向滚动，避免长页面（如短视频看板）被 overflow:hidden 裁切 */}
        <Box
          component="main"
          sx={{
            flex: 1,
            minHeight: 0,
            minWidth: 0,
            overflowX: 'hidden',
            overflowY: 'auto',
            p: 2,
            display: 'flex',
            flexDirection: 'column',
            alignItems: isShortvideoRoute ? 'center' : 'stretch',
          }}
        >
          {isShortvideoRoute ? (
            <Box
              sx={{
                width: '100%',
                maxWidth: svMaxWidth === 'full' ? 'none' : svMaxWidth,
                minWidth: 0,
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <Outlet />
            </Box>
          ) : (
            <Outlet />
          )}
        </Box>
      </Box>
    </Box>
  )
}

