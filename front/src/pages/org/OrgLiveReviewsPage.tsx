import { useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent,
  DialogTitle, Grid, Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import RefreshIcon from '@mui/icons-material/Refresh'
import VisibilityIcon from '@mui/icons-material/Visibility'
import type { GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { EmptyState, PageHeader, StandardDataGrid } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { orgApi, type LiveReview } from '@/api/org'
import { getErrorMessage } from '@/utils/errorHandler'

const STATUS_TABS = [
  { label: '全部', value: -1 },
  { label: '分析中', value: 0 },
  { label: '已完成', value: 1 },
  { label: '失败', value: 2 },
]
const LIVE_REVIEW_READY_ENDPOINTS = [
  '/ai/evolution/live-review/list',
  '/ai/evolution/live-review/get',
  '/ai/evolution/live-review/trigger',
  '/ai/evolution/live-review/delete',
] as const
const LIVE_REVIEW_UNSUPPORTED_ACTIONS = ['review-approve', 'review-reject', 'manual-complete', 'server-export'] as const
const LIVE_REVIEW_UNSUPPORTED_ENDPOINTS = [
  '/org/live-review/approve',
  '/org/live-review/reject',
  '/ai/evolution/live-review/complete',
  '/ai/evolution/live-review/export',
] as const
const LIVE_REVIEW_READY_ENDPOINTS_ATTR = LIVE_REVIEW_READY_ENDPOINTS.join(',')
const LIVE_REVIEW_UNSUPPORTED_ACTIONS_ATTR = LIVE_REVIEW_UNSUPPORTED_ACTIONS.join(',')
const LIVE_REVIEW_UNSUPPORTED_ENDPOINTS_ATTR = LIVE_REVIEW_UNSUPPORTED_ENDPOINTS.join(',')

function statusMeta(status: number) {
  if (status === 0) return { label: '分析中', color: 'warning' as const }
  if (status === 1) return { label: '已完成', color: 'success' as const }
  if (status === 2) return { label: '失败', color: 'error' as const }
  return { label: '未知', color: 'default' as const }
}

function money(value: number) {
  if (value >= 10000) return `¥${(value / 10000).toFixed(2)}万`
  return `¥${value.toLocaleString()}`
}

export default function OrgLiveReviewsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(-1)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<LiveReview | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<LiveReview | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [triggerOpen, setTriggerOpen] = useState(false)
  const [sessionId, setSessionId] = useState('')
  const [accountId, setAccountId] = useState('')
  const [triggerError, setTriggerError] = useState('')
  const selectedId = selected?.id

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['org-live-reviews', tab, page],
    queryFn: () => orgApi.liveReviewList({
      status: tab === -1 ? undefined : tab,
      page,
      rows: 20,
    }),
  })
  const rows = data?.list ?? []
  const total = data?.total ?? rows.length
  const doneCount = rows.filter(r => r.status === 1).length
  const runningCount = rows.filter(r => r.status === 0).length
  const failedCount = rows.filter(r => r.status === 2).length
  const totalTokens = rows.reduce((sum, row) => sum + Number(row.tokensUsed ?? 0), 0)
  const { data: detail, isFetching: detailFetching, isError: detailErrorState, error: detailError, refetch: refetchDetail } = useQuery({
    queryKey: ['org-live-review-detail', selectedId],
    queryFn: () => orgApi.liveReviewGet(selectedId as number),
    enabled: selectedId != null,
  })
  const detailReview = detail ?? selected

  const kpiCards = [
    { label: '当前页记录', value: rows.length, hint: `总数 ${total}`, metric: 'page-rows' },
    { label: '已完成', value: doneCount, hint: 'status=1', metric: 'done-count' },
    { label: '分析中/失败', value: `${runningCount}/${failedCount}`, hint: 'status=0 / 2', metric: 'running-failed-count' },
    { label: 'Token 消耗', value: totalTokens.toLocaleString(), hint: '当前页聚合', metric: 'tokens-used' },
  ]

  const invalidate = () => qc.invalidateQueries({ queryKey: ['org-live-reviews'] })
  const triggerMutation = useMutation({
    mutationFn: () => {
      const id = Number(sessionId)
      if (!Number.isFinite(id) || id <= 0) throw new Error('请输入有效的场次 ID')
      return orgApi.liveReviewTrigger({ sessionId: id, accountId: accountId ? Number(accountId) : undefined })
    },
    onSuccess: (id) => {
      toast(`已提交复盘任务 #${id}`, 'success')
      setTriggerOpen(false)
      setSessionId('')
      setAccountId('')
      setTriggerError('')
      invalidate()
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setTriggerError(`${message}。endpoint=/ai/evolution/live-review/trigger，sessionId=${sessionId || '-'}，accountId=${accountId || '-'}`)
      toast(`提交失败：${message}`, 'error')
    },
  })
  const deleteMutation = useMutation({
    mutationFn: (id: number) => orgApi.liveReviewDelete(id),
    onSuccess: () => {
      toast('复盘记录已删除', 'success')
      invalidate()
      setSelected(null)
      setDeleteTarget(null)
      setDeleteError('')
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setDeleteError(`${message}。endpoint=/ai/evolution/live-review/delete，reviewId=${deleteTarget?.id ?? '-'}，sessionId=${deleteTarget?.sessionId ?? '-'}`)
      toast(`删除失败：${message}`, 'error')
    },
  })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 72 },
    { field: 'sessionId', headerName: '场次 ID', width: 100 },
    {
      field: 'status',
      headerName: '状态',
      width: 100,
      renderCell: ({ value }) => {
        const s = statusMeta(Number(value))
        return <Chip label={s.label} size="small" color={s.color} />
      },
    },
    { field: 'totalGmv', headerName: 'GMV', width: 120, renderCell: ({ value }) => money(Number(value ?? 0)) },
    { field: 'totalViewers', headerName: '总观看', width: 100, renderCell: ({ value }) => Number(value ?? 0).toLocaleString() },
    { field: 'peakViewers', headerName: '峰值观看', width: 100, renderCell: ({ value }) => Number(value ?? 0).toLocaleString() },
    { field: 'conversionRate', headerName: '转化率', width: 100, renderCell: ({ value }) => `${(Number(value ?? 0) * 100).toFixed(2)}%` },
    { field: 'modelUsed', headerName: '模型', width: 120, renderCell: ({ value }) => value || '-' },
    { field: 'tokensUsed', headerName: 'Token', width: 100, renderCell: ({ value }) => Number(value ?? 0).toLocaleString() },
    { field: 'createTime', headerName: '创建时间', width: 170, renderCell: ({ value }) => value ? String(value).slice(0, 19).replace('T', ' ') : '-' },
    {
      field: '_actions',
      headerName: '操作',
      width: 180,
      sortable: false,
      renderCell: ({ row }) => {
        const review = row as LiveReview
        return (
          <Stack direction="row" spacing={0.5}>
            <Button size="small" startIcon={<VisibilityIcon />} onClick={() => setSelected(review)}>详情</Button>
            <Button
              size="small"
              color="error"
              startIcon={<DeleteIcon />}
              data-contract-action="delete-review"
              data-contract-endpoint="/ai/evolution/live-review/delete"
              disabled={deleteMutation.isPending}
              onClick={() => {
                setDeleteTarget(review)
                setDeleteError('')
              }}
            >
              删除
            </Button>
          </Stack>
        )
      },
    },
  ]

  return (
    <Box
      data-testid="org-live-review-workbench"
      data-contract-scope="org-live-review"
      data-ready-endpoints={LIVE_REVIEW_READY_ENDPOINTS_ATTR}
      data-unsupported-actions={LIVE_REVIEW_UNSUPPORTED_ACTIONS_ATTR}
      data-unsupported-endpoints={LIVE_REVIEW_UNSUPPORTED_ENDPOINTS_ATTR}
      data-status-filter={tab}
      data-page-index={page}
      data-row-count={rows.length}
      data-total-count={total}
      data-done-count={doneCount}
      data-running-count={runningCount}
      data-failed-count={failedCount}
      data-no-local-review-fallback="true"
      data-row-retained-on-action-error="true"
      data-no-manual-complete-action="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 96px)' }}
    >
      <PageHeader
        title="直播复盘"
        subtitle="复盘记录已对齐 `/ai/evolution/live-review/*`；组织端审核通过/拒绝尚无独立接口，页面不再伪装审批成功。"
        breadcrumbs={[{ label: '机构' }, { label: '直播复盘' }]}
        actions={
          <>
            <Button startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>
            <Button
              variant="contained"
              onClick={() => {
                setTriggerError('')
                setTriggerOpen(true)
              }}
            >
              触发复盘
            </Button>
          </>
        }
      />

      {isError && (
        <Alert
          severity="error"
          data-testid="org-live-review-list-source-error"
          data-source-endpoint="/ai/evolution/live-review/list"
          data-no-local-review-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          复盘列表加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      <Alert
        severity="info"
        data-testid="org-live-review-approval-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="degraded"
        data-source-endpoints={LIVE_REVIEW_READY_ENDPOINTS_ATTR}
        data-unsupported-actions={LIVE_REVIEW_UNSUPPORTED_ACTIONS_ATTR}
        data-unsupported-endpoints={LIVE_REVIEW_UNSUPPORTED_ENDPOINTS_ATTR}
      >
        当前后端直播复盘状态只有 `0=分析中、1=完成、2=失败`，没有组织审批状态；“通过/拒绝复盘”需要新增审核表或状态字段后再启用。`/ai/evolution/live-review/complete` 是内部回调，不作为组织端人工完成按钮暴露。
      </Alert>

      <Grid container spacing={1.5}>
        {kpiCards.map(card => (
          <Grid item xs={12} sm={6} md={3} key={card.label}>
            <Card
              variant="outlined"
              data-testid="org-live-review-kpi-card"
              data-contract-status="local-derived"
              data-source-endpoint="/ai/evolution/live-review/list"
              data-kpi-label={card.label}
              data-kpi-metric={card.metric}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{card.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{card.value}</Typography>
                <Typography variant="caption" color="text.secondary">{card.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Tabs value={tab} onChange={(_, v) => { setTab(v); setPage(0) }} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        {STATUS_TABS.map(t => (
          <Tab
            key={t.value}
            label={t.label}
            value={t.value}
            data-testid="org-live-review-status-tab"
            data-status-filter={t.value}
            data-source-endpoint="/ai/evolution/live-review/list"
          />
        ))}
      </Tabs>

      <Box
        data-testid="org-live-review-list-surface"
        data-contract-status="ready"
        data-source-endpoint="/ai/evolution/live-review/list"
        data-status-filter={tab}
        data-row-count={rows.length}
        data-total-count={total}
        data-server-export="unsupported"
        data-no-local-review-fallback="true"
        sx={{ flex: 1, minHeight: 360 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={total}
          getRowId={(r) => (r as LiveReview).id}
          paginationMode="server"
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={m => setPage(m.page)}
          pageSizeOptions={[20]}
          showExport={false}
          slots={{
            noRowsOverlay: () => (
              <EmptyState title="暂无复盘记录" description="可以输入场次 ID 触发 AI 复盘，或等待场次工作台生成复盘任务。" />
            ),
          }}
        />
      </Box>

      <Dialog open={triggerOpen} onClose={() => setTriggerOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>触发直播复盘</DialogTitle>
        <DialogContent
          data-testid="org-live-review-trigger-dialog"
          data-contract-status="ready"
          data-contract-action="trigger-review"
          data-contract-endpoint="/ai/evolution/live-review/trigger"
          data-internal-complete-endpoint="/ai/evolution/live-review/complete"
          data-no-internal-complete="true"
          data-input-retained="true"
        >
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">后端会按 `sessionId` 聚合监控与话术数据，并异步生成 AI 复盘。</Alert>
            {triggerError && (
              <Alert
                severity="error"
                data-testid="org-live-review-trigger-error"
                data-source-endpoint="/ai/evolution/live-review/trigger"
                data-input-retained="true"
              >
                提交复盘失败：{triggerError}。场次 ID 和账号 ID 会保留。
              </Alert>
            )}
            <TextField label="场次 ID" type="number" value={sessionId} onChange={e => setSessionId(e.target.value)} size="small" fullWidth required />
            <TextField label="账号 ID（可选）" type="number" value={accountId} onChange={e => setAccountId(e.target.value)} size="small" fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTriggerOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => triggerMutation.mutate()} disabled={!sessionId || triggerMutation.isPending}>提交</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={selected !== null} onClose={() => setSelected(null)} maxWidth="md" fullWidth>
        <DialogTitle>复盘详情</DialogTitle>
        <DialogContent
          data-testid="org-live-review-detail-dialog"
          data-contract-status="ready"
          data-contract-action="detail-via-get"
          data-contract-endpoint="/ai/evolution/live-review/get"
          data-review-id={selectedId ?? ''}
          data-detail-fetching={detailFetching}
          data-no-fake-detail-report="true"
        >
          {detailErrorState && (
            <Alert
              severity="error"
              data-testid="org-live-review-detail-source-error"
              data-source-endpoint="/ai/evolution/live-review/get"
              action={<Button color="inherit" size="small" onClick={() => refetchDetail()}>重试</Button>}
              sx={{ mb: 2 }}
            >
              复盘详情加载失败：{getErrorMessage(detailError)}。当前不会用空报告冒充详情成功。
            </Alert>
          )}
          {detailReview && (
            <Stack spacing={2} sx={{ mt: 1 }}>
              <Grid container spacing={1.5}>
                {[
                  { label: '场次 ID', value: detailReview.sessionId },
                  { label: '状态', value: statusMeta(detailReview.status).label },
                  { label: 'GMV', value: money(detailReview.totalGmv) },
                  { label: '转化率', value: `${(detailReview.conversionRate * 100).toFixed(2)}%` },
                ].map(item => (
                  <Grid item xs={6} md={3} key={item.label}>
                    <Box sx={{ p: 1.5, border: 1, borderColor: 'divider', borderRadius: 1 }}>
                      <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                      <Typography variant="body2" fontWeight={700}>{item.value}</Typography>
                    </Box>
                  </Grid>
                ))}
              </Grid>
              <Box>
                <Typography variant="subtitle2">报告内容</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                  {detailReview.reportContent || '暂无报告内容'}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2">优秀话术</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                  {detailReview.topScripts || '暂无'}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2">薄弱点</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                  {detailReview.weakPoints || '暂无'}
                </Typography>
              </Box>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelected(null)}>关闭</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={deleteTarget !== null} onClose={() => !deleteMutation.isPending && setDeleteTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>删除直播复盘记录</DialogTitle>
        <DialogContent
          data-testid="org-live-review-delete-dialog"
          data-contract-status="ready"
          data-contract-action="delete-review"
          data-contract-endpoint="/ai/evolution/live-review/delete"
          data-review-id={deleteTarget?.id ?? ''}
          data-session-id={deleteTarget?.sessionId ?? ''}
        >
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="warning">
              确定删除复盘记录 #{deleteTarget?.id}？
              该操作会调用 `/ai/evolution/live-review/delete`，reviewId={deleteTarget?.id ?? '-'}，sessionId={deleteTarget?.sessionId ?? '-'}，不会删除原直播场次。
            </Alert>
            {deleteError && (
              <Alert
                severity="error"
                data-testid="org-live-review-delete-error"
                data-source-endpoint="/ai/evolution/live-review/delete"
                data-review-id={deleteTarget?.id ?? ''}
                data-row-retained-on-action-error="true"
              >
                删除失败：{deleteError}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={deleteMutation.isPending}>取消</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
            disabled={deleteTarget === null || deleteMutation.isPending}
          >
            {deleteMutation.isPending ? '删除中...' : '确认删除'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
