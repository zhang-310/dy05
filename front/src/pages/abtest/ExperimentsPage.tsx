import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, TextField, Button, Chip, Stack,
  Typography, Grid, Tabs, Tab, Dialog, DialogTitle,
  DialogContent, DialogActions, Stepper, Step, StepLabel,
  FormControl, InputLabel, Select, MenuItem, IconButton,
  Paper, Tooltip, Collapse, Slider,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import PauseIcon from '@mui/icons-material/Pause'
import StopIcon from '@mui/icons-material/Stop'
import BarChartIcon from '@mui/icons-material/BarChart'
import DeleteIcon from '@mui/icons-material/Delete'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { abtestApi, type AbVariant } from '@/api/abtest'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

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
  2: { label: '已暂停', color: 'warning' },
  3: { label: '已结束', color: 'success' },
}

const STATUS_TABS = [
  { value: -1, label: '全部' },
  { value: 0, label: '草稿' },
  { value: 1, label: '运行中' },
  { value: 2, label: '已暂停' },
  { value: 3, label: '已完成' },
]

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
  const [alpha, setAlpha] = useState(0.05)

  const n = useMemo(() => calcSampleSize(baseline, mde, power, alpha), [baseline, mde, power, alpha])
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
                <Select label="显著性水平 α" value={alpha} onChange={e => setAlpha(Number(e.target.value))}>
                  <MenuItem value={0.01}>0.01</MenuItem>
                  <MenuItem value={0.05}>0.05</MenuItem>
                  <MenuItem value={0.1}>0.10</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
          <Paper sx={{ mt: 2, p: 1.5, bgcolor: 'primary.50', borderRadius: 1 }}>
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
  experimentName: string
  description: string
  metric: string
  minSampleSize: number
  variants: Partial<AbVariant>[]
  trafficSplit: number
  sessionId?: number
}

const STEPS = ['基础信息', '添加变体', '流量配置', '确认启动']
const METRICS = ['转化率', 'GMV', '点击率', '加购率']

function CreateWizard({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const toast = useToast()
  const [step, setStep] = useState(0)
  const [form, setForm] = useState<WizardForm>({
    experimentName: '', description: '', metric: '转化率', minSampleSize: 1000,
    variants: [
      { variantName: '对照组 A', trafficRatio: 50, scriptStyle: '' },
      { variantName: '实验组 B', trafficRatio: 50, scriptStyle: '' },
    ],
    trafficSplit: 50,
  })

  const saveMut = useMutation({
    mutationFn: () => abtestApi.save({ experimentName: form.experimentName, description: form.description, trafficSplit: form.trafficSplit }),
    onSuccess: () => { toast('实验已创建', 'success'); onCreated(); onClose(); setStep(0) },
    onError: () => toast('创建失败', 'error'),
  })

  const addVariant = () => {
    if (form.variants.length >= 4) return
    const names = ['对照组 A', '实验组 B', '实验组 C', '实验组 D']
    const ratio = Math.floor(100 / (form.variants.length + 1))
    setForm(f => ({
      ...f,
      variants: [
        ...f.variants.map(v => ({ ...v, trafficRatio: ratio })),
        { variantName: names[f.variants.length], trafficRatio: ratio, scriptStyle: '' },
      ],
    }))
  }

  const removeVariant = (idx: number) => {
    if (form.variants.length <= 2) return
    setForm(f => ({ ...f, variants: f.variants.filter((_, i) => i !== idx) }))
  }

  const updateVariant = (idx: number, field: string, val: string) => {
    setForm(f => ({ ...f, variants: f.variants.map((v, i) => i === idx ? { ...v, [field]: val } : v) }))
  }

  const canNext = () => {
    if (step === 0) return form.experimentName.trim().length > 0
    if (step === 1) return form.variants.every(v => v.variantName)
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
            <TextField label="实验名称 *" value={form.experimentName} onChange={e => setForm(f => ({ ...f, experimentName: e.target.value }))} fullWidth size="small" />
            <TextField label="假设描述" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth size="small" multiline minRows={2} />
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
                  <TextField label="话术标识" value={v.scriptStyle ?? ''} onChange={e => updateVariant(i, 'scriptStyle', e.target.value)} size="small" sx={{ flex: 1 }} placeholder="关联话术ID或描述" />
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
            <Typography variant="body2">流量分配比例（每个变体）</Typography>
            {form.variants.map((v, i) => (
              <Stack key={i} direction="row" alignItems="center" spacing={2}>
                <Typography variant="body2" sx={{ width: 100 }}>{v.variantName}</Typography>
                <Slider value={v.trafficRatio ?? 50} min={10} max={90} step={5} onChange={(_, val) => updateVariant(i, 'trafficRatio', String(val))} sx={{ flex: 1 }} />
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
                  <Typography variant="body2">{form.experimentName}</Typography>
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
                    <Typography variant="body2" color="text.secondary">{v.variantName}</Typography>
                    <Typography variant="body2">{v.trafficRatio}% 流量</Typography>
                  </Stack>
                ))}
              </Stack>
            </Paper>
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>取消</Button>
        {step > 0 && <Button onClick={() => setStep(s => s - 1)}>上一步</Button>}
        {step < STEPS.length - 1
          ? <Button variant="contained" onClick={() => setStep(s => s + 1)} disabled={!canNext()}>下一步</Button>
          : <Button variant="contained" color="success" onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>启动实验</Button>
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
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['ab-experiments', { page, rows, keyword, statusTab }],
    queryFn: () => abtestApi.list({ page, rows, experimentName: keyword || undefined, status: statusTab === -1 ? undefined : statusTab }),
  })

  const startMut = useMutation({ mutationFn: abtestApi.start, onSuccess: () => { toast('实验已启动', 'success'); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) }, onError: () => toast('操作失败', 'error') })
  const pauseMut = useMutation({ mutationFn: abtestApi.pause, onSuccess: () => { toast('实验已暂停', 'success'); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) }, onError: () => toast('操作失败', 'error') })
  const stopMut = useMutation({ mutationFn: abtestApi.stop, onSuccess: () => { toast('实验已结束', 'success'); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) }, onError: () => toast('操作失败', 'error') })
  const delMut = useMutation({ mutationFn: abtestApi.delete, onSuccess: () => { toast('已删除', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['ab-experiments'] }) }, onError: () => toast('删除失败', 'error') })

  const columns: GridColDef[] = [
    {
      field: 'experimentName', headerName: '实验名称', flex: 2,
      renderCell: ({ row }) => (
        <Typography
          variant="body2" color="primary" sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
          onClick={() => navigate(`/admin/abtest/${row.id as number}`)}
        >
          {row.experimentName as string}
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
      renderCell: ({ row }) => String((row.variants as AbVariant[] | undefined)?.length ?? '-'),
    },
    { field: 'trafficSplit', headerName: '流量分配', width: 90, renderCell: ({ value }) => `${value}%` },
    {
      field: 'pValue', headerName: '显著性', width: 110, sortable: false,
      renderCell: ({ row }) => {
        const variants = (row.variants as AbVariant[] | undefined) ?? []
        const p = pValueFromVariants(variants)
        if (isNaN(p)) return <Typography variant="caption" color="text.secondary">—</Typography>
        return (
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Typography variant="caption">{p.toFixed(3)}</Typography>
            {p < 0.05 && <Typography variant="caption" title="统计显著">✅</Typography>}
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
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="详情">
              <IconButton size="small" onClick={() => navigate(`/admin/abtest/${row.id as number}`)}><BarChartIcon fontSize="small" /></IconButton>
            </Tooltip>
            {status === 0 && <Tooltip title="启动"><IconButton size="small" color="success" onClick={() => startMut.mutate(row.id as number)}><PlayArrowIcon fontSize="small" /></IconButton></Tooltip>}
            {status === 1 && <Tooltip title="暂停"><IconButton size="small" color="warning" onClick={() => pauseMut.mutate(row.id as number)}><PauseIcon fontSize="small" /></IconButton></Tooltip>}
            {(status === 1 || status === 2) && <Tooltip title="结束"><IconButton size="small" color="error" onClick={() => stopMut.mutate(row.id as number)}><StopIcon fontSize="small" /></IconButton></Tooltip>}
            {status === 0 && <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteId(row.id as number)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>}
          </Stack>
        )
      },
    },
  ]

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      {/* Page Title */}
      <Typography variant="h5" sx={{ mb: 2 }}>A/B 实验管理</Typography>
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
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page, pageSize: rows }}
        onPaginationModelChange={m => { setPage(m.page); setRows(m.pageSize) }}
        sx={{ flex: 1 }}
      />
      <CreateWizard open={wizardOpen} onClose={() => setWizardOpen(false)} onCreated={() => qc.invalidateQueries({ queryKey: ['ab-experiments'] })} />
      <ConfirmDialog open={deleteId !== null} content="确定要删除该实验吗？" onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}