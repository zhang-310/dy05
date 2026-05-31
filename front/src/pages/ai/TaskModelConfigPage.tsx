import { useState } from 'react'
import {
  Box,
  Stack,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Chip,
  Alert,
  Typography,
  FormControl,
  Select,
  Paper,
  Divider,
  Tooltip,
  IconButton,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import HubIcon from '@mui/icons-material/Hub'
import RuleIcon from '@mui/icons-material/Rule'
import AssessmentIcon from '@mui/icons-material/Assessment'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import type { AiModelAdminVO, AiTaskModelConfigRow, AiTaskModelConfigSavePayload } from '@/types/ai'
import { getErrorMessage } from '@/utils/errorHandler'

const TASK_MODEL_CONFIG_READY_ENDPOINTS = [
  '/ai/admin/task-model-config/list',
  '/ai/admin/task-model-config/save',
  '/ai/admin/task-model-config/delete',
  '/ai/admin/models/list',
].join('|')

const TASK_MODEL_CONFIG_UNSUPPORTED_ENDPOINTS = [
  '/ai/admin/task-model-config/local-save',
  '/ai/admin/task-model-config/local-delete',
  '/ai/admin/task-model-config/export',
  '/ai/admin/task-model-config/auto-map',
  '/ai/admin/models/get-secret',
].join('|')

type TaskFormState = AiTaskModelConfigSavePayload & { id?: number }

function emptyForm(): TaskFormState {
  return {
    taskCode: '',
    taskName: '',
    taskGroup: 'evolve',
    primaryModelId: undefined,
    fallbackModelId: undefined,
    fallback2ModelId: undefined,
    timeoutSeconds: undefined,
    maxRetries: 1,
    sortOrder: 0,
    status: 1,
  }
}

function SummaryMetric(props: { title: string; value: string; helper: string; icon: React.ReactNode; color?: 'primary' | 'success' | 'warning' | 'error' }) {
  const { title, value, helper, icon, color = 'primary' } = props
  return (
    <Box sx={{ minWidth: 0, flex: '1 1 160px' }}>
      <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: 0 }}>
        <Box sx={{ color: `${color}.main`, display: 'flex', flexShrink: 0 }}>{icon}</Box>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" color="text.secondary" component="div">{title}</Typography>
          <Typography variant="subtitle1" fontWeight={700} noWrap>{value}</Typography>
        </Box>
      </Stack>
      <Typography variant="caption" color="text.secondary" component="div" noWrap sx={{ mt: 0.25 }}>
        {helper}
      </Typography>
    </Box>
  )
}

function ModelSelect(props: {
  label: string
  value: number | undefined | null
  models: AiModelAdminVO[]
  onChange: (id: number | undefined) => void
  'data-testid'?: string
}) {
  const { label, value, models, onChange, 'data-testid': testId } = props
  return (
    <TextField
      data-testid={testId}
      select
      label={label}
      fullWidth
      size="small"
      value={value != null ? String(value) : ''}
      onChange={(e) => {
        const v = e.target.value
        onChange(v === '' ? undefined : Number(v))
      }}
    >
      <MenuItem value="">（未设置）</MenuItem>
      {models.map((m) => (
        <MenuItem key={m.id} value={String(m.id)}>
          {m.modelName} (#{m.id})
        </MenuItem>
      ))}
    </TextField>
  )
}

function rowToSavePayload(row: AiTaskModelConfigRow): AiTaskModelConfigSavePayload {
  return {
    id: row.id,
    taskCode: row.taskCode,
    taskName: row.taskName,
    taskGroup: (row.taskGroup ?? '').trim() || 'evolve',
    primaryModelId: row.primaryModelId ?? undefined,
    fallbackModelId: row.fallbackModelId ?? undefined,
    fallback2ModelId: row.fallback2ModelId ?? undefined,
    timeoutSeconds:
      row.timeoutSeconds === undefined || row.timeoutSeconds === null || Number.isNaN(Number(row.timeoutSeconds))
        ? undefined
        : Number(row.timeoutSeconds),
    maxRetries: row.maxRetries ?? 1,
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
  }
}

function isModelEnabled(model: AiModelAdminVO | undefined): boolean {
  return model != null && Number(model.status ?? 0) === 1
}

function InlineModelSelect(props: {
  value: number | null | undefined
  models: AiModelAdminVO[]
  disabled?: boolean
  savingThisRow: boolean
  onCommit: (id: number | undefined) => void
  'data-testid'?: string
}) {
  const { value, models, disabled, savingThisRow, onCommit, 'data-testid': testId } = props
  return (
    <FormControl
      size="small"
      fullWidth
      sx={{
        minWidth: 100,
        '& .MuiOutlinedInput-root': { height: 32, fontSize: 13 },
        '& .MuiOutlinedInput-notchedOutline': { borderColor: 'divider' },
      }}
    >
      <Select
        data-testid={testId}
        displayEmpty
        value={value != null ? String(value) : ''}
        disabled={disabled || savingThisRow}
        renderValue={(selected) => {
          if (selected === '') return <Typography variant="body2">（未设置）</Typography>
          const id = Number(selected)
          const m = models.find((x) => x.id === id)
          return (
            <Typography variant="body2" noWrap title={m ? `${m.modelName} (#${m.id})` : `#${id}`}>
              {m ? m.modelName : `#${id}`}
            </Typography>
          )
        }}
        onChange={(e) => {
          const v = e.target.value as string
          onCommit(v === '' ? undefined : Number(v))
        }}
        onClick={(e) => e.stopPropagation()}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <MenuItem value="">（未设置）</MenuItem>
        {models.map((m) => (
          <MenuItem key={m.id} value={String(m.id)}>
            {m.modelName} (#{m.id})
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  )
}

export default function TaskModelConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<TaskFormState>(emptyForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [saveErrorText, setSaveErrorText] = useState<string | null>(null)
  const [deleteErrorText, setDeleteErrorText] = useState<string | null>(null)

  const {
    data: rows = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['task-model-config'],
    queryFn: () => aiApi.taskModelConfigList(),
    retry: 2,
  })

  const {
    data: models = [],
    isLoading: modelsLoading,
    isError: modelsIsError,
    error: modelsError,
    refetch: refetchModels,
  } = useQuery({
    queryKey: ['models-config', 'task-model-dropdown'],
    queryFn: () => aiApi.adminModelsList(),
    retry: false,
  })

  const saveMut = useMutation({
    mutationFn: (p: AiTaskModelConfigSavePayload) => aiApi.taskModelConfigSave(p),
    onSuccess: () => {
      toast('保存成功', 'success')
      setSaveErrorText(null)
      void qc.invalidateQueries({ queryKey: ['task-model-config'] })
    },
    onError: (e: Error) => {
      const message = `任务模型配置保存失败（/ai/admin/task-model-config/save）：${e.message}`
      setSaveErrorText(message)
      toast(e.message, 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.taskModelConfigDelete(id),
    onSuccess: () => {
      toast('已删除', 'success')
      setDeleteErrorText(null)
      setDeleteId(null)
      void qc.invalidateQueries({ queryKey: ['task-model-config'] })
    },
    onError: (e: Error, id: number) => {
      const message = `任务模型配置删除失败（/ai/admin/task-model-config/delete，id=${id}）：${e.message}`
      setDeleteErrorText(message)
      toast(e.message, 'error')
    },
  })

  const openAdd = () => {
    setSaveErrorText(null)
    setForm(emptyForm())
    setOpen(true)
  }

  const openEdit = (row: AiTaskModelConfigRow) => {
    setSaveErrorText(null)
    setForm({
      id: row.id,
      taskCode: row.taskCode,
      taskName: row.taskName,
      taskGroup: row.taskGroup ?? 'evolve',
      primaryModelId: row.primaryModelId ?? undefined,
      fallbackModelId: row.fallbackModelId ?? undefined,
      fallback2ModelId: row.fallback2ModelId ?? undefined,
      timeoutSeconds: row.timeoutSeconds ?? undefined,
      maxRetries: row.maxRetries ?? 1,
      sortOrder: row.sortOrder ?? 0,
      status: row.status ?? 1,
    })
    setOpen(true)
  }

  const buildSavePayload = (): AiTaskModelConfigSavePayload | null => {
    const taskCode = form.taskCode.trim()
    const taskName = form.taskName.trim()
    if (!taskCode || !taskName) {
      const message = '请填写任务代码与任务名称'
      setSaveErrorText(message)
      toast(message, 'error')
      return null
    }
    return {
      id: form.id,
      taskCode,
      taskName,
      taskGroup: (form.taskGroup ?? '').trim() || 'evolve',
      primaryModelId: form.primaryModelId ?? undefined,
      fallbackModelId: form.fallbackModelId ?? undefined,
      fallback2ModelId: form.fallback2ModelId ?? undefined,
      timeoutSeconds:
        form.timeoutSeconds === undefined || form.timeoutSeconds === null || Number.isNaN(Number(form.timeoutSeconds))
          ? undefined
          : Number(form.timeoutSeconds),
      maxRetries: form.maxRetries ?? 1,
      sortOrder: form.sortOrder ?? 0,
      status: form.status ?? 1,
    }
  }

  const normalizedRows = Array.isArray(rows) ? rows : []
  const normalizedModels = Array.isArray(models) ? models : []
  const enabledModels = normalizedModels.filter(isModelEnabled)
  const modelById = new Map(normalizedModels.map((model) => [Number(model.id), model]))
  const modelNameById = new Map(normalizedModels.map((model) => [Number(model.id), model.modelName]))
  const hasResolvedModelName = (id: number | null | undefined, name: string | null | undefined) => {
    if (id == null) return true
    return Boolean((name ?? '').trim() || modelNameById.has(Number(id)))
  }
  const hasDisabledModelRef = (id: number | null | undefined) => {
    if (id == null) return false
    const model = modelById.get(Number(id))
    return model != null && !isModelEnabled(model)
  }

  const totalRows = normalizedRows.length
  const enabledRows = normalizedRows.filter(row => row.status === 1).length
  const primaryMappedRows = normalizedRows.filter(row => row.primaryModelId != null).length
  const fallbackMappedRows = normalizedRows.filter(row => row.fallbackModelId != null || row.fallback2ModelId != null).length
  const coveragePct = totalRows > 0 ? Math.round((primaryMappedRows / totalRows) * 100) : 0
  const primaryMissingRows = normalizedRows.filter(row => row.status === 1 && row.primaryModelId == null)
  const fallbackMissingRows = normalizedRows.filter(row => row.status === 1 && row.fallbackModelId == null && row.fallback2ModelId == null)
  const unresolvedModelRows = normalizedRows.filter(row =>
    !hasResolvedModelName(row.primaryModelId, row.primaryModelName)
    || !hasResolvedModelName(row.fallbackModelId, row.fallbackModelName)
    || !hasResolvedModelName(row.fallback2ModelId, row.fallback2ModelName)
  )
  const disabledModelRefRows = normalizedRows.filter(row =>
    hasDisabledModelRef(row.primaryModelId)
    || hasDisabledModelRef(row.fallbackModelId)
    || hasDisabledModelRef(row.fallback2ModelId)
  )
  const saveError = getErrorMessage(saveMut.error)
  const deleteError = getErrorMessage(deleteMut.error)

  const columns: GridColDef<AiTaskModelConfigRow>[] = [
    {
      field: 'taskCode',
      headerName: '任务',
      flex: 1,
      minWidth: 240,
      renderCell: ({ row }) => (
        <Stack spacing={0.25} sx={{ minWidth: 0, py: 0.5 }}>
          <Typography variant="body2" fontWeight={700} noWrap>{row.taskName}</Typography>
          <Stack direction="row" spacing={0.5} alignItems="center" sx={{ minWidth: 0 }}>
            <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }} noWrap>
              {row.taskCode}
            </Typography>
            <Chip label={row.taskGroup ?? 'evolve'} size="small" variant="outlined" sx={{ height: 18, fontSize: 10 }} />
          </Stack>
        </Stack>
      ),
    },
    {
      field: 'primaryModelId',
      headerName: '主模型',
      width: 170,
      sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ width: '100%', pr: 0.75 }}>
          <InlineModelSelect
            data-testid={`task-model-config-inline-primary-${row.id}`}
            value={row.primaryModelId}
            models={enabledModels}
            disabled={modelsLoading || enabledModels.length === 0}
            savingThisRow={saveMut.isPending && saveMut.variables?.id === row.id}
            onCommit={(id) => {
              const next = { ...row, primaryModelId: id ?? null }
              saveMut.mutate(rowToSavePayload(next))
            }}
          />
        </Box>
      ),
    },
    {
      field: 'fallbackModelId',
      headerName: '备用 1',
      width: 160,
      sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ width: '100%', pr: 0.75 }}>
          <InlineModelSelect
            data-testid={`task-model-config-inline-fb1-${row.id}`}
            value={row.fallbackModelId}
            models={enabledModels}
            disabled={modelsLoading || enabledModels.length === 0}
            savingThisRow={saveMut.isPending && saveMut.variables?.id === row.id}
            onCommit={(id) => {
              const next = { ...row, fallbackModelId: id ?? null }
              saveMut.mutate(rowToSavePayload(next))
            }}
          />
        </Box>
      ),
    },
    {
      field: 'fallback2ModelId',
      headerName: '备用 2',
      width: 160,
      sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ width: '100%', pr: 0.75 }}>
          <InlineModelSelect
            data-testid={`task-model-config-inline-fb2-${row.id}`}
            value={row.fallback2ModelId}
            models={enabledModels}
            disabled={modelsLoading || enabledModels.length === 0}
            savingThisRow={saveMut.isPending && saveMut.variables?.id === row.id}
            onCommit={(id) => {
              const next = { ...row, fallback2ModelId: id ?? null }
              saveMut.mutate(rowToSavePayload(next))
            }}
          />
        </Box>
      ),
    },
    {
      field: 'runtime',
      headerName: '运行参数',
      width: 120,
      renderCell: ({ row }) => (
        <Stack spacing={0.25} sx={{ py: 0.5 }}>
          <Typography variant="caption" noWrap>超时 {row.timeoutSeconds ?? '—'}s</Typography>
          <Typography variant="caption" color="text.secondary" noWrap>重试 {row.maxRetries ?? 1} · 排序 {row.sortOrder ?? 0}</Typography>
        </Stack>
      ),
    },
    {
      field: 'status',
      headerName: '状态',
      width: 78,
      renderCell: ({ value }) => (
        <Chip
          label={value === 1 ? '启用' : '停用'}
          size="small"
          color={value === 1 ? 'success' : 'default'}
        />
      ),
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 92,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.25}>
          <Tooltip title="编辑">
            <IconButton
              size="small"
              onClick={() => openEdit(row)}
              data-testid={`task-model-config-edit-${row.id}`}
              aria-label={`编辑：${row.taskName}`}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="删除">
            <IconButton
              size="small"
              color="error"
              onClick={() => { setDeleteErrorText(null); setDeleteId(row.id) }}
              data-testid={`task-model-config-delete-${row.id}`}
              aria-label={`删除：${row.taskName}`}
            >
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  const tableActions = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Button
        variant="outlined"
        size="small"
        startIcon={<AssessmentIcon fontSize="small" />}
        data-testid="task-model-config-go-benchmark"
        onClick={() => navigate('/admin/ai/model-benchmark')}
      >
        模型评测
      </Button>
      <Button
        variant="contained"
        size="small"
        startIcon={<AddIcon />}
        onClick={openAdd}
        data-testid="task-model-config-add"
      >
        新增配置
      </Button>
    </Stack>
  )

  return (
    <Box
      data-testid="task-model-config-page"
      data-ready-endpoints={TASK_MODEL_CONFIG_READY_ENDPOINTS}
      data-unsupported-endpoints={TASK_MODEL_CONFIG_UNSUPPORTED_ENDPOINTS}
      data-no-local-mapping-fallback="true"
      data-no-optimistic-inline-mutation="true"
      data-no-local-delete-mutation="true"
      data-no-plaintext-key-display="true"
      sx={{ height: 'calc(100vh - 48px - 16px)', p: 1.25, display: 'flex', flexDirection: 'column', gap: 1, overflow: 'hidden' }}
    >
      <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1 }}>
        <Stack direction={{ xs: 'column', lg: 'row' }} alignItems={{ xs: 'stretch', lg: 'center' }} spacing={1.25}>
          <Box sx={{ minWidth: 230, flex: '0 0 auto' }}>
            <Typography component="h1" variant="h5" sx={{ fontWeight: 700, lineHeight: 1.15 }}>
              任务模型映射配置
            </Typography>
            <Typography variant="caption" color="text.secondary" component="div" noWrap sx={{ mt: 0.25 }}>
              为不同 AI 任务指定主模型、备用模型和重试策略。
            </Typography>
          </Box>
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="任务配置" value={String(totalRows)} helper={`启用 ${enabledRows} 条，停用 ${totalRows - enabledRows} 条`} icon={<RuleIcon fontSize="small" />} />
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="主模型覆盖率" value={`${coveragePct}%`} helper={`${primaryMappedRows}/${totalRows || 0} 条任务已配置主模型`} icon={<HubIcon fontSize="small" />} color={coveragePct >= 90 ? 'success' : coveragePct >= 60 ? 'warning' : 'error'} />
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="备用模型覆盖" value={String(fallbackMappedRows)} helper="配置了 fallback1 或 fallback2 的任务数" icon={<HubIcon fontSize="small" />} color={fallbackMappedRows > 0 ? 'success' : 'warning'} />
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="可选模型" value={String(enabledModels.length)} helper={normalizedModels.length > 0 ? `启用 ${enabledModels.length} 个，禁用 ${normalizedModels.length - enabledModels.length} 个` : '暂无模型可用于映射'} icon={<HubIcon fontSize="small" />} color={enabledModels.length > 0 ? 'success' : 'warning'} />
        </Stack>
      </Paper>

      {isError ? (
        <Alert
          severity="error"
          data-testid="task-model-config-list-error"
          data-source-endpoint="/ai/admin/task-model-config/list"
          data-no-local-mapping-fallback="true"
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>
              重试
            </Button>
          }
        >
          任务模型映射加载失败：{getErrorMessage(error)}。请检查 <code>/ai/admin/task-model-config/list</code> 和管理员权限。
        </Alert>
      ) : null}

      {modelsIsError ? (
        <Alert
          severity="error"
          data-testid="task-model-config-model-list-error"
          data-source-endpoint="/ai/admin/models/list"
          data-no-local-model-option-fallback="true"
          data-no-plaintext-key-display="true"
          action={
            <Button color="inherit" size="small" onClick={() => void refetchModels()}>
              重试
            </Button>
          }
        >
          模型列表加载失败：{getErrorMessage(modelsError)}。请检查 <code>/ai/admin/models/list</code>；映射列表仍可查看，但下拉无法安全选择模型。
        </Alert>
      ) : null}

      {saveMut.isError ? (
        <Alert
          severity="error"
          data-testid="task-model-config-save-error"
          data-source-endpoint="/ai/admin/task-model-config/save"
          data-no-optimistic-inline-mutation="true"
          data-input-retained="true"
        >
          {saveErrorText ?? `任务模型配置保存失败：${saveError}。请检查 /ai/admin/task-model-config/save，后端会校验 taskCode 唯一性以及模型是否存在并启用。`}
        </Alert>
      ) : null}

      {deleteMut.isError ? (
        <Alert
          severity="error"
          data-testid="task-model-config-delete-error"
          data-source-endpoint="/ai/admin/task-model-config/delete"
          data-no-local-delete-mutation="true"
          data-row-retained-on-action-error="true"
        >
          {deleteErrorText ?? `任务模型配置删除失败：${deleteError}。请检查 /ai/admin/task-model-config/delete，失败时页面不会移除本地行。`}
        </Alert>
      ) : null}

      {totalRows > 0 ? (
        <Paper
          variant="outlined"
          data-testid="task-model-config-diagnostics-contract"
          data-source-endpoints="/ai/admin/task-model-config/list|/ai/admin/models/list"
          data-no-local-mapping-fallback="true"
          data-no-plaintext-key-display="true"
          sx={{ borderRadius: 1 }}
        >
          <Box sx={{ p: 1 }}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} alignItems={{ xs: 'stretch', md: 'center' }} justifyContent="space-between">
              <Typography variant="subtitle2" fontWeight={700}>
                映射覆盖诊断
              </Typography>
              <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
              <Chip size="small" color={primaryMissingRows.length === 0 ? 'success' : 'warning'} label={`主模型缺口 ${primaryMissingRows.length}`} />
              <Chip size="small" color={fallbackMissingRows.length === 0 ? 'success' : 'warning'} label={`备用缺口 ${fallbackMissingRows.length}`} />
              <Chip size="small" color={unresolvedModelRows.length === 0 ? 'success' : 'error'} label={`模型名未解析 ${unresolvedModelRows.length}`} />
              <Chip size="small" color={disabledModelRefRows.length === 0 ? 'success' : 'error'} label={`引用禁用模型 ${disabledModelRefRows.length}`} />
              </Stack>
            </Stack>
            {primaryMissingRows.length > 0 ? (
              <Alert severity="warning" sx={{ mt: 1, py: 0.5 }}>
                未配置主模型：{primaryMissingRows.map(row => `${row.taskName} (${row.taskCode})`).join('、')}。这些任务会依赖后端默认模型或调用侧降级。
              </Alert>
            ) : null}
            {fallbackMissingRows.length > 0 ? (
              <Alert severity="info" sx={{ mt: 1, py: 0.5 }}>
                未配置备用模型：{fallbackMissingRows.map(row => `${row.taskName} (${row.taskCode})`).join('、')}。主模型不可用时没有显式 fallback。
              </Alert>
            ) : null}
            {unresolvedModelRows.length > 0 ? (
              <Alert severity="error" sx={{ mt: 1, py: 0.5 }}>
                有配置引用的模型名未解析：{unresolvedModelRows.map(row => `${row.taskName} (${row.taskCode})`).join('、')}。请检查模型是否已删除或未启用。
              </Alert>
            ) : null}
            {disabledModelRefRows.length > 0 ? (
              <Alert severity="error" sx={{ mt: 1, py: 0.5 }}>
                有配置引用了已禁用模型：{disabledModelRefRows.map(row => `${row.taskName} (${row.taskCode})`).join('、')}。后端保存会拒绝禁用模型，请改选启用模型。
              </Alert>
            ) : null}
            {primaryMissingRows.length === 0 && fallbackMissingRows.length === 0 && unresolvedModelRows.length === 0 && disabledModelRefRows.length === 0 ? (
              <Alert severity="success" sx={{ mt: 1, py: 0.5 }}>启用任务已配置主模型与备用模型，列表行的模型名称均可解析。</Alert>
            ) : null}
          </Box>
        </Paper>
      ) : null}

      {!modelsLoading && enabledModels.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="task-model-config-no-enabled-models"
          data-source-endpoint="/ai/admin/models/list"
          data-no-local-model-option-fallback="true"
        >
          暂无启用模型可选。请先在「模型配置」新增并启用模型，否则任务映射只能保存为空模型。
        </Alert>
      ) : null}

      {!isLoading && totalRows === 0 ? (
        <Alert
          severity="info"
          data-testid="task-model-config-empty"
          data-source-endpoint="/ai/admin/task-model-config/list"
          data-no-local-mapping-fallback="true"
        >
          暂无任务模型映射配置。新增配置后，可为不同 AI 任务指定主模型与备用模型。
        </Alert>
      ) : null}

      <Box data-testid="task-model-config-grid" sx={{ flex: 1, minHeight: 320, overflow: 'hidden' }}>
        <StandardDataGrid
          rows={normalizedRows}
          columns={columns}
          loading={isLoading}
          paginationMode="client"
          getRowId={(r) => String(r.id)}
          actionSlot={tableActions}
          rowHeight={52}
          columnHeaderHeight={40}
          sx={{
            borderRadius: 1,
            '& .MuiDataGrid-toolbarContainer': { minHeight: 38, px: 1, py: 0.5 },
            '& .MuiDataGrid-columnHeaders': { minHeight: '40px !important', maxHeight: '40px !important' },
            '& .MuiDataGrid-cell': { py: 0, alignItems: 'center' },
            '& .MuiDataGrid-cell:focus, & .MuiDataGrid-columnHeader:focus': { outline: 'none' },
          }}
        />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? '编辑任务模型配置' : '新增任务模型配置'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {modelsLoading ? (
              <Alert severity="info">正在加载模型列表…</Alert>
            ) : null}
            <TextField
              label="任务代码"
              value={form.taskCode}
              fullWidth
              size="small"
              required
              onChange={(e) => setForm((p) => ({ ...p, taskCode: e.target.value }))}
              data-testid="task-model-config-field-taskCode"
            />
            <TextField
              label="任务名称"
              value={form.taskName}
              fullWidth
              size="small"
              required
              onChange={(e) => setForm((p) => ({ ...p, taskName: e.target.value }))}
              data-testid="task-model-config-field-taskName"
            />
            <TextField
              label="任务分组"
              value={form.taskGroup ?? ''}
              fullWidth
              size="small"
              helperText="默认 evolve，与后端一致"
              onChange={(e) => setForm((p) => ({ ...p, taskGroup: e.target.value }))}
              data-testid="task-model-config-field-taskGroup"
            />
            <ModelSelect
              label="主模型"
              models={enabledModels}
              value={form.primaryModelId}
              onChange={(id) => setForm((p) => ({ ...p, primaryModelId: id }))}
              data-testid="task-model-config-field-primaryModelId"
            />
            <ModelSelect
              label="备用模型 1"
              models={enabledModels}
              value={form.fallbackModelId}
              onChange={(id) => setForm((p) => ({ ...p, fallbackModelId: id }))}
              data-testid="task-model-config-field-fallbackModelId"
            />
            <ModelSelect
              label="备用模型 2"
              models={enabledModels}
              value={form.fallback2ModelId}
              onChange={(id) => setForm((p) => ({ ...p, fallback2ModelId: id }))}
              data-testid="task-model-config-field-fallback2ModelId"
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="超时（秒）"
                type="number"
                value={form.timeoutSeconds ?? ''}
                fullWidth
                size="small"
                inputProps={{ min: 0 }}
                onChange={(e) => {
                  const raw = e.target.value
                  setForm((p) => ({
                    ...p,
                    timeoutSeconds: raw === '' ? undefined : Number(raw),
                  }))
                }}
                data-testid="task-model-config-field-timeoutSeconds"
              />
              <TextField
                label="最大重试"
                type="number"
                value={form.maxRetries ?? 1}
                fullWidth
                size="small"
                inputProps={{ min: 0 }}
                onChange={(e) => setForm((p) => ({ ...p, maxRetries: Number(e.target.value) }))}
                data-testid="task-model-config-field-maxRetries"
              />
            </Stack>
            <Stack direction="row" spacing={2}>
              <TextField
                label="排序"
                type="number"
                value={form.sortOrder ?? 0}
                fullWidth
                size="small"
                onChange={(e) => setForm((p) => ({ ...p, sortOrder: Number(e.target.value) }))}
                data-testid="task-model-config-field-sortOrder"
              />
              <TextField
                select
                label="状态"
                value={form.status ?? 1}
                fullWidth
                size="small"
                onChange={(e) => setForm((p) => ({ ...p, status: Number(e.target.value) }))}
                data-testid="task-model-config-field-status"
              >
                <MenuItem value={1}>启用</MenuItem>
                <MenuItem value={0}>停用</MenuItem>
              </TextField>
            </Stack>
            {saveErrorText != null ? (
              <Alert
                severity="error"
                data-testid="task-model-config-form-error"
                data-input-retained="true"
                data-no-optimistic-inline-mutation="true"
              >
                {saveErrorText}。弹窗会保留任务代码、任务名称、模型映射和重试参数，便于修正后重试。
              </Alert>
            ) : null}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button data-testid="task-model-config-dialog-cancel" onClick={() => setOpen(false)}>
            取消
          </Button>
          <Button
            variant="contained"
            disabled={saveMut.isPending}
            data-testid="task-model-config-dialog-save"
            onClick={() => {
              const payload = buildSavePayload()
              if (payload) {
                saveMut.mutate(payload, {
                  onSuccess: () => setOpen(false),
                })
              }
            }}
          >
            保存
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleteId != null}
        title="删除任务模型配置"
        content={deleteErrorText ?? '确定删除该条配置？删除后为逻辑删除。'}
        onClose={() => setDeleteId(null)}
        loading={deleteMut.isPending}
        onConfirm={() => {
          if (deleteId != null) deleteMut.mutate(deleteId)
        }}
      />
    </Box>
  )
}
