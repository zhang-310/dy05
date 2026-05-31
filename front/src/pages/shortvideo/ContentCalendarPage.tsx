import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid,
  Chip, Button, IconButton, CircularProgress,
  Alert, TextField, MenuItem,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import AddIcon from '@mui/icons-material/Add'
import ScheduleIcon from '@mui/icons-material/Schedule'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi, adaptContentCalendarMonthView, type ContentCalendarMonthStats, type ContentCalendarSaveParams } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { FormDialog, PageHeader } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'

interface ContentCalendarItem { date: string; items?: Array<{ title: string; status: string }> }
interface ManualPlanForm {
  planDate: string
  title: string
  contentType: string
  publishTime: string
  brief: string
  projectId: string
  accountId: string
  priority: number
  tags: string
  status: number
}

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日']
const CONTENT_CALENDAR_VIEW_ENDPOINT = '/short-video/content/calendar'
const CONTENT_CALENDAR_STATS_ENDPOINT = '/short-video/content/calendar-stats'
const CONTENT_CALENDAR_SAVE_ENDPOINT = '/short-video/content-calendar/save'
const CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT = '/short-video/content-calendar/auto-generate'
const SEO_PUBLISH_TIME_ENDPOINT = '/short-video/seo/suggest-publish-time'
const CONTENT_CALENDAR_READY_ENDPOINTS = [
  CONTENT_CALENDAR_VIEW_ENDPOINT,
  CONTENT_CALENDAR_STATS_ENDPOINT,
  CONTENT_CALENDAR_SAVE_ENDPOINT,
  CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT,
  SEO_PUBLISH_TIME_ENDPOINT,
].join('|')
const CONTENT_CALENDAR_READY_ROUTES = [
  shortvideoRoutes.contentCalendar,
  `${shortvideoRoutes.contentCalendar}?year=:year&month=:month`,
  shortvideoRoutes.daily,
].join('|')
const CONTENT_CALENDAR_SUPPORTED_ACTIONS = [
  'refresh-content-calendar',
  'navigate-calendar-month',
  'auto-generate-calendar',
  'open-manual-plan-dialog',
  'add-plan-from-calendar-cell',
].join('|')
const CONTENT_CALENDAR_UNSUPPORTED_ENDPOINTS = [
  '/short-video/content-calendar/mock',
  '/short-video/content-calendar/local-view',
  '/short-video/content-calendar/local-save',
  '/short-video/content-calendar/local-auto-generate',
  '/short-video/content-calendar/static-stats',
  '/short-video/seo/local-publish-time',
  '/short-video/project/local-calendar',
].join('|')

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate()
}
function getFirstDayOfWeek(year: number, month: number) {
  const d = new Date(year, month - 1, 1).getDay()
  return d === 0 ? 6 : d - 1 // Monday=0
}

function formatCompletionRate(rate?: number) {
  if (rate == null || !Number.isFinite(rate)) return '--'
  const percent = rate > 0 && rate <= 1 ? rate * 100 : rate
  return `${Math.round(percent * 10) / 10}%`
}

function createManualPlanForm(planDate: string): ManualPlanForm {
  return {
    planDate,
    title: '',
    contentType: 'video',
    publishTime: '',
    brief: '',
    projectId: '',
    accountId: '',
    priority: 2,
    tags: '',
    status: 0,
  }
}

function toOptionalNumber(value: string): number | undefined {
  const trimmed = value.trim()
  if (!trimmed) return undefined
  const parsed = Number(trimmed)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

function normalizeCalendarItems(raw: unknown): ContentCalendarItem[] {
  if (Array.isArray(raw)) return raw as ContentCalendarItem[]
  if (raw && typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    return normalizeCalendarItems(obj.list ?? obj.records ?? obj.items ?? obj.rows ?? obj.content ?? obj.data)
  }
  return []
}

function normalizeStringRows(raw: unknown): string[] {
  if (Array.isArray(raw)) return raw.map((item) => String(item ?? '').trim()).filter(Boolean)
  if (raw && typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    return normalizeStringRows(obj.list ?? obj.records ?? obj.items ?? obj.rows ?? obj.content ?? obj.data)
  }
  return []
}

export default function ContentCalendarPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [personaId, setPersonaId] = useState('')
  const [manualOpen, setManualOpen] = useState(false)
  const [manualForm, setManualForm] = useState<ManualPlanForm>(() => createManualPlanForm(now.toISOString().slice(0, 10)))

  const { data: calendarData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['content-calendar', year, month],
    queryFn: async () => {
      const raw = await shortvideoApi.contentCalendarView(year, month)
      return adaptContentCalendarMonthView(raw)
    },
  })

  const { data: stats, isError: statsIsError, error: statsError, refetch: refetchStats } = useQuery({
    queryKey: ['content-calendar-stats', year, month],
    queryFn: () => shortvideoApi.contentCalendarMonthStats(year, month),
  })

  const { data: rawRecommendTimes = [], isLoading: recLoading, isError: recIsError, error: recError, refetch: refetchRec } = useQuery({
    queryKey: ['recommend-publish-time-seo', year, month],
    queryFn: () => shortvideoApi.seoSuggestPublishTime({}),
  })

  const parsedPersonaId = personaId.trim() ? Number(personaId.trim()) : undefined
  const activePersonaId = parsedPersonaId != null && Number.isFinite(parsedPersonaId) && parsedPersonaId > 0 ? parsedPersonaId : undefined

  const calArr = normalizeCalendarItems(calendarData)
  const recommendTimes = normalizeStringRows(rawRecommendTimes)
  const calMap: Record<string, typeof calArr[0]> = {}
  calArr.forEach(d => { calMap[d.date] = d })

  const statsData: ContentCalendarMonthStats = stats ?? {}
  const daysInMonth = getDaysInMonth(year, month)
  const firstDay = getFirstDayOfWeek(year, month)
  const cells: (number | null)[] = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ]

  const prevMonth = () => {
    if (month === 1) { setMonth(12); setYear(y => y - 1) }
    else setMonth(m => m - 1)
  }
  const nextMonth = () => {
    if (month === 12) { setMonth(1); setYear(y => y + 1) }
    else setMonth(m => m + 1)
  }

  const autoGenerateMut = useMutation({
    mutationFn: async () => {
      if (!activePersonaId) throw new Error('personaId 必填')
      const from = `${year}-${String(month).padStart(2, '0')}-01`
      const to = `${year}-${String(month).padStart(2, '0')}-${String(daysInMonth).padStart(2, '0')}`
      return shortvideoApi.contentCalendarAutoGenerate({ personaId: activePersonaId, from, to })
    },
    onSuccess: (n) => {
      toast(`已自动排期 ${n} 条`, 'success')
      qc.invalidateQueries({ queryKey: ['content-calendar'] })
      qc.invalidateQueries({ queryKey: ['content-calendar-stats'] })
    },
    onError: (e) => toast(`自动排期失败：${getErrorMessage(e)}`, 'error'),
  })

  const manualSaveMut = useMutation({
    mutationFn: async (): Promise<number> => {
      if (!activePersonaId) throw new Error('personaId 必填')
      const title = manualForm.title.trim()
      if (!title) throw new Error('计划标题必填')
      if (!manualForm.planDate) throw new Error('计划日期必填')
      const body: ContentCalendarSaveParams = {
        personaId: activePersonaId,
        planDate: manualForm.planDate,
        contentType: manualForm.contentType,
        title,
        brief: manualForm.brief.trim() || undefined,
        publishTime: manualForm.publishTime || undefined,
        projectId: toOptionalNumber(manualForm.projectId),
        accountId: toOptionalNumber(manualForm.accountId),
        priority: manualForm.priority,
        tags: manualForm.tags.trim() || undefined,
        status: manualForm.status,
      }
      return shortvideoApi.contentCalendarSave(body)
    },
    onSuccess: () => {
      toast('计划已保存', 'success')
      setManualOpen(false)
      qc.invalidateQueries({ queryKey: ['content-calendar'] })
      qc.invalidateQueries({ queryKey: ['content-calendar-stats'] })
    },
    onError: (e) => toast(`计划保存失败：${getErrorMessage(e)}`, 'error'),
  })

  const openManualPlan = (planDate?: string) => {
    setManualForm(createManualPlanForm(planDate ?? `${year}-${String(month).padStart(2, '0')}-${String(Math.min(now.getDate(), daysInMonth)).padStart(2, '0')}`))
    setManualOpen(true)
  }

  const padded = [...cells]
  while (padded.length % 7 !== 0) padded.push(null)

  return (
    <Box
      data-testid="content-calendar-page"
      data-ready-endpoints={CONTENT_CALENDAR_READY_ENDPOINTS}
      data-ready-routes={CONTENT_CALENDAR_READY_ROUTES}
      data-supported-actions={CONTENT_CALENDAR_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={CONTENT_CALENDAR_UNSUPPORTED_ENDPOINTS}
      data-no-local-calendar-fallback="true"
      data-no-local-schedule-mutation="true"
      data-input-retained-on-error="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="内容日历"
        breadcrumbs={[{ label: '短视频' }, { label: '内容日历' }]}
        subtitle={`月视图来自 POST ${CONTENT_CALENDAR_VIEW_ENDPOINT}，自动排期写入 sv_content_calendar。`}
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              size="small"
              startIcon={<RefreshIcon />}
              onClick={() => { refetch(); refetchStats(); refetchRec() }}
              data-testid="content-calendar-refresh-button"
              data-source-endpoint={CONTENT_CALENDAR_VIEW_ENDPOINT}
            >
              刷新
            </Button>
            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              disabled={!activePersonaId}
              onClick={() => openManualPlan()}
              data-testid="content-calendar-open-manual-button"
            >
              新建计划
            </Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="content-calendar-boundary-contract"
        data-no-local-calendar-fallback="true"
        data-no-local-schedule-mutation="true"
        data-supported-actions={CONTENT_CALENDAR_SUPPORTED_ACTIONS}
      >
        手动创建计划写入 POST {CONTENT_CALENDAR_SAVE_ENDPOINT}；自动排期调用 POST {CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT} 批量补齐缺失日期。
      </Alert>

      <Stack
        direction="row"
        spacing={1}
        alignItems="center"
        flexWrap="wrap"
        data-testid="content-calendar-filter-contract"
        data-server-period-payload="year|month"
        data-server-persona-payload="personaId"
      >
        <TextField
          size="small"
          label="达人 Persona ID"
          value={personaId}
          onChange={e => setPersonaId(e.target.value)}
          sx={{ width: 180 }}
        />
        <Button
          variant="outlined"
          startIcon={<AutoAwesomeIcon />}
          disabled={!activePersonaId || autoGenerateMut.isPending}
          onClick={() => autoGenerateMut.mutate()}
          data-testid="content-calendar-auto-generate-button"
          data-source-endpoint={CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT}
        >
          自动生成本月排期
        </Button>
      </Stack>

      {isError && (
        <Alert
          severity="error"
          data-testid="content-calendar-view-error"
          data-no-local-calendar-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          内容日历加载失败（POST {CONTENT_CALENDAR_VIEW_ENDPOINT}）：{getErrorMessage(error)}。页面不会用本地日历补齐。
        </Alert>
      )}
      {statsIsError && (
        <Alert
          severity="warning"
          data-testid="content-calendar-stats-error"
          data-no-static-stats-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetchStats()}>重试</Button>}
        >
          月统计不可用（POST {CONTENT_CALENDAR_STATS_ENDPOINT}）：{getErrorMessage(statsError)}。统计卡保持空值，不影响月视图。
        </Alert>
      )}
      {autoGenerateMut.isError && (
        <Alert severity="error" data-testid="content-calendar-auto-error" data-no-local-schedule-mutation="true" data-input-retained="true">
          自动排期失败（POST {CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT}）：{getErrorMessage(autoGenerateMut.error)}。当前 personaId 和月份会保留，不伪造排期条目。
        </Alert>
      )}
      {manualSaveMut.isError && (
        <Alert severity="error" data-testid="content-calendar-save-error" data-no-local-schedule-mutation="true" data-input-retained="true">
          计划保存失败（POST {CONTENT_CALENDAR_SAVE_ENDPOINT}）：{getErrorMessage(manualSaveMut.error)}。弹窗输入会保留，月历不插入本地假计划。
        </Alert>
      )}

      {/* 统计卡片 */}
      <Grid container spacing={2} data-testid="content-calendar-stats-contract" data-source-endpoint={CONTENT_CALENDAR_STATS_ENDPOINT} data-no-static-stats-fallback="true">
        {[
          { label: '本月计划数', value: String(statsData.plannedCount ?? 0) },
          { label: '已发布', value: String(statsData.publishedCount ?? 0) },
          { label: '完成率', value: formatCompletionRate(statsData.completionRate) },
        ].map(kpi => (
          <Grid item xs={6} sm={3} key={kpi.label}>
            <Card variant="outlined"><CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{kpi.value}</Typography>
            </CardContent></Card>
          </Grid>
        ))}
      </Grid>

      {/* 月历导航 */}
      <Card variant="outlined" data-testid="content-calendar-month-grid-contract" data-source-endpoint={CONTENT_CALENDAR_VIEW_ENDPOINT} data-no-local-calendar-fallback="true">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
            <IconButton onClick={prevMonth} data-testid="content-calendar-prev-month-button">
              <ChevronLeftIcon />
            </IconButton>
            <Typography variant="subtitle1" fontWeight={600}>{year} 年 {month} 月</Typography>
            <IconButton onClick={nextMonth} data-testid="content-calendar-next-month-button">
              <ChevronRightIcon />
            </IconButton>
          </Stack>

          {(isLoading || autoGenerateMut.isPending) && <CircularProgress sx={{ mx: 'auto', display: 'block' }} />}

          {/* 星期头 */}
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 0.5, mb: 0.5 }}>
            {WEEKDAYS.map(w => (
              <Box key={w} sx={{ textAlign: 'center', py: 0.5 }}>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>{w}</Typography>
              </Box>
            ))}
          </Box>

          {/* 日历格 */}
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 0.5 }}>
            {padded.map((day, i) => {
              if (!day) return <Box key={`empty-${i}`} />
              const dateKey = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
              const dayData = calMap[dateKey]
              const items = dayData?.items ?? []
              const isToday = day === now.getDate() && month === now.getMonth() + 1 && year === now.getFullYear()
              return (
                <Box
                  key={day}
                  data-testid={isToday ? 'content-calendar-today-cell' : undefined}
                  sx={(theme) => ({
                    border: '1px solid',
                    borderColor: isToday ? 'primary.main' : 'divider',
                    borderRadius: 1,
                    minHeight: 80,
                    p: 0.5,
                    bgcolor: isToday
                      ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
                      : 'transparent',
                  })}
                >
                  <Typography variant="caption" fontWeight={isToday ? 700 : 400}
                    color={isToday ? 'primary.main' : 'text.primary'}>{day}</Typography>
                  <Stack spacing={0.25} mt={0.25}>
                    {items.slice(0, 2).map((item, j) => (
                      <Chip key={j} label={item.title} size="small"
                        color={item.status === 'published' ? 'success' : 'default'}
                        sx={{ fontSize: 10, height: 18 }} />
                    ))}
                    {items.length > 2 && (
                      <Typography variant="caption" color="text.secondary">+{items.length - 2} 更多</Typography>
                    )}
                    {items.length === 0 && activePersonaId ? (
                      <Button
                        size="small"
                        variant="text"
                        onClick={() => openManualPlan(dateKey)}
                        sx={{ alignSelf: 'flex-start', minWidth: 0, px: 0.5 }}
                        data-testid="content-calendar-cell-add-button"
                      >
                        添加
                      </Button>
                    ) : null}
                  </Stack>
                </Box>
              )
            })}
          </Box>
        </CardContent>
      </Card>

      {!recLoading && recommendTimes.length > 0 && (
        <Card variant="outlined" data-testid="content-calendar-publish-time-contract" data-source-endpoint={SEO_PUBLISH_TIME_ENDPOINT} data-no-local-publish-time-fallback="true">
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" mb={1}>
              <ScheduleIcon fontSize="small" color="primary" />
              <Typography variant="subtitle2" fontWeight={600}>AI 推荐发布时间</Typography>
            </Stack>
            <Stack direction="row" flexWrap="wrap" gap={1}>
              {recommendTimes.map((t, i) => (
                <Chip key={i} label={String(t)} size="small" color="primary" variant="outlined" />
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      {recIsError && (
        <Alert severity="warning" data-testid="content-calendar-publish-time-error" data-no-local-publish-time-fallback="true">
          发布时间推荐不可用（POST {SEO_PUBLISH_TIME_ENDPOINT}）：{getErrorMessage(recError)}。不影响内容日历和自动排期。
        </Alert>
      )}

      <FormDialog
        open={manualOpen}
        title="新建内容计划"
        onClose={() => setManualOpen(false)}
        onConfirm={() => manualSaveMut.mutate()}
        loading={manualSaveMut.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Alert severity="info" variant="outlined">
            当前 Persona ID：{activePersonaId ?? '未填写'}。保存后会刷新月视图和月统计。
          </Alert>
          {manualSaveMut.isError && (
            <Alert severity="error" data-testid="content-calendar-dialog-save-error" data-no-local-schedule-mutation="true" data-input-retained="true">
              计划保存失败（POST {CONTENT_CALENDAR_SAVE_ENDPOINT}）：{getErrorMessage(manualSaveMut.error)}。请修正后重试，当前标题、日期、项目和标签不会被清空。
            </Alert>
          )}
          <TextField
            label="计划日期"
            type="date"
            size="small"
            value={manualForm.planDate}
            onChange={e => setManualForm(f => ({ ...f, planDate: e.target.value }))}
            InputLabelProps={{ shrink: true }}
            fullWidth
            required
          />
          <TextField
            label="计划标题"
            size="small"
            value={manualForm.title}
            onChange={e => setManualForm(f => ({ ...f, title: e.target.value }))}
            fullWidth
            required
          />
          <TextField
            select
            label="内容类型"
            size="small"
            value={manualForm.contentType}
            onChange={e => setManualForm(f => ({ ...f, contentType: e.target.value }))}
            fullWidth
            required
          >
            <MenuItem value="video">短视频</MenuItem>
            <MenuItem value="live_clip">直播切片</MenuItem>
            <MenuItem value="remake">爆款复刻</MenuItem>
            <MenuItem value="daily">日更</MenuItem>
          </TextField>
          <TextField
            label="发布时间"
            type="datetime-local"
            size="small"
            value={manualForm.publishTime}
            onChange={e => setManualForm(f => ({ ...f, publishTime: e.target.value }))}
            InputLabelProps={{ shrink: true }}
            fullWidth
          />
          <Grid container spacing={1.5}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="项目 ID（可选）"
                size="small"
                value={manualForm.projectId}
                onChange={e => setManualForm(f => ({ ...f, projectId: e.target.value }))}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="账号 ID（可选）"
                size="small"
                value={manualForm.accountId}
                onChange={e => setManualForm(f => ({ ...f, accountId: e.target.value }))}
                fullWidth
              />
            </Grid>
          </Grid>
          <Grid container spacing={1.5}>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="优先级"
                size="small"
                value={manualForm.priority}
                onChange={e => setManualForm(f => ({ ...f, priority: Number(e.target.value) }))}
                fullWidth
              >
                <MenuItem value={1}>高</MenuItem>
                <MenuItem value={2}>中</MenuItem>
                <MenuItem value={3}>低</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="状态"
                size="small"
                value={manualForm.status}
                onChange={e => setManualForm(f => ({ ...f, status: Number(e.target.value) }))}
                fullWidth
              >
                <MenuItem value={0}>待发布</MenuItem>
                <MenuItem value={1}>制作中</MenuItem>
                <MenuItem value={2}>已发布</MenuItem>
              </TextField>
            </Grid>
          </Grid>
          <TextField
            label="标签"
            size="small"
            value={manualForm.tags}
            onChange={e => setManualForm(f => ({ ...f, tags: e.target.value }))}
            helperText="逗号分隔，例如：屏障修护,测评"
            fullWidth
          />
          <TextField
            label="计划说明"
            size="small"
            value={manualForm.brief}
            onChange={e => setManualForm(f => ({ ...f, brief: e.target.value }))}
            fullWidth
            multiline
            minRows={3}
          />
        </Stack>
      </FormDialog>
    </Box>
  )
}
