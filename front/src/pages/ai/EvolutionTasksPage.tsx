import { useState, useMemo } from 'react'
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  LinearProgress,
  Stack,
  TextField,
  Alert,
  Divider,
} from '@mui/material'
import {
  PlayArrow as TriggerIcon,
  Visibility as ViewIcon,
  Pause as CancelIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import { ConfirmDialog, PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { parseBackendDateTimeMs } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'
import type { AiEvolveTaskVO } from '@/types/ai'

const STATUS_LABELS: Record<number, { label: string; color: 'default' | 'primary' | 'success' | 'error' | 'warning' }> = {
  0: { label: '待执行', color: 'default' },
  1: { label: '执行中', color: 'primary' },
  2: { label: '已完成', color: 'success' },
  3: { label: '失败', color: 'error' },
  4: { label: '已取消', color: 'warning' },
}

const DAYS_OPTIONS = [{ value: 7, label: '近7天' }, { value: 14, label: '近14天' }, { value: 30, label: '近30天' }, { value: 90, label: '近90天' }]
const TASK_TYPE_OPTIONS = [
  { value: 'gap', label: '知识缺口' },
  { value: 'deepen', label: '深度进化' },
  { value: 'timeliness', label: '时效性' },
  { value: 'quality', label: '质量评分' },
  { value: 'classify', label: '自动分类' },
  { value: 'share', label: '跨域共享' },
]

const EVOLUTION_TASK_READY_ENDPOINTS = [
  '/ai/evolution/task/list',
  '/ai/evolution/task/trigger',
  '/ai/evolution/task/cancel',
  '/ai/admin/evolve/report/by-task',
].join('|')

const EVOLUTION_TASK_UNSUPPORTED_ENDPOINTS = [
  '/ai/evolution/task/mock',
  '/ai/evolution/task/local-insert',
  '/ai/evolution/task/local-cancel',
  '/ai/evolution/task/local-report',
  '/ai/evolution/task/export',
  '/ai/knowledge-evolution/local-analyze',
  '/ai/knowledge-evolution/local-optimize',
].join('|')

export default function EvolutionTasksPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [statusFilter, setStatusFilter] = useState('')
  const [daysFilter, setDaysFilter] = useState(30)
  const [reportOpen, setReportOpen] = useState(false)
  const [reportData, setReportData] = useState<Record<string, unknown> | null>(null)
  const [reportTab, setReportTab] = useState(0)
  const [triggering, setTriggering] = useState(false)
  const [taskType, setTaskType] = useState('gap')
  const [targetKbId, setTargetKbId] = useState('')
  const [pageError, setPageError] = useState<string | null>(null)
  const [cancelTaskId, setCancelTaskId] = useState<number | null>(null)
  const [canceling, setCanceling] = useState(false)

  const {
    data: tasks = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['evolve-tasks', statusFilter],
    queryFn: () =>
      aiApi
        .evolveTaskList({ rows: 50, page: 0, status: statusFilter ? Number(statusFilter) : undefined })
        .then((r) => normalizeRows<AiEvolveTaskVO>(r)),
    refetchInterval: 15000,
  })

  const filtered = useMemo(() => {
    const cutoff = Date.now() - daysFilter * 24 * 3600 * 1000
    return tasks.filter((t) => {
      const matchStatus = !statusFilter || String(t.status) === statusFilter
      if (!matchStatus) return false
      const createMs = parseBackendDateTimeMs(t.createTime)
      const matchTime = !Number.isFinite(createMs) || Number.isNaN(createMs) ? true : createMs >= cutoff
      return matchTime
    })
  }, [tasks, statusFilter, daysFilter])

  const taskStats = useMemo(() => {
    const running = filtered.filter((task) => Number(task.status) === 1).length
    const pending = filtered.filter((task) => Number(task.status) === 0).length
    const completed = filtered.filter((task) => Number(task.status) === 2).length
    const failed = filtered.filter((task) => Number(task.status) === 3).length
    const cancelable = filtered.filter((task) => [0, 1].includes(Number(task.status))).length
    return { running, pending, completed, failed, cancelable }
  }, [filtered])

  const handleTrigger = async () => {
    setTriggering(true)
    setPageError(null)
    try {
      const kb = targetKbId.trim() ? Number(targetKbId.trim()) : undefined
      if (kb != null && (!Number.isFinite(kb) || kb <= 0)) {
        toast('知识库 ID 必须为正整数', 'error')
        return
      }
      await aiApi.evolveTaskTrigger({ taskType, targetKbId: kb })
      toast('已触发进化任务', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-tasks'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`触发失败（POST /ai/evolution/task/trigger）：${message}。taskType 与 targetKbId 输入会保留。`)
      toast(`触发失败：${message}`, 'error')
    } finally {
      setTriggering(false)
    }
  }

  const handleViewReport = async (task: Record<string, unknown>) => {
    setPageError(null)
    try {
      const data = await aiApi.evolveTaskReport(task.id as number)
      setReportData(data)
      setReportTab(0)
      setReportOpen(true)
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`加载报告失败（POST /ai/admin/evolve/report/by-task）：${message}。任务卡片和筛选条件会保留。`)
      toast(message, 'error')
    }
  }

  const handleCancel = async () => {
    if (cancelTaskId == null) return
    setCanceling(true)
    setPageError(null)
    try {
      await aiApi.evolveTaskCancel(cancelTaskId)
      toast('已取消', 'success')
      setCancelTaskId(null)
      queryClient.invalidateQueries({ queryKey: ['evolve-tasks'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`取消失败（POST /ai/evolution/task/cancel）：${message}。任务卡片会保留，避免误判已取消。`)
      toast(`取消失败：${message}`, 'error')
    } finally {
      setCanceling(false)
    }
  }

  const reportSections = reportData
    ? [
        { label: '方法论', content: reportData.methodologySection ?? '' },
        { label: '深化问题', content: reportData.deepenSection ?? '' },
        { label: '迭代建议', content: reportData.iterateSection ?? '' },
        { label: '全文', content: reportData.fullContent ?? '' },
      ].filter((s) => String(s.content).trim())
    : []

  return (
    <Box
      data-testid="evolution-tasks-page"
      data-ready-endpoints={EVOLUTION_TASK_READY_ENDPOINTS}
      data-unsupported-endpoints={EVOLUTION_TASK_UNSUPPORTED_ENDPOINTS}
      data-no-local-task-insertion="true"
      data-no-local-cancel-mutation="true"
      data-no-local-report-fallback="true"
      sx={{ p: 2, minHeight: '100vh' }}
    >
      <PageHeader
        title="进化任务"
        subtitle="读取 /ai/evolution/task/list；触发任务需明确 taskType，可选 targetKbId 限定知识库。"
        breadcrumbs={[{ label: 'AI中心' }, { label: '进化任务' }]}
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => void refetch()}
              data-testid="evolution-tasks-refresh-list"
              data-source-endpoint="/ai/evolution/task/list"
              data-refresh-scope="task-list-only"
            >
              刷新
            </Button>
            <Button
              variant="contained"
              startIcon={triggering ? <CircularProgress size={16} color="inherit" /> : <TriggerIcon />}
              onClick={handleTrigger}
              disabled={triggering}
              data-testid="evolution-tasks-trigger-button"
              data-source-endpoint="/ai/evolution/task/trigger"
              data-input-retained="true"
              data-no-local-task-insertion="true"
            >
              立即触发
            </Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="evolution-tasks-boundary-contract"
        data-source-endpoints={EVOLUTION_TASK_READY_ENDPOINTS}
        data-no-local-task-insertion="true"
        data-no-local-report-fallback="true"
      >
        任务状态由后端字符串映射为 0-4：待执行、执行中、完成、失败、取消；触发类型使用后端进化角度 <code>gap/deepen/timeliness/quality/classify/share</code>。
        报告读取 <code>/ai/admin/evolve/report/by-task</code>，若任务尚未生成报告会显示后端错误而不是空白弹窗。
      </Alert>

      {isError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="evolution-tasks-list-error"
          data-source-endpoint="/ai/evolution/task/list"
          data-no-local-task-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          进化任务加载失败（POST /ai/evolution/task/list）：{getErrorMessage(error)}
        </Alert>
      ) : null}
      {pageError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="evolution-tasks-operation-error"
          data-input-retained="true"
          data-no-local-task-insertion="true"
          data-no-local-cancel-mutation="true"
          data-no-local-report-fallback="true"
        >
          {pageError}
        </Alert>
      ) : null}

      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ mb: 2 }}
        flexWrap="wrap"
        useFlexGap
        data-testid="evolution-tasks-filter-contract"
        data-source-endpoint="/ai/evolution/task/list"
        data-client-time-filter-only="true"
        data-no-client-status-mutation="true"
      >
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel id="evolution-tasks-status-label">状态</InputLabel>
          <Select
            labelId="evolution-tasks-status-label"
            id="evolution-tasks-status"
            value={statusFilter}
            label="状态"
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <MenuItem value="">全部</MenuItem>
            {Object.entries(STATUS_LABELS).map(([v, { label }]) => (
              <MenuItem key={v} value={v}>{label}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel id="evolution-tasks-days-label">时间范围</InputLabel>
          <Select
            labelId="evolution-tasks-days-label"
            id="evolution-tasks-days"
            value={daysFilter}
            label="时间范围"
            onChange={(e) => setDaysFilter(Number(e.target.value))}
          >
            {DAYS_OPTIONS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
        <Divider orientation="vertical" flexItem />
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel id="evolution-tasks-trigger-type-label">触发类型</InputLabel>
          <Select
            labelId="evolution-tasks-trigger-type-label"
            id="evolution-tasks-trigger-type"
            value={taskType}
            label="触发类型"
            onChange={(e) => setTaskType(String(e.target.value))}
          >
            {TASK_TYPE_OPTIONS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField
          size="small"
          label="targetKbId（可选）"
          value={targetKbId}
          onChange={(e) => setTargetKbId(e.target.value)}
          type="number"
          inputProps={{ min: 1 }}
          sx={{ width: 180 }}
        />
        <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
          当前筛选 {filtered.length} / 拉取 {tasks.length}
        </Typography>
      </Stack>

      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        sx={{ mb: 2 }}
        data-testid="evolution-tasks-summary-cards"
        data-source-endpoint="/ai/evolution/task/list"
        data-filtered-count={filtered.length}
        data-cancelable-count={taskStats.cancelable}
        data-no-local-task-fallback="true"
      >
        {[
          { label: '待执行', value: taskStats.pending, hint: 'status=0' },
          { label: '执行中', value: taskStats.running, hint: 'status=1，可取消' },
          { label: '已完成', value: taskStats.completed, hint: 'status=2，可查报告' },
          { label: '失败', value: taskStats.failed, hint: 'status=3，不本地修正' },
        ].map((item) => (
          <Card key={item.label} variant="outlined" sx={{ flex: 1, minWidth: 0 }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h6" fontWeight={800}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
      ) : filtered.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="evolution-tasks-empty"
          data-no-local-task-fallback="true"
          data-no-local-task-insertion="true"
        >
          暂无进化任务。可选择触发类型后点击“立即触发”，或等待定时进化任务写入队列。
        </Alert>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: 2 }}>
          {filtered.map((task) => {
            const st = STATUS_LABELS[Number(task.status)] ?? { label: String(task.status), color: 'default' as const }
            const progress = Number(task.progress ?? 0)
            return (
              <Card
                key={String(task.id)}
                variant="outlined"
                data-testid="evolution-tasks-card"
                data-source-endpoint="/ai/evolution/task/list"
                data-task-id={String(task.id)}
                data-task-type={String(task.taskType ?? '')}
                data-task-status={String(task.status ?? '')}
                data-task-kb-id={task.kbId == null ? '' : String(task.kbId)}
                data-task-progress={String(task.progress ?? '')}
                data-no-local-status-mutation="true"
              >
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Chip size="small" color={st.color} label={st.label} />
                    <Typography variant="caption" color="text.secondary">
                    {String(task.taskType ?? '')}
                    {task.kbId != null && (
                      <span> · KB {String(task.kbId)}</span>
                    )}
                  </Typography>
                  </Box>
                  {Number(task.status) === 1 && (
                    <LinearProgress variant="determinate" value={progress} sx={{ mb: 1 }} />
                  )}
                  <Typography variant="caption" color="text.secondary">
                    {String(task.createTime ?? '').slice(0, 16)}
                  </Typography>
                  {!!task.result && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1, wordBreak: 'break-all' }} noWrap>
                      {String(task.result).slice(0, 80)}
                    </Typography>
                  )}
                  {task.scoreTotal != null && Number(task.scoreTotal) > 0 && (
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      质量分: {String(task.scoreTotal)}
                    </Typography>
                  )}
                </CardContent>
                <CardActions>
                  <Button
                    size="small"
                    startIcon={<ViewIcon />}
                    onClick={() => handleViewReport(task)}
                    data-testid="evolution-tasks-view-report"
                    data-source-endpoint="/ai/admin/evolve/report/by-task"
                    data-task-id={String(task.id)}
                    data-no-local-report-fallback="true"
                  >
                    查看报告
                  </Button>
                  {[0, 1].includes(Number(task.status)) && (
                    <Button
                      size="small"
                      color="warning"
                      startIcon={<CancelIcon />}
                      onClick={() => setCancelTaskId(Number(task.id))}
                      data-testid="evolution-tasks-cancel-open"
                      data-source-endpoint="/ai/evolution/task/cancel"
                      data-task-id={String(task.id)}
                      data-no-local-cancel-mutation="true"
                    >
                      取消
                    </Button>
                  )}
                </CardActions>
              </Card>
            )
          })}
        </Box>
      )}

      <Dialog
        open={reportOpen}
        onClose={() => setReportOpen(false)}
        maxWidth="md"
        fullWidth
        data-testid="evolution-tasks-report-dialog"
        data-source-endpoint="/ai/admin/evolve/report/by-task"
        data-no-local-report-fallback="true"
      >
        <DialogTitle>进化报告</DialogTitle>
        <DialogContent dividers>
          {reportData && (
            <>
              {reportSections.length > 1 && (
                <Tabs value={reportTab} onChange={(_, v) => setReportTab(v)} sx={{ mb: 2 }}>
                  {reportSections.map((s, i) => <Tab key={i} label={s.label} />)}
                </Tabs>
              )}
              {reportSections.length > 0 ? (
                <Box sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13 }}>
                  {String(reportSections[reportTab]?.content ?? reportData.fullContent ?? '')}
                </Box>
              ) : (
                <Alert severity="warning">该任务暂无可展示报告内容，请确认进化报告已写入。</Alert>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReportOpen(false)}>关闭</Button>
        </DialogActions>
      </Dialog>
      <ConfirmDialog
        open={cancelTaskId !== null}
        title="取消进化任务"
        content="确定要取消该进化任务吗？执行中的任务取消后需要重新触发。"
        onClose={() => setCancelTaskId(null)}
        onConfirm={handleCancel}
        loading={canceling}
      />
    </Box>
  )
}
