import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Button, TextField,
  Grid, CircularProgress, Paper, Divider, Chip, FormControl,
  InputLabel, Select, MenuItem, Slider, Alert,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import CompareArrowsIcon from '@mui/icons-material/CompareArrows'
import { useMutation } from '@tanstack/react-query'
import { useQuery } from '@tanstack/react-query'
import { personaViralFusion, type PersonaFusionScript } from '@/api/shortvideo'
import { shortvideoApi } from '@/api/shortvideo'
import { douyinApi, type DyPersona } from '@/api/douyin'
import { productApi } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'

export default function PersonaViralFusionPage() {
  const toast = useToast()
  const [personaId, setPersonaId] = useState<number | ''>(``)
  const [viralVideoId, setViralVideoId] = useState<number | ''>(``)
  const [productId, setProductId] = useState<number | ''>(``)
  const [topic, setTopic] = useState('')
  const [duration, setDuration] = useState(60)
  const [count, setCount] = useState(3)
  const [fusionMode, setFusionMode] = useState<'full_viral' | 'hybrid' | 'persona_led'>('hybrid')
  const [results, setResults] = useState<PersonaFusionScript[]>([])

  const { data: personas = [] } = useQuery({
    queryKey: ['personas-list'],
    queryFn: () => douyinApi.personaList(),
  })

  const { data: viralPage } = useQuery({
    queryKey: ['viral-list-fusion'],
    queryFn: () => shortvideoApi.viralList({ rows: 50 }),
  })
  const viralVideos = Array.isArray(viralPage) ? viralPage : []

  const { data: productsPage } = useQuery({
    queryKey: ['products-list-fusion'],
    queryFn: () => productApi.list({ rows: 100 }),
  })
  const products = productsPage?.list ?? []

  const fusionMut = useMutation({
    mutationFn: () => personaViralFusion({
      personaId: Number(personaId),
      viralVideoId: Number(viralVideoId),
      productId: productId !== '' ? Number(productId) : undefined,
      topic: topic || undefined,
      duration,
      count,
      fusionMode,
    }),
    onSuccess: (data: Record<string, unknown>) => {
      if (data.error) {
        toast(String(data.error), 'error')
        return
      }
      const script = String(data.script ?? '')
      setResults([{ original: '', fused: script, predictedConversionRate: 0 }])
      toast('融合脚本已生成', 'success')
    },
    onError: () => toast('生成失败', 'error'),
  })

  const canGenerate = personaId !== '' && viralVideoId !== ''

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="人设融合爆款"
        subtitle="将爆款视频结构与你的人设融合，生成高转化话术"
      />

      <Grid container spacing={3}>
        {/* 配置面板 */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>融合配置</Typography>
              <Divider sx={{ mb: 2 }} />
              <Stack spacing={2}>
                <FormControl fullWidth size="small" required>
                  <InputLabel>选择人设</InputLabel>
                  <Select value={personaId} label="选择人设 *"
                    onChange={e => setPersonaId(e.target.value as number)}>
                    {(personas as DyPersona[]).map((p) => (
                      <MenuItem key={p.id} value={p.id}>{p.personaName}</MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth size="small" required>
                  <InputLabel>爆款视频</InputLabel>
                  <Select value={viralVideoId} label="爆款视频 *"
                    onChange={e => setViralVideoId(e.target.value as number)}>
                    {viralVideos.map(v => (
                      <MenuItem key={v.id} value={v.id}>{v.title}</MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth size="small">
                  <InputLabel>关联商品（可选）</InputLabel>
                  <Select value={productId} label="关联商品（可选）"
                    onChange={e => setProductId(e.target.value as number)}>
                    <MenuItem value="">不关联</MenuItem>
                    {products.map(p => (
                      <MenuItem key={p.id} value={p.id}>{p.productName}</MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <TextField label="话题/场景（可选）" fullWidth size="small"
                  value={topic} onChange={e => setTopic(e.target.value)}
                  placeholder="如：春节促销、护肤日常"
                />

                <FormControl fullWidth size="small">
                  <InputLabel>融合模式</InputLabel>
                  <Select value={fusionMode} label="融合模式"
                    onChange={e => setFusionMode(e.target.value as typeof fusionMode)}>
                    <MenuItem value="full_viral">完全爆款风格</MenuItem>
                    <MenuItem value="hybrid">混合融合（推荐）</MenuItem>
                    <MenuItem value="persona_led">人设主导</MenuItem>
                  </Select>
                </FormControl>

                <Box>
                  <Typography variant="caption">目标时长：{duration}秒</Typography>
                  <Slider value={duration} min={15} max={180} step={15}
                    marks={[{ value: 15, label: '15s' }, { value: 60, label: '1min' }, { value: 180, label: '3min' }]}
                    onChange={(_e, v) => setDuration(v as number)}
                  />
                </Box>

                <Box>
                  <Typography variant="caption">生成数量：{count} 个</Typography>
                  <Slider value={count} min={1} max={5} step={1}
                    marks onChange={(_e, v) => setCount(v as number)}
                  />
                </Box>

                <Button variant="contained" fullWidth
                  startIcon={fusionMut.isPending ? <CircularProgress size={16} color="inherit" /> : <AutoAwesomeIcon />}
                  onClick={() => fusionMut.mutate()}
                  disabled={!canGenerate || fusionMut.isPending}>
                  {fusionMut.isPending ? 'AI 融合生成中…' : '开始融合生成'}
                </Button>

                {!canGenerate && (
                  <Alert severity="info" sx={{ fontSize: 12 }}>请选择人设和爆款视频</Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* 结果对比 */}
        <Grid item xs={12} md={8}>
          {results.length === 0 ? (
            <Paper sx={{ p: 6, textAlign: 'center', height: 400, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
              <CompareArrowsIcon sx={{ fontSize: 48, color: 'text.disabled' }} />
              <Typography color="text.secondary">配置参数后点击「开始融合生成」</Typography>
              <Typography variant="caption" color="text.disabled">AI 将分析爆款结构并与你的人设融合，生成高转化话术</Typography>
            </Paper>
          ) : (
            <Stack spacing={2}>
              {results.map((script, idx) => (
                <Card key={idx}>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
                      <Stack direction="row" alignItems="center" spacing={1}>
                        <Chip label={`方案 ${idx + 1}`} color="primary" size="small" />
                        <Chip
                          label={`预测转化率 ${(script.predictedConversionRate * 100).toFixed(1)}%`}
                          color={script.predictedConversionRate >= 0.05 ? 'success' : 'default'}
                          size="small"
                        />
                      </Stack>
                      <Button size="small" startIcon={<ContentCopyIcon />}
                        onClick={() => { navigator.clipboard.writeText(script.fused); toast('已复制融合话术', 'success') }}>
                        复制
                      </Button>
                    </Stack>
                    <Grid container spacing={2}>
                      <Grid item xs={12} md={6}>
                        <Typography variant="caption" color="text.secondary" gutterBottom display="block">原始爆款话术</Typography>
                        <Paper sx={{ p: 1.5, bgcolor: 'action.hover', fontSize: 13, whiteSpace: 'pre-wrap', minHeight: 120, maxHeight: 240, overflowY: 'auto' }}>
                          {script.original}
                        </Paper>
                      </Grid>
                      <Grid item xs={12} md={6}>
                        <Typography variant="caption" color="primary" gutterBottom display="block">融合后话术</Typography>
                        <Paper sx={{ p: 1.5, bgcolor: 'primary.50', fontSize: 13, whiteSpace: 'pre-wrap', minHeight: 120, maxHeight: 240, overflowY: 'auto', border: 1, borderColor: 'primary.light' }}>
                          {script.fused}
                        </Paper>
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          )}
        </Grid>
      </Grid>
    </Box>
  )
}