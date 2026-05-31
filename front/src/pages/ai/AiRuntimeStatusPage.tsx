import { useMemo, type ReactNode } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline'
import SchoolIcon from '@mui/icons-material/School'
import QueueIcon from '@mui/icons-material/Queue'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import { useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import { PageHeader } from '@/components/base'

type RuntimeSection = Record<string, unknown>

function asRecord(value: unknown): RuntimeSection {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as RuntimeSection : {}
}

function asNumber(value: unknown): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

function asText(value: unknown, fallback = '-'): string {
  if (value === null || value === undefined || value === '') return fallback
  return String(value)
}

function boolLabel(value: unknown): string {
  return value === true ? '开启' : '关闭'
}

function healthColor(health: unknown): 'success' | 'warning' | 'error' | 'default' {
  const value = String(health ?? '')
  if (['running', 'waiting', 'idle'].includes(value)) return 'success'
  if (['degraded', 'blocked'].includes(value)) return 'warning'
  if (value === 'disabled') return 'default'
  return 'default'
}

function StatusCard({
  title,
  icon,
  health,
  children,
}: {
  title: string
  icon: ReactNode
  health: unknown
  children: ReactNode
}) {
  return (
    <Card variant="outlined" sx={{ height: '100%', borderRadius: 1 }}>
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
          <Stack direction="row" alignItems="center" spacing={1} minWidth={0}>
            {icon}
            <Typography variant="subtitle1" fontWeight={700} noWrap>{title}</Typography>
          </Stack>
          <Chip label={asText(health, 'unknown')} size="small" color={healthColor(health)} variant="outlined" />
        </Stack>
        {children}
      </CardContent>
    </Card>
  )
}

function MetricLine({ label, value }: { label: string; value: unknown }) {
  return (
    <Stack direction="row" justifyContent="space-between" spacing={2} sx={{ py: 0.35 }}>
      <Typography variant="body2" color="text.secondary">{label}</Typography>
      <Typography variant="body2" fontWeight={600} textAlign="right" sx={{ wordBreak: 'break-word' }}>
        {asText(value)}
      </Typography>
    </Stack>
  )
}

export default function AiRuntimeStatusPage() {
  const statusQuery = useQuery({
    queryKey: ['ai-runtime-status'],
    queryFn: () => aiApi.runtimeStatus(),
    refetchInterval: 30000,
  })

  const data = statusQuery.data ?? {}
  const evolution = asRecord(data.evolution)
  const official = asRecord(data.officialCollect)
  const queue = asRecord(data.indexQueue)
  const officialSummary = asRecord(official.summary)
  const lastTask = asRecord(evolution.lastTask)
  const lastItem = asRecord(official.lastItem)
  const taskCounts = asRecord(evolution.taskCounts)

  const topAlert = useMemo(() => {
    if (evolution.enabled === false) return { severity: 'warning' as const, text: '进化引擎当前关闭，定时任务不会生成新进化任务。' }
    if (String(evolution.health ?? '') === 'blocked') return { severity: 'error' as const, text: `进化引擎被熔断阻断：${asText(asRecord(evolution.circuitBreaker).reason)}` }
    if (official.enabled === false) return { severity: 'warning' as const, text: '抖音官方知识采集当前关闭，官方规则不会自动复采。' }
    if (asNumber(queue.failed) > 0) return { severity: 'warning' as const, text: `索引队列存在 ${asNumber(queue.failed)} 条失败任务，需要修复或重试。` }
    return null
  }, [evolution, official, queue])

  return (
    <Box sx={{ p: 3, bgcolor: 'background.default', minHeight: '100vh' }}>
      <PageHeader
        title="采集 / 进化运行状态"
        subtitle="集中查看进化调度、抖音官方知识采集、OCR/ASR 和索引队列是否真的在运行。"
        breadcrumbs={[{ label: 'AI 中心', href: '/admin/ai/dashboard' }, { label: '运行状态' }]}
        actions={(
          <Stack direction="row" spacing={1}>
            <Button startIcon={<RefreshIcon />} variant="outlined" onClick={() => void statusQuery.refetch()}>
              刷新
            </Button>
            <Button component={RouterLink} to="/admin/ai/evolution" variant="contained" startIcon={<PlayCircleOutlineIcon />}>
              进化引擎
            </Button>
          </Stack>
        )}
      />

      {statusQuery.isFetching ? <LinearProgress sx={{ mb: 2 }} /> : null}
      {statusQuery.isError ? (
        <Alert severity="error" sx={{ mb: 2 }}>运行状态接口加载失败，请检查登录态和 `/api/v1/ai/admin/runtime-status`。</Alert>
      ) : null}
      {topAlert ? <Alert severity={topAlert.severity} sx={{ mb: 2 }}>{topAlert.text}</Alert> : null}

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <StatusCard title="进化引擎" icon={<PlayCircleOutlineIcon color="primary" />} health={evolution.health}>
            <MetricLine label="调度开关" value={boolLabel(evolution.enabled)} />
            <MetricLine label="深度进化" value={boolLabel(evolution.deepEvolveEnabled)} />
            <MetricLine label="当前运行" value={evolution.running === true ? '运行中' : '未运行'} />
            <MetricLine label="间隔" value={`${asNumber(evolution.intervalMinutes)} 分钟`} />
            <MetricLine label="下次预计" value={evolution.nextEstimatedRunAt} />
            <MetricLine label="最近任务" value={lastTask.taskNo} />
            <MetricLine label="最近状态" value={lastTask.status} />
            <MetricLine label="最近分数" value={lastTask.scoreTotal} />
            <MetricLine label="完成/失败" value={`${asNumber(taskCounts.completed)} / ${asNumber(taskCounts.failed)}`} />
          </StatusCard>
        </Grid>

        <Grid item xs={12} md={4}>
          <StatusCard title="官方知识采集" icon={<SchoolIcon color="primary" />} health={official.health}>
            <MetricLine label="采集开关" value={boolLabel(official.enabled)} />
            <MetricLine label="Cron" value={official.cron} />
            <MetricLine label="下次采集" value={official.nextRunAt} />
            <MetricLine label="OCR / ASR" value={`${boolLabel(official.imageOcrEnabled)} / ${boolLabel(official.videoAsrEnabled)}`} />
            <MetricLine label="重媒体解析" value={boolLabel(official.deepMediaExtractionEnabled)} />
            <MetricLine label="总记录" value={officialSummary.total} />
            <MetricLine label="失败 / 待媒体" value={`${asNumber(officialSummary.failed)} / ${asNumber(officialSummary.mediaPending)}`} />
            <MetricLine label="最近入库" value={official.lastIndexedAt} />
            <MetricLine label="最近标题" value={lastItem.title} />
          </StatusCard>
        </Grid>

        <Grid item xs={12} md={4}>
          <StatusCard title="索引队列" icon={<QueueIcon color="primary" />} health={queue.health}>
            <MetricLine label="待处理" value={queue.pending} />
            <MetricLine label="处理中" value={queue.processing} />
            <MetricLine label="失败" value={queue.failed} />
            <MetricLine label="已完成" value={queue.done} />
            <Box sx={{ mt: 1.5 }}>
              <Button
                component={RouterLink}
                to="/admin/ai/knowledge"
                size="small"
                variant="outlined"
                startIcon={<ErrorOutlineIcon />}
              >
                查看知识库队列
              </Button>
            </Box>
          </StatusCard>
        </Grid>
      </Grid>
    </Box>
  )
}
