/**
 * 风格对比图表组件
 * @author Claude Code
 * @since 2026-03-06
 */

import React from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  LinearProgress,
  alpha,
  useTheme,
} from '@mui/material'
import type { Theme } from '@mui/material/styles'
import ReactECharts from 'echarts-for-react'
import type { StyleMetrics } from '@/types/effectiveness'

interface StyleComparisonChartProps {
  styles: StyleMetrics[]
  onStyleClick?: (style: string) => void
}

type StyleTone = 'primary' | 'secondary' | 'success' | 'warning' | 'error'

function semanticColor(theme: Theme, tone: StyleTone) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
}

function surfaceColor(theme: Theme, tone: StyleTone) {
  return alpha(semanticColor(theme, tone), theme.palette.mode === 'dark' ? 0.18 : 0.1)
}

function getScoreTone(score: number): StyleTone {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'error'
}

/**
 * 风格对比图表组件
 */
export const StyleComparisonChart: React.FC<StyleComparisonChartProps> = ({
  styles,
  onStyleClick,
}) => {
  const theme = useTheme()
  const scoreColor = semanticColor(theme, 'secondary')
  const conversionColor = semanticColor(theme, 'primary')
  const quietSurface = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.07 : 0.04)
  const progressTrackColor = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.18 : 0.12)

  if (!styles || styles.length === 0) {
    return (
      <Card>
        <CardContent>
          <Typography color="textSecondary">暂无风格数据</Typography>
        </CardContent>
      </Card>
    )
  }

  // 按评分排序
  const sortedStyles = [...styles].sort((a, b) => b.avgScore - a.avgScore)

  // 准备柱状图数据
  const chartData = sortedStyles.map((s) => ({
    name: s.style,
    评分: s.avgScore,
    转化率: (s.avgConversion * 100).toFixed(1),
    版本数: s.versionCount,
  }))

  // 找出最高评分
  const maxScore = Math.max(...sortedStyles.map((s) => s.avgScore))
  const minScore = Math.min(...sortedStyles.map((s) => s.avgScore))

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* 柱状图 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            风格评分对比
          </Typography>
          <Box data-testid="style-comparison-chart-surface" data-chart-colors={`${scoreColor}|${conversionColor}`}>
          <ReactECharts
            option={{
              xAxis: { type: 'category', data: chartData.map((d) => d.name) },
              yAxis: [
                { type: 'value', name: '平均评分', min: 0, max: 100 },
                { type: 'value', name: '转化率 %', position: 'right' }
              ],
              series: [
                {
                  data: chartData.map((d) => d.评分),
                  type: 'bar',
                  name: '平均评分',
                  yAxisIndex: 0,
                  itemStyle: { color: scoreColor },
                },
                {
                  data: chartData.map((d) => d.转化率),
                  type: 'bar',
                  name: '转化率 %',
                  yAxisIndex: 1,
                  itemStyle: { color: conversionColor },
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

      {/* 详细表格 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            风格详细数据
          </Typography>
          <TableContainer component={Paper}>
            <Table>
              <TableHead data-testid="style-comparison-table-head-surface" sx={{ backgroundColor: quietSurface }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 'bold' }}>风格</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                    版本数
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                    平均评分
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                    平均转化率
                  </TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>最高版本</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>评分进度</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sortedStyles.map((style, idx) => (
                  <TableRow
                    key={style.style}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => onStyleClick?.(style.style)}
                  >
                    <TableCell sx={{ fontWeight: 'bold' }}>
                      <Chip
                        label={style.style}
                        variant="outlined"
                        size="small"
                        color={idx === 0 ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell align="right">{style.versionCount}</TableCell>
                    <TableCell
                      align="right"
                      data-testid={style.avgScore === maxScore ? 'style-comparison-max-score-cell-surface' : undefined}
                      data-score-tone={style.avgScore === maxScore ? 'warning' : undefined}
                      sx={{
                        backgroundColor:
                          style.avgScore === maxScore ? surfaceColor(theme, 'warning') : 'transparent',
                        fontWeight:
                          style.avgScore === maxScore ? 'bold' : 'normal',
                      }}
                    >
                      {style.avgScore.toFixed(2)}
                    </TableCell>
                    <TableCell align="right">
                      {(style.avgConversion * 100).toFixed(1)}%
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        v{style.topVersion.versionNumber} ({style.topVersion.score.toFixed(1)})
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        {(() => {
                          const tone = getScoreTone(style.avgScore)
                          const color = semanticColor(theme, tone)
                          return (
                        <LinearProgress
                          data-testid="style-comparison-progress-surface"
                          data-score-tone={tone}
                          data-score-color={color}
                          variant="determinate"
                          value={(style.avgScore / 100) * 100}
                          sx={{
                            flex: 1,
                            height: 8,
                            borderRadius: 4,
                            backgroundColor: progressTrackColor,
                            '& .MuiLinearProgress-bar': {
                              backgroundColor: color,
                            },
                          }}
                        />
                          )
                        })()}
                        <Typography variant="caption" sx={{ minWidth: 35 }}>
                          {(style.avgScore / 100) * 100}%
                        </Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* 风格统计摘要 */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
            风格统计
          </Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 2 }}>
            <Box data-testid="style-comparison-summary-surface" data-summary-tone="neutral" sx={{ p: 2, backgroundColor: quietSurface, border: `1px solid ${theme.palette.divider}`, borderRadius: 1 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                风格总数
              </Typography>
              <Typography variant="h6">{styles.length}</Typography>
            </Box>

            <Box data-testid="style-comparison-summary-surface" data-summary-tone="warning" sx={{ p: 2, backgroundColor: surfaceColor(theme, 'warning'), border: `1px solid ${alpha(semanticColor(theme, 'warning'), 0.3)}`, borderRadius: 1 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                最高评分
              </Typography>
              <Typography variant="h6">{maxScore.toFixed(2)}</Typography>
              <Typography variant="caption">
                {sortedStyles[0]?.style}
              </Typography>
            </Box>

            <Box data-testid="style-comparison-summary-surface" data-summary-tone="error" sx={{ p: 2, backgroundColor: surfaceColor(theme, 'error'), border: `1px solid ${alpha(semanticColor(theme, 'error'), 0.3)}`, borderRadius: 1 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                最低评分
              </Typography>
              <Typography variant="h6">{minScore.toFixed(2)}</Typography>
              <Typography variant="caption">
                {sortedStyles[sortedStyles.length - 1]?.style}
              </Typography>
            </Box>

            <Box data-testid="style-comparison-summary-surface" data-summary-tone="primary" sx={{ p: 2, backgroundColor: surfaceColor(theme, 'primary'), border: `1px solid ${alpha(semanticColor(theme, 'primary'), 0.3)}`, borderRadius: 1 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                平均评分
              </Typography>
              <Typography variant="h6">
                {(
                  sortedStyles.reduce((sum, s) => sum + s.avgScore, 0) / sortedStyles.length
                ).toFixed(2)}
              </Typography>
            </Box>

            <Box data-testid="style-comparison-summary-surface" data-summary-tone="success" sx={{ p: 2, backgroundColor: surfaceColor(theme, 'success'), border: `1px solid ${alpha(semanticColor(theme, 'success'), 0.3)}`, borderRadius: 1 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                平均转化率
              </Typography>
              <Typography variant="h6">
                {(
                  sortedStyles.reduce((sum, s) => sum + s.avgConversion, 0) /
                  sortedStyles.length *
                  100
                ).toFixed(1)}
                %
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default StyleComparisonChart
