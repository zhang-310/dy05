import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Chip,
  TextField, Button, CircularProgress, Alert, Grid, Divider,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import { useMutation } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import ReactECharts from 'echarts-for-react'

interface ChainNode {
  id: string; title: string; playCount: number; likeCount: number
  author: string; coverUrl?: string; level: number; parentId?: string
}

export default function ViralChainPage() {
  const toast = useToast()
  const [viralVideoId, setViralVideoId] = useState('')
  const [chain, setChain] = useState<ChainNode[]>([])
  const [stats, setStats] = useState<Record<string, unknown>>({})

  const analyzeMut = useMutation({
    mutationFn: () => shortvideoApi.viralAnalyze(Number(viralVideoId)),
    onSuccess: () => {
      toast('已触发爆款 AI 分析任务', 'success')
      setChain([])
      setStats({})
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const chartNodes = chain.map(n => ({
    id: n.id, name: n.title?.slice(0, 12) ?? n.id,
    symbolSize: Math.max(20, Math.log10(n.playCount + 1) * 10),
    value: n.playCount,
    itemStyle: { color: n.level === 0 ? '#1976d2' : n.level === 1 ? '#42a5f5' : '#90caf9' },
  }))

  const chartLinks = chain
    .filter(n => n.parentId)
    .map(n => ({ source: n.parentId!, target: n.id }))

  const chartOption = {
    tooltip: { formatter: (p: { data?: { name?: string; value?: number } }) => `${p.data?.name}<br/>播放: ${(p.data?.value ?? 0).toLocaleString()}` },
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      data: chartNodes, links: chartLinks,
      label: { show: true, fontSize: 10 },
      lineStyle: { curveness: 0.1, color: '#ccc' },
      force: { repulsion: 300, gravity: 0.1 },
    }],
  }

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6" fontWeight={600}>爆款传播链分析</Typography>
      <Alert severity="info" sx={{ mb: 0 }}>传播链图谱需后端专用接口；当前按钮对应 <code>/short-video/viral/analyze</code>，仅触发 AI 分析。</Alert>

      <Card variant="outlined">
        <CardContent>
          <Stack direction="row" spacing={1}>
            <TextField
              size="small" label="爆款视频 ID（库内）" value={viralVideoId}
              onChange={e => setViralVideoId(e.target.value)}
              placeholder="数字 ID，来自爆款列表"
              sx={{ flex: 1 }}
            />
            <Button variant="contained" startIcon={<SearchIcon />}
              onClick={() => analyzeMut.mutate()}
              disabled={!viralVideoId.trim() || analyzeMut.isPending}>
              {analyzeMut.isPending ? '提交中...' : '触发分析'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {analyzeMut.isPending && <CircularProgress sx={{ mx: 'auto' }} />}

      {chain.length > 0 && (
        <>
          <Grid container spacing={2}>
            {[
              { label: '传播节点数', value: chain.length },
              { label: '最大传播层级', value: String(stats.maxLevel ?? '--') },
              { label: '总播放量', value: Number(stats.totalPlay ?? 0).toLocaleString() },
              { label: '改编视频数', value: String(stats.remakeCount ?? '--') },
            ].map(kpi => (
              <Grid item xs={12} sm={6} md={3} key={kpi.label}>
                <Card variant="outlined"><CardContent sx={{ py: 1.5 }}>
                  <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
                  <Typography variant="h5" fontWeight={700}>{kpi.value}</Typography>
                </CardContent></Card>
              </Grid>
            ))}
          </Grid>

          <Card variant="outlined">
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                <AccountTreeIcon color="primary" />
                <Typography variant="subtitle2" fontWeight={600}>传播链图谱</Typography>
              </Stack>
              <ReactECharts option={chartOption} style={{ height: 480 }} />
            </CardContent>
          </Card>

          <Divider />
          <Typography variant="subtitle2" fontWeight={600}>传播节点列表</Typography>
          <Stack spacing={1}>
            {chain.slice(0, 20).map(n => (
              <Card key={n.id} variant="outlined">
                <CardContent sx={{ py: 1 }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="body2" fontWeight={600} noWrap>{n.title}</Typography>
                      <Typography variant="caption" color="text.secondary">{n.author} · 层级 {n.level}</Typography>
                    </Box>
                    <Stack direction="row" spacing={1}>
                      <Chip label={`播放 ${(n.playCount ?? 0).toLocaleString()}`} size="small" />
                      <Chip label={`点赞 ${(n.likeCount ?? 0).toLocaleString()}`} size="small" color="error" variant="outlined" />
                    </Stack>
                  </Stack>
                </CardContent>
              </Card>
            ))}
          </Stack>
        </>
      )}

      {!analyzeMut.isPending && analyzeMut.isSuccess && chain.length === 0 && (
        <Alert severity="info">分析任务已提交；传播链可视化待后端提供图谱接口后展示。</Alert>
      )}
    </Box>
  )
}
