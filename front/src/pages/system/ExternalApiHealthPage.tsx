import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid, Chip, Button,
  IconButton, Tooltip, CircularProgress, Alert,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { systemApi, type ExternalApiConfig } from '@/api/system'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'
import { EmptyState, ErrorAlert, PageHeader } from '@/components/base'
import { normalizeRows } from '@/utils/response-normalize'
import { getErrorMessage } from '@/utils/errorHandler'

function healthToView(status?: string): 'ok' | 'error' | 'unknown' {
  if (status === 'healthy') return 'ok'
  if (status === 'down' || status === 'degraded') return 'error'
  return 'unknown'
}

function formatPercent(value: unknown): string {
  if (typeof value !== 'number' || !Number.isFinite(value)) return '--'
  const normalized = value > 0 && value <= 1 ? value * 100 : value
  return `${normalized.toFixed(1)}%`
}

const EXTERNAL_API_HEALTH_ENDPOINTS = [
  '/system/external-api/list',
  '/system/external-api/health-status',
  '/system/external-api/probe',
].join('|')
const EXTERNAL_API_HEALTH_UNSUPPORTED_ENDPOINTS = [
  '/system/external-api/get-secret',
  '/system/external-api/secret',
  '/system/external-api/ping',
].join('|')
const EXTERNAL_API_HEALTH_UNSUPPORTED_ACTIONS = [
  'browser-direct-provider-ping',
  'local-health-fallback',
  'synthetic-provider-card',
  'plaintext-secret-display',
  'local-health-mutation',
].join('|')

export default function ExternalApiHealthPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [pinging, setPinging] = useState<string | null>(null)
  const [operationError, setOperationError] = useState<{ providerCode: string; message: string } | null>(null)

  const { data, isLoading, refetch, isError, error } = useQuery({
    queryKey: ['external-api-configs'],
    queryFn: () => systemApi.externalApiList(),
    refetchInterval: 60000,
  })

  const pingMut = useMutation({
    mutationFn: (providerCode: string) => systemApi.externalApiProbe(providerCode),
    onMutate: (providerCode) => setPinging(providerCode),
    onSuccess: () => { toast('后端探测已完成', 'success'); setOperationError(null); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error, providerCode) => {
      setOperationError({ providerCode, message: getErrorMessage(e) })
      toast(e.message, 'error')
    },
    onSettled: () => setPinging(null),
  })

  const configs = normalizeRows<ExternalApiConfig>(data)
  const enabledCount = configs.filter(c => c.isEnabled).length
  const okCount = configs.filter(c => healthToView(c.healthStatus) === 'ok').length
  const errorCount = configs.filter(c => healthToView(c.healthStatus) === 'error').length
  const unknownCount = configs.length - okCount - errorCount
  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
      data-testid="external-api-health-page-workbench"
      data-contract-scope="system-external-api-health-monitoring"
      data-ready-endpoints={EXTERNAL_API_HEALTH_ENDPOINTS}
      data-unsupported-endpoints={EXTERNAL_API_HEALTH_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={EXTERNAL_API_HEALTH_UNSUPPORTED_ACTIONS}
      data-row-count={configs.length}
      data-enabled-count={enabledCount}
      data-ok-count={okCount}
      data-error-count={errorCount}
      data-unknown-count={unknownCount}
      data-loading={String(isLoading)}
      data-load-error={String(isError)}
      data-operation-provider={operationError?.providerCode ?? ''}
      data-no-local-health-fallback="true"
      data-no-browser-direct-provider-ping="true"
      data-no-plaintext-secret-display="true"
      data-no-local-health-mutation="true"
    >
      <PageHeader
        title="外部 API 健康监控"
        subtitle="读取后端定时探测写入的 healthStatus、avgLatencyMs 和 successRatePct"
        actions={(
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()}>
            刷新列表
          </Button>
        )}
      />

      <Grid container spacing={2}>
        {[
          { label: '接入数量', value: configs.length },
          { label: '启用配置', value: enabledCount, color: 'primary.main' },
          { label: '正常', value: okCount, color: 'success.main' },
          { label: '异常', value: errorCount, color: 'error.main' },
          { label: '未知', value: unknownCount },
          { label: '可用率', value: configs.length > 0 ? `${Math.round(okCount / configs.length * 100)}%` : '--' },
        ].map(kpi => (
          <Grid item xs={6} sm={4} md={2} key={kpi.label}>
            <Card variant="outlined"><CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
              <Typography variant="h5" fontWeight={700} color={kpi.color}>{kpi.value}</Typography>
            </CardContent></Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Box
          data-testid="external-api-health-load-error"
          data-contract-source="/system/external-api/list"
          data-no-local-health-fallback="true"
        >
          <ErrorAlert
            title="外部 API 健康状态加载失败"
            message={error instanceof Error ? error.message : '未知错误'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      {operationError && (
        <Box
          data-testid="external-api-health-operation-error"
          data-contract-source="/system/external-api/probe"
          data-provider-code={operationError.providerCode}
          data-row-retained="true"
          data-no-local-health-fallback="true"
          data-no-browser-direct-provider-ping="true"
          data-no-local-health-mutation="true"
        >
          <ErrorAlert
            title="后端探测失败"
          message={`/system/external-api/probe 探测失败：${operationError.message}。供应商 ${operationError.providerCode} 的原健康状态已保留。`}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      {errorCount > 0 && (
        <Alert severity="error">有 {errorCount} 个外部 API 连接异常，请及时处理</Alert>
      )}

      {!isError && configs.length > 0 && (
        <Alert
          severity="info"
          data-testid="external-api-health-source-contract"
          data-contract-source="/system/external-api/list|/system/external-api/health-status"
          data-unsupported-endpoints={EXTERNAL_API_HEALTH_UNSUPPORTED_ENDPOINTS}
          data-no-browser-direct-provider-ping="true"
          data-no-local-health-fallback="true"
          data-no-local-health-mutation="true"
        >
          后端定时任务默认每 5 分钟对已启用供应商执行探测。本页卡片按钮会触发后端立即探测，不在浏览器直连第三方，也不暴露密钥。
        </Alert>
      )}

      {isLoading && <CircularProgress sx={{ mx: 'auto' }} />}

      <Grid container spacing={2}>
        {configs.map((cfg: ExternalApiConfig) => {
          const viewStatus = healthToView(cfg.healthStatus)
          return (
          <Grid item xs={12} sm={6} md={4} key={cfg.id}>
            <Card
              variant="outlined"
              data-testid="external-api-health-card"
              data-contract-source="/system/external-api/list"
              data-provider-code={cfg.providerCode}
              data-health-status={cfg.healthStatus ?? 'unknown'}
              data-view-status={viewStatus}
              data-no-local-health-fallback="true"
              data-no-local-health-mutation="true"
              data-no-plaintext-secret-display="true"
              sx={{ borderColor: viewStatus === 'error' ? 'error.light' : viewStatus === 'ok' ? 'success.light' : 'divider' }}
            >
              <CardContent sx={{ py: 1.5 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      {viewStatus === 'ok'
                        ? <CheckCircleIcon color="success" fontSize="small" />
                        : viewStatus === 'error'
                        ? <ErrorIcon color="error" fontSize="small" />
                        : <CircularProgress size={14} />}
                      <Typography variant="body2" fontWeight={600} noWrap>{cfg.providerName}</Typography>
                    </Stack>
                    <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>{cfg.providerCode}</Typography>
                  </Box>
                  <Tooltip title="立即后端探测">
                    <span>
                      <IconButton size="small" onClick={() => pingMut.mutate(cfg.providerCode)}
                        aria-label={`立即探测 ${cfg.providerName}`}
                        data-testid="external-api-health-mark-unknown"
                        data-contract-source="/system/external-api/probe"
                        data-provider-code={cfg.providerCode}
                        data-action="probe"
                        data-no-browser-direct-provider-ping="true"
                        data-no-local-health-mutation="true"
                        data-row-retained-on-error="true"
                        disabled={pinging === cfg.providerCode || pingMut.isPending}>
                        {pinging === cfg.providerCode ? <CircularProgress size={16} /> : <RefreshIcon fontSize="small" />}
                      </IconButton>
                    </span>
                  </Tooltip>
                </Stack>

                <Stack spacing={0.5} mt={1}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="caption" color="text.secondary">状态</Typography>
                    <Chip
                      label={viewStatus === 'ok' ? '正常' : viewStatus === 'error' ? '异常' : '未知'}
                      size="small"
                      color={viewStatus === 'ok' ? 'success' : viewStatus === 'error' ? 'error' : 'default'}
                    />
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="caption" color="text.secondary">配置状态</Typography>
                    <Typography variant="caption" fontWeight={600}>{cfg.isEnabled ? '已启用' : '已禁用'}</Typography>
                  </Stack>
                  {(cfg.avgLatencyMs ?? 0) > 0 && (
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="caption" color="text.secondary">延迟</Typography>
                      <Typography variant="caption" fontWeight={600}
                        color={(cfg.avgLatencyMs ?? 0) > 1000 ? 'warning.main' : 'text.primary'}>
                        {cfg.avgLatencyMs} ms
                      </Typography>
                    </Stack>
                  )}
                  {cfg.successRatePct != null && (
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="caption" color="text.secondary">成功率</Typography>
                      <Typography variant="caption" fontWeight={600}>{formatPercent(cfg.successRatePct)}</Typography>
                    </Stack>
                  )}
                  {cfg.lastHealthCheck && (
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="caption" color="text.secondary">上次检测</Typography>
                      <Typography variant="caption">{formatDate(cfg.lastHealthCheck)}</Typography>
                    </Stack>
                  )}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
          )
        })}
      </Grid>

      {!isLoading && configs.length === 0 && (
        <Box
          data-testid="external-api-health-empty"
          data-contract-source="/system/external-api/list"
          data-no-local-health-fallback="true"
          data-no-synthetic-provider-card="true"
        >
          <EmptyState
            title="暂无外部 API 配置"
            description="请先在外部 API 配置页新增供应商；健康页只展示已落库配置的探测结果。"
          />
        </Box>
      )}
    </Box>
  )
}
