import { useState, useCallback } from 'react'
import {
  Box, TextField, Button, Stack, Chip, Switch,
  Drawer, Typography, Divider, IconButton,
  MenuItem, Select, Grid, Card, CardContent, Alert,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { systemApi, type ExternalApiConfig, type ExternalApiConfigSave } from '@/api/system'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

const CATEGORY_CONFIG: Record<string, { label: string; color: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'default' | 'info' }> = {
  ai: { label: 'AI', color: 'primary' },
  data: { label: '数据', color: 'info' },
  media: { label: '媒体', color: 'secondary' },
  sms: { label: '短信', color: 'success' },
  llm: { label: 'LLM', color: 'primary' },
  video: { label: '视频', color: 'secondary' },
  tts: { label: 'TTS', color: 'success' },
  trend: { label: '热点', color: 'warning' },
  audit: { label: '审核', color: 'error' },
  storage: { label: '存储', color: 'default' },
}

const CATEGORY_OPTIONS = ['ai', 'data', 'media', 'sms', 'llm', 'video', 'tts', 'trend', 'audit', 'storage']
const EXTERNAL_API_CONFIG_ENDPOINTS = [
  '/system/external-api/list',
  '/system/external-api/save',
  '/system/external-api/delete',
].join('|')
const EXTERNAL_API_CONFIG_UNSUPPORTED_ENDPOINTS = [
  '/system/external-api/get',
  '/system/external-api/get-secret',
  '/system/external-api/probe',
  '/system/external-api/ping',
  '/system/external-api/export',
  '/system/external-api/rotate-secret-local',
].join('|')
const EXTERNAL_API_CONFIG_UNSUPPORTED_ACTIONS = [
  'plaintext-secret-display',
  'browser-health-probe',
  'local-api-config-fallback',
  'optimistic-enable-toggle',
].join('|')

const defaultForm: Partial<ExternalApiConfigSave> = {
  providerCode: '', providerName: '', category: 'ai', baseUrl: '', isEnabled: true, priority: 0,
}

function toSavePayload(row: ExternalApiConfig, isEnabled = row.isEnabled): Partial<ExternalApiConfigSave> {
  return {
    id: row.id,
    providerCode: row.providerCode,
    providerName: row.providerName,
    category: row.category,
    baseUrl: row.baseUrl,
    isEnabled,
    priority: row.priority,
    rateLimitPerMin: row.rateLimitPerMin,
    dailyQuota: row.dailyQuota,
    monthlyQuota: row.monthlyQuota,
    extraConfig: row.extraConfig,
  }
}

export default function ExternalApiConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, category: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ExternalApiConfigSave>>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [detail, setDetail] = useState<ExternalApiConfig | null>(null)
  const [operationError, setOperationError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['external-api-configs', search],
    queryFn: () => systemApi.externalApiList({
      page: search.page, rows: search.rows,
      category: search.category || undefined,
    }),
  })

  const saveMut = useMutation({
    mutationFn: systemApi.externalApiSave,
    onSuccess: () => { toast('保存成功', 'success'); setOperationError(null); setFormOpen(false); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error) => {
      setOperationError(`/system/external-api/save 保存失败：${getErrorMessage(e)}。供应商 ${form.providerCode || '-'} 的输入已保留。`)
      toast(e.message, 'error')
    },
  })
  const delMut = useMutation({
    mutationFn: systemApi.externalApiDelete,
    onSuccess: () => { toast('删除成功', 'success'); setOperationError(null); setDeleteId(null); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error) => {
      setOperationError(`/system/external-api/delete 删除失败：${getErrorMessage(e)}。配置 ID ${deleteId ?? '-'} 已保留。`)
      toast(e.message, 'error')
    },
  })
  const toggleMut = useMutation({
    mutationFn: (row: ExternalApiConfig) => systemApi.externalApiSave(toSavePayload(row, !row.isEnabled)),
    onSuccess: () => { toast('状态已更新', 'success'); setOperationError(null); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error, row) => {
      setOperationError(`/system/external-api/save 启停失败：${getErrorMessage(e)}。供应商 ${row.providerCode} 的原状态已保留。`)
      toast(e.message, 'error')
    },
  })

  const openAdd = useCallback(() => { setForm({ ...defaultForm }); setFormOpen(true) }, [])
  const openEdit = useCallback((row: ExternalApiConfig) => { setForm(toSavePayload(row)); setFormOpen(true) }, [])

  const configs = data?.list ?? []
  const enabledCount = configs.filter(item => item.isEnabled).length
  const healthyCount = configs.filter(item => item.healthStatus === 'healthy').length
  const degradedCount = configs.filter(item => item.healthStatus === 'down' || item.healthStatus === 'degraded').length

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'providerName', headerName: '供应商', flex: 1, minWidth: 140,
      renderCell: ({ row, value }) => (
        <Button size="small" variant="text" sx={{ justifyContent: 'flex-start' }}
          onClick={() => setDetail(row as ExternalApiConfig)}>{String(value)}</Button>
      ) },
    { field: 'providerCode', headerName: '编码', width: 140 },
    { field: 'category', headerName: '分类', width: 90,
      renderCell: ({ value }) => {
        const c = CATEGORY_CONFIG[value as string] ?? { label: String(value), color: 'default' as const }
        return <Chip label={c.label} color={c.color} size="small" />
      } },
    { field: 'baseUrl', headerName: 'Base URL', flex: 1.5, minWidth: 180 },
    { field: 'healthStatus', headerName: '健康', width: 100,
      renderCell: ({ value }) => {
        const status = String(value ?? 'unknown')
        return <Chip label={status} color={status === 'healthy' ? 'success' : status === 'down' ? 'error' : 'warning'} size="small" variant="outlined" />
      } },
    { field: 'isEnabled', headerName: '启用', width: 80,
      renderCell: ({ row }) => (
        <Switch size="small" checked={(row as ExternalApiConfig).isEnabled}
          inputProps={{ 'aria-label': `切换 ${(row as ExternalApiConfig).providerName} 启用状态` }}
          data-testid="external-api-config-toggle-contract"
          data-contract-source="/system/external-api/save"
          data-provider-code={(row as ExternalApiConfig).providerCode}
          data-current-enabled={String((row as ExternalApiConfig).isEnabled)}
          data-row-retained-on-error="true"
          data-no-optimistic-enable-toggle="true"
          onClick={e => e.stopPropagation()}
          onChange={() => toggleMut.mutate(row as ExternalApiConfig)}
          disabled={toggleMut.isPending} />
      ) },
    { field: 'createTime', headerName: '创建时间', width: 150, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={0.5}>
          <Button size="small" onClick={() => openEdit(row as ExternalApiConfig)}>编辑</Button>
          <Button
            size="small"
            color="error"
            data-testid="external-api-config-delete-open-button"
            data-contract-source="/system/external-api/delete"
            data-target-id={(row as ExternalApiConfig).id}
            onClick={() => setDeleteId((row as ExternalApiConfig).id)}
          >
            删除
          </Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Select size="small" value={query.category} displayEmpty
        inputProps={{ 'aria-label': '配置分类筛选' }}
        onChange={e => { setQuery(q => ({ ...q, category: e.target.value })); setSearch(s => ({ ...s, category: e.target.value, page: 0 })) }}
        sx={{ minWidth: 110 }}>
        <MenuItem value="">全部分类</MenuItem>
        {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{CATEGORY_CONFIG[c]?.label ?? c}</MenuItem>)}
      </Select>
      <Button variant="contained" size="small" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
    </Stack>
  )

  const actionSlot = (
    <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openAdd}>新增配置</Button>
  )

  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 2 }}
      data-testid="external-api-config-page-workbench"
      data-contract-scope="system-external-api-config-management"
      data-ready-endpoints={EXTERNAL_API_CONFIG_ENDPOINTS}
      data-unsupported-endpoints={EXTERNAL_API_CONFIG_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={EXTERNAL_API_CONFIG_UNSUPPORTED_ACTIONS}
      data-row-count={configs.length}
      data-total-count={data?.total ?? 0}
      data-enabled-count={enabledCount}
      data-healthy-count={healthyCount}
      data-degraded-count={degradedCount}
      data-loading={String(isFetching)}
      data-load-error={String(isError)}
      data-operation-error={operationError ?? ''}
      data-form-open={String(formOpen)}
      data-delete-id={deleteId ?? ''}
      data-detail-open={String(detail !== null)}
      data-no-local-api-config-fallback="true"
      data-no-plaintext-secret-display="true"
      data-no-browser-health-probe="true"
      data-no-optimistic-enable-toggle="true"
      data-secret-write-only="true"
    >
      <PageHeader
        title="外部 API 配置"
        subtitle="管理供应商接入、密钥加密提交、启停和健康状态字段，不在前端回显明文密钥"
        actions={(
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="external-api-config-source-contract"
        data-contract-source="/system/external-api/list|/system/external-api/save|/system/external-api/delete"
        data-unsupported-endpoints={EXTERNAL_API_CONFIG_UNSUPPORTED_ENDPOINTS}
        data-no-local-api-config-fallback="true"
        data-no-plaintext-secret-display="true"
        data-secret-write-only="true"
      >
        列表、启停和删除均走 `/system/external-api/*` 后端接口；API Key / Secret 仅在新增或轮换时以密码框提交，列表与详情不回显明文。
      </Alert>

      <Grid container spacing={2}>
        {[
          { label: '配置总数', value: configs.length },
          { label: '已启用', value: enabledCount, color: 'success.main' },
          { label: '健康', value: healthyCount, color: 'success.main' },
          { label: '异常/降级', value: degradedCount, color: degradedCount > 0 ? 'error.main' : 'text.primary' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5 }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h5" fontWeight={700} color={item.color}>{item.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Box
          data-testid="external-api-config-load-error"
          data-contract-source="/system/external-api/list"
          data-no-local-api-config-fallback="true"
        >
          <ErrorAlert
            title="外部 API 配置加载失败"
            message={error instanceof Error ? error.message : '外部 API 配置接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      {operationError && (
        <Box
          data-testid="external-api-config-operation-error"
          data-contract-source={operationError.match(/\/system\/external-api\/[a-z-]+/)?.[0] ?? 'unknown'}
          data-input-retained={String(operationError.includes('输入已保留'))}
          data-row-retained={String(operationError.includes('已保留') || operationError.includes('原状态已保留'))}
          data-no-local-api-config-fallback="true"
        >
          <ErrorAlert
            title="外部 API 配置操作失败"
            message={operationError}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      <Box
        data-testid="external-api-config-grid-contract"
        data-contract-source="/system/external-api/list"
        data-row-count={configs.length}
        data-total-count={data?.total ?? 0}
        data-category-filter={search.category || 'all'}
        data-no-local-api-config-fallback="true"
        data-no-local-category-filter="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={configs} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={(
            <Box
              data-testid="external-api-config-search-contract"
              data-contract-source="/system/external-api/list"
              data-category-filter={query.category || 'all'}
              data-applied-category={search.category || 'all'}
              data-no-local-api-config-fallback="true"
              data-no-local-category-filter="true"
              sx={{ display: 'contents' }}
            >
              {searchSlot}
            </Box>
          )}
          actionSlot={(
            <Box
              data-testid="external-api-config-action-contract"
              data-contract-source="/system/external-api/save"
              data-no-direct-secret-read="true"
              data-secret-write-only="true"
              sx={{ display: 'contents' }}
            >
              {actionSlot}
            </Box>
          )}
          sx={{ flex: 1 }}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      <FormDialog open={formOpen} title={form.id ? '编辑外部API配置' : '新增外部API配置'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack
          spacing={2}
          sx={{ pt: 1 }}
          data-testid="external-api-config-form-contract"
          data-contract-source="/system/external-api/save"
          data-mode={form.id ? 'edit' : 'create'}
          data-provider-code={form.providerCode || 'empty'}
          data-input-retained={String(Boolean(operationError?.includes('/system/external-api/save 保存失败')))}
          data-no-plaintext-secret-display="true"
          data-no-direct-secret-read="true"
          data-secret-write-only="true"
        >
          {operationError?.includes('/system/external-api/save 保存失败') && (
            <Alert
              severity="error"
              data-testid="external-api-config-form-save-error"
              data-contract-source="/system/external-api/save"
              data-input-retained="true"
            >
              {operationError}
            </Alert>
          )}
          <Alert severity="info">
            API Key / Secret 只在新增或轮换时提交给后端加密存储，列表和详情不会回显明文。
          </Alert>
          <TextField label="供应商编码" required value={form.providerCode ?? ''} onChange={e => setForm(f => ({ ...f, providerCode: e.target.value }))} fullWidth size="small" />
          <TextField label="供应商名称" required value={form.providerName ?? ''} onChange={e => setForm(f => ({ ...f, providerName: e.target.value }))} fullWidth size="small" />
          <Select size="small" value={form.category ?? 'ai'} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} displayEmpty fullWidth>
            {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{CATEGORY_CONFIG[c]?.label ?? c}</MenuItem>)}
          </Select>
          <TextField label="Base URL" required value={form.baseUrl ?? ''} onChange={e => setForm(f => ({ ...f, baseUrl: e.target.value }))} fullWidth size="small" />
          <Box
            data-testid="external-api-config-secret-write-contract"
            data-contract-source="/system/external-api/save"
            data-secret-write-only="true"
            data-no-direct-secret-read="true"
            data-no-plaintext-secret-display="true"
          >
            <Stack spacing={2}>
              <TextField label="API Key（可选，提交后后端加密存储）" type="password" value={form.apiKey ?? ''}
                onChange={e => setForm(f => ({ ...f, apiKey: e.target.value }))} fullWidth size="small"
                placeholder="新增或轮换时填写" />
              <TextField label="API Secret（可选，提交后后端加密存储）" type="password" value={form.apiSecret ?? ''}
                onChange={e => setForm(f => ({ ...f, apiSecret: e.target.value }))} fullWidth size="small"
                placeholder="新增或轮换时填写" />
            </Stack>
          </Box>
          <Stack direction="row" spacing={1}>
            <TextField label="优先级" type="number" value={form.priority ?? 0}
              onChange={e => setForm(f => ({ ...f, priority: Number(e.target.value || 0) }))}
              fullWidth size="small" />
            <TextField label="每分钟限流" type="number" value={form.rateLimitPerMin ?? ''}
              onChange={e => setForm(f => ({ ...f, rateLimitPerMin: e.target.value === '' ? undefined : Number(e.target.value) }))}
              fullWidth size="small" />
          </Stack>
        </Stack>
      </FormDialog>

      {deleteId !== null && (
        <Box
          data-testid="external-api-config-delete-contract"
          data-contract-source="/system/external-api/delete"
          data-target-id={String(deleteId)}
          data-row-retained={String(delMut.isError)}
          data-no-local-api-config-fallback="true"
          sx={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}
        />
      )}
      <ConfirmDialog open={deleteId !== null} content="确定要删除该API配置吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />

      <Drawer anchor="right" open={detail !== null} onClose={() => setDetail(null)}
        PaperProps={{ sx: { width: 480, p: 3 } }}>
        {detail && (
          <Stack
            spacing={2}
            data-testid="external-api-config-detail-drawer"
            data-contract-source="/system/external-api/list"
            data-unsupported-endpoints="/system/external-api/get|/system/external-api/get-secret"
            data-provider-code={detail.providerCode}
            data-has-encrypted-secret={String(Boolean(detail.apiKeyEncrypted || detail.apiSecretEncrypted))}
            data-no-plaintext-secret-display="true"
            data-no-direct-secret-read="true"
          >
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">{detail.providerName}</Typography>
              <IconButton onClick={() => setDetail(null)}><CloseIcon /></IconButton>
            </Stack>
            <Divider />
            <Box><Typography variant="caption" color="text.secondary">分类</Typography>
              <Typography>{CATEGORY_CONFIG[detail.category ?? '']?.label ?? detail.category ?? '-'}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">供应商编码</Typography>
              <Typography>{detail.providerCode}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">Base URL</Typography>
              <Typography sx={{ wordBreak: 'break-all' }}>{detail.baseUrl}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">健康状态</Typography>
              <Typography>{detail.healthStatus ?? 'unknown'}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">密钥存储</Typography>
              <Typography>{detail.apiKeyEncrypted || detail.apiSecretEncrypted ? '已加密保存，明文不可回显' : '未检测到已保存密钥'}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">状态</Typography>
              <Chip label={detail.isEnabled ? '已启用' : '已禁用'} color={detail.isEnabled ? 'success' : 'default'} size="small" /></Box>
            <Box><Typography variant="caption" color="text.secondary">创建时间</Typography>
              <Typography>{formatDate(detail.createTime)}</Typography></Box>
            <Stack direction="row" spacing={1}>
              <Button size="small" variant="outlined" onClick={() => { openEdit(detail); setDetail(null) }}>编辑</Button>
            </Stack>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
