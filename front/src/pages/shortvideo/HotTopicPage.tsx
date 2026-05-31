import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Typography, Stack, Card, CardContent, Chip, Button,
  Grid, IconButton, Tooltip, CircularProgress, Alert,
  TextField, Dialog, DialogTitle, DialogContent, DialogActions,
  Paper, Divider, FormControl, InputLabel, Select, MenuItem, Badge,
  useTheme,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import BookmarkIcon from '@mui/icons-material/Bookmark'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import { useQuery, useMutation } from '@tanstack/react-query'
import { trendsCurrent, type TrendTopic, type DouyinOfficialReference } from '@/api/shortvideo'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import ReactECharts from 'echarts-for-react'
import { PageHeader } from '@/components/base'
import OfficialReferencesPanel from '@/components/OfficialReferencesPanel'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'

const SOURCE_CHIPS = [
  { label: '全部', value: '' },
  { label: '天API', value: '天API' },
  { label: '抖音热榜', value: '抖音热榜' },
  { label: '微博', value: '微博' },
  { label: 'B站', value: 'B站' },
]

const CATEGORIES = ['全部', '护肤', '彩妆', '美容', '时尚', '生活', '成分党']
const HOT_TOPIC_POOL_ENDPOINT = '/short-video/cross/hot-topic-pool'
const AI_GENERATE_SCRIPT_ENDPOINT = '/short-video/ai/generate-script-rich'
const PROJECT_SAVE_ENDPOINT = '/short-video/project/save'
const HOT_TOPIC_READY_ENDPOINTS = [
  HOT_TOPIC_POOL_ENDPOINT,
  AI_GENERATE_SCRIPT_ENDPOINT,
  PROJECT_SAVE_ENDPOINT,
] as const
const HOT_TOPIC_READY_ROUTES = [
  shortvideoRoutes.hotTopics,
  shortvideoRoutes.hotTopicCreate,
  shortvideoRoutes.quickGenerate,
].join('|')
const HOT_TOPIC_SUPPORTED_ACTIONS = [
  'refresh-hot-topic-pool',
  'filter-hot-topics-client-side',
  'generate-hot-topic-idea',
  'save-hot-topic-project',
  'navigate-hot-topic-create',
  'client-only-favorite-hot-topic',
].join('|')
const HOT_TOPIC_UNSUPPORTED_ENDPOINTS = [
  '/short-video/hot-topic/mock',
  '/short-video/hot-topic/local-list',
  '/short-video/hot-topic/static-rank',
  '/short-video/hot-topic/local-save',
  '/short-video/ai/local-generate-script',
  '/short-video/ai/template-script',
  '/short-video/project/local-save',
  '/short-video/hot-topic/favorite/save',
] as const
const RANK_TONES = ['error', 'warning', 'secondary', 'info'] as const

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
  const theme = useTheme()
  const [sourceFilter, setSourceFilter] = useState('')
  const [category, setCategory] = useState('全部')
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [saved, setSaved] = useState<Set<string>>(new Set())
  const [genDialogOpen, setGenDialogOpen] = useState(false)
  const [genKeyword, setGenKeyword] = useState('')
  const [genResult, setGenResult] = useState('')
  const [genRefs, setGenRefs] = useState<DouyinOfficialReference[]>([])
  const [saveError, setSaveError] = useState('')
  const { data: rawData = [], isLoading, isError, error, refetch, dataUpdatedAt } = useQuery({
    queryKey: ['hot-topics-trends', sourceFilter, category],
    queryFn: () => trendsCurrent({ category: category === '全部' ? undefined : category, limit: 50 }),
    refetchInterval: 30 * 60 * 1000,
  })
  const data = normalizeRows<TrendTopic>(rawData)

  const filtered: TrendTopic[] = data.filter(t => {
    const srcOk = !sourceFilter || t.source === sourceFilter
    const catOk = category === '全部' || t.category?.includes(category)
    return srcOk && catOk
  })

  const top5 = filtered.slice(0, 5)
  const rankBarColor = (index: number) => {
    const tone = RANK_TONES[index] ?? 'info'
    return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  }
  const rankBarColors = top5.map((_, index) => rankBarColor(index))
  const chartOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 16, right: 16, bottom: 40, top: 10, containLabel: true },
    xAxis: { type: 'category', data: top5.map(t => t.keyword?.slice(0, 8) ?? ''), axisLabel: { interval: 0, rotate: 15, fontSize: 11 } },
    yAxis: { type: 'value', name: '热度' },
    series: [{
      type: 'bar',
      data: top5.map((t, i) => ({
        value: Number(t.hotScore ?? 0),
        itemStyle: { color: rankBarColor(i) },
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
      if (next.has(word)) {
        next.delete(word)
      } else {
        next.add(word)
      }
      return next
    })
  }

  const handleGenScript = (word: string) => {
    setGenKeyword(word)
    setGenResult('')
    setGenRefs([])
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
    onMutate: () => setSaveError(''),
    onSuccess: () => toast('已保存为短视频项目', 'success'),
    onError: (e, variables) => {
      const message = getErrorMessage(e)
      setSaveError(`保存热点项目失败（POST ${PROJECT_SAVE_ENDPOINT}）：${message}。已选话题和 publishTitle 会保留，不插入本地项目。当前标题：${variables.title}`)
      toast(`保存失败：${message}`, 'error')
    },
  })

  const ideaMut = useMutation({
    mutationFn: (keyword: string) =>
      shortvideoApi.aiGenerateScriptRich({
        copyText: keyword,
        sceneType: '热点借势',
        style: 'trend',
        duration: 60,
      }),
    onSuccess: (result) => {
      setGenResult(result.content)
      setGenRefs(result.officialReferences ?? [])
      toast('脚本创意已生成', 'success')
    },
    onError: (e) => toast(`脚本创意生成失败：${getErrorMessage(e)}`, 'error'),
  })

  const handleGenerateIdea = () => {
    if (!genKeyword.trim()) return
    setGenResult('')
    setGenRefs([])
    ideaMut.mutate(genKeyword.trim())
  }

  const lastUpdate = dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--'

  return (
    <Box
      sx={{ p: 3 }}
      data-testid="hot-topic-page"
      data-contract-scope="shortvideo-hot-topic"
      data-ready-endpoints={HOT_TOPIC_READY_ENDPOINTS.join('|')}
      data-ready-routes={HOT_TOPIC_READY_ROUTES}
      data-supported-actions={HOT_TOPIC_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={HOT_TOPIC_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-hot-topic-fallback="true"
      data-client-only-favorite="true"
    >
      <PageHeader
        title="热点话题"
        subtitle={`实时热词追踪 · 最后更新 ${lastUpdate}`}
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<AddCircleOutlineIcon />}
              onClick={() => navigate(shortvideoRoutes.hotTopicCreate)}
              data-testid="hot-topic-open-creation-button"
              data-target-route={shortvideoRoutes.hotTopicCreate}
            >
              热点借势创作
            </Button>
            {selected.size > 0 && (
              <Badge badgeContent={selected.size} color="primary">
                <Button
                  variant="outlined"
                  startIcon={<TrendingUpIcon />}
                  onClick={() => saveMut.mutate({ title: `热词项目-${[...selected][0]}`, publishTitle: [...selected].join('、') })}
                  data-testid="hot-topic-save-project-button"
                  data-source-endpoint={PROJECT_SAVE_ENDPOINT}
                >
                  保存为项目
                </Button>
              </Badge>
            )}
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={() => refetch()}
              disabled={isLoading}
              data-testid="hot-topic-refresh-button"
              data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
            >
              刷新
            </Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="hot-topic-boundary-contract"
        data-source-endpoints={HOT_TOPIC_READY_ENDPOINTS.join('|')}
        data-no-local-hot-topic-fallback="true"
        data-no-local-script-template="true"
        data-client-only-favorite="true"
        data-supported-actions={HOT_TOPIC_SUPPORTED_ACTIONS}
      >
        热点池来自 POST {HOT_TOPIC_POOL_ENDPOINT}；脚本创意生成走 POST {AI_GENERATE_SCRIPT_ENDPOINT}；保存为项目走 POST {PROJECT_SAVE_ENDPOINT}。收藏当前仅为页面内临时标记，后端尚未提供热点收藏表。
      </Alert>
      {isError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          data-testid="hot-topic-list-error"
          data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
          data-no-local-hot-topic-fallback="true"
          data-input-retained="true"
        >
          热点池加载失败（POST {HOT_TOPIC_POOL_ENDPOINT}）：{getErrorMessage(error)}。请检查热点同步任务；页面不会展示本地模拟热榜。
        </Alert>
      )}
      {saveError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setSaveError('')}
          data-testid="hot-topic-project-save-error"
          data-source-endpoint={PROJECT_SAVE_ENDPOINT}
          data-no-local-project-create="true"
          data-input-retained="true"
        >
          {saveError}
        </Alert>
      )}

      {/* 来源筛选 */}
      <Stack
        direction="row"
        spacing={1}
        sx={{ mb: 2, flexWrap: 'wrap' }}
        data-testid="hot-topic-filter-contract"
        data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
        data-client-filter-only="true"
      >
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
                <Box
                  data-testid="hot-topic-rank-chart-surface"
                  data-chart-colors={rankBarColors.join('|')}
                  data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
                  data-no-static-rank="true"
                >
                  <ReactECharts option={chartOption} style={{ height: 220 }} />
                </Box>
              )}
            </CardContent>
          </Card>

          {/* 已选话题 */}
          {selected.size > 0 && (
            <Paper
              sx={{ p: 2, mt: 2 }}
              data-testid="hot-topic-selected-topics"
              data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
              data-input-retained="true"
            >
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
            <Paper
              sx={{ p: 4, textAlign: 'center' }}
              data-testid="hot-topic-empty"
              data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
              data-no-local-hot-topic-fallback="true"
            >
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
                  data-testid="hot-topic-row-card"
                  data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
                  data-client-only-favorite="true"
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
                            color={saved.has(t.keyword) ? 'primary' : 'default'}
                            data-testid="hot-topic-favorite-button">
                            <BookmarkIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="生成脚本创意">
                          <IconButton size="small" onClick={e => { e.stopPropagation(); handleGenScript(t.keyword) }} data-testid="hot-topic-generate-idea-button">
                            <AutoAwesomeIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="加入项目">
                          <IconButton size="small" onClick={e => {
                            e.stopPropagation()
                            saveMut.mutate({ title: `热词-${t.keyword}`, publishTitle: t.keyword })
                          }} data-testid="hot-topic-inline-save-button">
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
          {ideaMut.isPending ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 2 }}>
              <CircularProgress size={20} />
              <Typography>AI 脚本创意生成中…</Typography>
            </Box>
          ) : genResult ? (
            <Stack spacing={1.5}>
              <Paper sx={{ p: 2, bgcolor: 'action.hover', whiteSpace: 'pre-wrap', fontSize: 13 }}>
                {genResult}
              </Paper>
              <OfficialReferencesPanel
                testId="hot-topic-official-references"
                endpoint="/short-video/ai/generate-script-rich"
                references={genRefs}
                maxItems={6}
              />
            </Stack>
          ) : (
            <Alert severity="info">生成结果将来自真实短视频 AI 脚本接口，不再使用页面内固定模板。</Alert>
          )}
          {ideaMut.isError && (
            <Alert
              severity="error"
              sx={{ mt: 2 }}
              data-testid="hot-topic-idea-error"
              data-source-endpoint={AI_GENERATE_SCRIPT_ENDPOINT}
              data-no-local-script-template="true"
              data-input-retained="true"
            >
              脚本创意生成失败（POST {AI_GENERATE_SCRIPT_ENDPOINT}）：{getErrorMessage(ideaMut.error)}。关键词会保留，不回退到页面内固定模板。
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenDialogOpen(false)}>关闭</Button>
          <Button variant="outlined" startIcon={<ContentCopyIcon />}
            disabled={!genResult}
            data-testid="hot-topic-copy-idea-button"
            onClick={() => { navigator.clipboard.writeText(genResult); toast('已复制', 'success') }}>
            复制
          </Button>
          <Button variant="contained" startIcon={<AutoAwesomeIcon />}
            onClick={handleGenerateIdea} disabled={ideaMut.isPending || !genKeyword} data-testid="hot-topic-generate-idea-confirm-button" data-source-endpoint={AI_GENERATE_SCRIPT_ENDPOINT}>
            {ideaMut.isPending ? '生成中...' : '生成创意'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
