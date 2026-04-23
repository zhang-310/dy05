import { useMemo, useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid, Button,
  LinearProgress, Chip, Alert, TextField, Dialog, DialogTitle,
  DialogContent, DialogActions, MenuItem, Select, InputLabel, FormControl,
  Link as MuiLink,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import EditIcon from '@mui/icons-material/Edit'
import WarningIcon from '@mui/icons-material/Warning'
import { Link } from 'react-router-dom'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
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
  unit?: string
  period?: string
}

function parseQuotaItems(overview: AiAdminQuotaOverviewVO | undefined): QuotaItem[] {
  const raw = overview?.items
  if (!Array.isArray(raw)) return []
  return raw.map((row) => ({
    feature: String(row.feature ?? ''),
    limit: Number(row.limit ?? 0),
    used: Number(row.used ?? 0),
    unit: row.unit ? String(row.unit) : undefined,
    period: row.period ? String(row.period) : undefined,
  }))
}

function pickTrendUsed(d: AiQuotaTrendItem): number {
  return Number(d.usedCount ?? d.used ?? 0)
}

function pickTrendMax(d: AiQuotaTrendItem): number {
  return Number(d.maxCount ?? d.max ?? d.remaining ?? 0)
}

function QuotaBar({ item }: { item: QuotaItem }) {
  const pct = item.limit > 0 ? Math.min(100, (item.used / item.limit) * 100) : 0
  const color = pct >= 90 ? '#d32f2f' : pct >= 70 ? '#ff9800' : '#2e7d32'
  const warn = pct >= 70
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
            {warn && <WarningIcon sx={{ fontSize: 16, color: '#ff9800' }} />}
            <Typography variant="caption" sx={{ color, fontWeight: 600 }}>
              {item.used.toLocaleString()} / {item.limit.toLocaleString()}{item.unit ? ` ${item.unit}` : ''}
            </Typography>
            <Chip label={`${pct.toFixed(1)}%`} size="small"
              sx={{ bgcolor: color + '22', color, fontWeight: 700 }} />
          </Stack>
        </Stack>
        <LinearProgress
          variant="determinate" value={pct}
          sx={{ height: 8, borderRadius: 4,
            bgcolor: color + '22',
            '& .MuiLinearProgress-bar': { bgcolor: color } }}
        />
      </CardContent>
    </Card>
  )
}

export default function AiQuotaPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [editOpen, setEditOpen] = useState(false)
  const [editData, setEditData] = useState<Record<string, unknown>>({})
  const [historyFeature, setHistoryFeature] = useState('')
  const [historyPage, setHistoryPage] = useState(0)

  const { data: quotaData, isLoading, refetch } = useQuery({
    queryKey: ['ai-quota'],
    queryFn: () => aiApi.quotaGet(),
    refetchInterval: 60000,
  })

  const { data: trendSeries = [], isLoading: trendLoading } = useQuery({
    queryKey: ['ai-quota-trend-chart', TREND_DAYS],
    queryFn: () => aiApi.quotaTrend({ days: TREND_DAYS }),
    staleTime: 60_000,
  })

  const { data: historyData, isFetching: historyFetching } = useQuery({
    queryKey: ['ai-quota-history', historyFeature, historyPage],
    queryFn: () => aiApi.quotaHistory({ page: historyPage, rows: 20, feature: historyFeature || undefined }),
  })

  const updateMut = useMutation({
    mutationFn: (params: Record<string, unknown>) => aiApi.quotaUpdate(params),
    onSuccess: () => {
      toast('配额已更新', 'success')
      setEditOpen(false)
      qc.invalidateQueries({ queryKey: ['ai-quota'] })
      qc.invalidateQueries({ queryKey: ['ai-quota-trend-chart'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
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

  const warnings = items.filter(i => i.limit > 0 && (i.used / i.limit) >= 0.7)

  const openEdit = () => {
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
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={1}>
        <Box>
          <Typography variant="h6" fontWeight={700}>AI 配额管理</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            运维侧 AI 调用额度（配置中心 + ai_call_quota / 调用日志聚合）。与
            <MuiLink component={Link} to="/admin/payment/subscription">订阅用量</MuiLink>
            {' '}不同。更多图表见
            <MuiLink component={Link} to="/admin/ai/dashboard"> AI 看板</MuiLink>。
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button startIcon={<RefreshIcon />} onClick={() => refetch()} disabled={isLoading}>刷新</Button>
          <Button variant="contained" startIcon={<EditIcon />} onClick={openEdit}>调整配额上限</Button>
        </Stack>
      </Stack>

      <Alert severity="info">
        运行时按<strong>用户当日总额度</strong>校验（ensureQuota）；下方分功能上限主要用于<strong>展示与配置</strong>，
        与日志类型映射一致的分功能硬拦截需后端单独实现。
      </Alert>

      {warnings.length > 0 && (
        <Alert severity="warning" icon={<WarningIcon />}>
          {warnings.map(w => `${FEATURE_LABELS[w.feature] ?? w.feature}今日已用 ${Math.round(w.used / w.limit * 100)}%`).join('；')}
        </Alert>
      )}

      {isLoading ? (
        <LinearProgress />
      ) : items.length > 0 ? (
        <Box>{items.map(item => <QuotaBar key={item.feature} item={item} />)}</Box>
      ) : (
        <Alert severity="info">暂无配额配置</Alert>
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
            <Box sx={{ mb: 2 }}>
              <ReactECharts option={trendOption} style={{ height: 220 }} />
            </Box>
          ) : (
            !trendLoading && <Alert severity="info" sx={{ mb: 2 }}>暂无趋势数据</Alert>
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
