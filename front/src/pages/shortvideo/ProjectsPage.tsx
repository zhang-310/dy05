import { useState, useCallback } from 'react'
import { Box, TextField, Button, Chip, Stack, MenuItem, IconButton, Tooltip, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { shortvideoApi, type SvProject, type SvProjectSave } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { formatDate } from '@/utils/date'

/** 与实体 status 字段一致：draft / processing / completed / failed */
const STATUS_MAP: Record<string, { label: string; color: 'warning' | 'success' | 'default' | 'error' }> = {
  draft: { label: '草稿', color: 'default' },
  processing: { label: '进行中', color: 'warning' },
  completed: { label: '已完成', color: 'success' },
  failed: { label: '失败', color: 'error' },
}

const PROJECT_TYPE_MAP: Record<string, string> = {
  viral_clone: '爆款复刻',
  daily: '日更',
  soft_ad: '软广',
}

export default function ProjectsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [search, setSearch] = useState({ page: 0, rows: 20, title: '', status: undefined as string | undefined })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SvProject>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({ queryKey: ['sv-projects', search], queryFn: () => shortvideoApi.list(search) })
  const saveMut = useMutation({
    mutationFn: (body: SvProjectSave) => shortvideoApi.save(body),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['sv-projects'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({ mutationFn: shortvideoApi.delete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['sv-projects'] }) }, onError: (e: Error) => toast(e.message, 'error') })

  const openAdd = useCallback(() => {
    setForm({ projectType: 'viral_clone', status: 'draft', title: '' })
    setFormOpen(true)
  }, [])
  const openEdit = useCallback((row: SvProject) => { setForm({ ...row }); setFormOpen(true) }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])

  const handleConfirmSave = useCallback(() => {
    const title = String(form.title ?? '').trim()
    const projectType = String(form.projectType ?? '').trim()
    if (!title) { toast('请填写项目标题', 'warning'); return }
    if (!projectType) { toast('请选择项目类型', 'warning'); return }
    const body: SvProjectSave = {
      id: form.id,
      title,
      projectType,
      status: form.status?.trim() || 'draft',
      accountId: form.accountId,
      scriptId: form.scriptId,
      shotListId: form.shotListId,
      publishTitle: form.publishTitle?.trim() || undefined,
      personaId: form.personaId,
      scheduleDate: form.scheduleDate,
      shootStatus: form.shootStatus,
    }
    saveMut.mutate(body)
  }, [form, saveMut, toast])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '项目标题', flex: 1, minWidth: 160 },
    {
      field: 'projectType', headerName: '类型', width: 100,
      valueFormatter: (v: string) => PROJECT_TYPE_MAP[v] ?? v,
    },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const key = String(value ?? 'draft')
        const s = STATUS_MAP[key] ?? { label: key, color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: (p: GridRenderCellParams<SvProject>) => {
        const row = p.row
        return (
          <Stack direction="row" gap={0.5} alignItems="center">
            <Tooltip title="进入工作台">
              <IconButton size="small" color="primary" onClick={() => navigate(`${shortvideoRoutes.workbench}?projectId=${row.id}`)}>
                <OpenInNewIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
            <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>
          </Stack>
        )
      },
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
      <TextField size="small" label="标题" value={query.title}
        onChange={e => setQuery(q => ({ ...q, title: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 180 }} />
      <TextField select size="small" label="状态" value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : e.target.value }))}
        sx={{ width: 120 }}>
        <MenuItem value="">全部</MenuItem>
        {Object.entries(STATUS_MAP).map(([v, s]) => <MenuItem key={v} value={v}>{s.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </Stack>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建项目</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>短视频项目</Typography>
      <StandardDataGrid rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching} paginationMode="server" paginationModel={{ page: search.page, pageSize: search.rows }} onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))} searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }} />
      <FormDialog open={formOpen} title={form.id ? '编辑项目' : '新建项目'} onClose={() => setFormOpen(false)} onConfirm={handleConfirmSave} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="项目标题" required value={form.title ?? ''} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} fullWidth size="small" />
          <TextField select label="项目类型" required value={form.projectType ?? 'viral_clone'} onChange={e => setForm(f => ({ ...f, projectType: e.target.value }))} size="small" fullWidth>
            {Object.entries(PROJECT_TYPE_MAP).map(([v, label]) => <MenuItem key={v} value={v}>{label}</MenuItem>)}
          </TextField>
          <TextField select label="状态" value={form.status ?? 'draft'} onChange={e => setForm(f => ({ ...f, status: e.target.value }))} size="small" fullWidth>
            {Object.entries(STATUS_MAP).map(([v, s]) => <MenuItem key={v} value={v}>{s.label}</MenuItem>)}
          </TextField>
          <TextField label="发布标题 / 备注" value={form.publishTitle ?? ''} onChange={e => setForm(f => ({ ...f, publishTitle: e.target.value }))} fullWidth size="small" multiline minRows={2} helperText="对应后端 publishTitle，可选" />
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该项目吗？" onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}
