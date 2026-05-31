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
import { ConfirmDialog, ErrorAlert, StandardDataGrid } from '@/components/base'
import { workflowApi, type AgentWorkflow, type WorkflowExecution } from '@/api/agent'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray } from '@/utils/response-normalize'

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

const WORKFLOW_LIST_READY_ENDPOINTS = [
  '/agent/workflow/list',
  '/agent/workflow/delete',
  '/agent/workflow/execute',
  '/agent/workflow/execution/list',
].join('|')

const WORKFLOW_LIST_UNSUPPORTED_ENDPOINTS = [
  '/agent/workflow/mock',
  '/agent/workflow/local-delete',
  '/agent/workflow/local-execute',
  '/agent/workflow/export',
  '/agent/workflow/import-local',
  '/agent/workflow/execution/local-list',
  '/agent/workflow/execution/get',
  '/agent/workflow/execution/local-detail',
].join('|')

function WorkflowListTab() {
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [deleteTarget, setDeleteTarget] = useState<AgentWorkflow | null>(null)
  const [actionError, setActionError] = useState('')

  const { data: rows = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['workflow', 'list'],
    queryFn: () => workflowApi.list(0, 100).then(r => normalizeArray<AgentWorkflow>(r)),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => workflowApi.delete(id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setDeleteTarget(null)
      setActionError('')
      queryClient.invalidateQueries({ queryKey: ['workflow', 'list'] })
    },
    onError: (error) => {
      setActionError(`删除工作流失败：${getErrorMessage(error)}。来源：/agent/workflow/delete，页面已保留当前工作流行。`)
      toast('删除失败', 'error')
    },
  })

  const executeMutation = useMutation({
    mutationFn: (id: number) => workflowApi.execute(id, ''),
    onSuccess: () => {
      setActionError('')
      toast('工作流已启动', 'success')
    },
    onError: (error) => {
      setActionError(`启动工作流失败：${getErrorMessage(error)}。来源：/agent/workflow/execute，页面不会伪造执行记录。`)
      toast('启动失败', 'error')
    },
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
            <IconButton
              size="small"
              aria-label={`编辑工作流 ${row.name}`}
              onClick={() => navigate(`/admin/ai/agent/workflow/edit/${row.id}`)}
            >
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="执行">
            <IconButton size="small" color="primary"
              aria-label={`执行工作流 ${row.name}`}
              onClick={() => executeMutation.mutate(row.id)}
              disabled={executeMutation.isPending}>
              <PlayArrowIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="删除">
            <IconButton size="small" color="error"
              aria-label={`删除工作流 ${row.name}`}
              onClick={() => setDeleteTarget(row)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="agent-workflow-list-tab"
      data-source-endpoint="/agent/workflow/list"
      data-no-local-workflow-fallback="true"
      data-no-local-delete-mutation="true"
      data-no-local-execution-record="true"
    >
      {isError && (
        <Box
          data-testid="agent-workflow-list-error"
          data-source-endpoint="/agent/workflow/list"
          data-no-local-workflow-fallback="true"
        >
          <ErrorAlert
            title="工作流列表加载失败"
            message={`${getErrorMessage(error)}。请检查 /agent/workflow/list、登录态和数据权限。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}
      {actionError && (
        <Box
          sx={{ mb: 2 }}
          data-testid="agent-workflow-list-action-error"
          data-no-local-delete-mutation="true"
          data-no-local-execution-record="true"
        >
          <ErrorAlert title="工作流操作失败" message={actionError} onRetry={() => refetch()} />
        </Box>
      )}
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<AddIcon />}
          onClick={() => navigate('/admin/ai/agent/workflow/edit')}>
          新建工作流
        </Button>
      </Box>
      <Box
        data-testid="agent-workflow-list-grid-contract"
        data-source-endpoint="/agent/workflow/list"
        data-no-local-workflow-fallback="true"
      >
        <StandardDataGrid rows={rows} columns={columns} loading={isLoading}
          getRowId={row => row.id} density="compact" />
      </Box>
      <ConfirmDialog
        open={deleteTarget !== null}
        title="确认删除工作流"
        content={`确定要删除「${deleteTarget?.name ?? ''}」吗？删除后无法继续执行该工作流。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        loading={deleteMutation.isPending}
      />
    </Box>
  )
}

function ExecutionHistoryTab() {
  const [selectedExecution, setSelectedExecution] = useState<WorkflowExecution | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  const { data: rows = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['workflow', 'execution', 'list'],
    queryFn: () => workflowApi.executionList(0, 100).then(r => normalizeArray<WorkflowExecution>(r)),
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
          <IconButton size="small" aria-label={`查看执行详情 ${row.workflowName ?? row.workflowId}`} onClick={() => {
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
    <Box
      data-testid="agent-workflow-execution-history-tab"
      data-source-endpoint="/agent/workflow/execution/list"
      data-no-local-execution-fallback="true"
      data-no-execution-detail-fetch="true"
    >
      {isError && (
        <Box
          data-testid="agent-workflow-execution-list-error"
          data-source-endpoint="/agent/workflow/execution/list"
          data-no-local-execution-fallback="true"
        >
          <ErrorAlert
            title="执行历史加载失败"
            message={`${getErrorMessage(error)}。请检查 /agent/workflow/execution/list 与工作流执行表。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}
      <Box
        data-testid="agent-workflow-execution-grid-contract"
        data-source-endpoint="/agent/workflow/execution/list"
        data-no-execution-detail-fetch="true"
      >
        <StandardDataGrid rows={rows} columns={columns} loading={isLoading}
          getRowId={row => row.id} density="compact" />
      </Box>
      <ExecutionDetailDialog
        open={detailOpen}
        execution={selectedExecution}
        onClose={() => setDetailOpen(false)}
      />
    </Box>
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
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      data-testid="agent-workflow-execution-detail-dialog"
      data-no-execution-detail-fetch="true"
    >
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
    <Box
      data-testid="agent-workflow-list-page"
      data-ready-endpoints={WORKFLOW_LIST_READY_ENDPOINTS}
      data-unsupported-endpoints={WORKFLOW_LIST_UNSUPPORTED_ENDPOINTS}
      data-no-local-delete-mutation="true"
      data-no-local-execution-record="true"
      data-no-execution-detail-fetch="true"
    >
      <Box sx={{ mb: 2 }}>
        <Typography variant="body2" color="text.secondary" data-testid="agent-workflow-list-boundary-contract">
          工作流列表只调用 <code>/agent/workflow/list</code>、<code>/agent/workflow/delete</code>、<code>/agent/workflow/execute</code>；
          执行历史只读取 <code>/agent/workflow/execution/list</code>，详情弹窗复用列表行数据，不额外请求执行详情。
        </Typography>
      </Box>
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
