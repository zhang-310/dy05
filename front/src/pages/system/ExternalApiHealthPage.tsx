import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid, Chip, Button,
  IconButton, Tooltip, CircularProgress, Alert,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { systemApi } from '@/api/system'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'

interface ExternalApiConfig {
  id: number; name: string; apiKey: string; baseUrl: string
  provider: string; enabled: boolean; lastCheckTime: string
  status: 'ok' | 'error' | 'unknown'; latencyMs: number
  errorMsg?: string
}

export default function ExternalApiHealthPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [pinging, setPinging] = useState<number | null>(null)

  const { data = [], isLoading, refetch } = useQuery({
    queryKey: ['external-api-configs'],
    queryFn: () => systemApi.externalApiList(),
    refetchInterval: 60000,
  })

  const pingMut = useMutation({
    mutationFn: (id: number) => systemApi.externalApiHealthStatus(id),
    onMutate: (id) => setPinging(id),
    onSuccess: () => { toast('连通性测试完成', 'success'); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
    onSettled: () => setPinging(null),
  })

  const configs = (Array.isArray(data) ? data : []) as ExternalApiConfig[]
  const okCount = configs.filter(c => c.status === 'ok').length
  const errorCount = configs.filter(c => c.status === 'error').length

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h6" fontWeight={600}>外部 API 健康监控</Typography>
        <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>
      </Stack>

      <Grid container spacing={2}>
        {[
          { label: '接入数量', value: configs.length },
          { label: '正常', value: okCount, color: 'success.main' },
          { label: '异常', value: errorCount, color: 'error.main' },
          { label: '可用率', value: configs.length > 0 ? `${Math.round(okCount / configs.length * 100)}%` : '--' },
        ].map(kpi => (
          <Grid item xs={6} sm={3} key={kpi.label}>
            <Card variant="outlined"><CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
              <Typography variant="h5" fontWeight={700} color={kpi.color}>{kpi.value}</Typography>
            </CardContent></Card>
          </Grid>
        ))}
      </Grid>

      {errorCount > 0 && (
        <Alert severity="error">有 {errorCount} 个外部 API 连接异常，请及时处理</Alert>
      )}

      {isLoading && <CircularProgress sx={{ mx: 'auto' }} />}

      <Grid container spacing={2}>
        {configs.map(cfg => (
          <Grid item xs={12} sm={6} md={4} key={cfg.id}>
            <Card variant="outlined" sx={{ borderColor: cfg.status === 'error' ? 'error.light' : cfg.status === 'ok' ? 'success.light' : 'divider' }}>
              <CardContent sx={{ py: 1.5 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      {cfg.status === 'ok'
                        ? <CheckCircleIcon color="success" fontSize="small" />
                        : cfg.status === 'error'
                        ? <ErrorIcon color="error" fontSize="small" />
                        : <CircularProgress size={14} />}
                      <Typography variant="body2" fontWeight={600} noWrap>{cfg.name}</Typography>
                    </Stack>
                    <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>{cfg.provider}</Typography>
                  </Box>
                  <Tooltip title="测试连通性">
                    <span>
                      <IconButton size="small" onClick={() => pingMut.mutate(cfg.id)}
                        disabled={pinging === cfg.id}>
                        {pinging === cfg.id ? <CircularProgress size={16} /> : <RefreshIcon fontSize="small" />}
                      </IconButton>
                    </span>
                  </Tooltip>
                </Stack>

                <Stack spacing={0.5} mt={1}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="caption" color="text.secondary">状态</Typography>
                    <Chip
                      label={cfg.status === 'ok' ? '正常' : cfg.status === 'error' ? '异常' : '未知'}
                      size="small"
                      color={cfg.status === 'ok' ? 'success' : cfg.status === 'error' ? 'error' : 'default'}
                    />
                  </Stack>
                  {cfg.latencyMs > 0 && (
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="caption" color="text.secondary">延迟</Typography>
                      <Typography variant="caption" fontWeight={600}
                        color={cfg.latencyMs > 1000 ? 'warning.main' : 'text.primary'}>
                        {cfg.latencyMs} ms
                      </Typography>
                    </Stack>
                  )}
                  {cfg.lastCheckTime && (
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="caption" color="text.secondary">上次检测</Typography>
                      <Typography variant="caption">{formatDate(cfg.lastCheckTime)}</Typography>
                    </Stack>
                  )}
                  {cfg.errorMsg && (
                    <Typography variant="caption" color="error.main" sx={{ wordBreak: 'break-all' }}>
                      {cfg.errorMsg}
                    </Typography>
                  )}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {!isLoading && configs.length === 0 && (
        <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
          暂无外部 API 配置，请在系统设置中添加
        </Typography>
      )}
    </Box>
  )
}
