import { useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { PageHeader } from '@/components/base'
import { scriptApi, type ScriptGenerationResult, type ScriptVariant } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'

const STYLE_OPTIONS = [
  { value: 'professional', label: '专业严谨' },
  { value: 'casual', label: '轻松亲切' },
  { value: 'urgent', label: '紧迫促销' },
  { value: 'storytelling', label: '故事叙述' },
]

const GENERATE_ENDPOINT = '/script/generate'
const RELATED_ENDPOINTS = ['/script/optimize', '/script/list', '/script/save']
const UNSUPPORTED_ENDPOINTS = [
  '/short-video/script/generate',
  '/product/script/generate',
  '/copy/ai/generate',
  '/live/ai/generate-product-script',
  '/script/template/by-scene',
  '/script/generate/mock',
]
const UNSUPPORTED_ACTIONS = [
  'inline-script-optimize',
  'inline-library-save',
  'shortvideo-script-generate',
  'product-script-generate',
  'copy-ai-generate',
  'live-product-script-generate',
  'static-mock-variants',
]

function splitFeatures(value: string) {
  return value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export default function ScriptGenerationPage() {
  const toast = useToast()
  const [productName, setProductName] = useState('')
  const [price, setPrice] = useState('')
  const [features, setFeatures] = useState('')
  const [duration, setDuration] = useState(60)
  const [variants, setVariants] = useState(3)
  const [style, setStyle] = useState('professional')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<ScriptGenerationResult | null>(null)

  const featureList = useMemo(() => splitFeatures(features), [features])
  const bestVariant = result?.variants?.[0]

  const handleGenerate = async () => {
    if (!productName.trim()) {
      toast('请填写商品名称', 'warning')
      return
    }
    if (featureList.length === 0) {
      toast('请填写核心卖点', 'warning')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await scriptApi.generate({
        productName: productName.trim(),
        productPrice: Number(price) > 0 ? Number(price) : 0.01,
        keyFeatures: featureList,
        duration,
        style,
        variants,
      })
      setResult({
        ...res,
        variants: Array.isArray(res.variants) ? res.variants : [],
      })
      toast('话术已生成', 'success')
    } catch (e) {
      const msg = e instanceof Error ? e.message : '生成失败，请检查模型与缓存服务'
      setError(`话术生成失败：${msg}。接口来源：${GENERATE_ENDPOINT}；上下文：route=/admin/script/generation; productName=${productName.trim()}; style=${style}; duration=${duration}; variants=${variants}; featureCount=${featureList.length}。失败会保留当前输入，不使用 mock 版本填充。`)
      toast(msg, 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => toast('已复制', 'success'))
  }

  const renderVariant = (variant: ScriptVariant, index: number) => (
    <Card
      key={variant.id || index}
      variant="outlined"
      data-testid={`script-generation-variant-${index}`}
      data-contract-source={`${GENERATE_ENDPOINT} variants`}
      data-variant-id={variant.id}
      data-variant-score={variant.score ?? ''}
      data-no-mock-variant="true"
    >
      <CardContent>
        <Stack spacing={1.5}>
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
            <Typography variant="subtitle1" fontWeight={700}>版本 {index + 1}</Typography>
            {variant.score != null && <Chip size="small" label={`评分 ${Number(variant.score).toFixed(1)}`} color="primary" />}
            {variant.keyPoints && <Chip size="small" label={variant.keyPoints} variant="outlined" />}
            <Box sx={{ flex: 1 }} />
            <Button
              size="small"
              startIcon={<ContentCopyIcon />}
              onClick={() => handleCopy(variant.content)}
              data-contract-action="copy-backend-variant"
            >
              复制
            </Button>
          </Stack>
          <Typography
            component="pre"
            sx={{ m: 0, p: 1.5, bgcolor: 'action.hover', borderRadius: 1, whiteSpace: 'pre-wrap', lineHeight: 1.7 }}
          >
            {variant.content}
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  )

  return (
    <Box
      data-testid="script-generation-workbench"
      data-contract-scope="script-generation"
      data-ready-endpoints={GENERATE_ENDPOINT}
      data-related-endpoints={RELATED_ENDPOINTS.join('|')}
      data-unsupported-endpoints={UNSUPPORTED_ENDPOINTS.join('|')}
      data-unsupported-actions={UNSUPPORTED_ACTIONS.join('|')}
      data-product-name-length={productName.trim().length}
      data-feature-count={featureList.length}
      data-duration={duration}
      data-variants-requested={variants}
      data-style={style}
      data-result-id={result?.id ?? ''}
      data-result-variant-count={result?.variants?.length ?? 0}
      data-generation-time-ms={result?.generationTime ?? ''}
      data-no-mock-variants="true"
    >
      <PageHeader
        title="话术生成"
        breadcrumbs={[{ label: '话术管理' }, { label: '话术生成' }]}
        subtitle="按后端 /script/generate 契约生成多版本话术：商品、价格、卖点、时长、风格与版本数均为必填口径。"
      />

      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="script-generation-contract-alert"
        data-contract-source={GENERATE_ENDPOINT}
        data-no-script-optimize-request="true"
        data-no-library-save-request="true"
        data-no-shortvideo-generate-request="true"
        data-no-product-script-generate-request="true"
        data-no-copy-ai-generate-request="true"
        data-no-live-script-generate-request="true"
        data-no-mock-variants="true"
      >
        数据源：{GENERATE_ENDPOINT}。该接口会写入生成记录和版本结果；页面只展示后端返回内容，不补静态示例。
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} md={5}>
          <Card
            variant="outlined"
            data-testid="script-generation-form-card"
            data-ready-endpoint={GENERATE_ENDPOINT}
            data-contract-payload="productName|productPrice|keyFeatures|duration|style|variants"
          >
            <CardContent>
              <Stack spacing={2}>
                <TextField
                  label="商品名称"
                  required
                  value={productName}
                  onChange={(e) => setProductName(e.target.value)}
                  inputProps={{ 'data-contract-param': 'productName' }}
                />
                <TextField
                  label="价格"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  type="number"
                  inputProps={{ min: 0.01, step: 0.01, 'data-contract-param': 'productPrice' }}
                />
                <TextField
                  label="核心卖点"
                  required
                  multiline
                  minRows={4}
                  value={features}
                  onChange={(e) => setFeatures(e.target.value)}
                  placeholder="一行一个，或用逗号分隔"
                  inputProps={{ 'data-contract-param': 'keyFeatures' }}
                />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField
                    label="时长（秒）"
                    type="number"
                    value={duration}
                    onChange={(e) => setDuration(Math.max(15, Math.min(300, Number(e.target.value) || 60)))}
                    inputProps={{ min: 15, max: 300, 'data-contract-param': 'duration' }}
                    sx={{ flex: 1 }}
                  />
                  <TextField
                    label="版本数"
                    type="number"
                    value={variants}
                    onChange={(e) => setVariants(Math.max(1, Math.min(5, Number(e.target.value) || 3)))}
                    inputProps={{ min: 1, max: 5, 'data-contract-param': 'variants' }}
                    sx={{ flex: 1 }}
                  />
                </Stack>
                <Alert
                  severity="info"
                  data-testid="script-generation-payload-alert"
                  data-contract-source={GENERATE_ENDPOINT}
                  data-key-features-shape="array"
                >
                  请求字段：productName、productPrice、keyFeatures、duration、style、variants。卖点会以数组提交，避免后端校验把逗号字符串判为无效。
                </Alert>
                <FormControl>
                  <InputLabel id="script-generation-style-label">话术风格</InputLabel>
                  <Select
                    labelId="script-generation-style-label"
                    id="script-generation-style"
                    value={style}
                    label="话术风格"
                    onChange={(e) => setStyle(e.target.value)}
                    inputProps={{ 'data-contract-param': 'style' }}
                  >
                    {STYLE_OPTIONS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
                  </Select>
                </FormControl>
                <Button
                  variant="contained"
                  startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <AutoAwesomeIcon />}
                  onClick={handleGenerate}
                  disabled={loading}
                  data-contract-action="generateScript"
                  data-ready-endpoint={GENERATE_ENDPOINT}
                >
                  {loading ? 'AI 生成中...' : 'AI 生成话术'}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={7}>
          <Stack spacing={2}>
            <Card
              variant="outlined"
              data-testid="script-generation-summary-card"
              data-contract-source="local-form-state|/script/generate"
              data-feature-count={featureList.length}
              data-duration={duration}
              data-variants-requested={variants}
              data-generation-time-ms={result?.generationTime ?? ''}
            >
              <CardContent>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  <Chip label={`卖点 ${featureList.length}`} color={featureList.length > 0 ? 'success' : 'default'} />
                  <Chip label={`${duration}s`} variant="outlined" />
                  <Chip label={`${variants} 个版本`} variant="outlined" />
                  {result?.generationTime != null && <Chip label={`${result.generationTime}ms`} color="primary" variant="outlined" />}
                </Stack>
                <Divider sx={{ my: 1.5 }} />
                <Typography variant="body2" color="text.secondary">
                  返回结果按评分排序。若生成失败，优先检查 AI 服务、Redis 缓存和 `/script/generate` 参数校验。
                </Typography>
              </CardContent>
            </Card>

            {error && (
              <Alert
                severity="error"
                data-testid="script-generation-error"
                data-contract-source={GENERATE_ENDPOINT}
                data-no-mock-variants="true"
                data-product-name={productName.trim()}
                data-style={style}
                data-feature-count={featureList.length}
              >
                {error}
              </Alert>
            )}

            {!result && !loading && (
              <Alert
                severity="info"
                data-testid="script-generation-empty-input-hint"
                data-contract-source="empty-before-generate"
              >
                填写商品与卖点后生成多版本话术，生成记录会由后端写入 `script_generation` 与 `script_variant`。
              </Alert>
            )}

            {result && result.variants.length === 0 && (
              <Alert
                severity="warning"
                data-testid="script-generation-empty-variants"
                data-contract-source={GENERATE_ENDPOINT}
                data-no-mock-variants="true"
              >
                {GENERATE_ENDPOINT} 已返回生成批次，但未返回 variants。请检查 AI 生成链路、评分链路和 `sc_script_variant` 写入，不使用 mock 版本填充。
              </Alert>
            )}

            {bestVariant && (
              <Alert
                severity="success"
                data-testid="script-generation-best-variant"
                data-contract-source={`${GENERATE_ENDPOINT} variants[0]`}
              >
                当前最高分版本：{bestVariant.score != null ? Number(bestVariant.score).toFixed(1) : '未返回评分'}。
              </Alert>
            )}

            {result?.variants?.map(renderVariant)}
          </Stack>
        </Grid>
      </Grid>
    </Box>
  )
}
