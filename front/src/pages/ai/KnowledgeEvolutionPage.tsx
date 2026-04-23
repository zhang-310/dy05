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
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
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

  const hasDataError =
    kbsQuery.isError || tasksQuery.isError || roiQuery.isError || trendQuery.isError

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
    }}>
      <Box>
        <Typography variant="h5" color="text.primary">进化监控看板</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          按选定知识库展示进化任务、ROI 与质量趋势；完整编排与多 Tab 能力见
          <Link component={RouterLink} to="/admin/ai/evolution" sx={{ ml: 0.5 }}>自进化引擎</Link>
          。服务端「触发定时知识进化」仅表示调度已调用引擎；若该库无可用主题或未配置进化模型，不会产生任务行，请结合同时间段「主题池为空」等 WARN 日志排查。
        </Typography>
      </Box>

      {hasDataError ? (
        <Alert
          severity="error"
          action={(
            <Button color="inherit" size="small" onClick={refetchAll}>
              重试
            </Button>
          )}
        >
          部分数据加载失败：
          {kbsQuery.isError ? ' 知识库列表' : ''}
          {tasksQuery.isError ? ' 任务列表' : ''}
          {roiQuery.isError ? ' ROI' : ''}
          {trendQuery.isError ? ' 趋势' : ''}
          。请检查登录与后端服务。
        </Alert>
      ) : null}

      <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1}>
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
        <Card variant="outlined" sx={{ flexShrink: 0, width: '100%' }}>
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
        <Card variant="outlined" sx={{ flexShrink: 0, width: '100%' }}>
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
        <Alert severity="info">
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
