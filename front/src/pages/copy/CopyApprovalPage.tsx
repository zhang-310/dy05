import { useState } from 'react'
import { Box, Stack, Typography, Card, CardContent, Chip, Button, TextField, Drawer, Divider, Alert } from '@mui/material'
import { alpha } from '@mui/material/styles'
import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import { copyApi, type CopyApproval, type CopyApprovalQuery } from '@/api/copy'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { PageHeader } from '@/components/base'

const STATUS_LABELS: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  0: { label: '已拒绝', color: 'error' },
  1: { label: '已通过', color: 'success' },
  2: { label: '待审核', color: 'warning' },
  3: { label: '修改中', color: 'default' },
}
const COPY_APPROVAL_READY_ENDPOINTS = ['/copy/approval/search', '/copy/approval/get', '/copy/approval/save']
const COPY_APPROVAL_UNSUPPORTED_ACTIONS = ['approval-revise', 'approval-stats', 'server-export']
const COPY_APPROVAL_UNSUPPORTED_ENDPOINTS = ['/copy/approval/revise', '/copy/approval/stats', '/copy/approval/export']
const contractValue = (items: string[]) => items.join(',')

export default function CopyApprovalPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<CopyApproval | null>(null)
  const [comment, setComment] = useState('')
  const [actionError, setActionError] = useState('')

  const query: CopyApprovalQuery = { page, rows: 50, approvalStatus: status }
  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['copy-approvals', query],
    queryFn: () => copyApi.approvalSearch(query),
  })

  const approveMut = useMutation({
    mutationFn: ({ id, c }: { id: number; c?: string }) => { setActionError(''); return copyApi.approvalApprove(id, c) },
    onSuccess: () => { toast('已通过', 'success'); setSelected(null); setComment(''); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e) => { setActionError(`/copy/approval/get + /copy/approval/save 审批失败：${getErrorMessage(e)}`); toast('审批失败', 'error') },
  })
  const rejectMut = useMutation({
    mutationFn: ({ id, c }: { id: number; c?: string }) => { setActionError(''); return copyApi.approvalReject(id, c) },
    onSuccess: () => { toast('已拒绝', 'success'); setSelected(null); setComment(''); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e) => { setActionError(`/copy/approval/get + /copy/approval/save 驳回失败：${getErrorMessage(e)}`); toast('驳回失败', 'error') },
  })
  const allItems = data?.list ?? []
  const columns = [2, 1, 0]
  const pendingCount = allItems.filter(item => item.approvalStatus === 2).length
  const approvedCount = allItems.filter(item => item.approvalStatus === 1).length
  const rejectedCount = allItems.filter(item => item.approvalStatus === 0).length

  return (
    <Box
      sx={{ p: 3 }}
      data-testid="copy-approval-workbench"
      data-contract-scope="copy-approval-standalone"
      data-ready-endpoints={contractValue(COPY_APPROVAL_READY_ENDPOINTS)}
      data-unsupported-actions={contractValue(COPY_APPROVAL_UNSUPPORTED_ACTIONS)}
      data-unsupported-endpoints={contractValue(COPY_APPROVAL_UNSUPPORTED_ENDPOINTS)}
      data-active-filter={status ?? 'all'}
      data-page={page}
      data-total-count={data?.total ?? 0}
      data-pending-count={pendingCount}
      data-approved-count={approvedCount}
      data-rejected-count={rejectedCount}
    >
      <PageHeader title="文案审批" subtitle="审批状态与后端 copy_approval 对齐：2=待审核、1=通过、0=拒绝。" />
      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="copy-approval-contract-alert"
        data-contract-status="ready-with-explicit-degradation"
        data-ready-endpoints={contractValue(COPY_APPROVAL_READY_ENDPOINTS)}
        data-unsupported-actions={contractValue(COPY_APPROVAL_UNSUPPORTED_ACTIONS)}
        data-unsupported-endpoints={contractValue(COPY_APPROVAL_UNSUPPORTED_ENDPOINTS)}
        data-no-stats-request="true"
      >
        通过和拒绝会先读取审批详情，再调用 `/copy/approval/save` 更新状态；改稿接口尚未落库，页面会提示到文案库编辑后重新提交。
      </Alert>
      {isError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={<Button size="small" color="inherit" onClick={() => refetch()}>重试</Button>}
          data-testid="copy-approval-list-error"
          data-contract-status="source-error"
          data-source-endpoint="/copy/approval/search"
          data-no-local-cards="true"
        >
          /copy/approval/search 审批列表加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      {actionError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          {actionError}。失败不会关闭详情抽屉或本地移动审批卡片。
        </Alert>
      ) : null}
      {/* 状态 Tab 筛选 */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, width: '100%', minHeight: 560, overflow: 'hidden' }}>
        <Stack direction="row" spacing={1} mb={1}>
          {([undefined, 2, 1, 0] as (number | undefined)[]).map(s => (
            <Chip key={String(s)} label={s === undefined ? '全部' : STATUS_LABELS[s].label}
              color={status === s ? 'primary' : 'default'} onClick={() => setStatus(s)} variant={status === s ? 'filled' : 'outlined'}
              data-testid="copy-approval-status-filter"
              data-filter-status={s ?? 'all'}
              data-contract-action="filter-by-status"
              data-source-endpoint="/copy/approval/search" />
          ))}
        </Stack>
        {/* 三栏看板 */}
        {isFetching && !data ? (
          <Typography color="text.secondary">加载中...</Typography>
        ) : !isError && allItems.length === 0 ? (
          <Alert severity="info">暂无审批记录。请先从文案库提交审批。</Alert>
        ) : !isError && (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 2, flex: 1, overflow: 'auto' }}>
          {columns.map(col => {
            const colStatus = col
            const colItems = allItems.filter(item => item.approvalStatus === colStatus)
            const info = STATUS_LABELS[colStatus]
            return (
              <Box
                key={col}
                data-testid="copy-approval-kanban-column-surface"
                data-approval-status={colStatus}
                data-contract-status="server-list-grouped"
                data-source-endpoint="/copy/approval/search"
                data-item-count={colItems.length}
                sx={(theme) => ({
                  bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                  border: '1px solid',
                  borderColor: 'divider',
                  borderRadius: 1,
                  p: 1,
                  overflow: 'auto',
                })}
              >
                <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                  <Chip label={info.label} size="small" color={info.color} />
                  <Typography variant="caption" color="text.secondary">{colItems.length} 项</Typography>
                </Stack>
                <Stack spacing={1}>
                  {colItems.map(item => (
                    <Card
                      key={item.id}
                      variant="outlined"
                      sx={{ cursor: 'pointer', '&:hover': { boxShadow: 2 } }}
                      data-testid="copy-approval-card"
                      data-approval-id={item.id}
                      data-copy-id={item.copyId}
                      data-approval-status={item.approvalStatus}
                      data-contract-status="server-source"
                      data-source-endpoint="/copy/approval/search"
                      onClick={() => setSelected(item)}>
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Typography variant="body2" fontWeight={600} gutterBottom>文案 #{item.copyId}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {item.copyTitle || item.copyContent || item.comments || '暂无文案摘要'}
                        </Typography>
                        <Typography variant="caption" display="block" color="text.secondary" mt={0.5}>{formatDate(item.createTime)}</Typography>
                      </CardContent>
                    </Card>
                  ))}
                  {colItems.length === 0 && <Typography variant="caption" color="text.disabled" sx={{ p: 1 }}>暂无</Typography>}
                </Stack>
              </Box>
            )
          })}
        </Box>
        )}
      </Box>

      {/* 分页控制 */}
      {(data?.total ?? 0) > 50 && (
        <Stack direction="row" alignItems="center" justifyContent="flex-end" spacing={1} mt={1}>
          <Typography variant="caption" color="text.secondary">共 {data?.total} 条</Typography>
          <Button size="small" disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</Button>
          <Typography variant="caption">{page + 1}</Typography>
          <Button size="small" disabled={(page + 1) * 50 >= (data?.total ?? 0)} onClick={() => setPage(p => p + 1)}>下一页</Button>
        </Stack>
      )}

      {/* 详情 & 操作抽屉 */}
      <Drawer anchor="right" open={!!selected} onClose={() => setSelected(null)}
        PaperProps={{ sx: { width: 480, p: 3 } }}>
        {selected && (
          <Stack
            spacing={2}
            data-testid="copy-approval-detail-drawer"
            data-approval-id={selected.id}
            data-copy-id={selected.copyId}
            data-approval-status={selected.approvalStatus}
            data-ready-endpoints={contractValue(['/copy/approval/get', '/copy/approval/save'])}
            data-unsupported-endpoint="/copy/approval/revise"
          >
            <Typography variant="h6">审批详情</Typography>
            <Divider />
            <Stack direction="row" spacing={1}>
              <Chip label={STATUS_LABELS[selected.approvalStatus]?.label} color={STATUS_LABELS[selected.approvalStatus]?.color} size="small" />
            </Stack>
            {selected.copyTitle && (
              <Box>
                <Typography variant="caption" color="text.secondary">文案标题</Typography>
                <Typography>{selected.copyTitle}</Typography>
              </Box>
            )}
            <Box>
              <Typography variant="caption" color="text.secondary">文案ID</Typography>
              <Typography>{selected.copyId}</Typography>
            </Box>
            {selected.copyContent && (
              <Box>
                <Typography variant="caption" color="text.secondary">文案内容</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{selected.copyContent}</Typography>
              </Box>
            )}
            <Box>
              <Typography variant="caption" color="text.secondary">审批意见</Typography>
              <Typography>{selected.comments || '（无）'}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">审批时间</Typography>
              <Typography>{selected.approvalTime ? formatDate(selected.approvalTime) : '—'}</Typography>
            </Box>
            <Divider />
            {selected.approvalStatus === 2 && (
              <Stack spacing={1}>
                <TextField label="审批意见" value={comment} onChange={e => setComment(e.target.value)} multiline minRows={2} fullWidth size="small" />
                {actionError ? <Alert severity="error">{actionError}。请保留意见后重试。</Alert> : null}
                <Stack direction="row" spacing={1}>
                  <Button variant="contained" color="success" size="small" startIcon={<CheckIcon />}
                    onClick={() => approveMut.mutate({ id: selected.id, c: comment })} loading={approveMut.isPending}
                    data-testid="copy-approval-action-button"
                    data-contract-action="approve"
                    data-ready-endpoints={contractValue(['/copy/approval/get', '/copy/approval/save'])}>通过</Button>
                  <Button variant="outlined" color="error" size="small" startIcon={<CloseIcon />}
                    onClick={() => rejectMut.mutate({ id: selected.id, c: comment })} loading={rejectMut.isPending}
                    data-testid="copy-approval-action-button"
                    data-contract-action="reject"
                    data-ready-endpoints={contractValue(['/copy/approval/get', '/copy/approval/save'])}>拒绝</Button>
                </Stack>
                <Divider />
                <Alert
                  severity="warning"
                  variant="outlined"
                  data-testid="copy-approval-revise-degradation"
                  data-contract-status="unsupported"
                  data-unsupported-endpoint="/copy/approval/revise"
                  data-no-revise-request="true"
                >
                  审批改稿接口尚未落库。需要修改文案时，请先拒绝并填写意见，再到文案库编辑后重新提交审批。
                </Alert>
              </Stack>
            )}
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
