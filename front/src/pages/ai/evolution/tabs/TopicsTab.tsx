import { useState } from 'react'
import {
  Alert, Box, Button, Chip, Stack, TextField, Typography,
  Dialog, DialogTitle, DialogContent, DialogActions,
  FormControl, InputLabel, Select, MenuItem, IconButton, Link,
} from '@mui/material'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { formatDate } from '@/utils/date'
import type { AiEvolveTopicSaveVO, AiEvolveTopicVO } from '@/types/ai'
import { displayTopicTier } from '@/pages/ai/evolution/topicPriority'
import { parseScopeKbId } from '@/pages/ai/evolution/engineConstants'
import { getErrorMessage } from '@/utils/errorHandler'

export interface TopicsTabProps {
  scopeKbId: string
}

export function TopicsTab({ scopeKbId }: TopicsTabProps) {
  const toast = useToast()
  const qc = useQueryClient()
  const [editItem, setEditItem] = useState<Partial<AiEvolveTopicVO> | null>(null)
  const [pageError, setPageError] = useState<string | null>(null)

  const scopeKbNum = parseScopeKbId(scopeKbId)

  const { data, isFetching, isError, error, isSuccess } = useQuery({
    queryKey: ['evolve-topics', scopeKbId || 'all'],
    queryFn: () => aiApi.topicList(scopeKbNum != null ? { kbId: scopeKbNum } : {}),
  })

  const saveMut = useMutation({
    mutationFn: (p: AiEvolveTopicSaveVO) => aiApi.topicSave(p),
    onSuccess: () => {
      setPageError(null)
      toast('保存成功', 'success')
      setEditItem(null)
      qc.invalidateQueries({ queryKey: ['evolve-topics'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setPageError(`保存失败（POST /ai/evolution/topic/save）：${message}。编辑弹窗和输入值会保留。`)
      toast(`保存失败：${message}`, 'error')
    },
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.topicDelete(id),
    onSuccess: () => {
      setPageError(null)
      toast('已删除', 'success')
      qc.invalidateQueries({ queryKey: ['evolve-topics'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setPageError(`删除失败（POST /ai/evolution/topic/delete）：${message}。主题行会保留，避免误删。`)
      toast(`删除失败：${message}`, 'error')
    },
  })
  const triggerMut = useMutation({
    mutationFn: (topic: AiEvolveTopicVO) => aiApi.evolveTaskTrigger({
      taskType: 'deepen',
      targetKbId: topic.kbId ?? scopeKbNum,
      targetId: Number(topic.id),
    }),
    onSuccess: () => {
      setPageError(null)
      toast('深度进化已触发', 'success')
      qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setPageError(`运行主题失败（POST /ai/evolution/task/trigger）：${message}。主题行会保留。`)
      toast(`触发失败：${message}`, 'error')
    },
  })

  const rows = Array.isArray(data) ? data : []
  const tier = (p: unknown) => displayTopicTier(typeof p === 'number' ? p : Number(p))

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'topicName', headerName: '主题名称', flex: 1.5, minWidth: 140,
      valueGetter: (_v, row: AiEvolveTopicVO) => row.topicName ?? row.topic ?? '' },
    { field: 'kbId', headerName: 'KB', width: 72, valueGetter: (_v, row: AiEvolveTopicVO) => row.kbId ?? '—' },
    { field: 'category', headerName: '分类', width: 100 },
    { field: 'priority', headerName: '优先级', width: 100,
      renderCell: (p: GridRenderCellParams) => {
        const t = tier(p.value)
        return (
          <Chip label={`P${t}`} size="small"
            color={t === 1 ? 'error' : t === 2 ? 'warning' : 'default'} />
        )
      } },
    { field: 'status', headerName: '状态', width: 80,
      renderCell: (p: GridRenderCellParams) => (
        <Chip label={p.value === 1 ? '启用' : '停用'} size="small"
          color={p.value === 1 ? 'success' : 'default'} />
      ) },
    { field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: (p: GridRenderCellParams) => (
        <Typography variant="body2">{formatDate(String(p.value ?? ''))}</Typography>
      ) },
    { field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: (p: GridRenderCellParams) => {
        const row = p.row as AiEvolveTopicVO
        const rowName = String(row.topicName ?? row.topic ?? row.id ?? '主题')
        return (
          <Stack direction="row" spacing={0.5}>
            <IconButton size="small" aria-label={`编辑主题 ${rowName}`} onClick={() => setEditItem(row)}><EditIcon fontSize="small" /></IconButton>
            <Button size="small" startIcon={<PlayArrowIcon />}
              onClick={() => triggerMut.mutate(row)}
              disabled={triggerMut.isPending}>运行</Button>
            <IconButton size="small" color="error" aria-label={`删除主题 ${rowName}`} onClick={() => deleteMut.mutate(Number(row.id))}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Stack>
        )
      } },
  ]

  const submitSave = () => {
    if (!editItem?.topicName?.trim()) {
      toast('请填写主题名称', 'error')
      return
    }
    const tierVal = displayTopicTier(editItem.priority)
    const payload: AiEvolveTopicSaveVO = {
      id: editItem.id,
      kbId: editItem.kbId != null && editItem.kbId > 0 ? editItem.kbId : scopeKbNum,
      accountId: editItem.accountId,
      topicName: editItem.topicName.trim(),
      category: editItem.category,
      priority: tierVal,
      status: editItem.status,
    }
    saveMut.mutate(payload)
  }

  return (
    <Box
      data-testid="evolution-topics-tab-contract"
      data-contract-scope="ai-evolution-topics"
      data-ready-endpoints="/ai/evolution/topic/list|/ai/evolution/topic/save|/ai/evolution/topic/delete|/ai/evolution/task/trigger"
      data-unsupported-actions="/ai/evolution/pending-deepen/trigger|local-topic-create|static-topic-list"
      data-no-pending-deepen-trigger="true"
      data-no-local-topic-fallback="true"
      sx={{ bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)', borderRadius: 'var(--border-radius-xl)' }}
    >
      {isError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          加载主题池失败（POST /ai/evolution/topic/list）：{getErrorMessage(error)}
        </Alert>
      ) : null}
      {pageError ? <Alert severity="error" sx={{ mb: 2 }}>{pageError}</Alert> : null}
      {!isFetching && !isError && rows.length === 0 ? (
        <Typography variant="body2" sx={{ mb: 2, color: 'var(--color-text-secondary)' }}>
          当前没有可展示的启用主题。可使用右上角「新建主题」添加；若为新环境，请确认库表
          <code style={{ margin: '0 4px' }}>ai_evolve_topic</code>
          已有数据（应用启动时会尝试写入默认主题）。
          <Link component={RouterLink} to="/admin/ai/evolution-topics" sx={{ ml: 0.5 }}>打开主题池独立页</Link>
        </Typography>
      ) : null}
      {isSuccess && rows.length > 0 ? (
        <Typography variant="body2" sx={{ mb: 1, color: 'var(--color-text-secondary)' }}>
          已加载 <strong>{rows.length}</strong> 条启用主题（表格下方可分页浏览）
        </Typography>
      ) : null}
      <Stack direction="row" justifyContent="flex-end" mb={2}>
        <Button
          data-testid="evolution-topic-create-action-surface"
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setEditItem({
            status: 1,
            priority: 2,
            kbId: scopeKbNum,
          })}
          sx={(theme) => ({
            bgcolor: theme.palette.primary.main,
            color: theme.palette.primary.contrastText,
            '&:hover': { bgcolor: theme.palette.primary.dark },
          })}
        >
          新建主题
        </Button>
      </Stack>
      {/*
        StandardDataGrid 内部对 DataGrid 使用 height:100% + absolute；若再开 autoHeight，
        父链无明确高度时可视区域会塌成 0（数据已返回但看起来像「空白」）。固定高度并关闭 autoHeight。
      */}
      <Box sx={{ width: '100%', height: 560, minHeight: 360 }}>
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          getRowId={r => String((r as AiEvolveTopicVO).id)}
          pagination
          pageSizeOptions={[25, 50, 100]}
          initialState={{ pagination: { paginationModel: { pageSize: 50 } } }}
          slotProps={{ toolbar: {} as import('@mui/x-data-grid').GridToolbarProps }}
          autoHeight={false}
        />
      </Box>
      <Dialog open={!!editItem} onClose={() => setEditItem(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{editItem?.id ? '编辑主题' : '新建主题'}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} pt={1}>
            <TextField fullWidth size="small" label="主题名称"
              value={String(editItem?.topicName ?? editItem?.topic ?? '')}
              onChange={e => setEditItem(v => v ? { ...v, topicName: e.target.value } : v)} />
            <TextField fullWidth size="small" label="知识库 ID（可选）"
              value={editItem?.kbId != null ? String(editItem.kbId) : ''}
              onChange={e => setEditItem(v => {
                const raw = e.target.value.trim()
                if (!v) return v
                if (!raw) return { ...v, kbId: undefined }
                const n = Number(raw)
                return { ...v, kbId: Number.isFinite(n) ? n : v.kbId }
              })} />
            <Stack direction="row" spacing={2}>
              <TextField size="small" label="分类" sx={{ flex: 1 }}
                value={String(editItem?.category ?? '')}
                onChange={e => setEditItem(v => v ? { ...v, category: e.target.value } : v)} />
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>优先级</InputLabel>
                <Select label="优先级" value={tier(editItem?.priority)}
                  onChange={e => setEditItem(v => v ? { ...v, priority: Number(e.target.value) } : v)}>
                  <MenuItem value={1}>P1 紧急</MenuItem>
                  <MenuItem value={2}>P2 普通</MenuItem>
                  <MenuItem value={3}>P3 低</MenuItem>
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 80, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface-dark)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
                <InputLabel sx={{ color: 'var(--color-text-secondary)' }}>状态</InputLabel>
                <Select label="状态" value={Number(editItem?.status ?? 1)} sx={{ color: 'var(--color-text-primary)' }}
                  onChange={e => setEditItem(v => v ? { ...v, status: Number(e.target.value) } : v)}>
                  <MenuItem value={1}>启用</MenuItem>
                  <MenuItem value={0}>停用</MenuItem>
                </Select>
              </FormControl>
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ bgcolor: 'var(--color-surface-light)' }}>
          <Button onClick={() => setEditItem(null)} sx={{ color: 'var(--color-text-primary)' }}>取消</Button>
          <Button data-testid="evolution-topic-save-action-surface" variant="contained" disabled={saveMut.isPending}
            onClick={submitSave}
            sx={(theme) => ({
              bgcolor: theme.palette.primary.main,
              color: theme.palette.primary.contrastText,
              '&:hover': { bgcolor: theme.palette.primary.dark },
            })}>保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
