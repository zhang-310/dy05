import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Chip, Grid,
  Button, Tab, Tabs,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import type { EvolutionReviewTaskRow } from '@/types/evolutionEngine'

/** 扩展字段：后端可能在 reviewStatus 之外附加 status / contentSummary / result */
type EvolutionReviewTaskRowEx = EvolutionReviewTaskRow & {
  status?: string
  contentSummary?: string
  result?: string
}

const STATUS_TABS = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: '', label: '全部' },
]

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error'> = {
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'error',
}

const TASK_TYPE_LABELS: Record<string, string> = {
  DEEPEN: '深度进化', GAP: '知识缺口', TIMELINESS: '时效性', QUALITY: '质量评分',
}

export default function EvolutionReviewPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [page, setPage] = useState(0)
  const [actionRow, setActionRow] = useState<EvolutionReviewTaskRowEx | null>(null)
  const [actionType, setActionType] = useState<'approve' | 'reject'>('approve')
  const [comment, setComment] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)

  const statusFilter = STATUS_TABS[tab].value

  const { data, isFetching } = useQuery({
    queryKey: ['evolve-review-tasks', statusFilter, page],
    queryFn: () => aiApi.evolutionReviewList({ page, rows: 20, status: statusFilter || undefined }),
  })

  const { data: stats } = useQuery({
    queryKey: ['evolve-review-stats'],
    queryFn: () => aiApi.evolutionReviewStats(),
    refetchInterval: 30000,
  })

  const approveMut = useMutation({
    mutationFn: (id: number) => aiApi.evolutionReviewApprove(id),
    onSuccess: () => {
      toast('已通过并入库', 'success')
      setDialogOpen(false); setComment('')
      qc.invalidateQueries({ queryKey: ['evolve-review-tasks'] })
      qc.invalidateQueries({ queryKey: ['evolve-review-stats'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const rejectMut = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      aiApi.evolutionReviewReject(id, reason),
    onSuccess: () => {
      toast('已拒绝', 'success')
      setDialogOpen(false); setComment('')
      qc.invalidateQueries({ queryKey: ['evolve-review-tasks'] })
      qc.invalidateQueries({ queryKey: ['evolve-review-stats'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAction = (row: EvolutionReviewTaskRowEx, type: 'approve' | 'reject') => {
    setActionRow(row); setActionType(type); setComment(''); setDialogOpen(true)
  }

  const isPending = approveMut.isPending || rejectMut.isPending

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'taskType', headerName: '来源', width: 130,
      renderCell: ({ value }) => (
        <Chip label={TASK_TYPE_LABELS[String(value)] ?? String(value ?? '')} size="small" variant="outlined" />
      ) },
    { field: 'reviewStatus', headerName: '状态', width: 100,
      renderCell: ({ row }) => {
        const r = row as EvolutionReviewTaskRowEx
        const v = String(r.reviewStatus ?? r.status ?? 'PENDING')
        return <Chip label={v} color={STATUS_COLOR[v] ?? 'default'} size="small" />
      } },
    { field: 'contentPreview', headerName: '内容摘要', flex: 1, minWidth: 200,
      valueGetter: (_v: unknown, row: EvolutionReviewTaskRowEx) =>
        String(row.contentPreview ?? row.contentSummary ?? row.result ?? row.content ?? '') },
    { field: 'qualityScore', headerName: '质量分', width: 90,
      renderCell: ({ value }) => value != null
        ? <Chip label={`★ ${Number(value).toFixed(1)}`} size="small"
            color={Number(value) >= 8 ? 'success' : Number(value) >= 6 ? 'warning' : 'error'} />
        : null },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => {
        const r = row as EvolutionReviewTaskRowEx
        const rs = String(r.reviewStatus ?? r.status ?? 'PENDING')
        if (rs !== 'PENDING') {
          return <Typography variant="body2" color="text.secondary">—</Typography>
        }
        return (
          <Stack direction="row" spacing={0.5}>
            <Button size="small" color="success" startIcon={<CheckCircleIcon fontSize="small" />}
              onClick={() => openAction(r, 'approve')}>通过</Button>
            <Button size="small" color="error" startIcon={<CancelIcon fontSize="small" />}
              onClick={() => openAction(r, 'reject')}>拒绝</Button>
          </Stack>
        )
      } },
  ]

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const pending = stats?.pendingCount ?? stats?.pending ?? 0
  const approved = stats?.approvedCount ?? stats?.approved ?? 0
  const rejected = stats?.rejectedCount ?? stats?.rejected ?? 0
  const passRate = (approved + rejected) > 0 ? Math.round(approved / (approved + rejected) * 100) : 0

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', p: 2, gap: 2 }}>
      <Typography variant="h6" fontWeight={700}>进化内容审核</Typography>

      <Grid container spacing={2}>
        {([
          { label: '待审核', value: pending, color: 'warning.main' as const },
          { label: '本月通过', value: approved, color: 'success.main' as const },
          { label: '本月拒绝', value: rejected, color: 'error.main' as const },
          { label: '通过率', value: `${passRate}%`, color: 'primary.main' as const },
        ] as const).map(k => (
          <Grid item xs={6} sm={3} key={k.label}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{k.label}</Typography>
                <Typography variant="h5" fontWeight={700} color={k.color}>{k.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Tabs value={tab} onChange={(_, v) => { setTab(v); setPage(0) }} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        {STATUS_TABS.map((t, i) => <Tab key={i} label={t.label} />)}
      </Tabs>

      <Box sx={{ flex: 1 }}>
        <StandardDataGrid
          rows={rows}
          columns={columns}
          rowCount={total}
          loading={isFetching}
          paginationMode="server"
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={m => setPage(m.page)}
          sx={{ height: 'calc(100vh - 340px)' }}
          slotProps={{ toolbar: undefined }}
        />
      </Box>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{actionType === 'approve' ? '确认通过并入库' : '拒绝原因'}</DialogTitle>
        <DialogContent>
          {actionRow && (
            <Box sx={{ mb: 2, p: 1.5, bgcolor: 'grey.50', borderRadius: 1 }}>
              <Typography variant="caption" color="text.secondary" display="block">内容摘要：</Typography>
              <Typography variant="body2">
                {String(actionRow.contentPreview ?? actionRow.contentSummary ?? actionRow.result ?? actionRow.content ?? '（无摘要）')}
              </Typography>
            </Box>
          )}
          <TextField
            label={actionType === 'approve' ? '审核备注（可选）' : '拒绝原因'}
            value={comment} onChange={e => setComment(e.target.value)}
            fullWidth multiline minRows={3} sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            color={actionType === 'approve' ? 'success' : 'error'}
            disabled={isPending}
            startIcon={actionType === 'approve' ? <CheckCircleIcon /> : <CancelIcon />}
            onClick={() => {
              if (!actionRow) return
              const id = Number(actionRow.id)
              if (actionType === 'approve') approveMut.mutate(id)
              else rejectMut.mutate({ id, reason: comment })
            }}
          >
            {actionType === 'approve' ? '通过并入库' : '确认拒绝'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
