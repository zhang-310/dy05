/**
 * AnomalyAlerts 组件
 * W-08: 异常告警列表，包含 Isolation Forest 异常检测结果
 */

import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  IconButton,
  Tooltip,
  Alert,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import React, { useState } from 'react';
import type { AnomalyAlert } from '@/types/monitoring';
import * as monitoringApi from '@/api/monitoring';

interface AnomalyAlertsProps {
  alerts: AnomalyAlert[];
}

/**
 * 获取告警严重级别的颜色和图标
 */
const getSeverityInfo = (
  severity: string
): {
  color: 'error' | 'warning' | 'info' | 'success';
  icon: React.ReactElement;
  label: string;
} => {
  const map: Record<string, { color: 'error' | 'warning' | 'info' | 'success'; icon: React.ReactElement; label: string }> = {
    critical: { color: 'error', icon: <ErrorIcon />, label: '严重' },
    high: { color: 'warning', icon: <WarningIcon />, label: '高' },
    medium: { color: 'info', icon: <WarningIcon />, label: '中' },
    low: { color: 'success', icon: <CheckCircleIcon />, label: '低' },
  };
  return map[severity] ?? { color: 'info', icon: <CheckCircleIcon />, label: severity };
};

/**
 * 获取告警状态的标签和颜色
 */
const getStatusInfo = (
  status: string
): { color: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'; label: string } => {
  const map: Record<string, any> = {
    active: { color: 'error', label: '活跃' },
    acknowledged: { color: 'warning', label: '已确认' },
    resolved: { color: 'success', label: '已解决' },
  };
  return map[status] || { color: 'default', label: status };
};

/**
 * 告警操作对话框
 */
interface AlertActionDialogProps {
  open: boolean;
  alert: AnomalyAlert | null;
  action: 'acknowledge' | 'resolve' | null;
  onClose: () => void;
  onSubmit: (notes: string) => Promise<void>;
}

function AlertActionDialog({
  open,
  alert,
  action,
  onClose,
  onSubmit,
}: AlertActionDialogProps) {
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      await onSubmit(notes);
      setNotes('');
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const dialogTitle =
    action === 'acknowledge' ? '确认告警' : action === 'resolve' ? '解决告警' : '';

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{dialogTitle}</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        {alert && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="body2" color="textSecondary" sx={{ mb: 0.5 }}>
              告警：{alert.message}
            </Typography>
            <Typography variant="caption" color="textSecondary">
              {alert.metric} = {alert.currentValue?.toFixed(2)}（阈值：
              {alert.threshold?.toFixed(2)}）
            </Typography>
          </Box>
        )}

        <TextField
          fullWidth
          multiline
          rows={3}
          placeholder={
            action === 'acknowledge'
              ? '输入确认备注信息...'
              : '输入解决方案备注...'
          }
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          variant="outlined"
          size="small"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          取消
        </Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={loading}
          color={action === 'acknowledge' ? 'warning' : 'success'}
        >
          {action === 'acknowledge' ? '确认告警' : '标记为已解决'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/**
 * 异常告警列表
 */
export function AnomalyAlerts({ alerts }: AnomalyAlertsProps) {
  const [selectedAlert, setSelectedAlert] = useState<AnomalyAlert | null>(null);
  const [actionType, setActionType] = useState<'acknowledge' | 'resolve' | null>(
    null
  );
  const [dialogOpen, setDialogOpen] = useState(false);

  /**
   * 打开告警操作对话框
   */
  const handleOpenActionDialog = (
    alert: AnomalyAlert,
    action: 'acknowledge' | 'resolve'
  ) => {
    setSelectedAlert(alert);
    setActionType(action);
    setDialogOpen(true);
  };

  /**
   * 处理告警操作
   */
  const handleAlertAction = async (notes: string) => {
    if (!selectedAlert || !actionType) return;

    try {
      if (actionType === 'acknowledge') {
        await monitoringApi.acknowledgeAlert(selectedAlert.alertId || 0, notes);
      } else if (actionType === 'resolve') {
        await monitoringApi.resolveAlert(selectedAlert.alertId || 0, notes);
      }
      // 实际应该重新刷新列表，这里由父组件处理
    } catch (err) {
      console.error('Failed to perform alert action:', err);
    }
  };

  if (!alerts || alerts.length === 0) {
    return (
      <Paper sx={{ p: 3, textAlign: 'center' }}>
        <Typography color="textSecondary">暂无活跃告警 ✅</Typography>
      </Paper>
    );
  }

  return (
    <>
      <Paper sx={{ overflow: 'hidden' }}>
        {/* 告警统计 */}
        <Box sx={{ p: 2, backgroundColor: '#f5f5f5' }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
            活跃告警列表 ({alerts.length})
          </Typography>
          <Alert severity="warning" sx={{ fontSize: '0.875rem' }}>
            Isolation Forest 异常检测：自动识别统计异常值（异常分数 &gt;
            0.8）
          </Alert>
        </Box>

        {/* 告警表格 */}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                <TableCell align="center" width="80px">
                  严重级别
                </TableCell>
                <TableCell>告警类型</TableCell>
                <TableCell>指标名称</TableCell>
                <TableCell align="right" width="120px">
                  指标值
                </TableCell>
                <TableCell align="right" width="100px">
                  持续时间
                </TableCell>
                <TableCell width="80px">状态</TableCell>
                <TableCell align="center" width="80px">
                  操作
                </TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {alerts.map((alert) => {
                const severityInfo = getSeverityInfo(alert.severity);
                const statusInfo = getStatusInfo(alert.status);

                return (
                  <TableRow
                    key={alert.alertId}
                    sx={{
                      '&:hover': { backgroundColor: '#fafafa' },
                      borderLeft:
                        alert.severity === 'critical'
                          ? '4px solid #f44336'
                          : 'none',
                    }}
                  >
                    {/* 严重级别 */}
                    <TableCell align="center">
                      <Chip
                        label={severityInfo.label}
                        color={severityInfo.color}
                        size="small"
                        icon={severityInfo.icon}
                      />
                    </TableCell>

                    {/* 告警类型 */}
                    <TableCell>
                      <Typography variant="body2">
                        {alert.alertType === 'metric_threshold'
                          ? '指标告警'
                          : alert.alertType === 'anomaly_detection'
                          ? '异常检测'
                          : alert.alertType === 'health_check'
                          ? '健康检查'
                          : alert.alertType}
                      </Typography>
                    </TableCell>

                    {/* 指标名称 */}
                    <TableCell>
                      <Tooltip title={alert.message}>
                        <Typography
                          variant="body2"
                          sx={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            maxWidth: 200,
                          }}
                        >
                          {alert.metric || alert.ruleName || '未知指标'}
                        </Typography>
                      </Tooltip>
                    </TableCell>

                    {/* 指标值 */}
                    <TableCell align="right">
                      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                        <Typography
                          variant="body2"
                          sx={{
                            fontWeight: 600,
                            color:
                              (alert.currentValue ?? 0) > (alert.threshold ?? 0)
                                ? '#f44336'
                                : '#666',
                          }}
                        >
                          {(alert.currentValue ?? 0).toFixed(2)}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          / {(alert.threshold ?? 0).toFixed(2)}
                        </Typography>
                      </Box>
                    </TableCell>

                    {/* 告警持续时间 */}
                    <TableCell align="right">
                      <Typography variant="body2">
                        {alert.lastDetectedAt && alert.firstDetectedAt
                          ? new Date(alert.lastDetectedAt).getTime() -
                            new Date(alert.firstDetectedAt).getTime() > 3600000
                            ? '&gt; 1小时'
                            : '&lt; 1小时'
                          : '-'}
                      </Typography>
                    </TableCell>

                    {/* 状态 */}
                    <TableCell>
                      <Chip
                        label={statusInfo.label}
                        color={statusInfo.color}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>

                    {/* 操作 */}
                    <TableCell align="center">
                      {alert.status === 'active' ? (
                        <Tooltip title="查看详情">
                          <IconButton
                            size="small"
                            onClick={() =>
                              handleOpenActionDialog(alert, 'acknowledge')
                            }
                          >
                            <MoreVertIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      ) : alert.status === 'acknowledged' ? (
                        <Button
                          size="small"
                          onClick={() =>
                            handleOpenActionDialog(alert, 'resolve')
                          }
                          sx={{ color: '#4caf50' }}
                        >
                          解决
                        </Button>
                      ) : (
                        <Typography variant="caption" color="textSecondary">
                          已关闭
                        </Typography>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* 告警操作对话框 */}
      <AlertActionDialog
        open={dialogOpen}
        alert={selectedAlert}
        action={actionType}
        onClose={() => {
          setDialogOpen(false);
          setSelectedAlert(null);
          setActionType(null);
        }}
        onSubmit={handleAlertAction}
      />
    </>
  );
}
