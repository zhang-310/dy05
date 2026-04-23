import { useState } from 'react'
import { Box, Card, CardContent, Typography, Stack, Button, TextField, Chip, Grid, MenuItem, LinearProgress } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

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

interface CalendarItem {
  title?: string
  videoTitle?: string
  publishDate?: string
  date?: string
  scheduledDate?: string
  status?: number
  publishTime?: string
  [key: string]: unknown
}

interface DashboardStats {
  [key: string]: number | string | undefined
}

export default function DailyContentPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [projectId, setProjectId] = useState('')
  const [days, setDays] = useState(7)

  const weekDates = getWeekDates()
  const startDate = weekDates[0].toISOString().slice(0, 10)
  const endDate = weekDates[6].toISOString().slice(0, 10)

  const { data: calendarData, isLoading } = useQuery({
    queryKey: ['sv-daily-calendar', projectId, startDate],
    queryFn: () => shortvideoApi.calendar({
      startDate,
      endDate,
      projectId: projectId ? Number(projectId) : undefined,
    }),
  })
  const calendar = Array.isArray(calendarData) ? calendarData as CalendarItem[] : []

  const { data: statsData } = useQuery({
    queryKey: ['sv-daily-stats', projectId, days],
    queryFn: () => shortvideoApi.dashboardStats(),
  })
  const stats = statsData as DashboardStats | undefined

  const generateMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      await shortvideoApi.generateDaily(projectId ? Number(projectId) : 0)
    },
    onSuccess: () => { toast('日排计划已生成', 'success'); qc.invalidateQueries({ queryKey: ['sv-daily-calendar'] }) },
    onError: () => toast('生成失败', 'error'),
  })

  const calendarMap: Record<string, Record<string, unknown>[]> = {}
  for (const item of calendar) {
    const dateStr = String(item.publishDate ?? item.date ?? item.scheduledDate ?? '').slice(0, 10)
    if (dateStr) {
      if (!calendarMap[dateStr]) calendarMap[dateStr] = []
      calendarMap[dateStr].push(item)
    }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField size="small" label="项目 ID" value={projectId}
          onChange={e => setProjectId(e.target.value)} sx={{ width: 160 }} />
        <TextField select size="small" label="统计天数" value={days}
          onChange={e => setDays(Number(e.target.value))} sx={{ width: 120 }}>
          {[7, 14, 30].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
        </TextField>
        <Box sx={{ flex: 1 }} />
        <Button variant="outlined" startIcon={<AutoAwesomeIcon />}
          onClick={() => generateMutation.mutate()} disabled={!projectId || generateMutation.isPending}>
          AI 生成日排
        </Button>
        <Button variant="contained" startIcon={<AddIcon />} disabled>
          手动排期
        </Button>
      </Stack>

      {isLoading && <LinearProgress />}

      {/* 统计卡片 */}
      {stats && (
        <Grid container spacing={2}>
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
      <Typography variant="subtitle2">本周排期</Typography>
      <Grid container spacing={1.5}>
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
                    <Typography variant="caption" color="text.secondary">无排期</Typography>
                  ) : (
                    <Stack spacing={0.5}>
                      {items.map((item, i) => (
                        <Box key={i} sx={{ p: 0.75, bgcolor: 'grey.50', borderRadius: 0.5 }}>
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
    </Box>
  )
}
