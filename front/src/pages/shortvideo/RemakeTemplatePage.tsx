import { useState } from 'react'
import { Box, TextField, Button, MenuItem, Select, FormControl, InputLabel } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import type { RemakeTemplate } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { Dialog, DialogTitle, DialogContent, DialogActions, Grid, TextField as MuiTextField } from '@mui/material'

const TEMPLATE_TYPES = ['口播', '剧情', '开箱', '测评', '教程']

const DEFAULT_FORM: Partial<RemakeTemplate> = { templateName: '', templateType: '', content: '' }

export default function RemakeTemplatePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, templateName: '', templateType: '' })
  const [query, setQuery] = useState(search)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<Partial<RemakeTemplate>>(DEFAULT_FORM)
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['sv-remake-templates', search],
    queryFn: () => shortvideoApi.remakeTemplateList({
      ...search,
      templateName: search.templateName || undefined,
      templateType: search.templateType || undefined,
    }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<RemakeTemplate>) => shortvideoApi.remakeTemplateSave(p),
    onSuccess: () => { toast('保存成功', 'success'); setDialogOpen(false); qc.invalidateQueries({ queryKey: ['sv-remake-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: shortvideoApi.remakeTemplateDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['sv-remake-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = () => { setForm(DEFAULT_FORM); setDialogOpen(true) }
  const openEdit = (row: RemakeTemplate) => { setForm(row); setDialogOpen(true) }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 160 },
    { field: 'templateType', headerName: '类型', width: 120 },
    { field: 'content', headerName: '内容摘要', flex: 2, minWidth: 200, valueFormatter: (v: string) => v?.slice(0, 80) },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 150, sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>
        </Box>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField label="模板名称" size="small" value={query.templateName} onChange={e => setQuery(q => ({ ...q, templateName: e.target.value }))} sx={{ width: 160 }} />
      <FormControl size="small" sx={{ minWidth: 120 }}>
        <InputLabel>类型</InputLabel>
        <Select label="类型" value={query.templateType} onChange={e => setQuery(q => ({ ...q, templateType: e.target.value }))}>
          <MenuItem value="">全部</MenuItem>
          {TEMPLATE_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
        </Select>
      </FormControl>
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { const d = { page: 0, rows: 20, templateName: '', templateType: '' }; setQuery(d); setSearch(d) }}>重置</Button>
    </>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
        actionSlot={<Button variant="contained" onClick={openAdd}>新建模板</Button>}
        sx={{ flex: 1 }}
      />

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{form.id ? '编辑模板' : '新建模板'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={8}>
              <MuiTextField label="模板名称" fullWidth value={form.templateName ?? ''} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth>
                <InputLabel>类型</InputLabel>
                <Select label="类型" value={form.templateType ?? ''} onChange={e => setForm(f => ({ ...f, templateType: e.target.value }))}>
                  {TEMPLATE_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <MuiTextField label="模板内容" fullWidth multiline minRows={6} value={form.content ?? ''} onChange={e => setForm(f => ({ ...f, content: e.target.value }))} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" disabled={!form.templateName || saveMut.isPending} onClick={() => saveMut.mutate(form)}>保存</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该模板吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending} />
    </Box>
  )
}
