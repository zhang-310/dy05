import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Typography, Stack, Card, CardContent, Chip, Button,
  Grid, IconButton, Tooltip, CircularProgress,
  TextField, Dialog, DialogTitle, DialogContent, DialogActions,
  Paper, Divider, FormControl, InputLabel, Select, MenuItem, Badge,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import BookmarkIcon from '@mui/icons-material/Bookmark'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import { useQuery, useMutation } from '@tanstack/react-query'
import { trendsCurrent, type TrendTopic } from '@/api/shortvideo'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import ReactECharts from 'echarts-for-react'
import { PageHeader } from '@/components/base'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const SOURCE_CHIPS = [
  { label: '全部', value: '' },
  { label: '天API', value: '天API' },
  { label: '抖音热榜', value: '抖音热榜' },
  { label: '微博', value: '微博' },
  { label: 'B站', value: 'B站' },
]

const CATEGORIES = ['全部', '护肤', '彩妆', '美容', '时尚', '生活', '成分党']

const COMPETITION_COLOR: Record<string, 'success' | 'warning' | 'error'> = {
  '低': 'success',
  '中': 'warning',
  '高': 'error',
}

function formatHeat(heat: number): string {
  if (heat >= 10000) return `${(heat / 10000).toFixed(0)}万`
  if (heat >= 1000) return `${(heat / 1000).toFixed(1)}k`
  return String(heat)
}

export default function HotTopicPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [sourceFilter, setSourceFilter] = useState('')
  const [category, setCategory] = useState('全部')
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [saved, setSaved] = useState<Set<string>>(new Set())
  const [genDialogOpen, setGenDialogOpen] = useState(false)
  const [genKeyword, setGenKeyword] = useState('')
  const [genResult, setGenResult] = useState('')
  const [generating, setGenerating] = useState(false)
  const { data = [], isLoading, refetch, dataUpdatedAt } = useQuery({
    queryKey: ['hot-topics-trends', sourceFilter, category],
    queryFn: () => trendsCurrent({ category: category === '全部' ? undefined : category, limit: 50 }),
    refetchInterval: 30 * 60 * 1000,
  })

  const filtered: TrendTopic[] = data.filter(t => {
    const srcOk = !sourceFilter || t.source === sourceFilter
    const catOk = category === '全部' || t.category?.includes(category)
    return srcOk && catOk
  })

  const top5 = filtered.slice(0, 5)
  const chartOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 16, right: 16, bottom: 40, top: 10, containLabel: true },
    xAxis: { type: 'category', data: top5.map(t => t.keyword?.slice(0, 8) ?? ''), axisLabel: { interval: 0, rotate: 15, fontSize: 11 } },
    yAxis: { type: 'value', name: '热度' },
    series: [{
      type: 'bar',
      data: top5.map((t, i) => ({
        value: Number(t.hotScore ?? 0),
        itemStyle: { color: i === 0 ? '#f44336' : i === 1 ? '#ff5722' : i === 2 ? '#ff9800' : '#42a5f5' },
      })),
      label: { show: true, position: 'top', fontSize: 10, formatter: (v: { value: number }) => formatHeat(v.value) },
    }],
  }

  const handleCopy = (word: string) => {
    navigator.clipboard.writeText(word)
    toast(`已复制「${word}」`, 'success')
  }

  const handleSave = (word: string) => {
    setSaved(prev => new Set([...prev, word]))
    toast(`已收藏「${word}」`, 'success')
  }

  const handleSelect = (word: string) => {
    setSelected(prev => {
      const next = new Set(prev)
      next.has(word) ? next.delete(word) : next.add(word)
      return next
    })
  }

  const handleGenScript = (word: string) => {
    setGenKeyword(word)
    setGenResult('')
    setGenDialogOpen(true)
  }

  const saveMut = useMutation({
    mutationFn: (params: { title: string; publishTitle?: string }) =>
      shortvideoApi.save({
        title: params.title,
        projectType: 'viral_clone',
        status: 'draft',
        publishTitle: params.publishTitle?.trim()
          ? params.publishTitle.trim().slice(0, 255)
          : undefined,
      }),
    onSuccess: () => toast('已保存为短视频项目', 'success'),
    onError: () => toast('保存失败', 'error'),
  })

  const handleGenerateIdea = async () => {
    setGenerating(true)
    setGenResult('')
    try {
      await new Promise(r => setTimeout(r, 1200))
      setGenResult(`基于热词「${genKeyword}」的短视频创意：\n\n1. 开场钩子：用「你知道${genKeyword}最新趋势吗？」引发好奇\n2. 核心内容：结合品牌护肤理念，深度解析${genKeyword}的行业背景\n3. 互动设计：提问观众「你对${genKeyword}有什么看法？」增加评论\n4. 结尾转化：引导关注账号，预告下期${genKeyword}系列内容\n\n建议时长：60秒（前5秒强钩子 + 中段干货 + 后5秒行动引导）`)
    } finally {
      setGenerating(false)
    }
  }

  const lastUpdate = dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--'

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="热点话题"
        subtitle={`实时热词追踪 · 最后更新 ${lastUpdate}`}
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<AddCircleOutlineIcon />} onClick={() => navigate(shortvideoRoutes.hotTopicCreate)}>
              热点借势创作
            </Button>
            {selected.size > 0 && (
              <Badge badgeContent={selected.size} color="primary">
                <Button variant="outlined" startIcon={<TrendingUpIcon />}
                  onClick={() => saveMut.mutate({ title: `热词项目-${[...selected][0]}`, publishTitle: [...selected].join('、') })}>
                  保存为项目
                </Button>
              </Badge>
            )}
            <Button variant="contained" startIcon={<RefreshIcon />} onClick={() => refetch()} disabled={isLoading}>
              刷新
            </Button>
          </Stack>
        }
      />

      {/* 来源筛选 */}
      <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
        {SOURCE_CHIPS.map(c => (
          <Chip key={c.value} label={c.label}
            color={sourceFilter === c.value ? 'primary' : 'default'}
            onClick={() => setSourceFilter(c.value)}
            sx={{ cursor: 'pointer' }}
          />
        ))}
        <Divider orientation="vertical" flexItem />
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>品类</InputLabel>
          <Select value={category} label="品类" onChange={e => setCategory(e.target.value as string)}>
            {CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </Select>
        </FormControl>
      </Stack>

      <Grid container spacing={3}>
        {/* 左侧：热度图表 */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>
                <LocalFireDepartmentIcon fontSize="small" sx={{ mr: 0.5, color: 'error.main', verticalAlign: 'middle' }} />
                TOP5 热度排行
              </Typography>
              {isLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
              ) : (
                <ReactECharts option={chartOption} style={{ height: 220 }} />
              )}
            </CardContent>
          </Card>

          {/* 已选话题 */}
          {selected.size > 0 && (
            <Paper sx={{ p: 2, mt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>已选话题（{selected.size}）</Typography>
              <Stack direction="row" flexWrap="wrap" gap={1}>
                {[...selected].map(w => (
                  <Chip key={w} label={w} onDelete={() => handleSelect(w)} color="primary" size="small" />
                ))}
              </Stack>
            </Paper>
          )}
        </Grid>

        {/* 右侧：话题卡片列表 */}
        <Grid item xs={12} md={8}>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>
          ) : filtered.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center' }}>
              <Typography color="text.secondary">暂无热点数据</Typography>
            </Paper>
          ) : (
            <Stack spacing={1.5}>
              {filtered.map((t, idx) => (
                <Card key={t.keyword}
                  sx={{
                    cursor: 'pointer',
                    border: selected.has(t.keyword) ? 2 : 1,
                    borderColor: selected.has(t.keyword) ? 'primary.main' : 'divider',
                    transition: 'border-color 0.2s',
                  }}
                  onClick={() => handleSelect(t.keyword)}
                >
                  <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Stack direction="row" alignItems="flex-start" spacing={1}>
                      <Typography variant="h6" color={idx < 3 ? 'error.main' : 'text.secondary'}
                        sx={{ minWidth: 28, fontWeight: 700 }}>
                        {idx + 1}
                      </Typography>
                      <Box flex={1}>
                        <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
                          <Typography variant="body1" fontWeight={600}>{t.keyword}</Typography>
                          {t.source && <Chip label={t.source} size="small" variant="outlined" />}
                          {t.competition && (
                            <Chip label={`竞争度:${t.competition}`} size="small"
                              color={COMPETITION_COLOR[t.competition] ?? 'default'} />
                          )}
                          {saved.has(t.keyword) && <Chip label="已收藏" size="small" color="success" />}
                        </Stack>
                        <Stack direction="row" spacing={2} sx={{ mt: 0.5 }}>
                          <Typography variant="caption" color="text.secondary">
                            热度 {formatHeat(Number(t.hotScore ?? 0))}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            视频 {formatHeat(Number(t.videoCount ?? 0))}
                          </Typography>
                          {(t.growthRate ?? 0) > 0 && (
                            <Typography variant="caption" color="success.main">
                              +{t.growthRate}% 增长
                            </Typography>
                          )}
                        </Stack>
                        {t.relatedKeywords?.length > 0 && (
                          <Stack direction="row" spacing={0.5} sx={{ mt: 0.5 }} flexWrap="wrap">
                            {t.relatedKeywords.slice(0, 3).map(kw => (
                              <Chip key={kw} label={kw} size="small" sx={{ height: 18, fontSize: 11 }} />
                            ))}
                          </Stack>
                        )}
                      </Box>
                      <Stack direction="row" spacing={0.5}>
                        <Tooltip title="复制话题">
                          <IconButton size="small" onClick={e => { e.stopPropagation(); handleCopy(t.keyword) }}>
                            <ContentCopyIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="收藏">
                          <IconButton size="small" onClick={e => { e.stopPropagation(); handleSave(t.keyword) }}
                            color={saved.has(t.keyword) ? 'primary' : 'default'}>
                            <BookmarkIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="生成脚本创意">
                          <IconButton size="small" onClick={e => { e.stopPropagation(); handleGenScript(t.keyword) }}>
                            <AutoAwesomeIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="加入项目">
                          <IconButton size="small" onClick={e => {
                            e.stopPropagation()
                            saveMut.mutate({ title: `热词-${t.keyword}`, publishTitle: t.keyword })
                          }}>
                            <AddCircleOutlineIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </Stack>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          )}
        </Grid>
      </Grid>

      {/* 生成脚本创意弹窗 */}
      <Dialog open={genDialogOpen} onClose={() => setGenDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <AutoAwesomeIcon sx={{ mr: 1, verticalAlign: 'middle', color: 'primary.main' }} />
          基于「{genKeyword}」生成脚本创意
        </DialogTitle>
        <DialogContent>
          <TextField
            label="关键词"
            value={genKeyword}
            onChange={e => setGenKeyword(e.target.value)}
            fullWidth size="small" sx={{ mb: 2, mt: 1 }}
          />
          {generating ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 2 }}>
              <CircularProgress size={20} />
              <Typography>AI 创意生成中…</Typography>
            </Box>
          ) : genResult ? (
            <Paper sx={{ p: 2, bgcolor: 'action.hover', whiteSpace: 'pre-wrap', fontSize: 13 }}>
              {genResult}
            </Paper>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenDialogOpen(false)}>关闭</Button>
          <Button variant="outlined" startIcon={<ContentCopyIcon />}
            disabled={!genResult}
            onClick={() => { navigator.clipboard.writeText(genResult); toast('已复制', 'success') }}>
            复制
          </Button>
          <Button variant="contained" startIcon={<AutoAwesomeIcon />}
            onClick={handleGenerateIdea} disabled={generating || !genKeyword}>
            生成创意
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}