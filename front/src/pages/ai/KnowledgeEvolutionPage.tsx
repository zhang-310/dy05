import { useState, useMemo } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import {
  Box, Typography, Card, CardContent, Grid, Chip, Button,
  Stack, LinearProgress, Alert, Divider,
  FormControl, InputLabel, Select, MenuItem, Link,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import HistoryIcon from '@mui/icons-material/History'
import AssessmentIcon from '@mui/icons-material/Assessment'
import StorageIcon from '@mui/icons-material/Storage'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { GridColDef } from '@mui/x-data-grid'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { aiApi, type KnowledgeBase } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import ReactECharts from 'echarts-for-react'
import { AGENT_TYPES, parseScopeKbId, TASK_STATUS_MAP } from '@/pages/ai/evolution/engineConstants'
import {
  EVOLVE_TASK_STATUS_FILTER_OPTIONS,
  pageHasRunningTask,
  statusToUi,
} from '@/pages/ai/evolution/taskQueueListFilters'
import type { EvolveRoiPayload, QualityScoreTrendPoint } from '@/types/evolutionEngine'
import { AnalysisResultSection } from '@/pages/ai/knowledge-evolution/AnalysisResultSection'

const TREND_DAY_OPTIONS = [7, 14, 30] as const
const KNOWLEDGE_EVOLUTION_READY_ENDPOINTS = [
  '/ai/knowledge-base/list',
  '/ai/evolution/task/list',
  '/ai/evolution/roi',
  '/ai/evolution/score-trend',
  '/ai/evolution/task/trigger',
  '/ai/knowledge-evolution/analyze',
  '/ai/knowledge-evolution/auto-optimize',
  '/ai/knowledge-evolution/report',
].join('|')

const KNOWLEDGE_EVOLUTION_UNSUPPORTED_ENDPOINTS = [
  '/ai/evolution/task/mock',
  '/ai/evolution/roi/mock',
  '/ai/evolution/score-trend/mock',
  '/ai/knowledge-evolution/local-analyze',
  '/ai/knowledge-evolution/local-optimize',
  '/ai/knowledge-evolution/export-local',
  '/script/template/writeback',
].join('|')

/** ECharts axis tooltip single series row (narrow, avoids `any`) */
interface AxisTooltipRow {
  axisValue?: string
  value?: number | string
  dataIndex?: number
}

function trendTooltipFormatter(params: unknown, points: QualityScoreTrendPoint[]): string {
  const row = (Array.isArray(params) ? params[0] : params) as AxisTooltipRow | undefined
  const idx = row?.dataIndex ?? 0
  const axis = row?.axisValue ?? ''
  const val = row?.value ?? 0
  return `${axis}<br/>质量分: ${val}<br/>任务数: ${points[idx]?.count ?? 0}`
}

function errorMessage(error: unknown): string {
  if (error instanceof Error && error.message) return error.message
  return '未知错误'
}

function buildQualityDiagnostics(
  roiData: EvolveRoiPayload,
  trendArr: QualityScoreTrendPoint[],
  taskTotal: number,
  selectedKbName: string,
): string[] {
  const avgScore = Number(roiData.avgScore ?? 0)
  const totalRuns = Number(roiData.totalRuns ?? 0)
  const coveredDocs = Number(roiData.coveredDocs ?? 0)
  const messages: string[] = []

  if (avgScore > 0 && avgScore < 60) {
    messages.push(`平均质量分 ${avgScore}/100 偏低：优先检查低分任务详情、知识分块是否过短、是否缺少可检索来源和引用。`)
  }
  if (totalRuns === 0) {
    messages.push(`${selectedKbName} 近期开启后尚无完成任务：可能是主题池为空、调度未触发、模型未配置或任务仍在异步排队。`)
  }
  if (coveredDocs === 0) {
    messages.push('覆盖知识库为 0：当前 ROI 没有关联到可统计 KB，请确认任务 targetKbId/kbId 是否写入。')
  }
  if (trendArr.length === 0) {
    messages.push('暂无趋势点：质量评分任务尚未落库，或筛选范围/天数内没有完成任务。')
  }
  if (taskTotal === 0) {
    messages.push('任务列表为空：若后台日志出现“主题池为空”或“无可进化内容”，这是明确降级，不会写入任务行。')
  }
  if (messages.length === 0) {
    messages.push('质量链路正常：已有 ROI、趋势或任务数据，可从任务详情继续追踪评分明细和优化结果。')
  }
  return messages
}

export default function KnowledgeEvolutionPage() {
  const theme = useTheme()
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [scopeKbId, setScopeKbId] = useState('')
  const [trendDays, setTrendDays] = useState<number>(7)
  const [taskType, setTaskType] = useState('')
  const [statusCategory, setStatusCategory] = useState('')
  const scopeKbNumeric = parseScopeKbId(scopeKbId)
  const scopeKey = scopeKbId || 'all'
  const statusParam = statusCategory === '' ? undefined : Number(statusCategory)

  const kbsQuery = useQuery({
    queryKey: ['knowledge-evolution-kb-options'],
    queryFn: () => aiApi.kbList({ page: 0, rows: 200 }),
  })

  const tasksQuery = useQuery({
    queryKey: ['evolve-task-list', page, scopeKey, taskType || 'all', statusCategory || 'all'],
    queryFn: () => aiApi.evolveTaskList({
      page,
      rows: 20,
      taskType: taskType || undefined,
      kbId: scopeKbNumeric,
      status: statusParam,
    }),
    refetchInterval: (query) => {
      const list = query.state.data?.list ?? []
      return pageHasRunningTask(list) ? 8000 : 30000
    },
  })

  const roiQuery = useQuery({
    queryKey: ['evolve-roi', scopeKey],
    queryFn: () => aiApi.evolveRoi(scopeKbNumeric != null ? { kbId: scopeKbNumeric } : {}),
    refetchInterval: 30000,
  })

  const trendQuery = useQuery({
    queryKey: ['evolve-score-trend', scopeKey, trendDays],
    queryFn: () => aiApi.scoreTrend({ days: trendDays, kbId: scopeKbNumeric }),
    refetchInterval: 30000,
  })

  const onScopeKbChange = (v: string) => {
    setScopeKbId(v)
    setPage(0)
  }

  const onFilterChange = (nextTaskType: string, nextStatus: string) => {
    setTaskType(nextTaskType)
    setStatusCategory(nextStatus)
    setPage(0)
  }

  const triggerMut = useMutation({
    mutationFn: (taskTypeParam: string) => aiApi.evolveTaskTrigger({
      taskType: taskTypeParam,
      targetKbId: scopeKbNumeric,
    }),
    onSuccess: () => {
      toast('进化任务已启动', 'success')
      const invalidateEvolutionQueries = () => {
        qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
        qc.invalidateQueries({ queryKey: ['evolve-roi'] })
        qc.invalidateQueries({ queryKey: ['evolve-score-trend'] })
      }
      invalidateEvolutionQueries()
      window.setTimeout(invalidateEvolutionQueries, 1200)
      window.setTimeout(invalidateEvolutionQueries, 3500)
    },
    onError: (e: Error) => toast(e.message || '触发失败', 'error'),
  })

  const roiData: EvolveRoiPayload = roiQuery.data ?? {}
  const trendArr: QualityScoreTrendPoint[] = trendQuery.data ?? []
  const taskList = tasksQuery.data?.list ?? []
  const taskTotal = tasksQuery.data?.total ?? 0
  const kbList = (kbsQuery.data ?? []) as KnowledgeBase[]
  const selectedKb = kbList.find(k => String(k.id) === scopeKbId)
  const selectedKbName = selectedKb?.kbName ?? '全部知识库'
  const diagnostics = buildQualityDiagnostics(roiData, trendArr, taskTotal, selectedKbName)

  const hasDataError =
    kbsQuery.isError || tasksQuery.isError || roiQuery.isError || trendQuery.isError

  const dataErrorItems = [
    {
      active: kbsQuery.isError,
      label: '知识库列表加载失败',
      endpoint: '/ai/knowledge-base/list',
      message: errorMessage(kbsQuery.error),
    },
    {
      active: tasksQuery.isError,
      label: '进化任务列表加载失败',
      endpoint: '/ai/evolution/task/list',
      message: errorMessage(tasksQuery.error),
    },
    {
      active: roiQuery.isError,
      label: 'ROI 指标加载失败',
      endpoint: '/ai/evolution/roi',
      message: errorMessage(roiQuery.error),
    },
    {
      active: trendQuery.isError,
      label: '质量趋势加载失败',
      endpoint: '/ai/evolution/score-trend',
      message: errorMessage(trendQuery.error),
    },
  ].filter(item => item.active)

  const refetchAll = () => {
    void kbsQuery.refetch()
    void tasksQuery.refetch()
    void roiQuery.refetch()
    void trendQuery.refetch()
  }

  const primaryMain = theme.palette.primary.main
  const chartOption = useMemo(() => ({
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: unknown) => trendTooltipFormatter(params, trendArr),
    },
    xAxis: {
      type: 'category' as const,
      data: trendArr.map(t => String(t.date ?? '').substring(5)),
      axisLabel: { fontSize: 11 },
    },
    yAxis: { type: 'value' as const, name: '质量分', max: 100 },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    series: [{
      name: '知识质量',
      type: 'line' as const,
      smooth: true,
      data: trendArr.map(t => t.score ?? 0),
      itemStyle: { color: primaryMain },
      areaStyle: { color: alpha(primaryMain, 0.12) },
    }],
  }), [trendArr, primaryMain])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'taskNo', headerName: '任务编号', width: 200 },
    { field: 'taskType', headerName: '进化角度', width: 120,
      renderCell: ({ value }) => {
        const agent = AGENT_TYPES.find(a => a.code === String(value ?? ''))
        return <Typography variant="body2">{agent?.label ?? String(value ?? '')}</Typography>
      } },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const ui = statusToUi(value)
        const s = TASK_STATUS_MAP[ui] ?? TASK_STATUS_MAP[0]
        return <Chip label={s.label} color={s.color} size="small" />
      } },
    { field: 'progress', headerName: '进度', width: 140,
      renderCell: ({ value }) => (
        <Box sx={{ width: '100%', display: 'flex', alignItems: 'center', gap: 1 }}>
          <LinearProgress variant="determinate" value={Number(value ?? 0)} sx={{ flex: 1, height: 6, borderRadius: 3 }} />
          <Typography variant="caption">{value ?? 0}%</Typography>
        </Box>
      ) },
    { field: 'scoreTotal', headerName: '质量分', width: 90,
      renderCell: ({ value }) => value != null && value > 0 ? (
        <Chip label={value} size="small" color={value >= 80 ? 'success' : value >= 60 ? 'warning' : 'default'} />
      ) : <Typography variant="caption" color="text.disabled">-</Typography>
    },
    { field: 'targetId', headerName: '目标KB', width: 90 },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
  ]

  return (
    <Box sx={{
      p: 3,
      pb: 4,
      width: '100%',
      maxWidth: '100%',
      minHeight: 'calc(100vh - 120px)',
      display: 'flex',
      flexDirection: 'column',
      gap: 2,
      bgcolor: 'background.default',
      // 避免作为 flex 子项时被侧栏+main 的布局压扁固定高度区块（表格/图表）
      alignSelf: 'stretch',
      boxSizing: 'border-box',
    }}
      data-testid="knowledge-evolution-workbench"
      data-ready-endpoints={KNOWLEDGE_EVOLUTION_READY_ENDPOINTS}
      data-unsupported-endpoints={KNOWLEDGE_EVOLUTION_UNSUPPORTED_ENDPOINTS}
      data-no-local-roi-fallback="true"
      data-no-local-task-fallback="true"
      data-no-local-trend-fallback="true"
      data-no-client-score-synthesis="true"
      data-no-optimistic-evolution-mutation="true"
    >
      <PageHeader
        title="进化监控看板"
        subtitle="按选定知识库展示进化任务、ROI、质量趋势与自动优化结果；调度触发不等于任务必然入库，页面会明确显示空任务、低分和趋势缺失原因。"
        breadcrumbs={[{ label: 'AI 中心' }, { label: '知识进化' }]}
        actions={(
          <Link component={RouterLink} to="/admin/ai/evolution" variant="body2">
            打开自进化引擎
          </Link>
        )}
      />

      {hasDataError ? (
        <Alert
          severity="error"
          data-testid="knowledge-evolution-load-error"
          data-no-local-roi-fallback="true"
          data-no-local-task-fallback="true"
          data-no-local-trend-fallback="true"
          action={(
            <Button color="inherit" size="small" onClick={refetchAll}>
              重试
            </Button>
          )}
        >
          <Typography variant="subtitle2" fontWeight={600} component="div">
            部分数据加载失败，请检查登录状态、后端服务和接口日志。
          </Typography>
          <Stack component="ul" sx={{ pl: 2, my: 0.5 }} spacing={0.25}>
            {dataErrorItems.map(item => (
              <Typography key={item.endpoint} component="li" variant="body2">
                {item.label}（{item.endpoint}）：{item.message}
              </Typography>
            ))}
          </Stack>
        </Alert>
      ) : null}

      {triggerMut.isError ? (
        <Alert
          severity="error"
          data-testid="knowledge-evolution-trigger-error"
          data-no-local-task-insertion="true"
          data-filter-retained="true"
        >
          进化任务触发失败（/ai/evolution/task/trigger）：{errorMessage(triggerMut.error)}
        </Alert>
      ) : null}

      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        flexWrap="wrap"
        gap={1}
        data-testid="knowledge-evolution-filter-contract"
        data-selected-kb-id={scopeKbId || 'all'}
        data-task-type={taskType || 'all'}
        data-status-category={statusCategory || 'all'}
        data-server-filter="true"
      >
        <Typography variant="h6" fontWeight={600} color="text.primary">指标与快捷触发</Typography>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel id="ke-scope-kb">知识库范围</InputLabel>
            <Select
              labelId="ke-scope-kb"
              label="知识库范围"
              value={scopeKbId}
              onChange={e => onScopeKbChange(e.target.value)}
            >
              <MenuItem value="">全部知识库</MenuItem>
              {kbList.map(k => (
                <MenuItem key={k.id} value={String(k.id)}>{k.kbName ?? `KB #${k.id}`}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button size="small" variant="outlined" startIcon={<HistoryIcon />}
            onClick={() => triggerMut.mutate('quality')} disabled={triggerMut.isPending}>
            质量巡检
          </Button>
          <Button size="small" variant="outlined" startIcon={<AssessmentIcon />}
            onClick={() => triggerMut.mutate('timeliness')} disabled={triggerMut.isPending}>
            时效性检查
          </Button>
          <Button size="small" variant="contained" color="primary" startIcon={<AutoFixHighIcon />}
            onClick={() => triggerMut.mutate('gap')} disabled={triggerMut.isPending}>
            触发进化
          </Button>
        </Stack>
      </Stack>

      <Alert
        severity={diagnostics.some(m => m.includes('偏低') || m.includes('为空') || m.includes('尚无')) ? 'warning' : 'success'}
        icon={<WarningAmberIcon fontSize="inherit" />}
        data-testid="knowledge-evolution-quality-diagnostics"
        data-source-endpoints="/ai/evolution/roi|/ai/evolution/score-trend|/ai/evolution/task/list"
        data-no-client-score-synthesis="true"
        data-no-local-diagnostic-fallback="true"
        data-selected-kb-name={selectedKbName}
      >
        <Typography variant="subtitle2" fontWeight={600} component="div">
          质量分与任务链路诊断（{selectedKbName}）
        </Typography>
        <Stack component="ul" sx={{ pl: 2, my: 0.5 }} spacing={0.25}>
          {diagnostics.map((msg, i) => (
            <Typography key={i} component="li" variant="body2">
              {msg}
            </Typography>
          ))}
        </Stack>
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">新增知识</Typography>
                  <Typography variant="h5" fontWeight={700} color="text.primary">{String(roiData.newKnowledge ?? 0)}</Typography>
                </Box>
                <TrendingUpIcon sx={{ fontSize: 32 }} color="primary" />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">平均质量分</Typography>
                  <Typography variant="h5" fontWeight={700} color="text.primary">{String(roiData.avgScore ?? 0)}</Typography>
                </Box>
                <AssessmentIcon sx={{ fontSize: 32 }} color="success" />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">进化次数</Typography>
                  <Typography variant="h5" fontWeight={700} color="text.primary">{String(roiData.totalRuns ?? 0)}</Typography>
                </Box>
                <CheckCircleIcon sx={{ fontSize: 32 }} color="info" />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">覆盖知识库</Typography>
                  <Typography variant="h5" fontWeight={700} color="text.primary">{String(roiData.coveredDocs ?? 0)}</Typography>
                </Box>
                <StorageIcon sx={{ fontSize: 32 }} color="warning" />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {trendArr.length > 0 && (
        <Card
          variant="outlined"
          sx={{ flexShrink: 0, width: '100%' }}
          data-testid="knowledge-evolution-trend-contract"
          data-source-endpoint="/ai/evolution/score-trend"
          data-no-local-trend-fallback="true"
          data-trend-count={String(trendArr.length)}
        >
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1} sx={{ mb: 1 }}>
              <Typography variant="subtitle2" fontWeight={600} color="text.primary">
                知识质量趋势（近 {trendDays} 天）
              </Typography>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel id="ke-trend-days">趋势天数</InputLabel>
                <Select
                  labelId="ke-trend-days"
                  label="趋势天数"
                  value={trendDays}
                  onChange={e => setTrendDays(Number(e.target.value))}
                >
                  {TREND_DAY_OPTIONS.map(d => (
                    <MenuItem key={d} value={d}>{d} 天</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
            <Box sx={{ width: '100%', height: 240, minHeight: 240, flexShrink: 0 }}>
              <ReactECharts option={chartOption} style={{ width: '100%', height: '100%' }} opts={{ renderer: 'canvas' }} />
            </Box>
          </CardContent>
        </Card>
      )}

      {trendArr.length === 0 && !trendQuery.isFetching && !trendQuery.isError ? (
        <Card
          variant="outlined"
          sx={{ flexShrink: 0, width: '100%' }}
          data-testid="knowledge-evolution-trend-empty"
          data-source-endpoint="/ai/evolution/score-trend"
          data-no-local-trend-fallback="true"
        >
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1}>
              <Typography variant="subtitle2" color="text.secondary">暂无趋势数据，可切换天数或等待任务打分</Typography>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel id="ke-trend-days-empty">趋势天数</InputLabel>
                <Select
                  labelId="ke-trend-days-empty"
                  label="趋势天数"
                  value={trendDays}
                  onChange={e => setTrendDays(Number(e.target.value))}
                >
                  {TREND_DAY_OPTIONS.map(d => (
                    <MenuItem key={d} value={d}>{d} 天</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
          </CardContent>
        </Card>
      ) : null}

      <Box sx={{ flexShrink: 0, width: '100%', minWidth: 0 }}>
        <AnalysisResultSection onAfterOptimize={refetchAll} />
      </Box>

      <Divider />

      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel id="ke-task-type">Agent 类型</InputLabel>
          <Select
            labelId="ke-task-type"
            label="Agent 类型"
            value={taskType}
            onChange={e => onFilterChange(e.target.value, statusCategory)}
          >
            <MenuItem value="">全部</MenuItem>
            {AGENT_TYPES.map(a => (
              <MenuItem key={a.code} value={a.code}>{a.label}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel id="ke-task-status">状态</InputLabel>
          <Select
            labelId="ke-task-status"
            label="状态"
            value={statusCategory}
            onChange={e => onFilterChange(taskType, e.target.value)}
          >
            {EVOLVE_TASK_STATUS_FILTER_OPTIONS.map(o => (
              <MenuItem key={o.value || 'all'} value={o.value}>{o.label}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="subtitle1" fontWeight={600} color="text.primary">进化任务列表</Typography>
        {taskTotal > 0 && (
          <Typography variant="caption" color="text.secondary">
            共 {taskTotal} 条记录
          </Typography>
        )}
      </Box>

      {taskTotal === 0 && !tasksQuery.isFetching && !tasksQuery.isError && (
        <Alert
          severity="info"
          data-testid="knowledge-evolution-task-empty"
          data-source-endpoint="/ai/evolution/task/list"
          data-no-local-task-fallback="true"
        >
          暂无匹配任务。若日志里已有「触发定时知识进化」但列表仍为空：请把「知识库范围」选为「全部知识库」、状态选「全部」；并查看服务端是否出现「主题池为空（kbId=…）」等跳过说明（该情况下不会写入任务表）。
        </Alert>
      )}

      <Box
        sx={{
          width: '100%',
          minWidth: 0,
          height: 520,
          minHeight: 480,
          flexShrink: 0,
        }}
      >
        <StandardDataGrid
          rows={taskList}
          columns={columns}
          rowCount={taskTotal}
          loading={tasksQuery.isFetching}
          paginationMode="server"
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={m => setPage(m.page)}
          pageSizeOptions={[20, 50]}
          getRowId={(row) =>
            row.id != null ? String(row.id) : `t-${String(row.taskNo ?? '')}-${String(row.createTime ?? '')}`}
        />
      </Box>
    </Box>
  )
}
