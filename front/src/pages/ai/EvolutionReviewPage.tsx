import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Chip, Grid,
  Button, Tab, Tabs,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Alert,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { DataGridEmptyOverlay, PageHeader, StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import type { EvolutionReviewTaskRow } from '@/types/evolutionEngine'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

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
  DEEPEN: '深度进化', deepen: '深度进化', gap: '知识缺口', timeliness: '时效性', quality: '质量评分',
  GAP: '知识缺口', TIMELINESS: '时效性', QUALITY: '质量评分',
}

const EVOLUTION_REVIEW_READY_ENDPOINTS = '/ai/evolution-review/list,/ai/evolution-review/stats,/ai/evolution-review/approve,/ai/evolution-review/reject'
const EVOLUTION_REVIEW_UNSUPPORTED_ENDPOINTS = '/ai/evolution-review/mock,/ai/evolution-review/local-list,/ai/evolution-review/static-stats,/ai/evolution-review/local-approve,/ai/evolution-review/local-reject'

export default function EvolutionReviewPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [page, setPage] = useState(0)
  const [actionRow, setActionRow] = useState<EvolutionReviewTaskRowEx | null>(null)
  const [actionType, setActionType] = useState<'approve' | 'reject'>('approve')
  const [comment, setComment] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const statusFilter = STATUS_TABS[tab].value

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['evolve-review-tasks', statusFilter, page],
    queryFn: () => aiApi.evolutionReviewList({ page, rows: 20, status: statusFilter || undefined }),
  })

  const {
    data: stats,
    isError: statsIsError,
    error: statsError,
    refetch: refetchStats,
  } = useQuery({
    queryKey: ['evolve-review-stats'],
    queryFn: () => aiApi.evolutionReviewStats(),
    refetchInterval: 30000,
  })

  const approveMut = useMutation({
    mutationFn: ({ id, comment }: { id: number; comment?: string }) => aiApi.evolutionReviewApprove(id, comment),
    onSuccess: () => {
      setActionError(null)
      toast('已通过并入库', 'success')
      setDialogOpen(false); setComment('')
      qc.invalidateQueries({ queryKey: ['evolve-review-tasks'] })
      qc.invalidateQueries({ queryKey: ['evolve-review-stats'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`通过失败（POST /ai/evolution-review/approve）：${message}。弹窗、备注和审核任务行会保留。`)
      toast(`审核通过失败：${message}`, 'error')
    },
  })

  const rejectMut = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      aiApi.evolutionReviewReject(id, reason),
    onSuccess: () => {
      setActionError(null)
      toast('已拒绝', 'success')
      setDialogOpen(false); setComment('')
      qc.invalidateQueries({ queryKey: ['evolve-review-tasks'] })
      qc.invalidateQueries({ queryKey: ['evolve-review-stats'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`拒绝失败（POST /ai/evolution-review/reject）：${message}。弹窗、拒绝原因和审核任务行会保留。`)
      toast(`审核拒绝失败：${message}`, 'error')
    },
  })

  const openAction = (row: EvolutionReviewTaskRowEx, type: 'approve' | 'reject') => {
    setActionRow(row); setActionType(type); setComment(''); setActionError(null); setDialogOpen(true)
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

  const rows = normalizeRows<EvolutionReviewTaskRowEx>(data)
  const total = readTotal(data, rows.length)

  const pending = stats?.pendingCount ?? stats?.pending ?? 0
  const approved = stats?.approvedCount ?? stats?.approved ?? 0
  const rejected = stats?.rejectedCount ?? stats?.rejected ?? 0
  const revised = stats?.revisedCount ?? stats?.revised ?? 0
  const passRate = stats?.approvalRate7d != null
    ? Number(stats.approvalRate7d)
    : (approved + rejected) > 0 ? Math.round(approved / (approved + rejected) * 100) : 0

  return (
    <Box
      data-testid="evolution-review-page"
      data-ready-endpoints={EVOLUTION_REVIEW_READY_ENDPOINTS}
      data-unsupported-endpoints={EVOLUTION_REVIEW_UNSUPPORTED_ENDPOINTS}
      data-no-local-review-fallback="true"
      data-no-static-stats-fallback="true"
      data-no-local-review-mutation="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', p: 2, gap: 2 }}
    >
      <PageHeader
        title="进化内容审核"
        subtitle="审核 ai_evolution_review_task；通过会推动进化任务完成并反哺主题权重，拒绝会记录原因。"
        breadcrumbs={[{ label: 'AI中心' }, { label: '进化内容审核' }]}
        actions={
          <Button
            size="small"
            variant="outlined"
            startIcon={<RefreshIcon fontSize="small" />}
            onClick={() => {
              void refetch()
              void refetchStats()
            }}
          >
            刷新
          </Button>
        }
      />

      <Alert
        data-testid="evolution-review-boundary-contract"
        data-ready-endpoints={EVOLUTION_REVIEW_READY_ENDPOINTS}
        data-no-local-review-fallback="true"
        data-no-static-stats-fallback="true"
        severity="info"
      >
        列表调用 <code>/ai/evolution-review/list</code>，统计调用 <code>/ai/evolution-review/stats</code>；
        审核通过提交 <code>{'{ taskId, comment }'}</code>，拒绝提交 <code>{'{ taskId, reason }'}</code>；列表兼容裸数组、分页对象和 <code>records/items</code> 包装。
      </Alert>

      {isError ? (
        <Alert
          data-testid="evolution-review-list-error"
          data-ready-endpoints="/ai/evolution-review/list"
          data-no-local-review-fallback="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          审核任务加载失败：{getErrorMessage(error)}
        </Alert>
      ) : null}

      {statsIsError ? (
        <Alert
          data-testid="evolution-review-stats-warning"
          data-ready-endpoints="/ai/evolution-review/stats"
          data-no-static-stats-fallback="true"
          severity="warning"
          action={<Button color="inherit" size="small" onClick={() => void refetchStats()}>重试</Button>}
        >
          审核统计不可用：{getErrorMessage(statsError)}。下方统计卡片会按 0 兜底，不影响列表审核。
        </Alert>
      ) : null}

      {approveMut.isError ? (
        <Alert
          data-testid="evolution-review-approve-error"
          data-ready-endpoints="/ai/evolution-review/approve"
          data-no-local-review-mutation="true"
          data-input-retained="true"
          severity="error"
        >{actionError ?? `通过失败（POST /ai/evolution-review/approve）：${getErrorMessage(approveMut.error)}`}</Alert>
      ) : null}
      {rejectMut.isError ? (
        <Alert
          data-testid="evolution-review-reject-error"
          data-ready-endpoints="/ai/evolution-review/reject"
          data-no-local-review-mutation="true"
          data-input-retained="true"
          severity="error"
        >{actionError ?? `拒绝失败（POST /ai/evolution-review/reject）：${getErrorMessage(rejectMut.error)}`}</Alert>
      ) : null}

      <Grid
        data-testid="evolution-review-summary-cards"
        data-ready-endpoints="/ai/evolution-review/stats"
        data-no-static-stats-fallback="true"
        container
        spacing={2}
      >
        {([
          { label: '待审核', value: pending, color: 'warning.main' as const },
          { label: '本月通过', value: approved, color: 'success.main' as const },
          { label: '已拒绝', value: rejected, color: 'error.main' as const },
          { label: '已修订', value: revised, color: 'info.main' as const },
          { label: '7天通过率', value: `${passRate}%`, color: 'primary.main' as const },
        ] as const).map(k => (
          <Grid item xs={6} sm={2.4} key={k.label}>
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
        {!isFetching && rows.length === 0 ? (
          <Alert
            data-testid="evolution-review-empty-state"
            data-no-static-review-fallback="true"
            severity="warning"
            sx={{ mb: 1 }}
          >
            当前筛选下暂无审核任务。只有进化任务进入灰区或质量阈值需要人工确认时，后端才会写入审核表。
          </Alert>
        ) : null}
        <Box
          data-testid="evolution-review-grid"
          data-ready-endpoints="/ai/evolution-review/list"
          data-pagination-mode="server"
          data-no-local-review-fallback="true"
        >
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
            slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          />
        </Box>
      </Box>

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'evolution-review-action-dialog',
          'data-ready-endpoints': actionType === 'approve' ? '/ai/evolution-review/approve' : '/ai/evolution-review/reject',
          'data-no-local-review-mutation': 'true',
          'data-input-retained': 'true',
        } as Record<string, string>}
      >
        <DialogTitle>{actionType === 'approve' ? '确认通过并入库' : '拒绝原因'}</DialogTitle>
        <DialogContent>
          {actionError ? <Alert severity="error" sx={{ mb: 2 }}>{actionError}</Alert> : null}
          {actionRow && (
            <Box
              data-testid="evolution-review-summary-surface"
              sx={(theme) => ({
                mb: 2,
                p: 1.5,
                bgcolor: theme.palette.mode === 'dark'
                  ? theme.palette.background.default
                  : alpha(theme.palette.common.black, 0.025),
                border: `1px solid ${theme.palette.divider}`,
                borderRadius: 1,
              })}
            >
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
              if (actionType === 'approve') approveMut.mutate({ id, comment: comment.trim() || undefined })
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
