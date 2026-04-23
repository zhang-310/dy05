import { useState } from 'react'
import {
  Box, Typography, Stack, Button, TextField, Chip,
  FormControl, InputLabel, Select, MenuItem,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { productApi, type StylePreset } from '@/api/product'

const SCENES = ['护肤', '彩妆', '保健', '食品', '服饰', '家居', '其他']
const TONES = ['专业', '亲切', '活泼', '权威', '温暖']

export default function StylePresetPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, scene: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<StylePreset>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data = [], isFetching } = useQuery({
    queryKey: ['style-presets'],
    queryFn: () => productApi.stylePresetList(),
  })

  const saveMut = useMutation({
    mutationFn: (params: Partial<StylePreset>) => productApi.stylePresetSave(params),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['style-presets'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => productApi.stylePresetDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['style-presets'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const allRows = Array.isArray(data) ? data : []
  const rows = allRows.filter(r => !search.scene || r.category === search.scene)
  const total = rows.length

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'presetName', headerName: '预设名称', flex: 1, minWidth: 140 },
    { field: 'category', headerName: '适用场景', width: 100,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'styleValue', headerName: '语气风格', width: 90,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" /> },
    { field: 'description', headerName: '描述', flex: 1,
      renderCell: ({ value }) => (
        <Typography variant="body2" noWrap>{String(value ?? '')}</Typography>
      ) },
    { field: 'isEnabled', headerName: '状态', width: 80,
      renderCell: ({ value }) => <Chip label={value ? '启用' : '停用'} color={value ? 'success' : 'default'} size="small" /> },
    { field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => { setForm(row as StylePreset); setFormOpen(true) }}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as StylePreset).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <FormControl size="small" sx={{ minWidth: 120 }}>
      <InputLabel>场景</InputLabel>
      <Select value={search.scene} label="场景" onChange={e => setSearch(s => ({ ...s, scene: e.target.value, page: 0 }))}>
        <MenuItem value="">全部</MenuItem>
        {SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
      </Select>
    </FormControl>
  )

  const actionSlot = <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setForm({ isEnabled: true }); setFormOpen(true) }}>新建预设</Button>

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6" fontWeight={600}>话术风格预设</Typography>

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="client"
        searchSlot={searchSlot} actionSlot={actionSlot}
        sx={{ height: 500 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑风格预设' : '新建风格预设'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="预设名称" value={form.presetName ?? ''} onChange={e => setForm(f => ({ ...f, presetName: e.target.value }))} fullWidth />
          <TextField label="预设代码" value={form.presetCode ?? ''} onChange={e => setForm(f => ({ ...f, presetCode: e.target.value }))} fullWidth />
          <Stack direction="row" spacing={2}>
            <FormControl fullWidth size="small">
              <InputLabel>适用场景</InputLabel>
              <Select value={form.category ?? ''} label="适用场景" onChange={e => setForm(f => ({ ...f, category: e.target.value }))}>
                {SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>语气风格</InputLabel>
              <Select value={form.styleValue ?? ''} label="语气风格" onChange={e => setForm(f => ({ ...f, styleValue: e.target.value }))}>
                {TONES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
          </Stack>
          <TextField label="描述" value={form.description ?? ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth multiline minRows={3} />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content="确定删除该风格预设？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId && deleteMut.mutate(deleteId)} />
    </Box>
  )
}
