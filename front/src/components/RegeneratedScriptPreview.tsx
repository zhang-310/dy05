/**
 * 重新生成话术预览组件
 * W-05: 内容对比、差异高亮、预期改进
 * @author Claude Code
 * @since 2026-03-06
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Button,
  Stack,
  Chip,
  Alert,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import CompareIcon from '@mui/icons-material/Compare';
import type { RegeneratedScriptVO } from '@/types/optimization';

interface RegeneratedScriptPreviewProps {
  regenerated: RegeneratedScriptVO | null;
  onApply?: (regeneratedId: number) => Promise<RegeneratedScriptVO | void>;
  onReject?: (regeneratedId: number, reason?: string) => Promise<void>;
  isLoading?: boolean;
}

/**
 * 差异高亮文本
 */
function DifferenceHighlight({ original, modified }: { original: string; modified: string }) {
  // 简单的差异展示：展示原文和新文
  return (
    <Box sx={{ display: 'flex', gap: 2 }}>
      {/* 原文 */}
      <Box sx={{ flex: 1 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#f44336', mb: 1 }}>
          原文本
        </Typography>
        <Paper sx={{ p: 2, backgroundColor: '#ffebee', borderLeft: '4px solid #f44336' }}>
          <Typography
            variant="body2"
            sx={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              lineHeight: 1.6,
              color: '#c62828',
            }}
          >
            {original}
          </Typography>
        </Paper>
      </Box>

      {/* 新文 */}
      <Box sx={{ flex: 1 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#4caf50', mb: 1 }}>
          重生成文本
        </Typography>
        <Paper sx={{ p: 2, backgroundColor: '#e8f5e9', borderLeft: '4px solid #4caf50' }}>
          <Typography
            variant="body2"
            sx={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              lineHeight: 1.6,
              color: '#2e7d32',
            }}
          >
            {modified}
          </Typography>
        </Paper>
      </Box>
    </Box>
  );
}

export const RegeneratedScriptPreview: React.FC<RegeneratedScriptPreviewProps> = ({
  regenerated,
  onApply,
  onReject,
  isLoading = false,
}) => {
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [actionInProgress, setActionInProgress] = useState(false);

  if (!regenerated) {
    return (
      <Alert severity="info">暂无重生成的话术版本</Alert>
    );
  }

  const handleApply = async () => {
    if (!onApply) return;
    setActionInProgress(true);
    try {
      await onApply(regenerated.id);
    } finally {
      setActionInProgress(false);
    }
  };

  const handleReject = async () => {
    if (!onReject) return;
    setActionInProgress(true);
    try {
      await onReject(regenerated.id, rejectReason);
      setDetailDialogOpen(false);
      setRejectReason('');
    } finally {
      setActionInProgress(false);
    }
  };

  const getSimilarityStatus = (similarity: number): 'excellent' | 'good' | 'fair' | 'poor' => {
    if (similarity >= 80) return 'excellent';
    if (similarity >= 60) return 'good';
    if (similarity >= 40) return 'fair';
    return 'poor';
  };

  const statusColor = {
    excellent: { bg: '#e8f5e9', text: '#2e7d32' },
    good: { bg: '#e3f2fd', text: '#1565c0' },
    fair: { bg: '#fff3e0', text: '#e65100' },
    poor: { bg: '#ffebee', text: '#c62828' },
  };

  const similarity = regenerated.contentDifference.similarityScore;
  const similarityStatus = getSimilarityStatus(similarity);

  return (
    <Box>
      {/* 预览卡片 */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            {/* 左侧：元数据 */}
            <Grid item xs={12} md={5}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                    应用的建议数
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 'bold', color: '#2196f3' }}>
                    {regenerated.appliedSuggestionCount}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                    预期改进分数
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="h5" sx={{ fontWeight: 'bold', color: '#4caf50' }}>
                      +{regenerated.estimatedImprovementScore.toFixed(1)}
                    </Typography>
                    <Chip label="分" size="small" />
                  </Box>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                    内容相似度
                  </Typography>
                  <Box
                    sx={{
                      p: 1.5,
                      backgroundColor: statusColor[similarityStatus].bg,
                      borderRadius: 1,
                    }}
                  >
                    <Typography
                      variant="body2"
                      sx={{ color: statusColor[similarityStatus].text, fontWeight: 'bold' }}
                    >
                      {similarity.toFixed(1)}%
                    </Typography>
                  </Box>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ color: '#999', mb: 0.5 }}>
                    审批状态
                  </Typography>
                  <Chip
                    label={
                      regenerated.approvalStatus === 'pending'
                        ? '待审批'
                        : regenerated.approvalStatus === 'approved'
                          ? '已批准'
                          : '已拒绝'
                    }
                    color={
                      regenerated.approvalStatus === 'pending'
                        ? 'warning'
                        : regenerated.approvalStatus === 'approved'
                          ? 'success'
                          : 'error'
                    }
                  />
                </Box>
              </Stack>
            </Grid>

            {/* 右侧：差异摘要 */}
            <Grid item xs={12} md={7}>
              <Box>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  改动摘要
                </Typography>
                <Paper sx={{ p: 2, backgroundColor: '#f9f9f9' }}>
                  <Typography variant="body2" sx={{ lineHeight: 1.6, color: '#666' }}>
                    {regenerated.contentDifference.differenceSummary}
                  </Typography>
                </Paper>
              </Box>
            </Grid>
          </Grid>

          {/* 操作按钮 */}
          {regenerated.approvalStatus === 'pending' && (
            <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
              <Button
                variant="contained"
                color="success"
                startIcon={<CheckIcon />}
                onClick={handleApply}
                disabled={actionInProgress || isLoading}
                fullWidth
              >
                应用新版本
              </Button>
              <Button
                variant="outlined"
                color="error"
                startIcon={<CloseIcon />}
                onClick={() => setDetailDialogOpen(true)}
                disabled={actionInProgress || isLoading}
                fullWidth
              >
                拒绝
              </Button>
              <Button
                variant="outlined"
                startIcon={<CompareIcon />}
                onClick={() => setDetailDialogOpen(true)}
                disabled={isLoading}
                fullWidth
              >
                详细对比
              </Button>
            </Stack>
          )}
        </CardContent>
      </Card>

      {/* 详细对比对话框 */}
      <Dialog open={detailDialogOpen} onClose={() => setDetailDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>内容对比详情</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Stack spacing={2}>
            {/* 差异展示 */}
            <DifferenceHighlight
              original={regenerated.contentDifference.originalContent}
              modified={regenerated.contentDifference.regeneratedContent}
            />

            {/* 具体改变 */}
            {regenerated.contentDifference.changes.length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  具体改变 ({regenerated.contentDifference.changes.length} 项)
                </Typography>
                <Stack spacing={1}>
                  {regenerated.contentDifference.changes.map((change, idx) => (
                    <Paper key={idx} sx={{ p: 1.5, backgroundColor: '#f5f5f5' }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Chip
                          label={
                            change.type === 'insertion'
                              ? '新增'
                              : change.type === 'deletion'
                                ? '删除'
                                : '修改'
                          }
                          size="small"
                          color={
                            change.type === 'insertion'
                              ? 'success'
                              : change.type === 'deletion'
                                ? 'error'
                                : 'warning'
                          }
                        />
                        <Typography variant="caption" sx={{ color: '#999' }}>
                          位置: {change.position}
                        </Typography>
                      </Box>
                      <Typography variant="body2" sx={{ color: '#666', mb: 0.5 }}>
                        <strong>原:</strong> "{change.original}"
                      </Typography>
                      <Typography variant="body2" sx={{ color: '#666', mb: 0.5 }}>
                        <strong>新:</strong> "{change.modified}"
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#999' }}>
                        {change.reason}
                      </Typography>
                    </Paper>
                  ))}
                </Stack>
              </Box>
            )}

            {/* 拒绝理由输入框 */}
            <TextField
              label="拒绝理由（可选）"
              multiline
              rows={3}
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              fullWidth
              placeholder="如果拒绝，请说明原因"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>关闭</Button>
          {regenerated.approvalStatus === 'pending' && onReject && (
            <Button
              onClick={handleReject}
              color="error"
              disabled={actionInProgress}
            >
              拒绝
            </Button>
          )}
          {regenerated.approvalStatus === 'pending' && onApply && (
            <Button
              onClick={handleApply}
              color="success"
              variant="contained"
              disabled={actionInProgress}
            >
              应用
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
};
