import { useState, useCallback, useRef } from 'react'
import { Box, TextField, Button, Chip, Stack, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import UploadIcon from '@mui/icons-material/Upload'
import DownloadIcon from '@mui/icons-material/Download'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { scriptApi, type ViolationWord, type ViolationWordSave } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const SEV = [
  { value: 1, label: '低', color: 'info' as const },
  { value: 2, label: '中', color: 'warning' as const },
  { value: 3, label: '高', color: 'error' as const },
]

const defaultForm: Partial<ViolationWordSave> = { word: '', category: '', severity: 2, replacement: '', status: 1 }

export default function ViolationWordPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, word: '', category: '', severity: undefined as number | undefined })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ViolationWordSave>>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['violation-words', search],
    queryFn: () => scriptApi.violationList(search),
  })
  const saveMut = useMutation({
    mutationFn: scriptApi.violationSave,
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['violation-words'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: scriptApi.violationDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['violation-words'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const [importOpen, setImportOpen] = useState(false)
  const [importText, setImportText] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)

  const handleExport = useCallback(() => {
    const allRows = data?.list ?? []
    const csv = ['违规词,分类,严重程度,替换词', ...allRows.map(r =>
      [r.word, r.category ?? '', r.severity ?? 2, r.replacement ?? ''].join(',')
    )].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = 'violation-words.csv'; a.click()
    URL.revokeObjectURL(url)
  }, [data])

  const handleImportFile = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = ev => setImportText(String(ev.target?.result ?? ''))
    reader.readAsText(file, 'utf-8')
    setImportOpen(true)
    e.target.value = ''
  }, [])

  const handleImportConfirm = useCallback(async () => {
    const lines = importText.split('\n').filter(l => l.trim() && !l.startsWith('违规词'))
    let ok = 0
    for (const line of lines) {
      const [word, category, severity, replacement] = line.split(',')
      if (!word?.trim()) continue
      try {
        await saveMut.mutateAsync({ word: word.trim(), category: category?.trim() ?? '', severity: Number(severity) || 2, replacement: replacement?.trim() ?? '', status: 1 })
        ok++
      } catch { /* skip */ }
    }
    toast(`导入完成，成功 ${ok} 条`, 'success')
    setImportOpen(false); setImportText('')
    qc.invalidateQueries({ queryKey: ['violation-words'] })
  }, [importText, saveMut, toast, qc])

  const openAdd = useCallback(() => { setForm(defaultForm); setFormOpen(true) }, [])
  const openEdit = useCallback((row: ViolationWord) => {
    setForm({ id: row.id, word: row.word, category: row.category, severity: row.severity, replacement: row.replacement, status: row.status })
    setFormOpen(true)
  }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])
  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'word', headerName: '违规词', flex: 1, minWidth: 120 },
    { field: 'category', headerName: '分类', width: 120 },
    {
      field: 'severity', headerName: '严重程度', width: 100,
      renderCell: ({ value }) => {
        const opt = SEV.find(o => o.value === value)
        return <Chip label={opt?.label ?? String(value)} color={opt?.color ?? 'default'} size="small" />
      },
    },
    { field: 'replacement', headerName: '替换词', flex: 1, minWidth: 120 },
    {
      field: 'status', headerName: '状态', width: 80,
      renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 140, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={1}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
      <TextField size="small" label="违规词" value={query.word}
        onChange={e => setQuery(q => ({ ...q, word: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 150 }} />
      <TextField size="small" label="分类" value={query.category}
        onChange={e => setQuery(q => ({ ...q, category: e.target.value }))} sx={{ width: 120 }} />
      <TextField select size="small" label="严重程度" value={query.severity ?? ''}
        onChange={e => setQuery(q => ({ ...q, severity: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 110 }}>
        <MenuItem value="">全部</MenuItem>
        {SEV.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </Stack>
  )
  const actionSlot = (
    <Stack direction="row" spacing={1}>
      <input ref={fileRef} type="file" accept=".csv,.txt" style={{ display: 'none' }} onChange={handleImportFile} />
      <Button variant="outlined" size="small" startIcon={<UploadIcon />} onClick={() => fileRef.current?.click()}>导入CSV</Button>
      <Button variant="outlined" size="small" startIcon={<DownloadIcon />} onClick={handleExport}>导出</Button>
      <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增违规词</Button>
    </Stack>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={form.id ? '编辑违规词' : '新增违规词'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="违规词" required value={form.word ?? ''} onChange={e => setForm(f => ({ ...f, word: e.target.value }))} fullWidth size="small" />
          <TextField label="分类" value={form.category ?? ''} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} fullWidth size="small" />
          <TextField select label="严重程度" value={form.severity ?? 2} onChange={e => setForm(f => ({ ...f, severity: Number(e.target.value) }))} size="small" fullWidth>
            {SEV.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </TextField>
          <TextField label="替换词" value={form.replacement ?? ''} onChange={e => setForm(f => ({ ...f, replacement: e.target.value }))} fullWidth size="small" />
          <TextField select label="状态" value={form.status ?? 1} onChange={e => setForm(f => ({ ...f, status: Number(e.target.value) }))} size="small" fullWidth>
            <MenuItem value={1}>启用</MenuItem>
            <MenuItem value={0}>禁用</MenuItem>
          </TextField>
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该违规词吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
      <Dialog open={importOpen} onClose={() => setImportOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>批量导入违规词</DialogTitle>
        <DialogContent>
          <Box component="p" sx={{ mt: 0, mb: 1, fontSize: 13, color: 'text.secondary' }}>格式：每行一条，逗号分隔：违规词,分类,严重程度(1/2/3),替换词</Box>
          <TextField multiline minRows={8} fullWidth size="small" value={importText}
            onChange={e => setImportText(e.target.value)}
            placeholder="skincare黑名单词,敏感词,3,&#10;另一个词,一般违规,2,替换词" />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleImportConfirm} disabled={!importText.trim()}>确认导入</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

