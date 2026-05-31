import { useEffect, useMemo, useState } from 'react'
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
  Tooltip,
  ListSubheader,
  useTheme,
  useMediaQuery,
  FormControl,
  Select,
  type SelectChangeEvent,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import MenuIcon from '@mui/icons-material/Menu'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import StarIcon from '@mui/icons-material/Star'
import StarBorderIcon from '@mui/icons-material/StarBorder'
import HistoryIcon from '@mui/icons-material/History'
import SearchIcon from '@mui/icons-material/Search'
import TranslateIcon from '@mui/icons-material/Translate'
import LightModeIcon from '@mui/icons-material/LightMode'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import { Outlet, useNavigate, useLocation, Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import i18n from '@/i18n'
import { useUserStore } from '@/stores'
import { isTrackableVisitPath, useRecentVisitsStore } from '@/stores/recentVisits'
import { getBreadcrumbs } from '@/utils/breadcrumbs'
import { buildLoginHref } from '@/utils/login-redirect'
import { ADMIN_SHORTVIDEO_BASE, TALENT_SHORTVIDEO_BASE, USER_SHORTVIDEO_BASE } from '@/constants/shortvideoRoutes'
import { SHORTVIDEO_WIDTH_OPTIONS, useShortvideoMainWidth, type ShortvideoMainWidthToken } from '@/hooks/useShortvideoMainWidth'
import { CommandPalette } from '@/layouts/components/CommandPalette'
import { WhatsNewPopover } from '@/components/onboarding/WhatsNewPopover'
import { useThemeMode } from '@/theme/AppThemeProvider'

const TOPBAR_HEIGHT = 48

interface BaseLayoutProps {
  drawerContent: React.ReactNode | ((helpers: { onNavigate: () => void }) => React.ReactNode)
}

export function BaseLayout({ drawerContent }: BaseLayoutProps) {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))
  const { t } = useTranslation()
  const { mode: themeMode, toggleMode: toggleThemeMode } = useThemeMode()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [quickMenuAnchor, setQuickMenuAnchor] = useState<null | HTMLElement>(null)
  const [languageAnchor, setLanguageAnchor] = useState<null | HTMLElement>(null)
  const [commandOpen, setCommandOpen] = useState(false)
  const { userInfo, logout } = useUserStore()
  const rawRecentItems = useRecentVisitsStore(state => state.items)
  const rawFavoriteItems = useRecentVisitsStore(state => state.favorites)
  const pushRecentVisit = useRecentVisitsStore(state => state.push)
  const toggleFavorite = useRecentVisitsStore(state => state.toggleFavorite)
  const navigate = useNavigate()
  const location = useLocation()
  const crumbs = getBreadcrumbs(location.pathname)
  const currentPath = location.pathname + location.search + location.hash
  const currentPageLabel = crumbs[crumbs.length - 1]?.label ?? currentPath
  const recentItems = useMemo(
    () => rawRecentItems.filter(item => isTrackableVisitPath(item.path)),
    [rawRecentItems],
  )
  const favoriteItems = useMemo(
    () => rawFavoriteItems.filter(item => isTrackableVisitPath(item.path)),
    [rawFavoriteItems],
  )
  const currentFavorited = favoriteItems.some(item => item.path === currentPath)
  const isShortvideoRoute =
    location.pathname === ADMIN_SHORTVIDEO_BASE ||
    location.pathname.startsWith(`${ADMIN_SHORTVIDEO_BASE}/`) ||
    location.pathname === TALENT_SHORTVIDEO_BASE ||
    location.pathname.startsWith(`${TALENT_SHORTVIDEO_BASE}/`) ||
    location.pathname === USER_SHORTVIDEO_BASE ||
    location.pathname.startsWith(`${USER_SHORTVIDEO_BASE}/`)
  const { maxWidth: svMaxWidth, selectValue: svWidthSelect, setWidth: setSvWidth } = useShortvideoMainWidth()
  const sidebarShadowColor = alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.32 : 0.08)

  useEffect(() => {
    pushRecentVisit(currentPath, currentPageLabel)
  }, [currentPageLabel, currentPath, pushRecentVisit])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setCommandOpen((open) => !open)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  const handleLogout = () => {
    const returnPath = location.pathname + location.search + location.hash
    logout()
    navigate(buildLoginHref(returnPath))
  }

  const rolePrefix = useMemo(() => {
    if (location.pathname.startsWith('/org')) return '/org'
    if (location.pathname.startsWith('/talent')) return '/talent'
    if (location.pathname.startsWith('/user')) return '/user'
    return '/admin'
  }, [location.pathname])

  const handleProfileNavigate = () => {
    setAnchorEl(null)
    navigate(`${rolePrefix}/profile`)
  }

  const renderDrawerContent = (onNavigate: () => void) =>
    typeof drawerContent === 'function' ? drawerContent({ onNavigate }) : drawerContent

  const handleQuickNavigate = (path: string) => {
    setQuickMenuAnchor(null)
    navigate(path)
  }

  const handleCommandNavigate = (path: string) => {
    setCommandOpen(false)
    navigate(path)
  }

  const handleLanguageChange = (lng: 'zh' | 'en') => {
    setLanguageAnchor(null)
    void i18n.changeLanguage(lng)
  }

  const nextThemeLabel = themeMode === 'dark' ? t('layout.themeLight') : t('layout.themeDark')
  const nextThemeAriaLabel = themeMode === 'dark' ? '切换亮色主题' : '切换暗色主题'

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
            <IconButton edge="start" onClick={() => setMobileOpen(true)} size="small" aria-label="打开导航菜单">
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
          <Tooltip title={currentFavorited ? '取消收藏当前页' : '收藏当前页'}>
            <IconButton
              size="small"
              onClick={() => toggleFavorite(currentPath, currentPageLabel)}
              aria-label={currentFavorited ? '取消收藏当前页' : '收藏当前页'}
              sx={{ ml: 0.5 }}
            >
              {currentFavorited ? <StarIcon fontSize="small" color="warning" /> : <StarBorderIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
          <Tooltip title="常用与最近访问">
            <IconButton
              size="small"
              onClick={(e) => setQuickMenuAnchor(e.currentTarget)}
              aria-label="打开常用与最近访问"
            >
              <HistoryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="命令面板 · 全局搜索（Ctrl / ⌘ + K）">
            <IconButton
              size="small"
              onClick={() => setCommandOpen(true)}
              aria-label="打开命令面板"
            >
              <SearchIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <WhatsNewPopover />
          <Tooltip title={nextThemeLabel}>
            <IconButton
              size="small"
              onClick={toggleThemeMode}
              aria-label={nextThemeAriaLabel}
            >
              {themeMode === 'dark' ? <LightModeIcon fontSize="small" /> : <DarkModeIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
          <Tooltip title={t('lang.switch')}>
            <IconButton
              size="small"
              onClick={(e) => setLanguageAnchor(e.currentTarget)}
              aria-label={t('lang.switch')}
            >
              <TranslateIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Menu
            anchorEl={languageAnchor}
            open={Boolean(languageAnchor)}
            onClose={() => setLanguageAnchor(null)}
          >
            <MenuItem
              selected={i18n.language.startsWith('zh')}
              onClick={() => handleLanguageChange('zh')}
              sx={{ fontSize: 13 }}
            >
              {t('lang.zh')}
            </MenuItem>
            <MenuItem
              selected={i18n.language.startsWith('en')}
              onClick={() => handleLanguageChange('en')}
              sx={{ fontSize: 13 }}
            >
              {t('lang.en')}
            </MenuItem>
          </Menu>
          <Menu
            anchorEl={quickMenuAnchor}
            open={Boolean(quickMenuAnchor)}
            onClose={() => setQuickMenuAnchor(null)}
            slotProps={{ paper: { sx: { width: 280, maxWidth: 'calc(100vw - 32px)' } } }}
          >
            <ListSubheader disableSticky>收藏</ListSubheader>
            {favoriteItems.length === 0 ? (
              <MenuItem disabled sx={{ fontSize: 13 }}>暂无收藏页面</MenuItem>
            ) : favoriteItems.slice(0, 8).map(item => (
              <MenuItem key={item.path} onClick={() => handleQuickNavigate(item.path)} sx={{ fontSize: 13 }}>
                <Typography variant="body2" noWrap title={item.path}>{item.label}</Typography>
              </MenuItem>
            ))}
            <Divider />
            <ListSubheader disableSticky>最近访问</ListSubheader>
            {recentItems.length === 0 ? (
              <MenuItem disabled sx={{ fontSize: 13 }}>暂无最近访问</MenuItem>
            ) : recentItems.slice(0, 8).map(item => (
              <MenuItem key={item.path} onClick={() => handleQuickNavigate(item.path)} sx={{ fontSize: 13 }}>
                <Typography variant="body2" noWrap title={item.path}>{item.label}</Typography>
              </MenuItem>
            ))}
          </Menu>
          <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ ml: 0.5 }} aria-label="打开用户菜单">
            <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: 13 }}>
              {userInfo?.nickname?.[0] ?? <AccountCircleIcon fontSize="small" />}
            </Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled sx={{ fontSize: 13 }}>{userInfo?.username ?? '未登录'}</MenuItem>
            <Divider />
            <MenuItem onClick={handleProfileNavigate} sx={{ fontSize: 13 }}>个人中心</MenuItem>
            <MenuItem onClick={handleLogout} sx={{ fontSize: 13 }}>退出登录</MenuItem>
          </Menu>
          <CommandPalette
            open={commandOpen}
            pathname={location.pathname}
            recentItems={recentItems}
            favoriteItems={favoriteItems}
            onClose={() => setCommandOpen(false)}
            onNavigate={handleCommandNavigate}
          />
        </Toolbar>
      </AppBar>

      {/* Body: sidebar + content */}
      <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Desktop sidebar */}
        <Box
          data-testid="layout-desktop-sidebar-surface"
          data-shadow-tone="sidebar-elevation"
          sx={{
          display: { xs: 'none', md: 'flex' },
          flexShrink: 0,
          height: '100%',
          boxShadow: `2px 0 8px ${sidebarShadowColor}`,
          bgcolor: 'background.paper',
          overflow: 'hidden',
        }}>
          {renderDrawerContent(() => undefined)}
        </Box>

        {/* Mobile drawer */}
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: 'auto' } }}
        >
          {renderDrawerContent(() => setMobileOpen(false))}
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
