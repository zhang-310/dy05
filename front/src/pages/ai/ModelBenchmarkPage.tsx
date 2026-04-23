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
import { PageHeader, StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useNavigate } from 'react-router-dom'
import type { AiModelBenchmarkComparisonRow, AiTaskModelConfigRow } from '@/types/ai'

const PRIORITY_OPTIONS = [
  { value: 'latency', label: '优先延迟（平均 ms 最低）' },
  { value: 'tokens', label: '优先 Token（平均用量最低）' },
  { value: 'success', label: '优先成功率（最高）' },
] as const

function isComparisonRow(o: unknown): o is AiModelBenchmarkComparisonRow {
  if (!o || typeof o !== 'object') return false
  return Number.isFinite((o as Partial<AiModelBenchmarkComparisonRow>).modelId)
}

function normalizeComparisonRows(raw: unknown): AiModelBenchmarkComparisonRow[] {
  if (!Array.isArray(raw)) return []
  const out: AiModelBenchmarkComparisonRow[] = []
  for (const item of raw) {
    if (!isComparisonRow(item)) continue
    const modelId = Number(item.modelId)
    if (!Number.isFinite(modelId)) continue
    out.push({
      modelId,
      modelName: item.modelName,
      taskCode: item.taskCode ?? '',
      avgLatencyMs: item.avgLatencyMs ?? 0,
      successRate: item.successRate ?? 0,
      avgTokens: item.avgTokens ?? 0,
      totalCalls: item.totalCalls ?? 0,
    })
  }
  return out
}

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

  const { data: taskConfigs = [] } = useQuery({
    queryKey: ['task-model-config', 'benchmark-filter'],
    queryFn: () => aiApi.taskModelConfigList(),
    staleTime: 60_000,
  })

  const taskMenuOptions = useMemo(() => taskOptionsFromConfig(taskConfigs), [taskConfigs])

  const {
    data: benchmarks = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['model-benchmarks', taskFilter],
    queryFn: async () => {
      const res = await aiApi.modelBenchmarkComparison({
        taskCode: taskFilter.trim() || undefined,
      })
      return normalizeComparisonRows(res)
    },
  })

  const {
    data: recommendation,
    isFetching: bestLoading,
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
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const rows = useMemo(
    () => benchmarks.map((row, idx) => ({ ...row, id: `${row.modelId}-${row.taskCode}-${idx}` })),
    [benchmarks],
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
    <Box data-testid="model-benchmark-page" sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
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
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>
              重试
            </Button>
          }
        >
          {error instanceof Error ? error.message : '加载失败'}
        </Alert>
      ) : null}

      <Card variant="outlined">
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
                    <Chip size="small" variant="outlined" label="暂无聚合数据，可先录入样本" />
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
        <Card variant="outlined">
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

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        getRowId={(r) => r.id}
        sx={{ height: 'calc(100vh - 420px)', minHeight: 320 }}
      />
    </Box>
  )
}
