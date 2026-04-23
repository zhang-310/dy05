import { useState } from 'react'
import { Box, Card, CardContent, Typography, Stack, Chip, Button, TextField, Grid } from '@mui/material'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import { useQuery } from '@tanstack/react-query'
import { shortvideoApi, adaptContentCalendarMonthView } from '@/api/shortvideo'

const DAYS_OF_WEEK = ['日', '一', '二', '三', '四', '五', '六']

function getMonthDates(year: number, month: number) {
  const dates: Date[] = []
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  for (let d = first; d <= last; d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)) {
    dates.push(new Date(d))
  }
  return dates
}

export default function PublishPage() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())
  const [accountId, setAccountId] = useState('')

  const monthNum = month + 1

  const { data: calArr = [] } = useQuery({
    queryKey: ['sv-publish-calendar', year, monthNum],
    queryFn: async () => {
      const raw = await shortvideoApi.contentCalendarView(year, monthNum)
      return adaptContentCalendarMonthView(raw)
    },
  })

  const { data: recommendData } = useQuery({
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
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
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

      <Card variant="outlined">
        <CardContent>
          <Stack direction="row" alignItems="center" spacing={2} mb={2}>
            <CalendarMonthIcon color="primary" />
            <Button size="small" onClick={prevMonth}>{'<'}</Button>
            <Typography variant="subtitle1" fontWeight={700}>
              {year} 年 {month + 1} 月
            </Typography>
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
                  <Box sx={{
                    height: 80, border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 0.5, m: 0.25,
                    bgcolor: isToday ? 'primary.50' : 'transparent',
                  }}>
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
        </CardContent>
      </Card>

      {flatList.length > 0 && (
        <>
          <Typography variant="subtitle2">本月条目（{flatList.length} 条）</Typography>
          <Stack spacing={1}>
            {flatList.map((item, i) => (
              <Card key={i} variant="outlined">
                <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
                  <Stack direction="row" spacing={2} alignItems="center">
                    <Typography variant="body2" fontWeight={500} sx={{ flex: 1 }}>
                      {item.title || `内容 ${i + 1}`}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.publishDate}
                    </Typography>
                    <Chip
                      label={item.status === 'published' ? '已发布' : '计划'}
                      size="small"
                      color={item.status === 'published' ? 'success' : 'default'}
                    />
                  </Stack>
                </CardContent>
              </Card>
            ))}
          </Stack>
        </>
      )}

      {flatList.length === 0 && (
        <Typography color="text.secondary" textAlign="center" sx={{ py: 4 }}>本月暂无计划/发布数据（数据来自项目排期与已发布视频）</Typography>
      )}
    </Box>
  )
}
