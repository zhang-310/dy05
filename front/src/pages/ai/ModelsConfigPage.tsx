import { useState } from 'react'
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Switch,
  FormControlLabel,
  Alert,
  AlertTitle,
  Stack,
  Chip,
  Tooltip,
  IconButton,
  LinearProgress,
  Typography,
  Collapse,
  Paper,
  Divider,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import ApiIcon from '@mui/icons-material/Api'
import RefreshIcon from '@mui/icons-material/Refresh'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import StarIcon from '@mui/icons-material/Star'
import StarBorderIcon from '@mui/icons-material/StarBorder'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type { AiModelAdminVO, AiModelConnectionTestResult, AiModelSavePayload } from '@/types/ai'
import { getErrorMessage } from '@/utils/errorHandler'

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

const PROVIDER_DEFAULT_BASE_URLS: Record<string, string> = {
  ollama: '读取 ai.ollama.url，Docker 访问宿主机通常为 http://host.docker.internal:11434',
  openai: '读取 ai.openai.api_url，默认 https://api.openai.com',
  anthropic: '读取 ai.anthropic.api_url',
  deepseek: '读取 ai.deepseek.api_url，默认 DeepSeek OpenAI 兼容地址',
  volcengine: '读取后端 provider 对应系统配置',
  custom: '需要显式填写 OpenAI 兼容 API Base URL',
}

const MODELS_CONFIG_READY_ENDPOINTS = [
  '/ai/admin/models/list',
  '/ai/admin/models/save',
  '/ai/admin/models/delete',
  '/ai/admin/models/set-default',
  '/ai/admin/models/test-connection',
].join('|')

const MODELS_CONFIG_UNSUPPORTED_ENDPOINTS = [
  '/ai/admin/models/get-secret',
  '/ai/admin/models/export',
  '/ai/admin/models/local-test-connection',
  '/ai/admin/models/local-set-default',
  '/ai/admin/models/local-delete',
  '/ai/admin/models/plaintext-key',
].join('|')

function formatNumber(num: number | null | undefined): string {
  if (num == null) return '—'
  return num.toString()
}

function isEnabled(model: AiModelAdminVO): boolean {
  return Number(model.status ?? 0) === 1
}

function isDefaultModel(model: AiModelAdminVO): boolean {
  return Number(model.isDefault ?? 0) === 1
}

function hasExplicitQuotaLimit(model: AiModelAdminVO): boolean {
  return model.quotaLimit != null && Number.isFinite(Number(model.quotaLimit)) && Number(model.quotaLimit) > 0
}

function hasConfiguredBaseUrl(model: AiModelAdminVO): boolean {
  return Boolean((model.resolvedBaseUrl ?? model.apiBaseUrl ?? '').trim())
}

function hasDirectApiKey(model: AiModelAdminVO): boolean {
  return Boolean((model.apiKeyMasked ?? '').trim())
}

function providerNeedsDirectOrEnvKey(model: AiModelAdminVO): boolean {
  const provider = (model.modelProvider ?? '').toLowerCase()
  return provider !== '' && provider !== 'ollama'
}

function getProviderLabel(provider: string | undefined): string {
  return PROVIDER_LABELS[provider ?? ''] || provider || '未上报'
}

function getQuotaPercent(model: AiModelAdminVO): number | undefined {
  if (!hasExplicitQuotaLimit(model)) return undefined
  const limit = Number(model.quotaLimit)
  const used = Number(model.quotaUsed ?? 0)
  return Math.round((used / limit) * 100)
}

function SummaryMetric(props: { title: string; value: string; helper: string; icon: React.ReactNode; color?: 'primary' | 'success' | 'warning' | 'error' }) {
  const { title, value, helper, icon, color = 'primary' } = props
  return (
    <Box sx={{ minWidth: 0, flex: '1 1 180px' }}>
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

export default function ModelsConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<FormState>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [testingId, setTestingId] = useState<number | null>(null)
  const [testResults, setTestResults] = useState<Record<number, AiModelConnectionTestResult>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [operationError, setOperationError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [diagnosticsOpen, setDiagnosticsOpen] = useState(false)

  const { data: models = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['models-config'],
    queryFn: () => aiApi.adminModelsList(),
    retry: 2,
  })

  const saveMut = useMutation({
    mutationFn: (params: AiModelSavePayload) => aiApi.adminModelsSave(params),
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormError(null)
      setOperationError(null)
      setFormOpen(false)
      void qc.invalidateQueries({ queryKey: ['models-config'] })
    },
    onError: (e: Error) => {
      const message = `模型保存失败（/ai/admin/models/save）：${e.message}`
      setFormError(message)
      setOperationError(message)
      toast(e.message, 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.adminModelsDelete(id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setDeleteError(null)
      setOperationError(null)
      setDeleteId(null)
      void qc.invalidateQueries({ queryKey: ['models-config'] })
    },
    onError: (e: Error, id: number) => {
      const message = `模型删除失败（/ai/admin/models/delete，id=${id}）：${e.message}`
      setDeleteError(message)
      setOperationError(message)
      toast(e.message, 'error')
    },
  })

  const setDefaultMut = useMutation({
    mutationFn: (id: number) => aiApi.adminModelsSetDefault(id),
    onSuccess: () => {
      toast('已设为默认', 'success')
      setOperationError(null)
      void qc.invalidateQueries({ queryKey: ['models-config'] })
    },
    onError: (e: Error, id: number) => {
      const modelName = modelList.find(model => model.id === id)?.modelName ?? `#${id}`
      const message = `默认模型设置失败（/ai/admin/models/set-default，${modelName}）：${e.message}`
      setOperationError(message)
      toast(e.message, 'error')
    },
  })

  const testMut = useMutation({
    mutationFn: (id: number) => aiApi.adminModelsTestConnection(id),
    onSuccess: (data, id) => {
      setTestResults((prev) => ({ ...prev, [id]: data }))
      if (data.success) {
        toast(`连通成功 · tokens=${data.tokensUsed ?? 0}`, 'success')
      } else {
        toast(data.errorMsg != null && data.errorMsg !== '' ? data.errorMsg : '连通失败', 'error')
      }
      setOperationError(null)
      setTestingId(null)
    },
    onError: (e: Error, id: number) => {
      const modelName = modelList.find(model => model.id === id)?.modelName ?? `#${id}`
      setOperationError(`连通性测试请求失败（/ai/admin/models/test-connection，${modelName}）：${e.message}。该操作不会伪造通过状态。`)
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
    setFormError(null)
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
    setFormError(null)
    setFormOpen(true)
  }

  const handleSave = () => {
    const modelName = form.modelName?.trim()
    const provider = form.provider?.trim()
    const endpoint = form.endpoint?.trim()
    if (!modelName || !provider || endpoint == null || endpoint === '') {
      const message = '请填写模型名称、提供商与模型 ID'
      setFormError(message)
      toast(message, 'error')
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
  const enabledCount = modelList.filter(isEnabled).length
  const disabledCount = modelList.length - enabledCount
  const defaultModels = modelList.filter(isDefaultModel)
  const defaultModel = defaultModels[0]
  const customBaseUrlCount = modelList.filter(m => m.apiBaseUrl != null && m.apiBaseUrl !== '').length
  const providerCount = new Set(modelList.map(m => m.modelProvider).filter(Boolean)).size
  const enabledWithoutDefault = enabledCount > 0 && defaultModels.length === 0
  const multipleDefaults = defaultModels.length > 1
  const enabledWithoutBaseUrl = modelList.filter(m => isEnabled(m) && !hasConfiguredBaseUrl(m))
  const enabledExternalWithoutDirectKey = modelList.filter(m => isEnabled(m) && providerNeedsDirectOrEnvKey(m) && !hasDirectApiKey(m))
  const quotaWarningModels = modelList.filter(m => isEnabled(m) && getQuotaPercent(m) != null && Number(getQuotaPercent(m)) >= 90)
  const noQuotaLimitCount = modelList.filter(m => isEnabled(m) && !hasExplicitQuotaLimit(m)).length
  const testedResults = Object.entries(testResults)
    .map(([id, result]) => ({ id: Number(id), model: modelList.find(item => item.id === Number(id)), result }))
    .filter(item => item.model != null)
  const hasCriticalConfigIssue = enabledWithoutDefault || multipleDefaults || enabledWithoutBaseUrl.length > 0

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 56 },
    {
      field: 'isDefault',
      headerName: '默认',
      width: 64,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        return (
          <Tooltip title={m.isDefault === 1 ? '默认模型' : '设为默认'}>
            <IconButton
              size="small"
              color={m.isDefault === 1 ? 'primary' : 'default'}
              onClick={() => m.isDefault !== 1 && setDefaultMut.mutate(m.id)}
              disabled={m.isDefault === 1}
              aria-label={m.isDefault === 1 ? `${m.modelName} 已是默认模型` : `设为默认：${m.modelName}`}
            >
              {m.isDefault === 1 ? <StarIcon fontSize="small" /> : <StarBorderIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
        )
      },
    },
    {
      field: 'modelName',
      headerName: '模型',
      flex: 0.9,
      minWidth: 240,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        return (
          <Stack spacing={0.5} sx={{ minWidth: 0, py: 0.75 }}>
            <Typography variant="body2" fontWeight={700} noWrap>
              {m.modelName}
            </Typography>
            <Stack direction="row" spacing={0.5} alignItems="center" flexWrap="wrap" useFlexGap>
              <Chip label={getProviderLabel(m.modelProvider)} size="small" variant="outlined" sx={{ height: 20, fontSize: 11 }} />
              <Chip label={m.status === 1 ? '启用' : '禁用'} size="small" color={m.status === 1 ? 'success' : 'default'} sx={{ height: 20, fontSize: 11 }} />
            </Stack>
          </Stack>
        )
      },
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 152,
      sortable: false,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        const isTesting = testingId === m.id && testMut.isPending
        const latestResult = testResults[m.id]
        return (
          <Stack direction="row" spacing={0.25} alignItems="center">
            <Tooltip title="测试连接">
              <IconButton
                size="small"
                color="primary"
                disabled={isTesting}
                aria-label={`测试连接：${m.modelName}`}
                onClick={() => {
                  setTestingId(m.id)
                  testMut.mutate(m.id)
                }}
              >
                <PlayArrowIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {latestResult ? (
              <Tooltip title={latestResult.success ? `最近连通成功，tokens=${latestResult.tokensUsed ?? 0}` : (latestResult.errorMsg || '最近连通失败')}>
                <Chip
                  label={latestResult.success ? '已通过' : '失败'}
                  size="small"
                  color={latestResult.success ? 'success' : 'error'}
                  variant="outlined"
                  sx={{ height: 22, fontSize: 11 }}
                />
              </Tooltip>
            ) : null}
            <Tooltip title="编辑">
              <IconButton size="small" aria-label={`编辑：${m.modelName}`} onClick={() => openEdit(m)}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="删除">
              <IconButton size="small" color="error" aria-label={`删除：${m.modelName}`} onClick={() => { setDeleteError(null); setDeleteId(m.id) }}>
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        )
      },
    },
    {
      field: 'resolvedBaseUrl',
      headerName: '接入信息',
      flex: 1.5,
      minWidth: 300,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        const hasCustom = m.apiBaseUrl != null && m.apiBaseUrl !== ''
        return (
          <Stack spacing={0.5} sx={{ minWidth: 0, py: 0.75 }}>
            <Tooltip title={m.modelVersion || '未配置模型 ID'} placement="top-start">
              <Typography variant="caption" sx={{ fontFamily: 'monospace' }} noWrap>
                model: {m.modelVersion || '—'}
              </Typography>
            </Tooltip>
            <Tooltip title={m.resolvedBaseUrl || '未配置'} placement="top-start">
              <Stack direction="row" spacing={0.5} alignItems="center" sx={{ minWidth: 0 }}>
                <Typography variant="caption" color={m.resolvedBaseUrl ? 'text.secondary' : 'error.main'} sx={{ fontFamily: 'monospace', minWidth: 0 }} noWrap>
                  {m.resolvedBaseUrl || '未配置 Base URL'}
                </Typography>
                {hasCustom && (
                  <Chip label="自定义" size="small" color="info" sx={{ height: 18, fontSize: 10, flexShrink: 0 }} />
                )}
              </Stack>
            </Tooltip>
          </Stack>
        )
      },
    },
    {
      field: 'runtimeGuard',
      headerName: '安全 / 配额',
      width: 190,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        const needsKey = providerNeedsDirectOrEnvKey(m)
        const used = m.quotaUsed ?? 0
        const limit = m.quotaLimit
        const hasLimit = hasExplicitQuotaLimit(m)
        const percentage = getQuotaPercent(m) ?? 0
        const color = percentage > 90 ? 'error' : percentage > 70 ? 'warning' : 'default'
        return (
          <Stack spacing={0.5} sx={{ minWidth: 0, py: 0.75 }}>
            <Typography
              variant="caption"
              sx={{ fontFamily: 'monospace', color: m.apiKeyMasked ? 'text.secondary' : needsKey ? 'warning.main' : 'text.secondary' }}
              noWrap
            >
              {m.apiKeyMasked || (needsKey ? '依赖环境变量' : '无需密钥')}
            </Typography>
            <Stack direction="row" spacing={0.5} alignItems="center">
              <Typography variant="caption" noWrap>
                {used} / {hasLimit ? limit : '未配置'}
              </Typography>
              {hasLimit && percentage > 0 && (
                <Chip label={`${percentage}%`} size="small" color={color} sx={{ height: 18, fontSize: 10 }} />
              )}
            </Stack>
          </Stack>
        )
      },
    },
    {
      field: 'runtimeParams',
      headerName: '参数',
      width: 116,
      renderCell: ({ row }) => {
        const m = row as AiModelAdminVO
        return (
          <Stack spacing={0.5} sx={{ py: 0.75 }}>
            <Typography variant="caption" noWrap>Temp {String(m.temperature ?? '—')}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap>{formatNumber(m.maxTokens)} tokens</Typography>
          </Stack>
        )
      },
    },
  ]

  const tableActions = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={() => void refetch()}>
        刷新
      </Button>
      <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={openAdd}>
        新增模型
      </Button>
    </Stack>
  )

  return (
    <Box
      data-testid="models-config-page"
      data-ready-endpoints={MODELS_CONFIG_READY_ENDPOINTS}
      data-unsupported-endpoints={MODELS_CONFIG_UNSUPPORTED_ENDPOINTS}
      data-secret-write-only="true"
      data-no-plaintext-key-display="true"
      data-no-local-model-fallback="true"
      data-no-optimistic-default-mutation="true"
      data-no-local-delete-mutation="true"
      data-no-local-connection-success="true"
      sx={{ height: 'calc(100vh - 48px - 16px)', display: 'flex', flexDirection: 'column', p: 1.25, gap: 1, overflow: 'hidden' }}
    >
      <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1 }}>
        <Stack direction={{ xs: 'column', lg: 'row' }} alignItems={{ xs: 'stretch', lg: 'center' }} spacing={1.25}>
          <Box sx={{ minWidth: 220, flex: '0 0 auto' }}>
            <Typography component="h1" variant="h5" sx={{ fontWeight: 700, lineHeight: 1.15 }}>
            模型配置
            </Typography>
            <Typography variant="caption" color="text.secondary" component="div" noWrap sx={{ mt: 0.25 }}>
              网关、密钥掩码、默认路由和连通性统一管理。
            </Typography>
          </Box>
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="模型总数" value={String(modelList.length)} helper={`启用 ${enabledCount} 个，禁用 ${disabledCount} 个`} icon={<ApiIcon fontSize="small" />} />
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="默认模型" value={defaultModel?.modelName ?? '未设置'} helper={defaultModel ? `${defaultModel.modelProvider} / ${defaultModel.modelVersion}` : '请设置一个默认模型用于兜底'} icon={<StarIcon fontSize="small" />} color={defaultModel ? 'success' : 'warning'} />
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="提供商" value={String(providerCount)} helper="当前接入的模型服务商数量" icon={<ApiIcon fontSize="small" />} />
          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', lg: 'block' } }} />
          <SummaryMetric title="自定义网关" value={String(customBaseUrlCount)} helper="配置了 API Base URL 的模型数" icon={<ApiIcon fontSize="small" />} color={customBaseUrlCount > 0 ? 'success' : 'primary'} />
        </Stack>
      </Paper>

      <Alert
        severity="info"
        data-testid="models-config-secret-contract"
        data-secret-write-only="true"
        data-no-plaintext-key-display="true"
        data-source-endpoint="/ai/admin/models/list"
        sx={{ py: 0.5, alignItems: 'center' }}
      >
        API Key 只写不读；编辑留空不覆盖旧密钥，列表只展示掩码。
      </Alert>

      {!isLoading && modelList.length > 0 ? (
        <Paper variant="outlined" sx={{ borderRadius: 1 }}>
          <Box
            data-testid="models-config-diagnostics-contract"
            data-source-endpoint="/ai/admin/models/list"
            data-no-local-model-fallback="true"
            data-no-plaintext-key-display="true"
            sx={{ p: 1.25 }}
          >
            <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', md: 'center' }} spacing={1.25}>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="subtitle2" fontWeight={700}>
                  模型可用性诊断
                </Typography>
                <Typography variant="caption" color="text.secondary" component="div" noWrap>
                  基于掩码密钥、resolvedBaseUrl、默认模型、状态和配额检查，不读取明文 key。
                </Typography>
              </Box>
              <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                <Chip color={hasCriticalConfigIssue ? 'error' : 'success'} size="small" label={hasCriticalConfigIssue ? '存在关键配置风险' : '关键配置正常'} />
                <Chip color={enabledExternalWithoutDirectKey.length > 0 ? 'warning' : 'success'} size="small" label={`环境密钥兜底 ${enabledExternalWithoutDirectKey.length}`} />
                <Chip color={quotaWarningModels.length > 0 ? 'error' : 'success'} size="small" label={`配额高风险 ${quotaWarningModels.length}`} />
                <Chip color={noQuotaLimitCount > 0 ? 'warning' : 'success'} size="small" label={`未配置模型额度 ${noQuotaLimitCount}`} />
                <Button
                  size="small"
                  variant="text"
                  endIcon={diagnosticsOpen ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
                  onClick={() => setDiagnosticsOpen(v => !v)}
                  sx={{ minWidth: 88 }}
                >
                  {diagnosticsOpen ? '收起' : '详情'}
                </Button>
              </Stack>
            </Stack>

            <Collapse in={diagnosticsOpen} timeout="auto" unmountOnExit>
              <Stack spacing={1} sx={{ mt: 1.25 }}>
                {enabledWithoutDefault ? (
                  <Alert severity="error" sx={{ py: 0.5 }}>当前有启用模型但没有默认模型。调用侧会依赖各任务映射或后端兜底，建议设置一个默认模型。</Alert>
                ) : null}
                {multipleDefaults ? (
                  <Alert severity="error" sx={{ py: 0.5 }}>检测到多个默认模型：{defaultModels.map(model => model.modelName).join('、')}。请只保留一个默认模型，避免路由不可预期。</Alert>
                ) : null}
                {enabledWithoutBaseUrl.length > 0 ? (
                  <Alert severity="error" sx={{ py: 0.5 }}>
                    启用模型缺少 resolvedBaseUrl：{enabledWithoutBaseUrl.map(model => `${model.modelName} (${getProviderLabel(model.modelProvider)})`).join('、')}。请检查自定义 Base URL 或后端 provider 默认配置。
                  </Alert>
                ) : null}
                {enabledExternalWithoutDirectKey.length > 0 ? (
                  <Alert severity="warning" sx={{ py: 0.5 }}>
                    {enabledExternalWithoutDirectKey.map(model => model.modelName).join('、')} 未返回模型级密钥，页面按“后端环境变量兜底”展示。若连通失败，请检查 provider 对应环境变量或配置中心。
                  </Alert>
                ) : null}
                {quotaWarningModels.length > 0 ? (
                  <Alert severity="error" sx={{ py: 0.5 }}>
                    {quotaWarningModels.map(model => `${model.modelName} ${getQuotaPercent(model)}%`).join('、')} 配额接近或超过上限，请调整模型额度或切换任务模型映射。
                  </Alert>
                ) : null}
                {noQuotaLimitCount > 0 ? (
                  <Alert severity="info" sx={{ py: 0.5 }}>有 {noQuotaLimitCount} 个启用模型未配置模型级 quotaLimit，页面按“未配置”展示，不把它计算成无限额度。</Alert>
                ) : null}
                {!enabledWithoutDefault && !multipleDefaults && enabledWithoutBaseUrl.length === 0 && enabledExternalWithoutDirectKey.length === 0 && quotaWarningModels.length === 0 ? (
                  <Alert severity="success" sx={{ py: 0.5 }}>启用模型均有默认路由和请求 Base URL，当前未发现配额高风险。</Alert>
                ) : null}
              </Stack>
            </Collapse>

            {testedResults.length > 0 ? (
              <Box sx={{ mt: 1.25 }}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  最近连通性测试
                </Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {testedResults.map(({ id, model, result }) => (
                    <Chip
                      key={id}
                      color={result.success ? 'success' : 'error'}
                      variant="outlined"
                      label={`${model?.modelName ?? `#${id}`}：${result.success ? `通过 tokens=${result.tokensUsed ?? 0}` : result.errorMsg || '失败'}`}
                    />
                  ))}
                </Stack>
              </Box>
            ) : null}
          </Box>
        </Paper>
      ) : null}

      {!isLoading && modelList.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="models-config-empty"
          data-source-endpoint="/ai/admin/models/list"
          data-no-local-model-fallback="true"
        >
          暂无模型配置。AI 调用会依赖系统默认配置或后端降级策略，建议先新增至少一个可连通模型。
        </Alert>
      ) : null}

      {isError && (
        <Alert
          severity="error"
          data-testid="models-config-list-error"
          data-source-endpoint="/ai/admin/models/list"
          data-no-local-model-fallback="true"
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>
              重试
            </Button>
          }
        >
          模型列表加载失败：{getErrorMessage(error)}。请检查 <code>/ai/admin/models/list</code>、管理员权限以及后端 `AiModelController`。
        </Alert>
      )}

      {testMut.isError ? (
        <Alert
          severity="error"
          data-testid="models-config-test-error"
          data-source-endpoint="/ai/admin/models/test-connection"
          data-no-local-connection-success="true"
          data-row-retained-on-action-error="true"
        >
          {operationError ?? `连通性测试请求失败：${getErrorMessage(testMut.error)}。该操作只调用真实 /ai/admin/models/test-connection，不会伪造连通状态。`}
        </Alert>
      ) : null}

      {setDefaultMut.isError ? (
        <Alert
          severity="error"
          data-testid="models-config-default-error"
          data-source-endpoint="/ai/admin/models/set-default"
          data-no-optimistic-default-mutation="true"
          data-row-retained-on-action-error="true"
        >
          {operationError ?? `默认模型设置失败：${getErrorMessage(setDefaultMut.error)}。请确认模型未被删除且仍可用。`}
        </Alert>
      ) : null}

      {saveMut.isError ? (
        <Alert
          severity="error"
          data-testid="models-config-save-error"
          data-source-endpoint="/ai/admin/models/save"
          data-input-retained="true"
          data-secret-write-only="true"
        >
          {operationError ?? `模型保存失败：${getErrorMessage(saveMut.error)}。请检查必填字段、模型 ID 和 API Base URL。`}
        </Alert>
      ) : null}

      {deleteMut.isError ? (
        <Alert
          severity="error"
          data-testid="models-config-delete-error"
          data-source-endpoint="/ai/admin/models/delete"
          data-no-local-delete-mutation="true"
          data-row-retained-on-action-error="true"
        >
          {deleteError ?? `模型删除失败：${getErrorMessage(deleteMut.error)}。删除为后端逻辑删除，失败时页面不会移除本地行。`}
        </Alert>
      ) : null}

      {testMut.isPending ? <LinearProgress aria-label="模型连通性测试中" /> : null}

      <Box sx={{ flex: 1, minHeight: 320, overflow: 'hidden' }}>
        <StandardDataGrid
          rows={modelList}
          columns={columns}
          loading={isLoading}
          pageSizeOptions={[10, 20, 50]}
          initialState={{
            pagination: { paginationModel: { pageSize: 20 } },
          }}
          actionSlot={tableActions}
          rowHeight={64}
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

      {/* 编辑对话框 */}
      <FormDialog
        open={formOpen}
        title={form.id ? '编辑模型' : '新增模型'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave}
        loading={saveMut.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Alert
            severity="info"
            data-testid="models-config-form-save-contract"
            data-secret-write-only="true"
            data-no-plaintext-key-display="true"
          >
            <AlertTitle>保存契约</AlertTitle>
            这里提交 `modelName/provider/endpoint/apiBaseUrl/apiKey/maxTokens/temperature/status/isDefault`。编辑时 API Key 留空不会覆盖后端已有密钥。
          </Alert>
          <TextField
            label="模型名称（展示用）"
            value={form.modelName ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, modelName: e.target.value }))}
            fullWidth
            placeholder="例如：GPT-4 Turbo"
          />
          <TextField
            select
            label="提供商"
            value={form.provider ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, provider: e.target.value }))}
            fullWidth
            size="small"
          >
            <MenuItem value="">选择提供商</MenuItem>
            {PROVIDERS.map((p) => (
              <MenuItem key={p} value={p}>
                {PROVIDER_LABELS[p] || p}
              </MenuItem>
            ))}
          </TextField>
          {form.provider ? (
            <Alert severity={form.provider === 'custom' && !form.apiBaseUrl ? 'warning' : 'info'}>
              {PROVIDER_DEFAULT_BASE_URLS[form.provider] ?? '未知 provider 会按后端默认 OpenAI 兼容地址解析。'}
            </Alert>
          ) : null}
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
          {formError != null ? (
            <Alert
              severity="error"
              data-testid="models-config-form-error"
              data-input-retained="true"
              data-secret-write-only="true"
            >
              {formError}。弹窗会保留当前模型名称、provider、模型 ID、Base URL 和 API Key 输入，便于修正后重试。
            </Alert>
          ) : null}
        </Stack>
      </FormDialog>

      {/* 删除确认对话框 */}
      <ConfirmDialog
        open={deleteId !== null}
        content={deleteError ?? '确定删除该模型配置？此操作不可恢复。'}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId != null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
