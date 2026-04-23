import { useState } from 'react'
import {
  Box, Card, CardContent, TextField, Button, Typography, Stack,
  LinearProgress, Chip, Divider, Alert,
} from '@mui/material'
import { TrendingUp as TrendingUpIcon } from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

interface SeoInsightResult {
  tags: string[]
  abTitles: string[]
  note: string
  error?: string
}

export default function ContentEffectPredictPage() {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [publishTime, setPublishTime] = useState('')
  const [result, setResult] = useState<SeoInsightResult | null>(null)
  const [loading, setLoading] = useState(false)

  const handlePredict = async () => {
    if (!content && !title) return
    setLoading(true)
    try {
      const baseTitle = title.trim() || content.slice(0, 24) || '短视频'
      const [tags, abTitles] = await Promise.all([
        shortvideoApi.seoSuggestTags({
          title: baseTitle,
          description: content || undefined,
          industry: '美妆护肤',
        }),
        shortvideoApi.seoSuggestAbTitles({ baseTitle }),
      ])
      const parts: string[] = []
      if (publishTime) parts.push(`计划发布时间：${publishTime}`)
      parts.push('说明：后端暂无独立「播放量预测」接口，以下为 SEO 标签与标题变体，可作为上线前曝光优化参考。')
      setResult({
        tags,
        abTitles,
        note: parts.join('\n'),
      })
    } catch {
      toast('获取 SEO 建议失败', 'error')
      setResult({ tags: [], abTitles: [], note: '', error: '请求失败' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box>
      <PageHeader
        title="内容效果预测"
        breadcrumbs={[{ label: '短视频' }, { label: '效果预测' }]}
        subtitle="基于 SEO 标签与标题变体辅助评估传播潜力（非数值预测）"
      />
      <Card sx={{ mb: 3 }}>
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
            >
              {loading ? '分析中...' : '生成 SEO 传播建议'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {loading && <LinearProgress sx={{ mb: 2 }} />}

      {!!result && !result.error && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>SEO 传播建议</Typography>
            <Divider sx={{ mb: 2 }} />
            <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>{result.note}</Typography>
            {result.tags.length > 0 && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" gutterBottom>推荐话题标签</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5}>
                  {result.tags.map((t, i) => <Chip key={i} size="small" label={t} />)}
                </Stack>
              </Box>
            )}
            {result.abTitles.length > 0 && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>标题 A/B 变体</Typography>
                <Stack spacing={0.5}>
                  {result.abTitles.map((t, i) => (
                    <Typography key={i} variant="body2">· {t}</Typography>
                  ))}
                </Stack>
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {!!result?.error && <Alert severity="error">{result.error}</Alert>}
    </Box>
  )
}
