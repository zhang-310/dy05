import { useState, useCallback } from 'react'
import { Box, TextField, Stack, Button, MenuItem, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { scriptApi, type ScriptTemplate, type ScriptTemplateQuery } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const SCENES = ['开场', '产品介绍', '促销', '互动', '结尾', '其他']

const defaultForm = { templateName: '', templateContent: '', scene: '', industry: '', tags: '', status: 1 }

export default function ScriptTemplatePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState<ScriptTemplateQuery>({ page: 0, rows: 20 })
  const [draft, setDraft] = useState<ScriptTemplateQuery>({ page: 0, rows: 20 })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(defaultForm)
  const [editId, setEditId] = useState<number | undefined>()
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['script-templates', search],
    queryFn: () => scriptApi.templateSearch(search),
  })

  const saveMut = useMutation({
    mutationFn: (p: typeof defaultForm & { id?: number }) => scriptApi.templateSave(p),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['script-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: scriptApi.templateDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['script-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setEditId(undefined); setFormOpen(true) }, [])
  const openEdit = useCallback((row: ScriptTemplate) => {
    setForm({ templateName: row.templateName, templateContent: row.templateContent, scene: row.scene, industry: row.industry, tags: row.tags, status: row.status })
    setEditId(row.id); setFormOpen(true)
  }, [])
  const handleSearch = useCallback(() => setSearch({ ...draft, page: 0 }), [draft])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1 },
    { field: 'scene', headerName: '场景', width: 100 },
    { field: 'industry', headerName: '行业', width: 100 },
    { field: 'useCount', headerName: '使用次数', width: 90 },
    { field: 'createTime', headerName: '创建时间', width: 160, renderCell: ({ value }) => formatDate(value) },
    {
      field: '_actions', headerName: '操作', width: 120,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Stack direction="row" spacing={1}>
          <Box component="span" sx={{ color: 'primary.main', cursor: 'pointer', fontSize: 13 }} onClick={() => openEdit(row as ScriptTemplate)}>编辑</Box>
          <Box component="span" sx={{ color: 'error.main', cursor: 'pointer', fontSize: 13 }} onClick={() => setDeleteId(row.id)}>删除</Box>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField size="small" label="模板名称" value={draft.keyword ?? ''}
        onChange={e => setDraft(d => ({ ...d, keyword: e.target.value }))} sx={{ width: 160 }} />
      <TextField select size="small" label="场景" value={draft.scene ?? ''}
        onChange={e => setDraft(d => ({ ...d, scene: e.target.value }))} sx={{ width: 120 }}>
        <MenuItem value="">全部</MenuItem>
        {SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
      </TextField>
      <Box component="button" onClick={handleSearch}
        sx={{ px: 2, py: 0.5, bgcolor: 'primary.main', color: 'white', border: 'none', borderRadius: 1, cursor: 'pointer' }}>查询</Box>
    </Stack>
  )
  const actionSlot = (
    <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增模板</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>话术模板</Typography>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={editId ? '编辑话术模板' : '新增话术模板'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(editId ? { ...form, id: editId } : form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="模板名称" value={form.templateName} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} fullWidth />
          <TextField select label="场景" value={form.scene} onChange={e => setForm(f => ({ ...f, scene: e.target.value }))} fullWidth>
            {SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
          </TextField>
          <TextField label="行业" value={form.industry} onChange={e => setForm(f => ({ ...f, industry: e.target.value }))} fullWidth />
          <TextField label="标签" value={form.tags} onChange={e => setForm(f => ({ ...f, tags: e.target.value }))} fullWidth />
          <TextField label="模板内容" value={form.templateContent} onChange={e => setForm(f => ({ ...f, templateContent: e.target.value }))} fullWidth multiline minRows={6} />
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该话术模板吗？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}
