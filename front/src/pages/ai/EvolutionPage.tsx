import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import {
  Box, Button, Chip, Tabs, Tab, Stack, Typography, IconButton, Tooltip,
  FormControl, InputLabel, Select, MenuItem, Link,
} from '@mui/material'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import PauseIcon from '@mui/icons-material/Pause'
import RefreshIcon from '@mui/icons-material/Refresh'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { KnowledgeBase } from '@/api/ai'
import type { EvolveStatusPayload } from '@/types/evolutionEngine'
import { parseScopeKbId } from '@/pages/ai/evolution/engineConstants'
import { TaskQueueTab } from '@/pages/ai/evolution/tabs/TaskQueueTab'
import { TopicsTab } from '@/pages/ai/evolution/tabs/TopicsTab'
import { QualityHeatmapTab } from '@/pages/ai/evolution/tabs/QualityHeatmapTab'
import { EvolutionMapTab } from '@/pages/ai/evolution/tabs/EvolutionMapTab'
import { RoiTab } from '@/pages/ai/evolution/tabs/RoiTab'
import { ReviewTab } from '@/pages/ai/evolution/tabs/ReviewTab'

export default function EvolutionPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [scopeKbId, setScopeKbId] = useState('')

  const { data: kbs } = useQuery({
    queryKey: ['evolution-kb-options'],
    queryFn: () => aiApi.kbList({ page: 0, rows: 200 }),
  })

  const { data: statusData } = useQuery({
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
    onError: () => toast('启动失败', 'error'),
  })

  const status = statusData as EvolveStatusPayload | undefined
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

  const kbList = (kbs ?? []) as KnowledgeBase[]

  return (
    <Box sx={{ p: 3, bgcolor: 'var(--color-surface-dark)', minHeight: '100vh' }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2} flexWrap="wrap" gap={1}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap>
            <Typography variant="h5" fontWeight={700} sx={{ color: 'var(--color-text-primary)' }}>进化引擎</Typography>
            <Link component={RouterLink} to="/admin/ai/knowledge-evolution" variant="caption" sx={{ alignSelf: 'flex-end' }}>
              进化监控看板
            </Link>
            <Link component={RouterLink} to="/admin/ai/evolution-topics" variant="caption" sx={{ alignSelf: 'flex-end' }}>
              主题池（独立页）
            </Link>
          </Stack>
          <Stack direction="row" spacing={2} mt={0.5} flexWrap="wrap" useFlexGap alignItems="center">
            <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>
              上次运行: {String(status?.lastRunTime ?? '—')} &nbsp; 下次运行: {String(status?.nextRunTime ?? '—')}
            </Typography>
            <Chip
              label={isRunning ? '🟢 运行中' : '⚫ 已停止'}
              size="small"
              color={isRunning ? 'success' : 'default'}
            />
            <Chip
              label={circuitBroken ? '🔴 熔断触发' : '✅ 熔断正常'}
              size="small"
              color={circuitBroken ? 'error' : 'success'}
              variant="outlined"
            />
          </Stack>
        </Box>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
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
          <Tooltip title="对选中知识库立即运行全部 Agent（未选则由后端解析默认 KB）">
            <Button variant="contained" startIcon={<PlayArrowIcon />}
              onClick={() => executeMut.mutate()} disabled={executeMut.isPending}
              sx={{ bgcolor: 'var(--color-primary)', color: '#000', '&:hover': { bgcolor: 'var(--color-primary-dark)' } }}>
              立即运行
            </Button>
          </Tooltip>
          <Tooltip title="引擎暂停需后端调度开关支持，当前为占位">
            <Button variant="outlined" startIcon={<PauseIcon />} onClick={() => toast('暂停接口待与调度器对接', 'info')}>
              暂停引擎
            </Button>
          </Tooltip>
          <IconButton onClick={() => qc.invalidateQueries({ queryKey: ['evolve-status'] })}>
            <RefreshIcon />
          </IconButton>
        </Stack>
      </Stack>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'var(--color-surface-light)' }}>
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
