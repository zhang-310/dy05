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
} from '@mui/material'
import {
  PlayArrow as TriggerIcon,
  Visibility as ViewIcon,
  Pause as CancelIcon,
} from '@mui/icons-material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { parseBackendDateTimeMs } from '@/utils/date'

const STATUS_LABELS: Record<number, { label: string; color: 'default' | 'primary' | 'success' | 'error' | 'warning' }> = {
  0: { label: '待执行', color: 'default' },
  1: { label: '执行中', color: 'primary' },
  2: { label: '已完成', color: 'success' },
  3: { label: '失败', color: 'error' },
  4: { label: '已取消', color: 'warning' },
}

const DAYS_OPTIONS = [{ value: 7, label: '近7天' }, { value: 14, label: '近14天' }, { value: 30, label: '近30天' }, { value: 90, label: '近90天' }]

export default function EvolutionTasksPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [statusFilter, setStatusFilter] = useState('')
  const [daysFilter, setDaysFilter] = useState(30)
  const [reportOpen, setReportOpen] = useState(false)
  const [reportData, setReportData] = useState<Record<string, unknown> | null>(null)
  const [reportTab, setReportTab] = useState(0)
  const [triggering, setTriggering] = useState(false)

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['evolve-tasks'],
    queryFn: () => aiApi.evolveTaskList({ rows: 50, page: 0 }).then((r) => r.list ?? []) as Promise<Record<string, unknown>[]>,
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

  const handleTrigger = async () => {
    setTriggering(true)
    try {
      await aiApi.evolveTaskTrigger({ taskType: 'gap' })
      toast('已触发进化任务', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-tasks'] })
    } catch {
      toast('触发失败', 'error')
    } finally {
      setTriggering(false)
    }
  }

  const handleViewReport = async (task: Record<string, unknown>) => {
    try {
      const data = await aiApi.evolveTaskReport(task.id as number)
      setReportData(data)
      setReportTab(0)
      setReportOpen(true)
    } catch (e) {
      const message = e instanceof Error ? e.message : '加载报告失败'
      toast(message, 'error')
    }
  }

  const handleCancel = async (id: number) => {
    if (!window.confirm('确认取消该任务？')) return
    try {
      await aiApi.evolveTaskCancel(id)
      toast('已取消', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-tasks'] })
    } catch {
      toast('取消失败', 'error')
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
    <Box sx={{ bgcolor: 'var(--color-surface-dark)', minHeight: '100vh' }}>
      <PageHeader
        title="进化任务"
        breadcrumbs={[{ label: 'AI中心' }, { label: '进化任务' }]}
        actions={
          <Button
            variant="contained"
            startIcon={triggering ? <CircularProgress size={16} color="inherit" /> : <TriggerIcon />}
            onClick={handleTrigger}
            disabled={triggering}
            sx={{ bgcolor: 'var(--color-primary)', color: '#000', '&:hover': { bgcolor: 'var(--color-primary-dark)' } }}
          >
            立即触发
          </Button>
        }
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 2, px: 'var(--spacing-lg)' }}>
        <FormControl size="small" sx={{ minWidth: 140, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
          <InputLabel sx={{ color: 'var(--color-text-secondary)' }}>状态</InputLabel>
          <Select value={statusFilter} label="状态" onChange={(e) => setStatusFilter(e.target.value)} sx={{ color: 'var(--color-text-primary)' }}>
            <MenuItem value="">全部</MenuItem>
            {Object.entries(STATUS_LABELS).map(([v, { label }]) => (
              <MenuItem key={v} value={v}>{label}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 120, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
          <InputLabel sx={{ color: 'var(--color-text-secondary)' }}>时间范围</InputLabel>
          <Select value={daysFilter} label="时间范围" onChange={(e) => setDaysFilter(Number(e.target.value))} sx={{ color: 'var(--color-text-primary)' }}>
            {DAYS_OPTIONS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
      </Box>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
      ) : filtered.length === 0 ? (
        <Typography sx={{ color: 'var(--color-text-secondary)', py: 4, textAlign: 'center' }}>暂无任务</Typography>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: 2, px: 'var(--spacing-lg)', py: 'var(--spacing-lg)' }}>
          {filtered.map((task) => {
            const st = STATUS_LABELS[Number(task.status)] ?? { label: String(task.status), color: 'default' as const }
            const progress = Number(task.progress ?? 0)
            return (
              <Card key={String(task.id)} variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Chip size="small" color={st.color} label={st.label} />
                    <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>
                    {String(task.taskType ?? '')}
                    {task.kbId != null && (
                      <span> · KB {String(task.kbId)}</span>
                    )}
                  </Typography>
                  </Box>
                  {Number(task.status) === 1 && (
                    <LinearProgress variant="determinate" value={progress} sx={{ mb: 1 }} />
                  )}
                  <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>
                    {String(task.createTime ?? '').slice(0, 16)}
                  </Typography>
                  {!!task.result && (
                    <Typography variant="body2" sx={{ mt: 1, color: 'var(--color-text-secondary)', wordBreak: 'break-all' }} noWrap>
                      {String(task.result).slice(0, 80)}
                    </Typography>
                  )}
                  {task.scoreTotal != null && Number(task.scoreTotal) > 0 && (
                    <Typography variant="body2" sx={{ mt: 1, color: 'var(--color-text-primary)' }}>
                      质量分: {String(task.scoreTotal)}
                    </Typography>
                  )}
                </CardContent>
                <CardActions>
                  <Button size="small" startIcon={<ViewIcon />} onClick={() => handleViewReport(task)} sx={{ color: 'var(--color-primary)' }}>
                    查看报告
                  </Button>
                  {[0, 1].includes(Number(task.status)) && (
                    <Button size="small" color="warning" startIcon={<CancelIcon />} onClick={() => handleCancel(task.id as number)} sx={{ color: 'var(--color-warning)' }}>
                      取消
                    </Button>
                  )}
                </CardActions>
              </Card>
            )
          })}
        </Box>
      )}

      <Dialog open={reportOpen} onClose={() => setReportOpen(false)} maxWidth="md" fullWidth PaperProps={{ sx: { bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)' } }}>
        <DialogTitle sx={{ bgcolor: 'var(--color-surface-light)' }}>进化报告</DialogTitle>
        <DialogContent dividers sx={{ bgcolor: 'var(--color-surface)' }}>
          {reportData && (
            <>
              {reportSections.length > 1 && (
                <Tabs value={reportTab} onChange={(_, v) => setReportTab(v)} sx={{ mb: 2 }}>
                  {reportSections.map((s, i) => <Tab key={i} label={s.label} />)}
                </Tabs>
              )}
              <Box sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13, color: 'var(--color-text-primary)' }}>
                {String(reportSections[reportTab]?.content ?? reportData.fullContent ?? '')}
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ bgcolor: 'var(--color-surface-light)' }}>
          <Button onClick={() => setReportOpen(false)} sx={{ color: 'var(--color-text-primary)' }}>关闭</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
