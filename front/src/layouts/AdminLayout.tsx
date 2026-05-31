import { useState, useEffect, useMemo } from 'react'
import {
  Box,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import { useNavigate, useLocation } from 'react-router-dom'
import { BaseLayout } from './BaseLayout'
import { type NavGroup } from './roleNavigation'
import { useAdminNavGroups } from '@/hooks/useFilteredRoleNav'

const RAIL_WIDTH = 72
const SUB_WIDTH = 156

function isChildActive(pathname: string, childPath: string) {
  if (pathname === childPath || pathname.startsWith(`${childPath}/`)) return true
  if (childPath === '/admin/ai/agent/workflow/list') {
    return pathname.startsWith('/admin/ai/agent/workflow/')
  }
  return false
}

function AdminNav({ onNavigate = () => undefined }: { onNavigate?: () => void }) {
  const location = useLocation()
  const navigate = useNavigate()
  const adminNavGroups = useAdminNavGroups()

  const activeGroup = useMemo(() => {
    for (const g of adminNavGroups) {
      if (g.path && location.pathname === g.path) return g.key
      if (g.children?.some(c => isChildActive(location.pathname, c.path))) return g.key
    }
    return null
  }, [location.pathname, adminNavGroups])

  const [selectedKey, setSelectedKey] = useState<string | null>(activeGroup)

  useEffect(() => {
    setSelectedKey(activeGroup)
  }, [activeGroup])

  const selectedGroup = adminNavGroups.find(g => g.key === selectedKey && g.children)

  const handleRailClick = (group: NavGroup) => {
    if (group.path) {
      navigate(group.path)
      setSelectedKey(group.key)
      onNavigate()
    } else {
      setSelectedKey(prev => (prev === group.key ? null : group.key))
    }
  }

  return (
    <Box sx={{ display: 'flex', height: '100%' }}>
      {/* === Rail (第一列) === */}
      <Box
        data-testid="admin-nav-rail"
        sx={{
          width: RAIL_WIDTH,
          flexShrink: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          bgcolor: 'background.paper',
          borderRight: '1px solid',
          borderColor: 'divider',
          height: '100%',
          overflowY: 'auto',
        }}
      >
        {adminNavGroups.map((group) => {
          const isActive =
            group.key === selectedKey ||
            (group.path ? location.pathname === group.path : false)
          return (
            <Box
              key={group.key}
              onClick={() => handleRailClick(group)}
              sx={(theme) => ({
                width: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                py: 1.5,
                cursor: 'pointer',
                color: isActive ? 'primary.main' : 'text.secondary',
                bgcolor: isActive ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.10) : 'transparent',
                borderRight: isActive ? '2px solid' : '2px solid transparent',
                borderColor: isActive ? 'primary.main' : 'transparent',
                '&:hover': {
                  bgcolor: isActive
                    ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.28 : 0.14)
                    : 'action.hover',
                  color: 'primary.main',
                },
                transition: 'all 0.15s',
                gap: 0.5,
              })}
            >
              <Box sx={{ fontSize: 20, display: 'flex', alignItems: 'center' }}>{group.icon}</Box>
              <Typography sx={{ fontSize: 12, lineHeight: 1.2, textAlign: 'center', userSelect: 'none', fontWeight: isActive ? 600 : 400 }}>
                {group.label}
              </Typography>
            </Box>
          )
        })}
      </Box>

      {/* === Sub-menu (第二列) === */}
      {selectedGroup && (
        <Box
          data-testid="admin-nav-submenu"
          sx={{
            width: SUB_WIDTH,
            flexShrink: 0,
            bgcolor: 'background.default',
            borderRight: '1px solid',
            borderColor: 'divider',
            height: '100%',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {/* Sub-menu header */}
          <Box sx={{ px: 2, py: 1.5 }}>
            <Typography sx={{ fontSize: 12, color: 'text.disabled', fontWeight: 600, letterSpacing: 0.5 }}>
              {selectedGroup.label}
            </Typography>
          </Box>
          <Divider />
          <List disablePadding dense sx={{ flex: 1 }}>
            {selectedGroup.children!.map((child) => {
              const active = isChildActive(location.pathname, child.path)
              return (
                <Box key={child.path}>
                  {child.section && (
                    <Box sx={{ px: 1.5, pt: 1.5, pb: 0.5 }}>
                      <Typography sx={{ fontSize: 10, color: 'text.disabled', fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase' }}>
                        {child.section}
                      </Typography>
                    </Box>
                  )}
                <ListItemButton
                  selected={active}
                  onClick={() => {
                    navigate(child.path)
                    onNavigate()
                  }}
                  sx={(theme) => ({
                    py: 0.8,
                    pl: 1.5,
                    pr: 1,
                    borderRadius: 0,
                    '&.Mui-selected': {
                      bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.10),
                      borderRight: '2px solid',
                      borderColor: 'primary.main',
                    },
                    '&.Mui-selected:hover': {
                      bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.28 : 0.14),
                    },
                  })}
                >
                  <ListItemIcon sx={{ minWidth: 28, color: active ? 'primary.main' : 'text.disabled' }}>
                    {child.icon}
                  </ListItemIcon>
                  <ListItemText
                    primary={child.label}
                    primaryTypographyProps={{
                      fontSize: 12,
                      color: active ? 'primary.main' : 'text.secondary',
                      fontWeight: active ? 600 : 400,
                    }}
                  />
                </ListItemButton>
                </Box>
              )
            })}
          </List>
        </Box>
      )}
    </Box>
  )
}

export function AdminLayout() {
  return <BaseLayout drawerContent={({ onNavigate }) => <AdminNav onNavigate={onNavigate} />} />
}

