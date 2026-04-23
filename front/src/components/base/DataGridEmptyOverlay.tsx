
import { Box, Typography } from '@mui/material'
import InboxIcon from '@mui/icons-material/Inbox'

export function DataGridEmptyOverlay() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        color: 'text.disabled',
      }}
    >
      <InboxIcon sx={{ fontSize: 48, mb: 1 }} />
      <Typography variant="body2">暂无数据</Typography>
    </Box>
  )
}
