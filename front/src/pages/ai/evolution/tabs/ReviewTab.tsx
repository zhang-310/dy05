import { useState } from 'react'
import {
  Box, Button, Chip, Drawer, Paper, Stack, TextField, Typography,
  Alert,
} from '@mui/material'
import { useTheme, type Theme } from '@mui/material/styles'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { REVIEW_STATUS_LABEL } from '@/pages/ai/evolution/engineConstants'
import type { EvolutionReviewStats, EvolutionReviewTaskRow } from '@/types/evolutionEngine'
import { getErrorMessage } from '@/utils/errorHandler'

type ReviewStatTone = 'warning' | 'success' | 'error'

function themeToneColor(theme: Theme, tone: ReviewStatTone) {
  const palette = theme.palette[tone]
  return theme.palette.mode === 'dark' ? palette.light : palette.main
}

export function ReviewTab() {
  const theme = useTheme()
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [detailRow, setDetailRow] = useState<EvolutionReviewTaskRow | null>(null)
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectTaskId, setRejectTaskId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [pageError, setPageError] = useState<string | null>(null)

  const { data, isFetching, isError, error } = useQuery({
    queryKey: ['evolution-review', page, statusFilter],
    queryFn: () => aiApi.evolutionReviewList({
      page,
      rows: 20,
      status: statusFilter !== '' ? statusFilter : undefined,
    }),
  })
  const { data: stats, isError: statsIsError, error: statsError } = useQuery({
    queryKey: ['evolution-review-stats'],
    queryFn: () => aiApi.evolutionReviewStats(),
  })

  const approveMut = useMutation({
    mutationFn: (taskId: number) => aiApi.evolutionReviewApprove(taskId),
    onSuccess: () => {
      setPageError(null)
      toast('已通过', 'success')
      qc.invalidateQueries({ queryKey: ['evolution-review'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setPageError(`审核通过失败（POST /ai/evolution-review/approve）：${message}。审核任务行会保留。`)
      toast(`操作失败：${message}`, 'error')
    },
  })
  const rejectMut = useMutation({
    mutationFn: ({ taskId, reason }: { taskId: number; reason: string }) =>
      aiApi.evolutionReviewReject(taskId, reason),
    onSuccess: () => {
      setPageError(null)
      toast('已拒绝', 'success')
      qc.invalidateQueries({ queryKey: ['evolution-review'] })
      setRejectOpen(false)
      setRejectTaskId(null)
      setRejectReason('')
    },
    onError: (e: unknown) => {
      const msg = getErrorMessage(e)
      setPageError(`审核拒绝失败（POST /ai/evolution-review/reject）：${msg}。拒绝理由和任务行会保留。`)
      toast(msg || '操作失败', 'error')
    },
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0
  const statsData = stats

  const openReject = (taskId: number) => {
    setRejectTaskId(taskId)
    setRejectReason('')
    setRejectOpen(true)
  }

  const confirmReject = () => {
    const r = rejectReason.trim()
    if (!r) {
      toast('请填写拒绝理由', 'error')
      return
    }
    if (rejectTaskId != null) {
      rejectMut.mutate({ taskId: rejectTaskId, reason: r })
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'evolveTaskId', headerName: '进化任务', width: 100,
      valueGetter: (_v, row: EvolutionReviewTaskRow) => row.evolveTaskId ?? '—' },
    { field: 'contentPreview', headerName: '内容摘要', flex: 2, minWidth: 200,
      valueGetter: (_v, row: EvolutionReviewTaskRow) =>
        String(row.contentPreview ?? row.content ?? '').slice(0, 200) },
    { field: 'reviewStatus', headerName: '状态', width: 100,
      renderCell: (p: GridRenderCellParams) => {
        const rs = String(p.value ?? 'PENDING')
        const done = rs === 'APPROVED'
        const rej = rs === 'REJECTED'
        return (
          <Chip label={REVIEW_STATUS_LABEL[rs] ?? rs} size="small"
            color={done ? 'success' : rej ? 'error' : 'warning'} />
        )
      } },
    { field: 'qualityScore', headerName: '质量分', width: 90,
      renderCell: (p: GridRenderCellParams) => (
        p.value != null
          ? <Chip size="small" label={String(p.value)} variant="outlined" />
          : null
      ) },
    { field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: (p: GridRenderCellParams) => (
        <Typography variant="body2">{formatDate(String(p.value ?? ''))}</Typography>
      ) },
    { field: 'actions', headerName: '操作', width: 220, sortable: false,
      renderCell: (p: GridRenderCellParams) => {
        const row = p.row as EvolutionReviewTaskRow
        const rs = String(row.reviewStatus ?? 'PENDING')
        const taskId = Number(row.id)
        return (
          <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
            <Button size="small" variant="outlined" onClick={() => setDetailRow(row)}>详情</Button>
            {rs === 'PENDING' ? (
              <>
                <Button size="small" color="success" onClick={() => approveMut.mutate(taskId)}>通过</Button>
                <Button size="small" color="error" onClick={() => openReject(taskId)}>拒绝</Button>
              </>
            ) : (
              <Typography variant="body2" color="text.secondary">—</Typography>
            )}
          </Stack>
        )
      } },
  ]

  const stat = (k: string) => Number(statsData?.[k as keyof EvolutionReviewStats] ?? 0)
  const reviewStatCards = [
    { k: 'pendingCount', k2: 'pending', l: '待审核', tone: 'warning' },
    { k: 'approvedCount', k2: 'approved', l: '已通过', tone: 'success' },
    { k: 'rejectedCount', k2: 'rejected', l: '已拒绝', tone: 'error' },
  ] satisfies Array<{ k: string; k2: string; l: string; tone: ReviewStatTone }>

  return (
    <Box
      data-testid="evolution-review-tab-contract"
      data-contract-scope="ai-evolution-review"
      data-ready-endpoints="/ai/evolution-review/list|/ai/evolution-review/stats|/ai/evolution-review/approve|/ai/evolution-review/reject"
      data-unsupported-actions="local-approval-mutation|local-review-card-move|static-review-stats"
      data-no-local-review-fallback="true"
      data-no-static-review-stats="true"
    >
      <Alert severity="info" sx={{ mb: 2 }}>
        审核列表调用 <code>/ai/evolution-review/list</code>，统计调用 <code>/ai/evolution-review/stats</code>；
        拒绝同时提交 <code>reason</code> 与 <code>comment</code> 兼容后端字段；失败时不移除审核行，拒绝理由保留在输入框内。
      </Alert>
      {isError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          审核任务加载失败（POST /ai/evolution-review/list）：{getErrorMessage(error)}
        </Alert>
      ) : null}
      {statsIsError ? (
        <Alert severity="warning" sx={{ mb: 2 }}>
          审核统计加载失败（POST /ai/evolution-review/stats）：{getErrorMessage(statsError)}。列表仍可独立审核。
        </Alert>
      ) : null}
      {pageError ? <Alert severity="error" sx={{ mb: 2 }}>{pageError}</Alert> : null}
      {rejectOpen && (
        <Box sx={{ mb: 2, p: 2, border: '1px solid var(--color-surface-light)', borderRadius: 1, bgcolor: 'var(--color-surface)' }}>
          <Typography variant="subtitle2" gutterBottom sx={{ color: 'var(--color-text-primary)' }}>拒绝审核（必填理由）</Typography>
          <TextField
            fullWidth
            multiline
            minRows={2}
            size="small"
            placeholder="请说明拒绝原因，将写入审核记录与任务备注"
            value={rejectReason}
            onChange={e => setRejectReason(e.target.value)}
            sx={{ mb: 1 }}
          />
          <Stack direction="row" spacing={1}>
            <Button variant="contained" color="error" size="small" disabled={rejectMut.isPending} onClick={confirmReject}>确认拒绝</Button>
            <Button size="small" onClick={() => { setRejectOpen(false); setRejectTaskId(null); setRejectReason('') }}>取消</Button>
          </Stack>
        </Box>
      )}
      {statsData && (
        <Stack direction="row" spacing={2} mb={2}>
          {reviewStatCards.map(s => (
            <Paper key={s.k} variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center', bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              <Typography
                data-testid="evolution-review-stat-value-surface"
                data-review-stat-tone={s.tone}
                variant="h5"
                fontWeight={700}
                sx={{ color: themeToneColor(theme, s.tone) }}
              >
                {stat(s.k) || stat(s.k2)}
              </Typography>
              <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>{s.l}</Typography>
            </Paper>
          ))}
        </Stack>
      )}
      <Stack direction="row" spacing={1} mb={2} flexWrap="wrap" useFlexGap>
        {[
          { v: '', l: '全部' },
          { v: 'PENDING', l: '待审核' },
          { v: 'APPROVED', l: '已通过' },
          { v: 'REJECTED', l: '已拒绝' },
        ].map(s => (
          <Chip key={s.v || 'all'} label={s.l} onClick={() => { setStatusFilter(s.v); setPage(0) }}
            color={statusFilter === s.v ? 'primary' : 'default'}
            variant={statusFilter === s.v ? 'filled' : 'outlined'} />
        ))}
      </Stack>
      <StandardDataGrid
        rows={rows} columns={columns} loading={isFetching}
        paginationMode="server" rowCount={total}
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={m => setPage(m.page)}
        pageSizeOptions={[20]}
        getRowId={r => r.id}
        slotProps={{ toolbar: {} as import('@mui/x-data-grid').GridToolbarProps }}
        autoHeight
        sx={{ bgcolor: 'var(--color-surface)', '& .MuiDataGrid-row': { borderColor: 'var(--color-surface-light)' } }}
      />
      <Drawer anchor="right" open={!!detailRow} onClose={() => setDetailRow(null)}
        PaperProps={{ sx: { width: 440, p: 2, bgcolor: 'var(--color-surface)' } }}>
        {detailRow && (
          <Stack spacing={1.5}>
            <Typography variant="h6" sx={{ color: 'var(--color-text-primary)' }}>审核详情 #{detailRow.id}</Typography>
            <Typography variant="body2"><strong>进化任务 ID：</strong>{detailRow.evolveTaskId ?? '—'}</Typography>
            <Typography variant="body2"><strong>状态：</strong>{REVIEW_STATUS_LABEL[String(detailRow.reviewStatus ?? 'PENDING')] ?? detailRow.reviewStatus}</Typography>
            <Typography variant="body2"><strong>质量分：</strong>{detailRow.qualityScore ?? '—'}</Typography>
            <Typography variant="subtitle2" sx={{ color: 'var(--color-text-primary)' }}>内容摘要</Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', color: 'var(--color-text-secondary)' }}>
              {detailRow.contentPreview ?? detailRow.content ?? '—'}
            </Typography>
            {detailRow.reviewComment ? (
              <>
                <Typography variant="subtitle2">审核备注</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{detailRow.reviewComment}</Typography>
              </>
            ) : null}
            {detailRow.revisedContent ? (
              <>
                <Typography variant="subtitle2">修订内容</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{detailRow.revisedContent}</Typography>
              </>
            ) : null}
            <Typography variant="caption" color="text.secondary">
              创建 {detailRow.createTime ? formatDate(String(detailRow.createTime)) : '—'}
              {detailRow.reviewedAt ? ` · 审核 ${formatDate(String(detailRow.reviewedAt))}` : ''}
            </Typography>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
