import { useState } from 'react'
import {
  Box, Card, CardContent, TextField, Button, Typography, Stack,
  LinearProgress, Chip, Divider, Alert,
} from '@mui/material'
import { TrendingUp as TrendingUpIcon, ContentCopy as ContentCopyIcon } from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeStringArray } from '@/utils/response-normalize'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

interface SeoInsightResult {
  tags: string[]
  abTitles: string[]
  note: string
  error?: string
}
const SEO_TAGS_ENDPOINT = '/short-video/seo/suggest-tags'
const SEO_AB_TITLES_ENDPOINT = '/short-video/seo/suggest-ab-titles'
const CONTENT_EFFECT_READY_ENDPOINTS = [SEO_TAGS_ENDPOINT, SEO_AB_TITLES_ENDPOINT].join('|')
const CONTENT_EFFECT_READY_ROUTES = [
  shortvideoRoutes.effectPredict,
  shortvideoRoutes.seoOptimize,
].join('|')
const CONTENT_EFFECT_SUPPORTED_ACTIONS = [
  'generate-seo-effect-advice',
  'copy-seo-effect-advice',
  'preserve-partial-seo-results',
].join('|')
const CONTENT_EFFECT_UNSUPPORTED_ENDPOINTS = [
  '/short-video/effect-predict/mock',
  '/short-video/effect-predict/local-score',
  '/short-video/effect-predict/local-forecast',
  '/short-video/effect-predict/play-count',
  '/short-video/effect-predict/conversion-rate',
  '/short-video/seo/local-tags',
  '/short-video/seo/local-ab-titles',
].join('|')

export default function ContentEffectPredictPage() {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [publishTime, setPublishTime] = useState('')
  const [result, setResult] = useState<SeoInsightResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handlePredict = async () => {
    if (!content && !title) return
    setLoading(true)
    setErrorMessage('')
    try {
      const baseTitle = title.trim() || content.slice(0, 24) || '短视频'
      const tagPayload = {
        title: baseTitle,
        description: content || undefined,
        industry: '美妆护肤',
      }
      const titlePayload = { baseTitle }
      let tags: string[] = []
      let abTitles: string[] = []
      const errors: string[] = []
      const [tagsResult, abTitlesResult] = await Promise.allSettled([
        shortvideoApi.seoSuggestTags(tagPayload),
        shortvideoApi.seoSuggestAbTitles(titlePayload),
      ])
      if (tagsResult.status === 'fulfilled') {
        tags = normalizeStringArray(tagsResult.value)
      } else {
        errors.push(`推荐标签失败（POST ${SEO_TAGS_ENDPOINT}）：${getErrorMessage(tagsResult.reason)}`)
      }
      if (abTitlesResult.status === 'fulfilled') {
        abTitles = normalizeStringArray(abTitlesResult.value)
      } else {
        errors.push(`标题 A/B 失败（POST ${SEO_AB_TITLES_ENDPOINT}）：${getErrorMessage(abTitlesResult.reason)}`)
      }
      if (errors.length === 2) {
        throw new Error(`${errors.join('；')}。当前标题、脚本和计划发布时间会保留。`)
      }
      if (errors.length === 1) {
        setErrorMessage(`${errors[0]}。另一条链路结果仍会展示，当前输入会保留。`)
      }
      const parts: string[] = []
      if (publishTime) parts.push(`计划发布时间：${publishTime}`)
      parts.push(`接口来源：POST ${SEO_TAGS_ENDPOINT} / ${SEO_AB_TITLES_ENDPOINT}`)
      parts.push('说明：后端暂无独立「播放量预测」接口，以下为 SEO 标签与标题变体，可作为上线前曝光优化参考。')
      setResult({
        tags,
        abTitles,
        note: parts.join('\n'),
      })
    } catch (error) {
      const message = getErrorMessage(error)
      setErrorMessage(message)
      toast(`获取 SEO 建议失败：${message}`, 'error')
      setResult({ tags: [], abTitles: [], note: '', error: message })
    } finally {
      setLoading(false)
    }
  }

  const copyPlan = () => {
    if (!result) return
    const text = [
      result.note,
      result.tags.length > 0 ? `推荐标签：${result.tags.join('、')}` : '',
      result.abTitles.length > 0 ? `标题变体：${result.abTitles.join(' | ')}` : '',
    ].filter(Boolean).join('\n')
    navigator.clipboard.writeText(text).catch(() => {})
    toast('建议已复制', 'success')
  }

  return (
    <Box
      data-testid="content-effect-predict-page"
      data-ready-endpoints={CONTENT_EFFECT_READY_ENDPOINTS}
      data-ready-routes={CONTENT_EFFECT_READY_ROUTES}
      data-supported-actions={CONTENT_EFFECT_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={CONTENT_EFFECT_UNSUPPORTED_ENDPOINTS}
      data-no-client-score-synthesis="true"
      data-no-local-seo-fallback="true"
      data-input-retained-on-error="true"
    >
      <PageHeader
        title="内容效果预测"
        breadcrumbs={[{ label: '短视频' }, { label: '效果预测' }]}
        subtitle={`基于 POST ${SEO_TAGS_ENDPOINT} 与 ${SEO_AB_TITLES_ENDPOINT} 辅助评估传播潜力（非数值预测）`}
        actions={
          result && !result.error ? (
            <Button size="small" startIcon={<ContentCopyIcon />} onClick={copyPlan} data-testid="content-effect-copy-plan-button">复制建议</Button>
          ) : undefined
        }
      />

      <Alert
        severity="warning"
        variant="outlined"
        data-testid="content-effect-boundary-contract"
        data-no-play-count-prediction="true"
        data-no-conversion-rate-prediction="true"
        data-no-local-score-fallback="true"
        data-supported-actions={CONTENT_EFFECT_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        后端当前没有独立播放量/转化率预测接口，本页按 POST {SEO_TAGS_ENDPOINT}、POST {SEO_AB_TITLES_ENDPOINT} 和计划发布时间生成上线前传播建议，不输出伪预测分数。
      </Alert>

      <Card
        sx={{ mb: 3 }}
        data-testid="content-effect-input-contract"
        data-server-payload-source="title|description|industry|baseTitle"
        data-input-retained-on-error="true"
      >
        <CardContent>
          <Stack spacing={2}>
            <TextField label="标题" value={title} onChange={(e) => setTitle(e.target.value)} fullWidth />
            <TextField
              label="脚本内容" value={content} onChange={(e) => setContent(e.target.value)}
              fullWidth multiline rows={6}
            />
            <TextField
              label="计划发布时间" type="datetime-local" value={publishTime}
              onChange={(e) => setPublishTime(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button
              variant="contained"
              startIcon={<TrendingUpIcon />}
              onClick={handlePredict}
              disabled={loading || (!content && !title)}
              sx={{ alignSelf: 'flex-start' }}
              data-testid="content-effect-generate-button"
              data-source-endpoints={CONTENT_EFFECT_READY_ENDPOINTS}
            >
              {loading ? '分析中...' : '生成 SEO 传播建议'}
            </Button>
            {errorMessage && (
              <Alert
                severity="error"
                data-testid="content-effect-error"
                data-input-retained="true"
                data-no-local-seo-fallback="true"
                action={<Button size="small" color="inherit" onClick={handlePredict}>重试</Button>}
              >
                SEO 传播建议生成失败：{errorMessage}
              </Alert>
            )}
          </Stack>
        </CardContent>
      </Card>

      {loading && <LinearProgress sx={{ mb: 2 }} />}

      {!!result && !result.error && (
        <Card
          data-testid="content-effect-result-contract"
          data-source-endpoints={CONTENT_EFFECT_READY_ENDPOINTS}
          data-no-client-score-synthesis="true"
          data-no-local-seo-fallback="true"
        >
          <CardContent>
            <Typography variant="h6" gutterBottom>SEO 传播建议</Typography>
            <Divider sx={{ mb: 2 }} />
            <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>{result.note}</Typography>
            {result.tags.length > 0 ? (
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" gutterBottom>推荐话题标签</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5} data-testid="content-effect-tags-result" data-source-endpoint={SEO_TAGS_ENDPOINT}>
                  {result.tags.map((t, i) => <Chip key={i} size="small" label={t} />)}
                </Stack>
              </Box>
            ) : (
              <Alert
                severity="info"
                data-testid="content-effect-tags-empty"
                data-no-local-tags-fallback="true"
                sx={{ mb: 2 }}
              >
                标签接口未返回推荐项，请补充标题或内容后重试。
              </Alert>
            )}
            {result.abTitles.length > 0 ? (
              <Box data-testid="content-effect-ab-title-result" data-source-endpoint={SEO_AB_TITLES_ENDPOINT}>
                <Typography variant="subtitle2" gutterBottom>标题 A/B 变体</Typography>
                <Stack spacing={0.5}>
                  {result.abTitles.map((t, i) => (
                    <Typography key={i} variant="body2">· {t}</Typography>
                  ))}
                </Stack>
              </Box>
            ) : (
              <Alert
                severity="info"
                data-testid="content-effect-ab-title-empty"
                data-no-local-title-fallback="true"
              >
                标题变体接口未返回结果，通常是标题过短或 LLM/SEO 服务不可用。
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

    </Box>
  )
}
