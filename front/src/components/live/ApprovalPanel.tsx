import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  List,
  ListItem,
  ListItemText,
  TextField,
  Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty'
import {
  submitApproval,
  approveScript,
  rejectScript,
  getApprovalHistory,
  type LiveApprovalHistoryRecord,
} from '@/api/live-approval'

interface ApprovalPanelProps {
  sessionId: number
  /** 通过/拒绝时必填，与后端 `LiveApprovalController` 的 `scriptId` 一致 */
  scriptId?: number
  canApprove?: boolean
  onStatusChange?: () => void
}

const statusConfig: Record<string, { label: string; color: 'warning' | 'success' | 'error'; icon: React.ReactElement }> = {
  pending: { label: '待审批', color: 'warning', icon: <HourglassEmptyIcon fontSize="small" /> },
  approved: { label: '已通过', color: 'success', icon: <CheckCircleIcon fontSize="small" /> },
  rejected: { label: '已拒绝', color: 'error', icon: <CancelIcon fontSize="small" /> },
}

export default function ApprovalPanel({ sessionId, scriptId, canApprove, onStatusChange }: ApprovalPanelProps) {
  const [currentStatus, setCurrentStatus] = useState<string>('pending')
  const [history, setHistory] = useState<LiveApprovalHistoryRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogAction, setDialogAction] = useState<'approve' | 'reject'>('approve')
  const [comment, setComment] = useState('')

  const loadHistory = useCallback(async () => {
    try {
      const data = await getApprovalHistory({ sessionId })
      setHistory(Array.isArray(data) ? data : [])
      if (Array.isArray(data) && data.length > 0) {
        const latest = data[0]
        if (latest.action === 'approve') {
          setCurrentStatus('approved')
        } else if (latest.action === 'reject') {
          setCurrentStatus('rejected')
        } else {
          setCurrentStatus('pending')
        }
      }
    } catch (e: unknown) {
      console.error('加载审批历史失败', e)
    }
  }, [sessionId])

  useEffect(() => {
    loadHistory()
  }, [loadHistory])

  const handleSubmitApproval = async () => {
    setLoading(true)
    setError(null)
    try {
      await submitApproval({ sessionId })
      setCurrentStatus('pending')
      await loadHistory()
      onStatusChange?.()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '提交审批失败')
    } finally {
      setLoading(false)
    }
  }

  const openDialog = (action: 'approve' | 'reject') => {
    setDialogAction(action)
    setComment('')
    setDialogOpen(true)
  }

  const handleConfirmAction = async () => {
    if (scriptId == null) {
      setError('缺少 scriptId，无法调用审批接口')
      setDialogOpen(false)
      return
    }
    setLoading(true)
    setError(null)
    setDialogOpen(false)
    try {
      if (dialogAction === 'approve') {
        await approveScript({ sessionId, scriptId, comment: comment || undefined })
        setCurrentStatus('approved')
      } else {
        await rejectScript({ sessionId, scriptId, comment: comment || undefined })
        setCurrentStatus('rejected')
      }
      await loadHistory()
      onStatusChange?.()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '操作失败')
    } finally {
      setLoading(false)
    }
  }

  const cfg = statusConfig[currentStatus] ?? statusConfig.pending

  return (
    <Card variant="outlined">
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="subtitle1" fontWeight={600}>
            审批状态
          </Typography>
          <Chip icon={cfg.icon} label={cfg.label} color={cfg.color} size="small" />
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {currentStatus !== 'pending' && currentStatus !== 'approved' && (
          <Button variant="outlined" size="small" onClick={handleSubmitApproval} disabled={loading} sx={{ mb: 2 }}>
            提交审批
          </Button>
        )}

        {canApprove && currentStatus === 'pending' && scriptId == null && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            请传入话术 <code>scriptId</code> 后可通过/拒绝审批（后端要求按话术维度审批）。
          </Alert>
        )}

        {canApprove && currentStatus === 'pending' && (
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Button
              variant="contained"
              color="success"
              size="small"
              startIcon={<CheckCircleIcon />}
              onClick={() => openDialog('approve')}
              disabled={loading || scriptId == null}
            >
              通过
            </Button>
            <Button
              variant="contained"
              color="error"
              size="small"
              startIcon={<CancelIcon />}
              onClick={() => openDialog('reject')}
              disabled={loading || scriptId == null}
            >
              拒绝
            </Button>
          </Box>
        )}

        <Divider sx={{ my: 1 }} />

        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          审批历史
        </Typography>

        {history.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            暂无审批记录
          </Typography>
        ) : (
          <List dense disablePadding>
            {history.map((record, idx) => {
              const actionCfg = statusConfig[record.action] ?? statusConfig.pending
              return (
                <ListItem key={record.id ?? idx} sx={{ px: 0 }}>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Chip label={actionCfg.label} color={actionCfg.color} size="small" variant="outlined" />
                        <Typography variant="body2" color="text.secondary">
                          {record.operatorId != null ? `操作人 #${record.operatorId}` : '系统'}
                        </Typography>
                      </Box>
                    }
                    secondary={
                      <>
                        {record.comment && (
                          <Typography variant="body2" component="span">
                            {record.comment}
                          </Typography>
                        )}
                        {record.createTime && (
                          <Typography variant="caption" color="text.secondary" component="span" sx={{ ml: 1 }}>
                            {record.createTime}
                          </Typography>
                        )}
                      </>
                    }
                  />
                </ListItem>
              )
            })}
          </List>
        )}
      </CardContent>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{dialogAction === 'approve' ? '通过审批' : '拒绝审批'}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            multiline
            minRows={3}
            maxRows={6}
            fullWidth
            label="审批意见"
            placeholder="请输入审批意见（可选）"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button
            onClick={handleConfirmAction}
            variant="contained"
            color={dialogAction === 'approve' ? 'success' : 'error'}
          >
            确认{dialogAction === 'approve' ? '通过' : '拒绝'}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  )
}
