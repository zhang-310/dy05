/**
 * 搜索分析图表组件
 * W-06: ECharts 可视化、搜索热词、趋势、点击率
 * @author Claude Code
 * @since 2026-03-06
 */

import React, { useMemo, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  CircularProgress,
  Alert,
  ToggleButton,
  ToggleButtonGroup,
  alpha,
  useTheme,
} from '@mui/material';
import type { Theme } from '@mui/material/styles';
import * as echarts from 'echarts';
import type { SearchAnalyticsVO } from '@/types/search';

interface SearchAnalyticsChartProps {
  analytics: SearchAnalyticsVO | null;
  isLoading?: boolean;
  error?: string | null;
  height?: number;
}

type SearchAnalyticsTone = 'primary' | 'secondary' | 'success' | 'warning' | 'error';

function semanticColor(theme: Theme, tone: SearchAnalyticsTone) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main;
}

function surfaceColor(theme: Theme, tone: SearchAnalyticsTone) {
  return alpha(semanticColor(theme, tone), theme.palette.mode === 'dark' ? 0.18 : 0.1);
}

export const SearchAnalyticsChart: React.FC<SearchAnalyticsChartProps> = ({
  analytics,
  isLoading = false,
  error,
  height = 400,
}) => {
  const theme = useTheme();
  const [chartType, setChartType] = useState<'trend' | 'keywords' | 'quality'>('trend');
  const trendChartRef = React.useRef<HTMLDivElement>(null);
  const keywordsChartRef = React.useRef<HTMLDivElement>(null);
  const qualityChartRef = React.useRef<HTMLDivElement>(null);
  const trendChartInstance = React.useRef<echarts.ECharts | null>(null);
  const keywordsChartInstance = React.useRef<echarts.ECharts | null>(null);
  const qualityChartInstance = React.useRef<echarts.ECharts | null>(null);
  const chartColors = useMemo(
    () => ({
      primary: semanticColor(theme, 'primary'),
      secondary: semanticColor(theme, 'secondary'),
      success: semanticColor(theme, 'success'),
      warning: semanticColor(theme, 'warning'),
      error: semanticColor(theme, 'error'),
      axisText: theme.palette.text.secondary,
      splitLine: theme.palette.divider,
      tooltipLabel: alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.22 : 0.16),
    }),
    [theme],
  );

  // 生成趋势图表配置
  const trendChartOption = useMemo(() => {
    if (!analytics || !analytics.searchTrend || analytics.searchTrend.length === 0) {
      return null;
    }

    const dates = analytics.searchTrend.map((point) =>
      new Date(point.timestamp).toLocaleDateString('zh-CN')
    );
    const counts = analytics.searchTrend.map((point) => point.count);

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '10%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: dates,
        boundaryGap: false,
      },
      yAxis: {
        type: 'value',
        name: '搜索次数',
      },
      series: [
        {
          data: counts,
          type: 'line',
          smooth: true,
          itemStyle: {
            color: chartColors.primary,
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {
                offset: 0,
                color: alpha(chartColors.primary, 0.4),
              },
              {
                offset: 1,
                color: alpha(chartColors.primary, 0.1),
              },
            ]),
          },
        },
      ],
    };
  }, [analytics, chartColors.primary]);

  // 生成关键词图表配置
  const keywordsChartOption = useMemo(() => {
    if (!analytics || !analytics.topSearchQueries || analytics.topSearchQueries.length === 0) {
      return null;
    }

    const queries = analytics.topSearchQueries.slice(0, 10);
    const names = queries.map((q) => q.query);
    const counts = queries.map((q) => q.count);

    return {
      tooltip: {
        trigger: 'axis',
        axisLabel: {
          interval: 0,
          rotate: 45,
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '10%',
        top: '10%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: names,
      },
      yAxis: {
        type: 'value',
        name: '搜索次数',
      },
      series: [
        {
          data: counts,
          type: 'bar',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {
                offset: 0,
                color: chartColors.primary,
              },
              {
                offset: 1,
                color: chartColors.secondary,
              },
            ]),
          },
        },
      ],
    };
  }, [analytics, chartColors.primary, chartColors.secondary]);

  // 生成质量分布图表配置
  const qualityChartOption = useMemo(() => {
    if (!analytics) return null;

    const data = [
      {
        value: analytics.resultQualityScores.excellent,
        name: '优秀 (>80分)',
        itemStyle: { color: chartColors.success },
      },
      {
        value: analytics.resultQualityScores.good,
        name: '良好 (60-80分)',
        itemStyle: { color: chartColors.primary },
      },
      {
        value: analytics.resultQualityScores.fair,
        name: '一般 (40-60分)',
        itemStyle: { color: chartColors.warning },
      },
      {
        value: analytics.resultQualityScores.poor,
        name: '差 (<40分)',
        itemStyle: { color: chartColors.error },
      },
    ];

    return {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)',
      },
      legend: {
        left: 'center',
        bottom: 0,
      },
      series: [
        {
          name: '搜索结果质量',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          data,
        },
      ],
    };
  }, [analytics, chartColors.error, chartColors.primary, chartColors.success, chartColors.warning]);

  // 初始化和更新图表
  React.useEffect(() => {
    if (chartType === 'trend' && trendChartRef.current && trendChartOption) {
      if (!trendChartInstance.current) {
        trendChartInstance.current = echarts.init(trendChartRef.current);
      }
      trendChartInstance.current.setOption(trendChartOption);
    }
  }, [chartType, trendChartOption]);

  React.useEffect(() => {
    if (chartType === 'keywords' && keywordsChartRef.current && keywordsChartOption) {
      if (!keywordsChartInstance.current) {
        keywordsChartInstance.current = echarts.init(keywordsChartRef.current);
      }
      keywordsChartInstance.current.setOption(keywordsChartOption);
    }
  }, [chartType, keywordsChartOption]);

  React.useEffect(() => {
    if (chartType === 'quality' && qualityChartRef.current && qualityChartOption) {
      if (!qualityChartInstance.current) {
        qualityChartInstance.current = echarts.init(qualityChartRef.current);
      }
      qualityChartInstance.current.setOption(qualityChartOption);
    }
  }, [chartType, qualityChartOption]);

  // 处理窗口大小变化
  React.useEffect(() => {
    const handleResize = () => {
      trendChartInstance.current?.resize();
      keywordsChartInstance.current?.resize();
      qualityChartInstance.current?.resize();
    };
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

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

  if (!analytics) {
    return (
      <Card>
        <CardContent sx={{ height }}>
          <Alert severity="info">暂无分析数据</Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              搜索分析
            </Typography>
            <ToggleButtonGroup
              value={chartType}
              exclusive
              onChange={(_, newChartType) => {
                if (newChartType !== null) {
                  setChartType(newChartType);
                }
              }}
              size="small"
            >
              <ToggleButton value="trend">趋势</ToggleButton>
              <ToggleButton value="keywords">热词</ToggleButton>
              <ToggleButton value="quality">质量分布</ToggleButton>
            </ToggleButtonGroup>
          </Box>

          {/* 统计信息 */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={6} sm={3}>
              <Box data-testid="search-analytics-stat-surface" data-stat-tone="primary" data-stat-color={chartColors.primary} sx={{ p: 1.5, backgroundColor: surfaceColor(theme, 'primary'), border: `1px solid ${alpha(chartColors.primary, 0.3)}`, borderRadius: 1 }}>
                <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                  总搜索次
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: chartColors.primary }}>
                  {analytics.totalSearches}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Box data-testid="search-analytics-stat-surface" data-stat-tone="success" data-stat-color={chartColors.success} sx={{ p: 1.5, backgroundColor: surfaceColor(theme, 'success'), border: `1px solid ${alpha(chartColors.success, 0.3)}`, borderRadius: 1 }}>
                <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                  独立用户
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: chartColors.success }}>
                  {analytics.uniqueUsers}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Box data-testid="search-analytics-stat-surface" data-stat-tone="warning" data-stat-color={chartColors.warning} sx={{ p: 1.5, backgroundColor: surfaceColor(theme, 'warning'), border: `1px solid ${alpha(chartColors.warning, 0.3)}`, borderRadius: 1 }}>
                <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                  平均点击率
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: chartColors.warning }}>
                  {analytics.averageClickThroughRate.toFixed(1)}%
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Box data-testid="search-analytics-stat-surface" data-stat-tone="secondary" data-stat-color={chartColors.secondary} sx={{ p: 1.5, backgroundColor: surfaceColor(theme, 'secondary'), border: `1px solid ${alpha(chartColors.secondary, 0.3)}`, borderRadius: 1 }}>
                <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                  平均结果数
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: chartColors.secondary }}>
                  {analytics.averageResultsReturned.toFixed(0)}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Box>

        {/* 图表区域 */}
        {chartType === 'trend' && (
          <Box
            ref={trendChartRef}
            data-testid="search-analytics-trend-chart-surface"
            data-chart-color={chartColors.primary}
            data-chart-area-colors={`${alpha(chartColors.primary, 0.4)}|${alpha(chartColors.primary, 0.1)}`}
            sx={{
              width: '100%',
              height: `${height}px`,
            }}
          />
        )}
        {chartType === 'keywords' && (
          <Box
            ref={keywordsChartRef}
            data-testid="search-analytics-keywords-chart-surface"
            data-chart-colors={`${chartColors.primary}|${chartColors.secondary}`}
            sx={{
              width: '100%',
              height: `${height}px`,
            }}
          />
        )}
        {chartType === 'quality' && (
          <Box
            ref={qualityChartRef}
            data-testid="search-analytics-quality-chart-surface"
            data-chart-colors={`${chartColors.success}|${chartColors.primary}|${chartColors.warning}|${chartColors.error}`}
            sx={{
              width: '100%',
              height: `${height}px`,
            }}
          />
        )}
      </CardContent>
    </Card>
  );
};
