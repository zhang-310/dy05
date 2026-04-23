import { useState } from 'react'
import {
  Box, Typography, Stack, Button, TextField, Card, CardContent,
  Chip, Grid, Select, MenuItem, InputLabel, FormControl, Alert,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { shortvideoApi, type SvDrama } from '@/api/shortvideo'

const GENRES = ['都市', '古装', '悬疑', '爱情', '喜剧', '励志', '其他']
const STATUS_MAP: Record<string, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  draft: { label: '草稿', color: 'default' },
  planning: { label: '策划中', color: 'default' },
  shooting: { label: '拍摄中', color: 'warning' },
  post: { label: '后期中', color: 'warning' },
  done: { label: '已完成', color: 'success' },
  '0': { label: '策划中', color: 'default' },
  '1': { label: '拍摄中', color: 'warning' },
  '2': { label: '后期中', color: 'warning' },
  '3': { label: '已完成', color: 'success' },
}

export default function DramaPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, genre: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SvDrama> & { episodes?: number }>({ status: 'draft' })
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [genDramaId, setGenDramaId] = useState('')
  const [genQuery, setGenQuery] = useState('')
  const [genResult, setGenResult] = useState('')

  const { data: list = [], isFetching } = useQuery({
    queryKey: ['drama-projects'],
    queryFn: () => shortvideoApi.dramaList(),
  })

  const filtered = search.genre
    ? list.filter(d => String(d.genre ?? '') === search.genre)
    : list
  const start = search.page * search.rows
  const rows = filtered.slice(start, start + search.rows)
  const total = filtered.length

  const saveMut = useMutation({
    mutationFn: async (params: Partial<SvDrama> & { episodes?: number }) => {
      const totalEpisodes = params.totalEpisodes ?? params.episodes ?? 1
      const description = params.synopsis ?? params.description
      if (params.id) {
        await shortvideoApi.dramaUpdate({
          id: params.id,
          title: params.title,
          description,
          genre: params.genre,
          totalEpisodes,
        })
      } else {
        await shortvideoApi.dramaCreate({
          title: params.title ?? '',
          description,
          genre: params.genre,
          totalEpisodes,
        })
      }
    },
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['drama-projects'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => shortvideoApi.dramaDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['drama-projects'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const genMut = useMutation({
    mutationFn: () => shortvideoApi.dramaGenerateScript({
      dramaId: Number(genDramaId),
      theme: genQuery,
    }),
    onSuccess: (res) => setGenResult(typeof res === 'string' ? res : JSON.stringify(res)),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '剧情标题', flex: 1, minWidth: 160 },
    { field: 'genre', headerName: '题材', width: 100,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'totalEpisodes', headerName: '集数', width: 80,
      valueGetter: (_v, row) => row.totalEpisodes ?? (row as { episodes?: number }).episodes ?? '—' },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const key = String(value ?? 'draft')
        const s = STATUS_MAP[key] ?? { label: key, color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      } },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => {
            const r = row as SvDrama
            setForm({ ...r, episodes: r.totalEpisodes })
            setFormOpen(true)
          }}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as SvDrama).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <FormControl size="small" sx={{ minWidth: 120 }}>
      <InputLabel>题材</InputLabel>
      <Select value={search.genre} label="题材" onChange={e => setSearch(s => ({ ...s, genre: e.target.value, page: 0 }))}>
        <MenuItem value="">全部</MenuItem>
        {GENRES.map(g => <MenuItem key={g} value={g}>{g}</MenuItem>)}
      </Select>
    </FormControl>
  )

  const actionSlot = <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setForm({ totalEpisodes: 10, status: 'draft' }); setFormOpen(true) }}>新建剧情项目</Button>

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6" fontWeight={600}>剧情短视频创作</Typography>

      <Alert severity="info">列表与创建/更新/删除/剧本生成与后端 <code>/short-video/drama/*</code> 对齐；列表为全量数组前端分页。</Alert>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" fontWeight={600} gutterBottom>AI 剧本生成（占位服务）</Typography>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
            <TextField size="small" label="短剧 ID" value={genDramaId} onChange={e => setGenDramaId(e.target.value)} sx={{ width: 120 }} />
            <Typography variant="caption" color="text.secondary">在下方表格创建短剧后填写其 ID</Typography>
          </Stack>
          <Stack direction="row" spacing={1}>
            <TextField size="small" value={genQuery} onChange={e => setGenQuery(e.target.value)}
              placeholder="主题 / 风格提示..."
              sx={{ flex: 1 }} />
            <Button variant="outlined" startIcon={<AutoFixHighIcon />}
              onClick={() => genMut.mutate()} disabled={!genQuery.trim() || !genDramaId.trim() || genMut.isPending}>
              {genMut.isPending ? '生成中...' : '生成剧本'}
            </Button>
          </Stack>
          {genResult && (
            <Box sx={{ mt: 1.5, p: 1.5, bgcolor: 'grey.50', borderRadius: 1, whiteSpace: 'pre-wrap', fontSize: 13 }}>
              {genResult}
            </Box>
          )}
        </CardContent>
      </Card>

      <Box sx={{ flex: 1 }}>
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={total} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot} actionSlot={actionSlot}
          sx={{ height: 450 }}
        />
      </Box>

      <FormDialog open={formOpen} title={form.id ? '编辑剧情项目' : '新建剧情项目'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="剧情标题" value={form.title ?? ''} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} fullWidth />
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <FormControl fullWidth size="small">
                <InputLabel>题材</InputLabel>
                <Select value={form.genre ?? ''} label="题材" onChange={e => setForm(f => ({ ...f, genre: e.target.value }))}>
                  {GENRES.map(g => <MenuItem key={g} value={g}>{g}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <TextField label="集数" type="number" value={form.totalEpisodes ?? form.episodes ?? 10}
                onChange={e => setForm(f => ({ ...f, totalEpisodes: Number(e.target.value), episodes: Number(e.target.value) }))} fullWidth size="small" />
            </Grid>
          </Grid>
          <TextField label="故事简介" value={form.synopsis ?? form.description ?? ''} onChange={e => setForm(f => ({ ...f, synopsis: e.target.value, description: e.target.value }))} fullWidth multiline minRows={3} />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content="确定删除该剧情项目？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)} loading={deleteMut.isPending} />
    </Box>
  )
}
