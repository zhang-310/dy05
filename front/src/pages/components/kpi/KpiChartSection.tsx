import { Box, Typography, Card, Skeleton, FormControl, InputLabel, Select, MenuItem } from '@mui/material'
import { useTranslation } from 'react-i18next'
import { useMemo } from 'react'
import ReactECharts from 'echarts-for-react'
import type { EChartsOption } from 'echarts'
import { echarts } from '@/utils/echarts-registry'
import type { LiveFormatGmvSummaryVO, ConversionFunnelVO } from '@/types/dashboard'
import { parseMoney } from '@/types/dashboard'

export interface KpiChartSectionProps {
  loading: boolean
  lookbackDays: number
  formatGmv: LiveFormatGmvSummaryVO | null
  funnel: ConversionFunnelVO | null
  onLookbackDaysChange: (v: number) => void
}

export function KpiChartSection({
  loading,
  lookbackDays,
  formatGmv,
  funnel,
  onLookbackDaysChange,
}: KpiChartSectionProps) {
  const { t } = useTranslation()

  const formatGmvChartOption: EChartsOption = useMemo(() => {
    const rows = formatGmv?.rows ?? []
    const categories = rows.map((r) => r.liveFormat ?? '-')
    const values = rows.map((r) => parseMoney(r.totalGmv))
    return {
      tooltip: { trigger: 'axis' },
      grid: { left: 56, right: 16, bottom: 56, top: 24 },
      xAxis: {
        type: 'category',
        data: categories,
        axisLabel: { rotate: 28, fontSize: 11 },
      },
      yAxis: { type: 'value', name: 'GMV (¥)' },
      series: [
        {
          type: 'bar',
          data: values,
          itemStyle: { borderRadius: [4, 4, 0, 0] },
        },
      ],
    }
  }, [formatGmv])

  const roiScatterOption: EChartsOption = useMemo(() => {
    const rows = formatGmv?.rows ?? []
    const data = rows.map((r) => {
      const sessions = r.sessionCount ?? 0
      const total = parseMoney(r.totalGmv)
      const avg = sessions > 0 ? total / sessions : 0
      return {
        name: r.liveFormat ?? '-',
        value: [sessions, avg, total],
      }
    })
    const maxTotal = Math.max(...data.map((d) => (d.value[2] as number)), 1)
    return {
      tooltip: {
        trigger: 'item' as const,
        formatter: (p: unknown) => {
          const params = p as { name?: string; value?: number[] }
          const [sessions, avg, total] = params.value ?? [0, 0, 0]
          return `${params.name ?? ''}<br/>场次数: ${sessions}<br/>单场均GMV: ¥${avg.toLocaleString('zh-CN', { maximumFractionDigits: 0 })}<br/>总GMV: ¥${total.toLocaleString('zh-CN', { maximumFractionDigits: 0 })}`
        },
      },
      grid: { left: 72, right: 24, bottom: 48, top: 24 },
      xAxis: { type: 'value' as const, name: '场次数', nameLocation: 'middle' as const, nameGap: 28 },
      yAxis: { type: 'value' as const, name: '单场均GMV (¥)', nameLocation: 'middle' as const, nameGap: 50 },
      series: [
        {
          type: 'scatter' as const,
          data,
          symbolSize: (val: number[]) => Math.max(10, Math.min(60, (val[2] / maxTotal) * 60)),
          label: {
            show: true,
            position: 'top' as const,
            formatter: (p: unknown) => (p as { name?: string }).name ?? '',
            fontSize: 11,
          },
        },
      ],
    } as EChartsOption
  }, [formatGmv])

  const funnelChartOption: EChartsOption = useMemo(() => {
    const steps = funnel?.steps ?? []
    return {
      tooltip: {
        trigger: 'item' as const,
        formatter: (p: unknown) => {
          const params = p as { name?: string; value?: number; data?: { rate?: number } }
          return `${params.name ?? ''}: ${(params.value ?? 0).toLocaleString()}（转化率 ${(params.data?.rate ?? 0).toFixed(1)}%）`
        },
      },
      series: [
        {
          type: 'funnel' as const,
          left: '10%',
          top: 16,
          bottom: 16,
          width: '80%',
          min: 0,
          max: Math.max(...steps.map((s) => s.value), 1),
          sort: 'none' as const,
          gap: 4,
          label: {
            show: true,
            position: 'inside' as const,
            formatter: (p: unknown) => {
              const params = p as { name?: string; value?: number }
              return `${params.name ?? ''}\n${(params.value ?? 0).toLocaleString()}`
            },
          },
          data: steps.map((s) => ({ name: s.name, value: s.value, rate: s.rate })),
        },
      ],
    } as EChartsOption
  }, [funnel])

  return (
    <>
      {/* Format GMV Bar Chart */}
      <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 1 }}>
          <Box>
            <Typography variant="subtitle2" fontWeight={600}>
              {t('kpi.formatGmvTitle')}
            </Typography>
            <Typography variant="caption" color="text.secondary" display="block">
              {t('kpi.formatGmvHint')}
              {formatGmv?.since != null ? ` · ${t('kpi.dateFrom')}: ${formatGmv.since}` : ''}
            </Typography>
          </Box>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel>{t('kpi.lookbackDays')}</InputLabel>
            <Select
              label={t('kpi.lookbackDays')}
              value={lookbackDays}
              onChange={(e) => onLookbackDaysChange(Number(e.target.value))}
            >
              <MenuItem value={30}>30</MenuItem>
              <MenuItem value={90}>90</MenuItem>
              <MenuItem value={180}>180</MenuItem>
              <MenuItem value={365}>365</MenuItem>
            </Select>
          </FormControl>
        </Box>
        {loading && !formatGmv ? (
          <Skeleton variant="rounded" height={260} />
        ) : (
          <ReactECharts echarts={echarts} option={formatGmvChartOption} style={{ height: 280 }} notMerge lazyUpdate />
        )}
      </Card>

      {/* ROI Scatter Chart */}
      <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
        <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 0.5 }}>
          {t('kpi.roiScatterTitle', '直播形式 ROI 分布')}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.5 }}>
          {t('kpi.roiScatterHint', 'X轴=场次数, Y轴=单场均GMV, 气泡大小=总GMV')}
        </Typography>
        {loading && !formatGmv ? (
          <Skeleton variant="rounded" height={300} />
        ) : (
          <ReactECharts echarts={echarts} option={roiScatterOption} style={{ height: 320 }} notMerge lazyUpdate />
        )}
      </Card>

      {/* Conversion Funnel Chart */}
      <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
        <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 0.5 }}>
          {t('kpi.funnelTitle', '转化漏斗')}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.5 }}>
          {t('kpi.funnelHint', '曝光 → 互动 → 加购 → 成交')}
          {funnel?.lookbackDays != null ? ` · 近 ${funnel.lookbackDays} 天` : ''}
        </Typography>
        {loading && !funnel ? (
          <Skeleton variant="rounded" height={280} />
        ) : (
          <ReactECharts echarts={echarts} option={funnelChartOption} style={{ height: 300 }} notMerge lazyUpdate />
        )}
      </Card>
    </>
  )
}
