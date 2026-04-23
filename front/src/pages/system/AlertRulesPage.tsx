import { useState, useCallback } from 'react'
import { Box, Stack, Button, Switch, FormControlLabel, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { systemApi, type AlertRule, type AlertRuleSave } from '@/api/system'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { TextField, MenuItem } from '@mui/material'

const defaultForm: AlertRuleSave = { ruleName: '', metric: '', threshold: 0, operator: '>', severity: 'warning', status: 1 }

export default function AlertRulesPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20 })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<AlertRuleSave>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['alert-rules', search],
    queryFn: () => systemApi.alertRuleSearch(search as unknown as Record<string, unknown>),
  })

  const saveMut = useMutation({
    mutationFn: async (p: Partial<AlertRuleSave>): Promise<void> => { await (p.id ? systemApi.alertRuleUpdate(p) : systemApi.alertRuleCreate(p)) },
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['alert-rules'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: systemApi.alertRuleDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['alert-rules'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const toggleMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) =>
      status === 1 ? systemApi.alertRuleEnable(id) : systemApi.alertRuleDisable(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['alert-rules'] }),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AlertRule) => {
    setForm({ id: row.id, ruleName: row.ruleName, metric: row.metric, threshold: row.threshold, operator: row.operator, severity: row.severity, status: row.status })
    setFormOpen(true)
  }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'ruleName', headerName: '规则名称', flex: 1 },
    { field: 'metric', headerName: '指标', width: 150 },
    { field: 'threshold', headerName: '阈值', width: 90 },
    { field: 'severity', headerName: '严重级别', width: 100 },
    {
      field: 'status', headerName: '启用', width: 90,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Switch size="small" checked={row.status === 1}
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
          <Box component="span" sx={{ color: 'primary.main', cursor: 'pointer', fontSize: 13 }} onClick={() => openEdit(row as AlertRule)}>编辑</Box>
          <Box component="span" sx={{ color: 'error.main', cursor: 'pointer', fontSize: 13 }} onClick={() => setDeleteId(row.id)}>删除</Box>
        </Stack>
      ),
    },
  ]

  const actionSlot = (
    <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增规则</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>告警规则</Typography>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        actionSlot={actionSlot} sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={form.id ? '编辑告警规则' : '新增告警规则'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
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
      <ConfirmDialog open={deleteId !== null} content="确定要删除该告警规则吗？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}
