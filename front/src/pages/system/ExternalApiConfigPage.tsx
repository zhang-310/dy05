import { useState, useCallback } from 'react'
import {
  Box, TextField, Button, Stack, Chip, Switch,
  Drawer, Typography, Divider, IconButton,
  MenuItem, Select,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { systemApi, type ExternalApiConfig } from '@/api/system'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const CATEGORY_CONFIG: Record<string, { label: string; color: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'default' | 'info' }> = {
  llm: { label: 'LLM', color: 'primary' },
  video: { label: '视频', color: 'secondary' },
  tts: { label: 'TTS', color: 'success' },
  trend: { label: '热点', color: 'warning' },
  audit: { label: '审核', color: 'error' },
  storage: { label: '存储', color: 'default' },
}

const CATEGORY_OPTIONS = ['llm', 'video', 'tts', 'trend', 'audit', 'storage']
const AUTH_TYPES = ['api_key', 'bearer', 'basic', 'none']

const defaultForm: Partial<ExternalApiConfig> = {
  apiName: '', category: 'llm', baseUrl: '', authType: 'api_key', status: 1,
}

export default function ExternalApiConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, category: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ExternalApiConfig>>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [detail, setDetail] = useState<ExternalApiConfig | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['external-api-configs', search],
    queryFn: () => systemApi.externalApiList({
      page: search.page, rows: search.rows,
      category: search.category || undefined,
    }),
  })

  const saveMut = useMutation({
    mutationFn: systemApi.externalApiSave,
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: systemApi.externalApiDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['external-api-configs'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const toggleMut = useMutation({
    mutationFn: (row: ExternalApiConfig) => systemApi.externalApiSave({ ...row, status: row.status === 1 ? 0 : 1 }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['external-api-configs'] }),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setFormOpen(true) }, [])
  const openEdit = useCallback((row: ExternalApiConfig) => { setForm(row); setFormOpen(true) }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'apiName', headerName: 'API名称', flex: 1, minWidth: 140,
      renderCell: ({ row, value }) => (
        <Button size="small" variant="text" sx={{ justifyContent: 'flex-start' }}
          onClick={() => setDetail(row as ExternalApiConfig)}>{String(value)}</Button>
      ) },
    { field: 'category', headerName: '分类', width: 90,
      renderCell: ({ value }) => {
        const c = CATEGORY_CONFIG[value as string] ?? { label: String(value), color: 'default' as const }
        return <Chip label={c.label} color={c.color} size="small" />
      } },
    { field: 'baseUrl', headerName: 'Base URL', flex: 1.5, minWidth: 180 },
    { field: 'authType', headerName: '认证方式', width: 100 },
    { field: 'status', headerName: '启用', width: 80,
      renderCell: ({ row }) => (
        <Switch size="small" checked={(row as ExternalApiConfig).status === 1}
          onClick={e => e.stopPropagation()}
          onChange={() => toggleMut.mutate(row as ExternalApiConfig)} />
      ) },
    { field: 'createTime', headerName: '创建时间', width: 150, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={0.5}>
          <Button size="small" onClick={() => openEdit(row as ExternalApiConfig)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as ExternalApiConfig).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Select size="small" value={query.category} displayEmpty
        onChange={e => { setQuery(q => ({ ...q, category: e.target.value })); setSearch(s => ({ ...s, category: e.target.value, page: 0 })) }}
        sx={{ minWidth: 110 }}>
        <MenuItem value="">全部分类</MenuItem>
        {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{CATEGORY_CONFIG[c]?.label ?? c}</MenuItem>)}
      </Select>
      <Button variant="contained" size="small" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
    </Stack>
  )

  const actionSlot = (
    <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openAdd}>新增配置</Button>
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 2 }}>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑外部API配置' : '新增外部API配置'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="API名称" required value={form.apiName ?? ''} onChange={e => setForm(f => ({ ...f, apiName: e.target.value }))} fullWidth size="small" />
          <Select size="small" value={form.category ?? 'llm'} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} displayEmpty fullWidth>
            {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{CATEGORY_CONFIG[c]?.label ?? c}</MenuItem>)}
          </Select>
          <TextField label="Base URL" required value={form.baseUrl ?? ''} onChange={e => setForm(f => ({ ...f, baseUrl: e.target.value }))} fullWidth size="small" />
          <Select size="small" value={form.authType ?? 'api_key'} onChange={e => setForm(f => ({ ...f, authType: e.target.value }))} fullWidth>
            {AUTH_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
          </Select>
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该API配置吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />

      <Drawer anchor="right" open={detail !== null} onClose={() => setDetail(null)}
        PaperProps={{ sx: { width: 480, p: 3 } }}>
        {detail && (
          <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">{detail.apiName}</Typography>
              <IconButton onClick={() => setDetail(null)}><CloseIcon /></IconButton>
            </Stack>
            <Divider />
            <Box><Typography variant="caption" color="text.secondary">分类</Typography>
              <Typography>{CATEGORY_CONFIG[detail.category]?.label ?? detail.category}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">Base URL</Typography>
              <Typography sx={{ wordBreak: 'break-all' }}>{detail.baseUrl}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">认证方式</Typography>
              <Typography>{detail.authType}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">状态</Typography>
              <Chip label={detail.status === 1 ? '已启用' : '已禁用'} color={detail.status === 1 ? 'success' : 'default'} size="small" /></Box>
            <Box><Typography variant="caption" color="text.secondary">创建时间</Typography>
              <Typography>{formatDate(detail.createTime)}</Typography></Box>
            <Stack direction="row" spacing={1}>
              <Button size="small" variant="outlined" onClick={() => { openEdit(detail); setDetail(null) }}>编辑</Button>
            </Stack>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
