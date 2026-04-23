import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import {
  Box, Typography, Stack, Card, CardContent, Chip,
  TextField, Button, CircularProgress, Alert, Grid, Divider,
  FormControl, InputLabel, Select, MenuItem, Paper,
  List, ListItem, ListItemText, LinearProgress, IconButton, Tooltip,
  Tabs, Tab, Link, Accordion, AccordionSummary, AccordionDetails,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import PsychologyIcon from '@mui/icons-material/Psychology'
import PersonIcon from '@mui/icons-material/Person'
import HubIcon from '@mui/icons-material/Hub'
import LightbulbIcon from '@mui/icons-material/Lightbulb'
import RefreshIcon from '@mui/icons-material/Refresh'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd'
import BiotechIcon from '@mui/icons-material/Biotech'
import { useQuery, useMutation } from '@tanstack/react-query'
import {
  brainApi,
  type BrainTrendSignal,
  type BrainCognitiveProfile,
  type BrainIndustryInsights,
  type BrainHostPersona,
  type BrainGraphSubgraph,
  type BrainGraphNode,
  type BrainGraphEdge,
} from '@/api/brain'
import { useToast } from '@/contexts/ToastContext'
import { useIndustryBrainStore } from '@/stores/industryBrainStore'
import ReactECharts from 'echarts-for-react'
import { IndustryBrainAdvancedTab } from '@/pages/ai/IndustryBrainAdvancedTab'

/** 行业洞察等业务品类 */
const INSIGHT_CATEGORIES = ['护肤', '彩妆', '美容仪器', '香氛', '个护', '全品类']

/** 与 TrendMonitorServiceImpl 中榜单来源 category 一致：douyin / weibo / network */
const TREND_SOURCE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '全部来源' },
  { value: 'douyin', label: '抖音热搜' },
  { value: 'weibo', label: '微博热搜' },
  { value: 'network', label: '全网热搜' },
]

const SCRIPT_TYPE_PRESETS = ['种草', '促销', '产品介绍', '互动答疑', '逼单转化']
const TIME_SLOT_PRESETS = ['早场', '午场', '晚场', '深夜']

function pushPendingHotKeyword(keyword: string, toast: ReturnType<typeof useToast>) {
  const { pendingHotKeywords, setPendingHotKeywords } = useIndustryBrainStore.getState()
  if (pendingHotKeywords.includes(keyword)) {
    toast('该词已在待注入列表中', 'info')
    return
  }
  setPendingHotKeywords([...pendingHotKeywords, keyword])
  toast('已加入直播话术热词队列（话术页将自动合并）', 'success')
}

function enqueueInsightHotTopics(topics: string[], toast: ReturnType<typeof useToast>) {
  const { pendingHotKeywords, setPendingHotKeywords } = useIndustryBrainStore.getState()
  const merged = [...pendingHotKeywords]
  let added = 0
  for (const w of topics) {
    const s = (w ?? '').trim()
    if (s && !merged.includes(s)) {
      merged.push(s)
      added++
    }
  }
  if (added === 0) {
    toast('没有新的热词可加入队列', 'info')
    return
  }
  setPendingHotKeywords(merged)
  toast(`已将 ${added} 个热点词加入直播话术队列`, 'success')
}

function TrendsTab() {
  const toast = useToast()
  const [source, setSource] = useState('')
  const [keywordContains, setKeywordContains] = useState('')
  const { data: rawTrends = [], isLoading, refetch } = useQuery({
    queryKey: ['brain-trends', source],
    queryFn: () => brainApi.trendsCurrent({
      category: source || undefined,
      limit: 80,
    }),
    refetchInterval: 60000,
  })
  const kw = keywordContains.trim().toLowerCase()
  const trends = kw
    ? rawTrends.filter((t: BrainTrendSignal) =>
        (t.title ?? '').toLowerCase().includes(kw) || (t.description ?? '').toLowerCase().includes(kw))
    : rawTrends

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text)
    toast(`已复制「${text}」`, 'success')
  }

  const top10 = [...trends].sort((a, b) => Number(b.heatScore ?? 0) - Number(a.heatScore ?? 0)).slice(0, 10)

  const chartOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: top10.map((t: BrainTrendSignal) => t.title), axisLabel: { interval: 0, rotate: 24 } },
    yAxis: { type: 'value', name: '热度(归一)' },
    series: [{
      type: 'bar',
      data: top10.map((t: BrainTrendSignal) => t.heatScore ?? 0),
      itemStyle: { color: '#1976d2' },
      label: { show: true, position: 'top', fontSize: 10 },
    }],
  }

  return (
    <Box sx={{ mt: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center" mb={2} flexWrap="wrap" useFlexGap>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>榜单来源</InputLabel>
          <Select value={source} label="榜单来源" onChange={e => setSource(e.target.value)}>
            {TREND_SOURCE_OPTIONS.map(c => <MenuItem key={c.value || 'all'} value={c.value}>{c.label}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField
          size="small"
          label="关键词包含（本地筛选）"
          value={keywordContains}
          onChange={e => setKeywordContains(e.target.value)}
          sx={{ minWidth: 200 }}
        />
        <IconButton size="small" onClick={() => refetch()} disabled={isLoading}>
          <RefreshIcon fontSize="small" />
        </IconButton>
        {isLoading && <CircularProgress size={20} />}
        <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>每 60 秒自动刷新 · 数据来自 TianAPI 等</Typography>
      </Stack>

      {trends.length > 0 && (
        <Card variant="outlined" sx={{ mb: 2 }}>
          <CardContent sx={{ py: 1 }}>
            <Typography variant="caption" color="text.secondary" mb={1} display="block">热度 TOP10</Typography>
            <ReactECharts option={chartOption} style={{ height: 220 }} />
          </CardContent>
        </Card>
      )}

      <Grid container spacing={1.5}>
        {trends.map((t: BrainTrendSignal) => (
          <Grid item xs={12} sm={6} md={4} key={t.id}>
            <Card variant="outlined" sx={{ '&:hover': { boxShadow: 2 } }}>
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="body2" fontWeight={600} noWrap title={t.title}>{t.title}</Typography>
                  </Stack>
                  <Stack direction="row" spacing={0.5}>
                    <Chip label={t.category} size="small" color="primary" variant="outlined" />
                    <Tooltip title="复制">
                      <IconButton size="small" onClick={() => handleCopy(t.title)}><ContentCopyIcon fontSize="small" /></IconButton>
                    </Tooltip>
                    <Tooltip title="加入直播话术热词">
                      <IconButton size="small" color="secondary" onClick={() => pushPendingHotKeyword(t.title, toast)}>
                        <PlaylistAddIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                </Stack>
                <Stack direction="row" alignItems="center" spacing={1} mt={0.5}>
                  <TrendingUpIcon fontSize="small" color="success" />
                  <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }} noWrap>
                    热度 {(t.heatScore ?? 0).toFixed(2)} · {t.source}
                  </Typography>
                </Stack>
                {t.description ? (
                  <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>{t.description}</Typography>
                ) : null}
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      {!isLoading && trends.length === 0 && <Alert severity="info">暂无趋势数据（请检查 TianAPI 与 trend-monitor 配置）</Alert>}
    </Box>
  )
}

function CausalTab() {
  const toast = useToast()
  const [scriptType, setScriptType] = useState('种草')
  const [persona, setPersona] = useState('专业种草')
  const [productType, setProductType] = useState('护肤品')
  const [timeSlot, setTimeSlot] = useState('晚场')
  const [hostCode, setHostCode] = useState('')
  const { data: personas = [] } = useQuery({
    queryKey: ['brain-host-personas-causal'],
    queryFn: () => brainApi.hostPersonas(),
  })
  const inferMut = useMutation({
    mutationFn: () => brainApi.causalInfer({
      scriptType,
      persona,
      productType,
      timeSlot,
      ...(hostCode ? { hostCode } : {}),
    }),
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const r = inferMut.data
  return (
    <Box sx={{ mt: 2 }}>
      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="subtitle2" fontWeight={600} mb={2}>策略要素（与后端贝叶斯 + LLM 因果引擎一致）</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={4}>
              <FormControl size="small" fullWidth>
                <InputLabel>话术类型</InputLabel>
                <Select value={scriptType} label="话术类型" onChange={e => setScriptType(e.target.value)}>
                  {SCRIPT_TYPE_PRESETS.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField size="small" fullWidth label="人设关键词" value={persona} onChange={e => setPersona(e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField size="small" fullWidth label="产品类型" value={productType} onChange={e => setProductType(e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <FormControl size="small" fullWidth>
                <InputLabel>时段</InputLabel>
                <Select value={timeSlot} label="时段" onChange={e => setTimeSlot(e.target.value)}>
                  {TIME_SLOT_PRESETS.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <FormControl size="small" fullWidth>
                <InputLabel>主播 hostCode（可选）</InputLabel>
                <Select value={hostCode} label="主播 hostCode（可选）" onChange={e => setHostCode(e.target.value)}>
                  <MenuItem value="">不指定</MenuItem>
                  {(personas as BrainHostPersona[]).map(p => (
                    <MenuItem key={p.id} value={p.hostCode}>{p.hostName}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="contained"
                onClick={() => inferMut.mutate()}
                disabled={inferMut.isPending}
                startIcon={inferMut.isPending ? <CircularProgress size={16} color="inherit" /> : <PsychologyIcon />}
              >
                推断转化与风险
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      {inferMut.isPending && <LinearProgress sx={{ mb: 2 }} />}
      {r && (
        <Stack spacing={2}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" color="primary" gutterBottom>
                预期转化率 {Math.round((r.expectedConversionRate ?? 0) * 100)}%
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{r.explanation}</Typography>
            </CardContent>
          </Card>
          {(r.keyFactors?.length ?? 0) > 0 && (
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>关键促进因素</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5}>
                  {(r.keyFactors ?? []).map((f, i) => <Chip key={i} label={f} size="small" color="success" variant="outlined" />)}
                </Stack>
              </CardContent>
            </Card>
          )}
          {(r.riskPoints?.length ?? 0) > 0 && (
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>风险点</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5}>
                  {(r.riskPoints ?? []).map((f, i) => <Chip key={i} label={f} size="small" color="warning" variant="outlined" />)}
                </Stack>
              </CardContent>
            </Card>
          )}
        </Stack>
      )}
    </Box>
  )
}

function UserProfileTab() {
  const [accountIdStr, setAccountIdStr] = useState('')
  const accountIdNum = accountIdStr.trim() === '' ? undefined : parseInt(accountIdStr, 10)
  const invalidId = accountIdStr.trim() !== '' && (Number.isNaN(accountIdNum) || accountIdNum! <= 0)

  const { data: profile, isLoading, refetch } = useQuery({
    queryKey: ['brain-user-profile', accountIdNum ?? 'self'],
    queryFn: () => brainApi.userProfile(
      accountIdNum != null && !Number.isNaN(accountIdNum) ? { accountId: accountIdNum } : {}
    ),
    enabled: !invalidId,
  })

  const p = profile as BrainCognitiveProfile | undefined
  const prefEntries = p?.contentPreferences ? Object.entries(p.contentPreferences).filter(([k]) => !k.startsWith('_')) : []

  return (
    <Box sx={{ mt: 2 }}>
      <Alert severity="info" sx={{ mb: 2 }}>
        此处为 <strong>运营者认知画像</strong>（话术偏好、风格标签等），非 C 端受众年龄/性别统计。
      </Alert>
      <Stack direction="row" spacing={2} mb={2} alignItems="center">
        <TextField
          label="查看对象用户 ID（可选，默认当前登录用户）"
          size="small"
          value={accountIdStr}
          onChange={e => setAccountIdStr(e.target.value)}
          error={invalidId}
          helperText={invalidId ? '请输入正整数' : ' '}
          sx={{ width: 360 }}
        />
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => refetch()} disabled={isLoading || invalidId}>刷新</Button>
        {isLoading && <CircularProgress size={20} sx={{ alignSelf: 'center' }} />}
      </Stack>
      {p && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">学习进度（脚本编辑参与）</Typography>
                <LinearProgress variant="determinate" value={Math.min(100, Math.max(0, (p.learningProgress ?? 0) * 100))} sx={{ mt: 1, mb: 0.5 }} />
                <Typography variant="caption">{(p.learningProgress ?? 0).toFixed(2)}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">用户 ID / 更新时间</Typography>
                <Typography variant="body2" fontWeight={600}>{p.userId}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {p.lastUpdatedAt ? new Date(p.lastUpdatedAt).toLocaleString() : '--'}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>内容偏好（话术类型权重）</Typography>
                {prefEntries.length === 0 ? <Typography variant="body2" color="text.secondary">暂无</Typography> : (
                  <Stack spacing={1}>
                    {prefEntries.sort((a, b) => b[1] - a[1]).map(([k, v]) => (
                      <Box key={k}>
                        <Stack direction="row" justifyContent="space-between"><Typography variant="caption">{k}</Typography><Typography variant="caption">{v.toFixed(2)}</Typography></Stack>
                        <LinearProgress variant="determinate" value={Math.min(100, v * 100)} />
                      </Box>
                    ))}
                  </Stack>
                )}
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>表达风格标签</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5}>
                  {(p.expressionStyleTags ?? []).map((tag, i) => <Chip key={i} label={tag} size="small" variant="outlined" />)}
                  {(p.expressionStyleTags ?? []).length === 0 && <Typography variant="body2" color="text.secondary">暂无</Typography>}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>扩展字段</Typography>
                <Typography variant="caption" component="pre" sx={{ whiteSpace: 'pre-wrap', m: 0 }}>
                  {JSON.stringify(p.interactionPattern ?? {}, null, 2)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
      {!isLoading && !invalidId && !p && <Alert severity="info">暂无画像数据</Alert>}
    </Box>
  )
}

/** 后端 subgraph-json 使用 name / sourceNodeId / targetNodeId / confidence，与图表期望的 label / source / target / weight 对齐 */
function normalizeBrainSubgraphPayload(data: BrainGraphSubgraph | undefined): { nodes: BrainGraphNode[]; edges: BrainGraphEdge[] } {
  if (!data) return { nodes: [], edges: [] }
  // 追加后端 subgraph 额外字段（name/confidence/sourceNodeId/targetNodeId）
  type BackendNode = BrainGraphNode & { name?: string; confidence?: number }
  type BackendEdge = BrainGraphEdge & { sourceNodeId?: string; targetNodeId?: string; confidence?: number }
  const nodes: BrainGraphNode[] = (data.nodes ?? []).map((raw) => {
    const n = raw as BackendNode
    const label = n.label || n.name || ''
    const w = n.weight ?? n.confidence ?? 1
    return {
      id: n.id == null ? '' : String(n.id),
      label: label || (n.id == null ? '' : String(n.id)),
      type: n.type || 'unknown',
      weight: Number.isFinite(w) ? w : 1,
    }
  })
  const edges: BrainGraphEdge[] = (data.edges ?? []).map((raw) => {
    const e = raw as BackendEdge
    const src = e.source ?? e.sourceNodeId
    const tgt = e.target ?? e.targetNodeId
    const w = e.weight ?? e.confidence ?? 0.5
    return {
      source: src == null ? '' : String(src),
      target: tgt == null ? '' : String(tgt),
      relation: e.relation || '',
      weight: Number.isFinite(w) ? w : 0.5,
    }
  })
  return { nodes, edges }
}

function KnowledgeGraphTab() {
  const toast = useToast()
  const [searchNode, setSearchNode] = useState('')
  const [nodeFilter, setNodeFilter] = useState('')
  const [graphQuery, setGraphQuery] = useState('美妆 直播')
  const [entityType, setEntityType] = useState('topic')
  const [entityRows, setEntityRows] = useState<Record<string, unknown>[]>([])
  const [ragResult, setRagResult] = useState<{ context: string; hops?: number; available?: boolean } | null>(null)

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['brain-knowledge-graph', graphQuery],
    queryFn: () => brainApi.knowledgeGraphSubgraph({ query: graphQuery.trim() || undefined, limit: 80 }),
  })

  const entityMut = useMutation({
    mutationFn: () => brainApi.knowledgeGraphQuery({
      entityType,
      keyword: graphQuery.trim(),
      limit: 24,
    }),
    onSuccess: (rows) => {
      setEntityRows(rows)
      toast(`检索到 ${rows.length} 条实体`, 'success')
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const ragMut = useMutation({
    mutationFn: () => brainApi.graphRagContext({ query: graphQuery.trim() || ' ', limit: 24 }),
    onSuccess: (r) => {
      setRagResult(r)
      toast('GraphRAG 上下文已更新', 'success')
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const { nodes: rawNodes, edges: rawEdges } = normalizeBrainSubgraphPayload(data)
  const filtered = nodeFilter
    ? rawNodes.filter(n => n.label.includes(nodeFilter) || n.type.includes(nodeFilter))
    : rawNodes
  const filteredIds = new Set(filtered.map(n => n.id))
  const filteredEdges = rawEdges.filter(e => filteredIds.has(e.source) && filteredIds.has(e.target))
  const chartOption = {
    tooltip: { formatter: (p: { data?: { name?: string; value?: number } }) => `${p.data?.name ?? ''}<br/>权重: ${p.data?.value ?? 0}` },
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      data: filtered.map(n => ({ id: n.id, name: n.label, value: n.weight, symbolSize: Math.max(16, n.weight * 3), category: n.type })),
      links: filteredEdges.map(e => ({ source: e.source, target: e.target, name: e.relation })),
      categories: [...new Set(rawNodes.map(n => n.type))].map(t => ({ name: t })),
      label: { show: true, fontSize: 10 },
      lineStyle: { curveness: 0.1, opacity: 0.6 },
      force: { repulsion: 200, gravity: 0.1 },
    }],
    legend: [{ data: [...new Set(rawNodes.map(n => n.type))] }],
  }

  return (
    <Box sx={{ mt: 2 }}>
      <Alert severity="info" sx={{ mb: 2 }}>
        左侧子图：PostgreSQL <code>ai_graph_node</code> / <code>ai_graph_edge</code>，按查询分词匹配实体名展开。右侧实体检索可走 Neo4j。配置键 <code>app.ai.brain.knowledge-graph</code>。
      </Alert>
      <Stack direction="row" spacing={2} mb={2} flexWrap="wrap" useFlexGap alignItems="center">
        <TextField
          label="子图 / 检索 / GraphRAG 查询主题"
          size="small"
          value={graphQuery}
          onChange={e => setGraphQuery(e.target.value)}
          sx={{ width: 280 }}
        />
        <Button variant="contained" size="small" onClick={() => refetch()} disabled={isLoading}>加载子图</Button>
        <TextField
          label="节点筛选"
          size="small"
          value={searchNode}
          onChange={e => setSearchNode(e.target.value)}
          sx={{ width: 180 }}
        />
        <Button variant="outlined" size="small" onClick={() => setNodeFilter(searchNode)}>筛选</Button>
        <Button variant="text" size="small" onClick={() => { setSearchNode(''); setNodeFilter('') }}>重置</Button>
        {isLoading && <CircularProgress size={20} />}
      </Stack>

      <Grid container spacing={2}>
        <Grid item xs={12} md={7}>
          {filtered.length > 0
            ? <ReactECharts option={chartOption} style={{ height: 480 }} />
            : !isLoading && (
              <Alert severity="info">
                当前无子图：库内无图数据、或关键词未命中实体名。请写入 <code>ai_graph_*</code>、在工作台物化关系，或改用与实体名一致的分词。Neo4j 不会自动填充左侧子图。
              </Alert>
            )}
          {filtered.length > 0 && (
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
              显示 {filtered.length} 个节点 / {filteredEdges.length} 条边
              {nodeFilter && `（已筛选：${nodeFilter}）`}
            </Typography>
          )}
        </Grid>
        <Grid item xs={12} md={5}>
          <Card variant="outlined" sx={{ mb: 2 }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="subtitle2" fontWeight={600} gutterBottom>实体检索</Typography>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <FormControl size="small" sx={{ minWidth: 120 }}>
                  <InputLabel>类型</InputLabel>
                  <Select value={entityType} label="类型" onChange={e => setEntityType(e.target.value)}>
                    <MenuItem value="topic">topic</MenuItem>
                    <MenuItem value="concept">concept</MenuItem>
                    <MenuItem value="product">product</MenuItem>
                  </Select>
                </FormControl>
                <Button size="small" variant="outlined" onClick={() => entityMut.mutate()} disabled={entityMut.isPending}>
                  {entityMut.isPending ? <CircularProgress size={18} /> : '检索实体'}
                </Button>
              </Stack>
              <List dense disablePadding sx={{ maxHeight: 200, overflow: 'auto', mt: 1 }}>
                {entityRows.map((row, i) => (
                  <ListItem key={i} disableGutters sx={{ py: 0.25, alignItems: 'flex-start' }}>
                    <ListItemText
                      primaryTypographyProps={{ variant: 'caption', component: 'pre', sx: { whiteSpace: 'pre-wrap', m: 0, fontFamily: 'inherit' } }}
                      primary={JSON.stringify(row, null, 0)}
                    />
                  </ListItem>
                ))}
              </List>
              {entityRows.length === 0 && <Typography variant="caption" color="text.secondary">点击「检索实体」查看结果</Typography>}
            </CardContent>
          </Card>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="subtitle2" fontWeight={600} gutterBottom>GraphRAG 上下文</Typography>
              <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                <Button size="small" variant="contained" onClick={() => ragMut.mutate()} disabled={ragMut.isPending}>
                  {ragMut.isPending ? <CircularProgress size={18} color="inherit" /> : '拉取上下文'}
                </Button>
                {ragResult?.available === false && <Chip size="small" label="图谱不可用" color="warning" />}
                {ragResult?.hops != null && <Chip size="small" label={`max-hops: ${ragResult.hops}`} variant="outlined" />}
                {ragResult?.context != null && (
                  <Chip size="small" label={`${ragResult.context.length} 字`} variant="outlined" />
                )}
              </Stack>
              <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'grey.50', maxHeight: 280, overflow: 'auto' }}>
                <Typography variant="caption" component="pre" sx={{ whiteSpace: 'pre-wrap', m: 0, fontFamily: 'inherit' }}>
                  {ragResult?.context || '点击「拉取上下文」生成多跳路径摘要（供 RAG 拼接）。'}
                </Typography>
              </Paper>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}

const INSIGHT_CARD_ORDER: (keyof BrainIndustryInsights)[] = [
  '行业分类', '趋势热点', '趋势来源', '近7天竞品洞察', '差异化建议', '用户偏好焦点', '优先动作', '数据口径',
]

function renderInsightValue(_k: string, v: unknown) {
  if (Array.isArray(v)) {
    return (
      <List dense disablePadding>
        {(v as string[]).map((item, j) => (
          <ListItem key={j} disableGutters sx={{ py: 0.2 }}>
            <ListItemText primary={item} primaryTypographyProps={{ variant: 'body2' }} />
          </ListItem>
        ))}
      </List>
    )
  }
  return <Typography variant="body2" fontWeight={600} sx={{ whiteSpace: 'pre-wrap' }}>{String(v)}</Typography>
}

function InsightsTab() {
  const toast = useToast()
  const [category, setCategory] = useState('护肤')
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['brain-insights', category],
    queryFn: () => brainApi.industryInsights(category),
  })
  const insight = data as BrainIndustryInsights | undefined
  const extraEntries = insight
    ? Object.entries(insight).filter(([k]) => !INSIGHT_CARD_ORDER.includes(k as keyof BrainIndustryInsights))
    : []

  return (
    <Box sx={{ mt: 2 }}>
      <Stack direction="row" spacing={2} mb={2} flexWrap="wrap" useFlexGap alignItems="center">
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>选择品类</InputLabel>
          <Select value={category} label="选择品类" onChange={e => setCategory(e.target.value)}>
            {INSIGHT_CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </Select>
        </FormControl>
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => refetch()} disabled={isLoading}>刷新</Button>
        {insight?.趋势热点 && insight.趋势热点.length > 0 && (
          <Button
            size="small"
            variant="contained"
            color="secondary"
            startIcon={<PlaylistAddIcon />}
            onClick={() => enqueueInsightHotTopics(insight.趋势热点 ?? [], toast)}
          >
            趋势热点入队
          </Button>
        )}
        {isLoading && <CircularProgress size={20} sx={{ alignSelf: 'center' }} />}
      </Stack>
      {insight && (
        <Grid container spacing={2}>
          {INSIGHT_CARD_ORDER.map((key) => {
            const v = insight[key]
            if (v === undefined || v === null || (Array.isArray(v) && v.length === 0 && key !== '数据口径')) {
              return null
            }
            if (Array.isArray(v) && v.length === 0) return null
            if (typeof v === 'string' && !v.trim()) return null
            return (
              <Grid item xs={12} sm={6} key={key}>
                <Paper variant="outlined" sx={{ p: 2, height: '100%' }}>
                  <Typography variant="overline" color="text.secondary" display="block">{key}</Typography>
                  {renderInsightValue(key, v)}
                </Paper>
              </Grid>
            )
          })}
        </Grid>
      )}
      {extraEntries.length > 0 && (
        <Accordion sx={{ mt: 2 }}>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="subtitle2">其它字段（{extraEntries.length}）</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={1}>
              {extraEntries.map(([k, v]) => (
                <Grid item xs={12} sm={6} key={k}>
                  <Paper variant="outlined" sx={{ p: 1.5 }}>
                    <Typography variant="caption" color="text.secondary">{k}</Typography>
                    {renderInsightValue(k, v)}
                  </Paper>
                </Grid>
              ))}
            </Grid>
          </AccordionDetails>
        </Accordion>
      )}
      <Box sx={{ mt: 3 }}>
        <Button component={RouterLink} to="/admin/ai/brain-diagnosis" variant="outlined" size="small">
          去深度诊断中心
        </Button>
      </Box>
      {!isLoading && !insight && <Alert severity="info">暂无行业洞察数据</Alert>}
    </Box>
  )
}

const TABS = [
  { label: '行业趋势', icon: <TrendingUpIcon fontSize="small" /> },
  { label: '因果推断', icon: <PsychologyIcon fontSize="small" /> },
  { label: '用户画像', icon: <PersonIcon fontSize="small" /> },
  { label: '知识图谱', icon: <HubIcon fontSize="small" /> },
  { label: '行业洞察', icon: <LightbulbIcon fontSize="small" /> },
  { label: '高级工作台', icon: <BiotechIcon fontSize="small" /> },
]

export default function IndustryBrainPage() {
  const [tab, setTab] = useState(0)
  return (
    <Box sx={{ p: 2, height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', gap: 1, overflow: 'hidden' }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap>
        <Typography variant="h6" fontWeight={600}>行业大脑</Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          <Chip label="AI 驱动" color="primary" size="small" icon={<PsychologyIcon />} />
          <Link component={RouterLink} to="/admin/ai/brain-diagnosis" underline="hover" variant="body2">诊断中心</Link>
        </Stack>
      </Stack>
      <Alert severity="info" sx={{ py: 0.5 }}>
        能力导览：趋势与洞察可一键写入直播热词；「高级工作台」含生命周期、战略/增长、图谱审核等。需 Neo4j / TianAPI / LLM 等环境支持。
      </Alert>
      <Divider />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
        {TABS.map((t, i) => <Tab key={i} label={t.label} iconPosition="start" icon={t.icon} />)}
      </Tabs>
      <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0 }}>
        {tab === 0 && <TrendsTab />}
        {tab === 1 && <CausalTab />}
        {tab === 2 && <UserProfileTab />}
        {tab === 3 && <KnowledgeGraphTab />}
        {tab === 4 && <InsightsTab />}
        {tab === 5 && <IndustryBrainAdvancedTab />}
      </Box>
    </Box>
  )
}
