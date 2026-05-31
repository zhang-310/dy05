import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, TextField, Button, Chip, Stack,
  Typography, Grid, Tabs, Tab, Dialog, DialogTitle,
  DialogContent, DialogActions, Stepper, Step, StepLabel,
  FormControl, InputLabel, Select, MenuItem, IconButton,
  Paper, Tooltip, Collapse, Slider, Alert,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import PauseIcon from '@mui/icons-material/Pause'
import StopIcon from '@mui/icons-material/Stop'
import BarChartIcon from '@mui/icons-material/BarChart'
import DeleteIcon from '@mui/icons-material/Delete'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader } from '@/components/base'
import { abtestApi, type AbExperiment, type AbVariant } from '@/api/abtest'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray, readTotal } from '@/utils/response-normalize'

// ─── p-value helper ──────────────────────────────────────────────────────────
function ncdf(z: number): number {
  const t = 1 / (1 + 0.2316419 * Math.abs(z))
  const d = 0.3989423 * Math.exp(-z * z / 2)
  const p = t * (0.3193815 + t * (-0.3565638 + t * (1.7814779 + t * (-1.8212560 + t * 1.3302744))))
  return z > 0 ? 1 - d * p : d * p
}
function pValueFromVariants(variants: AbVariant[]): number {
  if (!variants || variants.length < 2) return NaN
  const v1 = variants[0], v2 = variants[1]
  if (!v1.exposures || !v2.exposures) return NaN
  const pool = (v1.conversions + v2.conversions) / (v1.exposures + v2.exposures)
  const se = Math.sqrt(pool * (1 - pool) * (1 / v1.exposures + 1 / v2.exposures))
  if (se === 0) return NaN
  const z = Math.abs(v1.conversionRate - v2.conversionRate) / se
  return 2 * (1 - ncdf(z))
}

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'info' | 'warning' | 'success' }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '进行中', color: 'info' },
  2: { label: '已完成', color: 'success' },
  3: { label: '已暂停', color: 'warning' },
}

const STATUS_TABS = [
  { value: -1, label: '全部' },
  { value: 0, label: '草稿' },
  { value: 1, label: '运行中' },
  { value: 3, label: '已暂停' },
  { value: 2, label: '已完成' },
]

const ABTEST_ENDPOINTS = {
  list: '/abtest/experiment/list',
  save: '/abtest/experiment/save',
  delete: '/abtest/experiment/delete',
  status: '/abtest/experiment/update-status',
  detail: '/admin/ai/abtest',
} as const
const ABTEST_READY_ENDPOINTS = [
  ABTEST_ENDPOINTS.list,
  ABTEST_ENDPOINTS.save,
  ABTEST_ENDPOINTS.delete,
  ABTEST_ENDPOINTS.status,
] as const
const ABTEST_UNSUPPORTED_ENDPOINTS = [
  '/abtest/experiment/mock',
  '/abtest/experiment/local-list',
  '/abtest/experiment/local-save',
  '/abtest/experiment/local-delete',
  '/abtest/experiment/local-update-status',
  '/abtest/experiment/static-result',
  '/abtest/experiment/segment-analysis',
  '/abtest/traffic-ratio/save',
] as const

// ─── Sample Size Calculator ───────────────────────────────────────────────────
function zScore(p: number): number {
  // Approximation of normal quantile
  const a = [2.515517, 0.802853, 0.010328]
  const b = [1.432788, 0.189269, 0.001308]
  const t = Math.sqrt(-2 * Math.log(p))
  return t - (a[0] + a[1] * t + a[2] * t * t) / (1 + b[0] * t + b[1] * t * t + b[2] * t * t * t)
}

function calcSampleSize(baseline: number, mde: number, power: number, alpha: number): number {
  const p = baseline / 100
  const delta = p * (mde / 100)
  const za = zScore(alpha / 2)
  const zb = zScore(1 - power)
  return Math.ceil(((za + zb) ** 2 * p * (1 - p)) / (delta ** 2))
}

function SampleCalculator({ onSampleSize }: { onSampleSize: (n: number) => void }) {
  const [open, setOpen] = useState(false)
  const [baseline, setBaseline] = useState(3.8)
  const [mde, setMde] = useState(15)
  const [power, setPower] = useState(0.8)
  const [significanceLevel, setSignificanceLevel] = useState(0.05)

  const n = useMemo(() => calcSampleSize(baseline, mde, power, significanceLevel), [baseline, mde, power, significanceLevel])
  const sessions = (n / 500).toFixed(1)

  return (
    <Paper variant="outlined" sx={{ mt: 1, borderRadius: 1, overflow: 'hidden' }}>
      <Stack direction="row" alignItems="center" sx={{ px: 2, py: 1, cursor: 'pointer', bgcolor: 'action.hover' }} onClick={() => setOpen(v => !v)}>
        <Typography variant="body2" fontWeight={600} sx={{ flex: 1 }}>样本量计算器</Typography>
        {open ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
      </Stack>
      <Collapse in={open}>
        <Box sx={{ px: 2, py: 1.5 }}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <TextField label="基准转化率 (%)" type="number" size="small" fullWidth value={baseline} onChange={e => setBaseline(Number(e.target.value))} inputProps={{ step: 0.1, min: 0.1, max: 100 }} />
            </Grid>
            <Grid item xs={6}>
              <TextField label="MDE (%)" type="number" size="small" fullWidth value={mde} onChange={e => setMde(Number(e.target.value))} inputProps={{ step: 1, min: 1, max: 100 }} helperText={`即 ≥${(baseline * mde / 100).toFixed(2)}% 提升`} />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth size="small">
                <InputLabel>统计功效</InputLabel>
                <Select label="统计功效" value={power} onChange={e => setPower(Number(e.target.value))}>
                  <MenuItem value={0.7}>70%</MenuItem>
                  <MenuItem value={0.8}>80%</MenuItem>
                  <MenuItem value={0.9}>90%</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth size="small">
                <InputLabel>显著性水平 α</InputLabel>
                <Select label="显著性水平 α" value={significanceLevel} onChange={e => setSignificanceLevel(Number(e.target.value))}>
                  <MenuItem value={0.01}>0.01</MenuItem>
                  <MenuItem value={0.05}>0.05</MenuItem>
                  <MenuItem value={0.1}>0.10</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
          <Paper
            data-testid="abtest-sample-size-result-surface"
            variant="outlined"
            sx={(theme) => ({
              mt: 2,
              p: 1.5,
              bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
              borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.34 : 0.18),
              borderRadius: 1,
            })}
          >
            <Typography variant="body2">每个变体最少需要：<strong>{n.toLocaleString()}</strong> 样本</Typography>
            <Typography variant="body2" color="text.secondary">预计约 <strong>{sessions}</strong> 场次（按历史均值 500 UV/场）</Typography>
          </Paper>
          <Button size="small" sx={{ mt: 1 }} onClick={() => { onSampleSize(n); setOpen(false) }}>应用此样本量</Button>
        </Box>
      </Collapse>
    </Paper>
  )
}
// ─── CreateWizard ────────────────────────────────────────────────────────────
interface WizardForm {
  name: string
  description: string
  experimentType: string
  targetEntityType: string
  targetEntityId: string
  metric: string
  minSampleSize: number
  variants: Partial<AbVariant>[]
}

const STEPS = ['基础信息', '添加变体', '样本评估', '确认保存']
const METRICS = ['转化率', 'GMV', '点击率', '加购率']
const EXPERIMENT_TYPES = [
  { value: 'script_style', label: '话术风格' },
  { value: 'live', label: '直播场次' },
  { value: 'video', label: '短视频' },
  { value: 'copy', label: '文案' },
]
const TARGET_ENTITY_TYPES = [
  { value: 'live_session', label: '直播场次' },
  { value: 'product', label: '商品' },
  { value: 'short_video_project', label: '短视频项目' },
]

function CreateWizard({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const toast = useToast()
  const [step, setStep] = useState(0)
  const [saveError, setSaveError] = useState('')
  const [form, setForm] = useState<WizardForm>({
    name: '', description: '', experimentType: 'script_style', targetEntityType: 'live_session', targetEntityId: '',
    metric: '转化率', minSampleSize: 1000,
    variants: [
      { variantName: '对照组 A', variantType: 'A', trafficRatio: 50, styleCode: '', content: '' },
      { variantName: '实验组 B', variantType: 'B', trafficRatio: 50, styleCode: '', content: '' },
    ],
  })
  const wizardContext = () => {
    const variantNames = form.variants.map(v => `${v.variantType || '-'}:${v.variantName || '未命名'}`).join(',')
    return `experimentName=${form.name.trim() || '未填写'}; experimentType=${form.experimentType}; targetEntityType=${form.targetEntityType || '-'}; targetEntityId=${form.targetEntityId || '-'}; metric=${form.metric}; minSampleSize=${form.minSampleSize}; variants=${variantNames}; step=${STEPS[step]}`
  }

  const saveMut = useMutation({
    mutationFn: () => {
      setSaveError('')
      return abtestApi.save({
        name: form.name,
        description: form.description,
        experimentType: form.experimentType,
        targetEntityType: form.targetEntityType || undefined,
        targetEntityId: form.targetEntityId ? Number(form.targetEntityId) : undefined,
        status: 0,
        variants: form.variants.map((variant, index) => ({
          variantName: variant.variantName ?? `变体 ${index + 1}`,
          variantType: String(variant.variantType ?? (index === 0 ? 'A' : 'B')),
          content: variant.content,
          styleCode: variant.styleCode ?? variant.scriptStyle,
        })),
      })
    },
    onSuccess: () => { toast('实验已创建', 'success'); onCreated(); onClose(); setStep(0) },
    onError: (error: Error) => { setSaveError(`创建实验失败（POST ${ABTEST_ENDPOINTS.save}）：${getErrorMessage(error)}（${wizardContext()}）`); toast(`创建失败：${getErrorMessage(error)}`, 'error') },
  })

  const addVariant = () => {
    if (form.variants.length >= 4) return
    const names = ['对照组 A', '实验组 B', '实验组 C', '实验组 D']
    const ratio = Math.floor(100 / (form.variants.length + 1))
    setForm(f => ({
      ...f,
      variants: [
        ...f.variants.map(v => ({ ...v, trafficRatio: ratio })),
        { variantName: names[f.variants.length], variantType: String.fromCharCode(65 + f.variants.length), trafficRatio: ratio, styleCode: '', content: '' },
      ],
    }))
  }

  const removeVariant = (idx: number) => {
    if (form.variants.length <= 2) return
    setForm(f => ({ ...f, variants: f.variants.filter((_, i) => i !== idx) }))
  }

  const updateVariant = (idx: number, field: string, val: string | number) => {
    setForm(f => ({ ...f, variants: f.variants.map((v, i) => i === idx ? { ...v, [field]: val } : v) }))
  }

  const canNext = () => {
    if (step === 0) return form.name.trim().length > 0
    if (step === 1) return form.variants.length >= 2 && form.variants.every(v => v.variantName && v.variantType)
    return true
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>新建实验</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        <Stepper activeStep={step} sx={{ mb: 3 }}>
          {STEPS.map(s => <Step key={s}><StepLabel>{s}</StepLabel></Step>)}
        </Stepper>

        {step === 0 && (
          <Stack spacing={2}>
            <TextField label="实验名称 *" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} fullWidth size="small" />
            <TextField label="假设描述" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth size="small" multiline minRows={2} />
            <FormControl fullWidth size="small">
              <InputLabel>实验类型</InputLabel>
              <Select label="实验类型" value={form.experimentType} onChange={e => setForm(f => ({ ...f, experimentType: e.target.value }))}>
                {EXPERIMENT_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
              </Select>
            </FormControl>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth size="small">
                  <InputLabel>目标实体类型</InputLabel>
                  <Select label="目标实体类型" value={form.targetEntityType} onChange={e => setForm(f => ({ ...f, targetEntityType: e.target.value }))}>
                    {TARGET_ENTITY_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField label="目标实体 ID" type="number" value={form.targetEntityId} onChange={e => setForm(f => ({ ...f, targetEntityId: e.target.value }))} fullWidth size="small" />
              </Grid>
            </Grid>
            <FormControl fullWidth size="small">
              <InputLabel>核心指标</InputLabel>
              <Select label="核心指标" value={form.metric} onChange={e => setForm(f => ({ ...f, metric: e.target.value }))}>
                {METRICS.map(m => <MenuItem key={m} value={m}>{m}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="最小样本量" type="number" value={form.minSampleSize} onChange={e => setForm(f => ({ ...f, minSampleSize: Number(e.target.value) }))} fullWidth size="small" />
            <SampleCalculator onSampleSize={n => setForm(f => ({ ...f, minSampleSize: n }))} />
          </Stack>
        )}

        {step === 1 && (
          <Stack spacing={2}>
            {form.variants.map((v, i) => (
              <Paper key={i} variant="outlined" sx={{ p: 2 }}>
                <Stack direction="row" spacing={1} alignItems="flex-start">
                  <Chip label={String.fromCharCode(65 + i)} size="small" color={i === 0 ? 'default' : 'primary'} sx={{ mt: 0.5 }} />
                  <TextField label="变体名称" value={v.variantName ?? ''} onChange={e => updateVariant(i, 'variantName', e.target.value)} size="small" sx={{ flex: 1 }} />
                  <TextField label="风格编码" value={v.styleCode ?? v.scriptStyle ?? ''} onChange={e => updateVariant(i, 'styleCode', e.target.value)} size="small" sx={{ flex: 1 }} placeholder="professional / friendly" />
                  <TextField label="变体内容" value={v.content ?? ''} onChange={e => updateVariant(i, 'content', e.target.value)} size="small" sx={{ flex: 1 }} placeholder="话术内容或差异说明" />
                  {i >= 2 && <IconButton size="small" color="error" onClick={() => removeVariant(i)}><DeleteIcon fontSize="small" /></IconButton>}
                </Stack>
              </Paper>
            ))}
            {form.variants.length < 4 && (
              <Button variant="outlined" startIcon={<AddIcon />} onClick={addVariant}>添加变体（{form.variants.length}/4）</Button>
            )}
          </Stack>
        )}

        {step === 2 && (
          <Stack spacing={2}>
            <Alert severity="info">当前后端 `ab_variant` 暂无流量比例字段，滑块仅用于样本量评估和前端展示，不会写入数据库。</Alert>
            <Typography variant="body2">计划流量分配比例（仅评估）</Typography>
            {form.variants.map((v, i) => (
              <Stack key={i} direction="row" alignItems="center" spacing={2}>
                <Typography variant="body2" sx={{ width: 100 }}>{v.variantName}</Typography>
                <Slider value={v.trafficRatio ?? 50} min={10} max={90} step={5} onChange={(_, val) => updateVariant(i, 'trafficRatio', Number(val))} sx={{ flex: 1 }} />
                <Typography variant="body2" sx={{ width: 40 }}>{v.trafficRatio}%</Typography>
              </Stack>
            ))}
          </Stack>
        )}

        {step === 3 && (
          <Stack spacing={1.5}>
            <Typography variant="subtitle2">配置摘要</Typography>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Stack spacing={1}>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">实验名称</Typography>
                  <Typography variant="body2">{form.name}</Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">实验类型</Typography>
                  <Typography variant="body2">{form.experimentType}</Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">核心指标</Typography>
                  <Typography variant="body2">{form.metric}</Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">变体数</Typography>
                  <Typography variant="body2">{form.variants.length}</Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">最小样本量</Typography>
                  <Typography variant="body2">{form.minSampleSize.toLocaleString()}</Typography>
                </Stack>
                {form.variants.map((v, i) => (
                  <Stack key={i} direction="row" justifyContent="space-between">
                    <Typography variant="body2" color="text.secondary">{v.variantName} / {v.variantType}</Typography>
                    <Typography variant="body2">{v.styleCode || v.content || '-'}</Typography>
                  </Stack>
                ))}
              </Stack>
            </Paper>
            {saveError ? (
              <Alert data-testid="abtest-experiment-save-error" data-input-retained="true" data-no-local-experiment-create="true" severity="error">
                {saveError}。创建失败会保留当前向导输入。
              </Alert>
            ) : null}
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>取消</Button>
        {step > 0 && <Button onClick={() => setStep(s => s - 1)}>上一步</Button>}
        {step < STEPS.length - 1
          ? <Button variant="contained" onClick={() => setStep(s => s + 1)} disabled={!canNext()}>下一步</Button>
          : <Button variant="contained" color="success" onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>保存实验</Button>
        }
      </DialogActions>
    </Dialog>
  )
}
// ─── Main Page ───────────────────────────────────────────────────────────────
export default function ExperimentsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [statusTab, setStatusTab] = useState(-1)
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState(20)
  const [wizardOpen, setWizardOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<AbExperiment | null>(null)
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['ab-experiments', { page, rows, keyword, statusTab }],
    queryFn: () => abtestApi.list({ page, rows, keyword: keyword || undefined, status: statusTab === -1 ? undefined : statusTab }),
  })
  const experimentRows = normalizeArray<AbExperiment>(data?.list ?? data)
  const experimentTotal = readTotal(data, experimentRows.length)
  const filterContext = () => {
    const tabLabel = STATUS_TABS.find(item => item.value === statusTab)?.label ?? String(statusTab)
    return `route=/admin/ai/abtest/experiments; keyword=${keyword.trim() || '空'}; statusTab=${tabLabel}; page=${page}; rows=${rows}`
  }
  const experimentContext = (row?: Partial<AbExperiment> | null, fallbackId?: number | string) => {
    const id = row?.id ?? fallbackId ?? '-'
    const name = row?.name ?? row?.experimentName ?? '未知实验'
    const status = row?.status == null ? '-' : `${row.status}/${STATUS_MAP[Number(row.status)]?.label ?? '未知'}`
    return `experimentId=${id}; experimentName=${name}; experimentType=${row?.experimentType ?? '-'}; target=${row?.targetEntityType ?? '-'}#${row?.targetEntityId ?? '-'}; status=${status}; ${filterContext()}`
  }

  const startMut = useMutation({
    mutationFn: (row: AbExperiment) => { setActionError(''); return abtestApi.start(row.id) },
    onSuccess: () => { toast('实验已启动', 'success'); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) },
    onError: (e: Error, row) => { setActionError(`启动实验失败（POST ${ABTEST_ENDPOINTS.status}）：${getErrorMessage(e)}（targetStatus=1/进行中; ${experimentContext(row)}）`); toast(`操作失败：${getErrorMessage(e)}`, 'error') },
  })
  const pauseMut = useMutation({
    mutationFn: (row: AbExperiment) => { setActionError(''); return abtestApi.pause(row.id) },
    onSuccess: () => { toast('实验已暂停', 'success'); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) },
    onError: (e: Error, row) => { setActionError(`暂停实验失败（POST ${ABTEST_ENDPOINTS.status}）：${getErrorMessage(e)}（targetStatus=3/已暂停; ${experimentContext(row)}）`); toast(`操作失败：${getErrorMessage(e)}`, 'error') },
  })
  const stopMut = useMutation({
    mutationFn: (row: AbExperiment) => { setActionError(''); return abtestApi.stop(row.id) },
    onSuccess: () => { toast('实验已完成', 'success'); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) },
    onError: (e: Error, row) => { setActionError(`结束实验失败（POST ${ABTEST_ENDPOINTS.status}）：${getErrorMessage(e)}（targetStatus=2/已完成; ${experimentContext(row)}）`); toast(`操作失败：${getErrorMessage(e)}`, 'error') },
  })
  const delMut = useMutation({
    mutationFn: (row: AbExperiment) => { setActionError(''); return abtestApi.delete(row.id) },
    onSuccess: () => { toast('已删除', 'success'); setDeleteTarget(null); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) },
    onError: (e: Error, row) => { setActionError(`删除实验失败（POST ${ABTEST_ENDPOINTS.delete}）：${getErrorMessage(e)}（${experimentContext(row)}）`); toast(`删除失败：${getErrorMessage(e)}`, 'error') },
  })

  const columns: GridColDef[] = [
    {
      field: 'name', headerName: '实验名称', flex: 2,
      renderCell: ({ row }) => (
        <Typography
          variant="body2" color="primary" sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
          onClick={() => navigate(`/admin/ai/abtest/${row.id as number}`)}
        >
          {String(row.name ?? row.experimentName ?? '-')}
        </Typography>
      ),
    },
    {
      field: 'status', headerName: '状态', width: 110,
      renderCell: ({ value }) => {
        const s = STATUS_MAP[Number(value)] ?? { label: '未知', color: 'default' as const }
        return <Chip label={s.label} size="small" color={s.color} />
      },
    },
    {
      field: 'variants', headerName: '变体数', width: 80,
      renderCell: ({ row }) => String(normalizeArray<AbVariant>(row.variants).length || '-'),
    },
    { field: 'experimentType', headerName: '实验类型', width: 120 },
    {
      field: 'targetEntityId', headerName: '目标实体', width: 130,
      renderCell: ({ row }) => row.targetEntityId ? `${row.targetEntityType ?? '-'} #${row.targetEntityId}` : '-',
    },
    {
      field: 'pValue', headerName: '显著性', width: 110, sortable: false,
      renderCell: ({ row }) => {
        const variants = normalizeArray<AbVariant>(row.variants)
        const p = pValueFromVariants(variants)
        if (isNaN(p)) return <Typography variant="caption" color="text.secondary">—</Typography>
        return (
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Typography variant="caption">{p.toFixed(3)}</Typography>
            {p < 0.05 && <CheckCircleIcon color="success" fontSize="small" titleAccess="统计显著" />}
          </Stack>
        )
      },
    },
    {
      field: 'startTime', headerName: '开始时间', width: 120,
      renderCell: ({ value }) => value ? formatDate(String(value)) : '-',
    },
    {
      field: 'actions', headerName: '操作', width: 220, sortable: false,
      renderCell: ({ row }) => {
        const status = row.status as number
        const rowName = String(row.name ?? row.experimentName ?? row.id ?? '实验')
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="详情">
              <IconButton size="small" aria-label={`查看 ${rowName}`} onClick={() => navigate(`/admin/ai/abtest/${row.id as number}`)}><BarChartIcon fontSize="small" /></IconButton>
            </Tooltip>
            {status === 0 && <Tooltip title="启动"><IconButton size="small" color="success" aria-label={`启动 ${rowName}`} onClick={() => startMut.mutate(row as AbExperiment)}><PlayArrowIcon fontSize="small" /></IconButton></Tooltip>}
            {status === 1 && <Tooltip title="暂停"><IconButton size="small" color="warning" aria-label={`暂停 ${rowName}`} onClick={() => pauseMut.mutate(row as AbExperiment)}><PauseIcon fontSize="small" /></IconButton></Tooltip>}
            {(status === 1 || status === 3) && <Tooltip title="结束"><IconButton size="small" color="error" aria-label={`结束 ${rowName}`} onClick={() => stopMut.mutate(row as AbExperiment)}><StopIcon fontSize="small" /></IconButton></Tooltip>}
            {status === 0 && <Tooltip title="删除"><IconButton size="small" color="error" aria-label={`删除 ${rowName}`} onClick={() => setDeleteTarget(row as AbExperiment)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>}
          </Stack>
        )
      },
    },
  ]

  return (
    <Box
      data-testid="abtest-experiments-page"
      data-ready-endpoints={ABTEST_READY_ENDPOINTS.join('|')}
      data-unsupported-endpoints={ABTEST_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-experiment-fallback="true"
      data-no-local-experiment-mutation="true"
      data-no-static-abtest-result="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <PageHeader
        title="A/B 实验管理"
        subtitle="对齐 `/abtest/experiment/*` 真实契约：状态 0=草稿、1=运行中、2=已完成、3=已暂停；分群分析接口尚未落库。"
        breadcrumbs={[{ label: 'AI' }, { label: 'A/B 实验' }]}
      />
      {isError && (
        <Alert
          data-testid="abtest-experiments-list-error"
          data-input-retained="true"
          data-no-local-experiment-fallback="true"
          severity="error"
          sx={{ mb: 1.5 }}
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          实验列表加载失败（POST {ABTEST_ENDPOINTS.list}）：{getErrorMessage(error)}（{filterContext()}）
        </Alert>
      )}
      {actionError ? (
        <Alert data-testid="abtest-experiments-action-error" data-no-local-experiment-mutation="true" severity="error" sx={{ mb: 1.5 }}>
          {actionError}。失败不会本地切换状态或移除实验行。
        </Alert>
      ) : null}
      {/* Status filter tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1.5 }}>
        <Tabs value={statusTab} onChange={(_, v) => { setStatusTab(v); setPage(0) }}>
          {STATUS_TABS.map(t => <Tab key={t.value} label={t.label} value={t.value} />)}
        </Tabs>
      </Box>
      {/* Toolbar */}
      <Stack direction="row" spacing={1} sx={{ mb: 1.5 }} alignItems="center">
        <TextField
          size="small" placeholder="搜索实验名称..." value={keyword}
          onChange={e => { setKeyword(e.target.value); setPage(0) }}
          sx={{ width: 240 }}
        />
        <Box sx={{ flex: 1 }} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setWizardOpen(true)}>新建实验</Button>
      </Stack>
      <Box data-testid="abtest-experiments-grid" data-source-endpoint={ABTEST_ENDPOINTS.list} data-no-local-experiment-fallback="true" sx={{ flex: 1, minHeight: 0 }}>
        <StandardDataGrid
          rows={experimentRows} columns={columns} rowCount={experimentTotal}
          loading={isFetching} paginationMode="server"
          paginationModel={{ page, pageSize: rows }}
          onPaginationModelChange={m => { setPage(m.page); setRows(m.pageSize) }}
          sx={{ flex: 1 }}
        />
      </Box>
      <CreateWizard open={wizardOpen} onClose={() => setWizardOpen(false)} onCreated={() => qc.invalidateQueries({ queryKey: ['ab-experiments'] })} />
      <ConfirmDialog
        open={deleteTarget !== null}
        content={`确定要删除该实验吗？endpoint=${ABTEST_ENDPOINTS.delete}; ${experimentContext(deleteTarget)}。删除失败会保留实验行和当前筛选。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending}
      />
    </Box>
  )
}
