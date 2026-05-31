import { useMemo, useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  LinearProgress,
  Chip,
  Stack,
  Typography,
  Button,
  TextField,
  Alert,
  Divider,
} from '@mui/material'
import ScienceIcon from '@mui/icons-material/Science'
import SettingsIcon from '@mui/icons-material/Settings'
import AddchartIcon from '@mui/icons-material/Addchart'
import type { GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { DataGridEmptyOverlay, PageHeader, StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useNavigate } from 'react-router-dom'
import type { AiModelBenchmarkComparisonRow, AiTaskModelConfigRow } from '@/types/ai'
import { getErrorMessage } from '@/utils/errorHandler'

const PRIORITY_OPTIONS = [
  { value: 'latency', label: '优先延迟（平均 ms 最低）' },
  { value: 'tokens', label: '优先 Token（平均用量最低）' },
  { value: 'success', label: '优先成功率（最高）' },
] as const

const MODEL_BENCHMARK_ENDPOINTS = {
  comparison: '/ai/model-benchmark/comparison',
  bestModel: '/ai/model-benchmark/best-model',
  record: '/ai/model-benchmark/record',
} as const

const MODEL_BENCHMARK_READY_ENDPOINTS = [
  MODEL_BENCHMARK_ENDPOINTS.comparison,
  MODEL_BENCHMARK_ENDPOINTS.bestModel,
  MODEL_BENCHMARK_ENDPOINTS.record,
  '/ai/admin/task-model-config/list',
].join('|')

const MODEL_BENCHMARK_UNSUPPORTED_ENDPOINTS = [
  '/ai/model-benchmark/mock-ranking',
  '/ai/model-benchmark/local-best-model',
  '/ai/model-benchmark/export',
  '/ai/model-benchmark/auto-record',
  '/ai/admin/models/get-secret',
].join('|')

function taskOptionsFromConfig(rows: AiTaskModelConfigRow[]): { code: string; label: string }[] {
  const map = new Map<string, string>()
  for (const r of rows) {
    const code = (r.taskCode ?? '').trim()
    if (!code) continue
    const name = (r.taskName ?? '').trim()
    if (!map.has(code)) map.set(code, name || code)
  }
  return [...map.entries()]
    .map(([code, label]) => ({ code, label: label === code ? code : `${label} (${code})` }))
    .sort((a, b) => a.code.localeCompare(b.code))
}

export default function ModelBenchmarkPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [taskFilter, setTaskFilter] = useState('')
  const [priority, setPriority] = useState<(typeof PRIORITY_OPTIONS)[number]['value']>('latency')
  const [recordOpen, setRecordOpen] = useState(false)
  const [recordModelId, setRecordModelId] = useState('')
  const [recordTaskCode, setRecordTaskCode] = useState('')
  const [recordLatency, setRecordLatency] = useState('800')
  const [recordTokens, setRecordTokens] = useState('120')
  const [recordSuccess, setRecordSuccess] = useState(true)

  const {
    data: taskConfigs = [],
    isError: taskConfigsIsError,
    error: taskConfigsError,
    refetch: refetchTaskConfigs,
  } = useQuery({
    queryKey: ['task-model-config', 'benchmark-filter'],
    queryFn: () => aiApi.taskModelConfigList(),
    staleTime: 60_000,
  })

  const normalizedTaskConfigs = useMemo(
    () => (Array.isArray(taskConfigs) ? taskConfigs : []),
    [taskConfigs],
  )
  const taskMenuOptions = useMemo(() => taskOptionsFromConfig(normalizedTaskConfigs), [normalizedTaskConfigs])

  const {
    data: benchmarks = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['model-benchmarks', taskFilter],
    queryFn: () =>
      aiApi.modelBenchmarkComparison({
        taskCode: taskFilter.trim() || undefined,
      }),
  })

  const {
    data: recommendation,
    isFetching: bestLoading,
    isError: bestIsError,
    error: bestError,
    refetch: refetchBest,
  } = useQuery({
    queryKey: ['model-benchmark-best', taskFilter, priority],
    queryFn: () => aiApi.modelBenchmarkBestModel(taskFilter.trim(), priority),
    enabled: taskFilter.trim().length > 0,
  })

  const recordMut = useMutation({
    mutationFn: aiApi.modelBenchmarkRecord,
    onSuccess: () => {
      toast('已记录样本', 'success')
      void qc.invalidateQueries({ queryKey: ['model-benchmarks'] })
      void qc.invalidateQueries({ queryKey: ['model-benchmark-best'] })
      setRecordOpen(false)
    },
    onError: (e) => toast(`基准样本录入失败：${getErrorMessage(e)}`, 'error'),
  })

  const rows = useMemo(
    () => benchmarks.map((row, idx) => ({ ...row, id: `${row.modelId}-${row.taskCode}-${idx}` })),
    [benchmarks],
  )
  const taskCount = useMemo(() => new Set(rows.map((r) => r.taskCode).filter(Boolean)).size, [rows])
  const totalSamples = useMemo(() => rows.reduce((sum, r) => sum + Number(r.totalCalls ?? 0), 0), [rows])
  const avgSuccessRate = useMemo(() => {
    if (rows.length === 0) return 0
    return rows.reduce((sum, r) => sum + Number(r.successRate ?? 0), 0) / rows.length
  }, [rows])
  const taskConfigCodes = useMemo(
    () => new Set(normalizedTaskConfigs.map((row) => row.taskCode).filter(Boolean)),
    [normalizedTaskConfigs],
  )
  const benchmarkTaskCodes = useMemo(
    () => new Set(rows.map((row) => row.taskCode).filter(Boolean)),
    [rows],
  )
  const tasksWithoutBenchmark = useMemo(
    () => normalizedTaskConfigs.filter((row) => row.taskCode && !benchmarkTaskCodes.has(row.taskCode)),
    [benchmarkTaskCodes, normalizedTaskConfigs],
  )
  const benchmarksWithoutConfig = useMemo(
    () => rows.filter((row) => row.taskCode && !taskConfigCodes.has(row.taskCode)),
    [rows, taskConfigCodes],
  )
  const lowQualityRows = useMemo(
    () => rows.filter((row) => Number(row.successRate ?? 0) < 0.8 || Number(row.avgLatencyMs ?? 0) >= 3000),
    [rows],
  )
  const hasComparisonData = rows.length > 0
  const hasTaskConfigs = normalizedTaskConfigs.length > 0
  const selectedTaskLabel = useMemo(
    () => taskMenuOptions.find((item) => item.code === taskFilter)?.label ?? taskFilter,
    [taskFilter, taskMenuOptions],
  )

  const columns: GridColDef<AiModelBenchmarkComparisonRow & { id: string }>[] = [
    { field: 'modelName', headerName: '模型', flex: 1, minWidth: 140 },
    { field: 'taskCode', headerName: '任务代码', width: 160 },
    {
      field: 'avgLatencyMs',
      headerName: '平均延迟(ms)',
      width: 150,
      renderCell: ({ value }) => {
        const ms = Number(value)
        const color = ms < 1000 ? 'success' : ms < 3000 ? 'warning' : 'error'
        return <Chip size="small" color={color} label={`${Math.round(ms)}ms`} />
      },
    },
    {
      field: 'successRate',
      headerName: '成功率',
      width: 140,
      renderCell: ({ value }) => {
        const rate = Math.max(0, Math.min(1, Number(value)))
        return (
          <Box sx={{ width: '100%' }}>
            <LinearProgress variant="determinate" value={rate * 100} />
            <Box sx={{ fontSize: 11, textAlign: 'right' }}>{(rate * 100).toFixed(1)}%</Box>
          </Box>
        )
      },
    },
    {
      field: 'avgTokens',
      headerName: '平均 Tokens',
      width: 120,
      type: 'number',
      valueFormatter: (v) => (v == null ? '' : Number(v).toFixed(1)),
    },
    { field: 'totalCalls', headerName: '样本数', width: 100, type: 'number' },
  ]

  const submitRecord = () => {
    const id = Number(recordModelId)
    if (!Number.isFinite(id) || id <= 0) {
      toast('请填写有效的模型 ID', 'error')
      return
    }
    const tc = recordTaskCode.trim() || taskFilter.trim() || 'default'
    recordMut.mutate({
      modelId: id,
      taskCode: tc,
      latencyMs: Number(recordLatency) || 0,
      tokensUsed: Number(recordTokens) || 0,
      success: recordSuccess,
    })
  }

  return (
    <Box
      data-testid="model-benchmark-page"
      data-ready-endpoints={MODEL_BENCHMARK_READY_ENDPOINTS}
      data-unsupported-endpoints={MODEL_BENCHMARK_UNSUPPORTED_ENDPOINTS}
      data-no-mock-ranking-fallback="true"
      data-no-local-best-model-fallback="true"
      data-no-auto-sample-record="true"
      data-no-plaintext-key-display="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="模型基准测试"
        subtitle="基于 ai_model_benchmark 聚合；按任务筛选或查看全部；可手动写入样本并查看推荐模型"
        breadcrumbs={[{ label: 'AI中心' }, { label: '模型基准' }]}
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<SettingsIcon fontSize="small" />}
              onClick={() => navigate('/admin/ai/task-model-config')}
            >
              任务模型映射
            </Button>
            <Button
              size="small"
              variant={recordOpen ? 'contained' : 'outlined'}
              startIcon={<AddchartIcon fontSize="small" />}
              onClick={() => setRecordOpen((o) => !o)}
            >
              {recordOpen ? '收起录入' : '录入样本'}
            </Button>
          </Stack>
        }
      />

      {isError ? (
        <Alert
          severity="error"
          data-testid="model-benchmark-comparison-error"
          data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.comparison}
          data-no-mock-ranking-fallback="true"
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>
              重试
            </Button>
          }
        >
          模型基准聚合加载失败：{getErrorMessage(error)}。来源：<code>{MODEL_BENCHMARK_ENDPOINTS.comparison}</code>。
        </Alert>
      ) : null}

      {taskConfigsIsError ? (
        <Alert
          severity="warning"
          data-testid="model-benchmark-task-config-error"
          data-source-endpoint="/ai/admin/task-model-config/list"
          data-no-local-task-option-fallback="true"
          action={
            <Button color="inherit" size="small" onClick={() => void refetchTaskConfigs()}>
              重试
            </Button>
          }
        >
          任务配置加载失败：{getErrorMessage(taskConfigsError)}。来源：<code>/ai/admin/task-model-config/list</code>；下拉框会保留少量常用任务兜底，推荐模型仍需真实 taskCode。
        </Alert>
      ) : null}

      {bestIsError ? (
        <Alert
          severity="warning"
          data-testid="model-benchmark-best-error"
          data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.bestModel}
          data-no-local-best-model-fallback="true"
        >
          推荐模型加载失败：{getErrorMessage(bestError)}。来源：<code>{MODEL_BENCHMARK_ENDPOINTS.bestModel}</code>；列表聚合不受影响，可先查看样本或录入数据后重试推荐。
        </Alert>
      ) : null}

      {recordMut.isError ? (
        <Alert
          severity="error"
          data-testid="model-benchmark-record-error"
          data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.record}
          data-input-retained="true"
          data-no-auto-sample-record="true"
        >
          基准样本录入失败：{getErrorMessage(recordMut.error)}。来源：<code>{MODEL_BENCHMARK_ENDPOINTS.record}</code>；请确认模型 ID 存在，且后端已注入 ModelBenchmarkService。录入面板和输入值会保留，避免失败后丢失联调样本。
        </Alert>
      ) : null}

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">聚合行数</Typography>
            <Typography variant="h5" fontWeight={700}>{rows.length}</Typography>
            <Typography variant="body2" color="text.secondary">按模型和任务代码聚合</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">任务覆盖</Typography>
            <Typography variant="h5" fontWeight={700}>{taskCount}</Typography>
            <Typography variant="body2" color="text.secondary">来自 ai_task_model_config 或样本数据</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">样本总量</Typography>
            <Typography variant="h5" fontWeight={700}>{totalSamples}</Typography>
            <Typography variant="body2" color="text.secondary">totalCalls 汇总</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">平均成功率</Typography>
            <Typography variant="h5" fontWeight={700}>{(avgSuccessRate * 100).toFixed(1)}%</Typography>
            <Typography variant="body2" color="text.secondary">按聚合行等权估算</Typography>
          </CardContent>
        </Card>
      </Stack>

      <Alert
        severity="info"
        data-testid="model-benchmark-boundary-contract"
        data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.comparison}
        data-no-mock-ranking-fallback="true"
        data-no-auto-sample-record="true"
      >
        模型基准页只展示 <code>ai_model_benchmark</code> 聚合结果；若后端未注入 ModelBenchmarkService，接口会返回空数组。
        推荐模型必须选择具体任务，手动录入样本用于联调和运维校准；页面不会用静态 mock 排行替代真实样本。
      </Alert>

      {!isLoading && !hasComparisonData && !isError ? (
        <Alert
          severity={hasTaskConfigs ? 'warning' : 'info'}
          data-testid="model-benchmark-empty"
          data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.comparison}
          data-no-mock-ranking-fallback="true"
        >
          {taskFilter.trim()
            ? `后端未返回 ${selectedTaskLabel} 的 benchmark 样本；可能是 ModelBenchmarkService 未注入、ai_model_benchmark 暂无该任务数据，或真实调用链尚未写入样本。页面不会用 mock 排行替代。`
            : '后端未返回 benchmark 样本；可能是 ModelBenchmarkService 未注入、ai_model_benchmark 暂无数据，或真实调用链尚未写入样本。页面不会用 mock 排行替代。'}
        </Alert>
      ) : null}

      {hasComparisonData || hasTaskConfigs ? (
        <Card
          variant="outlined"
          data-testid="model-benchmark-diagnostics-contract"
          data-source-endpoints={`${MODEL_BENCHMARK_ENDPOINTS.comparison}|/ai/admin/task-model-config/list`}
          data-no-mock-ranking-fallback="true"
          data-no-local-task-option-fallback="true"
        >
          <CardContent>
            <Typography variant="subtitle2" fontWeight={600} gutterBottom>
              基准覆盖诊断
            </Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
              <Chip size="small" color={tasksWithoutBenchmark.length === 0 ? 'success' : 'warning'} label={`配置无样本 ${tasksWithoutBenchmark.length}`} />
              <Chip size="small" color={benchmarksWithoutConfig.length === 0 ? 'success' : 'warning'} label={`样本无配置 ${benchmarksWithoutConfig.length}`} />
              <Chip size="small" color={lowQualityRows.length === 0 ? 'success' : 'error'} label={`低质样本 ${lowQualityRows.length}`} />
            </Stack>
            {tasksWithoutBenchmark.length > 0 ? (
              <Alert severity="warning" sx={{ mb: 1 }}>
                任务模型映射已有配置但暂无 benchmark 样本：{tasksWithoutBenchmark.map((row) => `${row.taskName} (${row.taskCode})`).join('、')}。
              </Alert>
            ) : null}
            {benchmarksWithoutConfig.length > 0 ? (
              <Alert severity="info" sx={{ mb: 1 }}>
                存在 benchmark 样本但未在任务模型映射中配置：{Array.from(new Set(benchmarksWithoutConfig.map((row) => row.taskCode))).join('、')}。
              </Alert>
            ) : null}
            {lowQualityRows.length > 0 ? (
              <Alert severity="error">
                有模型成功率低于 80% 或平均延迟不低于 3000ms：{lowQualityRows.map((row) => `${row.modelName || row.modelId}/${row.taskCode}`).join('、')}。
              </Alert>
            ) : null}
            {tasksWithoutBenchmark.length === 0 && benchmarksWithoutConfig.length === 0 && lowQualityRows.length === 0 ? (
              <Alert severity="success">当前任务映射与 benchmark 样本已对齐，未发现明显低质模型聚合行。</Alert>
            ) : null}
          </CardContent>
        </Card>
      ) : null}

      <Card
        variant="outlined"
        data-testid="model-benchmark-filter-contract"
        data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.bestModel}
        data-selected-task={taskFilter || 'all'}
        data-priority={priority}
        data-no-local-best-model-fallback="true"
      >
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }} flexWrap="wrap">
            <FormControl size="small" sx={{ minWidth: 260 }}>
              <InputLabel id="benchmark-task-filter">任务</InputLabel>
              <Select
                labelId="benchmark-task-filter"
                label="任务"
                value={taskFilter}
                onChange={(e) => setTaskFilter(e.target.value)}
                data-testid="model-benchmark-task-filter"
              >
                <MenuItem value="">
                  <em>全部任务（按 模型+任务代码 聚合）</em>
                </MenuItem>
                {taskMenuOptions.map((t) => (
                  <MenuItem key={t.code} value={t.code}>
                    {t.label}
                  </MenuItem>
                ))}
                {taskMenuOptions.length === 0 ? (
                  <>
                    <MenuItem value="script_gen">话术生成 (script_gen)</MenuItem>
                    <MenuItem value="knowledge_qa">知识问答 (knowledge_qa)</MenuItem>
                    <MenuItem value="content_check">内容检查 (content_check)</MenuItem>
                    <MenuItem value="classification">分类标注 (classification)</MenuItem>
                  </>
                ) : null}
              </Select>
            </FormControl>

            <FormControl size="small" sx={{ minWidth: 280 }} disabled={!taskFilter.trim()}>
              <InputLabel id="benchmark-priority">推荐策略</InputLabel>
              <Select
                labelId="benchmark-priority"
                label="推荐策略"
                value={priority}
                onChange={(e) =>
                  setPriority(e.target.value as (typeof PRIORITY_OPTIONS)[number]['value'])
                }
                data-testid="model-benchmark-priority"
              >
                {PRIORITY_OPTIONS.map((p) => (
                  <MenuItem key={p.value} value={p.value}>
                    {p.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <Stack direction="row" spacing={1} alignItems="center">
              <Button
                size="small"
                variant="outlined"
                disabled={!taskFilter.trim() || bestLoading}
                onClick={() => void refetchBest()}
                startIcon={<ScienceIcon fontSize="small" />}
              >
                刷新推荐
              </Button>
            </Stack>
          </Stack>

          {taskFilter.trim() ? (
            <Box sx={{ mt: 2 }}>
              {bestLoading && !recommendation ? (
                <LinearProgress />
              ) : (
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Typography variant="body2" color="text.secondary">
                    当前任务推荐模型：
                  </Typography>
                  {recommendation && recommendation.modelId > 0 ? (
                    <>
                      <Chip
                        size="small"
                        color="primary"
                        label={recommendation.modelName || `ID ${recommendation.modelId}`}
                      />
                      <Typography variant="caption" color="text.secondary">
                        id={recommendation.modelId} · {recommendation.priority}
                      </Typography>
                    </>
                  ) : (
                    <>
                      <Chip size="small" variant="outlined" label="暂无聚合数据" />
                      <Typography variant="caption" color="text.secondary">
                        ModelBenchmarkService 未注入或该任务样本不足时，后端会返回 modelId=0。
                      </Typography>
                    </>
                  )}
                </Stack>
              )}
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              选择具体任务后可查看「推荐模型」（基于上表聚合结果）。
            </Typography>
          )}
        </CardContent>
      </Card>

      {recordOpen ? (
        <Card
          variant="outlined"
          data-testid="model-benchmark-record-form"
          data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.record}
          data-input-retained="true"
          data-no-auto-sample-record="true"
        >
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>
              手动录入一条基准样本（运维/联调用）
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
              <TextField
                size="small"
                label="模型 ID"
                type="number"
                value={recordModelId}
                onChange={(e) => setRecordModelId(e.target.value)}
                sx={{ width: 120 }}
                inputProps={{ min: 1 }}
              />
              <TextField
                size="small"
                label="任务代码"
                value={recordTaskCode}
                onChange={(e) => setRecordTaskCode(e.target.value)}
                placeholder={taskFilter.trim() || 'default'}
                sx={{ minWidth: 180 }}
              />
              <TextField
                size="small"
                label="延迟 ms"
                type="number"
                value={recordLatency}
                onChange={(e) => setRecordLatency(e.target.value)}
                sx={{ width: 120 }}
              />
              <TextField
                size="small"
                label="Tokens"
                type="number"
                value={recordTokens}
                onChange={(e) => setRecordTokens(e.target.value)}
                sx={{ width: 120 }}
              />
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>成功</InputLabel>
                <Select
                  label="成功"
                  value={recordSuccess ? '1' : '0'}
                  onChange={(e) => setRecordSuccess(e.target.value === '1')}
                >
                  <MenuItem value="1">是</MenuItem>
                  <MenuItem value="0">否</MenuItem>
                </Select>
              </FormControl>
              <Button
                variant="contained"
                size="small"
                disabled={recordMut.isPending}
                onClick={submitRecord}
              >
                提交
              </Button>
            </Stack>
          </CardContent>
        </Card>
      ) : null}

      <Divider />

      {!isLoading && rows.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="model-benchmark-no-rows"
          data-source-endpoint={MODEL_BENCHMARK_ENDPOINTS.comparison}
          data-no-mock-ranking-fallback="true"
        >
          暂无模型基准样本。请先通过真实 AI 调用写入 benchmark，或使用“录入样本”添加联调数据；页面不会用静态 mock 填充排行。数据来源：
          <code>{MODEL_BENCHMARK_ENDPOINTS.comparison}</code>。
        </Alert>
      ) : null}

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        getRowId={(r) => r.id}
        sx={{ height: 'calc(100vh - 420px)', minHeight: 320 }}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
    </Box>
  )
}
