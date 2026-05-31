import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  FormControlLabel,
  Grid,
  LinearProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import PublishIcon from '@mui/icons-material/Publish'
import SaveIcon from '@mui/icons-material/Save'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import OfficialReferencesPanel from '@/components/OfficialReferencesPanel'
import { shortvideoApi, adaptContentCalendarMonthView } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import type { PublishReviewResult, PublishResult, SvProject } from '@/types/shortvideo'

const DAYS_OF_WEEK = ['日', '一', '二', '三', '四', '五', '六']
const PROJECT_GET_ENDPOINT = '/short-video/project/get'
const PROJECT_SAVE_ENDPOINT = '/short-video/project/save'
const PUBLISH_TITLE_ENDPOINT = '/short-video/publish/generate-title'
const PUBLISH_REVIEW_ENDPOINT = '/short-video/publish/ai-review'
const PUBLISH_SUBMIT_ENDPOINT = '/short-video/publish/publish'
const CALENDAR_ENDPOINT = '/short-video/content/calendar'
const CONTENT_RECOMMEND_TIME_ENDPOINT = '/short-video/content/publish-time-recommend'
const SEO_RECOMMEND_TIME_ENDPOINT = '/short-video/seo/suggest-publish-time'
const PUBLISH_READY_ENDPOINTS = [
  PROJECT_GET_ENDPOINT,
  PROJECT_SAVE_ENDPOINT,
  PUBLISH_TITLE_ENDPOINT,
  PUBLISH_REVIEW_ENDPOINT,
  PUBLISH_SUBMIT_ENDPOINT,
  CALENDAR_ENDPOINT,
  CONTENT_RECOMMEND_TIME_ENDPOINT,
  SEO_RECOMMEND_TIME_ENDPOINT,
] as const
const PUBLISH_UNSUPPORTED_ENDPOINTS = [
  '/short-video/publish/mock',
  '/short-video/publish/local-title',
  '/short-video/publish/local-review',
  '/short-video/publish/local-publish',
  '/short-video/content/local-calendar',
  '/short-video/content/static-publish-time',
  '/short-video/seo/local-publish-time',
] as const
const PUBLISH_READY_ROUTES = [
  shortvideoRoutes.publish,
  `${shortvideoRoutes.publish}?projectId=:id`,
  shortvideoRoutes.workbench,
].join('|')
const PUBLISH_SUPPORTED_ACTIONS = [
  'refresh-project-publish-state',
  'save-publish-plan',
  'generate-publish-title',
  'ai-review-publish',
  'submit-supported-platform-publish',
  'view-calendar-publish-surface',
  'view-publish-recommend-time',
].join('|')
const PLATFORM_OPTIONS = [
  {
    value: 'douyin',
    label: '抖音',
    status: 'ready',
    statusLabel: '已接入',
    description: '提交到真实发布接口',
    endpoint: PUBLISH_SUBMIT_ENDPOINT,
  },
  {
    value: 'weixin-video',
    label: '视频号',
    status: 'degraded',
    statusLabel: '显式降级',
    description: '后端暂未接入真实发布服务',
    endpoint: PUBLISH_SUBMIT_ENDPOINT,
  },
]
const PUBLISH_PLATFORM_DOWNGRADE_MESSAGE =
  `当前只有抖音会提交到真实发布接口 ${PUBLISH_SUBMIT_ENDPOINT}；未接入平台只展示显式降级，不会混入发布请求。`

function getMonthDates(year: number, month: number) {
  const dates: Date[] = []
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  for (let d = first; d <= last; d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)) {
    dates.push(new Date(d))
  }
  return dates
}

function parsePlatforms(value?: string): string[] {
  if (!value) return ['douyin']
  try {
    const parsed = JSON.parse(value) as unknown
    if (Array.isArray(parsed)) {
      const list = parsed.filter((item): item is string => typeof item === 'string' && item.trim() !== '')
      return list.length > 0 ? list : ['douyin']
    }
  } catch {
    // 非 JSON 时按逗号分隔。
  }
  const list = value.split(',').map((item) => item.trim()).filter(Boolean)
  return list.length > 0 ? list : ['douyin']
}

function formatDateTimeLocal(value?: string) {
  if (!value) return ''
  return value.replace(' ', 'T').slice(0, 16)
}

function isSupportedPublishPlatform(value: string) {
  return value === 'douyin'
}

function getPlatformLabel(value: string) {
  return PLATFORM_OPTIONS.find((platform) => platform.value === value)?.label ?? value
}

function getPlatformStatus(value: string) {
  return PLATFORM_OPTIONS.find((platform) => platform.value === value)?.status ?? 'degraded'
}

function mergeProjectSave(project: SvProject, patch: Partial<SvProject>) {
  return {
    id: project.id,
    accountId: project.accountId,
    title: project.title,
    projectType: project.projectType,
    status: project.status,
    scriptId: project.scriptId,
    shotListId: project.shotListId,
    finalVideoUrl: project.finalVideoUrl,
    thumbnailUrl: project.thumbnailUrl,
    characterReferenceUrl: project.characterReferenceUrl,
    sceneReferenceUrl: project.sceneReferenceUrl,
    duration: project.duration,
    publishTitle: project.publishTitle,
    publishPlatforms: project.publishPlatforms,
    publishTime: project.publishTime,
    reviewStatus: project.reviewStatus,
    relatedProductIds: project.relatedProductIds,
    ...patch,
  }
}

function CalendarPublishView() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())
  const [accountId, setAccountId] = useState('')

  const monthNum = month + 1

  const { data: calArr = [], isLoading: calendarLoading, isError: calendarIsError, error: calendarError, refetch: refetchCalendar } = useQuery({
    queryKey: ['sv-publish-calendar', year, monthNum],
    queryFn: async () => {
      const raw = await shortvideoApi.contentCalendarView(year, monthNum)
      return adaptContentCalendarMonthView(raw)
    },
  })

  const { data: recommendData, isError: recommendIsError, error: recommendError, refetch: refetchRecommend } = useQuery({
    queryKey: ['sv-recommend-time', accountId],
    queryFn: () =>
      accountId.trim()
        ? shortvideoApi.contentPublishTimeRecommend(Number(accountId)).then((rows) =>
            rows.map((r) => {
              const label = r.label != null ? String(r.label) : ''
              const avg = r.avgViewCount != null ? ` · 均播${Number(r.avgViewCount).toLocaleString()}` : ''
              return label ? `${label}${avg}` : JSON.stringify(r)
            }),
          )
        : shortvideoApi.seoSuggestPublishTime({}),
    enabled: true,
  })
  const recommendTimes = Array.isArray(recommendData) ? recommendData as string[] : []

  const calendarMap: Record<string, Array<{ title: string; status: string }>> = {}
  for (const day of calArr) {
    calendarMap[day.date] = day.items ?? []
  }

  const flatList = calArr.flatMap((d) =>
    (d.items ?? []).map((it) => ({
      title: it.title,
      status: it.status,
      publishDate: d.date,
    })),
  )

  const dates = getMonthDates(year, month)
  const firstDayOfWeek = new Date(year, month, 1).getDay()

  const prevMonth = () => { if (month === 0) { setYear(y => y - 1); setMonth(11) } else setMonth(m => m - 1) }
  const nextMonth = () => { if (month === 11) { setYear(y => y + 1); setMonth(0) } else setMonth(m => m + 1) }

  return (
    <Box
      data-testid="shortvideo-publish-calendar-page"
      data-contract-scope="shortvideo-publish-calendar"
      data-ready-endpoints={PUBLISH_READY_ENDPOINTS.join('|')}
      data-ready-routes={PUBLISH_READY_ROUTES}
      data-supported-actions={PUBLISH_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={PUBLISH_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-calendar-fallback="true"
      data-no-static-publish-time="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader title="审核发布" breadcrumbs={[{ label: '短视频' }, { label: '审核发布' }]} />
      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-publish-calendar-boundary-contract"
        data-source-endpoints={`${CALENDAR_ENDPOINT}|${CONTENT_RECOMMEND_TIME_ENDPOINT}|${SEO_RECOMMEND_TIME_ENDPOINT}`}
        data-publish-endpoint={PUBLISH_SUBMIT_ENDPOINT}
        data-no-local-calendar-fallback="true"
        data-supported-actions={PUBLISH_SUPPORTED_ACTIONS}
      >
        未带 projectId 时展示内容日历与发布时间建议；发布动作必须进入具体项目页，后端真实发布接口为 POST {PUBLISH_SUBMIT_ENDPOINT}。
      </Alert>
      <Stack
        direction="row"
        spacing={2}
        alignItems="center"
        flexWrap="wrap"
        data-testid="shortvideo-publish-calendar-filter"
        data-source-endpoints={`${CONTENT_RECOMMEND_TIME_ENDPOINT}|${SEO_RECOMMEND_TIME_ENDPOINT}`}
        data-input-retained="true"
      >
        <TextField size="small" label="抖音账号 ID（可选，填后推荐更准）" value={accountId}
          onChange={e => setAccountId(e.target.value)} sx={{ width: 280 }} />
        <Box sx={{ flex: 1 }} />
        {recommendTimes.length > 0 && (
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
            <Typography variant="caption" color="text.secondary">推荐发布时间：</Typography>
            {recommendTimes.slice(0, 5).map((t, i) => <Chip key={i} label={t} size="small" color="primary" variant="outlined" />)}
          </Stack>
        )}
      </Stack>
      {recommendIsError && (
        <Alert
          severity="warning"
          action={<Button color="inherit" size="small" onClick={() => refetchRecommend()}>重试</Button>}
          data-testid="shortvideo-publish-recommend-error"
          data-source-endpoint={accountId.trim() ? CONTENT_RECOMMEND_TIME_ENDPOINT : SEO_RECOMMEND_TIME_ENDPOINT}
          data-no-static-publish-time="true"
          data-input-retained="true"
        >
          发布时间建议加载失败（POST {accountId.trim() ? CONTENT_RECOMMEND_TIME_ENDPOINT : SEO_RECOMMEND_TIME_ENDPOINT}）：{getErrorMessage(recommendError)}。账号 ID 输入会保留，不使用静态推荐时间补齐。
        </Alert>
      )}

      <Card
        variant="outlined"
        data-testid="shortvideo-publish-calendar-surface"
        data-source-endpoint={CALENDAR_ENDPOINT}
        data-no-local-calendar-fallback="true"
      >
        <CardContent>
          <Stack direction="row" alignItems="center" spacing={2} mb={2}>
            <CalendarMonthIcon color="primary" />
            <Button size="small" onClick={prevMonth}>{'<'}</Button>
            <Typography variant="subtitle1" fontWeight={700}>{year} 年 {month + 1} 月</Typography>
            <Button size="small" onClick={nextMonth}>{'>'}</Button>
          </Stack>

          <Grid container columns={7} sx={{ mb: 0.5 }}>
            {DAYS_OF_WEEK.map(d => (
              <Grid item xs={1} key={d}>
                <Typography variant="caption" color="text.secondary" textAlign="center" display="block" fontWeight={600}>{d}</Typography>
              </Grid>
            ))}
          </Grid>

          <Grid container columns={7}>
            {Array.from({ length: firstDayOfWeek }).map((_, i) => (
              <Grid item xs={1} key={`empty-${i}`}>
                <Box sx={{ height: 80 }} />
              </Grid>
            ))}
            {dates.map((date) => {
              const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
              const items = calendarMap[dateStr] ?? []
              const isToday = dateStr === now.toISOString().slice(0, 10)
              return (
                <Grid item xs={1} key={dateStr}>
                  <Box
                    data-testid={isToday ? 'publish-calendar-today-cell' : undefined}
                    sx={(theme) => ({
                      height: 80,
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 1,
                      p: 0.5,
                      m: 0.25,
                      bgcolor: isToday
                        ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
                        : 'transparent',
                    })}
                  >
                    <Typography variant="caption" fontWeight={isToday ? 700 : 400}
                      color={isToday ? 'primary.main' : 'text.primary'}>
                      {date.getDate()}
                    </Typography>
                    <Stack spacing={0.25} mt={0.25}>
                      {items.slice(0, 2).map((item, i) => (
                        <Chip
                          key={i}
                          label={item.title || '计划'}
                          size="small"
                          sx={{ height: 18, fontSize: 10, '& .MuiChip-label': { px: 0.75 } }}
                          color={item.status === 'published' ? 'success' : 'default'}
                        />
                      ))}
                      {items.length > 2 && (
                        <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>+{items.length - 2} 更多</Typography>
                      )}
                    </Stack>
                  </Box>
                </Grid>
              )
            })}
          </Grid>
          {calendarLoading && <LinearProgress sx={{ mt: 2 }} />}
          {calendarIsError && (
            <Alert
              severity="error"
              sx={{ mt: 2 }}
              action={<Button color="inherit" size="small" onClick={() => refetchCalendar()}>重试</Button>}
              data-testid="shortvideo-publish-calendar-error"
              data-source-endpoint={CALENDAR_ENDPOINT}
              data-no-local-calendar-fallback="true"
            >
              内容日历加载失败（POST {CALENDAR_ENDPOINT}）：{getErrorMessage(calendarError)}。不会展示模拟排期，请检查内容日历聚合表和权限范围。
            </Alert>
          )}
        </CardContent>
      </Card>

      {flatList.length > 0 && (
        <>
          <Typography variant="subtitle2">本月条目（{flatList.length} 条）</Typography>
          <Stack spacing={1} data-testid="shortvideo-publish-calendar-list" data-source-endpoint={CALENDAR_ENDPOINT}>
            {flatList.map((item, i) => (
              <Card key={i} variant="outlined">
                <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
                  <Stack direction="row" spacing={2} alignItems="center">
                    <Typography variant="body2" fontWeight={500} sx={{ flex: 1 }}>
                      {item.title || `内容 ${i + 1}`}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">{item.publishDate}</Typography>
                    <Chip label={item.status === 'published' ? '已发布' : '计划'} size="small" color={item.status === 'published' ? 'success' : 'default'} />
                  </Stack>
                </CardContent>
              </Card>
            ))}
          </Stack>
        </>
      )}

      {flatList.length === 0 && (
        <Typography
          color="text.secondary"
          textAlign="center"
          sx={{ py: 4 }}
          data-testid="shortvideo-publish-calendar-empty"
          data-source-endpoint={CALENDAR_ENDPOINT}
          data-no-local-calendar-fallback="true"
        >
          本月暂无计划/发布数据（数据来自项目排期与已发布视频）
        </Typography>
      )}
    </Box>
  )
}

function ProjectPublishView({ projectId }: { projectId: number }) {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [publishTime, setPublishTime] = useState('')
  const [platforms, setPlatforms] = useState<string[]>(['douyin'])
  const [reviewResult, setReviewResult] = useState<PublishReviewResult | null>(null)
  const [publishResult, setPublishResult] = useState<PublishResult | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data: project, isLoading, isError: projectIsError, error: projectError, refetch: refetchProject } = useQuery({
    queryKey: ['sv-project', projectId],
    queryFn: () => shortvideoApi.get(projectId),
  })

  useEffect(() => {
    if (!project) return
    setTitle(project.publishTitle || project.title || '')
    setPublishTime(formatDateTimeLocal(project.publishTime))
    setPlatforms(parsePlatforms(project.publishPlatforms))
  }, [project])

  const { data: recommendData, isError: recommendIsError, error: recommendError, refetch: refetchRecommend } = useQuery({
    queryKey: ['sv-recommend-time', project?.accountId ?? null],
    queryFn: () =>
      project?.accountId
        ? shortvideoApi.contentPublishTimeRecommend(project.accountId).then((rows) =>
            rows.map((r) => String(r.label ?? JSON.stringify(r))),
          )
        : shortvideoApi.seoSuggestPublishTime({}),
    enabled: !!project,
  })
  const recommendTimes = Array.isArray(recommendData) ? recommendData as string[] : []
  const unsupportedPlatforms = platforms.filter((platform) => !isSupportedPublishPlatform(platform))
  const supportedPlatforms = platforms.filter(isSupportedPublishPlatform)
  const skippedPlatformResults = unsupportedPlatforms.map((platform) => ({
    platform,
    success: false,
    error: '前端已跳过：平台未接入真实发布能力，等待后端平台服务上线。',
  }))

  const flowPercent = useMemo(() => {
    if (project?.status === 'published' || project?.reviewStatus === 'approved') return 100
    if (project?.finalVideoUrl && reviewResult?.passed) return 85
    if (project?.finalVideoUrl) return 65
    return 40
  }, [project?.finalVideoUrl, project?.reviewStatus, project?.status, reviewResult?.passed])

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!project) throw new Error('项目未加载')
      return shortvideoApi.save(mergeProjectSave(project, {
        publishTitle: title.trim(),
        publishPlatforms: JSON.stringify(platforms),
        publishTime: publishTime || undefined,
      }))
    },
    onSuccess: () => {
      setActionError(null)
      toast('发布计划已保存', 'success')
      qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setActionError(`保存发布计划失败（POST ${PROJECT_SAVE_ENDPOINT}）：${message}。发布标题、平台选择和计划时间会保留。`)
      toast(`保存失败：${message}`, 'error')
    },
  })

  const titleMutation = useMutation({
    mutationFn: () => shortvideoApi.publishGenerateTitle({ projectId, videoUrl: project?.finalVideoUrl, count: 5 }),
    onSuccess: (res) => {
      setActionError(null)
      const first = res.titles?.[0]?.text
      if (first) setTitle(first)
      toast('标题已生成', 'success')
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setActionError(`AI 标题生成失败（POST ${PUBLISH_TITLE_ENDPOINT}）：${message}。当前发布标题和平台选择会保留，不使用模拟标题。`)
      toast(`标题生成失败：${message}`, 'error')
    },
  })

  const reviewMutation = useMutation({
    mutationFn: () => shortvideoApi.publishAiReview({
      projectId,
      videoUrl: project?.finalVideoUrl,
      title,
      coverUrl: project?.thumbnailUrl,
    }),
    onSuccess: (res) => {
      setActionError(null)
      setReviewResult(res)
      qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
      toast(res.passed ? 'AI 审核通过' : 'AI 审核未通过', res.passed ? 'success' : 'warning')
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setActionError(`AI 审核失败（POST ${PUBLISH_REVIEW_ENDPOINT}）：${message}。审核结果不会被伪造，当前标题和成片地址会保留。`)
      toast(`审核失败：${message}`, 'error')
    },
  })

  const publishMutation = useMutation({
    mutationFn: () => shortvideoApi.publishSubmit({
      projectId,
      videoUrl: project?.finalVideoUrl,
      title,
      platforms: supportedPlatforms,
      publishTime: publishTime || undefined,
    }),
    onSuccess: (res) => {
      setActionError(null)
      const mergedResult = {
        ...res,
        degraded: Boolean(res.degraded || skippedPlatformResults.length > 0),
        results: [...res.results, ...skippedPlatformResults],
      }
      setPublishResult(mergedResult)
      qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
      toast(
        mergedResult.success && !mergedResult.degraded ? '发布完成' : '发布完成，部分平台已跳过或失败',
        mergedResult.success && !mergedResult.degraded ? 'success' : 'warning',
      )
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setActionError(`发布失败（POST ${PUBLISH_SUBMIT_ENDPOINT}）：${message}。不会写入发布成功状态，发布标题、平台选择和计划时间会保留。`)
      toast(`发布失败：${message}`, 'error')
    },
  })

  const togglePlatform = (value: string) => {
    setPlatforms((prev) => {
      if (prev.includes(value)) {
        const next = prev.filter((item) => item !== value)
        return next.length > 0 ? next : prev
      }
      return [...prev, value]
    })
  }

  return (
    <Box
      data-testid="shortvideo-publish-workbench"
      data-contract-scope="shortvideo-publish"
      data-contract-endpoint={PUBLISH_SUBMIT_ENDPOINT}
      data-ready-endpoints={PUBLISH_READY_ENDPOINTS.join('|')}
      data-ready-routes={PUBLISH_READY_ROUTES}
      data-supported-actions={PUBLISH_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={PUBLISH_UNSUPPORTED_ENDPOINTS.join('|')}
      data-supported-platforms={supportedPlatforms.join(',')}
      data-unsupported-platforms={unsupportedPlatforms.join(',')}
      data-no-local-publish-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="项目发布"
        breadcrumbs={[{ label: '短视频' }, { label: '工作台', href: `${shortvideoRoutes.workbench}?projectId=${projectId}` }, { label: '审核发布' }]}
        subtitle={project ? `项目 #${project.id} · ${project.title}` : `项目 #${projectId}`}
        actions={
          <Button
            variant="outlined"
            onClick={() => navigate(`${shortvideoRoutes.workbench}?projectId=${projectId}`)}
            data-testid="shortvideo-publish-back-workbench-button"
            data-target-route={`${shortvideoRoutes.workbench}?projectId=${projectId}`}
          >
            返回工作台
          </Button>
        }
      />

      {isLoading && <LinearProgress />}

      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-publish-contract-summary"
        data-contract-status="partial"
        data-contract-endpoint={PUBLISH_SUBMIT_ENDPOINT}
        data-source-endpoints={`${PROJECT_GET_ENDPOINT}|${PROJECT_SAVE_ENDPOINT}|${PUBLISH_TITLE_ENDPOINT}|${PUBLISH_REVIEW_ENDPOINT}|${PUBLISH_SUBMIT_ENDPOINT}|${CONTENT_RECOMMEND_TIME_ENDPOINT}|${SEO_RECOMMEND_TIME_ENDPOINT}`}
        data-no-local-publish-fallback="true"
        data-supported-actions={PUBLISH_SUPPORTED_ACTIONS}
      >
        发布链路使用 POST {PUBLISH_TITLE_ENDPOINT}|{PUBLISH_REVIEW_ENDPOINT}|{PUBLISH_SUBMIT_ENDPOINT}；{PUBLISH_PLATFORM_DOWNGRADE_MESSAGE}
      </Alert>

      {projectIsError && (
        <Alert
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetchProject()}>重试</Button>}
          data-testid="shortvideo-publish-project-error"
          data-source-endpoint={PROJECT_GET_ENDPOINT}
          data-no-local-project-fallback="true"
        >
          项目详情加载失败（POST {PROJECT_GET_ENDPOINT}）：{getErrorMessage(projectError)}。请确认项目是否存在、当前账号是否有权限。
        </Alert>
      )}

      {recommendIsError && (
        <Alert
          severity="warning"
          action={<Button color="inherit" size="small" onClick={() => refetchRecommend()}>重试</Button>}
          data-testid="shortvideo-publish-project-recommend-error"
          data-source-endpoint={project?.accountId ? CONTENT_RECOMMEND_TIME_ENDPOINT : SEO_RECOMMEND_TIME_ENDPOINT}
          data-no-static-publish-time="true"
        >
          发布时间建议加载失败（POST {project?.accountId ? CONTENT_RECOMMEND_TIME_ENDPOINT : SEO_RECOMMEND_TIME_ENDPOINT}）：{getErrorMessage(recommendError)}。不会用静态时间补齐。
        </Alert>
      )}

      {actionError && (
        <Alert
          severity="error"
          data-testid="shortvideo-publish-action-error"
          data-source-endpoints={`${PROJECT_SAVE_ENDPOINT}|${PUBLISH_TITLE_ENDPOINT}|${PUBLISH_REVIEW_ENDPOINT}|${PUBLISH_SUBMIT_ENDPOINT}`}
          data-no-local-publish-fallback="true"
          data-input-retained="true"
        >
          {actionError}
        </Alert>
      )}

      {project && !project.finalVideoUrl && (
        <Alert
          severity="warning"
          action={<Button color="inherit" size="small" onClick={() => navigate(`${shortvideoRoutes.editing}?projectId=${projectId}`)}>去成片</Button>}
          data-testid="shortvideo-publish-final-video-missing"
          data-source-endpoint={PROJECT_GET_ENDPOINT}
          data-no-placeholder-video="true"
        >
          当前项目还没有成片地址，请先完成自动剪辑或上传成片。
        </Alert>
      )}

      <Card variant="outlined" data-testid="shortvideo-publish-project-status" data-source-endpoint={PROJECT_GET_ENDPOINT}>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle1" fontWeight={700}>成片与发布状态</Typography>
              <LinearProgress variant="determinate" value={flowPercent} sx={{ mt: 1, mb: 1, height: 8, borderRadius: 4 }} />
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Chip size="small" label={project?.status ?? 'draft'} />
                <Chip size="small" label={`审核 ${project?.reviewStatus ?? 'pending'}`} color={project?.reviewStatus === 'approved' ? 'success' : 'default'} />
                {project?.duration && <Chip size="small" label={`${project.duration}s`} variant="outlined" />}
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, wordBreak: 'break-all' }}>
                {project?.finalVideoUrl || '暂无成片 URL'}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1} alignItems="flex-start">
              {project?.finalVideoUrl && (
                <Button
                  variant="outlined"
                  href={project.finalVideoUrl}
                  target="_blank"
                  rel="noreferrer"
                  data-testid="shortvideo-publish-open-final-video-button"
                >
                  查看成片
                </Button>
              )}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid item xs={12} md={7}>
          <Card
            variant="outlined"
            data-testid="shortvideo-publish-form"
            data-source-endpoints={`${PROJECT_SAVE_ENDPOINT}|${PUBLISH_TITLE_ENDPOINT}|${PUBLISH_REVIEW_ENDPOINT}|${PUBLISH_SUBMIT_ENDPOINT}`}
            data-input-retained="true"
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>发布信息</Typography>
              <Stack spacing={2}>
                <TextField
                  label="发布标题"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  fullWidth
                  multiline
                  minRows={2}
                />
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {PLATFORM_OPTIONS.map((platform) => (
                    <FormControlLabel
                      key={platform.value}
                      control={(
                        <Checkbox
                          checked={platforms.includes(platform.value)}
                          onChange={() => togglePlatform(platform.value)}
                          inputProps={{ 'aria-label': platform.label }}
                        />
                      )}
                      label={(
                        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                          <Typography component="span" variant="body2">{platform.label}</Typography>
                          <Chip
                            size="small"
                            label={platform.statusLabel}
                            color={isSupportedPublishPlatform(platform.value) ? 'success' : 'warning'}
                            variant={isSupportedPublishPlatform(platform.value) ? 'filled' : 'outlined'}
                          />
                        </Stack>
                      )}
                    />
                  ))}
                </Stack>
                <Box
                  data-testid="shortvideo-publish-platform-matrix"
                  data-contract-status="partial"
                  data-contract-endpoint={PUBLISH_SUBMIT_ENDPOINT}
                  data-supported-platforms={supportedPlatforms.join(',')}
                  data-unsupported-platforms={unsupportedPlatforms.join(',')}
                  sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, bgcolor: 'background.default', p: 1.5 }}
                >
                  <Stack spacing={1}>
                    <Typography variant="subtitle2">平台发布能力</Typography>
                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                      {PLATFORM_OPTIONS.map((platform) => (
                        <Chip
                          key={platform.value}
                          label={`${platform.label} · ${platform.description}`}
                          color={isSupportedPublishPlatform(platform.value) ? 'success' : 'warning'}
                          variant="outlined"
                          data-testid="shortvideo-publish-platform-contract"
                          data-platform={platform.value}
                          data-contract-status={platform.status}
                          data-contract-endpoint={platform.endpoint}
                        />
                      ))}
                    </Stack>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      data-testid="shortvideo-publish-payload-platforms"
                      data-payload-platforms={supportedPlatforms.join(',')}
                    >
                      实际提交平台：{supportedPlatforms.length > 0 ? supportedPlatforms.map(getPlatformLabel).join('、') : '无可提交平台'}
                    </Typography>
                  </Stack>
                </Box>
                {unsupportedPlatforms.length > 0 && (
                  <Alert
                    severity="warning"
                    data-testid="shortvideo-publish-platform-downgrade"
                    data-downgrade-tone="platform-gap"
                    data-contract-status="degraded"
                    data-contract-endpoint={PUBLISH_SUBMIT_ENDPOINT}
                    data-unsupported-platforms={unsupportedPlatforms.join(',')}
                  >
                    {unsupportedPlatforms.map(getPlatformLabel).join('、')} 当前未接入真实发布能力，发布时不会提交到真实发布接口；结果区会标记为已跳过。
                  </Alert>
                )}
                <TextField
                  label="计划发布时间"
                  type="datetime-local"
                  value={publishTime}
                  onChange={(e) => setPublishTime(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  fullWidth
                />
                {recommendTimes.length > 0 && (
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    {recommendTimes.slice(0, 5).map((item, index) => (
                      <Chip key={`${item}-${index}`} size="small" label={item} variant="outlined" />
                    ))}
                  </Stack>
                )}
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  <Button
                    startIcon={<SaveIcon />}
                    variant="outlined"
                    onClick={() => saveMutation.mutate()}
                    disabled={!project || saveMutation.isPending}
                    data-testid="shortvideo-publish-save-button"
                    data-source-endpoint={PROJECT_SAVE_ENDPOINT}
                  >
                    保存计划
                  </Button>
                  <Button
                    startIcon={<AutoAwesomeIcon />}
                    variant="outlined"
                    onClick={() => titleMutation.mutate()}
                    disabled={!project || titleMutation.isPending}
                    data-testid="shortvideo-publish-title-button"
                    data-source-endpoint={PUBLISH_TITLE_ENDPOINT}
                  >
                    AI 标题
                  </Button>
                  <Button
                    startIcon={<FactCheckIcon />}
                    variant="outlined"
                    onClick={() => reviewMutation.mutate()}
                    disabled={!project?.finalVideoUrl || !title || reviewMutation.isPending}
                    data-testid="shortvideo-publish-review-button"
                    data-source-endpoint={PUBLISH_REVIEW_ENDPOINT}
                  >
                    AI 审核
                  </Button>
                  <Button
                    startIcon={<PublishIcon />}
                    variant="contained"
                    onClick={() => publishMutation.mutate()}
                    disabled={!project?.finalVideoUrl || !title || supportedPlatforms.length === 0 || publishMutation.isPending}
                    data-testid="shortvideo-publish-submit-button"
                    data-source-endpoint={PUBLISH_SUBMIT_ENDPOINT}
                  >
                    发布
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={5}>
          <Stack spacing={2}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>审核结果</Typography>
                {reviewResult ? (
                  <Stack spacing={1}>
                    <Chip label={reviewResult.passed ? '审核通过' : '需要修改'} color={reviewResult.passed ? 'success' : 'warning'} sx={{ alignSelf: 'flex-start' }} />
                    {(reviewResult.issues.length > 0 ? reviewResult.issues : reviewResult.suggestions).map((item, index) => (
                      <Typography key={`${item}-${index}`} variant="body2" color="text.secondary">{item}</Typography>
                    ))}
                    <OfficialReferencesPanel
                      testId="publish-review-official-references"
                      endpoint={PUBLISH_REVIEW_ENDPOINT}
                      references={reviewResult.officialReferences}
                      required={reviewResult.officialReferenceRequired}
                      satisfied={reviewResult.officialReferenceSatisfied}
                      status={reviewResult.officialReferenceStatus}
                      maxItems={5}
                    />
                  </Stack>
                ) : (
                  <Typography variant="body2" color="text.secondary">尚未执行 AI 审核。</Typography>
                )}
              </CardContent>
            </Card>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>发布结果</Typography>
                {publishResult ? (
                  <Stack spacing={1}>
                    {publishResult.degraded && (
                      <Alert
                        severity="warning"
                        data-testid="shortvideo-publish-result-downgrade"
                        data-downgrade-tone="platform-gap"
                        data-contract-endpoint={PUBLISH_SUBMIT_ENDPOINT}
                      >
                        发布链路降级：至少一个平台未接入或发布失败，项目状态只会在成功平台返回后回写为 published。
                      </Alert>
                    )}
                    {publishResult.results.map((item) => (
                      <Alert
                        key={item.platform}
                        severity={item.success ? 'success' : 'warning'}
                        data-testid="shortvideo-publish-platform-result"
                        data-platform={item.platform}
                        data-platform-contract-status={getPlatformStatus(item.platform)}
                        data-publish-success={item.success ? 'true' : 'false'}
                      >
                        {getPlatformLabel(item.platform)}: {item.success ? `发布成功 ${item.itemId ?? ''}` : item.error ?? '未完成'}
                      </Alert>
                    ))}
                  </Stack>
                ) : (
                  <Typography variant="body2" color="text.secondary">发布后会显示各平台返回结果；未配置或未授权时会显示明确原因。</Typography>
                )}
              </CardContent>
            </Card>
          </Stack>
        </Grid>
      </Grid>
    </Box>
  )
}

export default function PublishPage() {
  const [searchParams] = useSearchParams()
  const parsedProjectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null
  const projectId = parsedProjectId != null && Number.isFinite(parsedProjectId) && parsedProjectId > 0 ? parsedProjectId : null
  return projectId ? <ProjectPublishView projectId={projectId} /> : <CalendarPublishView />
}
