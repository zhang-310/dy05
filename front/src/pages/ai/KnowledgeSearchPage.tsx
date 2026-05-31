import { useState } from 'react'
import {
  Box, Typography, TextField, Button, Card, CardContent, CardActions,
  Chip, CircularProgress, InputAdornment, FormControl, InputLabel,
  Select, MenuItem, Stack, IconButton, Tooltip, Alert, Paper, Grid,
  FormControlLabel, Switch,
} from '@mui/material'
import {
  Search as SearchIcon, ThumbUp as ThumbUpIcon, ThumbDown as ThumbDownIcon,
  ContentCopy as CopyIcon, LocalFireDepartment as FireIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { useQuery, useMutation } from '@tanstack/react-query'
import { aiApi, type KbSearchHit } from '@/api/ai'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { useToast } from '@/contexts/ToastContext'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'
import { PageHeader } from '@/components/base'
import MarkdownViewer from '@/components/MarkdownViewer'

const HOT_SEARCHES = ['直播话术', '短视频脚本', '选品策略', '数据分析', '美妆技巧']

interface SearchResult {
  id: number; title: string; content: string; score?: number
  sourceType?: string; labels?: string[]; kbId?: number; text?: string
  chunkId?: number; explain?: string
}

function scorePercent(score?: number): number | null {
  if (score == null || !Number.isFinite(score)) return null
  return score > 1 ? Math.min(100, Math.round(score)) : Math.round(score * 100)
}

function scoreChipColor(percent: number | null): 'success' | 'warning' | 'default' {
  if (percent == null) return 'default'
  if (percent >= 80) return 'success'
  if (percent >= 50) return 'warning'
  return 'default'
}

export default function KnowledgeSearchPage() {
  const toast = useToast()
  const [selectedKbId, setSelectedKbId] = useState<number | ''>(``)
  const [query, setQuery] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [queryRewrite, setQueryRewrite] = useState(false)
  const [feedbackError, setFeedbackError] = useState<string | null>(null)
  const canSearch = selectedKbId !== '' && searchQuery.length > 0

  const { data: kbList = [], isError: kbListError, error: kbListErr, refetch: refetchKbList } = useQuery({
    queryKey: ['kb-list-for-search'],
    queryFn: () => aiApi.kbList({ rows: 100 }),
  })

  const { data: results = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['kb-search', selectedKbId, searchQuery, queryRewrite],
    queryFn: async () => {
      const res = await aiApi.kbSearch(
        selectedKbId as number,
        { query: searchQuery, topK: 10, queryRewrite }
      )
      const list: KbSearchHit[] = Array.isArray(res) ? res : []
      return list.map((item): SearchResult => ({
        id: Number(item.docId ?? 0),
        title: (item.title && item.title.trim() !== '')
          ? String(item.title).slice(0, 50)
          : String(item.content ?? '').slice(0, 50),
        content: String(item.content ?? ''),
        score: item.score != null ? Number(item.score) : undefined,
        sourceType: item.source != null ? String(item.source) : undefined,
        labels: Array.isArray(item.labels) ? item.labels.map((l: string) => String(l)) : undefined,
        kbId: selectedKbId as number,
        chunkId: item.chunkId,
        explain: item.explain,
      }))
    },
    enabled: canSearch,
  })

  const feedbackMut = useMutation({
    mutationFn: ({ docId, rating }: { docId: number; rating: -1 | 1 }) =>
      aiApi.kbFeedback({
        docId,
        rating,
        query: searchQuery,
        searchMode: 'knowledge-search',
      }),
    onSuccess: () => {
      setFeedbackError(null)
      toast('反馈已提交', 'success')
    },
    onError: (e: Error) => {
      const message = `反馈提交失败（/ai/knowledge-base/feedback）：${e.message}`
      setFeedbackError(message)
      toast(e.message, 'error')
    },
  })

  const handleSearch = async () => {
    if (!query.trim() || selectedKbId === '') return
    setFeedbackError(null)
    try {
      const ent = await checkGaifanEntitlement('knowledge-base', 'knowledge-base.rag')
      if (ent && ent.granted === false) {
        toast(commercialDenialMessage({ code: 4421, message: ent.reason ?? '无知识库 RAG 权益' }), 'warning')
        return
      }
      setSearchQuery(query.trim())
    } catch (e) {
      if (isCommercialDenial(e)) {
        toast(commercialDenialMessage(e), 'warning')
        return
      }
      setSearchQuery(query.trim())
    }
  }
  const handleHot = (term: string) => { setQuery(term); setFeedbackError(null); if (selectedKbId !== '') setSearchQuery(term) }
  const handleCopy = (text: string) => { navigator.clipboard.writeText(text); toast('已复制', 'success') }

  return (
    <Box
      data-testid="knowledge-search-page"
      data-ready-endpoints="/ai/knowledge-base/list,/ai/knowledge-base/{kbId}/search,/ai/knowledge-base/feedback"
      data-unsupported-endpoints="/ai/knowledge-base/search-all,/ai/knowledge-base/mock-search,/ai/knowledge-base/local-cache,/ai/knowledge-base/static-results"
      data-no-local-search-fallback="true"
      data-no-static-search-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 'var(--spacing-lg)', bgcolor: 'var(--color-surface-dark)', minHeight: '100%' }}
    >
      <PageHeader
        title="知识库检索"
        subtitle="按知识库执行混合检索，展示命中来源、相关度、分块与反馈状态"
        breadcrumbs={[{ label: 'AI中心' }, { label: '知识库检索' }]}
        actions={
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => { void refetchKbList(); if (searchQuery) void refetch() }}>
            刷新
          </Button>
        }
      />

      <Grid container spacing={1.5}>
        {[
          ['知识库', `${Array.isArray(kbList) ? kbList.length : 0}`, selectedKbId ? `当前 KB #${selectedKbId}` : '请先选择范围'],
          ['检索模式', queryRewrite ? 'LLM 改写' : '原句检索', queryRewrite ? '可能更慢但召回更宽' : '管理端默认低延迟'],
          ['命中数', `${results.length}`, searchQuery ? `关键词：${searchQuery}` : '尚未检索'],
          ['反馈接口', '/feedback', '点赞/点踩写入 boost_factor'],
        ].map(([title, value, helper]) => (
          <Grid item xs={12} sm={6} md={3} key={title}>
            <Paper
              variant="outlined"
              data-testid={`knowledge-search-summary-${title}`}
              data-source-endpoint={title === '知识库' ? '/ai/knowledge-base/list' : undefined}
              sx={{ p: 1.5, borderRadius: 1, height: '100%' }}
            >
              <Typography variant="caption" color="text.secondary">{title}</Typography>
              <Typography variant="h6" fontWeight={700}>{value}</Typography>
              <Typography variant="caption" color="text.secondary">{helper}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      {kbListError && (
        <Alert
          severity="error"
          data-testid="knowledge-search-kb-list-error"
          data-source-endpoint="/ai/knowledge-base/list"
          data-no-local-kb-list-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => { void refetchKbList() }}>重试</Button>}
        >
          知识库列表加载失败：{kbListErr instanceof Error ? kbListErr.message : String(kbListErr)}
        </Alert>
      )}

      {/* 搜索区 */}
      <Card
        data-testid="knowledge-search-form"
        data-source-endpoints="/ai/knowledge-base/list,/ai/knowledge-base/{kbId}/search"
        data-no-static-hot-search-submit="true"
        sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}
      >
        <CardContent sx={{ bgcolor: 'var(--color-surface)' }}>
          <Stack spacing={2}>
            <FormControl size="small" fullWidth sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: 'var(--border-radius-lg)',
                bgcolor: 'var(--color-surface-dark)',
                color: 'var(--color-text-primary)',
                '& fieldset': { borderColor: 'var(--color-surface-light)' },
                '&:hover fieldset': { borderColor: 'var(--color-primary)' }
              },
              '& .MuiInputLabel-root': { color: 'var(--color-text-secondary)' }
            }}>
              <InputLabel id="knowledge-search-kb-label">选择知识库</InputLabel>
              <Select
                id="knowledge-search-kb"
                labelId="knowledge-search-kb-label"
                value={selectedKbId}
                label="选择知识库"
                onChange={e => setSelectedKbId(e.target.value as number | '')}>
                <MenuItem value="" disabled>请选择知识库</MenuItem>
                {(Array.isArray(kbList) ? kbList : []).map((kb) => (
                  <MenuItem key={kb.id} value={kb.id}>{kb.kbName}</MenuItem>
                ))}
              </Select>
            </FormControl>
            {selectedKbId === '' && (
              <Alert
                severity="info"
                data-testid="knowledge-search-cross-kb-downgrade"
                data-unsupported-endpoint="/ai/knowledge-base/search-all"
                data-degrade-strategy="require-single-kb-selection"
                sx={{ py: 0.75 }}
              >
                当前后端检索接口为 `/ai/knowledge-base/{'{'}kbId{'}'}/search`，需要先选择一个知识库；跨库检索可后续接入 RAG 跨库服务。
              </Alert>
            )}
            <TextField
              fullWidth size="small" placeholder="输入搜索关键词..."
              value={query} onChange={e => setQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: '44px',
                  borderRadius: 'var(--border-radius-lg)',
                  bgcolor: 'var(--color-surface-dark)',
                  color: 'var(--color-text-primary)',
                  '& fieldset': { borderColor: 'var(--color-surface-light)' },
                  '&:hover fieldset': { borderColor: 'var(--color-primary)' },
                  '&.Mui-focused fieldset': { borderColor: 'var(--color-primary)' }
                },
                '& .MuiOutlinedInput-input::placeholder': { color: 'var(--color-text-secondary)' }
              }}
              InputProps={{
                startAdornment: <InputAdornment position="start"><SearchIcon sx={{ color: 'var(--color-text-secondary)' }} /></InputAdornment>,
                endAdornment: <InputAdornment position="end">
                  <Button variant="contained" size="small" onClick={handleSearch} disabled={!query.trim() || !selectedKbId}
                    data-testid="knowledge-search-primary-action-surface"
                    data-source-endpoint="/ai/knowledge-base/{kbId}/search"
                    sx={{
                      height: '32px',
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'primary.main',
                      color: 'primary.contrastText',
                      fontWeight: 600,
                      '&:hover': { bgcolor: 'primary.dark' }
                    }}>搜索</Button>
                </InputAdornment>,
              }} />
            <FormControlLabel
              control={<Switch checked={queryRewrite} onChange={e => setQueryRewrite(e.target.checked)} />}
              label="启用 LLM 查询改写"
            />
            <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
              <FireIcon
                data-testid="knowledge-search-hot-icon-surface"
                sx={(theme) => ({
                  color: theme.palette.mode === 'dark'
                    ? theme.palette.warning.light
                    : theme.palette.warning.main,
                  fontSize: 18,
                })}
              />
              <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>热门：</Typography>
              {HOT_SEARCHES.map(h => (
                <Chip key={h} label={h} size="small" clickable onClick={() => handleHot(h)} variant="outlined" />
              ))}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {isLoading && <CircularProgress sx={{ display: 'block', mx: 'auto' }} />}

      {isError && (
        <Alert
          severity="error"
          data-testid="knowledge-search-error"
          data-source-endpoint={`/ai/knowledge-base/${selectedKbId}/search`}
          data-no-local-search-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => { void refetch() }}>重试</Button>}
        >
          检索失败（/ai/knowledge-base/{selectedKbId}/search）：{error instanceof Error ? error.message : String(error)}。请检查 Docker 后端 `OLLAMA_URL=http://host.docker.internal:11434`、嵌入模型、Milvus/Elasticsearch 和目标知识库索引队列。
        </Alert>
      )}

      {feedbackError != null && (
        <Alert
          severity="error"
          data-testid="knowledge-search-feedback-error"
          data-source-endpoint="/ai/knowledge-base/feedback"
          data-no-optimistic-feedback-fallback="true"
          onClose={() => setFeedbackError(null)}
        >
          {feedbackError}。点赞/点踩失败不会本地修改 boost_factor，请在接口恢复后重试。
        </Alert>
      )}

      {!isLoading && canSearch && results.length === 0 && (
        <Alert
          severity="warning"
          data-testid="knowledge-search-empty-state"
          data-source-endpoint={`/ai/knowledge-base/${selectedKbId}/search`}
          data-no-static-search-fallback="true"
        >
          未找到相关内容。若知识库文档状态仍是“待向量同步”，请先处理嵌入服务或索引队列失败。
        </Alert>
      )}

      <Box
        data-testid="knowledge-search-results"
        data-source-endpoint="/ai/knowledge-base/{kbId}/search"
        data-no-static-search-fallback="true"
        sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
      >
      {results.map((r: SearchResult, i: number) => (
        <Card
          key={r.id ?? i}
          variant="outlined"
          data-testid="knowledge-search-result-card"
          data-source-endpoint="/ai/knowledge-base/{kbId}/search"
          data-markdown-renderer="MarkdownViewer"
          data-feedback-endpoint="/ai/knowledge-base/feedback"
          sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)', borderRadius: 'var(--border-radius-xl)', boxShadow: 'var(--shadow-elevation-1)', transition: 'all var(--transition-base)', '&:hover': { boxShadow: 'var(--shadow-elevation-2)', borderColor: 'var(--color-primary)' } }}
        >
          <CardContent sx={{ pb: 1, bgcolor: 'var(--color-surface)' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={1}>
              <Typography variant="subtitle2" fontWeight={600}>{r.title || `结果 ${i + 1}`}</Typography>
              {scorePercent(r.score) !== null && (
                <Chip label={`相关度 ${scorePercent(r.score)}%`} size="small" variant="filled"
                  color={scoreChipColor(scorePercent(r.score))}
                  data-testid="knowledge-search-score-chip-surface"
                  sx={{ fontWeight: 600 }} />
              )}
            </Stack>
            {r.labels && r.labels.length > 0 && (
              <Stack direction="row" spacing={0.5} mb={1} flexWrap="wrap">
                {r.labels.map(l => <Chip key={l} label={l} size="small" variant="outlined" sx={{ borderColor: 'var(--color-surface-light)', color: 'var(--color-text-secondary)' }} />)}
              </Stack>
            )}
            <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
              {r.sourceType && <Chip label={r.sourceType} size="small" variant="outlined" />}
              {r.kbId != null && <Chip label={`KB #${r.kbId}`} size="small" variant="outlined" />}
              {r.id > 0 && <Chip label={`文档 #${r.id}`} size="small" variant="outlined" />}
              {r.chunkId != null && <Chip label={`chunk ${r.chunkId}`} size="small" variant="outlined" />}
            </Stack>
            <Box data-testid="knowledge-search-result-markdown" data-renderer="MarkdownViewer">
              <MarkdownViewer
                content={String(r.content ?? r.text ?? '').slice(0, 600) + (String(r.content ?? r.text ?? '').length > 600 ? '...' : '')}
                compact
                sx={{ color: 'var(--color-text-secondary)' }}
              />
            </Box>
            {r.explain != null && r.explain !== '' && (
              <Alert severity="info" sx={{ mt: 1, py: 0.5 }} icon={false}>
                <Typography variant="caption" component="div">{r.explain}</Typography>
              </Alert>
            )}
          </CardContent>
          <CardActions sx={{ px: 'var(--spacing-md)', pb: 'var(--spacing-md)', bgcolor: 'var(--color-surface)' }}>
            <Tooltip title="复制内容">
              <IconButton size="small" onClick={() => handleCopy(String(r.content ?? r.text ?? ''))}><CopyIcon fontSize="small" /></IconButton>
            </Tooltip>
            <Tooltip title="有用">
              <span>
                <IconButton
                  size="small"
                  aria-label="有用"
                  data-testid="knowledge-search-feedback-up"
                  data-source-endpoint="/ai/knowledge-base/feedback"
                  disabled={r.id <= 0 || feedbackMut.isPending}
                  onClick={() => feedbackMut.mutate({ docId: r.id, rating: 1 })}
                >
                  <ThumbUpIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="无用">
              <span>
                <IconButton
                  size="small"
                  aria-label="无用"
                  data-testid="knowledge-search-feedback-down"
                  data-source-endpoint="/ai/knowledge-base/feedback"
                  disabled={r.id <= 0 || feedbackMut.isPending}
                  onClick={() => feedbackMut.mutate({ docId: r.id, rating: -1 })}
                >
                  <ThumbDownIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
          </CardActions>
        </Card>
      ))}
      </Box>
    </Box>
  )
}
