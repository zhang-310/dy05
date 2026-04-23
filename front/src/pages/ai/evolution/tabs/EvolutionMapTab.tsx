import { useState } from 'react'
import {
  Box, Button, Card, CardContent, Grid, IconButton, Paper, Stack, Typography, Alert,
} from '@mui/material'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import DeleteIcon from '@mui/icons-material/Delete'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type { AiEvolveTopicVO } from '@/types/ai'
import { displayTopicTier } from '@/pages/ai/evolution/topicPriority'

export interface EvolutionMapTabProps {
  scopeKbId: string
}

export function EvolutionMapTab({ scopeKbId }: EvolutionMapTabProps) {
  const [selectedNode, setSelectedNode] = useState<AiEvolveTopicVO | null>(null)

  const { data: topicsRaw } = useQuery({
    queryKey: ['evolve-topics-map', scopeKbId || 'all'],
    queryFn: () => aiApi.topicList(scopeKbId ? { kbId: Number(scopeKbId) } : {}),
  })

  const topics = topicsRaw ?? []

  const NODE_COLORS: Record<string, string> = {
    active: '#1976d2',
    gap: '#ff9800',
    expired: '#f44336',
    new: '#e0e0e0',
  }

  const nodes = topics.map((t, i) => {
    const tier = displayTopicTier(t.priority)
    return {
      id: String(t.id ?? i),
      name: String(t.topicName ?? t.topic ?? `主题${i + 1}`),
      symbolSize: 40 + (tier === 1 ? 20 : 0),
      itemStyle: { color: NODE_COLORS[String(t.status === 1 ? 'active' : t.status === 0 ? 'expired' : 'new')] },
      value: t,
    }
  })

  const edges = topics.slice(0, Math.max(0, topics.length - 1)).map((t, i) => ({
    source: String(t.id ?? i),
    target: String((topics[i + 1] as AiEvolveTopicVO)?.id ?? (i + 1)),
    lineStyle: { width: 1 + (i % 3), opacity: 0.55 },
  }))

  const graphOption = {
    tooltip: { formatter: (p: { name?: string }) => String(p.name ?? '') },
    series: [{
      type: 'graph',
      layout: 'force',
      force: { repulsion: 200, edgeLength: 120 },
      roam: true,
      label: { show: true, position: 'bottom', fontSize: 11 },
      nodes,
      edges,
      emphasis: { focus: 'adjacency' },
    }],
  }

  return (
    <Box>
      <Alert severity="info" sx={{ mb: 2, bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)' }}>
        连线仅按主题列表顺序示意，不代表真实知识依赖；用于快速总览主题分布。
      </Alert>
      <Stack direction="row" spacing={1} mb={2} alignItems="center">
        <AccountTreeIcon fontSize="small" sx={{ color: 'var(--color-primary)' }} />
        <Typography variant="subtitle2" sx={{ color: 'var(--color-text-primary)' }}>主题示意拓扑（力导向）</Typography>
        <Box sx={{ flex: 1 }} />
        <Stack direction="row" spacing={1}>
          {[{ color: '#1976d2', label: '核心知识' }, { color: '#ff9800', label: '缺口待补' },
            { color: '#f44336', label: '过期待更新' }, { color: '#e0e0e0', label: '新增' }].map(n => (
            <Stack key={n.label} direction="row" spacing={0.5} alignItems="center">
              <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: n.color }} />
              <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>{n.label}</Typography>
            </Stack>
          ))}
        </Stack>
        <Button size="small" startIcon={<FileDownloadIcon />} variant="outlined"
          onClick={() => {
            const el = document.querySelector('.evolution-map-chart canvas') as HTMLCanvasElement | null
            if (!el) return
            const a = document.createElement('a')
            a.download = 'evolution-map.png'
            a.href = el.toDataURL('image/png')
            a.click()
          }}>导出图片</Button>
      </Stack>
      {topics.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 6, textAlign: 'center', bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
          <Typography sx={{ color: 'var(--color-text-secondary)' }}>暂无主题数据，请先在主题池中添加进化主题</Typography>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          <Grid item xs={12} md={selectedNode ? 8 : 12}>
            <Card variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              <CardContent sx={{ p: 1 }}>
                <ReactECharts
                  className="evolution-map-chart"
                  option={graphOption}
                  style={{ height: 480 }}
                  onEvents={{ click: (p: { data?: { value?: AiEvolveTopicVO } }) => {
                    const v = p.data?.value
                    if (v) setSelectedNode(v)
                  }}}
                />
              </CardContent>
            </Card>
          </Grid>
          {selectedNode && (
            <Grid item xs={12} md={4}>
              <Card variant="outlined" sx={{ height: '100%', bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
                    <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'var(--color-text-primary)' }}>
                      {String(selectedNode.topicName ?? selectedNode.topic ?? '主题详情')}
                    </Typography>
                    <IconButton size="small" onClick={() => setSelectedNode(null)}><DeleteIcon fontSize="small" /></IconButton>
                  </Stack>
                  <Stack spacing={1}>
                    <Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}>
                      <strong>分类：</strong>{String(selectedNode.category ?? '—')}
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}>
                      <strong>优先级：</strong>P{displayTopicTier(selectedNode.priority)}
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}>
                      <strong>知识库：</strong>{selectedNode.kbId ?? '—'}
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}><strong>状态：</strong>
                      <Typography component="span" variant="body2" sx={{ ml: 0.5 }}>
                        {selectedNode.status === 1 ? '启用' : '停用'}
                      </Typography>
                    </Typography>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          )}
        </Grid>
      )}
    </Box>
  )
}
