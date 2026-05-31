import { Box, Card, CardContent, Typography, Stack, LinearProgress, Chip, Grid, Alert, Button } from '@mui/material'
import { paymentApi } from '@/api/payment'
import { UsageQuotaResponse } from './subscriptionPageModel'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'

const QUOTA_READY_ENDPOINT = '/payment/usage/quota'
const QUOTA_SUPPORT_ENDPOINT = '/payment/subscription/check-quota'
const QUOTA_CONTRACT_MESSAGE = '`/payment/usage/quota` 已接入真实用量汇总，消耗量来自 `payment_usage_record`，上限来自 `/payment/subscription/check-quota`；配额告警配置仍显式降级。'

export default function UsageQuotaPage() {
  const { data: quota, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['usage-quota'],
    queryFn: () => paymentApi.usageQuota(),
    refetchInterval: 60000,
  })

  const quotaMap = quota as UsageQuotaResponse | undefined
  const entries = quotaMap ? Object.entries(quotaMap) : []
  const errorMessage = error instanceof Error ? error.message : '配额接口异常，请检查 /payment/usage/quota。'
  const normalizedEntries = entries.filter(([, val]) => typeof val === 'object' && val !== null)

  return (
    <Box
      data-testid="payment-usage-quota-workbench"
      data-contract-scope="payment-usage-quota"
      data-ready-endpoint={QUOTA_READY_ENDPOINT}
      data-support-endpoint={QUOTA_SUPPORT_ENDPOINT}
      data-unsupported-actions="quota-alert-config"
      data-contract-status="ready"
      data-no-quota-alert-config-call="true"
      data-no-mock-quota-dimensions="true"
      sx={{ maxWidth: 980, mx: 'auto', py: 2 }}
    >
      <PageHeader
        title="额度使用详情"
        breadcrumbs={[{ label: '支付中心' }, { label: '额度详情' }]}
        subtitle="按 60 秒自动刷新展示真实用量汇总；额度来自订阅上限和当月使用记录。"
        actions={<Button variant="outlined" size="small" onClick={() => void refetch()} disabled={isLoading}>刷新</Button>}
      />
      {isLoading && <LinearProgress sx={{ mb: 2 }} />}
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}>
          额度加载失败：{errorMessage} 当前页面不会使用本地配额假数据兜底。
        </Alert>
      )}
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="payment-usage-quota-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-scope="payment-usage-quota"
        data-contract-status="ready"
        data-ready-endpoint={QUOTA_READY_ENDPOINT}
        data-support-endpoint={QUOTA_SUPPORT_ENDPOINT}
        data-no-quota-alert-config-call="true"
        data-no-mock-quota-dimensions="true"
      >
        {QUOTA_CONTRACT_MESSAGE}
      </Alert>
      {!isLoading && !isError && normalizedEntries.length === 0 && (
        <Alert severity="warning" sx={{ mb: 2 }} data-testid="payment-usage-quota-empty" data-no-mock-quota-dimensions="true">
          暂无额度数据。请确认订阅套餐已创建，且 `/payment/usage/quota` 返回了指标。
        </Alert>
      )}
      <Grid container spacing={2}>
        {entries.map(([key, val]) => {
          const v = val as { used?: number; total?: number; label?: string; resetTime?: string } | null
          if (typeof v !== 'object' || v === null) return null
          const used = v.used ?? 0
          const total = v.total ?? 0
          const pct = total > 0 ? Math.min(100, Math.round((used / total) * 100)) : 0
          const colorMap = pct > 80 ? 'error' : pct > 60 ? 'warning' : 'success'
          const limitState = total > 0 ? 'configured' : 'unlimited'
          return (
            <Grid item xs={12} sm={6} key={key}>
              <Card
                variant="outlined"
                data-testid="payment-usage-quota-card"
                data-contract-status="ready"
                data-contract-endpoint={QUOTA_READY_ENDPOINT}
                data-quota-key={key}
                data-quota-limit-state={limitState}
                data-quota-used={used}
                data-quota-total={total}
                data-quota-percent={pct}
              >
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
                  {total <= 0 && (
                    <Typography variant="caption" color="text.secondary" display="block" mt={0.5}>
                      未配置上限，当前仅展示消耗量。
                    </Typography>
                  )}
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
