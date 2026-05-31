import { useEffect, useState } from 'react'
import { Box, Typography, Paper, Alert, Stack, Button } from '@mui/material'
import { Link } from 'react-router-dom'

export default function CreditGovernancePage() {
  const [account, setAccount] = useState<Record<string, unknown> | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('/api/credits/account?tenantId=demo-tenant')
      .then((r) => r.json())
      .then((body) => {
        setAccount(body?.data ?? null)
        setError(null)
      })
      .catch((e) => setError(String(e)))
  }, [])

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>积分治理</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>
        商业化拒绝：无权益 HTTP 402 / code 4421；积分不足 HTTP 402 / code 4420。前端全局拦截见 request.ts。
      </Alert>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <Button component={Link} to="/admin/gaifan/payment" variant="outlined" size="small">
          购买套餐
        </Button>
        <Button component={Link} to="/official" variant="text" size="small">
          可售产品目录
        </Button>
      </Stack>
      {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}
      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle2" gutterBottom>demo-tenant 积分账户</Typography>
        <pre>{JSON.stringify(account, null, 2)}</pre>
      </Paper>
    </Box>
  )
}
