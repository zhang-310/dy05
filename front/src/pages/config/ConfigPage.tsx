import { useState, useCallback, useMemo } from 'react'
import { Alert, Box, Button, Chip, FormControlLabel, MenuItem, Stack, Switch, TextField } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { configApi, type ConfigSave, type SysConfig } from '@/api/config'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const VALUE_TYPES = ['string', 'number', 'boolean', 'json']
const CONFIG_ENDPOINTS = {
  list: '/config/list',
  save: '/config/save',
  delete: '/config/delete',
} as const
const CONFIG_UNSUPPORTED_ACTIONS = [
  'plaintext-sensitive-value-display',
  'local-config-fallback',
  'direct-secret-read',
  'implicit-config-delete',
]

export default function ConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, configKey: '' })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ConfigSave>>({})
  const [formValidationError, setFormValidationError] = useState('')
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({ queryKey: ['sys-configs', search], queryFn: () => configApi.list(search) })
  const saveMut = useMutation({ mutationFn: configApi.save, onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['sys-configs'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const delMut = useMutation({ mutationFn: configApi.delete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['sys-configs'] }) }, onError: (e: Error) => toast(e.message, 'error') })

  const openAdd = useCallback(() => {
    setFormValidationError('')
    setForm({ valueType: 'string', isSensitive: 0 })
    setFormOpen(true)
  }, [])
  const openEdit = useCallback((row: SysConfig) => {
    const isSensitive = Number(row.isSensitive ?? 0)
    setFormValidationError('')
    setForm({
      ...row,
      configValue: isSensitive === 1 ? '' : row.configValue,
      isSensitive,
    })
    setFormOpen(true)
  }, [])

  const handleSave = () => {
    const configKey = String(form.configKey ?? '').trim()
    if (!configKey) {
      setFormValidationError('请填写配置键')
      toast('请填写配置键', 'warning')
      return
    }
    if (form.id && Number(form.isSensitive ?? 0) === 1 && String(form.configValue ?? '').trim() === '') {
      setFormValidationError('敏感配置编辑必须输入新值，避免将脱敏值或空值覆盖真实配置。')
      toast('敏感配置编辑必须输入新值', 'warning')
      return
    }
    setFormValidationError('')
    saveMut.mutate({
      id: form.id,
      configKey,
      configValue: String(form.configValue ?? ''),
      valueType: form.valueType ?? 'string',
      isSensitive: Number(form.isSensitive ?? 0),
      configType: String(form.configType ?? '').trim() || undefined,
      configName: String(form.configName ?? '').trim() || undefined,
    })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'configKey', headerName: '配置键', flex: 1.2, minWidth: 180 },
    { field: 'configName', headerName: '配置名/说明', flex: 1, minWidth: 160, valueGetter: (_, row) => (row as SysConfig).configName ?? '-' },
    { field: 'configType', headerName: '分组', width: 120, valueGetter: (_, row) => (row as SysConfig).configType ?? '-' },
    {
      field: 'configValue',
      headerName: '配置值',
      flex: 1.5,
      minWidth: 220,
      valueGetter: (_, row) => Number((row as SysConfig).isSensitive ?? 0) === 1 ? '******' : ((row as SysConfig).configValue ?? ''),
    },
    { field: 'valueType', headerName: '类型', width: 100, valueGetter: (_, row) => (row as SysConfig).valueType ?? 'string' },
    {
      field: 'isSensitive',
      headerName: '敏感',
      width: 90,
      renderCell: ({ row }) => Number((row as SysConfig).isSensitive ?? 0) === 1
        ? <Chip label="已脱敏" size="small" color="warning" variant="outlined" />
        : <Chip label="普通" size="small" variant="outlined" />,
    },
    { field: 'actions', headerName: '操作', width: 140, sortable: false, renderCell: ({ row }) => <Stack direction="row" gap={1}><Button size="small" onClick={() => openEdit(row)}>编辑</Button><Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button></Stack> },
  ]

  const searchSlot = (
    <>
      <TextField label="配置键" size="small" value={query.configKey} onChange={e => setQuery(q => ({ ...q, configKey: e.target.value }))} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { setQuery(q => ({ ...q, configKey: '' })); setSearch({ page: 0, rows: 20, configKey: '' }) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增配置</Button>
  )

  const listErrorMessage = error instanceof Error ? error.message : '配置列表加载失败，请检查 /config/list。'
  const saveErrorMessage = saveMut.isError ? `${CONFIG_ENDPOINTS.save} 保存失败：${getErrorMessage(saveMut.error)}` : ''
  const deleteErrorMessage = delMut.isError ? `${CONFIG_ENDPOINTS.delete} 删除失败：${getErrorMessage(delMut.error)}` : ''
  const rows = normalizeRows<SysConfig>(data)
  const total = readTotal(data, rows.length)
  const deletingConfig = useMemo(() => rows.find(row => row.id === deleteId), [deleteId, rows])
  const sensitiveCount = rows.filter(row => Number(row.isSensitive ?? 0) === 1).length
  const editingSensitiveConfig = Boolean(form.id && Number(form.isSensitive ?? 0) === 1)

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="config-page-workbench"
      data-contract-scope="system-config-keyvalue-management"
      data-ready-endpoints={Object.values(CONFIG_ENDPOINTS).join('|')}
      data-unsupported-actions={CONFIG_UNSUPPORTED_ACTIONS.join('|')}
      data-query-key={search.configKey || 'empty'}
      data-row-count={rows.length}
      data-total-count={total}
      data-sensitive-count={sensitiveCount}
      data-loading={String(isFetching)}
      data-form-open={String(formOpen)}
      data-delete-open={String(deleteId !== null)}
      data-no-local-config-fallback="true"
      data-no-plaintext-sensitive-value="true"
    >
      <PageHeader
        title="系统配置"
        subtitle="配置键值对维护，适合运行时开关和基础参数。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert severity="info" variant="outlined">
        真实接口：<code>/config/list</code>、<code>/config/save</code>、<code>/config/delete</code>。
        后端字段为 <code>configName</code>、<code>configType</code>、<code>valueType</code>、<code>isSensitive</code>；敏感配置只展示脱敏值。
      </Alert>

      {isError && (
        <Box
          data-testid="config-list-error"
          data-contract-source={CONFIG_ENDPOINTS.list}
          data-no-local-config-fallback="true"
        >
          <ErrorAlert title="配置列表加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {saveMut.isError && (
        <Alert
          severity="error"
          data-testid="config-save-error"
          data-contract-source={CONFIG_ENDPOINTS.save}
          data-input-retained="true"
          data-no-local-config-fallback="true"
        >
          {saveErrorMessage}
        </Alert>
      )}
      {delMut.isError && (
        <Alert
          severity="error"
          data-testid="config-delete-error"
          data-contract-source={CONFIG_ENDPOINTS.delete}
          data-target-id={String(deleteId ?? 'none')}
          data-row-retained="true"
          data-no-local-config-fallback="true"
        >
          {deleteErrorMessage}
        </Alert>
      )}

      <StandardDataGrid
        rows={rows}
        columns={columns}
        rowCount={total}
        loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={(
          <Box
            data-testid="config-search-contract"
            data-contract-source={CONFIG_ENDPOINTS.list}
            data-query-key={query.configKey || 'empty'}
            data-applied-key={search.configKey || 'empty'}
            data-page={search.page}
            data-page-size={search.rows}
            data-no-local-config-fallback="true"
            sx={{ display: 'contents' }}
          >
            {searchSlot}
          </Box>
        )}
        actionSlot={(
          <Box
            data-testid="config-action-contract"
            data-contract-source={CONFIG_ENDPOINTS.save}
            data-no-direct-secret-read="true"
            sx={{ display: 'contents' }}
          >
            {actionSlot}
          </Box>
        )}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={form.id ? '编辑配置' : '新增配置'} onClose={() => setFormOpen(false)} onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack
          spacing={2}
          sx={{ pt: 1 }}
          data-testid="config-form-contract"
          data-contract-source={CONFIG_ENDPOINTS.save}
          data-mode={form.id ? 'edit' : 'create'}
          data-config-key={String(form.configKey ?? '') || 'empty'}
          data-value-type={form.valueType ?? 'string'}
          data-is-sensitive={String(Number(form.isSensitive ?? 0) === 1)}
          data-sensitive-value-prefilled={editingSensitiveConfig && String(form.configValue ?? '') !== '' ? 'true' : 'false'}
          data-sensitive-value-redacted-on-edit={String(editingSensitiveConfig)}
          data-loading={String(saveMut.isPending)}
          data-input-retained={saveErrorMessage || formValidationError ? 'true' : 'false'}
        >
          {formValidationError && (
            <Alert
              severity="warning"
              data-testid="config-form-validation-error"
              data-contract-source={CONFIG_ENDPOINTS.save}
              data-no-plaintext-sensitive-value="true"
            >
              {formValidationError}
            </Alert>
          )}
          {saveErrorMessage && (
            <Alert
              severity="error"
              data-testid="config-form-save-error"
              data-contract-source={CONFIG_ENDPOINTS.save}
              data-input-retained="true"
            >
              {saveErrorMessage}
            </Alert>
          )}
          <TextField label="配置键" value={form.configKey ?? ''} onChange={e => setForm(f => ({ ...f, configKey: e.target.value }))} fullWidth required />
          <TextField label="配置名/说明" value={form.configName ?? ''} onChange={e => setForm(f => ({ ...f, configName: e.target.value }))} fullWidth />
          <TextField label="分组" value={form.configType ?? ''} onChange={e => setForm(f => ({ ...f, configType: e.target.value }))} fullWidth />
          <TextField select label="值类型" value={form.valueType ?? 'string'} onChange={e => setForm(f => ({ ...f, valueType: e.target.value }))} fullWidth>
            {VALUE_TYPES.map(type => <MenuItem key={type} value={type}>{type}</MenuItem>)}
          </TextField>
          <TextField
            label="配置值"
            value={form.configValue ?? ''}
            onChange={e => {
              setFormValidationError('')
              setForm(f => ({ ...f, configValue: e.target.value }))
            }}
            fullWidth
            multiline
            minRows={2}
            placeholder={editingSensitiveConfig ? '敏感配置不回填旧值；如需保存请填写新值' : undefined}
            helperText={editingSensitiveConfig ? '敏感配置旧值不会从列表回填，避免把脱敏值写回真实配置。' : undefined}
          />
          <FormControlLabel
            control={<Switch checked={Number(form.isSensitive ?? 0) === 1} onChange={e => setForm(f => ({ ...f, isSensitive: e.target.checked ? 1 : 0 }))} />}
            label="敏感配置，列表仅展示脱敏值"
          />
        </Stack>
      </FormDialog>
      {deleteId !== null && (
        <Box
          data-testid="config-delete-contract"
          data-contract-source={CONFIG_ENDPOINTS.delete}
          data-target-id={String(deleteId)}
          data-target-key={deletingConfig?.configKey ?? 'unknown'}
          data-loading={String(delMut.isPending)}
          data-row-retained={delMut.isError ? 'true' : 'false'}
          data-no-local-config-fallback="true"
          sx={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}
        />
      )}
      <ConfirmDialog
        open={deleteId !== null}
        content={`${deleteErrorMessage ? `${deleteErrorMessage}\n` : ''}确定要删除配置「${deletingConfig?.configKey ?? deleteId ?? '-'}」吗？`}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending}
      />
    </Box>
  )
}
