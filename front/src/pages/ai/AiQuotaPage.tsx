import { useMemo, useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid, Button,
  LinearProgress, Chip, Alert, TextField, Dialog, DialogTitle,
  DialogContent, DialogActions, MenuItem, Select, InputLabel, FormControl,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import EditIcon from '@mui/icons-material/Edit'
import WarningIcon from '@mui/icons-material/Warning'
import TokenIcon from '@mui/icons-material/Token'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import { Link } from 'react-router-dom'
import { alpha } from '@mui/material/styles'
import type { GridColDef } from '@mui/x-data-grid'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import type { AiAdminQuotaOverviewVO, AiQuotaHistoryVO, AiQuotaTrendItem } from '@/types/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { formatDate } from '@/utils/date'

const TREND_DAYS = 30

const FEATURE_OPTIONS = ['all', 'script_gen', 'kb_search', 'image_gen', 'tts'] as const
const FEATURE_LABELS: Record<string, string> = {
  overall: '全站汇总',
  script_gen: '话术生成',
  kb_search: '知识库检索',
  image_gen: '图像生成',
  tts: '语音合成',
}

interface QuotaItem {
  feature: string
  limit: number
  used: number
  hasLimit: boolean
  unit?: string
  period?: string
}

function toOptionalNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function parseQuotaItems(overview: AiAdminQuotaOverviewVO | undefined): QuotaItem[] {
  const raw = overview?.items
  if (!Array.isArray(raw)) return []
  return raw.map((row) => {
    const record = row as unknown as Record<string, unknown>
    const limit = toOptionalNumber(record.limit ?? record.maxCount ?? record.max ?? record.totalQuota)
    const hasLimit = typeof record.hasLimit === 'boolean' ? record.hasLimit : limit !== null
    return {
      feature: String(record.feature ?? ''),
      limit: limit ?? 0,
      used: toOptionalNumber(record.used ?? record.usedCount ?? record.usedQuota ?? record.count) ?? 0,
      hasLimit,
      unit: record.unit ? String(record.unit) : undefined,
      period: record.period ? String(record.period) : undefined,
    }
  })
}

function pickTrendUsed(d: AiQuotaTrendItem): number {
  return toOptionalNumber(d.usedCount ?? d.used) ?? 0
}

function pickTrendMax(d: AiQuotaTrendItem): number {
  return toOptionalNumber(d.maxCount ?? d.max) ?? 0
}

function formatNumber(value: unknown): string {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? n.toLocaleString() : '--'
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error || '加载失败')
}

function compactStrings(values: Array<string | null>): string[] {
  return values.filter((value): value is string => Boolean(value))
}

function SummaryCard(props: { title: string; value: string; helper: string; icon: React.ReactNode; color?: 'primary' | 'success' | 'warning' | 'error' }) {
  const { title, value, helper, icon, color = 'primary' } = props
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" gap={1}>
          <Box>
            <Typography variant="caption" color="text.secondary">{title}</Typography>
            <Typography variant="h5" fontWeight={700} sx={{ mt: 0.5 }}>{value}</Typography>
          </Box>
          <Box sx={{ color: `${color}.main`, display: 'flex' }}>{icon}</Box>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>{helper}</Typography>
      </CardContent>
    </Card>
  )
}

function QuotaBar({ item }: { item: QuotaItem }) {
  const pct = item.hasLimit && item.limit > 0 ? Math.min(100, (item.used / item.limit) * 100) : 0
  const tone: 'error' | 'warning' | 'success' = pct >= 90 ? 'error' : pct >= 70 ? 'warning' : 'success'
  const warn = item.hasLimit && pct >= 70
  const label = FEATURE_LABELS[item.feature] ?? item.feature
  return (
    <Card variant="outlined" sx={{ mb: 1.5 }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={0.5}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="body2" fontWeight={600}>{label}</Typography>
            {item.period && <Chip label={item.period} size="small" variant="outlined" />}
          </Stack>
          <Stack direction="row" spacing={1} alignItems="center">
            {warn && <WarningIcon data-testid="ai-quota-warning-icon-surface" sx={{ fontSize: 16, color: 'warning.main' }} />}
            <Typography variant="caption" sx={{ color: `${tone}.main`, fontWeight: 600 }}>
              {item.used.toLocaleString()} / {item.hasLimit ? item.limit.toLocaleString() : '未配置'}{item.unit ? ` ${item.unit}` : ''}
            </Typography>
            <Chip label={item.hasLimit ? `${pct.toFixed(1)}%` : '未配置'} size="small"
              data-testid="ai-quota-percent-chip-surface"
              sx={(theme) => ({
                bgcolor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.2 : 0.1),
                color: `${tone}.main`,
                fontWeight: 700,
              })} />
          </Stack>
        </Stack>
        <LinearProgress
          variant="determinate" value={pct}
          data-testid="ai-quota-progress-surface"
          sx={{ height: 8, borderRadius: 4,
            bgcolor: (theme) => alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.2 : 0.1),
            '& .MuiLinearProgress-bar': { bgcolor: `${tone}.main` } }}
        />
        {!item.hasLimit ? (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
            后端未返回该功能上限，页面不按 0% 处理；请检查 ai.quota.feature 配置。
          </Typography>
        ) : null}
      </CardContent>
    </Card>
  )
}

export default function AiQuotaPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [editOpen, setEditOpen] = useState(false)
  const [editData, setEditData] = useState<Record<string, unknown>>({})
  const [editError, setEditError] = useState<string | null>(null)
  const [historyFeature, setHistoryFeature] = useState('')
  const [historyPage, setHistoryPage] = useState(0)

  const { data: quotaData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['ai-quota'],
    queryFn: () => aiApi.quotaGet(),
    refetchInterval: 60000,
  })

  const { data: trendSeries = [], isLoading: trendLoading, isError: trendError, error: trendErrorValue } = useQuery({
    queryKey: ['ai-quota-trend-chart', TREND_DAYS],
    queryFn: () => aiApi.quotaTrend({ days: TREND_DAYS }),
    staleTime: 60_000,
  })

  const { data: historyData, isFetching: historyFetching, isError: historyError, error: historyErrorValue, refetch: refetchHistory } = useQuery({
    queryKey: ['ai-quota-history', historyFeature, historyPage],
    queryFn: () => aiApi.quotaHistory({ page: historyPage, rows: 20, feature: historyFeature || undefined }),
  })

  const updateMut = useMutation({
    mutationFn: (params: Record<string, unknown>) => aiApi.quotaUpdate(params),
    onSuccess: () => {
      toast('配额已更新', 'success')
      setEditError(null)
      setEditOpen(false)
      qc.invalidateQueries({ queryKey: ['ai-quota'] })
      qc.invalidateQueries({ queryKey: ['ai-quota-trend-chart'] })
    },
    onError: (e: Error) => {
      const message = `配额更新失败（/ai/admin/quota/update）：${e.message}`
      setEditError(message)
      toast(e.message, 'error')
    },
  })

  const items = useMemo(() => parseQuotaItems(quotaData), [quotaData])

  const historyRows: AiQuotaHistoryVO[] = historyData?.list ?? []
  const historyTotal = historyData?.total ?? historyRows.length

  const historyColumns: GridColDef<AiQuotaHistoryVO>[] = [
    { field: 'feature', headerName: '功能', width: 140,
      renderCell: ({ value }) => FEATURE_LABELS[String(value)] ?? String(value) },
    { field: 'used', headerName: '用量', width: 100 },
    { field: 'limit', headerName: '上限', width: 100 },
    { field: 'period', headerName: '周期', width: 100 },
    { field: 'createTime', headerName: '时间', flex: 1, minWidth: 160,
      renderCell: ({ value }) => formatDate(String(value ?? '')) },
  ]

  const trendArr = Array.isArray(trendSeries) ? trendSeries : []
  const trendOption = useMemo(() => ({
    tooltip: { trigger: 'axis' as const },
    legend: { data: ['已用额度', '日上限(汇总)'], bottom: 0 },
    xAxis: {
      type: 'category' as const,
      data: trendArr.map((d) => String(d.date ?? '').slice(0, 10)),
    },
    yAxis: { type: 'value' as const, name: '次数' },
    series: [
      { name: '已用额度', type: 'bar' as const, data: trendArr.map((d) => pickTrendUsed(d)) },
      { name: '日上限(汇总)', type: 'line' as const, smooth: true, data: trendArr.map((d) => pickTrendMax(d)) },
    ],
    grid: { left: 48, right: 20, top: 28, bottom: 56 },
  }), [trendArr])

  const warnings = items.filter(i => i.hasLimit && i.limit > 0 && (i.used / i.limit) >= 0.7)
  const totalUsed = items.reduce((sum, item) => sum + item.used, 0)
  const totalLimit = items.filter(item => item.hasLimit).reduce((sum, item) => sum + item.limit, 0)
  const missingLimitCount = items.filter(item => !item.hasLimit).length
  const totalPct = totalLimit > 0 ? Math.min(100, (totalUsed / totalLimit) * 100) : 0
  const exhaustedCount = items.filter(item => item.hasLimit && item.limit > 0 && item.used >= item.limit).length
  const isAnyLoading = isLoading || trendLoading || historyFetching
  const loadErrors = compactStrings([
    isError ? `配额总览加载失败（/ai/admin/quota/get）：${errorMessage(error)}` : null,
    trendError ? `配额趋势加载失败（/ai/admin/dashboard/quota-trend）：${errorMessage(trendErrorValue)}` : null,
    historyError ? `历史用量加载失败（/ai/admin/quota/history）：${errorMessage(historyErrorValue)}` : null,
  ])

  const openEdit = () => {
    setEditError(null)
    const q = quotaData
    if (!q) {
      setEditData({})
      setEditOpen(true)
      return
    }
    const next: Record<string, unknown> = { dailyMax: q.dailyMax }
    for (const f of ['script_gen', 'kb_search', 'image_gen', 'tts'] as const) {
      const row = q[f] as { limit?: number } | undefined
      if (row && typeof row === 'object' && 'limit' in row) {
        next[`${f}_limit`] = row.limit
      }
    }
    setEditData(next)
    setEditOpen(true)
  }

  return (
    <Box
      data-testid="ai-quota-page"
      data-ready-endpoints="/ai/admin/quota/get,/ai/admin/dashboard/quota-trend,/ai/admin/quota/history,/ai/admin/quota/update"
      data-unsupported-endpoints="/ai/admin/quota/mock,/ai/admin/quota/local-cache,/ai/admin/dashboard/static-quota-trend"
      data-no-local-quota-fallback="true"
      data-no-static-quota-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="AI 配额管理"
        subtitle="运维侧 AI 调用额度，来自配置中心、ai_call_quota 与调用日志聚合。"
        actions={
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Button component={Link} to="/admin/payment/subscription" size="small" variant="outlined">订阅用量</Button>
            <Button component={Link} to="/admin/ai/dashboard" size="small" variant="outlined">AI 看板</Button>
            <Button startIcon={<RefreshIcon />} size="small" onClick={() => { void refetch(); void refetchHistory() }} disabled={isAnyLoading}>刷新</Button>
            <Button variant="contained" size="small" startIcon={<EditIcon />} onClick={openEdit}>调整配额上限</Button>
          </Stack>
        }
      />

      {isAnyLoading ? <LinearProgress /> : null}

      {loadErrors.length > 0 ? (
        <Alert
          severity="error"
          data-testid="ai-quota-load-error"
          data-source-endpoints="/ai/admin/quota/get,/ai/admin/dashboard/quota-trend,/ai/admin/quota/history"
          data-no-local-error-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => { void refetch(); void refetchHistory() }}>重试</Button>}
        >
          {loadErrors.join('；')}
        </Alert>
      ) : null}

      {missingLimitCount > 0 ? (
        <Alert
          severity="warning"
          data-testid="ai-quota-missing-limit-warning"
          data-degrade-source="/ai/admin/quota/get"
        >
          {missingLimitCount} 个配额项未返回上限，已按“未配置”展示；请检查 ai.quota.feature.*.limit 或 dailyMax 配置。
        </Alert>
      ) : null}

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="功能配额数" value={formatNumber(items.length)} helper="当前返回的配额功能项" icon={<TokenIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="总使用率" value={totalLimit > 0 ? `${totalPct.toFixed(1)}%` : '未配置'} helper={`${formatNumber(totalUsed)} / ${totalLimit > 0 ? formatNumber(totalLimit) : '未配置'}`} icon={<TrendingUpIcon />} color={totalPct >= 90 ? 'error' : totalPct >= 70 ? 'warning' : 'success'} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="预警功能" value={formatNumber(warnings.length)} helper="使用率超过 70% 的功能" icon={<WarningIcon />} color={warnings.length > 0 ? 'warning' : 'success'} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="已耗尽" value={formatNumber(exhaustedCount)} helper="当前用量达到或超过上限" icon={<WarningIcon />} color={exhaustedCount > 0 ? 'error' : 'success'} />
        </Grid>
      </Grid>

      <Alert severity="info">
        运行时按<strong>用户当日总额度</strong>校验（ensureQuota）；下方分功能上限主要用于<strong>展示与配置</strong>，
        与日志类型映射一致的分功能硬拦截需后端单独实现。
      </Alert>

      {warnings.length > 0 && (
        <Alert
          severity="warning"
          icon={<WarningIcon />}
          data-testid="ai-quota-threshold-warning"
          data-source-endpoint="/ai/admin/quota/get"
        >
          {warnings.map(w => `${FEATURE_LABELS[w.feature] ?? w.feature}今日已用 ${Math.round(w.used / w.limit * 100)}%`).join('；')}
        </Alert>
      )}

      {isLoading ? (
        <LinearProgress />
      ) : items.length > 0 ? (
        <Box
          data-testid="ai-quota-feature-list"
          data-source-endpoint="/ai/admin/quota/get"
          data-no-local-quota-fallback="true"
        >
          {items.map(item => <QuotaBar key={item.feature} item={item} />)}
        </Box>
      ) : (
        <Alert
          severity="info"
          data-testid="ai-quota-empty-state"
          data-source-endpoint="/ai/admin/quota/get"
          data-no-static-quota-fallback="true"
        >
          暂无配额配置
        </Alert>
      )}

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" fontWeight={600} gutterBottom>
            近{TREND_DAYS}天额度趋势（全站 ai_call_quota 按日汇总）
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
            数据来自看板接口 quota-trend，与下方表格筛选无关。
          </Typography>
          {trendLoading ? <LinearProgress sx={{ mb: 1 }} /> : null}
          {trendArr.length > 0 ? (
            <Box
              data-testid="ai-quota-trend-chart-surface"
              data-source-endpoint="/ai/admin/dashboard/quota-trend"
              data-no-static-trend-fallback="true"
              sx={{ mb: 2 }}
            >
              <ReactECharts option={trendOption} style={{ height: 220 }} />
            </Box>
          ) : (
            !trendLoading && (
              <Alert
                severity="info"
                data-testid="ai-quota-trend-empty"
                data-source-endpoint="/ai/admin/dashboard/quota-trend"
                data-no-static-trend-fallback="true"
                sx={{ mb: 2 }}
              >
                暂无趋势数据；如已有调用，请检查 ai_call_quota 是否按日写入。
              </Alert>
            )
          )}
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
            <Typography variant="subtitle2" fontWeight={600}>历史用量记录（近30天）</Typography>
            <FormControl size="small" sx={{ minWidth: 130 }}>
              <InputLabel>功能筛选</InputLabel>
              <Select value={historyFeature} label="功能筛选"
                onChange={e => { setHistoryFeature(e.target.value); setHistoryPage(0) }}>
                {FEATURE_OPTIONS.map(f => (
                  <MenuItem key={f} value={f === 'all' ? '' : f}>{f === 'all' ? '全部' : (FEATURE_LABELS[f] ?? f)}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
          <Box
            data-testid="ai-quota-history-grid"
            data-source-endpoint="/ai/admin/quota/history"
            data-pagination-mode="server"
            data-no-local-history-fallback="true"
          >
            <StandardDataGrid
              rows={historyRows}
              columns={historyColumns}
              rowCount={historyTotal}
              loading={historyFetching}
              paginationMode="server"
              paginationModel={{ page: historyPage, pageSize: 20 }}
              onPaginationModelChange={m => setHistoryPage(m.page)}
              getRowId={(r) =>
                String(r.id ?? `${String(r.feature ?? 'feature')}-${String(r.createTime ?? r.date ?? '')}-${String(r.period ?? '')}`)
              }
              sx={{ height: 260 }}
              slotProps={{ toolbar: {} }}
            />
          </Box>
        </CardContent>
      </Card>

      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>调整配额上限</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="日总调用上限（dailyMax）"
                type="number"
                fullWidth
                size="small"
                helperText="对应配置 ai.quota.daily-max，并更新当日 ai_call_quota.max_count"
                value={editData.dailyMax === undefined || editData.dailyMax === null ? '' : String(editData.dailyMax)}
                onChange={e => {
                  const v = e.target.value
                  setEditData(prev => ({
                    ...prev,
                    dailyMax: v === '' ? undefined : Number(v),
                  }))
                }}
              />
            </Grid>
            {(['script_gen', 'kb_search', 'image_gen', 'tts'] as const).map(f => (
              <Grid item xs={12} sm={6} key={f}>
                <TextField
                  label={`${FEATURE_LABELS[f]} 上限`}
                  type="number"
                  fullWidth
                  size="small"
                  value={editData[`${f}_limit`] === undefined || editData[`${f}_limit`] === null ? '' : String(editData[`${f}_limit`])}
                  onChange={e => setEditData(prev => ({ ...prev, [`${f}_limit`]: Number(e.target.value) }))}
                />
              </Grid>
            ))}
            {editError ? (
              <Grid item xs={12}>
                <Alert
                  severity="error"
                  data-testid="ai-quota-update-error"
                  data-source-endpoint="/ai/admin/quota/update"
                  data-preserves-form-input="true"
                >
                  {editError}。弹窗会保留当前 dailyMax 和各功能上限输入；请检查管理员权限、数值范围和后端配置写入。
                </Alert>
              </Grid>
            ) : null}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>取消</Button>
          <Button variant="contained" disabled={updateMut.isPending}
            onClick={() => updateMut.mutate(editData)}>保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
