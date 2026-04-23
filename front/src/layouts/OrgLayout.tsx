import {
  Box, List, ListItemButton, ListItemIcon, ListItemText, Typography, Divider,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import BarChartIcon from '@mui/icons-material/BarChart'
import AssignmentIcon from '@mui/icons-material/Assignment'
import { useNavigate, useLocation } from 'react-router-dom'
import { BaseLayout } from './BaseLayout'

const NAV_ITEMS = [
  { label: '工作台', icon: <DashboardIcon fontSize="small" />, path: '/org/dashboard' },
  { label: '成员管理', icon: <PeopleIcon fontSize="small" />, path: '/org/members' },
  { label: '数据分析', icon: <BarChartIcon fontSize="small" />, path: '/org/analytics' },
  { label: '直播场次', icon: <LiveTvIcon fontSize="small" />, path: '/org/live/sessions' },
  { label: '复盘审核', icon: <AssignmentIcon fontSize="small" />, path: '/org/live/reviews' },
]

function OrgDrawer() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Box sx={{ width: 180, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle2" fontWeight={700} color="primary.main">机构管理</Typography>
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
        <Typography variant="caption" color="text.secondary">机构版</Typography>
      </Box>
    </Box>
  )
}

export function OrgLayout() {
  return <BaseLayout drawerContent={<OrgDrawer />} />
}
