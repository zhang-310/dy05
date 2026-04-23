import { Box, Card, CardContent, Typography, Stack, LinearProgress, Chip, Grid } from '@mui/material'
import { paymentApi } from '@/api/payment'
import { UsageQuotaResponse } from './subscriptionPageModel'
import { useQuery } from '@tanstack/react-query'

export default function UsageQuotaPage() {
  const { data: quota, isLoading } = useQuery({
    queryKey: ['usage-quota'],
    queryFn: () => paymentApi.usageQuota(),
    refetchInterval: 60000,
  })

  if (isLoading) return <LinearProgress />

  const quotaMap = quota as UsageQuotaResponse | undefined
  const entries = quotaMap ? Object.entries(quotaMap) : []

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto', py: 2 }}>
      <Typography variant="h6" mb={3}>额度使用详情</Typography>
      <Grid container spacing={2}>
        {entries.map(([key, val]) => {
          const v = val as { used?: number; total?: number; label?: string; resetTime?: string } | null
          if (typeof v !== 'object' || v === null) return null
          const used = v.used ?? 0
          const total = v.total ?? 0
          const pct = total > 0 ? Math.min(100, Math.round((used / total) * 100)) : 0
          const colorMap = pct > 80 ? 'error' : pct > 60 ? 'warning' : 'success'
          return (
            <Grid item xs={12} sm={6} key={key}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2">{v.label ?? key}</Typography>
                    <Chip label={`${pct}%`} size="small" color={colorMap} />
                  </Stack>
                  <LinearProgress
                    variant="determinate" value={pct}
                    color={colorMap}
                    sx={{ borderRadius: 1, height: 10, mb: 1 }}
                  />
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="caption" color="text.secondary">已用：{used.toLocaleString()}</Typography>
                    <Typography variant="caption" color="text.secondary">总量：{total > 0 ? total.toLocaleString() : '不限'}</Typography>
                  </Stack>
                  {v.resetTime && (
                    <Typography variant="caption" color="text.secondary" display="block" mt={0.5}>
                      重置时间：{v.resetTime}
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>
    </Box>
  )
}
