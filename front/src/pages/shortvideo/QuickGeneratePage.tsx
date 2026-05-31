import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Button, TextField,
  FormControl, InputLabel, Select, MenuItem, Stack,
  CircularProgress, Dialog, DialogTitle, DialogContent,
  DialogActions, Alert, Chip, Grid, LinearProgress,
} from '@mui/material'
import {
  AutoAwesome as AiIcon, NavigateNext as NextIcon, Check as DoneIcon,
  Description as ScriptIcon, ViewColumn as ShotIcon,
} from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useGaifanEntitlementGate } from '@/hooks/useGaifanEntitlementGate'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import type { CreativePlanItem, QuickGenerateResult } from '@/types/shortvideo'

const STYLES = [
  { id: '温馨', label: '温馨治愈' },
  { id: '搞笑', label: '搞笑幽默' },
  { id: '高端', label: '高端大气' },
  { id: '专业', label: '专业干货' },
  { id: '数字人口播带货 产品细节展示', label: '数字人口播 + 产品细节展示' },
]

const QUICK_GENERATE_ENDPOINT = '/short-video/quick/generate'
const QUICK_GENERATE_READY_ENDPOINTS = QUICK_GENERATE_ENDPOINT
const QUICK_GENERATE_READY_ROUTES = [
  shortvideoRoutes.quickGenerate,
  `${shortvideoRoutes.quickGenerate}?keyword=:keyword&hotTopicId=:id`,
  `${shortvideoRoutes.workbench}?projectId=:id`,
  `${shortvideoRoutes.scriptPlanning}?projectId=:id`,
].join('|')
const QUICK_GENERATE_SUPPORTED_ACTIONS = [
  'quick-generate-project-script-shot',
  'prefill-from-hot-topic',
  'navigate-workbench',
  'navigate-script-planning',
].join('|')
const QUICK_GENERATE_UNSUPPORTED_ENDPOINTS = [
  '/short-video/quick/mock',
  '/short-video/quick/local-generate',
  '/short-video/quick/local-project',
  '/short-video/quick/local-script',
  '/short-video/quick/local-shot-list',
  '/short-video/project/local-save',
  '/short-video/script/local-save',
  '/short-video/shot-list/local-save',
].join('|')

function planText(item: CreativePlanItem): string {
  const left = item.time ?? item.type ?? item.label ?? ''
  const right = item.goal ?? item.requirement ?? ''
  return [left, right].filter(Boolean).join('：')
}

export default function QuickGeneratePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const toast = useToast()
  const gate = useGaifanEntitlementGate()
  const sourceKeyword = searchParams.get('keyword')?.trim() ?? ''
  const sourceHotTopicId = searchParams.get('hotTopicId')?.trim() ?? ''
  const [theme, setTheme] = useState(() => sourceKeyword)
  const [keywords, setKeywords] = useState(() => sourceKeyword)
  const [style, setStyle] = useState('温馨')
  const [generating, setGenerating] = useState(false)
  const [progressOpen, setProgressOpen] = useState(false)
  const [result, setResult] = useState<QuickGenerateResult | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [degradedMessage, setDegradedMessage] = useState('')

  const handleGenerate = async () => {
    if (!theme) { toast('请输入主题', 'warning'); return }
    if (!(await gate('shortvideo-maker', 'shortvideo-maker.script.generate'))) return
    setGenerating(true)
    setProgressOpen(true)
    setResult(null)
    setErrorMessage('')
    setDegradedMessage('')

    try {
      const res = await shortvideoApi.quickGenerate({
        theme: theme.trim(),
        keywords: keywords.trim() || undefined,
        style,
      })
      setResult(res)
      if (!res.projectId || !res.scriptId || !res.shotListId) {
        setDegradedMessage('快速生成接口已返回，但项目、脚本或分镜 ID 不完整；页面不会补造本地 ID，请检查后端 quick-generate 链路。')
      }
      toast('已生成项目、脚本与分镜', 'success')
    } catch (e) {
      const message = e instanceof Error ? e.message : '一键生成失败'
      setErrorMessage(message)
      toast(message, 'error')
      setProgressOpen(false)
    } finally {
      setGenerating(false)
    }
  }

  return (
    <Box
      data-testid="quick-generate-page"
      data-ready-endpoints={QUICK_GENERATE_READY_ENDPOINTS}
      data-ready-routes={QUICK_GENERATE_READY_ROUTES}
      data-supported-actions={QUICK_GENERATE_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={QUICK_GENERATE_UNSUPPORTED_ENDPOINTS}
      data-no-local-quick-generate="true"
      data-no-client-id-synthesis="true"
      data-no-local-project-script-shot="true"
    >
      <PageHeader
        title="一键快速生成"
        breadcrumbs={[{ label: '短视频' }, { label: '快速生成' }]}
        subtitle="调用后端 quick-generate：一次生成项目、脚本与分镜，素材与成片在项目工作台继续推进。"
      />

      <Grid container spacing={2}>
        <Grid item xs={12} md={7}>
          <Card variant="outlined" data-testid="quick-generate-form-card" data-input-retained="true">
            <CardContent>
              <Stack spacing={3}>
                <TextField
                  label="视频主题" required
                  value={theme} onChange={(e) => setTheme(e.target.value)}
                  placeholder="例如：护肤品开箱测评、美妆新手入门..."
                />
                <TextField
                  label="关键词（用逗号分隔）"
                  value={keywords} onChange={(e) => setKeywords(e.target.value)}
                  placeholder="例如：美白、保湿、平价好物..."
                />
                <FormControl>
                  <InputLabel id="quick-generate-style-label">视频风格</InputLabel>
                  <Select
                    labelId="quick-generate-style-label"
                    id="quick-generate-style"
                    value={style}
                    label="视频风格"
                    onChange={(e) => setStyle(e.target.value)}
                    data-testid="quick-generate-style-select"
                  >
                    {STYLES.map((s) => <MenuItem key={s.id} value={s.id}>{s.label}</MenuItem>)}
                  </Select>
                </FormControl>
                <Button
                  variant="contained" size="large"
                  startIcon={generating ? <CircularProgress size={18} color="inherit" /> : <AiIcon />}
                  onClick={handleGenerate}
                  disabled={generating}
                  fullWidth
                  data-testid="quick-generate-submit-button"
                  data-source-endpoint={QUICK_GENERATE_ENDPOINT}
                >
                  {generating ? '生成中...' : '一键生成脚本与分镜'}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={5}>
          <Card
            variant="outlined"
            data-testid="quick-generate-chain-contract"
            data-no-client-id-synthesis="true"
            data-supported-actions={QUICK_GENERATE_SUPPORTED_ACTIONS}
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>链路说明</Typography>
              <Stack spacing={1.5}>
                <Alert
                  severity="info"
                  data-testid="quick-generate-boundary-contract"
                  data-no-local-quick-generate="true"
                  data-supported-actions={QUICK_GENERATE_SUPPORTED_ACTIONS}
                >
                  生成项目、创作简报、脚本、分镜和生产清单。
                </Alert>
                {sourceKeyword && (
                  <Alert severity="success" variant="outlined" data-testid="quick-generate-hot-topic-prefill" data-navigation-param-only="true">
                    来自热点{sourceHotTopicId ? ` #${sourceHotTopicId}` : ''}：{sourceKeyword}，已预填主题与关键词。
                  </Alert>
                )}
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  <Chip icon={<DoneIcon />} label="项目" color={result?.projectId ? 'success' : 'default'} />
                  <Chip icon={<ScriptIcon />} label="脚本" color={result?.scriptId ? 'success' : 'default'} />
                  <Chip icon={<ShotIcon />} label="分镜" color={result?.shotListId ? 'success' : 'default'} />
                </Stack>
                <Typography variant="body2" color="text.secondary">
                  生成成功后进入项目工作台，继续完成参考图、关键帧、图生视频、剪辑与发布。
                </Typography>
              </Stack>
            </CardContent>
          </Card>
      </Grid>
    </Grid>

      {degradedMessage && (
        <Alert
          severity="warning"
          data-testid="quick-generate-degraded"
          data-no-client-id-synthesis="true"
          data-no-local-project-script-shot="true"
          sx={{ mt: 2 }}
        >
          {degradedMessage} 接口来源：POST {QUICK_GENERATE_ENDPOINT}。
        </Alert>
      )}

      {errorMessage && (
        <Alert
          severity="error"
          data-testid="quick-generate-error"
          data-input-retained="true"
          data-no-local-quick-generate="true"
          data-no-client-id-synthesis="true"
          sx={{ mt: 2 }}
        >
          一键生成失败（POST {QUICK_GENERATE_ENDPOINT}）：{errorMessage}。请检查脚本生成和分镜生成链路是否可用；页面不会补造项目、脚本或分镜 ID。
        </Alert>
      )}

      {result && (
        <Card
          variant="outlined"
          data-testid="quick-generate-result-card"
          data-source-endpoint={QUICK_GENERATE_ENDPOINT}
          data-no-client-id-synthesis="true"
          sx={{ mt: 2 }}
        >
          <CardContent>
            <Stack spacing={1.5}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <Typography variant="subtitle1" fontWeight={700}>{result.title || '快速生成结果'}</Typography>
                <Chip size="small" label={`项目 #${result.projectId}`} color="primary" />
                {result.scriptId && <Chip size="small" label={`脚本 #${result.scriptId}`} variant="outlined" />}
                {result.shotListId && <Chip size="small" label={`分镜 #${result.shotListId}`} variant="outlined" />}
              </Stack>
              {result.scriptContent && (
                <Typography
                  component="pre"
                  sx={{ m: 0, p: 1.5, bgcolor: 'action.hover', borderRadius: 1, whiteSpace: 'pre-wrap', maxHeight: 220, overflow: 'auto' }}
                >
                  {result.scriptContent}
                </Typography>
              )}
              {result.creativeBrief && (
                <Grid container spacing={1.5}>
                  <Grid item xs={12} md={6}>
                    <Stack spacing={1}>
                      <Typography variant="subtitle2">创作简报</Typography>
                      <Alert severity="info" variant="outlined">
                        {result.creativeBrief.targetAudience || '目标用户待补充'}
                      </Alert>
                      <Typography variant="body2" color="text.secondary">
                        {result.creativeBrief.corePromise || '核心承诺待补充'}
                      </Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                        {(result.creativeBrief.keywords ?? []).slice(0, 6).map((kw) => (
                          <Chip key={kw} size="small" label={kw} />
                        ))}
                        {result.creativeBrief.durationSeconds && <Chip size="small" label={`${result.creativeBrief.durationSeconds}s`} />}
                        {result.creativeBrief.aspectRatio && <Chip size="small" label={result.creativeBrief.aspectRatio} />}
                      </Stack>
                    </Stack>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Stack spacing={1}>
                      <Typography variant="subtitle2">前三秒钩子</Typography>
                      {(result.creativeBrief.hookOptions ?? []).slice(0, 3).map((hook) => (
                        <Typography key={hook} variant="body2" sx={{ p: 1, bgcolor: 'action.hover', borderRadius: 1 }}>
                          {hook}
                        </Typography>
                      ))}
                    </Stack>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Stack spacing={1}>
                      <Typography variant="subtitle2">叙事节拍</Typography>
                      {(result.creativeBrief.storyBeats ?? []).map((item, index) => (
                        <Typography key={`${item.time ?? index}`} variant="body2">{planText(item)}</Typography>
                      ))}
                    </Stack>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Stack spacing={1}>
                      <Typography variant="subtitle2">素材计划</Typography>
                      {(result.creativeBrief.materialPlan ?? []).map((item, index) => (
                        <Typography key={`${item.type ?? index}`} variant="body2">{planText(item)}</Typography>
                      ))}
                    </Stack>
                  </Grid>
                </Grid>
              )}
              {result.productionPlan?.nextActions?.length ? (
                <Alert severity="success" variant="outlined">
                  下一步：{result.productionPlan.nextActions.slice(0, 3).join(' / ')}
                </Alert>
              ) : null}
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Button
                  variant="contained"
                  onClick={() => navigate(`${shortvideoRoutes.workbench}?projectId=${result.projectId}`)}
                  data-testid="quick-generate-open-workbench-button"
                  data-target-route={`${shortvideoRoutes.workbench}?projectId=${result.projectId}`}
                >
                  进入项目工作台
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => navigate(`${shortvideoRoutes.scriptPlanning}?projectId=${result.projectId}`)}
                  data-testid="quick-generate-open-script-planning-button"
                  data-target-route={`${shortvideoRoutes.scriptPlanning}?projectId=${result.projectId}`}
                >
                  编辑脚本
                </Button>
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* 进度弹窗 */}
      <Dialog open={progressOpen} maxWidth="sm" fullWidth disableEscapeKeyDown>
        <DialogTitle>{generating ? '正在生成...' : '生成已完成'}</DialogTitle>
        <DialogContent data-testid="quick-generate-progress-dialog" data-no-local-step-simulation="true">
          {generating ? (
            <Stack spacing={1.5}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <CircularProgress size={18} />
                <Typography variant="body2">正在生成项目、脚本与分镜</Typography>
              </Stack>
              <LinearProgress />
            </Stack>
          ) : (
            <Alert icon={<DoneIcon />} severity="success">
              已完成项目、脚本与分镜初始化。素材、图生视频与合成请在项目工作台继续处理。
            </Alert>
          )}
        </DialogContent>
        {!generating && (
          <DialogActions>
            <Button onClick={() => setProgressOpen(false)}>关闭</Button>
            {result?.projectId && (
              <Button
                variant="contained"
                startIcon={<NextIcon />}
                onClick={() => navigate(`${shortvideoRoutes.workbench}?projectId=${result.projectId}`)}
                data-testid="quick-generate-dialog-open-workbench-button"
                data-target-route={`${shortvideoRoutes.workbench}?projectId=${result.projectId}`}
              >
                进入工作台
              </Button>
            )}
          </DialogActions>
        )}
      </Dialog>
    </Box>
  )
}
