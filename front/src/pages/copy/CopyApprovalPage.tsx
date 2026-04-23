import { useState } from 'react'
import { Box, Stack, Typography, Card, CardContent, Chip, Button, TextField, Drawer, Divider } from '@mui/material'
import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import EditIcon from '@mui/icons-material/Edit'
import { copyApi, type CopyApproval, type CopyApprovalQuery } from '@/api/copy'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const STATUS_LABELS: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  0: { label: '待审批', color: 'warning' },
  1: { label: '已通过', color: 'success' },
  2: { label: '已拒绝', color: 'error' },
  3: { label: '修改中', color: 'default' },
}

export default function CopyApprovalPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<CopyApproval | null>(null)
  const [comment, setComment] = useState('')
  const [reviseContent, setReviseContent] = useState('')

  const query: CopyApprovalQuery = { page, rows: 50, approvalStatus: status }
  const { data, isFetching } = useQuery({
    queryKey: ['copy-approvals', query],
    queryFn: () => copyApi.approvalSearch(query),
  })

  const approveMut = useMutation({
    mutationFn: ({ id, c }: { id: number; c?: string }) => copyApi.approvalApprove(id, c),
    onSuccess: () => { toast('已通过', 'success'); setSelected(null); setComment(''); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const rejectMut = useMutation({
    mutationFn: ({ id, c }: { id: number; c?: string }) => copyApi.approvalReject(id, c),
    onSuccess: () => { toast('已拒绝', 'success'); setSelected(null); setComment(''); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const reviseMut = useMutation({
    mutationFn: ({ id, content }: { id: number; content: string }) => copyApi.approvalRevise(id, content),
    onSuccess: () => { toast('已提交修改', 'success'); setSelected(null); setReviseContent(''); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const allItems = data?.list ?? []
  const columns = [0, 1, 2]

  if (isFetching) return <Box sx={{ p: 2 }}><Typography>加载中...</Typography></Box>

  return (
    <Box sx={{ display: 'flex', gap: 2, height: 'calc(100vh - 48px - 32px)', overflow: 'hidden' }}>
      {/* 状态 Tab 筛选 */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, width: '100%', overflow: 'hidden' }}>
        <Stack direction="row" spacing={1} mb={1}>
          {([undefined, 0, 1, 2] as (number | undefined)[]).map(s => (
            <Chip key={String(s)} label={s === undefined ? '全部' : STATUS_LABELS[s].label}
              color={status === s ? 'primary' : 'default'} onClick={() => setStatus(s)} variant={status === s ? 'filled' : 'outlined'} />
          ))}
        </Stack>
        {/* 三栏看板 */}
        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 2, flex: 1, overflow: 'auto' }}>
          {columns.map(col => {
            const colStatus = col
            const colItems = allItems.filter(item => item.approvalStatus === colStatus)
            const info = STATUS_LABELS[colStatus]
            return (
              <Box key={col} sx={{ bgcolor: 'grey.50', borderRadius: 1, p: 1, overflow: 'auto' }}>
                <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                  <Chip label={info.label} size="small" color={info.color} />
                  <Typography variant="caption" color="text.secondary">{colItems.length} 项</Typography>
                </Stack>
                <Stack spacing={1}>
                  {colItems.map(item => (
                    <Card key={item.id} variant="outlined" sx={{ cursor: 'pointer', '&:hover': { boxShadow: 2 } }}
                      onClick={() => setSelected(item)}>
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Typography variant="body2" fontWeight={600} gutterBottom>文案 #{item.copyId}</Typography>
                        <Typography variant="caption" color="text.secondary">{item.comments}</Typography>
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
          <Stack spacing={2}>
            <Typography variant="h6">审批详情</Typography>
            <Divider />
            <Stack direction="row" spacing={1}>
              <Chip label={STATUS_LABELS[selected.approvalStatus]?.label} color={STATUS_LABELS[selected.approvalStatus]?.color} size="small" />
            </Stack>
            <Box>
              <Typography variant="caption" color="text.secondary">文案ID</Typography>
              <Typography>{selected.copyId}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">审批意见</Typography>
              <Typography>{selected.comments || '（无）'}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">审批时间</Typography>
              <Typography>{selected.approvalTime ? formatDate(selected.approvalTime) : '—'}</Typography>
            </Box>
            <Divider />
            {selected.approvalStatus === 0 && (
              <Stack spacing={1}>
                <TextField label="审批意见" value={comment} onChange={e => setComment(e.target.value)} multiline minRows={2} fullWidth size="small" />
                <Stack direction="row" spacing={1}>
                  <Button variant="contained" color="success" size="small" startIcon={<CheckIcon />}
                    onClick={() => approveMut.mutate({ id: selected.id, c: comment })} loading={approveMut.isPending}>通过</Button>
                  <Button variant="outlined" color="error" size="small" startIcon={<CloseIcon />}
                    onClick={() => rejectMut.mutate({ id: selected.id, c: comment })} loading={rejectMut.isPending}>拒绝</Button>
                </Stack>
                <Divider />
                <TextField label="修改内容" value={reviseContent} onChange={e => setReviseContent(e.target.value)} multiline minRows={3} fullWidth size="small" />
                <Button variant="outlined" size="small" startIcon={<EditIcon />}
                  onClick={() => reviseMut.mutate({ id: selected.id, content: reviseContent })} loading={reviseMut.isPending}>提交修改</Button>
              </Stack>
            )}
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
