/**
 * MetricsDashboard (W-10)
 * 实时指标监控仪表板
 */

import { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  CircularProgress,
  Button,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material'
import { RefreshOutlined as RefreshIcon } from '@mui/icons-material'
import { useMonitoringData } from '@/hooks/useMonitoringData'

interface MetricStatCardProps {
  title: string
  value: string | number
  unit?: string
  trend?: number
  color?: string
}

function MetricStatCard({ title, value, unit = '', trend, color = '#1976d2' }: MetricStatCardProps) {
  const isPositive = trend && trend > 0

  return (
    <Card>
      <CardContent>
        <Typography color="textSecondary" gutterBottom>
          {title}
        </Typography>
        <Typography variant="h5" sx={{ color }}>
          {value}{unit}
        </Typography>
        {trend !== undefined && (
          <Typography variant="caption" sx={{ color: isPositive ? 'error.main' : 'success.main' }}>
            {isPositive ? '↑' : '↓'} {Math.abs(trend).toFixed(2)}%
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}

export function MetricsDashboard() {
  const [timeRange, setTimeRange] = useState<'hour' | 'day' | 'week'>('hour')
  const {
    realtimeMetrics,
    isLoading,
    error,
    refreshAll,
  } = useMonitoringData({
    pollingIntervalMs: 5000,
    enableAutoUpdate: true,
  })

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography color="error">{error}</Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* 标题与控制 */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5" fontWeight="bold">
          实时指标监控
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <ToggleButtonGroup
            value={timeRange}
            exclusive
            onChange={(_, value) => {
              if (value) setTimeRange(value)
            }}
            size="small"
          >
            <ToggleButton value="hour">1小时</ToggleButton>
            <ToggleButton value="day">1天</ToggleButton>
            <ToggleButton value="week">1周</ToggleButton>
          </ToggleButtonGroup>
          <Button
            variant="outlined"
            size="small"
            startIcon={isLoading ? <CircularProgress size={20} /> : <RefreshIcon />}
            onClick={refreshAll}
            disabled={isLoading}
          >
            刷新
          </Button>
        </Box>
      </Box>

      {/* 关键指标卡片 */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="CPU 使用率"
            value={(realtimeMetrics?.cpuUsage || 0).toFixed(1)}
            unit="%"
            color={
              (realtimeMetrics?.cpuUsage || 0) > 80
                ? '#d32f2f'
                : (realtimeMetrics?.cpuUsage || 0) > 50
                  ? '#f57c00'
                  : '#4caf50'
            }
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="内存使用率"
            value={(realtimeMetrics?.memoryUsage || 0).toFixed(1)}
            unit="%"
            color={
              (realtimeMetrics?.memoryUsage || 0) > 80
                ? '#d32f2f'
                : (realtimeMetrics?.memoryUsage || 0) > 50
                  ? '#f57c00'
                  : '#4caf50'
            }
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="平均响应时间"
            value={(realtimeMetrics?.responseTime || 0).toFixed(0)}
            unit="ms"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="错误率"
            value={(realtimeMetrics?.errorRate || 0).toFixed(2)}
            unit="%"
            color={
              (realtimeMetrics?.errorRate || 0) > 5
                ? '#d32f2f'
                : (realtimeMetrics?.errorRate || 0) > 1
                  ? '#f57c00'
                  : '#4caf50'
            }
          />
        </Grid>
      </Grid>

      {/* 详细指标 */}
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                网络指标
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">入站流量</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {(realtimeMetrics?.networkIn || 0).toFixed(1)} Mbps
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">出站流量</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {(realtimeMetrics?.networkOut || 0).toFixed(1)} Mbps
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">请求/秒</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {(realtimeMetrics?.requestsPerSecond || 0).toFixed(0)} req/s
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                缓存和队列
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">缓存命中率</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {(realtimeMetrics?.cacheHitRate || 0).toFixed(1)} %
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">队列深度</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {(realtimeMetrics?.queueDepth || 0).toFixed(0)}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">活跃连接</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {(realtimeMetrics?.activeConnections || 0).toFixed(0)}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
