import { useEffect, useState } from 'react'
import { Box, Card, CardContent, CircularProgress, Grid, Typography } from '@mui/material'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import PeopleIcon from '@mui/icons-material/People'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import AutoStoriesIcon from '@mui/icons-material/AutoStories'
import { PageHeader } from '@/components/base'
import { dashboardApi, type AdminStats, type KpiUnified } from '@/api/dashboard'
import { useToast } from '@/contexts/ToastContext'

interface KpiCardProps {
  label: string
  value: string
  sub?: string
  color?: string
  icon: React.ReactNode
}

function KpiCard({ label, value, sub, color = '#1976d2', icon }: KpiCardProps) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent sx={{ height: '100%', display: 'flex', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', width: '100%' }}>
          <Box>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography variant="h5" sx={{ fontWeight: 700, color }}>{value}</Typography>
            {sub && <Typography variant="caption" color="text.secondary">{sub}</Typography>}
          </Box>
          <Box sx={{ color, opacity: 0.7 }}>{icon}</Box>
        </Box>
      </CardContent>
    </Card>
  )
}

export default function UnifiedKpiPage() {
  const toast = useToast()
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [kpi, setKpi] = useState<KpiUnified | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      dashboardApi.adminStats(),
      dashboardApi.kpiUnified(30),
    ])
      .then(([s, k]) => {
        setStats({
          totalUsers: Number(s.totalUsers ?? 0),
          totalLiveSessions: Number(s.totalLiveSessions ?? 0),
          activeUsers: Number(s.activeUsers ?? 0),
          todayUsers: Number(s.todayUsers ?? 0),
          totalVideos: Number(s.totalVideos ?? 0),
          publishedVideos: Number(s.publishedVideos ?? 0),
          todayVideos: Number(s.todayVideos ?? 0),
          completedSessions: Number(s.completedSessions ?? 0),
          todaySessions: Number(s.todaySessions ?? 0),
          totalShortVideos: Number(s.totalShortVideos ?? 0),
          publishedShortVideos: Number(s.publishedShortVideos ?? 0),
          todayShortVideos: Number(s.todayShortVideos ?? 0),
          totalCopyItems: Number(s.totalCopyItems ?? 0),
          approvedCopyItems: Number(s.approvedCopyItems ?? 0),
          todayCopyItems: Number(s.todayCopyItems ?? 0),
          todayAiCalls: Number(s.todayAiCalls ?? 0),
          todayAiAttempts: Number(s.todayAiAttempts ?? 0),
          aiSuccessRate: Number(s.aiSuccessRate ?? 0),
          todayRevenue: Number(s.todayRevenue ?? 0),
        })
        setKpi({
          gmvToday: Number(k.gmvToday ?? 0),
          gmvMom: Number(k.gmvMom ?? 0),
          gmvYoy: Number(k.gmvYoy ?? 0),
          ordersToday: Number(k.ordersToday ?? 0),
          avgOrderValue: Number(k.avgOrderValue ?? 0),
          conversionRate: Number(k.conversionRate ?? 0),
          liveSessions: Number(k.liveSessions ?? 0),
          aiCallsToday: Number(k.aiCallsToday ?? 0),
        })
      })
      .catch(() => toast('加载 KPI 数据失败', 'error'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <Box sx={{ p: 4, display: 'flex', justifyContent: 'center' }}><CircularProgress /></Box>
  )

  const fmt = (n: number | undefined) => (n ?? 0).toLocaleString()
  const fmtMoney = (n: number | undefined) => `¥${(n ?? 0).toLocaleString()}`
  const fmtPct = (n: number | undefined) => `${((n ?? 0) * 100).toFixed(1)}%`

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="统一 KPI" subtitle="平台关键绩效指标总览" />

      <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 3, mb: 1 }}>GMV 与交易</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="今日 GMV" value={fmtMoney(kpi?.gmvToday)} sub={`环比 ${fmtPct(kpi?.gmvMom)}`} color="#fb8c00" icon={<TrendingUpIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="今日订单数" value={fmt(kpi?.ordersToday)} sub={`均单价 ${fmtMoney(kpi?.avgOrderValue)}`} color="#e53935" icon={<TrendingUpIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="转化率" value={fmtPct(kpi?.conversionRate)} color="#43a047" icon={<TrendingUpIcon />} />
        </Grid>
      </Grid>

      <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 3, mb: 1 }}>运营数据</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="今日直播场次" value={fmt(stats?.todaySessions)} sub={`累计 ${fmt(stats?.totalLiveSessions)}`} color="#1976d2" icon={<LiveTvIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="今日 AI 调用" value={fmt(stats?.todayAiCalls)} sub={`成功率 ${fmtPct(stats?.aiSuccessRate)}`} color="#7b1fa2" icon={<SmartToyIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="今日短视频" value={fmt(stats?.todayVideos)} sub={`累计 ${fmt(stats?.totalVideos)}`} color="#0097a7" icon={<VideoLibraryIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="活跃用户" value={fmt(stats?.activeUsers)} sub={`今日新增 ${fmt(stats?.todayUsers)}`} color="#f57c00" icon={<PeopleIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="文案库总量" value={fmt(stats?.totalCopyItems)} sub={`已审批 ${fmt(stats?.approvedCopyItems)}`} color="#388e3c" icon={<AutoStoriesIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <KpiCard label="今日收入" value={fmtMoney(stats?.todayRevenue)} color="#c62828" icon={<TrendingUpIcon />} />
        </Grid>
      </Grid>
    </Box>
  )
}
