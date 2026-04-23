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
} from '@mui/material'
import ReactECharts from 'echarts-for-react'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat'
import type { TrendPoint, TrendAnalysis } from '@/types/effectiveness'

interface EffectivenessTrendChartProps {
  data: TrendPoint[]
  analysis?: TrendAnalysis | null
}

const getTrendIcon = (trend: string) => {
  if (trend === 'up') return <TrendingUpIcon sx={{ color: '#4caf50' }} />
  if (trend === 'down') return <TrendingDownIcon sx={{ color: '#f44336' }} />
  return <TrendingFlatIcon sx={{ color: '#ff9800' }} />
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
  const [timePeriod, setTimePeriod] = useState<'7' | '30' | '90'>('30')
  const [groupBy, setGroupBy] = useState<'day' | 'week'>('day')

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
            <Card sx={{ backgroundColor: '#f5f5f5' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  {getTrendIcon(analysis.overallTrend)}
                  <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                    总体趋势
                  </Typography>
                </Box>
                <Typography variant="h6">{getTrendText(analysis.overallTrend)}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card
              sx={{
                backgroundColor:
                  analysis.scoreChange > 0 ? '#e8f5e9' : analysis.scoreChange < 0 ? '#ffebee' : '#fff3e0',
              }}
            >
              <CardContent>
                <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  评分变化
                </Typography>
                <Typography
                  variant="h6"
                  sx={{
                    color:
                      analysis.scoreChange > 0
                        ? '#4caf50'
                        : analysis.scoreChange < 0
                          ? '#f44336'
                          : '#ff9800',
                  }}
                >
                  {analysis.scoreChange > 0 ? '+' : ''}
                  {analysis.scoreChange.toFixed(2)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ backgroundColor: '#f5f5f5' }}>
              <CardContent>
                <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  最高评分日期
                </Typography>
                <Typography variant="body2">{analysis.highestDate}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ backgroundColor: '#f5f5f5' }}>
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
                  lineStyle: { width: 3, color: '#ff7043' },
                  symbol: 'circle',
                  symbolSize: 4,
                  itemStyle: { color: '#ff7043' },
                }
              ],
              tooltip: { trigger: 'axis' },
              legend: { show: true },
            }}
            style={{ height: '300px' }}
          />
        </CardContent>
      </Card>

      {/* 使用次数和转化率趋势 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            使用次数与转化率趋势
          </Typography>
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
                  lineStyle: { width: 2, dashArray: [5, 5], color: '#4caf50' },
                  symbol: 'none',
                },
                {
                  data: data.map((d) => d.conversionRate),
                  type: 'line',
                  name: '转化率',
                  yAxisIndex: 1,
                  smooth: true,
                  lineStyle: { width: 2, dashArray: [5, 5], color: '#2196f3' },
                  symbol: 'none',
                }
              ],
              tooltip: { trigger: 'axis' },
              legend: { show: true },
            }}
            style={{ height: '300px' }}
          />
        </CardContent>
      </Card>

      {/* 互动趋势 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            互动数据趋势
          </Typography>
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
                  lineStyle: { width: 2, color: '#ff9800' },
                  symbol: 'none',
                }
              ],
              tooltip: { trigger: 'axis' },
              legend: { show: true },
            }}
            style={{ height: '300px' }}
          />
        </CardContent>
      </Card>
    </Box>
  )
}

export default EffectivenessTrendChart
