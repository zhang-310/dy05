import { useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Grid, LinearProgress, MenuItem, Stack, TextField, Tooltip, Typography } from '@mui/material'
import { alpha } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi, type ContentCalendarSaveParams } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { FormDialog, PageHeader } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'

function getWeekDates() {
  const today = new Date()
  const day = today.getDay()
  const monday = new Date(today)
  monday.setDate(today.getDate() - (day === 0 ? 6 : day - 1))
  const dates: Date[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    dates.push(d)
  }
  return dates
}

const DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const DAILY_GENERATE_ENDPOINT = '/short-video/project/generate-daily'
const CONTENT_CALENDAR_DATE_RANGE_ENDPOINT = '/short-video/content-calendar/date-range'
const CONTENT_CALENDAR_SAVE_ENDPOINT = '/short-video/content-calendar/save'
const CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT = '/short-video/content-calendar/auto-generate'
const DASHBOARD_STATS_ENDPOINT = '/short-video/dashboard/stats'
const DAILY_CONTENT_READY_ENDPOINTS = [
  DAILY_GENERATE_ENDPOINT,
  CONTENT_CALENDAR_DATE_RANGE_ENDPOINT,
  CONTENT_CALENDAR_SAVE_ENDPOINT,
  CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT,
  DASHBOARD_STATS_ENDPOINT,
].join('|')
const DAILY_CONTENT_READY_ROUTES = [
  shortvideoRoutes.daily,
  `${shortvideoRoutes.daily}?personaId=:id`,
  shortvideoRoutes.contentCalendar,
].join('|')
const DAILY_CONTENT_SUPPORTED_ACTIONS = [
  'refresh-daily-content',
  'generate-daily-content',
  'auto-schedule-week',
  'open-manual-schedule-dialog',
  'add-schedule-from-week-cell',
].join('|')
const DAILY_CONTENT_UNSUPPORTED_ENDPOINTS = [
  '/short-video/daily/mock',
  '/short-video/daily/local-generate',
  '/short-video/daily/local-calendar',
  '/short-video/daily/local-save',
  '/short-video/project/local-generate-daily',
  '/short-video/content-calendar/local-date-range',
  '/short-video/content-calendar/local-save',
  '/short-video/dashboard/static-stats',
].join('|')

interface CalendarItem {
  id?: number
  title?: string
  brief?: string
  videoTitle?: string
  planDate?: string
  publishDate?: string
  date?: string
  scheduledDate?: string
  status?: number
  publishTime?: string
  contentType?: string
  priority?: number
  projectId?: number
  accountId?: number
  tags?: string
  [key: string]: unknown
}

interface DashboardStats {
  [key: string]: number | string | undefined
}

interface ScheduleSummary {
  total: number
  pending: number
  published: number
  linkedProject: number
}

interface SummaryCard {
  label: string
  value: number | string
  hint: string
}

interface ManualScheduleForm {
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

function createManualScheduleForm(planDate: string): ManualScheduleForm {
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

export default function DailyContentPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [personaId, setPersonaId] = useState('')
  const [topic, setTopic] = useState('')
  const [style, setStyle] = useState('')
  const [duration, setDuration] = useState('30秒')
  const [count, setCount] = useState(1)
  const [days, setDays] = useState(7)
  const [manualOpen, setManualOpen] = useState(false)
  const [manualForm, setManualForm] = useState<ManualScheduleForm>(() => createManualScheduleForm(new Date().toISOString().slice(0, 10)))
  const [operationError, setOperationError] = useState('')

  const weekDates = getWeekDates()
  const startDate = weekDates[0].toISOString().slice(0, 10)
  const endDate = weekDates[6].toISOString().slice(0, 10)

  const parsedPersonaId = personaId.trim() ? Number(personaId.trim()) : undefined
  const activePersonaId = parsedPersonaId != null && Number.isFinite(parsedPersonaId) && parsedPersonaId > 0 ? parsedPersonaId : undefined

  const { data: calendarData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['sv-daily-calendar', activePersonaId, startDate],
    queryFn: () => shortvideoApi.calendar({
      startDate,
      endDate,
      personaId: activePersonaId,
    }),
  })
  const calendar = Array.isArray(calendarData) ? calendarData as CalendarItem[] : []

  const { data: statsData, isError: statsIsError, error: statsError, refetch: refetchStats } = useQuery({
    queryKey: ['sv-daily-stats', activePersonaId, days],
    queryFn: () => shortvideoApi.dashboardStats(),
  })
  const stats = statsData as DashboardStats | undefined

  const generateMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      if (!activePersonaId) throw new Error('personaId 必填')
      await shortvideoApi.generateDaily({
        personaId: activePersonaId,
        scheduleDate: new Date().toISOString().slice(0, 10),
        count,
        style: style.trim() || undefined,
        duration: duration.trim() || undefined,
        topic: topic.trim() || undefined,
      })
    },
    onSuccess: () => {
      toast('日排计划已生成', 'success')
      setOperationError('')
      qc.invalidateQueries({ queryKey: ['sv-daily-calendar'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setOperationError(`POST ${DAILY_GENERATE_ENDPOINT} 生成失败：${message}。当前 personaId、主题、风格、时长和条数会保留，不生成本地占位日排。`)
      toast(`生成失败：${message}`, 'error')
    },
  })

  const autoCalendarMutation = useMutation({
    mutationFn: async (): Promise<number> => {
      if (!activePersonaId) throw new Error('personaId 必填')
      return shortvideoApi.contentCalendarAutoGenerate({
        personaId: activePersonaId,
        from: startDate,
        to: endDate,
      })
    },
    onSuccess: (n) => {
      toast(`已自动排期 ${n} 条`, 'success')
      setOperationError('')
      qc.invalidateQueries({ queryKey: ['sv-daily-calendar'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setOperationError(`POST ${CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT} 自动排期失败：${message}。本周排期列表保持后端返回状态，不追加本地假记录。`)
      toast(`自动排期失败：${message}`, 'error')
    },
  })

  const manualScheduleMutation = useMutation({
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
      toast('手动排期已保存', 'success')
      setOperationError('')
      setManualOpen(false)
      qc.invalidateQueries({ queryKey: ['sv-daily-calendar'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setOperationError(`POST ${CONTENT_CALENDAR_SAVE_ENDPOINT} 手动排期保存失败：${message}。弹窗继续保留标题、日期、项目和标签。`)
      toast(`手动排期失败：${message}`, 'error')
    },
  })

  const calendarMap: Record<string, Record<string, unknown>[]> = {}
  for (const item of calendar) {
    const dateStr = String(item.publishDate ?? item.date ?? item.scheduledDate ?? '').slice(0, 10)
    if (dateStr) {
      if (!calendarMap[dateStr]) calendarMap[dateStr] = []
      calendarMap[dateStr].push(item)
    }
  }

  const scheduleSummary = calendar.reduce<ScheduleSummary>(
    (acc, item) => {
      acc.total += 1
      if (Number(item.status) === 2) acc.published += 1
      else acc.pending += 1
      if (item.projectId != null) acc.linkedProject += 1
      return acc
    },
    { total: 0, pending: 0, published: 0, linkedProject: 0 },
  )

  const dashboardSummaryCards: SummaryCard[] = [
    { label: '本周排期', value: scheduleSummary.total, hint: 'date-range 返回条数' },
    { label: '待发布', value: scheduleSummary.pending, hint: 'status 非 2' },
    { label: '已发布', value: scheduleSummary.published, hint: 'status = 2' },
    { label: '绑定项目', value: scheduleSummary.linkedProject, hint: 'projectId 已写入' },
  ]

  const openManualSchedule = (date?: string) => {
    setManualForm(createManualScheduleForm(date ?? new Date().toISOString().slice(0, 10)))
    setManualOpen(true)
  }

  return (
    <Box
      data-testid="daily-content-page"
      data-ready-endpoints={DAILY_CONTENT_READY_ENDPOINTS}
      data-ready-routes={DAILY_CONTENT_READY_ROUTES}
      data-supported-actions={DAILY_CONTENT_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={DAILY_CONTENT_UNSUPPORTED_ENDPOINTS}
      data-no-local-daily-fallback="true"
      data-no-local-calendar-fallback="true"
      data-no-local-schedule-mutation="true"
      data-input-retained-on-error="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="日更内容排期"
        breadcrumbs={[{ label: '短视频' }, { label: '日更' }]}
        subtitle={`按 personaId 与日期生成每日拍摄脚本，并通过 POST ${CONTENT_CALENDAR_DATE_RANGE_ENDPOINT} 查看本周排期。`}
        actions={(
          <Button
            size="small"
            onClick={() => { refetch(); refetchStats() }}
            data-testid="daily-content-refresh-button"
            data-source-endpoint={CONTENT_CALENDAR_DATE_RANGE_ENDPOINT}
          >
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="daily-content-boundary-contract"
        data-no-local-daily-fallback="true"
        data-no-local-schedule-mutation="true"
        data-supported-actions={DAILY_CONTENT_SUPPORTED_ACTIONS}
      >
        “AI 生成日排”调用 POST {DAILY_GENERATE_ENDPOINT}；“自动排期”调用 POST {CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT}；手动排期直接写入 POST {CONTENT_CALENDAR_SAVE_ENDPOINT}。
      </Alert>

      <Stack
        direction="row"
        spacing={2}
        alignItems="center"
        flexWrap="wrap"
        data-testid="daily-content-filter-contract"
        data-server-persona-payload="personaId"
        data-server-date-range-payload="startDate|endDate"
        data-input-retained-on-error="true"
      >
        <TextField size="small" label="达人 Persona ID" value={personaId}
          onChange={e => setPersonaId(e.target.value)} sx={{ width: 180 }} />
        <TextField size="small" label="主题（可选）" value={topic}
          onChange={e => setTopic(e.target.value)} sx={{ width: 200 }} />
        <TextField size="small" label="风格（可选）" value={style}
          onChange={e => setStyle(e.target.value)} sx={{ width: 160 }} />
        <TextField select size="small" label="时长" value={duration}
          onChange={e => setDuration(e.target.value)} sx={{ width: 120 }}>
          {['15秒', '30秒', '60秒', '90秒'].map(d => <MenuItem key={d} value={d}>{d}</MenuItem>)}
        </TextField>
        <TextField select size="small" label="条数" value={count}
          onChange={e => setCount(Number(e.target.value))} sx={{ width: 100 }}>
          {[1, 2, 3, 5].map(n => <MenuItem key={n} value={n}>{n}</MenuItem>)}
        </TextField>
        <TextField select size="small" label="统计天数" value={days}
          onChange={e => setDays(Number(e.target.value))} sx={{ width: 120 }}>
          {[7, 14, 30].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
        </TextField>
        <Box sx={{ flex: 1 }} />
        <Button variant="outlined" startIcon={<AutoAwesomeIcon />}
          onClick={() => generateMutation.mutate()} disabled={!activePersonaId || generateMutation.isPending}
          data-testid="daily-content-generate-button"
          data-source-endpoint={DAILY_GENERATE_ENDPOINT}>
          AI 生成日排
        </Button>
        <Button variant="outlined" startIcon={<AutoAwesomeIcon />}
          onClick={() => autoCalendarMutation.mutate()} disabled={!activePersonaId || autoCalendarMutation.isPending}
          data-testid="daily-content-auto-schedule-button"
          data-source-endpoint={CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT}>
          自动排期
        </Button>
        <Tooltip title={activePersonaId ? '写入 sv_content_calendar 手动计划' : '请先填写达人 Persona ID'}>
          <span>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              disabled={!activePersonaId}
              onClick={() => openManualSchedule()}
              data-testid="daily-content-open-manual-button"
            >
              手动排期
            </Button>
          </span>
        </Tooltip>
      </Stack>

      {(isLoading || generateMutation.isPending || autoCalendarMutation.isPending) && <LinearProgress />}
      {!activePersonaId && <Alert severity="warning" data-testid="daily-content-persona-required" data-no-local-daily-fallback="true">请先填写达人 Persona ID；日排生成与内容日历过滤都按 personaId 工作，不再按项目 ID 生成。</Alert>}
      {isError && (
        <Alert
          severity="error"
          data-testid="daily-content-calendar-error"
          data-no-local-calendar-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          本周排期加载失败（POST {CONTENT_CALENDAR_DATE_RANGE_ENDPOINT}）：{getErrorMessage(error)}。页面不会用静态排期补齐。
        </Alert>
      )}
      {statsIsError && (
        <Alert severity="warning" data-testid="daily-content-stats-error" data-no-static-stats-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchStats()}>重试</Button>}>
          统计数据不可用（POST {DASHBOARD_STATS_ENDPOINT}）：{getErrorMessage(statsError)}。不影响排期生成。
        </Alert>
      )}
      {generateMutation.isError && <Alert severity="error" data-testid="daily-content-generate-error" data-no-local-daily-fallback="true" data-input-retained="true">AI 生成日排失败（POST {DAILY_GENERATE_ENDPOINT}）：{getErrorMessage(generateMutation.error)}</Alert>}
      {autoCalendarMutation.isError && <Alert severity="error" data-testid="daily-content-auto-error" data-no-local-schedule-mutation="true" data-input-retained="true">自动排期失败（POST {CONTENT_CALENDAR_AUTO_GENERATE_ENDPOINT}）：{getErrorMessage(autoCalendarMutation.error)}</Alert>}
      {manualScheduleMutation.isError && <Alert severity="error" data-testid="daily-content-save-error" data-no-local-schedule-mutation="true" data-input-retained="true">手动排期保存失败（POST {CONTENT_CALENDAR_SAVE_ENDPOINT}）：{getErrorMessage(manualScheduleMutation.error)}</Alert>}
      {operationError && <Alert severity="error" data-testid="daily-content-operation-error" data-no-local-schedule-mutation="true" data-input-retained="true" onClose={() => setOperationError('')}>{operationError}</Alert>}

      <Grid container spacing={2} data-testid="daily-content-summary-contract" data-source-endpoint={CONTENT_CALENDAR_DATE_RANGE_ENDPOINT} data-no-local-calendar-fallback="true">
        {dashboardSummaryCards.map((item) => (
          <Grid item xs={6} sm={3} key={item.label}>
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

      {/* 统计卡片 */}
      {stats && (
        <Grid container spacing={2} data-testid="daily-content-stats-contract" data-source-endpoint={DASHBOARD_STATS_ENDPOINT} data-no-static-stats-fallback="true">
          {Object.entries(stats)
            .filter(([, v]) => typeof v === 'number')
            .slice(0, 4)
            .map(([k, v]) => (
              <Grid item xs={6} sm={3} key={k}>
                <Card variant="outlined">
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Typography variant="caption" color="text.secondary">{k}</Typography>
                    <Typography variant="h6" fontWeight={700}>{String(v)}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
        </Grid>
      )}

      {/* 本周日历 */}
      <Typography variant="subtitle2">本周排期（{startDate} 至 {endDate}）</Typography>
      {!isLoading && calendar.length === 0 && (
        <Alert severity="info" data-testid="daily-content-empty" data-no-local-calendar-fallback="true">
          本周暂无排期。可点击“自动排期”批量补齐，也可以用“手动排期”写入单日计划；后端会按当前登录用户和 personaId 做数据归属。
        </Alert>
      )}
      <Grid container spacing={1.5} data-testid="daily-content-week-grid-contract" data-source-endpoint={CONTENT_CALENDAR_DATE_RANGE_ENDPOINT} data-no-local-calendar-fallback="true">
        {weekDates.map((date, idx) => {
          const dateStr = date.toISOString().slice(0, 10)
          const items = calendarMap[dateStr] ?? []
          const isToday = dateStr === new Date().toISOString().slice(0, 10)
          return (
            <Grid item xs={12} sm={6} md={true} key={dateStr}>
              <Card variant="outlined" sx={{
                borderColor: isToday ? 'primary.main' : 'divider',
                borderWidth: isToday ? 2 : 1,
                minHeight: 120,
              }}>
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2" color={isToday ? 'primary.main' : 'text.primary'}>
                      {DAY_LABELS[idx]}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {date.getMonth() + 1}/{date.getDate()}
                    </Typography>
                  </Stack>
                  {items.length === 0 ? (
                    <Stack spacing={1}>
                      <Typography variant="caption" color="text.secondary">无排期</Typography>
                      {activePersonaId && (
                        <Button
                          size="small"
                          variant="text"
                          startIcon={<AddIcon />}
                          onClick={() => openManualSchedule(dateStr)}
                          data-testid="daily-content-cell-add-button"
                        >
                          添加
                        </Button>
                      )}
                    </Stack>
                  ) : (
                    <Stack spacing={0.5}>
                      {items.map((item, i) => (
                        <Box
                          key={i}
                          data-testid="daily-content-schedule-item"
                          sx={(theme) => ({
                            p: 0.75,
                            bgcolor: theme.palette.mode === 'dark'
                              ? theme.palette.background.default
                              : alpha(theme.palette.common.black, 0.025),
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 0.5,
                          })}
                        >
                          <Typography variant="caption" fontWeight={500} display="block" noWrap>
                            {String(item.title ?? item.videoTitle ?? `内容 ${i + 1}`)}
                          </Typography>
                          <Stack direction="row" spacing={0.5} mt={0.25}>
                            <Chip
                              label={Number(item.status) === 2 ? '已发' : '待发'}
                              size="small"
                              color={Number(item.status) === 2 ? 'success' : 'default'}
                              sx={{ height: 16, fontSize: 10 }}
                            />
                            {item.contentType ? (
                              <Chip label={String(item.contentType)} size="small" variant="outlined" sx={{ height: 16, fontSize: 10 }} />
                            ) : null}
                            {item.publishTime ? (
                              <Typography variant="caption" color="text.secondary">
                                {String(item.publishTime).slice(11, 16)}
                              </Typography>
                            ) : null}
                          </Stack>
                        </Box>
                      ))}
                    </Stack>
                  )}
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>

      <FormDialog
        open={manualOpen}
        title="手动排期"
        onClose={() => setManualOpen(false)}
        onConfirm={() => manualScheduleMutation.mutate()}
        loading={manualScheduleMutation.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          {manualScheduleMutation.isError && (
            <Alert severity="error" data-testid="daily-content-dialog-save-error" data-no-local-schedule-mutation="true" data-input-retained="true">
              手动排期保存失败（POST {CONTENT_CALENDAR_SAVE_ENDPOINT}）：{getErrorMessage(manualScheduleMutation.error)}。当前表单不会关闭或清空。
            </Alert>
          )}
          <Alert severity="info" variant="outlined">
            手动排期会保存到 sv_content_calendar，必填字段为 personaId、planDate 和 contentType。
          </Alert>
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
