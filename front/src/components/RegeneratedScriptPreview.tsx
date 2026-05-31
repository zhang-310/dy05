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
  alpha,
  useTheme,
} from '@mui/material';
import type { Theme } from '@mui/material/styles';
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

type PreviewTone = 'primary' | 'success' | 'warning' | 'error' | 'info';
type SimilarityStatus = 'excellent' | 'good' | 'fair' | 'poor';

function semanticColor(theme: Theme, tone: PreviewTone) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main;
}

function surfaceColor(theme: Theme, tone: PreviewTone) {
  return alpha(semanticColor(theme, tone), theme.palette.mode === 'dark' ? 0.18 : 0.1);
}

function borderColor(theme: Theme, tone: PreviewTone) {
  return alpha(semanticColor(theme, tone), theme.palette.mode === 'dark' ? 0.45 : 0.28);
}

function getSimilarityTone(status: SimilarityStatus): PreviewTone {
  if (status === 'excellent') return 'success';
  if (status === 'good') return 'primary';
  if (status === 'fair') return 'warning';
  return 'error';
}

/**
 * 差异高亮文本
 */
function DifferenceHighlight({ original, modified }: { original: string; modified: string }) {
  const theme = useTheme();
  const originalColor = semanticColor(theme, 'error');
  const modifiedColor = semanticColor(theme, 'success');

  // 简单的差异展示：展示原文和新文
  return (
    <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 2 }}>
      {/* 原文 */}
      <Box sx={{ flex: 1 }}>
        <Typography
          data-testid="regenerated-diff-title-surface"
          data-diff-tone="error"
          data-diff-color={originalColor}
          variant="subtitle2"
          sx={{ fontWeight: 'bold', color: originalColor, mb: 1 }}
        >
          原文本
        </Typography>
        <Paper
          data-testid="regenerated-diff-original-surface"
          data-diff-tone="error"
          data-diff-color={originalColor}
          sx={{
            p: 2,
            backgroundColor: surfaceColor(theme, 'error'),
            borderLeft: `4px solid ${originalColor}`,
          }}
        >
          <Typography
            variant="body2"
            sx={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              lineHeight: 1.6,
              color: originalColor,
            }}
          >
            {original}
          </Typography>
        </Paper>
      </Box>

      {/* 新文 */}
      <Box sx={{ flex: 1 }}>
        <Typography
          data-testid="regenerated-diff-title-surface"
          data-diff-tone="success"
          data-diff-color={modifiedColor}
          variant="subtitle2"
          sx={{ fontWeight: 'bold', color: modifiedColor, mb: 1 }}
        >
          重生成文本
        </Typography>
        <Paper
          data-testid="regenerated-diff-modified-surface"
          data-diff-tone="success"
          data-diff-color={modifiedColor}
          sx={{
            p: 2,
            backgroundColor: surfaceColor(theme, 'success'),
            borderLeft: `4px solid ${modifiedColor}`,
          }}
        >
          <Typography
            variant="body2"
            sx={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              lineHeight: 1.6,
              color: modifiedColor,
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
  const theme = useTheme();
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

  const getSimilarityStatus = (similarity: number): SimilarityStatus => {
    if (similarity >= 80) return 'excellent';
    if (similarity >= 60) return 'good';
    if (similarity >= 40) return 'fair';
    return 'poor';
  };

  const contentDifference = regenerated.contentDifference ?? {
    originalContent: '',
    regeneratedContent: regenerated.regeneratedContent,
    differenceSummary: '后端未返回逐句差异明细，本页展示重新生成内容。',
    changes: [],
    similarityScore: 0,
  };
  const appliedSuggestionCount = regenerated.appliedSuggestionCount ?? (regenerated.suggestionId ? 1 : 0);
  const estimatedImprovementScore = regenerated.estimatedImprovementScore ?? Number(regenerated.aiQualityScore ?? 0);
  const approvalStatus = String(regenerated.approvalStatus ?? 'pending').toLowerCase();
  const similarity = contentDifference.similarityScore;
  const similarityStatus = getSimilarityStatus(similarity);
  const similarityTone = getSimilarityTone(similarityStatus);
  const similarityColor = semanticColor(theme, similarityTone);
  const appliedCountColor = semanticColor(theme, 'primary');
  const improvementColor = semanticColor(theme, 'success');
  const summarySurface = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.07 : 0.035);
  const changeSurface = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.07 : 0.04);

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
                  <Typography variant="subtitle2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                    应用的建议数
                  </Typography>
                  <Typography
                    data-testid="regenerated-applied-count-surface"
                    data-preview-tone="primary"
                    data-preview-color={appliedCountColor}
                    variant="h5"
                    sx={{ fontWeight: 'bold', color: appliedCountColor }}
                  >
                    {appliedSuggestionCount}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                    预期改进分数
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography
                      data-testid="regenerated-improvement-score-surface"
                      data-preview-tone="success"
                      data-preview-color={improvementColor}
                      variant="h5"
                      sx={{ fontWeight: 'bold', color: improvementColor }}
                    >
                      +{estimatedImprovementScore.toFixed(1)}
                    </Typography>
                    <Chip label="分" size="small" />
                  </Box>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                    内容相似度
                  </Typography>
                  <Box
                    data-testid="regenerated-similarity-surface"
                    data-similarity-tone={similarityTone}
                    data-similarity-color={similarityColor}
                    sx={{
                      p: 1.5,
                      backgroundColor: surfaceColor(theme, similarityTone),
                      border: `1px solid ${borderColor(theme, similarityTone)}`,
                      borderRadius: 1,
                    }}
                  >
                    <Typography
                      variant="body2"
                      sx={{ color: similarityColor, fontWeight: 'bold' }}
                    >
                      {similarity.toFixed(1)}%
                    </Typography>
                  </Box>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                    审批状态
                  </Typography>
                  <Chip
                    label={
                      approvalStatus === 'pending'
                        ? '待审批'
                        : approvalStatus === 'approved'
                          ? '已批准'
                          : '已拒绝'
                    }
                    color={
                      approvalStatus === 'pending'
                        ? 'warning'
                        : approvalStatus === 'approved'
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
                <Paper
                  data-testid="regenerated-summary-surface"
                  sx={{ p: 2, backgroundColor: summarySurface, border: `1px solid ${theme.palette.divider}` }}
                >
                  <Typography variant="body2" sx={{ lineHeight: 1.6, color: 'text.secondary' }}>
                    {contentDifference.differenceSummary}
                  </Typography>
                </Paper>
              </Box>
            </Grid>
          </Grid>

          {/* 操作按钮 */}
          {approvalStatus === 'pending' && (
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
              original={contentDifference.originalContent}
              modified={contentDifference.regeneratedContent}
            />

            {/* 具体改变 */}
            {contentDifference.changes.length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  具体改变 ({contentDifference.changes.length} 项)
                </Typography>
                <Stack spacing={1}>
                  {contentDifference.changes.map((change, idx) => (
                    <Paper
                      key={idx}
                      data-testid="regenerated-change-item-surface"
                      sx={{ p: 1.5, backgroundColor: changeSurface, border: `1px solid ${theme.palette.divider}` }}
                    >
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
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                          位置: {change.position}
                        </Typography>
                      </Box>
                      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                        <strong>原:</strong> &quot;{change.original}&quot;
                      </Typography>
                      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                        <strong>新:</strong> &quot;{change.modified}&quot;
                      </Typography>
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
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
          {approvalStatus === 'pending' && onReject && (
            <Button
              onClick={handleReject}
              color="error"
              disabled={actionInProgress}
            >
              拒绝
            </Button>
          )}
          {approvalStatus === 'pending' && onApply && (
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
