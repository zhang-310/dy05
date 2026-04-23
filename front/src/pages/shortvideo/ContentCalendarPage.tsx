import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid,
  Chip, Button, IconButton, CircularProgress,
} from '@mui/material'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import AddIcon from '@mui/icons-material/Add'
import ScheduleIcon from '@mui/icons-material/Schedule'
import { useQuery } from '@tanstack/react-query'
import { shortvideoApi, adaptContentCalendarMonthView, type ContentCalendarMonthStats } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

interface ContentCalendarItem { date: string; items?: Array<{ title: string; status: string }> }

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日']

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate()
}
function getFirstDayOfWeek(year: number, month: number) {
  const d = new Date(year, month - 1, 1).getDay()
  return d === 0 ? 6 : d - 1 // Monday=0
}

export default function ContentCalendarPage() {
  const toast = useToast()
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)

  const { data: calendarData, isLoading } = useQuery({
    queryKey: ['content-calendar', year, month],
    queryFn: async () => {
      const raw = await shortvideoApi.contentCalendarView(year, month)
      return adaptContentCalendarMonthView(raw)
    },
  })

  const { data: stats } = useQuery({
    queryKey: ['content-calendar-stats', year, month],
    queryFn: () => shortvideoApi.contentCalendarMonthStats(year, month),
  })

  const { data: recommendTimes = [], isLoading: recLoading } = useQuery({
    queryKey: ['recommend-publish-time-seo', year, month],
    queryFn: () => shortvideoApi.seoSuggestPublishTime({}),
  })

  const calArr = Array.isArray(calendarData) ? calendarData as ContentCalendarItem[] : []
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

  const padded = [...cells]
  while (padded.length % 7 !== 0) padded.push(null)

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h6" fontWeight={600}>内容日历</Typography>
        <Button variant="contained" size="small" startIcon={<AddIcon />}
          onClick={() => toast('请在对应日期格新建内容计划', 'info')}>
          新建计划
        </Button>
      </Stack>

      {/* 统计卡片 */}
      <Grid container spacing={2}>
        {[
          { label: '本月计划数', value: String(statsData.plannedCount ?? 0) },
          { label: '已发布', value: String(statsData.publishedCount ?? 0) },
          { label: '完成率', value: statsData.completionRate != null ? `${statsData.completionRate}%` : '--' },
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
      <Card variant="outlined">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
            <IconButton onClick={prevMonth}><ChevronLeftIcon /></IconButton>
            <Typography variant="subtitle1" fontWeight={600}>{year} 年 {month} 月</Typography>
            <IconButton onClick={nextMonth}><ChevronRightIcon /></IconButton>
          </Stack>

          {isLoading && <CircularProgress sx={{ mx: 'auto', display: 'block' }} />}

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
                <Box key={day} sx={{
                  border: '1px solid', borderColor: isToday ? 'primary.main' : 'divider',
                  borderRadius: 1, minHeight: 80, p: 0.5, bgcolor: isToday ? 'primary.50' : 'transparent',
                }}>
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
                  </Stack>
                </Box>
              )
            })}
          </Box>
        </CardContent>
      </Card>

      {!recLoading && recommendTimes.length > 0 && (
        <Card variant="outlined">
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
    </Box>
  )
}
