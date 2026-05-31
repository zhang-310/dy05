import { Box, Typography, Paper, Button, Stack } from '@mui/material'
import { useNavigate } from 'react-router-dom'

export default function PaymentCenterPage() {
  const navigate = useNavigate()
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>支付与商业化</Typography>
      <Paper sx={{ p: 2 }}>
        <Stack spacing={2}>
          <Typography color="text.secondary">
            订单与订阅仍使用现有支付模块；支付确认后可经 PaymentCreditGrantPort 写入 gf_credit_account。
          </Typography>
          <Button variant="outlined" onClick={() => navigate('/admin/payment/orders')}>订单管理</Button>
          <Button variant="outlined" onClick={() => navigate('/admin/gaifan/credits')}>积分治理</Button>
        </Stack>
      </Paper>
    </Box>
  )
}
