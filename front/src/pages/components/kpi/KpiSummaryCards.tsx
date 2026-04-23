import {
  Typography,
  Card,
  CardContent,
  Skeleton,
  Grid,
  Divider,
} from '@mui/material'
import { useTranslation } from 'react-i18next'
import type { UnifiedKpiVO } from '@/types/dashboard'
import { formatCurrencyYuan, parseMoney, calcGmvDelta } from '@/types/dashboard'

export interface KpiSummaryCardsProps {
  loading: boolean
  data: UnifiedKpiVO | null
}

export function KpiSummaryCards({ loading, data }: KpiSummaryCardsProps) {
  const { t } = useTranslation()

  const rev = data?.revenue
  const today = rev ? parseMoney(rev.todayGmv) : 0
  const yest = rev ? parseMoney(rev.yesterdayGmv) : 0
  const delta = rev ? calcGmvDelta(today, yest) : null
  const aov = rev ? parseMoney(rev.avgOrderValueToday) : 0
  const content = data?.content
  const traffic = data?.traffic
  const conversion = data?.conversion

  return (
    <>
      {/* Revenue KPI Cards */}
      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
        {t('kpi.revenue')}
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">{t('kpi.gmvToday')}</Typography>
              {loading ? <Skeleton width={120} height={40} /> : (
                <Typography variant="h5" fontWeight={700}>{formatCurrencyYuan(today)}</Typography>
              )}
              {delta && (
                <Typography variant="caption" color={delta.up ? 'success.main' : 'text.secondary'}>
                  {t('kpi.vsYesterday')} {delta.up ? '+' : ''}{delta.pct.toFixed(1)}%
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">{t('kpi.gmvYesterday')}</Typography>
              {loading ? <Skeleton width={120} height={40} /> : <Typography variant="h5">{formatCurrencyYuan(yest)}</Typography>}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">{t('kpi.aov')}</Typography>
              {loading ? <Skeleton width={120} height={40} /> : <Typography variant="h5">{formatCurrencyYuan(aov)}</Typography>}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">{t('kpi.cockpitCard')}</Typography>
              <Typography variant="body2" color="text.secondary">{t('kpi.cockpitCardDesc')}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Divider sx={{ my: 2 }} />

      {/* Content KPI Cards */}
      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
        {t('kpi.content')}
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.shortVideos')}</Typography>
              {loading ? <Skeleton width={80} height={32} /> : <Typography variant="h5">{content?.shortVideoCount ?? 0}</Typography>}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.scripts')}</Typography>
              {loading ? <Skeleton width={80} height={32} /> : <Typography variant="h5">{content?.productScriptCount ?? 0}</Typography>}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.kbDocs')}</Typography>
              {loading ? <Skeleton width={80} height={32} /> : <Typography variant="h5">{content?.kbDocumentCount ?? 0}</Typography>}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Traffic KPI Cards */}
      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
        {t('kpi.traffic')}
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.viewersSum')}</Typography>
              {loading ? <Skeleton width={100} height={32} /> : <Typography variant="h5">{(traffic?.liveViewerSum ?? 0).toLocaleString()}</Typography>}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.playsSum')}</Typography>
              {loading ? <Skeleton width={100} height={32} /> : <Typography variant="h5">{(traffic?.shortVideoViewSum ?? 0).toLocaleString()}</Typography>}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Conversion KPI Cards */}
      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
        {t('kpi.conversion')}
      </Typography>
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} sm={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.salesToday')}</Typography>
              {loading ? <Skeleton width={80} height={32} /> : <Typography variant="h5">{(conversion?.saleQuantitySinceToday ?? 0).toLocaleString()}</Typography>}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="body2" color="text.secondary">{t('kpi.revLines')}</Typography>
              {loading ? <Skeleton width={80} height={32} /> : <Typography variant="h5">{(conversion?.revenueLinesSinceToday ?? 0).toLocaleString()}</Typography>}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  )
}
