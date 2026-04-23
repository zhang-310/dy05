import {
  Box, List, ListItemButton, ListItemIcon, ListItemText, Typography, Divider,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import { useNavigate, useLocation } from 'react-router-dom'
import { BaseLayout } from './BaseLayout'

const NAV_ITEMS = [
  { label: '工作台', icon: <DashboardIcon fontSize="small" />, path: '/talent/dashboard' },
  { label: '直播场次', icon: <LiveTvIcon fontSize="small" />, path: '/talent/live/sessions' },
  { label: '短视频', icon: <VideoLibraryIcon fontSize="small" />, path: '/talent/shortvideo' },
]

function TalentDrawer() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Box sx={{ width: 160, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle2" fontWeight={700} color="secondary.main">达人端</Typography>
      </Box>
      <List dense sx={{ flex: 1, pt: 0.5 }}>
        {NAV_ITEMS.map(item => (
          <ListItemButton
            key={item.path}
            selected={location.pathname.startsWith(item.path)}
            onClick={() => navigate(item.path)}
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
  return <BaseLayout drawerContent={<TalentDrawer />} />
}
