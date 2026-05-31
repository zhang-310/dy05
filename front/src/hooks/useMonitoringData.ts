/**
 * useMonitoringData Hook
 * W-08: 实时指标监控、异常告警、系统健康状态、WebSocket 推送
 */

import { useState, useCallback, useEffect, useRef } from 'react';
import * as monitoringApi from '@/api/monitoring';
import { getErrorMessage } from '@/utils/errorHandler';
import type {
  RealtimeMetrics,
  AnomalyAlert,
  PerformanceTrendData,
  SystemHealthStatus,
} from '@/types/monitoring';

interface UseMonitoringDataOptions {
  pollingIntervalMs?: number; // 轮询间隔 (ms)
  enableWebSocket?: boolean; // 启用 WebSocket 推送
  enableAutoUpdate?: boolean; // 启用自动更新
}

interface UseMonitoringDataState {
  realtimeMetrics: RealtimeMetrics | null;
  activeAlerts: AnomalyAlert[];
  healthStatus: SystemHealthStatus | null;
  performanceTrends: Map<string, PerformanceTrendData>;
  isLoading: boolean;
  error: string | null;
  requestErrors: Record<string, string>;
  streamStatus: 'disabled' | 'connecting' | 'connected' | 'closed';
  lastUpdatedAt: string | null;
  alertsCount: {
    info: number;
    low: number;
    medium: number;
    high: number;
    critical: number;
  };
}

/**
 * Hook: 监控数据管理（实时指标、告警、系统健康）
 */
export function useMonitoringData(options: UseMonitoringDataOptions = {}) {
  const {
    pollingIntervalMs = 5000, // 5 秒轮询一次
    enableWebSocket = true,
    enableAutoUpdate = true,
  } = options;

  const [state, setState] = useState<UseMonitoringDataState>({
    realtimeMetrics: null,
    activeAlerts: [],
    healthStatus: null,
    performanceTrends: new Map(),
    isLoading: false,
    error: null,
    requestErrors: {},
    streamStatus: enableWebSocket ? 'connecting' : 'disabled',
    lastUpdatedAt: null,
    alertsCount: { info: 0, low: 0, medium: 0, high: 0, critical: 0 },
  });

  const pollingTimerRef = useRef<ReturnType<typeof setTimeout>>();
  const wsRef = useRef<EventSource | null>(null);
  const requestErrorsRef = useRef<Record<string, string>>({});

  const buildErrorMessage = useCallback(() => {
    const messages = Object.values(requestErrorsRef.current);
    return messages.length > 0 ? messages.join('；') : null;
  }, []);

  const setRequestError = useCallback((label: string, err: unknown) => {
    requestErrorsRef.current[label] = `${label}失败：${getErrorMessage(err)}`;
    setState((prev) => ({
      ...prev,
      error: buildErrorMessage(),
      requestErrors: { ...requestErrorsRef.current },
    }));
  }, [buildErrorMessage]);

  const clearRequestError = useCallback((label: string) => {
    delete requestErrorsRef.current[label];
    setState((prev) => ({
      ...prev,
      error: buildErrorMessage(),
      requestErrors: { ...requestErrorsRef.current },
    }));
  }, [buildErrorMessage]);

  /**
   * 获取实时指标
   */
  const fetchRealtimeMetrics = useCallback(async () => {
    try {
      const metrics = (await monitoringApi.getRealtimeMetrics()) as RealtimeMetrics;
      clearRequestError('实时指标加载');
      setState((prev) => ({
        ...prev,
        realtimeMetrics: metrics,
        lastUpdatedAt: new Date().toISOString(),
      }));
    } catch (err) {
      console.error('Failed to fetch realtime metrics:', err);
      setRequestError('实时指标加载', err);
    }
  }, [clearRequestError, setRequestError]);

  /**
   * 获取活跃告警
   */
  const fetchActiveAlerts = useCallback(async () => {
    try {
      const response = (await monitoringApi.getActiveAlerts(undefined, 100)) as { alerts: AnomalyAlert[]; total: number };
      const alertsCount = { info: 0, low: 0, medium: 0, high: 0, critical: 0 };
      const alerts = Array.isArray(response.alerts) ? response.alerts : [];

      alerts.forEach((alert: AnomalyAlert) => {
        const severity = alert.severity as keyof typeof alertsCount;
        if (severity in alertsCount) {
          alertsCount[severity]++;
        }
      });

      clearRequestError('活跃告警加载');
      setState((prev) => ({
        ...prev,
        activeAlerts: alerts,
        alertsCount,
      }));
    } catch (err) {
      console.error('Failed to fetch active alerts:', err);
      setRequestError('活跃告警加载', err);
    }
  }, [clearRequestError, setRequestError]);

  /**
   * 获取系统健康状态
   */
  const fetchHealthStatus = useCallback(async () => {
    try {
      const health = (await monitoringApi.getHealthStatus()) as SystemHealthStatus;
      clearRequestError('健康状态加载');
      setState((prev) => ({
        ...prev,
        healthStatus: health,
      }));
    } catch (err) {
      console.error('Failed to fetch health status:', err);
      setRequestError('健康状态加载', err);
    }
  }, [clearRequestError, setRequestError]);

  /**
   * 获取性能趋势数据
   */
  const fetchPerformanceTrend = useCallback(
    async (metricName: string, timeRange: 'hour' | 'day' | 'week' = 'hour') => {
      try {
        const errorLabel = `${metricName}趋势加载`;
        const trend = (await monitoringApi.getPerformanceTrend(
          metricName,
          timeRange,
          60
        )) as PerformanceTrendData;
        clearRequestError(errorLabel);
        setState((prev) => {
          const newTrends = new Map(prev.performanceTrends);
          newTrends.set(metricName, trend);
          return { ...prev, performanceTrends: newTrends };
        });
      } catch (err) {
        console.error(`Failed to fetch trend for ${metricName}:`, err);
        setRequestError(`${metricName}趋势加载`, err);
      }
    },
    [clearRequestError, setRequestError]
  );

  /**
   * 刷新所有监控数据
   */
  const refreshAll = useCallback(async () => {
    requestErrorsRef.current = {};
    setState((prev) => ({ ...prev, isLoading: true, error: null, requestErrors: {} }));
    try {
      await Promise.all([
        fetchRealtimeMetrics(),
        fetchActiveAlerts(),
        fetchHealthStatus(),
      ]);
    } finally {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, [fetchRealtimeMetrics, fetchActiveAlerts, fetchHealthStatus]);

  /**
   * 确认告警
   */
  const acknowledgeAlert = useCallback(async (alertId: number, notes?: string) => {
    try {
      await monitoringApi.acknowledgeAlert(alertId, notes);
      // 重新加载告警列表
      await fetchActiveAlerts();
    } catch (err) {
      console.error('Failed to acknowledge alert:', err);
      setState((prev) => ({
        ...prev,
        error: '确认告警失败',
      }));
    }
  }, [fetchActiveAlerts]);

  /**
   * 解决告警
   */
  const resolveAlert = useCallback(async (alertId: number, resolution?: string) => {
    try {
      await monitoringApi.resolveAlert(alertId, resolution);
      // 重新加载告警列表
      await fetchActiveAlerts();
    } catch (err) {
      console.error('Failed to resolve alert:', err);
      setState((prev) => ({
        ...prev,
        error: '解决告警失败',
      }));
    }
  }, [fetchActiveAlerts]);

  /**
   * 初始化：加载初始数据
   */
  useEffect(() => {
    if (!enableAutoUpdate) return;

    refreshAll();
  }, [enableAutoUpdate, refreshAll]);

  /**
   * 轮询更新
   */
  useEffect(() => {
    if (!enableAutoUpdate) return;

    pollingTimerRef.current = setInterval(() => {
      refreshAll();
    }, pollingIntervalMs);

    return () => {
      if (pollingTimerRef.current) {
        clearInterval(pollingTimerRef.current);
      }
    };
  }, [enableAutoUpdate, pollingIntervalMs, refreshAll]);

  /**
   * WebSocket 实时推送（可选）
   */
  useEffect(() => {
    if (!enableWebSocket) {
      setState((prev) => ({ ...prev, streamStatus: 'disabled' }));
      return;
    }

    setState((prev) => ({ ...prev, streamStatus: 'connecting' }));

    const eventSource = monitoringApi.subscribeToRealtimeMetrics();

    eventSource.addEventListener('metrics', (event) => {
      try {
        const metrics = JSON.parse(event.data) as RealtimeMetrics;
        clearRequestError('实时推送连接');
        setState((prev) => ({
          ...prev,
          realtimeMetrics: metrics,
          streamStatus: 'connected',
          lastUpdatedAt: new Date().toISOString(),
        }));
      } catch (err) {
        console.error('Failed to parse metrics:', err);
      }
    });

    eventSource.addEventListener('alert', (event) => {
      try {
        const alert = JSON.parse(event.data) as AnomalyAlert;
        setState((prev) => {
          const newAlerts = [alert, ...prev.activeAlerts];
          const newCounts = { ...prev.alertsCount };
          if (alert.severity in newCounts) {
            newCounts[alert.severity]++;
          }
          delete requestErrorsRef.current['实时推送连接'];
          return {
            ...prev,
            activeAlerts: newAlerts.slice(0, 100), // 保持最多 100 条
            alertsCount: newCounts,
            streamStatus: 'connected',
            error: buildErrorMessage(),
            requestErrors: { ...requestErrorsRef.current },
          };
        });
      } catch (err) {
        console.error('Failed to parse alert:', err);
      }
    });

    eventSource.addEventListener('health', (event) => {
      try {
        const health = JSON.parse(event.data) as SystemHealthStatus;
        clearRequestError('实时推送连接');
        setState((prev) => ({
          ...prev,
          healthStatus: health,
          streamStatus: 'connected',
        }));
      } catch (err) {
        console.error('Failed to parse health status:', err);
      }
    });

    eventSource.addEventListener('error', () => {
      console.error('WebSocket connection error');
      setRequestError('实时推送连接', new Error('SSE 连接已关闭'));
      setState((prev) => ({ ...prev, streamStatus: 'closed' }));
      eventSource.close();
    });

    wsRef.current = eventSource;

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [buildErrorMessage, clearRequestError, enableWebSocket, setRequestError]);

  /**
   * 清理
   */
  useEffect(() => {
    return () => {
      if (pollingTimerRef.current) {
        clearInterval(pollingTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  return {
    ...state,
    refreshAll,
    fetchRealtimeMetrics,
    fetchActiveAlerts,
    fetchHealthStatus,
    fetchPerformanceTrend,
    acknowledgeAlert,
    resolveAlert,
  };
}
