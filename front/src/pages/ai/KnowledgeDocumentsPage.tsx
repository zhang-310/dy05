import { useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box, Typography, Button, Stack, Chip, TextField,
  Select, MenuItem, IconButton, Drawer, Divider,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import SearchIcon from '@mui/icons-material/Search'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { aiApi, type KbDocument } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  0: { label: '处理中', color: 'warning' },
  1: { label: '已索引', color: 'success' },
  2: { label: '失败', color: 'error' },
}

const SOURCE_TYPES = ['全部', 'file', 'url', 'text', 'api']

export default function KnowledgeDocumentsPage() {
  const { kbId } = useParams<{ kbId: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const kbIdNum = Number(kbId)

  const [search, setSearch] = useState({ page: 0, rows: 20, keyword: '', sourceType: '' })
  const [keyword, setKeyword] = useState('')
  const [sourceType, setSourceType] = useState('全部')
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [detail, setDetail] = useState<KbDocument | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['kb-docs', kbIdNum, search],
    queryFn: () => aiApi.docList(kbIdNum, {
      page: search.page,
      rows: search.rows,
      keyword: search.keyword || undefined,
      sourceType: search.sourceType || undefined,
    }),
    enabled: !!kbIdNum,
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const delMut = useMutation({
    mutationFn: aiApi.docDelete,
    onSuccess: () => { toast('已删除', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['kb-docs', kbIdNum] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const handleImport = useCallback(() => {
    const url = prompt('输入文件路径或 URL')
    if (!url) return
    aiApi.kbImportFromPath({ kbId: kbIdNum, path: url })
      .then(() => { toast('导入任务已提交', 'success'); qc.invalidateQueries({ queryKey: ['kb-docs', kbIdNum] }) })
      .catch((e: Error) => toast(e.message, 'error'))
  }, [kbIdNum, qc, toast])

  const handleSearch = () => {
    setSearch(s => ({
      ...s,
      page: 0,
      keyword,
      sourceType: sourceType === '全部' ? '' : sourceType,
    }))
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '文件名', flex: 1, minWidth: 180,
      renderCell: ({ row, value }) => (
        <Button size="small" variant="text" sx={{ justifyContent: 'flex-start' }}
          onClick={() => setDetail(row as KbDocument)}>{String(value)}</Button>
      ) },
    { field: 'fileType', headerName: '类型', width: 90,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => {
        const s = STATUS_MAP[value as number] ?? { label: String(value), color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      } },
    { field: 'createTime', headerName: '创建时间', width: 155, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" color="error" onClick={() => setDeleteId((row as KbDocument).id)}>删除</Button>
      ) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Select size="small" value={sourceType} onChange={e => setSourceType(e.target.value)} sx={{ minWidth: 100, borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& .MuiOutlinedInput-notchedOutline': { borderColor: 'var(--color-surface-light)' }, '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'var(--color-primary)' } }}>
        {SOURCE_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
      </Select>
      <TextField size="small" placeholder="搜索文件名" value={keyword}
        onChange={e => setKeyword(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && handleSearch()}
        sx={{ width: 180, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
      <Button variant="contained" size="small" startIcon={<SearchIcon />} onClick={handleSearch} sx={{ bgcolor: 'var(--color-primary)', color: '#000', '&:hover': { bgcolor: 'var(--color-primary-dark)' } }}>查询</Button>
    </Stack>
  )

  const actionSlot = (
    <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={handleImport} sx={{ bgcolor: 'var(--color-primary)', color: '#000', '&:hover': { bgcolor: 'var(--color-primary-dark)' } }}>导入文档</Button>
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 2, bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)' }}>
      <Stack direction="row" alignItems="center" spacing={1}>
        <IconButton size="small" onClick={() => navigate('/admin/ai/knowledge')} sx={{ color: 'var(--color-text-primary)' }}><ArrowBackIcon /></IconButton>
        <Typography variant="h6" sx={{ color: 'var(--color-text-primary)' }}>知识库文档管理</Typography>
        <Chip label={`KB #${kbIdNum}`} size="small" variant="outlined" />
      </Stack>

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />

      <ConfirmDialog open={deleteId !== null} content="确定要删除该文档吗？（将同时删除分块索引）"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />

      <Drawer anchor="right" open={detail !== null} onClose={() => setDetail(null)}
        PaperProps={{ sx: { width: 480, p: 3, bgcolor: 'var(--color-surface)', borderLeft: '1px solid var(--color-surface-light)' } }}>
        {detail && (
          <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="h6" sx={{ wordBreak: 'break-all', fontSize: 15, color: 'var(--color-text-primary)' }}>{detail.title}</Typography>
              <IconButton onClick={() => setDetail(null)} sx={{ color: 'var(--color-text-primary)' }}><CloseIcon /></IconButton>
            </Stack>
            <Divider sx={{ borderColor: 'var(--color-surface-light)' }} />
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>文档ID</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{detail.id}</Typography></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>知识库ID</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{detail.kbId}</Typography></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>文件类型</Typography>
              <Chip label={detail.fileType} size="small" variant="outlined" sx={{ borderColor: 'var(--color-surface-light)' }} /></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>索引状态</Typography>
              <Chip label={STATUS_MAP[detail.status]?.label ?? String(detail.status)}
                color={STATUS_MAP[detail.status]?.color ?? 'default'} size="small" /></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>创建时间</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{formatDate(detail.createTime)}</Typography></Box>
            <Button size="small" color="error" variant="outlined"
              onClick={() => { setDeleteId(detail.id); setDetail(null) }} sx={{ borderColor: 'var(--color-error)', color: 'var(--color-error)', '&:hover': { bgcolor: 'rgba(239, 68, 68, 0.04)' } }}>删除文档</Button>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
