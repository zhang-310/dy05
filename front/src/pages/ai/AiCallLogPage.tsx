import { useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  LinearProgress,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import BarChartIcon from '@mui/icons-material/BarChart'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import TokenIcon from '@mui/icons-material/Token'
import VisibilityIcon from '@mui/icons-material/Visibility'
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid'
import ReactECharts from 'echarts-for-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import type { AiCallLogVO, AiCallTypeDistributionItem, AiCallVolumeTrendItem } from '@/types/ai'
import { formatDate } from '@/utils/date'

const CALL_TYPE_LABELS: Record<string, string> = {
  kb_search: '知识库检索',
  script_gen: '话术生成',
  script_generate: '话术生成',
  live_script_full: '直播整场话术',
  live_script_slot: '直播单品话术',
  live_script_skeleton: '直播脚本骨架',
  live_script_refine: '直播话术精修',
  live_script_chat: '直播话术问答',
  live_analysis: '直播分析',
  text2img: '文生图',
  image_gen: '图像生成',
  tts: '语音合成',
  video: '视频生成',
  agent_chat: '智能体对话',
  embedding: '向量嵌入',
  chat: '对话',
}

const CALL_TYPE_OPTIONS = [
  { value: '', label: '全部类型' },
  { value: 'kb_search', label: '知识库检索' },
  { value: 'script_gen', label: '话术生成' },
  { value: 'text2img', label: '文生图' },
  { value: 'tts', label: '语音合成' },
  { value: 'video', label: '视频生成' },
  { value: 'chat', label: '对话' },
]

function callTypeLabel(value: unknown): string {
  const key = String(value ?? '')
  return (CALL_TYPE_LABELS[key] ?? key) || '--'
}

function toNumber(value: unknown): number {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? n : 0
}

function isSuccessStatus(value: unknown): boolean {
  const status = toNumber(value)
  return status === 1 || status === 200
}

function isFallbackCall(value: unknown): boolean {
  return value === true || toNumber(value) === 1
}

function formatDuration(value: unknown): string {
  const ms = toNumber(value)
  if (ms <= 0) return '--'
  if (ms >= 1000) return `${(ms / 1000).toFixed(2)} s`
  return `${Math.round(ms)} ms`
}

function formatCount(value: unknown): string {
  return toNumber(value).toLocaleString()
}

function getTrendLabel(row: AiCallVolumeTrendItem): string {
  return String(row.date ?? row.time ?? row.hour ?? '')
}

function getTrendTotal(row: AiCallVolumeTrendItem): number {
  return toNumber(row.total ?? row.callCount ?? row.count)
}

function getTrendSuccess(row: AiCallVolumeTrendItem): number {
  return toNumber(row.success ?? row.successCount)
}

function getTrendFailed(row: AiCallVolumeTrendItem): number {
  return toNumber(row.failed ?? row.failedCount ?? row.error)
}

function getDistName(row: AiCallTypeDistributionItem): string {
  return callTypeLabel(row.type ?? row.taskType ?? row.callType)
}

function getDistValue(row: AiCallTypeDistributionItem): number {
  return toNumber(row.count ?? row.total)
}

function tryFormatJson(value: unknown): string {
  if (value === null || value === undefined || value === '') return '--'
  if (typeof value !== 'string') return JSON.stringify(value, null, 2)
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error || '加载失败')
}

function compactStrings(values: Array<string | null>): string[] {
  return values.filter((value): value is string => Boolean(value))
}

function SummaryCard(props: {
  title: string
  value: string
  helper: string
  icon: React.ReactNode
  color?: 'primary' | 'success' | 'warning' | 'error'
}) {
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
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          {helper}
        </Typography>
      </CardContent>
    </Card>
  )
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} gap={0.5} justifyContent="space-between">
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 120 }}>{label}</Typography>
      <Typography variant="body2" sx={{ textAlign: { xs: 'left', sm: 'right' }, wordBreak: 'break-word' }}>{value}</Typography>
    </Stack>
  )
}

export default function AiCallLogPage() {
  const theme = useTheme()
  const queryClient = useQueryClient()
  const [draftKeyword, setDraftKeyword] = useState('')
  const [keyword, setKeyword] = useState('')
  const [callType, setCallType] = useState('')
  const [status, setStatus] = useState('')
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 20 })
  const [selectedLog, setSelectedLog] = useState<AiCallLogVO | null>(null)

  const trendQuery = useQuery({
    queryKey: ['ai-call-trend', 14],
    queryFn: () => aiApi.callVolumeTrend({ days: 14 }),
    refetchInterval: 60000,
  })
  const distQuery = useQuery({
    queryKey: ['ai-call-distribution', 14],
    queryFn: () => aiApi.callTypeDistribution({ days: 14 }),
    refetchInterval: 60000,
  })
  const logQuery = useQuery({
    queryKey: ['ai-admin-call-log', keyword, callType, status, paginationModel.page, paginationModel.pageSize],
    queryFn: () => aiApi.adminCallLogList({
      page: paginationModel.page,
      rows: paginationModel.pageSize,
      keyword: keyword || undefined,
      callType: callType || undefined,
      status: status === '' ? undefined : Number(status),
    }),
    refetchInterval: 60000,
  })

  const trend = Array.isArray(trendQuery.data) ? trendQuery.data : []
  const dist = Array.isArray(distQuery.data) ? distQuery.data : []
  const logRows = logQuery.data?.list ?? []
  const rowCount = Number(logQuery.data?.total ?? logRows.length)
  const pageSuccess = logRows.filter(row => isSuccessStatus(row.status)).length
  const pageFailed = logRows.filter(row => !isSuccessStatus(row.status)).length
  const pageFallback = logRows.filter(row => isFallbackCall(row.isFallback)).length
  const pageTokens = logRows.reduce((sum, row) => sum + toNumber(row.totalTokens), 0)
  const totalTrendCalls = trend.reduce((sum, row) => sum + getTrendTotal(row), 0)
  const totalTrendFailed = trend.reduce((sum, row) => sum + getTrendFailed(row), 0)
  const isFetching = trendQuery.isFetching || distQuery.isFetching || logQuery.isFetching
  const errors = compactStrings([
    trendQuery.error ? `调用趋势加载失败（/ai/admin/dashboard/call-volume-trend）：${errorMessage(trendQuery.error)}` : null,
    distQuery.error ? `类型分布加载失败（/ai/admin/dashboard/call-type-distribution）：${errorMessage(distQuery.error)}` : null,
    logQuery.error ? `调用日志加载失败（/ai/admin/call-log/search）：${errorMessage(logQuery.error)}` : null,
  ])
  const trendFailureUnreported = totalTrendCalls > 0
    && totalTrendFailed === 0
    && trend.every(row => row.failed == null && row.failedCount == null && row.error == null)
  const trendTotalColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main
  const trendSuccessColor = theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main
  const trendFailedColor = theme.palette.mode === 'dark' ? theme.palette.error.light : theme.palette.error.main
  const distHoverShadowColor = alpha(
    theme.palette.common.black,
    theme.palette.mode === 'dark' ? 0.55 : 0.25,
  )

  const trendOption = useMemo(() => ({
    tooltip: { trigger: 'axis' as const },
    legend: { data: ['调用量', '成功量', '失败量'], top: 0 },
    xAxis: {
      type: 'category' as const,
      data: trend.map(getTrendLabel),
      axisLabel: { rotate: 30, fontSize: 11 },
    },
    yAxis: { type: 'value' as const, name: '调用次数' },
    series: [
      { name: '调用量', type: 'line' as const, smooth: true, data: trend.map(getTrendTotal), itemStyle: { color: trendTotalColor } },
      { name: '成功量', type: 'line' as const, smooth: true, data: trend.map(getTrendSuccess), itemStyle: { color: trendSuccessColor } },
      { name: '失败量', type: 'line' as const, smooth: true, data: trend.map(getTrendFailed), itemStyle: { color: trendFailedColor } },
    ],
    grid: { left: 50, right: 20, bottom: 60, top: 42 },
  }), [trend, trendFailedColor, trendSuccessColor, trendTotalColor])

  const distOption = useMemo(() => ({
    tooltip: { trigger: 'item' as const, formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical' as const, left: 'left', top: 8 },
    series: [{
      name: '调用类型',
      type: 'pie' as const,
      radius: ['42%', '70%'],
      center: ['58%', '54%'],
      data: dist.map(row => ({ name: getDistName(row), value: getDistValue(row) })),
      emphasis: { itemStyle: { shadowBlur: 8, shadowOffsetX: 0, shadowColor: distHoverShadowColor } },
    }],
  }), [dist, distHoverShadowColor])

  const refreshAll = () => {
    void queryClient.invalidateQueries({ queryKey: ['ai-call-trend'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-call-distribution'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-admin-call-log'] })
  }

  const applySearch = () => {
    setPaginationModel(prev => ({ ...prev, page: 0 }))
    setKeyword(draftKeyword.trim())
  }

  const columns: GridColDef<AiCallLogVO>[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    {
      field: 'createTime',
      headerName: '时间',
      width: 170,
      renderCell: ({ value }) => formatDate(String(value ?? '')),
    },
    {
      field: 'callType',
      headerName: '类型',
      width: 130,
      renderCell: ({ value }) => <Chip size="small" label={callTypeLabel(value)} variant="outlined" />,
    },
    { field: 'modelCode', headerName: '模型', width: 130, renderCell: ({ value }) => String(value ?? '--') },
    {
      field: 'inputSummary',
      headerName: '输入摘要',
      flex: 1,
      minWidth: 220,
      renderCell: ({ value }) => (
        <Tooltip title={String(value ?? '')} placement="top-start">
          <Typography variant="body2" noWrap sx={{ maxWidth: '100%' }}>{String(value ?? '--')}</Typography>
        </Tooltip>
      ),
    },
    { field: 'totalTokens', headerName: 'Tokens', width: 100, align: 'right', headerAlign: 'right', renderCell: ({ value }) => formatCount(value) },
    { field: 'durationMs', headerName: '耗时', width: 100, renderCell: ({ value }) => formatDuration(value) },
    {
      field: 'status',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => (
        <Chip size="small" label={isSuccessStatus(value) ? '成功' : '失败'} color={isSuccessStatus(value) ? 'success' : 'error'} />
      ),
    },
    {
      field: 'isFallback',
      headerName: '降级',
      width: 90,
      renderCell: ({ value }) => (
        <Chip size="small" label={isFallbackCall(value) ? '是' : '否'} color={isFallbackCall(value) ? 'warning' : 'default'} variant={isFallbackCall(value) ? 'filled' : 'outlined'} />
      ),
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 90,
      sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" startIcon={<VisibilityIcon fontSize="small" />} onClick={() => setSelectedLog(row)}>
          详情
        </Button>
      ),
    },
  ]

  return (
    <Box
      data-testid="ai-call-log-page"
      data-ready-endpoints="/ai/admin/dashboard/call-volume-trend,/ai/admin/dashboard/call-type-distribution,/ai/admin/call-log/search"
      data-unsupported-endpoints="/ai/admin/call-log/mock,/ai/admin/call-log/local-cache,/ai/admin/dashboard/static-call-trend"
      data-no-local-call-log-fallback="true"
      data-no-static-chart-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="AI 调用分析"
        subtitle="查看 AI 调用趋势、类型分布、失败/降级情况和最近调用日志。"
        actions={
          <Button startIcon={<RefreshIcon />} size="small" variant="contained" onClick={refreshAll} disabled={isFetching}>
            刷新
          </Button>
        }
      />

      {isFetching ? <LinearProgress /> : null}

      {errors.length > 0 ? (
        <Alert
          severity="error"
          data-testid="ai-call-log-load-error"
          data-source-endpoints="/ai/admin/dashboard/call-volume-trend,/ai/admin/dashboard/call-type-distribution,/ai/admin/call-log/search"
          data-no-local-error-fallback="true"
          action={<Button color="inherit" size="small" onClick={refreshAll}>重试</Button>}
        >
          {errors.join('；')}
        </Alert>
      ) : null}

      {trendFailureUnreported ? (
        <Alert
          severity="info"
          data-testid="ai-call-log-trend-downgrade"
          data-degrade-source="/ai/admin/dashboard/call-volume-trend"
        >
          调用趋势接口当前只返回成功调用量，失败量未单独上报；失败诊断以调用日志表的状态列为准。
        </Alert>
      ) : null}

      {logQuery.isSuccess && logRows.length === 0 ? (
        <Alert
          severity="info"
          data-testid="ai-call-log-empty-state"
          data-source-endpoint="/ai/admin/call-log/search"
          data-no-static-log-fallback="true"
        >
          当前筛选没有调用日志；如近期已有 AI 请求，请确认 `ai_call_log` 写入、`/ai/admin/call-log/search` 参数和管理员查询权限。
        </Alert>
      ) : null}

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="近 14 天调用量" value={formatCount(totalTrendCalls)} helper="来自调用趋势聚合接口" icon={<BarChartIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="当前页成功/失败" value={`${pageSuccess}/${pageFailed}`} helper={`当前筛选共 ${rowCount.toLocaleString()} 条日志`} icon={<ErrorOutlineIcon />} color={pageFailed > 0 ? 'warning' : 'success'} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="当前页 Token" value={formatCount(pageTokens)} helper="按日志 totalTokens 汇总，纯检索链路可能是估算值" icon={<TokenIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard title="降级调用" value={formatCount(pageFallback)} helper="当前页 fallback 标记数量，来自日志 isFallback" icon={<RefreshIcon />} color={pageFallback > 0 ? 'warning' : 'primary'} />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} lg={8}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>调用量趋势（近 14 天）</Typography>
              {trend.length > 0 ? (
                <Box
                  data-testid="ai-call-log-trend-chart-surface"
                  data-source-endpoint="/ai/admin/dashboard/call-volume-trend"
                  data-no-static-chart-fallback="true"
                >
                  <ReactECharts option={trendOption} style={{ height: 320 }} />
                </Box>
              ) : (
                <Alert
                  severity="info"
                  data-testid="ai-call-log-trend-empty"
                  data-source-endpoint="/ai/admin/dashboard/call-volume-trend"
                  data-no-static-trend-fallback="true"
                >
                  暂无调用趋势数据。若近期已有 AI 请求，请检查 ai_call_log 是否正常写入。
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>调用类型分布</Typography>
              {dist.length > 0 ? (
                <Box
                  data-testid="ai-call-log-distribution-chart-surface"
                  data-source-endpoint="/ai/admin/dashboard/call-type-distribution"
                  data-no-static-distribution-fallback="true"
                  data-hover-shadow-color={distHoverShadowColor}
                >
                  <ReactECharts option={distOption} style={{ height: 320 }} />
                </Box>
              ) : (
                <Alert
                  severity="info"
                  data-testid="ai-call-log-distribution-empty"
                  data-source-endpoint="/ai/admin/dashboard/call-type-distribution"
                  data-no-static-distribution-fallback="true"
                >
                  暂无调用类型分布数据。
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card variant="outlined">
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={1.5} sx={{ mb: 1.5 }}>
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>调用日志</Typography>
              <Typography variant="caption" color="text.secondary">管理端真实接口：/ai/admin/call-log/search</Typography>
            </Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ minWidth: { md: 620 } }}>
              <TextField
                size="small"
                placeholder="搜索输入摘要"
                value={draftKeyword}
                onChange={(event) => setDraftKeyword(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') applySearch()
                }}
                sx={{ flex: 1 }}
              />
              <TextField
                select
                size="small"
                label="类型"
                value={callType}
                onChange={(event) => {
                  setPaginationModel(prev => ({ ...prev, page: 0 }))
                  setCallType(event.target.value)
                }}
                sx={{ minWidth: 140 }}
              >
                {CALL_TYPE_OPTIONS.map(option => (
                  <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
              </TextField>
              <TextField
                select
                size="small"
                label="状态"
                value={status}
                onChange={(event) => {
                  setPaginationModel(prev => ({ ...prev, page: 0 }))
                  setStatus(event.target.value)
                }}
                sx={{ minWidth: 110 }}
              >
                <MenuItem value="">全部</MenuItem>
                <MenuItem value="1">成功</MenuItem>
                <MenuItem value="0">失败</MenuItem>
              </TextField>
              <Button startIcon={<SearchIcon />} variant="outlined" onClick={applySearch}>查询</Button>
            </Stack>
          </Stack>
          <Box
            data-testid="ai-call-log-grid"
            data-source-endpoint="/ai/admin/call-log/search"
            data-pagination-mode="server"
            data-no-local-call-log-fallback="true"
            sx={{ height: 430 }}
          >
            <StandardDataGrid
              rows={logRows}
              columns={columns}
              rowCount={rowCount}
              loading={logQuery.isFetching}
              paginationMode="server"
              paginationModel={paginationModel}
              onPaginationModelChange={setPaginationModel}
              getRowId={(row) => String(row.id)}
              pageSizeOptions={[10, 20, 50, 100]}
            />
          </Box>
        </CardContent>
      </Card>

      <Dialog open={selectedLog != null} onClose={() => setSelectedLog(null)} maxWidth="md" fullWidth>
        <DialogTitle>调用日志详情</DialogTitle>
        <DialogContent>
          {selectedLog ? (
            <Stack spacing={1.25} sx={{ mt: 1 }}>
              <DetailRow label="日志 ID" value={selectedLog.id} />
              <DetailRow label="调用类型" value={callTypeLabel(selectedLog.callType)} />
              <DetailRow label="模型" value={selectedLog.modelCode || '--'} />
              <DetailRow label="状态" value={<Chip size="small" label={isSuccessStatus(selectedLog.status) ? '成功' : '失败'} color={isSuccessStatus(selectedLog.status) ? 'success' : 'error'} />} />
              <DetailRow label="降级" value={<Chip size="small" label={isFallbackCall(selectedLog.isFallback) ? '是' : '否'} color={isFallbackCall(selectedLog.isFallback) ? 'warning' : 'default'} variant={isFallbackCall(selectedLog.isFallback) ? 'filled' : 'outlined'} />} />
              <DetailRow label="耗时" value={formatDuration(selectedLog.durationMs)} />
              <DetailRow label="Tokens" value={`${formatCount(selectedLog.promptTokens)} / ${formatCount(selectedLog.completionTokens)} / ${formatCount(selectedLog.totalTokens)}`} />
              <DetailRow label="关联短视频" value={selectedLog.linkedVideoId ?? '--'} />
              <DetailRow label="关联直播场次" value={selectedLog.linkedSessionId ?? '--'} />
              <DetailRow label="创建时间" value={formatDate(String(selectedLog.createTime ?? ''))} />
              <Divider />
              <Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>输入摘要</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{selectedLog.inputSummary || '--'}</Typography>
              </Box>
              {selectedLog.errorMessage ? (
                <Alert severity="error">{selectedLog.errorMessage}</Alert>
              ) : null}
              <Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>阶段耗时 / 引用 Chunk</Typography>
                <Box
                  component="pre"
                  data-testid="ai-call-log-stage-preview"
                  sx={(theme) => ({
                    m: 0,
                    p: 1.5,
                    bgcolor: theme.palette.mode === 'dark'
                      ? theme.palette.background.default
                      : alpha(theme.palette.common.black, 0.04),
                    border: `1px solid ${theme.palette.divider}`,
                    borderRadius: 1,
                    color: 'text.primary',
                    overflow: 'auto',
                    fontSize: 12,
                  })}
                >
                  {tryFormatJson(selectedLog.stageTimings || selectedLog.referencedChunkIds)}
                </Box>
              </Box>
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedLog(null)}>关闭</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
