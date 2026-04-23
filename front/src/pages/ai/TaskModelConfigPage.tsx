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
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import type { AiModelAdminVO, AiTaskModelConfigRow, AiTaskModelConfigSavePayload } from '@/types/ai'

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
      sx={{ minWidth: 100, '& .MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
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

  const { data: models = [], isLoading: modelsLoading } = useQuery({
    queryKey: ['models-config', 'task-model-dropdown'],
    queryFn: () => aiApi.adminModelsList(),
    retry: 2,
  })

  const saveMut = useMutation({
    mutationFn: (p: AiTaskModelConfigSavePayload) => aiApi.taskModelConfigSave(p),
    onSuccess: () => {
      toast('保存成功', 'success')
      void qc.invalidateQueries({ queryKey: ['task-model-config'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.taskModelConfigDelete(id),
    onSuccess: () => {
      toast('已删除', 'success')
      setDeleteId(null)
      void qc.invalidateQueries({ queryKey: ['task-model-config'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = () => {
    setForm(emptyForm())
    setOpen(true)
  }

  const openEdit = (row: AiTaskModelConfigRow) => {
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
      toast('请填写任务代码与任务名称', 'error')
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

  const columns: GridColDef<AiTaskModelConfigRow>[] = [
    { field: 'taskCode', headerName: '任务代码', width: 160 },
    { field: 'taskName', headerName: '任务名称', flex: 1, minWidth: 140 },
    { field: 'taskGroup', headerName: '任务分组', width: 100 },
    {
      field: 'primaryModelId',
      headerName: '主模型',
      width: 200,
      sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ py: 0.5, width: '100%', pr: 1 }}>
          <InlineModelSelect
            data-testid={`task-model-config-inline-primary-${row.id}`}
            value={row.primaryModelId}
            models={models}
            disabled={modelsLoading || models.length === 0}
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
      width: 180,
      sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ py: 0.5, width: '100%', pr: 1 }}>
          <InlineModelSelect
            data-testid={`task-model-config-inline-fb1-${row.id}`}
            value={row.fallbackModelId}
            models={models}
            disabled={modelsLoading || models.length === 0}
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
      width: 180,
      sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ py: 0.5, width: '100%', pr: 1 }}>
          <InlineModelSelect
            data-testid={`task-model-config-inline-fb2-${row.id}`}
            value={row.fallback2ModelId}
            models={models}
            disabled={modelsLoading || models.length === 0}
            savingThisRow={saveMut.isPending && saveMut.variables?.id === row.id}
            onCommit={(id) => {
              const next = { ...row, fallback2ModelId: id ?? null }
              saveMut.mutate(rowToSavePayload(next))
            }}
          />
        </Box>
      ),
    },
    { field: 'timeoutSeconds', headerName: '超时(s)', width: 90 },
    { field: 'maxRetries', headerName: '重试', width: 70 },
    { field: 'sortOrder', headerName: '排序', width: 70 },
    {
      field: 'status',
      headerName: '状态',
      width: 90,
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
      width: 160,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button
            size="small"
            startIcon={<EditIcon fontSize="small" />}
            onClick={() => openEdit(row)}
            data-testid={`task-model-config-edit-${row.id}`}
          >
            编辑
          </Button>
          <Button
            size="small"
            color="error"
            startIcon={<DeleteOutlineIcon fontSize="small" />}
            onClick={() => setDeleteId(row.id)}
            data-testid={`task-model-config-delete-${row.id}`}
          >
            删除
          </Button>
        </Stack>
      ),
    },
  ]

  return (
    <Box data-testid="task-model-config-page" sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <PageHeader
        title="任务模型映射配置"
        subtitle="表格内可直接选择主模型与备用模型并即时保存；也可使用「编辑」调整任务代码与其它参数"
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              size="small"
              onClick={() => navigate('/admin/ai/model-benchmark')}
            >
              前往模型评测
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={openAdd}
              data-testid="task-model-config-add"
            >
              新增配置
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

      <Box data-testid="task-model-config-grid">
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isLoading}
          paginationMode="client"
          getRowId={(r) => String(r.id)}
          sx={{ height: 'calc(100vh - 240px)' }}
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
              models={models}
              value={form.primaryModelId}
              onChange={(id) => setForm((p) => ({ ...p, primaryModelId: id }))}
              data-testid="task-model-config-field-primaryModelId"
            />
            <ModelSelect
              label="备用模型 1"
              models={models}
              value={form.fallbackModelId}
              onChange={(id) => setForm((p) => ({ ...p, fallbackModelId: id }))}
              data-testid="task-model-config-field-fallbackModelId"
            />
            <ModelSelect
              label="备用模型 2"
              models={models}
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
        content="确定删除该条配置？删除后为逻辑删除。"
        onClose={() => setDeleteId(null)}
        loading={deleteMut.isPending}
        onConfirm={() => {
          if (deleteId != null) deleteMut.mutate(deleteId)
        }}
      />
    </Box>
  )
}
