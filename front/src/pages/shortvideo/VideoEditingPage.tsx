import { useState } from 'react'
import { Box, Button, Stack, Typography, Chip, LinearProgress, TextField, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions, FormControlLabel, Switch, Divider, Alert } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi, autoCompose } from '@/api/shortvideo'
import { StandardDataGrid } from '@/components/base/StandardDataGrid'
import { useToast } from '@/contexts/ToastContext'
import type { GridColDef } from '@mui/x-data-grid'

const STATUS_MAP: Record<string, { label: string; color: 'default' | 'info' | 'warning' | 'success' | 'error' }> = {
  pending: { label: '待处理', color: 'default' },
  running: { label: '处理中', color: 'info' },
  completed: { label: '已完成', color: 'success' },
  failed: { label: '失败', color: 'error' },
}

export default function VideoEditingPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [composeOpen, setComposeOpen] = useState(false)
  const [composeForm, setComposeForm] = useState({ projectId: '', duration: 60, style: 'standard', subtitleEnabled: true })

  const { data, isLoading } = useQuery({
    queryKey: ['video-tasks', page],
    queryFn: () => shortvideoApi.videoTaskList({ page, rows: 20 }),
    refetchInterval: 10000,
  })
  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const cancelMutation = useMutation({
    mutationFn: async (taskId: number): Promise<void> => { await shortvideoApi.videoTaskCancel(taskId) },
    onSuccess: () => { toast('已取消', 'success'); qc.invalidateQueries({ queryKey: ['video-tasks'] }) },
    onError: () => toast('操作失败', 'error'),
  })
  const composeMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      await autoCompose({
        projectId: Number(composeForm.projectId),
        duration: composeForm.duration,
        style: composeForm.style,
        subtitleEnabled: composeForm.subtitleEnabled,
      })
    },
    onSuccess: () => {
      toast('自动合成任务已提交，5秒内开始处理', 'success')
      qc.invalidateQueries({ queryKey: ['video-tasks'] })
      setComposeOpen(false)
    },
    onError: () => toast('提交失败', 'error'),
  })
  const retryMutation = useMutation({
    mutationFn: async (taskId: number): Promise<void> => { await shortvideoApi.videoTaskRetry(taskId) },
    onSuccess: () => { toast('已重试', 'success'); qc.invalidateQueries({ queryKey: ['video-tasks'] }) },
    onError: () => toast('操作失败', 'error'),
  })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'taskType', headerName: '任务类型', width: 130 },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const key = String(value ?? '')
        const s = STATUS_MAP[key] ?? { label: key || '未知', color: 'default' as const }
        return <Chip label={s.label} size="small" color={s.color} />
      } },
    { field: 'progress', headerName: '进度', width: 160,
      renderCell: ({ value }) => (
        <Box sx={{ width: '100%', display: 'flex', alignItems: 'center', gap: 1 }}>
          <LinearProgress variant="determinate" value={Number(value ?? 0)} sx={{ flex: 1, height: 6, borderRadius: 3 }} />
          <Typography variant="caption">{Number(value ?? 0)}%</Typography>
        </Box>
      ) },
    { field: 'priority', headerName: '优先级', width: 80 },
    { field: 'createTime', headerName: '创建时间', width: 160 },
    { field: '_actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row: r }) => {
        const status = String(r.status ?? '')
        return (
          <Stack direction="row" spacing={0.5}>
            {(status === 'running' || status === 'pending') && (
              <Button size="small" color="warning" onClick={() => cancelMutation.mutate(Number(r.id))}>取消</Button>
            )}
            {status === 'failed' && (
              <Button size="small" color="primary" onClick={() => retryMutation.mutate(Number(r.id))}>重试</Button>
            )}
            {status === 'completed' && r.outputUrl && (
              <Button size="small" color="success" href={String(r.outputUrl)} target="_blank">查看</Button>
            )}
          </Stack>
        )
      } },
  ]

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 120px)' }}>
      <Alert severity="info">
        异步图生视频任务请在分镜页提交 keyframes；本页列表与取消/重试与后端 <code>/video-task</code> 对齐。
      </Alert>
      <Stack direction="row" spacing={2} justifyContent="flex-end">
        <Button variant="outlined" startIcon={<RefreshIcon />}
          onClick={() => qc.invalidateQueries({ queryKey: ['video-tasks'] })}>刷新</Button>
        <Button variant="outlined" startIcon={<AutoAwesomeIcon />}
          onClick={() => setComposeOpen(true)}>AI自动合成</Button>
      </Stack>

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={m => setPage(m.page)}
        pageSizeOptions={[20]}
      />

      {/* AI 自动合成对话框 */}
      <Dialog open={composeOpen} onClose={() => setComposeOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Stack direction="row" alignItems="center" spacing={1}>
            <AutoAwesomeIcon color="primary" />
            <span>AI自动合成视频</span>
          </Stack>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="项目 ID" type="number" required
              value={composeForm.projectId}
              onChange={e => setComposeForm(p => ({ ...p, projectId: e.target.value }))}
              size="small" fullWidth helperText="将基于该项目的脚本和素材自动合成视频" />
            <Divider />
            <TextField select label="目标时长" value={composeForm.duration}
              onChange={e => setComposeForm(p => ({ ...p, duration: Number(e.target.value) }))}
              size="small" fullWidth>
              {[15, 30, 60, 90, 120].map(d => (
                <MenuItem key={d} value={d}>{d}秒</MenuItem>
              ))}
            </TextField>
            <TextField select label="风格" value={composeForm.style}
              onChange={e => setComposeForm(p => ({ ...p, style: e.target.value }))}
              size="small" fullWidth>
              {['standard', 'dynamic', 'storytelling', 'tutorial'].map(s => (
                <MenuItem key={s} value={s}>{{ standard: '标准', dynamic: '动感', storytelling: '叙事', tutorial: '教程' }[s]}</MenuItem>
              ))}
            </TextField>
            <FormControlLabel
              control={<Switch checked={composeForm.subtitleEnabled}
                onChange={e => setComposeForm(p => ({ ...p, subtitleEnabled: e.target.checked }))} />}
              label="自动生成字幕（普通话识别率 ≥ 90%）"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setComposeOpen(false)}>取消</Button>
          <Button variant="contained" startIcon={<AutoAwesomeIcon />}
            onClick={() => composeMutation.mutate()}
            disabled={composeMutation.isPending || !composeForm.projectId}>
            开始合成
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
