/**
 * 效果趋势图表组件
 * @author Claude Code
 * @since 2026-03-06
 */

import React, { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  ButtonGroup,
  Paper,
  Grid,
  Stack,
  alpha,
  useTheme,
} from '@mui/material'
import ReactECharts from 'echarts-for-react'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat'
import type { TrendPoint, TrendAnalysis } from '@/types/effectiveness'
import type { Theme } from '@mui/material/styles'

interface EffectivenessTrendChartProps {
  data: TrendPoint[]
  analysis?: TrendAnalysis | null
}

type EffectivenessTone = 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'

const semanticColor = (theme: Theme, tone: EffectivenessTone) =>
  theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main

const getTrendTone = (trend: string): EffectivenessTone => {
  if (trend === 'up') return 'success'
  if (trend === 'down') return 'error'
  return 'warning'
}

const getScoreChangeTone = (scoreChange: number): EffectivenessTone => {
  if (scoreChange > 0) return 'success'
  if (scoreChange < 0) return 'error'
  return 'warning'
}

const getTrendIcon = (trend: string, color: string, tone: EffectivenessTone) => {
  const iconProps = {
    'data-testid': 'effectiveness-trend-icon-surface',
    'data-trend-tone': tone,
    'data-trend-color': color,
    sx: { color },
  }
  if (trend === 'up') return <TrendingUpIcon {...iconProps} />
  if (trend === 'down') return <TrendingDownIcon {...iconProps} />
  return <TrendingFlatIcon {...iconProps} />
}

const getTrendText = (trend: string) => {
  if (trend === 'up') return '上升'
  if (trend === 'down') return '下降'
  return '平稳'
}

/**
 * 效果趋势图表组件
 */
export const EffectivenessTrendChart: React.FC<EffectivenessTrendChartProps> = ({
  data,
  analysis,
}) => {
  const theme = useTheme()
  const [timePeriod, setTimePeriod] = useState<'7' | '30' | '90'>('30')
  const [groupBy, setGroupBy] = useState<'day' | 'week'>('day')

  const scoreColor = semanticColor(theme, 'secondary')
  const usageColor = semanticColor(theme, 'success')
  const conversionColor = semanticColor(theme, 'primary')
  const likesColor = semanticColor(theme, 'warning')
  const neutralCardBackground = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.07 : 0.04)
  const neutralCardBorder = alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.9 : 0.7)

  if (!data || data.length === 0) {
    return (
      <Card>
        <CardContent>
          <Typography color="textSecondary">暂无趋势数据</Typography>
        </CardContent>
      </Card>
    )
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* 时间范围和聚合方式选择 */}
      <Paper sx={{ p: 2 }}>
        <Stack spacing={2}>
          <Box>
            <Typography variant="body2" sx={{ mb: 1, fontWeight: 'bold' }}>
              时间范围
            </Typography>
            <ButtonGroup size="small" variant="outlined">
              <Button
                variant={timePeriod === '7' ? 'contained' : 'outlined'}
                onClick={() => setTimePeriod('7')}
              >
                7 日
              </Button>
              <Button
                variant={timePeriod === '30' ? 'contained' : 'outlined'}
                onClick={() => setTimePeriod('30')}
              >
                30 日
              </Button>
              <Button
                variant={timePeriod === '90' ? 'contained' : 'outlined'}
                onClick={() => setTimePeriod('90')}
              >
                90 日
              </Button>
            </ButtonGroup>
          </Box>

          <Box>
            <Typography variant="body2" sx={{ mb: 1, fontWeight: 'bold' }}>
              数据聚合
            </Typography>
            <ButtonGroup size="small" variant="outlined">
              <Button
                variant={groupBy === 'day' ? 'contained' : 'outlined'}
                onClick={() => setGroupBy('day')}
              >
                按天
              </Button>
              <Button
                variant={groupBy === 'week' ? 'contained' : 'outlined'}
                onClick={() => setGroupBy('week')}
              >
                按周
              </Button>
            </ButtonGroup>
          </Box>
        </Stack>
      </Paper>

      {/* 趋势分析摘要 */}
      {analysis && (
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={3}>
            <Card
              data-testid="effectiveness-analysis-card-surface"
              data-card-tone={getTrendTone(analysis.overallTrend)}
              sx={{
                backgroundColor: neutralCardBackground,
                border: `1px solid ${neutralCardBorder}`,
              }}
            >
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  {getTrendIcon(
                    analysis.overallTrend,
                    semanticColor(theme, getTrendTone(analysis.overallTrend)),
                    getTrendTone(analysis.overallTrend),
                  )}
                  <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                    总体趋势
                  </Typography>
                </Box>
                <Typography variant="h6">{getTrendText(analysis.overallTrend)}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            {(() => {
              const tone = getScoreChangeTone(analysis.scoreChange)
              const color = semanticColor(theme, tone)
              return (
            <Card
              data-testid="effectiveness-score-change-card-surface"
              data-card-tone={tone}
              data-card-color={color}
              sx={{
                backgroundColor: alpha(color, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                border: `1px solid ${alpha(color, theme.palette.mode === 'dark' ? 0.38 : 0.24)}`,
              }}
            >
              <CardContent>
                <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  评分变化
                </Typography>
                <Typography
                  data-testid="effectiveness-score-change-value-surface"
                  data-score-tone={tone}
                  data-score-color={color}
                  variant="h6"
                  sx={{ color }}
                >
                  {analysis.scoreChange > 0 ? '+' : ''}
                  {analysis.scoreChange.toFixed(2)}
                </Typography>
              </CardContent>
            </Card>
              )
            })()}
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card
              data-testid="effectiveness-analysis-card-surface"
              data-card-tone="primary"
              sx={{
                backgroundColor: neutralCardBackground,
                border: `1px solid ${neutralCardBorder}`,
              }}
            >
              <CardContent>
                <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  最高评分日期
                </Typography>
                <Typography variant="body2">{analysis.highestDate}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card
              data-testid="effectiveness-analysis-card-surface"
              data-card-tone="info"
              sx={{
                backgroundColor: neutralCardBackground,
                border: `1px solid ${neutralCardBorder}`,
              }}
            >
              <CardContent>
                <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  波动度
                </Typography>
                <Typography variant="h6">{(analysis.volatility * 100).toFixed(1)}%</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* 评分趋势线 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            评分趋势（主轴）
          </Typography>
          <Box data-testid="effectiveness-score-trend-chart-surface" data-chart-color={scoreColor}>
          <ReactECharts
            option={{
              xAxis: { type: 'category', data: data.map((d) => d.date) },
              yAxis: { type: 'value', min: 0, max: 100 },
              series: [
                {
                  data: data.map((d) => d.score),
                  type: 'line',
                  name: '评分',
                  smooth: true,
                  lineStyle: { width: 3, color: scoreColor },
                  symbol: 'circle',
                  symbolSize: 4,
                  itemStyle: { color: scoreColor },
                }
              ],
              tooltip: { trigger: 'axis' },
              legend: { show: true },
            }}
            style={{ height: '300px' }}
          />
          </Box>
        </CardContent>
      </Card>

      {/* 使用次数和转化率趋势 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            使用次数与转化率趋势
          </Typography>
          <Box
            data-testid="effectiveness-usage-conversion-chart-surface"
            data-chart-colors={`${usageColor}|${conversionColor}`}
          >
          <ReactECharts
            option={{
              xAxis: { type: 'category', data: data.map((d) => d.date) },
              yAxis: [
                { type: 'value', name: '使用次数' },
                { type: 'value', name: '转化率' }
              ],
              series: [
                {
                  data: data.map((d) => d.usageCount),
                  type: 'line',
                  name: '使用次数',
                  yAxisIndex: 0,
                  smooth: true,
                  lineStyle: { width: 2, dashArray: [5, 5], color: usageColor },
                  symbol: 'none',
                },
                {
                  data: data.map((d) => d.conversionRate),
                  type: 'line',
                  name: '转化率',
                  yAxisIndex: 1,
                  smooth: true,
                  lineStyle: { width: 2, dashArray: [5, 5], color: conversionColor },
                  symbol: 'none',
                }
              ],
              tooltip: { trigger: 'axis' },
              legend: { show: true },
            }}
            style={{ height: '300px' }}
          />
          </Box>
        </CardContent>
      </Card>

      {/* 互动趋势 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            互动数据趋势
          </Typography>
          <Box data-testid="effectiveness-likes-chart-surface" data-chart-color={likesColor}>
          <ReactECharts
            option={{
              xAxis: { type: 'category', data: data.map((d) => d.date) },
              yAxis: { type: 'value' },
              series: [
                {
                  data: data.map((d) => d.likesCount),
                  type: 'line',
                  name: '点赞数',
                  smooth: true,
                  lineStyle: { width: 2, color: likesColor },
                  symbol: 'none',
                }
              ],
              tooltip: { trigger: 'axis' },
              legend: { show: true },
            }}
            style={{ height: '300px' }}
          />
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default EffectivenessTrendChart
