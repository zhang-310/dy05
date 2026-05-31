import {
  Box, List, ListItemButton, ListItemIcon, ListItemText, Typography, Divider,
} from '@mui/material'
import { useNavigate, useLocation } from 'react-router-dom'
import { BaseLayout } from './BaseLayout'
import { ORG_NAV_ITEMS } from './roleNavigation'

function OrgDrawer({ onNavigate = () => undefined }: { onNavigate?: () => void }) {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Box sx={{ width: 180, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle2" fontWeight={700} color="primary.main">机构管理</Typography>
      </Box>
      <List dense sx={{ flex: 1, pt: 0.5 }}>
        {ORG_NAV_ITEMS.map(item => (
          <ListItemButton
            key={item.path}
            selected={location.pathname.startsWith(item.path)}
            onClick={() => {
              navigate(item.path)
              onNavigate()
            }}
            sx={{ borderRadius: 1, mx: 0.5, mb: 0.25 }}
          >
            <ListItemIcon sx={{ minWidth: 32 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} primaryTypographyProps={{ variant: 'body2' }} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <Box sx={{ p: 1 }}>
        <Typography variant="caption" color="text.secondary">机构版</Typography>
      </Box>
    </Box>
  )
}

export function OrgLayout() {
  return <BaseLayout drawerContent={({ onNavigate }) => <OrgDrawer onNavigate={onNavigate} />} />
}
