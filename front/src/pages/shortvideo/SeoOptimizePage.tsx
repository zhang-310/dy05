import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Stack, Button, TextField,
  Chip, Alert, CircularProgress, Divider, Grid, IconButton, Tooltip,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import SearchIcon from '@mui/icons-material/Search'
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningIcon from '@mui/icons-material/Warning'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeStringArray } from '@/utils/response-normalize'

interface SeoSuggestion {
  type: 'success' | 'warning' | 'error'
  message: string
}

interface SeoKeyword {
  keyword: string
  searchVolume: string
  trend?: string
}

interface SeoResult {
  suggestions?: SeoSuggestion[]
  score?: number
  optimizedTitle?: string
  keywords?: SeoKeyword[]
}

const SEO_TAGS_ENDPOINT = '/short-video/seo/suggest-tags'
const SEO_AB_TITLES_ENDPOINT = '/short-video/seo/suggest-ab-titles'
const SEO_READY_ENDPOINTS = [SEO_TAGS_ENDPOINT, SEO_AB_TITLES_ENDPOINT].join('|')
const SEO_READY_ROUTES = [
  shortvideoRoutes.seoOptimize,
  `${shortvideoRoutes.seoOptimize}?title=:title`,
  shortvideoRoutes.publish,
].join('|')
const SEO_SUPPORTED_ACTIONS = [
  'analyze-seo',
  'fetch-seo-keywords',
  'copy-optimized-title',
  'apply-optimized-title',
  'insert-keyword-to-title',
].join('|')
const SEO_UNSUPPORTED_ENDPOINTS = [
  '/short-video/seo/mock',
  '/short-video/seo/local-tags',
  '/short-video/seo/local-ab-titles',
  '/short-video/seo/local-keywords',
  '/short-video/seo/static-score',
  '/short-video/seo/static-search-volume',
  '/short-video/effect-predict/play-count',
  '/short-video/effect-predict/conversion-rate',
].join('|')

export default function SeoOptimizePage() {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [category, setCategory] = useState('')
  const [loading, setLoading] = useState(false)
  const [kwLoading, setKwLoading] = useState(false)
  const [result, setResult] = useState<SeoResult | null>(null)
  const [keywords, setKeywords] = useState<SeoKeyword[]>([])
  const [analysisError, setAnalysisError] = useState('')
  const [keywordError, setKeywordError] = useState('')

  const handleOptimize = async () => {
    if (!title && !content) return
    setLoading(true)
    setAnalysisError('')
    try {
      let tagsRaw: unknown
      try {
        tagsRaw = await shortvideoApi.seoSuggestTags({ title: title || '未命名', description: content || undefined, industry: category || '美妆护肤' })
      } catch (error) {
        throw new Error(`标签推荐失败（POST ${SEO_TAGS_ENDPOINT}）：${getErrorMessage(error)}`)
      }
      let abTitlesRaw: unknown = []
      if (title) {
        try {
          abTitlesRaw = await shortvideoApi.seoSuggestAbTitles({ baseTitle: title })
        } catch (error) {
          throw new Error(`标题 A/B 变体生成失败（POST ${SEO_AB_TITLES_ENDPOINT}）：${getErrorMessage(error)}`)
        }
      }
      const tags = normalizeStringArray(tagsRaw)
      const abTitles = normalizeStringArray(abTitlesRaw)
      const suggestions: SeoSuggestion[] = []
      if (tags.length > 0) {
        suggestions.push({ type: 'success', message: `推荐话题标签：${tags.slice(0, 8).join('、')}` })
      }
      if (abTitles.length > 0) {
        suggestions.push({ type: 'warning', message: `标题 A/B 变体可参考：${abTitles.slice(0, 3).join(' | ')}` })
      }
      if (suggestions.length === 0) {
        suggestions.push({ type: 'warning', message: '请先填写标题，以便生成标签与标题变体。' })
      }
      setResult({
        suggestions,
        score: tags.length >= 5 ? 75 : tags.length > 0 ? 60 : undefined,
        optimizedTitle: abTitles[0],
        keywords: tags.map((t) => ({ keyword: t, searchVolume: '—', trend: '推荐' })),
      })
    } catch (error) {
      const message = getErrorMessage(error)
      setAnalysisError(`${message}。当前标题、类别和脚本内容会保留，不展示模拟 SEO 评分。`)
      toast(`SEO 分析失败：${message}`, 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleFetchKeywords = async () => {
    if (!category && !title) return
    setKwLoading(true)
    setKeywordError('')
    try {
      const tags = normalizeStringArray(await shortvideoApi.seoSuggestTags({
        title: title || category || '短视频',
        description: content || undefined,
        industry: category || '美妆护肤',
      }))
      setKeywords(tags.map((t) => ({ keyword: t, searchVolume: '—', trend: '推荐' })))
    } catch (error) {
      const message = getErrorMessage(error)
      setKeywordError(`关键词获取失败（POST ${SEO_TAGS_ENDPOINT}）：${message}。当前类别和标题会保留，不使用静态热词补齐。`)
      toast(`关键词获取失败：${message}`, 'error')
    } finally {
      setKwLoading(false)
    }
  }

  const insertKeyword = (kw: string) => {
    setTitle(prev => prev ? `${prev} ${kw}` : kw)
    toast(`已插入关键词：${kw}`, 'success')
  }

  const copyText = (text: string) => {
    navigator.clipboard.writeText(text).catch(() => {})
    toast('已复制', 'success')
  }

  const suggestions = result?.suggestions ?? []
  const score = result?.score
  return (
    <Box
      data-testid="shortvideo-seo-optimize-page"
      data-ready-endpoints={SEO_READY_ENDPOINTS}
      data-ready-routes={SEO_READY_ROUTES}
      data-supported-actions={SEO_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={SEO_UNSUPPORTED_ENDPOINTS}
      data-no-static-seo-score="true"
      data-no-local-keyword-fallback="true"
      data-input-retained-on-error="true"
    >
      <PageHeader
        title="短视频 SEO 优化"
        breadcrumbs={[{ label: '短视频' }, { label: 'SEO优化' }]}
        subtitle={`接入 POST ${SEO_TAGS_ENDPOINT} 与 ${SEO_AB_TITLES_ENDPOINT}，输出标签、标题变体和可执行诊断。`}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-seo-boundary-contract"
        data-source-endpoints={SEO_READY_ENDPOINTS}
        data-no-play-count-prediction="true"
        data-no-static-search-volume="true"
        data-supported-actions={SEO_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        当前后端提供标签、标题 A/B、封面和发布时间建议；本页不伪装播放量预测，搜索量字段以“推荐”口径展示，接口失败时保留输入。
      </Alert>

      <Grid container spacing={3}>
        {/* 左栏：输入 + 分析结果 */}
        <Grid item xs={12} md={7}>
          <Card
            variant="outlined"
            data-testid="shortvideo-seo-input-contract"
            data-source-endpoints={SEO_READY_ENDPOINTS}
            data-input-retained-on-error="true"
            sx={{ mb: 3 }}
          >
            <CardContent>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>内容输入</Typography>
              <Stack spacing={2}>
                <TextField
                  label="视频标题" value={title}
                  onChange={e => setTitle(e.target.value)}
                  fullWidth
                  helperText={`${title.length} 字（建议 15-25 字）`}
                />
                <TextField
                  label="内容类别" value={category}
                  onChange={e => setCategory(e.target.value)}
                  fullWidth size="small" placeholder="如：护肤教程、彩妆测评"
                />
                <TextField
                  label="脚本内容（可选）" value={content}
                  onChange={e => setContent(e.target.value)}
                  fullWidth multiline rows={4} size="small"
                />
                <Button
                  variant="contained" startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <SearchIcon />}
                  onClick={handleOptimize} disabled={loading || (!title && !content)}
                  sx={{ height: 44, fontWeight: 600 }}
                  data-testid="shortvideo-seo-analyze-button"
                  data-source-endpoints={SEO_READY_ENDPOINTS}
                >
                  {loading ? 'AI 分析中...' : 'AI SEO 分析'}
                </Button>
                {analysisError && (
                  <Alert
                    severity="error"
                    data-testid="shortvideo-seo-analysis-error"
                    data-input-retained="true"
                    data-no-static-seo-score="true"
                    action={<Button size="small" color="inherit" onClick={handleOptimize}>重试</Button>}
                  >
                    SEO 分析失败：{analysisError}
                  </Alert>
                )}
              </Stack>
            </CardContent>
          </Card>

          {result && (
            <Card variant="outlined" data-testid="shortvideo-seo-result-contract" data-source-endpoints={SEO_READY_ENDPOINTS} data-no-static-seo-score="true">
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>AI SEO 建议</Typography>
                  {score != null && (
                    <Chip
                      data-testid="shortvideo-seo-score-chip"
                      data-score-source="tag-count-diagnostic"
                      label={`SEO 评分：${score}`}
                      color={score >= 80 ? 'success' : score >= 60 ? 'warning' : 'error'}
                      size="small"
                    />
                  )}
                </Stack>
                <Stack spacing={1}>
                  {suggestions.map((s, i) => (
                    <Alert key={i} severity={s.type === 'success' ? 'success' : s.type === 'warning' ? 'warning' : 'error'}
                      data-testid="shortvideo-seo-suggestion-item"
                      icon={s.type === 'success' ? <CheckCircleIcon fontSize="small" /> : <WarningIcon fontSize="small" />}
                    >
                      {s.message}
                    </Alert>
                  ))}
                  {suggestions.length === 0 && (
                    <Alert severity="info" data-testid="shortvideo-seo-result-empty" data-no-local-seo-fallback="true">暂无具体建议，请补充标题或脚本内容后重试。</Alert>
                  )}
                </Stack>
                {result.optimizedTitle && (
                  <Box
                    data-testid="shortvideo-seo-optimized-title-surface"
                    data-source-endpoint={SEO_AB_TITLES_ENDPOINT}
                    data-no-local-title-fallback="true"
                    sx={(theme) => ({
                      mt: 2,
                      p: 2,
                      bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.12 : 0.08),
                      borderRadius: 1,
                      border: 1,
                      borderColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.36 : 0.24),
                    })}
                  >
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Box>
                        <Typography
                          variant="caption"
                          fontWeight={600}
                          sx={(theme) => ({
                            color: theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.dark,
                          })}
                        >
                          AI 优化标题建议：
                        </Typography>
                        <Typography variant="body2" fontWeight={600} sx={{ mt: 0.5 }}>{result.optimizedTitle}</Typography>
                      </Box>
                        <Tooltip title="复制">
                        <IconButton size="small" onClick={() => copyText(result.optimizedTitle!)} data-testid="shortvideo-seo-copy-title-button">
                          <ContentCopyIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                    <Button size="small" variant="outlined" color="success" sx={{ mt: 1 }}
                      data-testid="shortvideo-seo-apply-title-button"
                      onClick={() => { setTitle(result.optimizedTitle!); toast('已应用优化标题', 'success') }}>
                      应用此标题
                    </Button>
                  </Box>
                )}
              </CardContent>
            </Card>
          )}
        </Grid>

        {/* 右栏：关键词推荐 */}
        <Grid item xs={12} md={5}>
          <Card variant="outlined" data-testid="shortvideo-seo-keyword-contract" data-source-endpoint={SEO_TAGS_ENDPOINT} data-no-local-keyword-fallback="true" data-no-static-search-volume="true">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>推荐关键词</Typography>
                <Button size="small" startIcon={kwLoading ? <CircularProgress size={14} /> : <LocalFireDepartmentIcon fontSize="small" />}
                  onClick={handleFetchKeywords} disabled={kwLoading}
                  data-testid="shortvideo-seo-fetch-keywords-button"
                  data-source-endpoint={SEO_TAGS_ENDPOINT}
                >
                  {kwLoading ? '获取中...' : '获取关键词'}
                </Button>
              </Stack>
              <Divider sx={{ mb: 2 }} />
              {keywordError ? (
                <Alert
                  severity="error"
                  data-testid="shortvideo-seo-keyword-error"
                  data-input-retained="true"
                  data-no-local-keyword-fallback="true"
                  action={<Button size="small" color="inherit" onClick={handleFetchKeywords}>重试</Button>}
                >
                  {keywordError}
                </Alert>
              ) : keywords.length === 0 ? (
                <Alert severity="info" data-testid="shortvideo-seo-keyword-empty" data-no-local-keyword-fallback="true" data-no-static-search-volume="true">输入类别或标题后点击「获取关键词」。接口只返回推荐标签，不返回平台搜索量。</Alert>
              ) : (
                <Stack spacing={1}>
                  {keywords.map((kw, i) => (
                    <Box key={i} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', py: 0.5 }}>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <LocalFireDepartmentIcon fontSize="small" color="error" />
                        <Typography variant="body2" fontWeight={600}>{kw.keyword}</Typography>
                      </Stack>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="caption" color="text.secondary">{kw.searchVolume}/月</Typography>
                        {kw.trend && <Chip label={kw.trend} size="small" color="success" variant="outlined" />}
                        <Button size="small" variant="outlined"
                          data-testid="shortvideo-seo-insert-keyword-button"
                          onClick={() => insertKeyword(kw.keyword)}>插入</Button>
                      </Stack>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
