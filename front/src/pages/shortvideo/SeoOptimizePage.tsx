import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Stack, Button, TextField,
  Chip, Alert, CircularProgress, Divider, Grid, IconButton, Tooltip,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningIcon from '@mui/icons-material/Warning'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

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

export default function SeoOptimizePage() {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [category, setCategory] = useState('')
  const [loading, setLoading] = useState(false)
  const [kwLoading, setKwLoading] = useState(false)
  const [result, setResult] = useState<SeoResult | null>(null)
  const [keywords, setKeywords] = useState<SeoKeyword[]>([])

  const handleOptimize = async () => {
    if (!title && !content) return
    setLoading(true)
    try {
      const [tags, abTitles] = await Promise.all([
        shortvideoApi.seoSuggestTags({ title: title || '未命名', description: content || undefined, industry: category || '美妆护肤' }),
        title ? shortvideoApi.seoSuggestAbTitles({ baseTitle: title }) : Promise.resolve([] as string[]),
      ])
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
    } catch {
      toast('SEO 分析失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleFetchKeywords = async () => {
    if (!category && !title) return
    setKwLoading(true)
    try {
      const tags = await shortvideoApi.seoSuggestTags({
        title: title || category || '短视频',
        description: content || undefined,
        industry: category || '美妆护肤',
      })
      setKeywords(tags.map((t) => ({ keyword: t, searchVolume: '—', trend: '推荐' })))
    } catch {
      toast('关键词获取失败', 'error')
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
    <Box sx={{ bgcolor: 'var(--color-surface-dark)', minHeight: '100vh', p: 'var(--spacing-lg)' }}>
      <PageHeader
        title="短视频 SEO 优化"
        breadcrumbs={[{ label: '短视频' }, { label: 'SEO优化' }]}
        subtitle="AI 分析标题与内容，提升搜索曝光率"
      />

      <Grid container spacing={3}>
        {/* 左栏：输入 + 分析结果 */}
        <Grid item xs={12} md={7}>
          <Card sx={{ mb: 'var(--spacing-lg)', bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
            <CardContent sx={{ p: 'var(--spacing-lg)' }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, color: 'var(--color-text-primary)', mb: 'var(--spacing-md)' }}>内容输入</Typography>
              <Stack spacing={2}>
                <TextField
                  label="视频标题" value={title}
                  onChange={e => setTitle(e.target.value)}
                  fullWidth
                  helperText={`${title.length} 字（建议 15-25 字）`}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'var(--color-surface-dark)',
                      color: 'var(--color-text-primary)',
                      '& fieldset': { borderColor: 'var(--color-surface-light)' },
                      '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                    },
                    '& .MuiInputLabel-root': { color: 'var(--color-text-secondary)' }
                  }}
                />
                <TextField
                  label="内容类别" value={category}
                  onChange={e => setCategory(e.target.value)}
                  fullWidth size="small" placeholder="如：护肤教程、彩妆测评"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'var(--color-surface-dark)',
                      color: 'var(--color-text-primary)',
                      '& fieldset': { borderColor: 'var(--color-surface-light)' },
                      '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                    }
                  }}
                />
                <TextField
                  label="脚本内容（可选）" value={content}
                  onChange={e => setContent(e.target.value)}
                  fullWidth multiline rows={4} size="small"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'var(--color-surface-dark)',
                      color: 'var(--color-text-primary)',
                      '& fieldset': { borderColor: 'var(--color-surface-light)' },
                      '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                    }
                  }}
                />
                <Button
                  variant="contained" startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <SearchIcon />}
                  onClick={handleOptimize} disabled={loading || (!title && !content)}
                  sx={{
                    height: '44px',
                    borderRadius: 'var(--border-radius-lg)',
                    bgcolor: 'var(--color-primary)',
                    color: '#000',
                    fontWeight: 600,
                    '&:hover': { bgcolor: 'var(--color-primary-dark)' }
                  }}
                >
                  {loading ? 'AI 分析中...' : 'AI SEO 分析 ✨'}
                </Button>
              </Stack>
            </CardContent>
          </Card>

          {result && (
            <Card sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
              <CardContent sx={{ p: 'var(--spacing-lg)' }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb='var(--spacing-md)'>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>AI SEO 建议</Typography>
                  {score != null && (
                    <Chip
                      label={`SEO 评分：${score}`}
                      sx={{
                        bgcolor: score >= 80 ? 'var(--color-success)' : score >= 60 ? 'var(--color-warning)' : 'var(--color-error)',
                        color: '#fff',
                        fontWeight: 600
                      }}
                      size="small"
                    />
                  )}
                </Stack>
                <Stack spacing={1}>
                  {suggestions.map((s, i) => (
                    <Alert key={i} severity={s.type === 'success' ? 'success' : s.type === 'warning' ? 'warning' : 'error'}
                      icon={s.type === 'success' ? <CheckCircleIcon fontSize="small" /> : <WarningIcon fontSize="small" />}
                      sx={{ py: 'var(--spacing-sm)', bgcolor: s.type === 'success' ? 'rgba(52, 199, 89, 0.1)' : s.type === 'warning' ? 'rgba(251, 191, 36, 0.1)' : 'rgba(239, 68, 68, 0.1)' }}
                    >
                      {s.message}
                    </Alert>
                  ))}
                  {suggestions.length === 0 && (
                    <Alert severity="info" sx={{ bgcolor: 'rgba(59, 130, 246, 0.1)' }}>暂无具体建议</Alert>
                  )}
                </Stack>
                {result.optimizedTitle && (
                  <Box sx={{ mt: 'var(--spacing-md)', p: 'var(--spacing-md)', bgcolor: 'rgba(52, 199, 89, 0.05)', borderRadius: 'var(--border-radius-lg)', border: '1px solid', borderColor: 'rgba(52, 199, 89, 0.3)' }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Box>
                        <Typography variant="caption" color="success.dark" fontWeight={600}>AI 优化标题建议：</Typography>
                        <Typography variant="body2" fontWeight={600} sx={{ mt: 0.5 }}>{result.optimizedTitle}</Typography>
                      </Box>
                      <Tooltip title="复制">
                        <IconButton size="small" onClick={() => copyText(result.optimizedTitle!)}>
                          <ContentCopyIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                    <Button size="small" variant="outlined" color="success" sx={{ mt: 1 }}
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
          <Card sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
            <CardContent sx={{ p: 'var(--spacing-lg)' }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb='var(--spacing-md)'>
                <Typography variant="subtitle1" sx={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>推荐关键词</Typography>
                <Button size="small" startIcon={kwLoading ? <CircularProgress size={14} /> : <LocalFireDepartmentIcon fontSize="small" />}
                  onClick={handleFetchKeywords} disabled={kwLoading}
                  sx={{ color: 'var(--color-primary)', '&:hover': { bgcolor: 'rgba(0, 208, 132, 0.1)' } }}>
                  {kwLoading ? '获取中...' : '获取关键词'}
                </Button>
              </Stack>
              <Divider sx={{ mb: 'var(--spacing-md)', borderColor: 'var(--color-surface-light)' }} />
              {keywords.length === 0 ? (
                <Typography color="text.secondary" variant="body2" sx={{ py: 2, textAlign: 'center' }}>
                  输入类别后点击「获取关键词」
                </Typography>
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
