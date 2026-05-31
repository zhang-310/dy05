import { useState, useCallback } from 'react'
import { Alert, Box, Button, Card, CardContent, Grid, Stack, Switch, FormControlLabel, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { systemApi, type AlertRule, type AlertRuleSave } from '@/api/system'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { TextField, MenuItem } from '@mui/material'
import { normalizeRows, readTotal } from '@/utils/response-normalize'
import { getErrorMessage } from '@/utils/errorHandler'

const defaultForm: AlertRuleSave = { ruleName: '', metric: '', threshold: 0, operator: '>', severity: 'warning', status: 1 }
const ALERT_RULE_READY_ENDPOINTS = [
  '/monitoring/alert-rules/search',
  '/monitoring/alert-rules/create',
  '/monitoring/alert-rules/update',
  '/monitoring/alert-rules/delete',
  '/monitoring/alert-rules/enable',
  '/monitoring/alert-rules/disable',
].join('|')

const ALERT_RULE_UNSUPPORTED_ACTIONS = [
  'local-filter',
  'local-rule-fallback',
  'legacy-system-alert-rule-endpoints',
  'optimistic-status-mutation',
].join('|')

export default function AlertRulesPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20 })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<AlertRuleSave>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [operationError, setOperationError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['alert-rules', search],
    queryFn: () => systemApi.alertRuleSearch(search),
  })

  const saveMut = useMutation({
    mutationFn: async (p: Partial<AlertRuleSave>): Promise<void> => { await (p.id ? systemApi.alertRuleUpdate(p) : systemApi.alertRuleCreate(p)) },
    onSuccess: () => { toast('保存成功', 'success'); setOperationError(null); setFormOpen(false); qc.invalidateQueries({ queryKey: ['alert-rules'] }) },
    onError: (e: Error) => {
      const endpoint = form.id ? '/monitoring/alert-rules/update' : '/monitoring/alert-rules/create'
      setOperationError(`${endpoint} 保存失败：${getErrorMessage(e)}。表单输入已保留。`)
      toast(e.message, 'error')
    },
  })
  const delMut = useMutation({
    mutationFn: systemApi.alertRuleDelete,
    onSuccess: () => { toast('删除成功', 'success'); setOperationError(null); setDeleteId(null); qc.invalidateQueries({ queryKey: ['alert-rules'] }) },
    onError: (e: Error) => {
      setOperationError(`/monitoring/alert-rules/delete 删除失败：${getErrorMessage(e)}。规则 ID ${deleteId ?? '-'} 已保留。`)
      toast(e.message, 'error')
    },
  })
  const toggleMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) =>
      status === 1 ? systemApi.alertRuleEnable(id) : systemApi.alertRuleDisable(id),
    onSuccess: () => { toast('状态已更新', 'success'); setOperationError(null); qc.invalidateQueries({ queryKey: ['alert-rules'] }) },
    onError: (e: Error, variables) => {
      const endpoint = variables.status === 1 ? '/monitoring/alert-rules/enable' : '/monitoring/alert-rules/disable'
      setOperationError(`${endpoint} 状态更新失败：${getErrorMessage(e)}。规则 ID ${variables.id} 的原状态已保留。`)
      toast(e.message, 'error')
    },
  })

  const openAdd = useCallback(() => { setForm({ ...defaultForm }); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AlertRule) => {
    setForm({ id: row.id, ruleName: row.ruleName, metric: row.metric, threshold: row.threshold, operator: row.operator, severity: row.severity, status: row.status })
    setFormOpen(true)
  }, [])

  const rows = normalizeRows<AlertRule>(data)
  const total = readTotal(data, rows.length)
  const enabledCount = rows.filter(row => row.status === 1 || row.enabled).length
  const criticalCount = rows.filter(row => String(row.severity).toLowerCase() === 'critical' || String(row.severity).toLowerCase() === 'high').length
  const warningCount = rows.filter(row => String(row.severity).toLowerCase() === 'warning' || String(row.severity).toLowerCase() === 'medium').length
  const deleteTarget = rows.find(row => row.id === deleteId)

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'ruleName', headerName: '规则名称', flex: 1 },
    { field: 'metric', headerName: '指标', width: 150 },
    { field: 'threshold', headerName: '阈值', width: 90 },
    { field: 'severity', headerName: '严重级别', width: 100 },
    {
      field: 'status', headerName: '启用', width: 90,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Switch
          size="small"
          checked={row.status === 1}
          inputProps={{ 'aria-label': `切换 ${row.ruleName ?? row.name ?? row.id} 告警规则` }}
          data-testid="alert-rule-toggle-contract"
          data-contract-source={row.status === 1 ? '/monitoring/alert-rules/disable' : '/monitoring/alert-rules/enable'}
          data-rule-id={row.id}
          data-current-status={row.status}
          data-row-retained-on-error="true"
          disabled={toggleMut.isPending}
          onChange={e => toggleMut.mutate({ id: row.id, status: e.target.checked ? 1 : 0 })} />
      ),
    },
    {
      field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: ({ value }) => formatDate(value),
    },
    {
      field: '_actions', headerName: '操作', width: 120,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            data-testid="alert-rule-edit-button"
            data-contract-source="/monitoring/alert-rules/update"
            data-rule-id={row.id}
            onClick={() => openEdit(row as AlertRule)}
          >
            编辑
          </Button>
          <Button
            size="small"
            color="error"
            data-testid="alert-rule-delete-open-button"
            data-contract-source="/monitoring/alert-rules/delete"
            data-rule-id={row.id}
            onClick={() => setDeleteId(row.id)}
          >
            删除
          </Button>
        </Stack>
      ),
    },
  ]

  const actionSlot = (
    <Button
      size="small"
      variant="contained"
      startIcon={<AddIcon />}
      data-testid="alert-rule-create-button"
      data-contract-source="/monitoring/alert-rules/create"
      onClick={openAdd}
    >
      新增规则
    </Button>
  )

  return (
    <Box
      data-testid="alert-rules-page-workbench"
      data-contract-scope="system-alert-rule-monitoring-endpoints"
      data-ready-endpoints={ALERT_RULE_READY_ENDPOINTS}
      data-unsupported-actions={ALERT_RULE_UNSUPPORTED_ACTIONS}
      data-row-count={rows.length}
      data-total-count={total}
      data-enabled-count={enabledCount}
      data-critical-count={criticalCount}
      data-warning-count={warningCount}
      data-operation-error={operationError ?? ''}
      data-form-open={String(formOpen)}
      data-delete-id={deleteId ?? ''}
      data-no-local-filter="true"
      data-no-local-rule-fallback="true"
      data-no-legacy-system-alert-rule-endpoints="true"
      data-no-optimistic-status-mutation="true"
      data-row-retained-on-action-error="true"
      sx={{ p: 2, height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="告警规则"
        subtitle="维护落库告警规则，启停、删除和编辑均调用 monitoring/alert-rules 真实接口"
        actions={(
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Grid container spacing={2}>
        {[
          { label: '规则总数', value: total },
          { label: '当前页启用', value: enabledCount, color: 'success.main' },
          { label: '当前页严重', value: criticalCount, color: criticalCount > 0 ? 'error.main' : 'text.primary' },
          { label: '当前页警告', value: warningCount, color: warningCount > 0 ? 'warning.main' : 'text.primary' },
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

      <Alert
        severity="info"
        data-testid="alert-rule-filter-degradation"
        data-contract-source="/monitoring/alert-rules/search"
        data-no-local-filter="true"
        data-no-legacy-system-alert-rule-endpoints="true"
      >
        当前后端 `/monitoring/alert-rules/search` 只消费分页参数，规则名、指标和严重级别筛选尚未落库实现；本页不做本地假筛选。
      </Alert>

      {isError && (
        <Box data-testid="alert-rule-load-error" data-contract-source="/monitoring/alert-rules/search" data-no-local-rule-fallback="true">
          <ErrorAlert
            title="告警规则加载失败"
            message={error instanceof Error ? error.message : '告警规则接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      {operationError && (
        <Box
          data-testid="alert-rule-operation-error"
          data-contract-source={operationError.match(/\/monitoring\/alert-rules\/[a-z-]+/)?.[0] ?? 'unknown'}
          data-input-retained={String(operationError.includes('表单输入已保留'))}
          data-row-retained={String(operationError.includes('已保留') || operationError.includes('原状态已保留'))}
        >
          <ErrorAlert
            title="告警规则操作失败"
            message={operationError}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      <Box
        data-testid="alert-rule-grid-contract"
        data-contract-source="/monitoring/alert-rules/search"
        data-row-count={rows.length}
        data-total-count={total}
        data-no-local-rule-fallback="true"
        data-no-legacy-system-alert-rule-endpoints="true"
        data-no-optimistic-status-mutation="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={total}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          actionSlot={actionSlot} sx={{ flex: 1 }}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>
      <FormDialog open={formOpen} title={form.id ? '编辑告警规则' : '新增告警规则'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack
          data-testid="alert-rule-form-contract"
          data-contract-source={form.id ? '/monitoring/alert-rules/update' : '/monitoring/alert-rules/create'}
          data-rule-id={form.id ?? ''}
          data-rule-name={form.ruleName ?? ''}
          data-metric={form.metric ?? ''}
          data-threshold={form.threshold ?? ''}
          data-input-retained={String(Boolean(operationError?.includes('保存失败')))}
          spacing={2}
          sx={{ pt: 1 }}
        >
          {operationError?.includes('保存失败') && <Alert severity="error">{operationError}</Alert>}
          <TextField label="规则名称" value={form.ruleName} onChange={e => setForm(f => ({ ...f, ruleName: e.target.value }))} fullWidth />
          <TextField label="监控指标" value={form.metric} onChange={e => setForm(f => ({ ...f, metric: e.target.value }))} fullWidth />
          <TextField label="阈值" type="number" value={form.threshold} onChange={e => setForm(f => ({ ...f, threshold: Number(e.target.value) }))} fullWidth />
          <TextField select label="比较符" value={form.operator ?? '>'} onChange={e => setForm(f => ({ ...f, operator: e.target.value }))} fullWidth>
            {['>', '<', '>=', '<=', '=='].map(op => <MenuItem key={op} value={op}>{op}</MenuItem>)}
          </TextField>
          <TextField select label="严重级别" value={form.severity ?? 'warning'} onChange={e => setForm(f => ({ ...f, severity: e.target.value }))} fullWidth>
            {['info', 'warning', 'critical'].map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
          </TextField>
          <FormControlLabel control={<Switch checked={form.status === 1} onChange={e => setForm(f => ({ ...f, status: e.target.checked ? 1 : 0 }))} />} label="启用" />
        </Stack>
      </FormDialog>
      <Box
        data-testid="alert-rule-delete-contract"
        data-contract-source="/monitoring/alert-rules/delete"
        data-open={String(deleteId !== null)}
        data-target-id={deleteId ?? ''}
        data-target-name={deleteTarget?.ruleName ?? ''}
        data-row-retained-on-error="true"
      >
        <ConfirmDialog open={deleteId !== null} content="确定要删除该告警规则吗？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
      </Box>
    </Box>
  )
}
