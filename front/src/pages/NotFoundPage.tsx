
import { Box, Typography, Button } from '@mui/material'
import { useNavigate } from 'react-router-dom'

export default function NotFoundPage() {
  const navigate = useNavigate()
  return (
    <Box
      sx={{
        minHeight: '60vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
      }}
    >
      <Typography variant="h1" fontWeight={700} color="text.disabled">
        404
      </Typography>
      <Typography variant="h6" color="text.secondary">
        页面不存在
      </Typography>
      <Button variant="contained" onClick={() => navigate('/admin/dashboard')}>
        返回首页
      </Button>
    </Box>
  )
}
