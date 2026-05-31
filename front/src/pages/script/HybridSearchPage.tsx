import { useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  InputAdornment,
  LinearProgress,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'
import { scriptApi, type ScriptHybridSearchItem } from '@/api/script'
import { getErrorMessage } from '@/utils/errorHandler'

const MODE_TABS = [
  { label: '混合', value: 'hybrid' as const, hint: '向量 + BM25' },
  { label: '语义', value: 'semantic' as const, hint: 'vectorWeight=1' },
  { label: '关键词', value: 'keyword' as const, hint: 'lexicalWeight=1' },
]

const READY_ENDPOINTS = ['/script/search/hybrid']
const RELATED_ENDPOINTS = ['/script/search/semantic', '/script/search/suggest']
const UNSUPPORTED_ENDPOINTS = [
  '/ai/search/hybrid',
  '/script/search/analytics',
  '/script/search/click-feedback',
  '/ai/knowledge/search',
  '/product/search',
  '/live/search',
  '/douyin/video/search',
]
const UNSUPPORTED_ACTIONS = [
  'ai-global-search',
  'inline-semantic-search',
  'inline-suggest-search',
  'inline-search-analytics',
  'inline-click-feedback',
  'cross-module-knowledge-search',
  'cross-module-product-search',
  'cross-module-live-search',
  'cross-module-douyin-search',
]

function modeWeight(mode: 'hybrid' | 'semantic' | 'keyword') {
  if (mode === 'semantic') return { vector: 1, lexical: 0 }
  if (mode === 'keyword') return { vector: 0, lexical: 1 }
  return { vector: 0.5, lexical: 0.5 }
}

function scoreColor(score: number) {
  if (score >= 0.8) return 'success'
  if (score >= 0.5) return 'warning'
  return 'default'
}

function resultScore(item: ScriptHybridSearchItem) {
  return Number(item.hybridScore ?? item.score ?? item.vectorScore ?? item.lexicalScore ?? 0)
}

export default function HybridSearchPage() {
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [mode, setMode] = useState<'hybrid' | 'semantic' | 'keyword'>('hybrid')
  const [category, setCategory] = useState('')
  const [results, setResults] = useState<ScriptHybridSearchItem[]>([])
  const [total, setTotal] = useState(0)
  const [searchTime, setSearchTime] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState('')

  const diagnostics = useMemo(() => {
    const highScore = results.filter((item) => resultScore(item) >= 0.8).length
    const withVector = results.filter((item) => Number(item.vectorScore ?? 0) > 0).length
    const withKeyword = results.filter((item) => Number(item.lexicalScore ?? 0) > 0).length
    return { highScore, withVector, withKeyword }
  }, [results])
  const weights = modeWeight(mode)

  const handleSearch = async () => {
    const keyword = query.trim()
    if (!keyword) {
      toast('请输入搜索关键词', 'warning')
      return
    }
    setLoading(true)
    setSearched(true)
    setError('')
    try {
      const res = await scriptApi.searchHybrid({
        query: keyword,
        mode,
        category: category || undefined,
        style: undefined,
        rows: 20,
        topK: 20,
      })
      const list = Array.isArray(res.list) ? res.list : []
      setResults(list)
      setTotal(Number(res.total ?? list.length))
      setSearchTime(res.searchTime ?? null)
    } catch (e) {
      const message = getErrorMessage(e)
      setError(`${message}；上下文：route=/admin/script/hybrid-search; query=${keyword}; mode=${mode}; category=${category || '全部'}; topK=20`)
      setResults([])
      setTotal(0)
      setSearchTime(null)
      toast(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box
      data-testid="hybrid-search-workbench"
      data-contract-scope="script-hybrid-search"
      data-ready-endpoints={READY_ENDPOINTS.join('|')}
      data-related-endpoints={RELATED_ENDPOINTS.join('|')}
      data-unsupported-endpoints={UNSUPPORTED_ENDPOINTS.join('|')}
      data-unsupported-actions={UNSUPPORTED_ACTIONS.join('|')}
      data-mode={mode}
      data-vector-weight={weights.vector}
      data-lexical-weight={weights.lexical}
      data-query-length={query.trim().length}
      data-category={category || '全部'}
      data-result-count={results.length}
      data-total={total}
      data-search-time-ms={searchTime ?? ''}
      data-vector-hit-count={diagnostics.withVector}
      data-bm25-hit-count={diagnostics.withKeyword}
      data-high-score-count={diagnostics.highScore}
      data-no-static-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="话术混合搜索"
        subtitle="对齐 /script/search/hybrid：返回 list/hybridScore/searchTime；知识库、商品、直播跨模块检索不在该接口内。"
        breadcrumbs={[{ label: '话术' }, { label: '混合搜索' }]}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="hybrid-search-contract-alert"
        data-contract-source="/script/search/hybrid"
        data-no-ai-global-search-request="true"
        data-no-semantic-request="true"
        data-no-suggest-request="true"
        data-no-analytics-request="true"
        data-no-click-feedback-request="true"
        data-no-cross-module-search-request="true"
      >
        当前页面只查询 <code>sc_script</code> 话术索引。顶部“语义/关键词”通过权重切换实现，分类筛选对齐后端落库字段；
        旧 <code>/ai/search/hybrid</code> 是 AI 全局搜索接口，不再用于话术工作台。
      </Alert>

      <Grid container spacing={2}>
        {[
          { label: '返回总数', value: total, testId: 'hybrid-search-kpi-total', source: '/script/search/hybrid total' },
          { label: '当前页结果', value: results.length, testId: 'hybrid-search-kpi-current-page', source: 'scriptApi-normalized list.length' },
          { label: '高相关', value: diagnostics.highScore, testId: 'hybrid-search-kpi-high-score', source: 'local-derived hybridScore>=0.8' },
          { label: '有向量分', value: diagnostics.withVector, testId: 'hybrid-search-kpi-vector-hit', source: '/script/search/hybrid vectorScore' },
          { label: '耗时(ms)', value: searchTime ?? '-', testId: 'hybrid-search-kpi-search-time', source: '/script/search/hybrid searchTime' },
        ].map((item) => (
          <Grid item xs={6} md={2.4} key={item.label}>
            <Card
              variant="outlined"
              data-testid={item.testId}
              data-contract-source={item.source}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card
        variant="outlined"
        data-testid="hybrid-search-query-card"
        data-ready-endpoint="/script/search/hybrid"
        data-related-endpoints={RELATED_ENDPOINTS.join('|')}
        data-vector-weight={weights.vector}
        data-lexical-weight={weights.lexical}
      >
        <CardContent>
          <Tabs value={mode} onChange={(_, value) => setMode(value)} sx={{ mb: 2 }}>
            {MODE_TABS.map((item) => {
              const itemWeights = modeWeight(item.value)
              return (
                <Tab
                  key={item.value}
                  value={item.value}
                  label={`${item.label}搜索`}
                  data-testid={`hybrid-search-mode-${item.value}`}
                  data-contract-action="weight-switch"
                  data-vector-weight={itemWeights.vector}
                  data-lexical-weight={itemWeights.lexical}
                />
              )
            })}
          </Tabs>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
            <TextField
              fullWidth
              placeholder="输入搜索内容..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              inputProps={{ 'data-contract-param': 'query' }}
              InputProps={{
                startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment>,
              }}
            />
            <TextField
              label="分类"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              inputProps={{ 'data-contract-param': 'category' }}
              sx={{ width: { xs: '100%', md: 150 } }}
            />
            <Button
              variant="contained"
              onClick={handleSearch}
              disabled={loading}
              data-contract-action="searchHybrid"
              data-ready-endpoint="/script/search/hybrid"
              sx={{ minWidth: 110 }}
            >
              {loading ? '搜索中...' : '搜索'}
            </Button>
          </Stack>

          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 2 }}>
            {MODE_TABS.map((item) => (
              <Chip key={item.value} label={`${item.label}: ${item.hint}`} size="small" variant={mode === item.value ? 'filled' : 'outlined'} />
            ))}
          </Stack>
        </CardContent>
      </Card>

      {loading && <LinearProgress />}

      {error && (
        <Alert
          severity="error"
          data-testid="hybrid-search-error"
          data-contract-source="/script/search/hybrid"
          data-no-static-fallback="true"
          data-mode={mode}
          data-category={category || '全部'}
          action={<Button color="inherit" size="small" onClick={handleSearch}>重试</Button>}
        >
          搜索失败：{error}。请检查向量服务、BM25 索引和 /script/search/hybrid；失败时不会展示静态搜索结果。
        </Alert>
      )}

      {searched && !loading && !error && results.length === 0 && (
        <Alert
          severity="warning"
          data-testid="hybrid-search-empty"
          data-contract-source="/script/search/hybrid"
          data-no-static-fallback="true"
        >
          未找到相关结果。可放宽分类筛选，或确认话术是否已生成 embedding 索引。
        </Alert>
      )}

      <Stack spacing={1.5}>
        {results.map((item, index) => {
          const score = resultScore(item)
          return (
            <Card
              key={`${item.scriptId ?? item.id}-${index}`}
              variant="outlined"
              data-testid={`hybrid-search-result-${index}`}
              data-contract-source="/script/search/hybrid"
              data-result-source={item.source ?? 'script'}
              data-script-id={item.scriptId ?? item.id}
              data-hybrid-score={score}
              data-vector-score={Number(item.vectorScore ?? 0)}
              data-lexical-score={Number(item.lexicalScore ?? 0)}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2} sx={{ mb: 1 }}>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="subtitle2" fontWeight={700} noWrap>{item.title}</Typography>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap sx={{ mt: 0.75 }}>
                      <Chip label={item.category || '未分类'} size="small" variant="outlined" />
                      {item.style && <Chip label={item.style} size="small" variant="outlined" />}
                      {item.author && <Chip label={item.author} size="small" variant="outlined" />}
                    </Stack>
                  </Box>
                  <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap justifyContent="flex-end">
                    <Chip label={`融合 ${(score * 100).toFixed(0)}%`} size="small" color={scoreColor(score) as 'success' | 'warning' | 'default'} />
                    <Chip label={`向量 ${(Number(item.vectorScore ?? 0) * 100).toFixed(0)}%`} size="small" variant="outlined" />
                    <Chip label={`BM25 ${(Number(item.lexicalScore ?? 0) * 100).toFixed(0)}%`} size="small" variant="outlined" />
                  </Stack>
                </Stack>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical' }}
                >
                  {item.content}
                </Typography>
              </CardContent>
            </Card>
          )
        })}
      </Stack>
    </Box>
  )
}
