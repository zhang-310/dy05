import {
  Box, List, ListItemButton, ListItemIcon, ListItemText, Typography, Divider,
} from '@mui/material'
import { useNavigate, useLocation } from 'react-router-dom'
import { BaseLayout } from './BaseLayout'
import { useTalentNavItems } from '@/hooks/useFilteredRoleNav'

function TalentDrawer({ onNavigate = () => undefined }: { onNavigate?: () => void }) {
  const navigate = useNavigate()
  const location = useLocation()
  const talentNavItems = useTalentNavItems()

  return (
    <Box sx={{ width: 160, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle2" fontWeight={700} color="secondary.main">达人端</Typography>
      </Box>
      <List dense sx={{ flex: 1, pt: 0.5 }}>
        {talentNavItems.map(item => (
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
        <Typography variant="caption" color="text.secondary">达人版</Typography>
      </Box>
    </Box>
  )
}

export function TalentLayout() {
  return <BaseLayout drawerContent={({ onNavigate }) => <TalentDrawer onNavigate={onNavigate} />} />
}
