/**
 * 优化历史趋势图表组件
 * W-05: ECharts 折线图、进化历史展示
 * @author Claude Code
 * @since 2026-03-06
 */

import React, { useMemo } from 'react';
import { Box, Card, CardContent, Typography, CircularProgress, Alert } from '@mui/material';
import * as echarts from 'echarts';
import type { EvolutionMetricsVO } from '@/types/optimization';

interface OptimizationHistoryChartProps {
  metrics: EvolutionMetricsVO | null;
  isLoading?: boolean;
  error?: string | null;
  height?: number;
}

export const OptimizationHistoryChart: React.FC<OptimizationHistoryChartProps> = ({
  metrics,
  isLoading = false,
  error,
  height = 400,
}) => {
  const chartRef = React.useRef<HTMLDivElement>(null);
  const chartInstance = React.useRef<echarts.ECharts | null>(null);

  // 生成图表配置
  const chartOption = useMemo(() => {
    if (!metrics || !metrics.evolutionTrend || metrics.evolutionTrend.length === 0) {
      return null;
    }

    const dates = metrics.evolutionTrend.map((point) =>
      new Date(point.timestamp).toLocaleDateString('zh-CN')
    );
    const scores = metrics.evolutionTrend.map((point) => point.overallScore);
    const applicatedCounts = metrics.evolutionTrend.map((point) => point.suggestionsApplied);
    const improvements = metrics.evolutionTrend.map((point) => point.averageImprovement);

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#6a7985',
          },
        },
      },
      legend: {
        data: ['整体评分', '应用建议数', '平均改进'],
        top: 20,
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '15%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: true,
        data: dates,
      },
      yAxis: [
        {
          type: 'value',
          name: '评分',
          position: 'left',
          alignTicks: true,
          axisLine: {
            lineStyle: {
              color: '#667eea',
            },
          },
          splitLine: {
            show: true,
            lineStyle: {
              color: '#e0e0e0',
            },
          },
        },
        {
          type: 'value',
          name: '改进',
          position: 'right',
          axisLine: {
            lineStyle: {
              color: '#764ba2',
            },
          },
        },
      ],
      series: [
        {
          name: '整体评分',
          data: scores,
          type: 'line',
          smooth: true,
          yAxisIndex: 0,
          itemStyle: {
            color: '#667eea',
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {
                offset: 0,
                color: 'rgba(102, 126, 234, 0.4)',
              },
              {
                offset: 1,
                color: 'rgba(102, 126, 234, 0.1)',
              },
            ]),
          },
        },
        {
          name: '应用建议数',
          data: applicatedCounts,
          type: 'bar',
          yAxisIndex: 0,
          itemStyle: {
            color: '#4ecdc4',
            opacity: 0.7,
          },
        },
        {
          name: '平均改进',
          data: improvements,
          type: 'line',
          smooth: true,
          yAxisIndex: 1,
          itemStyle: {
            color: '#764ba2',
          },
        },
      ],
    };
  }, [metrics]);

  // 初始化和更新图表
  React.useEffect(() => {
    if (!chartRef.current || !chartOption) return;

    if (!chartInstance.current) {
      chartInstance.current = echarts.init(chartRef.current, 'light', { useDirtyRect: true });
    }

    chartInstance.current.setOption(chartOption);

    const handleResize = () => {
      chartInstance.current?.resize();
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [chartOption]);

  if (isLoading) {
    return (
      <Card>
        <CardContent sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height }}>
          <CircularProgress />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent sx={{ height }}>
          <Alert severity="error">{error}</Alert>
        </CardContent>
      </Card>
    );
  }

  if (!metrics || !metrics.evolutionTrend || metrics.evolutionTrend.length === 0) {
    return (
      <Card>
        <CardContent sx={{ height }}>
          <Alert severity="info">暂无优化历史数据</Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
          优化进化趋势
        </Typography>
        <Box
          ref={chartRef}
          sx={{
            width: '100%',
            height: `${height}px`,
          }}
        />

        {/* 统计信息 */}
        <Box sx={{ mt: 3, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 2 }}>
          <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
            <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
              总分析次数
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#667eea' }}>
              {metrics.totalAnalysis}
            </Typography>
          </Box>
          <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
            <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
              已应用建议数
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#4caf50' }}>
              {metrics.acceptedSuggestions}
            </Typography>
          </Box>
          <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
            <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
              平均改进
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#764ba2' }}>
              {metrics.averageScoreImprovement.toFixed(2)}
            </Typography>
          </Box>
          <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
            <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
              成功率
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#ff9800' }}>
              {metrics.optimizationSuccessRate.toFixed(1)}%
            </Typography>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};
