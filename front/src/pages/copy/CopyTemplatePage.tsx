import { useState, useCallback } from 'react'
import {
  Box, TextField, Button, Stack, Chip, MenuItem, Select,
  Drawer, Typography, Divider, IconButton, Tooltip, Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import PreviewIcon from '@mui/icons-material/Visibility'
import type { GridColDef, GridRowSelectionModel } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { copyApi, type CopyTemplate } from '@/api/copy'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const CATEGORY_OPTIONS = ['开场白', '产品介绍', '促销话术', '互动话术', '结束语', '其他']
const ALL_CATEGORIES = ['全部', ...CATEGORY_OPTIONS]

/** 渲染模板变量占位符 {{var}} 为高亮 span */
function renderTemplateContent(content: string) {
  const parts = content.split(/(\{\{[^}]+\}\})/g)
  return parts.map((part, i) =>
    /^\{\{[^}]+\}\}$/.test(part)
      ? <Box key={i} component="span" sx={{ bgcolor: 'primary.light', color: 'primary.contrastText', px: 0.5, borderRadius: 0.5, fontSize: 13 }}>{part}</Box>
      : <span key={i}>{part}</span>
  )
}

const defaultForm: Partial<CopyTemplate> = { templateName: '', content: '', category: '' }

export default function CopyTemplatePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, templateName: '', category: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<CopyTemplate>>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [batchDeleteIds, setBatchDeleteIds] = useState<number[]>([])
  const [selection, setSelection] = useState<GridRowSelectionModel>([])
  const [preview, setPreview] = useState<CopyTemplate | null>(null)
  const [categoryFilter, setCategoryFilter] = useState('全部')

  const { data, isFetching } = useQuery({
    queryKey: ['copy-templates', search, categoryFilter],
    queryFn: () => copyApi.templateList({
      page: search.page,
      rows: search.rows,
      templateName: search.templateName || undefined,
      category: categoryFilter !== '全部' ? categoryFilter : undefined,
    }),
  })

  const saveMut = useMutation({
    mutationFn: copyApi.templateSave,
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['copy-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: copyApi.templateDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['copy-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setFormOpen(true) }, [])
  const openEdit = useCallback((row: CopyTemplate) => { setForm(row); setFormOpen(true) }, [])
  const handleSearch = useCallback(() => setSearch(q => ({ ...q, page: 0 })), [])
  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 160,
      renderCell: ({ row, value }) => (
        <Button size="small" variant="text" sx={{ textAlign: 'left', justifyContent: 'flex-start' }}
          onClick={() => setPreview(row as CopyTemplate)}>{String(value)}</Button>
      ) },
    { field: 'category', headerName: '分类', width: 110,
      renderCell: ({ value }) => value ? <Chip label={String(value)} size="small" variant="outlined" /> : null },
    { field: 'variables', headerName: '变量', width: 160,
      renderCell: ({ value }) => (
        <Stack direction="row" gap={0.5} flexWrap="wrap">
          {String(value ?? '').split(',').filter(Boolean).map(v => (
            <Chip key={v} label={`{{${v.trim()}}}`} size="small" color="primary" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: 11 }} />
          ))}
        </Stack>
      ) },
    { field: 'createTime', headerName: '创建时间', width: 155, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 220, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={0.5}>
          <Tooltip title="预览"><IconButton size="small" onClick={() => setPreview(row as CopyTemplate)}><PreviewIcon fontSize="small" /></IconButton></Tooltip>
          <Button size="small" onClick={() => openEdit(row as CopyTemplate)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as CopyTemplate).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Select size="small" value={categoryFilter} onChange={e => { setCategoryFilter(e.target.value); setSearch(s => ({ ...s, page: 0 })) }} sx={{ minWidth: 110 }}>
        {ALL_CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
      </Select>
      <TextField size="small" placeholder="模板名称" value={query.templateName}
        onChange={e => setQuery(q => ({ ...q, templateName: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 180 }} />
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </Stack>
  )

  const actionSlot = (
    <Stack direction="row" spacing={1}>
      {selection.length > 0 && (
        <Button size="small" color="error" variant="outlined"
          onClick={() => setBatchDeleteIds(selection as number[])}>批量删除({selection.length})</Button>
      )}
      <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openAdd}>新增模板</Button>
    </Stack>
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 2 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>文案模板</Typography>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        checkboxSelection onRowSelectionModelChange={m => setSelection(m)}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑模板' : '新增模板'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="模板名称" required value={form.templateName ?? ''} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} fullWidth size="small" />
          <Select size="small" value={form.category ?? ''} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} displayEmpty fullWidth>
            <MenuItem value="">选择分类</MenuItem>
            {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </Select>
          <TextField label="变量（逗号分隔，如 productName,price）" value={form.variables ?? ''} onChange={e => setForm(f => ({ ...f, variables: e.target.value }))} fullWidth size="small" />
          <TextField label="模板内容（用 {{变量名}} 插入变量）" value={form.content ?? ''} onChange={e => setForm(f => ({ ...f, content: e.target.value }))} fullWidth multiline minRows={5} />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该模板吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />

      <ConfirmDialog open={batchDeleteIds.length > 0}
        content={`确定要删除选中的 ${batchDeleteIds.length} 个模板吗？`}
        onClose={() => setBatchDeleteIds([])}
        onConfirm={async () => {
          for (const id of batchDeleteIds) { await copyApi.templateDelete(id) }
          toast(`已删除 ${batchDeleteIds.length} 个`, 'success')
          setBatchDeleteIds([]); setSelection([])
          qc.invalidateQueries({ queryKey: ['copy-templates'] })
        }}
        loading={false} />

      {/* 模板预览抽屉 */}
      <Drawer anchor="right" open={preview !== null} onClose={() => setPreview(null)}
        PaperProps={{ sx: { width: 560, p: 3 } }}>
        {preview && (
          <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">{preview.templateName}</Typography>
              <IconButton onClick={() => setPreview(null)}><CloseIcon /></IconButton>
            </Stack>
            <Stack direction="row" spacing={1} flexWrap="wrap">
              {preview.category && <Chip label={preview.category} size="small" />}
              {String(preview.variables ?? '').split(',').filter(Boolean).map(v => (
                <Chip key={v} label={`{{${v.trim()}}}`} size="small" color="primary" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: 11 }} />
              ))}
            </Stack>
            <Divider />
            <Typography variant="caption" color="text.secondary">创建时间：{formatDate(preview.createTime)}</Typography>
            <Paper variant="outlined" sx={{ p: 2, lineHeight: 1.9, fontSize: 14, whiteSpace: 'pre-wrap' }}>
              {renderTemplateContent(preview.content ?? '')}
            </Paper>
            <Stack direction="row" spacing={1}>
              <Tooltip title="复制内容">
                <Button startIcon={<ContentCopyIcon />} size="small" variant="outlined"
                  onClick={() => { navigator.clipboard.writeText(preview.content ?? ''); toast('已复制', 'success') }}>
                  复制
                </Button>
              </Tooltip>
              <Button size="small" onClick={() => { openEdit(preview); setPreview(null) }}>编辑</Button>
              <Button size="small" color="error" onClick={() => { setDeleteId(preview.id); setPreview(null) }}>删除</Button>
            </Stack>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}


