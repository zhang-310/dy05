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
} from '@mui/material';
import * as echarts from 'echarts';
import type { SearchAnalyticsVO } from '@/types/search';

interface SearchAnalyticsChartProps {
  analytics: SearchAnalyticsVO | null;
  isLoading?: boolean;
  error?: string | null;
  height?: number;
}

export const SearchAnalyticsChart: React.FC<SearchAnalyticsChartProps> = ({
  analytics,
  isLoading = false,
  error,
  height = 400,
}) => {
  const [chartType, setChartType] = useState<'trend' | 'keywords' | 'quality'>('trend');
  const trendChartRef = React.useRef<HTMLDivElement>(null);
  const keywordsChartRef = React.useRef<HTMLDivElement>(null);
  const qualityChartRef = React.useRef<HTMLDivElement>(null);
  const trendChartInstance = React.useRef<echarts.ECharts | null>(null);
  const keywordsChartInstance = React.useRef<echarts.ECharts | null>(null);
  const qualityChartInstance = React.useRef<echarts.ECharts | null>(null);

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
      ],
    };
  }, [analytics]);

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
                color: '#667eea',
              },
              {
                offset: 1,
                color: '#764ba2',
              },
            ]),
          },
        },
      ],
    };
  }, [analytics]);

  // 生成质量分布图表配置
  const qualityChartOption = useMemo(() => {
    if (!analytics) return null;

    const data = [
      {
        value: analytics.resultQualityScores.excellent,
        name: '优秀 (>80分)',
        itemStyle: { color: '#4caf50' },
      },
      {
        value: analytics.resultQualityScores.good,
        name: '良好 (60-80分)',
        itemStyle: { color: '#2196f3' },
      },
      {
        value: analytics.resultQualityScores.fair,
        name: '一般 (40-60分)',
        itemStyle: { color: '#ff9800' },
      },
      {
        value: analytics.resultQualityScores.poor,
        name: '差 (<40分)',
        itemStyle: { color: '#f44336' },
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
  }, [analytics]);

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
              <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
                <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                  总搜索次
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#667eea' }}>
                  {analytics.totalSearches}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
                <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                  独立用户
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#4caf50' }}>
                  {analytics.uniqueUsers}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
                <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                  平均点击率
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#ff9800' }}>
                  {analytics.averageClickThroughRate.toFixed(1)}%
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Box sx={{ p: 1.5, backgroundColor: '#f0f0f0', borderRadius: 1 }}>
                <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                  平均结果数
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: '#764ba2' }}>
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
            sx={{
              width: '100%',
              height: `${height}px`,
            }}
          />
        )}
        {chartType === 'keywords' && (
          <Box
            ref={keywordsChartRef}
            sx={{
              width: '100%',
              height: `${height}px`,
            }}
          />
        )}
        {chartType === 'quality' && (
          <Box
            ref={qualityChartRef}
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
