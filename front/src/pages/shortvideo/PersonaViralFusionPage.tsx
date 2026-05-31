import { useMemo, useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Button, TextField,
  Grid, CircularProgress, Paper, Divider, Chip, FormControl,
  InputLabel, Select, MenuItem, Slider, Alert, Skeleton,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import CompareArrowsIcon from '@mui/icons-material/CompareArrows'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useMutation } from '@tanstack/react-query'
import { useQuery } from '@tanstack/react-query'
import {
  matchPersonasForViral,
  personaViralFusion,
  type PersonaFusionResult,
  type PersonaFusionScript,
  type PersonaMatchResult,
} from '@/api/shortvideo'
import { shortvideoApi } from '@/api/shortvideo'
import { douyinApi, type DyPersona } from '@/api/douyin'
import { productApi } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

interface FusionDisplayResult extends PersonaFusionScript {
  scriptId?: number
  fusionScore?: number
  tokensUsed?: number
  remakeType?: string
  constraints: string[]
  constraintsApplied?: PersonaFusionResult['constraintsApplied']
}

const PERSONA_FUSION_ENDPOINTS = {
  personas: '/douyin/persona/list',
  viralList: '/short-video/viral/list',
  products: '/product/search',
  match: '/short-video/persona-fusion/match-personas',
  generate: '/short-video/persona-fusion/generate-fused-script',
} as const
const PERSONA_FUSION_READY_ENDPOINTS = [
  PERSONA_FUSION_ENDPOINTS.personas,
  PERSONA_FUSION_ENDPOINTS.viralList,
  PERSONA_FUSION_ENDPOINTS.products,
  PERSONA_FUSION_ENDPOINTS.match,
  PERSONA_FUSION_ENDPOINTS.generate,
].join('|')
const PERSONA_FUSION_READY_ROUTES = [
  shortvideoRoutes.personaFusion,
  `${shortvideoRoutes.personaFusion}?viralVideoId=:id&personaId=:id`,
  shortvideoRoutes.scriptPlanning,
].join('|')
const PERSONA_FUSION_SUPPORTED_ACTIONS = [
  'refresh-fusion-dependencies',
  'match-personas-for-viral',
  'generate-fused-script',
  'copy-fused-script',
  'apply-product-topic-constraints',
].join('|')
const PERSONA_FUSION_UNSUPPORTED_ENDPOINTS = [
  '/short-video/persona-fusion/mock',
  '/short-video/persona-fusion/local-personas',
  '/short-video/persona-fusion/local-viral',
  '/short-video/persona-fusion/local-products',
  '/short-video/persona-fusion/local-match',
  '/short-video/persona-fusion/local-generate',
  '/short-video/persona-fusion/static-score',
  '/short-video/project/local-create-from-fusion',
].join('|')

function buildFallbackOriginal(viralVideos: Array<{ id: number; title?: string; transcript?: string; description?: string }>, viralVideoId: number | '') {
  const viral = viralVideos.find(v => Number(v.id) === Number(viralVideoId))
  if (!viral) return '已选择爆款视频，但列表接口未返回口播原文。'
  return String(viral.transcript ?? viral.description ?? viral.title ?? '爆款素材未返回原始话术。')
}

export default function PersonaViralFusionPage() {
  const toast = useToast()
  const [personaId, setPersonaId] = useState<number | ''>(``)
  const [viralVideoId, setViralVideoId] = useState<number | ''>(``)
  const [productId, setProductId] = useState<number | ''>(``)
  const [topic, setTopic] = useState('')
  const [duration, setDuration] = useState(60)
  const [count, setCount] = useState(3)
  const [fusionMode, setFusionMode] = useState<'full_viral' | 'hybrid' | 'persona_led'>('hybrid')
  const [results, setResults] = useState<FusionDisplayResult[]>([])
  const [fusionFailure, setFusionFailure] = useState<string | null>(null)

  const {
    data: personas = [],
    isLoading: personasLoading,
    isError: personasIsError,
    error: personasError,
    refetch: refetchPersonas,
  } = useQuery({
    queryKey: ['personas-list'],
    queryFn: () => douyinApi.personaList(),
  })

  const {
    data: viralPage,
    isLoading: viralLoading,
    isError: viralIsError,
    error: viralError,
    refetch: refetchViral,
  } = useQuery({
    queryKey: ['viral-list-fusion'],
    queryFn: () => shortvideoApi.viralList({ rows: 50 }),
  })
  const viralVideos = Array.isArray(viralPage) ? viralPage : []

  const selectedViralId = viralVideoId !== '' ? Number(viralVideoId) : undefined
  const {
    data: personaMatches = [],
    isLoading: matchLoading,
    isError: matchIsError,
    error: matchError,
    refetch: refetchMatches,
  } = useQuery({
    queryKey: ['persona-match-for-viral', selectedViralId],
    enabled: selectedViralId != null && Number.isFinite(selectedViralId),
    queryFn: () => matchPersonasForViral(Number(selectedViralId)),
  })

  const {
    data: productsPage,
    isLoading: productsLoading,
    isError: productsIsError,
    error: productsError,
    refetch: refetchProducts,
  } = useQuery({
    queryKey: ['products-list-fusion'],
    queryFn: () => productApi.list({ rows: 100 }),
  })
  const products = productsPage?.list ?? []

  const selectedPersona = useMemo(
    () => (personas as DyPersona[]).find(p => Number(p.id) === Number(personaId)),
    [personas, personaId],
  )
  const selectedViral = useMemo(
    () => viralVideos.find(v => Number(v.id) === Number(viralVideoId)),
    [viralVideos, viralVideoId],
  )
  const selectedProduct = useMemo(
    () => products.find(p => Number(p.id) === Number(productId)),
    [products, productId],
  )

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
    onSuccess: (data: PersonaFusionResult) => {
      setFusionFailure(null)
      if (data.error) {
        const message = String(data.error)
        setFusionFailure(message)
        toast(message, 'error')
        return
      }
      const script = String(data.script ?? '')
      if (!script.trim()) {
        const message = '融合服务未返回 script 字段，请检查 LLM 配置、爆款素材文本和服务端日志。'
        setFusionFailure(message)
        toast(message, 'error')
        return
      }
      const constraints = [
        selectedPersona ? `人设：${selectedPersona.personaName}` : `人设 ID：${personaId}`,
        selectedViral ? `爆款：${selectedViral.title}` : `爆款 ID：${viralVideoId}`,
        data.constraintsApplied?.productName
          ? `商品已写入后端约束：${data.constraintsApplied.productName}`
          : selectedProduct
            ? `商品约束已提交：${selectedProduct.productName}`
            : '未关联商品',
        data.constraintsApplied?.topic
          ? `话题已写入后端约束：${data.constraintsApplied.topic}`
          : topic.trim()
            ? `话题约束已提交：${topic.trim()}`
            : '未填写话题约束',
        `目标时长已提交：${data.constraintsApplied?.durationSeconds ?? duration} 秒`,
        `生成数量已提交：${data.constraintsApplied?.count ?? count} 个`,
      ]
      setResults([{
        original: buildFallbackOriginal(viralVideos, viralVideoId),
        fused: script,
        predictedConversionRate: Number(data.fusionScore ?? 0),
        scriptId: data.scriptId,
        fusionScore: data.fusionScore,
        tokensUsed: data.tokensUsed,
        remakeType: data.remakeType,
        constraints,
        constraintsApplied: data.constraintsApplied,
      }])
      toast('融合脚本已生成', 'success')
    },
    onError: (error) => {
      const message = getErrorMessage(error)
      setFusionFailure(message)
      toast(`生成失败：${message}`, 'error')
    },
  })

  const canGenerate = personaId !== '' && viralVideoId !== ''
  const dependencyLoading = personasLoading || viralLoading || productsLoading || matchLoading
  const dependencyError = personasIsError || viralIsError || productsIsError

  const matchList = Array.isArray(personaMatches) ? personaMatches as PersonaMatchResult[] : []

  return (
    <Box
      data-testid="persona-fusion-page"
      data-ready-endpoints={PERSONA_FUSION_READY_ENDPOINTS}
      data-ready-routes={PERSONA_FUSION_READY_ROUTES}
      data-supported-actions={PERSONA_FUSION_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={PERSONA_FUSION_UNSUPPORTED_ENDPOINTS}
      data-no-local-persona-fallback="true"
      data-no-local-viral-fallback="true"
      data-no-local-product-fallback="true"
      data-no-local-fusion-fallback="true"
      data-input-retained-on-error="true"
      sx={{ p: 3 }}
    >
      <PageHeader
        title="人设融合爆款"
        subtitle="将爆款视频结构与你的人设融合，生成高转化话术"
        actions={
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => { refetchPersonas(); refetchViral(); refetchProducts(); if (selectedViralId) refetchMatches() }}
            disabled={dependencyLoading}
            data-testid="persona-fusion-refresh-button"
            data-source-endpoints={`${PERSONA_FUSION_ENDPOINTS.personas}|${PERSONA_FUSION_ENDPOINTS.viralList}|${PERSONA_FUSION_ENDPOINTS.products}|${PERSONA_FUSION_ENDPOINTS.match}`}
          >
            刷新素材
          </Button>
        }
      />

      <Stack spacing={1} sx={{ mb: 2 }}>
        <Alert
          severity="info"
          variant="outlined"
          data-testid="persona-fusion-boundary-contract"
          data-source-endpoints={PERSONA_FUSION_READY_ENDPOINTS}
          data-no-local-fusion-fallback="true"
          data-no-local-project-create="true"
          data-supported-actions={PERSONA_FUSION_SUPPORTED_ACTIONS}
        >
          融合生成写入 POST {PERSONA_FUSION_ENDPOINTS.generate}，真实入参为 `viralVideoId/personaId/remakeType/productId/topic/duration/count`；商品、话题、目标时长和生成数量会写入后端 Prompt 约束。
        </Alert>
        {selectedViralId && matchIsError && (
          <Alert severity="warning" data-testid="persona-fusion-match-error" data-no-local-match-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchMatches()}>重试</Button>}>
            爆款匹配人设不可用（POST {PERSONA_FUSION_ENDPOINTS.match}）：{getErrorMessage(matchError)}。不影响手动选择人设后继续融合。
          </Alert>
        )}
        {dependencyError && (
          <Alert severity="error" data-testid="persona-fusion-dependency-error" data-no-local-persona-fallback="true" data-no-local-viral-fallback="true" data-no-local-product-fallback="true">
            素材加载异常：
            {personasIsError ? ` POST ${PERSONA_FUSION_ENDPOINTS.personas} 人设=${getErrorMessage(personasError)}；` : ''}
            {viralIsError ? ` POST ${PERSONA_FUSION_ENDPOINTS.viralList} 爆款=${getErrorMessage(viralError)}；` : ''}
            {productsIsError ? ` POST ${PERSONA_FUSION_ENDPOINTS.products} 商品=${getErrorMessage(productsError)}；` : ''}
            页面不会补静态人设、爆款或商品。
          </Alert>
        )}
        {fusionFailure && (
          <Alert severity="error" data-testid="persona-fusion-generate-error" data-no-local-fusion-fallback="true" data-input-retained="true">
            融合生成失败（POST {PERSONA_FUSION_ENDPOINTS.generate}）：{fusionFailure}。请检查爆款是否已完成文本拆解、人设是否可见、LLM 服务是否可用；已保留当前选择和约束输入。
          </Alert>
        )}
      </Stack>

      <Grid container spacing={2} data-testid="persona-fusion-dependency-contract" data-source-endpoints={`${PERSONA_FUSION_ENDPOINTS.personas}|${PERSONA_FUSION_ENDPOINTS.viralList}|${PERSONA_FUSION_ENDPOINTS.products}|${PERSONA_FUSION_ENDPOINTS.match}`} data-no-local-persona-fallback="true" data-no-local-viral-fallback="true" data-no-local-product-fallback="true" sx={{ mb: 3 }}>
        {[
          { label: '可选人设', value: (personas as DyPersona[]).length, hint: PERSONA_FUSION_ENDPOINTS.personas },
          { label: '爆款素材', value: viralVideos.length, hint: PERSONA_FUSION_ENDPOINTS.viralList },
          { label: '可选商品', value: products.length, hint: PERSONA_FUSION_ENDPOINTS.products },
          { label: '匹配建议', value: selectedViralId ? matchList.length : '-', hint: PERSONA_FUSION_ENDPOINTS.match },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
                <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        {/* 配置面板 */}
        <Grid item xs={12} md={4}>
          <Card data-testid="persona-fusion-config-contract" data-source-endpoint={PERSONA_FUSION_ENDPOINTS.generate} data-input-retained-on-error="true">
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>融合配置</Typography>
              <Divider sx={{ mb: 2 }} />
              <Stack spacing={2}>
                {dependencyLoading && (
                  <Stack spacing={1}>
                    <Skeleton variant="rounded" height={40} />
                    <Skeleton variant="rounded" height={40} />
                    <Skeleton variant="rounded" height={40} />
                  </Stack>
                )}
                <FormControl fullWidth size="small" required>
                  <InputLabel id="persona-fusion-persona-label">选择人设</InputLabel>
                  <Select id="persona-fusion-persona" labelId="persona-fusion-persona-label" value={personaId} label="选择人设 *"
                    onChange={e => setPersonaId(e.target.value as number)}>
                    {(personas as DyPersona[]).length === 0 && <MenuItem disabled value="">暂无人设</MenuItem>}
                    {(personas as DyPersona[]).map((p) => (
                      <MenuItem key={p.id} value={p.id}>{p.personaName}</MenuItem>
                    ))}
                  </Select>
                </FormControl>

                {selectedViralId && (
                  <Alert
                    severity={matchList.length > 0 ? 'success' : 'info'}
                    data-testid="persona-fusion-match-contract"
                    data-source-endpoint={PERSONA_FUSION_ENDPOINTS.match}
                    data-no-local-match-fallback="true"
                    sx={{ fontSize: 12 }}
                  >
                    {matchLoading
                      ? '正在匹配适合该爆款的人设...'
                      : matchList.length > 0
                        ? `匹配建议：${matchList.slice(0, 3).map(m => `${m.personaName ?? `#${m.personaId ?? m.id}`}${m.matchScore != null ? ` ${(Number(m.matchScore) * 100).toFixed(0)}%` : ''}`).join('、')}`
                        : '当前爆款暂无匹配建议，可手动选择人设继续生成。'}
                  </Alert>
                )}

                <FormControl fullWidth size="small" required>
                  <InputLabel id="persona-fusion-viral-label">爆款视频</InputLabel>
                  <Select id="persona-fusion-viral" labelId="persona-fusion-viral-label" value={viralVideoId} label="爆款视频 *"
                    onChange={e => setViralVideoId(e.target.value as number)}>
                    {viralVideos.length === 0 && <MenuItem disabled value="">暂无爆款视频</MenuItem>}
                    {viralVideos.map(v => (
                      <MenuItem key={v.id} value={v.id}>{v.title}</MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth size="small">
                  <InputLabel id="persona-fusion-product-label">关联商品（可选）</InputLabel>
                  <Select id="persona-fusion-product" labelId="persona-fusion-product-label" value={productId} label="关联商品（可选）"
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
                  <InputLabel id="persona-fusion-mode-label">融合模式</InputLabel>
                  <Select id="persona-fusion-mode" labelId="persona-fusion-mode-label" value={fusionMode} label="融合模式"
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
                  disabled={!canGenerate || fusionMut.isPending || dependencyError}
                  data-testid="persona-fusion-generate-button"
                  data-source-endpoint={PERSONA_FUSION_ENDPOINTS.generate}>
                  {fusionMut.isPending ? 'AI 融合生成中…' : '开始融合生成'}
                </Button>

                {!canGenerate && (
                  <Alert severity="info" data-testid="persona-fusion-required-empty" data-no-local-fusion-fallback="true" sx={{ fontSize: 12 }}>请选择人设和爆款视频</Alert>
                )}
                {productId !== '' && (
                  <Alert severity="success" data-testid="persona-fusion-product-constraint" data-source-endpoint={PERSONA_FUSION_ENDPOINTS.generate} sx={{ fontSize: 12 }}>
                    关联商品会提交到后端 `generate-fused-script`，并按归属校验后注入 Prompt。
                  </Alert>
                )}
                {fusionMut.isError && (
                  <Alert severity="error" data-testid="persona-fusion-mutation-error" data-no-local-fusion-fallback="true" data-input-retained="true">
                    融合生成失败（POST {PERSONA_FUSION_ENDPOINTS.generate}）：{getErrorMessage(fusionMut.error)}
                  </Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* 结果对比 */}
        <Grid item xs={12} md={8}>
          {results.length === 0 ? (
            <Paper data-testid="persona-fusion-empty-contract" data-no-local-fusion-fallback="true" sx={{ p: 6, textAlign: 'center', height: 400, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
              <CompareArrowsIcon sx={{ fontSize: 48, color: 'text.disabled' }} />
              <Typography color="text.secondary">配置参数后点击「开始融合生成」</Typography>
              <Typography variant="caption" color="text.disabled">AI 将分析爆款结构并与你的人设融合，生成高转化话术</Typography>
            </Paper>
          ) : (
            <Stack spacing={2}>
              {results.map((script, idx) => (
                <Card key={idx} data-testid="persona-fusion-result-contract" data-source-endpoint={PERSONA_FUSION_ENDPOINTS.generate} data-no-local-fusion-fallback="true" data-no-local-project-create="true">
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
                      <Stack direction="row" alignItems="center" spacing={1}>
                        <Chip label={`方案 ${idx + 1}`} color="primary" size="small" />
                        <Chip
                          label={script.fusionScore != null ? `融合分 ${(Number(script.fusionScore) * 100).toFixed(1)}%` : '未接入数值预测'}
                          color={script.fusionScore != null && Number(script.fusionScore) >= 0.6 ? 'success' : 'default'}
                          size="small"
                        />
                        {script.scriptId != null && <Chip label={`脚本 #${script.scriptId}`} size="small" variant="outlined" />}
                        {script.remakeType && <Chip label={script.remakeType} size="small" variant="outlined" />}
                      </Stack>
                      <Button size="small" startIcon={<ContentCopyIcon />}
                        data-testid="persona-fusion-copy-button"
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
                        <Paper
                          data-testid="persona-fusion-fused-preview"
                          data-source-endpoint={PERSONA_FUSION_ENDPOINTS.generate}
                          sx={(theme) => ({
                            p: 1.5,
                            bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
                            fontSize: 13,
                            whiteSpace: 'pre-wrap',
                            minHeight: 120,
                            maxHeight: 240,
                            overflowY: 'auto',
                            border: 1,
                            borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.48 : 0.32),
                          })}
                        >
                          {script.fused}
                        </Paper>
                      </Grid>
                    </Grid>
                    <Alert severity="info" sx={{ mt: 2 }}>
                      {script.constraints.join('；')}。当前接口不会自动保存到短视频项目，复制后可进入脚本策划或项目工作台继续成片链路。
                    </Alert>
                    {script.constraintsApplied && (
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                        {script.constraintsApplied.productName && <Chip label={`商品：${script.constraintsApplied.productName}`} size="small" color="success" variant="outlined" />}
                        {script.constraintsApplied.topic && <Chip label={`话题：${script.constraintsApplied.topic}`} size="small" color="success" variant="outlined" />}
                        {script.constraintsApplied.durationSeconds != null && <Chip label={`时长：${script.constraintsApplied.durationSeconds}s`} size="small" color="success" variant="outlined" />}
                        {script.constraintsApplied.count != null && <Chip label={`数量：${script.constraintsApplied.count}`} size="small" color="success" variant="outlined" />}
                      </Stack>
                    )}
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
