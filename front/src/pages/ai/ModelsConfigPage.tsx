import { useState } from 'react'
import {
  Box,
  Button,
  TextField,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Alert,
  Stack,
  Chip,
  Tooltip,
  IconButton,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import StarIcon from '@mui/icons-material/Star'
import StarBorderIcon from '@mui/icons-material/StarBorder'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type { AiModelAdminVO, AiModelSavePayload } from '@/types/ai'

const PROVIDERS = ['openai', 'anthropic', 'volcengine', 'deepseek', 'ollama', 'custom']

const PROVIDER_LABELS: Record<string, string> = {
  openai: 'OpenAI',
  anthropic: 'Anthropic',
  volcengine: '火山引擎',
  deepseek: 'DeepSeek',
  ollama: 'Ollama',
  custom: '自定义',
}

type FormState = Partial<AiModelSavePayload> & { id?: number }

function formatNumber(num: number | null | undefined): string {
  if (num == null) return '—'
  return num.toString()
}

export default function ModelsConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<FormState>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [testingId, setTestingId] = useState<number | null>(null)

  const { data: models = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['models-config'],
    queryFn: () => aiApi.adminModelsList(),
    retry: 2,
  })

  const saveMut = useMutation({
    mutationFn: (params: AiModelSavePayload) => aiApi.adminModelsSave(params),
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormOpen(false)
      void qc.invalidateQueries({ queryKey: ['models-config'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.adminModelsDelete(id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setDeleteId(null)
      void qc.invalidateQueries({ queryKey: ['models-config'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const setDefaultMut = useMutation({
    mutationFn: (id: number) => aiApi.adminModelsSetDefault(id),
    onSuccess: () => {
      toast('已设为默认', 'success')
      void qc.invalidateQueries({ queryKey: ['models-config'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const testMut = useMutation({
    mutationFn: (id: number) => aiApi.adminModelsTestConnection(id),
    onSuccess: (data) => {
      if (data.success) {
        toast(`连通成功 · tokens=${data.tokensUsed ?? 0}`, 'success')
      } else {
        toast(data.errorMsg != null && data.errorMsg !== '' ? data.errorMsg : '连通失败', 'error')
      }
      setTestingId(null)
    },
    onError: (e: Error) => {
      toast(e.message, 'error')
      setTestingId(null)
    },
  })

  const openAdd = () => {
    setForm({
      temperature: 0.7,
      maxTokens: 2048,
      status: 1,
      provider: '',
      modelName: '',
      endpoint: '',
      apiBaseUrl: '',
    })
    setFormOpen(true)
  }

  const openEdit = (m: AiModelAdminVO) => {
    setForm({
      id: m.id,
      modelName: m.modelName,
      provider: m.modelProvider,
      endpoint: m.modelVersion,
      apiBaseUrl: m.apiBaseUrl ?? '',
      apiKey: '',
      maxTokens: m.maxTokens ?? 2048,
      temperature: typeof m.temperature === 'number' ? m.temperature : Number(m.temperature ?? 0.7),
      status: m.status ?? 1,
      isDefault: m.isDefault ?? 0,
    })
    setFormOpen(true)
  }

  const handleSave = () => {
    const modelName = form.modelName?.trim()
    const provider = form.provider?.trim()
    const endpoint = form.endpoint?.trim()
    if (!modelName || !provider || endpoint == null || endpoint === '') {
      toast('请填写模型名称、提供商与模型 ID', 'error')
      return
    }
    const maxTokens = form.maxTokens ?? 2048
    const temperature = form.temperature ?? 0.7
    const payload: AiModelSavePayload = {
      id: form.id,
      modelName,
      provider,
      endpoint,
      maxTokens,
      temperature,
      status: form.status ?? 1,
      isDefault: form.isDefault ?? 0,
    }
    if (form.apiBaseUrl != null && form.apiBaseUrl.trim() !== '') {
      payload.apiBaseUrl = form.apiBaseUrl.trim()
    } else {
      payload.apiBaseUrl = form.id != null ? '' : undefined
    }
    if (form.apiKey != null && form.apiKey.trim() !== '') {
      payload.apiKey = form.apiKey.trim()
    }
    saveMut.mutate(payload)
  }

  const modelList = Array.isArray(models) ? models : []

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    {
      field: 'isDefault',
      headerName: '默认',
      width: 80,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        return (
          <Tooltip title={m.isDefault === 1 ? '默认模型' : '设为默认'}>
            <IconButton
              size="small"
              color={m.isDefault === 1 ? 'primary' : 'default'}
              onClick={() => m.isDefault !== 1 && setDefaultMut.mutate(m.id)}
              disabled={m.isDefault === 1}
            >
              {m.isDefault === 1 ? <StarIcon fontSize="small" /> : <StarBorderIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
        )
      },
    },
    {
      field: 'modelName',
      headerName: '模型名称',
      flex: 1,
      minWidth: 150,
    },
    {
      field: 'modelProvider',
      headerName: '提供商',
      width: 120,
      renderCell: ({ value }) => (
        <Chip label={PROVIDER_LABELS[value as string] || value} size="small" variant="outlined" />
      ),
    },
    {
      field: 'modelVersion',
      headerName: '模型 ID',
      width: 180,
      renderCell: ({ value }) => (
        <Box sx={{ fontFamily: 'monospace', fontSize: 12 }}>{value}</Box>
      ),
    },
    {
      field: 'resolvedBaseUrl',
      headerName: 'API Base URL',
      flex: 2,
      minWidth: 250,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        const hasCustom = m.apiBaseUrl != null && m.apiBaseUrl !== ''
        return (
          <Tooltip title={m.resolvedBaseUrl || '未配置'} placement="top-start">
            <Box sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              <Box component="span" sx={{ fontSize: 12, fontFamily: 'monospace' }}>
                {m.resolvedBaseUrl || '—'}
              </Box>
              {hasCustom && (
                <Chip label="自定义" size="small" color="info" sx={{ ml: 0.5, height: 18, fontSize: 10 }} />
              )}
            </Box>
          </Tooltip>
        )
      },
    },
    {
      field: 'apiKeyMasked',
      headerName: 'API Key',
      width: 140,
      renderCell: ({ value }) => (
        <Box sx={{ fontFamily: 'monospace', fontSize: 11, color: 'text.secondary' }}>
          {value || '—'}
        </Box>
      ),
    },
    {
      field: 'temperature',
      headerName: 'Temp',
      width: 80,
      renderCell: ({ value }) => (
        <Box sx={{ fontSize: 12 }}>{String(value ?? '—')}</Box>
      ),
    },
    {
      field: 'maxTokens',
      headerName: 'Max Tokens',
      width: 110,
      renderCell: ({ value }) => (
        <Box sx={{ fontSize: 12 }}>{formatNumber(value as number)}</Box>
      ),
    },
    {
      field: 'quotaUsed',
      headerName: '配额使用',
      width: 120,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        const used = m.quotaUsed ?? 0
        const limit = m.quotaLimit
        const hasLimit = limit != null && limit > 0
        const percentage = hasLimit ? Math.round((used / limit) * 100) : 0
        const color = percentage > 90 ? 'error' : percentage > 70 ? 'warning' : 'default'
        return (
          <Box sx={{ fontSize: 12 }}>
            {used} / {hasLimit ? limit : '∞'}
            {hasLimit && percentage > 0 && (
              <Chip label={`${percentage}%`} size="small" color={color} sx={{ ml: 0.5, height: 18, fontSize: 10 }} />
            )}
          </Box>
        )
      },
    },
    {
      field: 'status',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => (
        <Chip label={value === 1 ? '启用' : '禁用'} size="small" color={value === 1 ? 'success' : 'default'} />
      ),
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 200,
      sortable: false,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        const isTesting = testingId === m.id && testMut.isPending
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="测试连接">
              <IconButton
                size="small"
                color="primary"
                disabled={isTesting}
                onClick={() => {
                  setTestingId(m.id)
                  testMut.mutate(m.id)
                }}
              >
                <PlayArrowIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="编辑">
              <IconButton size="small" onClick={() => openEdit(m)}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="删除">
              <IconButton size="small" color="error" onClick={() => setDeleteId(m.id)}>
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        )
      },
    },
  ]

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', p: 2, gap: 2 }}>
      <PageHeader
        title="模型配置"
        subtitle="管理 AI 模型配置，支持自定义 API Base URL 和密钥。模型 ID 将作为请求体中的 model 参数。"
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>
            新增模型
          </Button>
        }
      />

      <Alert severity="info">
        编辑时 API Key 留空表示不修改已保存的密钥。列表中的密钥为掩码展示（如 sk-***abc）。
      </Alert>

      {isError && (
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>
              重试
            </Button>
          }
        >
          加载失败：{error instanceof Error ? error.message : String(error)}
        </Alert>
      )}

      <StandardDataGrid
        rows={modelList}
        columns={columns}
        loading={isLoading}
        pageSizeOptions={[10, 20, 50]}
        initialState={{
          pagination: { paginationModel: { pageSize: 20 } },
        }}
      />

      {/* 编辑对话框 */}
      <FormDialog
        open={formOpen}
        title={form.id ? '编辑模型' : '新增模型'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave}
        loading={saveMut.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField
            label="模型名称（展示用）"
            value={form.modelName ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, modelName: e.target.value }))}
            fullWidth
            placeholder="例如：GPT-4 Turbo"
          />
          <Select
            value={form.provider ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, provider: e.target.value }))}
            displayEmpty
            fullWidth
            size="small"
          >
            <MenuItem value="">选择提供商</MenuItem>
            {PROVIDERS.map((p) => (
              <MenuItem key={p} value={p}>
                {PROVIDER_LABELS[p] || p}
              </MenuItem>
            ))}
          </Select>
          <TextField
            label="模型 ID（厂商 API 中的 model 名称）"
            value={form.endpoint ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, endpoint: e.target.value }))}
            fullWidth
            placeholder="例如：gpt-4-turbo-preview"
            helperText="将写入 model_version，并作为请求体中的 model 参数。"
          />
          <TextField
            label="自定义 API Base URL（可选）"
            value={form.apiBaseUrl ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, apiBaseUrl: e.target.value }))}
            fullWidth
            placeholder="例如：https://api.openai.com 或 https://your-gateway/v1"
            helperText="OpenAI 兼容网关根地址，勿含 /chat/completions。留空则使用系统默认配置。"
          />
          <TextField
            label="API Key（可选；编辑时留空不修改）"
            value={form.apiKey ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, apiKey: e.target.value }))}
            fullWidth
            type="password"
            autoComplete="off"
            placeholder={form.id ? '留空表示不修改' : '输入 API Key'}
          />
          <Stack direction="row" spacing={2}>
            <TextField
              label="Temperature"
              type="number"
              value={form.temperature ?? 0.7}
              onChange={(e) => setForm((f) => ({ ...f, temperature: Number(e.target.value) }))}
              sx={{ flex: 1 }}
              inputProps={{ step: 0.1, min: 0, max: 2 }}
            />
            <TextField
              label="Max Tokens"
              type="number"
              value={form.maxTokens ?? 2048}
              onChange={(e) => setForm((f) => ({ ...f, maxTokens: Number(e.target.value) }))}
              sx={{ flex: 1 }}
            />
          </Stack>
          <FormControlLabel
            control={
              <Switch
                checked={form.status === 1}
                onChange={(e) => setForm((f) => ({ ...f, status: e.target.checked ? 1 : 0 }))}
              />
            }
            label="启用"
          />
          <FormControlLabel
            control={
              <Switch
                checked={form.isDefault === 1}
                onChange={(e) => setForm((f) => ({ ...f, isDefault: e.target.checked ? 1 : 0 }))}
              />
            }
            label="设为默认"
          />
        </Stack>
      </FormDialog>

      {/* 删除确认对话框 */}
      <ConfirmDialog
        open={deleteId !== null}
        content="确定删除该模型配置？此操作不可恢复。"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId != null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
