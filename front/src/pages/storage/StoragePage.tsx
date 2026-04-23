import { useState, useRef } from 'react'
import { Box, TextField, Button, MenuItem, Select, FormControl, InputLabel, Chip, Tooltip, IconButton } from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { storageApi } from '@/api/storage'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const FILE_TYPES = ['image', 'video', 'audio', 'document', 'other']

function fmtSize(bytes: number) {
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

export default function StoragePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [search, setSearch] = useState({ page: 0, rows: 20, fileName: '', fileType: '' })
  const [query, setQuery] = useState(search)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const { data, isFetching } = useQuery({
    queryKey: ['files', search],
    queryFn: () => storageApi.list({ ...search, fileType: search.fileType || undefined, fileName: search.fileName || undefined }),
  })

  const uploadMut = useMutation({
    mutationFn: (file: File) => storageApi.upload(file),
    onSuccess: () => { toast('上传成功', 'success'); qc.invalidateQueries({ queryKey: ['files'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: storageApi.delete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['files'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) uploadMut.mutate(file)
    e.target.value = ''
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'fileName', headerName: '文件名', flex: 1, minWidth: 180 },
    { field: 'fileType', headerName: '类型', width: 100, renderCell: ({ value }) => <Chip label={value} size="small" /> },
    { field: 'fileSize', headerName: '大小', width: 100, valueFormatter: (v: number) => fmtSize(v ?? 0) },
    {
      field: 'fileUrl', headerName: '预览/链接', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Tooltip title={row.fileUrl}>
          <IconButton size="small" onClick={() => window.open(row.fileUrl, '_blank')}><OpenInNewIcon fontSize="small" /></IconButton>
        </Tooltip>
      ),
    },
    { field: 'createTime', headerName: '上传时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 90, sortable: false,
      renderCell: ({ row }) => <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>,
    },
  ]

  const searchSlot = (
    <>
      <TextField label="文件名" size="small" value={query.fileName} onChange={e => setQuery(q => ({ ...q, fileName: e.target.value }))} sx={{ width: 160 }} />
      <FormControl size="small" sx={{ minWidth: 110 }}>
        <InputLabel>类型</InputLabel>
        <Select label="类型" value={query.fileType} onChange={e => setQuery(q => ({ ...q, fileType: e.target.value }))}>
          <MenuItem value="">全部</MenuItem>
          {FILE_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
        </Select>
      </FormControl>
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
      <Button onClick={() => { setQuery({ page: 0, rows: 20, fileName: '', fileType: '' }); setSearch({ page: 0, rows: 20, fileName: '', fileType: '' }) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <>
      <input ref={fileInputRef} type="file" hidden onChange={handleFileChange} />
      <Button variant="contained" startIcon={<UploadFileIcon />} onClick={() => fileInputRef.current?.click()} disabled={uploadMut.isPending}>
        上传文件
      </Button>
    </>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        loading={isFetching}
        rowCount={data?.total ?? 0}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
        actionSlot={actionSlot}
      />
      <ConfirmDialog
        open={deleteId !== null}
        title="确认删除"
        content="删除后文件将从存储中移除，无法恢复。确认删除？"
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        onClose={() => setDeleteId(null)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}


