import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import {
  Alert, Box, Button, Card, CardContent, Chip, Tabs, Tab, Stack, Typography, IconButton, Tooltip,
  FormControl, InputLabel, Select, MenuItem, Link, Grid,
} from '@mui/material'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import RefreshIcon from '@mui/icons-material/Refresh'
import StorageIcon from '@mui/icons-material/Storage'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import ScheduleIcon from '@mui/icons-material/Schedule'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { KnowledgeBase } from '@/api/ai'
import { PageHeader } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'
import { parseScopeKbId } from '@/pages/ai/evolution/engineConstants'
import { TaskQueueTab } from '@/pages/ai/evolution/tabs/TaskQueueTab'
import { TopicsTab } from '@/pages/ai/evolution/tabs/TopicsTab'
import { QualityHeatmapTab } from '@/pages/ai/evolution/tabs/QualityHeatmapTab'
import { EvolutionMapTab } from '@/pages/ai/evolution/tabs/EvolutionMapTab'
import { RoiTab } from '@/pages/ai/evolution/tabs/RoiTab'
import { ReviewTab } from '@/pages/ai/evolution/tabs/ReviewTab'

const EVOLUTION_READY_ENDPOINTS = '/ai/knowledge-base/list,/ai/evolution/status,/ai/evolution/execution/execute'
const EVOLUTION_UNSUPPORTED_ENDPOINTS = '/ai/evolution/mock,/ai/evolution/local-status,/ai/evolution/local-execute,/ai/evolution/static-status'

interface EvolveStatusPayload {
  running?: boolean
  circuitBroken?: boolean
  lastRunTime?: unknown
  nextRunTime?: unknown
  message?: string
}

function formatStatusTime(value: unknown) {
  if (value == null || value === '') return '尚无记录'
  return String(value)
}

export default function EvolutionPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [scopeKbId, setScopeKbId] = useState('')
  const [executeError, setExecuteError] = useState<string | null>(null)

  const kbsQuery = useQuery({
    queryKey: ['evolution-kb-options'],
    queryFn: () => aiApi.kbList({ page: 0, rows: 200 }),
  })

  const statusQuery = useQuery({
    queryKey: ['evolve-status'],
    queryFn: () => aiApi.evolveStatus(),
    refetchInterval: 30000,
  })

  const executeMut = useMutation({
    mutationFn: () => aiApi.evolutionExecute({
      agentType: 'all',
      targetKbId: parseScopeKbId(scopeKbId),
    }),
    onSuccess: () => {
      setExecuteError(null)
      toast('进化引擎已启动', 'success')
      qc.invalidateQueries({ queryKey: ['evolve-status'] })
      qc.invalidateQueries({ queryKey: ['quality-score-history'] })
      qc.invalidateQueries({ queryKey: ['evolve-score-trend'] })
      qc.invalidateQueries({ queryKey: ['evolve-roi'] })
      qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
      window.setTimeout(() => qc.invalidateQueries({ queryKey: ['evolve-task-list'] }), 1500)
      window.setTimeout(() => qc.invalidateQueries({ queryKey: ['evolve-task-list'] }), 4000)
      qc.invalidateQueries({ queryKey: ['evolve-topics'] })
      qc.invalidateQueries({ queryKey: ['evolve-topics-map'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setExecuteError(`立即运行失败（POST /ai/evolution/execution/execute）：${message}。当前知识库范围会保留，可修复后重试。`)
      toast(`启动失败：${message}`, 'error')
    },
  })

  const status = (statusQuery.data ?? {}) as EvolveStatusPayload
  const isRunning = status?.running === true
  const circuitBroken = status?.circuitBroken === true

  const onScopeKbChange = (v: string) => {
    setScopeKbId(v)
    qc.invalidateQueries({ queryKey: ['quality-score-history'] })
    qc.invalidateQueries({ queryKey: ['evolve-score-trend'] })
    qc.invalidateQueries({ queryKey: ['evolve-roi'] })
    qc.invalidateQueries({ queryKey: ['evolve-topics'] })
    qc.invalidateQueries({ queryKey: ['evolve-topics-map'] })
    qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
  }

  const kbList: KnowledgeBase[] = Array.isArray(kbsQuery.data) ? kbsQuery.data : []
  const selectedKb = kbList.find(k => String(k.id) === scopeKbId)
  const hasInfraError = kbsQuery.isError || statusQuery.isError
  const runDisabledReason = circuitBroken
    ? '熔断已触发，请先查看后端 evolve status 与任务失败原因'
    : kbsQuery.isError
      ? '知识库列表加载失败，无法确认执行范围'
      : ''

  const refetchHeaderData = () => {
    void kbsQuery.refetch()
    void statusQuery.refetch()
  }

  return (
    <Box
      data-testid="evolution-page"
      data-ready-endpoints={EVOLUTION_READY_ENDPOINTS}
      data-unsupported-endpoints={EVOLUTION_UNSUPPORTED_ENDPOINTS}
      data-no-local-status-fallback="true"
      data-no-local-execute-fallback="true"
      data-no-static-status-fallback="true"
      sx={{ p: 3, bgcolor: 'background.default', minHeight: '100vh' }}
    >
      <PageHeader
        title="进化引擎"
        subtitle="按知识库范围编排质量、时效、缺口、分类、共享与深度进化 Agent；页面顶部展示调度、熔断和数据源状态，Tab 内承接具体任务与指标。"
        breadcrumbs={[{ label: 'AI 中心' }, { label: '进化引擎' }]}
        actions={(
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
            <Link component={RouterLink} to="/admin/ai/knowledge-evolution" variant="body2">
              监控看板
            </Link>
            <Link component={RouterLink} to="/admin/ai/evolution-topics" variant="body2">
              主题池
            </Link>
          </Stack>
        )}
      />

      {hasInfraError ? (
        <Alert
          data-testid="evolution-base-data-error"
          data-ready-endpoints="/ai/evolution/status,/ai/knowledge-base/list"
          data-no-local-status-fallback="true"
          data-no-local-kb-fallback="true"
          severity="error"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={refetchHeaderData}>重试</Button>}
        >
          进化引擎基础数据加载失败：
          {statusQuery.isError ? ' 状态接口' : ''}
          {kbsQuery.isError ? ' 知识库列表' : ''}
          。请检查登录态、后端 `/ai/evolution/status` 与 `/ai/knowledge-base/list`。
        </Alert>
      ) : null}

      {runDisabledReason ? (
        <Alert
          data-testid="evolution-run-disabled-downgrade"
          data-no-local-execute-fallback="true"
          data-input-retained="true"
          severity={circuitBroken ? 'warning' : 'info'}
          sx={{ mb: 2 }}
        >
          {runDisabledReason}
        </Alert>
      ) : null}
      {executeError ? (
        <Alert
          data-testid="evolution-execute-error"
          data-ready-endpoints="/ai/evolution/execution/execute"
          data-no-local-execute-fallback="true"
          data-input-retained="true"
          severity="error"
          sx={{ mb: 2 }}
        >
          {executeError}
        </Alert>
      ) : null}

      <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1.5} sx={{ mb: 2 }}>
        <Grid
          data-testid="evolution-status-cards"
          data-ready-endpoints="/ai/evolution/status,/ai/knowledge-base/list"
          data-no-local-status-fallback="true"
          container
          spacing={1.5}
          sx={{ flex: 1, minWidth: 280 }}
        >
          <Grid item xs={12} sm={6} md={4}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" justifyContent="space-between" spacing={1}>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary">调度状态</Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 0.75 }} flexWrap="wrap" useFlexGap>
                      <Chip label={isRunning ? '运行中' : '未运行'} size="small" color={isRunning ? 'success' : 'default'} />
                      <Chip label={circuitBroken ? '熔断触发' : '熔断正常'} size="small" color={circuitBroken ? 'error' : 'success'} variant="outlined" />
                    </Stack>
                  </Box>
                  <ScheduleIcon color={isRunning ? 'success' : 'disabled'} />
                </Stack>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" justifyContent="space-between" spacing={1}>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary">知识库范围</Typography>
                    <Typography variant="body2" fontWeight={600} noWrap title={selectedKb?.kbName ?? '全部知识库'}>
                      {selectedKb?.kbName ?? '全部知识库'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">可选 {kbList.length} 个知识库</Typography>
                  </Box>
                  <StorageIcon color={kbsQuery.isError ? 'error' : 'primary'} />
                </Stack>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" justifyContent="space-between" spacing={1}>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary">运行窗口</Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      上次：{formatStatusTime(status.lastRunTime)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      下次：{formatStatusTime(status.nextRunTime)}
                    </Typography>
                  </Box>
                  <WarningAmberIcon color={circuitBroken ? 'warning' : 'disabled'} />
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Stack
          data-testid="evolution-scope-run-surface"
          data-ready-endpoints="/ai/knowledge-base/list,/ai/evolution/execution/execute"
          data-no-local-execute-fallback="true"
          direction="row"
          spacing={1}
          alignItems="center"
          flexWrap="wrap"
          useFlexGap
          sx={{ justifyContent: 'flex-end' }}
        >
          <FormControl size="small" sx={{ minWidth: 200, '& .MuiOutlinedInput-root': { bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)' } }}>
            <InputLabel id="ev-scope-kb">知识库范围</InputLabel>
            <Select
              labelId="ev-scope-kb"
              label="知识库范围"
              value={scopeKbId}
              onChange={e => onScopeKbChange(e.target.value)}
            >
              <MenuItem value="">全部知识库</MenuItem>
              {kbList.map(k => (
                <MenuItem key={k.id} value={String(k.id)}>{k.kbName ?? `KB #${k.id}`}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Tooltip describeChild title={runDisabledReason || '对选中知识库立即运行全部 Agent（未选则运行全部知识库范围）'}>
            <Button data-testid="evolution-run-action-surface" variant="contained" startIcon={<PlayArrowIcon />}
              onClick={() => executeMut.mutate()} disabled={executeMut.isPending || Boolean(runDisabledReason)}
              sx={(theme) => ({
                bgcolor: theme.palette.primary.main,
                color: theme.palette.primary.contrastText,
                '&:hover': { bgcolor: theme.palette.primary.dark },
              })}>
              立即运行
            </Button>
          </Tooltip>
          <IconButton aria-label="刷新进化状态" onClick={refetchHeaderData}>
            <RefreshIcon />
          </IconButton>
        </Stack>
      </Stack>

      {status.message ? (
        <Alert
          data-testid="evolution-status-message"
          data-ready-endpoints="/ai/evolution/status"
          data-no-static-status-fallback="true"
          severity="info"
          sx={{ mb: 2 }}
        >
          后端状态说明：{String(status.message)}
        </Alert>
      ) : null}

      <Tabs
        data-testid="evolution-tab-host"
        data-no-page-level-tab-api="true"
        value={tab}
        onChange={(_, v) => setTab(v)}
        sx={{ mb: 2, borderBottom: 1, borderColor: 'var(--color-surface-light)' }}
      >
        <Tab label="任务队列" />
        <Tab label="主题池" />
        <Tab label="质量热力图" />
        <Tab label="进化地图" />
        <Tab label="ROI 指标" />
        <Tab label="进化审核" />
      </Tabs>

      <Box sx={{ minHeight: 400 }}>
        {tab === 0 && <TaskQueueTab scopeKbId={scopeKbId} />}
        {tab === 1 && <TopicsTab scopeKbId={scopeKbId} />}
        {tab === 2 && <QualityHeatmapTab scopeKbId={scopeKbId} />}
        {tab === 3 && <EvolutionMapTab scopeKbId={scopeKbId} />}
        {tab === 4 && <RoiTab scopeKbId={scopeKbId} />}
        {tab === 5 && <ReviewTab />}
      </Box>
    </Box>
  )
}
