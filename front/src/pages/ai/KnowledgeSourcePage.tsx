import { useState } from 'react'
import { Box, Button, Chip, Stack, Tooltip, IconButton, TextField, MenuItem } from '@mui/material'
import SyncIcon from '@mui/icons-material/Sync'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import DeleteIcon from '@mui/icons-material/Delete'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

interface KbSource {
  id: number
  sourceName: string
  sourceType: string
  connectionString?: string
  syncStatus: number
  lastSyncTime?: string
  createTime: string
}

const SOURCE_TYPES = [
  { value: 'database', label: '数据库' },
  { value: 'api', label: 'API接口' },
  { value: 'file', label: '文件系统' },
  { value: 'web', label: '网页爬虫' },
]

export default function KnowledgeSourcePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<KbSource>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['kb-sources', page, pageSize],
    queryFn: () => aiApi.kbSourceList({ page, rows: pageSize }),
  })

  const saveMut = useMutation({
    mutationFn: (params: Partial<KbSource>) => aiApi.kbSourceSave(params),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['kb-sources'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const delMut = useMutation({
    mutationFn: (id: number) => aiApi.kbSourceDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['kb-sources'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const syncMut = useMutation({
    mutationFn: (id: number) => aiApi.kbSourceSync(id),
    onSuccess: () => { toast('同步任务已启动', 'success'); qc.invalidateQueries({ queryKey: ['kb-sources'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const testMut = useMutation({
    mutationFn: (id: number) => aiApi.kbSourceTestConnection(id),
    onSuccess: (res) => toast(res.success ? '连接成功' : res.message || '连接失败', res.success ? 'success' : 'error'),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const rows = data?.list ?? []
  const total = data?.total ?? rows.length

  const cols: GridColDef[] = [
    { field: 'sourceName', headerName: '来源名称', flex: 1 },
    { field: 'sourceType', headerName: '来源类型', width: 120,
      renderCell: ({ value }) => {
        const t = SOURCE_TYPES.find(s => s.value === value)
        return <Chip label={t?.label ?? value} size="small" />
      },
    },
    { field: 'syncStatus', headerName: '同步状态', width: 120,
      renderCell: ({ value }) => value === 1
        ? <Chip icon={<CheckCircleIcon />} label="正常" size="small" color="success" />
        : <Chip icon={<ErrorIcon />} label="异常" size="small" color="error" />,
    },
    { field: 'lastSyncTime', headerName: '上次同步', width: 160, valueFormatter: ({ value }) => value ? formatDate(value) : '—' },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: ({ value }) => formatDate(value) },
    { field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => {
        const r = row as KbSource
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="同步"><IconButton size="small" onClick={() => syncMut.mutate(r.id)}><SyncIcon fontSize="small" /></IconButton></Tooltip>
            <Button size="small" variant="outlined" onClick={() => testMut.mutate(r.id)}>测试</Button>
            <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteId(r.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
          </Stack>
        )
      },
    },
  ]

  return (
    <Box>
      <PageHeader title="知识来源管理" breadcrumbs={[{ label: 'AI中心' }, { label: '知识来源' }]} />
      <StandardDataGrid
        rows={rows} columns={cols} loading={isFetching}
        rowCount={total} paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as KbSource).id}
      />
      <FormDialog
        open={formOpen} title={form.id ? '编辑来源' : '新增来源'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)}
        loading={saveMut.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="来源名称" required fullWidth value={form.sourceName ?? ''} onChange={e => setForm({ ...form, sourceName: e.target.value })} />
          <TextField label="来源类型" required fullWidth select value={form.sourceType ?? ''} onChange={e => setForm({ ...form, sourceType: e.target.value })}>
            {SOURCE_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
          </TextField>
          <TextField label="连接字符串" fullWidth multiline rows={3} value={form.connectionString ?? ''} onChange={e => setForm({ ...form, connectionString: e.target.value })} />
        </Stack>
      </FormDialog>
      <ConfirmDialog
        open={deleteId !== null} title="确认删除" content="确认删除此知识来源？"
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        onClose={() => setDeleteId(null)}
      />
    </Box>
  )
}

