import { useState } from 'react'
import {
  Box, Button, Tabs, Tab, Chip, Stack, IconButton, Tooltip, Typography,
  Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import DeleteIcon from '@mui/icons-material/Delete'
import VisibilityIcon from '@mui/icons-material/Visibility'
import HistoryIcon from '@mui/icons-material/History'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { workflowApi, type AgentWorkflow, type WorkflowExecution } from '@/api/agent'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { formatDate } from '@/utils/date'

const STATUS_MAP: Record<number, { label: string; color: 'success' | 'warning' | 'error' | 'default' }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已启用', color: 'success' },
  9: { label: '已禁用', color: 'error' },
}

const EXEC_STATUS_MAP: Record<number, { label: string; color: 'info' | 'success' | 'error' | 'default' }> = {
  0: { label: '运行中', color: 'info' },
  1: { label: '已完成', color: 'success' },
  2: { label: '失败', color: 'error' },
  3: { label: '已取消', color: 'default' },
}

function WorkflowListTab() {
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()

  const { data: rows = [], isLoading } = useQuery({
    queryKey: ['workflow', 'list'],
    queryFn: () => workflowApi.list(0, 100).then(r => r.list),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => workflowApi.delete(id),
    onSuccess: () => {
      toast('删除成功', 'success')
      queryClient.invalidateQueries({ queryKey: ['workflow', 'list'] })
    },
    onError: () => toast('删除失败', 'error'),
  })

  const executeMutation = useMutation({
    mutationFn: (id: number) => workflowApi.execute(id, ''),
    onSuccess: () => toast('工作流已启动', 'success'),
    onError: () => toast('启动失败', 'error'),
  })

  const columns: GridColDef<AgentWorkflow>[] = [
    { field: 'name', headerName: '工作流名称', flex: 1, minWidth: 160 },
    { field: 'description', headerName: '描述', flex: 1.5, minWidth: 200 },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const s = STATUS_MAP[value] ?? STATUS_MAP[0]
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    {
      field: 'stepCount', headerName: '步骤数', width: 90,
      renderCell: ({ row }) => row.steps?.length ?? 0,
    },
    {
      field: 'version', headerName: '版本', width: 80,
      renderCell: ({ value }) => `v${value}`,
    },
    {
      field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: ({ value }) => formatDate(value),
    },
    {
      field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="编辑">
            <IconButton size="small" onClick={() => navigate(`/ai/agent/workflow/edit/${row.id}`)}>
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="执行">
            <IconButton size="small" color="primary"
              onClick={() => executeMutation.mutate(row.id)}
              disabled={executeMutation.isPending}>
              <PlayArrowIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="删除">
            <IconButton size="small" color="error"
              onClick={() => deleteMutation.mutate(row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<AddIcon />}
          onClick={() => navigate('/ai/agent/workflow/edit')}>
          新建工作流
        </Button>
      </Box>
      <StandardDataGrid rows={rows} columns={columns} loading={isLoading}
        getRowId={row => row.id} density="compact" />
    </Box>
  )
}

function ExecutionHistoryTab() {
  const [selectedExecution, setSelectedExecution] = useState<WorkflowExecution | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  const { data: rows = [], isLoading } = useQuery({
    queryKey: ['workflow', 'execution', 'list'],
    queryFn: () => workflowApi.executionList(0, 100).then(r => r.list),
  })

  const columns: GridColDef<WorkflowExecution>[] = [
    {
      field: 'workflowName', headerName: '工作流', flex: 1, minWidth: 160,
      renderCell: ({ row }) => row.workflowName ?? `ID: ${row.workflowId}`,
    },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const s = EXEC_STATUS_MAP[value] ?? EXEC_STATUS_MAP[0]
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    {
      field: 'progress', headerName: '进度', width: 120,
      renderCell: ({ row }) => `${row.currentStepOrder ?? 0}/${row.totalSteps ?? 0} 步`,
    },
    {
      field: 'durationSeconds', headerName: '耗时', width: 90,
      renderCell: ({ value }) => value != null ? `${value}s` : '-',
    },
    {
      field: 'startTime', headerName: '开始时间', width: 160,
      renderCell: ({ value }) => value ? formatDate(value) : '-',
    },
    {
      field: 'errorMessage', headerName: '错误信息', flex: 1, minWidth: 200,
      renderCell: ({ value }) => value ? (
        <Typography variant="body2" color="error" noWrap>{value}</Typography>
      ) : '-',
    },
    {
      field: 'actions', headerName: '操作', width: 80, sortable: false,
      renderCell: ({ row }) => (
        <Tooltip title="查看详情">
          <IconButton size="small" onClick={() => {
            setSelectedExecution(row)
            setDetailOpen(true)
          }}>
            <HistoryIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ),
    },
  ]

  return (
    <>
      <StandardDataGrid rows={rows} columns={columns} loading={isLoading}
        getRowId={row => row.id} density="compact" />
      <ExecutionDetailDialog
        open={detailOpen}
        execution={selectedExecution}
        onClose={() => setDetailOpen(false)}
      />
    </>
  )
}

function ExecutionDetailDialog({
  open, execution, onClose,
}: {
  open: boolean
  execution: WorkflowExecution | null
  onClose: () => void
}) {
  if (!execution) return null

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>执行详情 — {execution.workflowName}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={1.5} sx={{ pt: 1 }}>
          <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
            <Box>
              <Typography variant="caption" color="text.secondary">状态</Typography>
              <Box display="block">
                <Chip label={EXEC_STATUS_MAP[execution.status]?.label ?? '-'} size="small"
                  color={EXEC_STATUS_MAP[execution.status]?.color ?? 'default'} />
              </Box>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">进度</Typography>
              <Typography>{execution.currentStepOrder ?? 0} / {execution.totalSteps ?? 0} 步</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">耗时</Typography>
              <Typography>{execution.durationSeconds ?? 0}s</Typography>
            </Box>
          </Box>
          {execution.errorMessage && (
            <Box>
              <Typography variant="caption" color="text.secondary">错误信息</Typography>
              <Typography variant="body2" color="error">{execution.errorMessage}</Typography>
            </Box>
          )}
          {execution.steps && execution.steps.length > 0 && (
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                步骤详情
              </Typography>
              <Stack spacing={1}>
                {execution.steps.map(step => (
                  <Box key={step.stepOrder} sx={{ p: 1.5, bgcolor: 'background.default', borderRadius: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                      <Typography variant="body2" fontWeight={600}>
                        {step.stepName ?? `步骤 ${step.stepOrder}`}
                      </Typography>
                      {step.skipped && <Chip label="跳过" size="small" color="default" />}
                      {step.error && <Chip label="失败" size="small" color="error" />}
                      {!step.skipped && !step.error && (
                        <Chip label="成功" size="small" color="success" />
                      )}
                    </Box>
                    {step.output && (
                      <Typography variant="body2" color="text.secondary" sx={{
                        whiteSpace: 'pre-wrap', maxHeight: 100, overflow: 'auto', fontSize: 12,
                      }}>
                        {step.output}
                      </Typography>
                    )}
                    {step.error && (
                      <Typography variant="body2" color="error" sx={{ fontSize: 12 }}>
                        {step.error}
                      </Typography>
                    )}
                  </Box>
                ))}
              </Stack>
            </Box>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>关闭</Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AgentWorkflowListPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box>
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label="工作流列表" />
          <Tab label="执行历史" />
        </Tabs>
      </Box>
      <Box sx={{ mt: 2 }}>
        {tab === 0 && <WorkflowListTab />}
        {tab === 1 && <ExecutionHistoryTab />}
      </Box>
    </Box>
  )
}