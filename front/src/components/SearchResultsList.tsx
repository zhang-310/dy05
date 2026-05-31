/**
 * 搜索结果列表组件
 * W-06: 脚本类型、效果评分、使用次数、反馈按钮
 * @author Claude Code
 * @since 2026-03-06
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  IconButton,
  Stack,
  Grid,
  Rating,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Paper,
  alpha,
  useTheme,
} from '@mui/material';
import type { Theme } from '@mui/material/styles';
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ThumbDownIcon from '@mui/icons-material/ThumbDown';
import ShareIcon from '@mui/icons-material/Share';
import VisibilityIcon from '@mui/icons-material/Visibility';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import type { HybridSearchResultVO } from '@/types/search';

interface SearchResultsListProps {
  results: HybridSearchResultVO[];
  isLoading?: boolean;
  total?: number;
  executionTimeMs?: number;
  onFeedback?: (resultId: number, isHelpful: boolean, rating?: number, comment?: string) => Promise<void>;
  onResultClick?: (result: HybridSearchResultVO) => void;
}

/**
 * 脚本类型标签颜色
 */
const scriptTypeColors: Record<string, 'default' | 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success'> = {
  product: 'primary',
  shortvideo: 'secondary',
  live: 'error',
  event: 'warning',
};

/**
 * 脚本类型标签文本
 */
const scriptTypeLabels: Record<string, string> = {
  product: '商品话术',
  shortvideo: '短视频',
  live: '直播话术',
  event: '活动话术',
};

type SearchTone = 'success' | 'warning' | 'error';

function semanticColor(theme: Theme, tone: SearchTone) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main;
}

function getRelevanceTone(score: number): SearchTone {
  if (score > 80) return 'success';
  if (score > 60) return 'warning';
  return 'error';
}

/**
 * 单个搜索结果卡片
 */
function SearchResultCard({
  result,
  onFeedback,
  onResultClick,
}: {
  result: HybridSearchResultVO;
  onFeedback?: (resultId: number, isHelpful: boolean, rating?: number, comment?: string) => Promise<void>;
  onResultClick?: (result: HybridSearchResultVO) => void;
}) {
  const theme = useTheme();
  const [feedbackDialogOpen, setFeedbackDialogOpen] = useState(false);
  const [rating, setRating] = useState<number | null>(null);
  const [comment, setComment] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const relevanceTone = getRelevanceTone(result.relevanceScore);
  const relevanceColor = semanticColor(theme, relevanceTone);
  const relevanceTrackColor = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.18 : 0.12);

  const handleHelpful = async () => {
    setFeedbackDialogOpen(true);
  };

  const handleUnhelpful = async () => {
    if (onFeedback) {
      try {
        await onFeedback(result.id, false, 1);
      } catch (err) {
        console.error('Failed to submit feedback:', err);
      }
    }
  };

  const handleSubmitFeedback = async () => {
    if (onFeedback) {
      setSubmittingFeedback(true);
      try {
        await onFeedback(result.id, true, rating || 5, comment);
        setFeedbackDialogOpen(false);
        setRating(null);
        setComment('');
      } finally {
        setSubmittingFeedback(false);
      }
    }
  };

  return (
    <>
      <Card
        sx={{
          mb: 2,
          cursor: 'pointer',
          transition: 'all 0.2s ease',
          '&:hover': {
            boxShadow: `0 4px 16px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.36 : 0.12)}`,
            transform: 'translateY(-2px)',
          },
        }}
        onClick={() => onResultClick?.(result)}
      >
        <CardContent>
          <Grid container spacing={2}>
            {/* 左侧：内容 */}
            <Grid item xs={12} md={8}>
              {/* 标题和类型 */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Chip
                  label={scriptTypeLabels[result.scriptType]}
                  color={scriptTypeColors[result.scriptType]}
                  size="small"
                />
                {result.tags && result.tags.length > 0 && (
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    {result.tags.slice(0, 2).map((tag, idx) => (
                      <Chip key={idx} label={tag} size="small" variant="outlined" />
                    ))}
                    {result.tags.length > 2 && (
                      <Chip label={`+${result.tags.length - 2}`} size="small" variant="outlined" />
                    )}
                  </Box>
                )}
              </Box>

              {/* 标题 */}
              {result.title && (
                <Typography
                  variant="h6"
                  sx={{
                    mb: 1,
                    fontWeight: 'bold',
                    color: 'text.primary',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                  }}
                >
                  {result.title}
                </Typography>
              )}

              {/* 内容预览 */}
              {result.preview && (
                <Typography
                  variant="body2"
                  sx={{
                    color: 'text.secondary',
                    mb: 1,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                  }}
                >
                  {result.preview}
                </Typography>
              )}

              {/* 元数据 */}
              <Stack direction="row" spacing={2} sx={{ mt: 1.5 }}>
                {result.author && (
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    作者: {result.author}
                  </Typography>
                )}
                {result.createdAt && (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <AccessTimeIcon sx={{ fontSize: 14, color: 'text.secondary' }} />
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      {new Date(result.createdAt).toLocaleDateString('zh-CN')}
                    </Typography>
                  </Box>
                )}
              </Stack>
            </Grid>

            {/* 右侧：评分和使用情况 */}
            <Grid item xs={12} md={4}>
              <Stack spacing={2} sx={{ height: '100%', justifyContent: 'space-between' }}>
                {/* 相关性评分 */}
                <Box>
                  <Typography variant="subtitle2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                    相关性
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box
                      data-testid="search-result-relevance-track-surface"
                      data-track-color={relevanceTrackColor}
                      sx={{
                        width: '100%',
                        height: '8px',
                        backgroundColor: relevanceTrackColor,
                        borderRadius: 4,
                        overflow: 'hidden',
                      }}
                    >
                      <Box
                        data-testid="search-result-relevance-fill-surface"
                        data-relevance-tone={relevanceTone}
                        data-relevance-color={relevanceColor}
                        sx={{
                          width: `${result.relevanceScore}%`,
                          height: '100%',
                          backgroundColor: relevanceColor,
                          transition: 'width 0.3s ease',
                        }}
                      />
                    </Box>
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 'bold',
                        minWidth: 30,
                        textAlign: 'right',
                      }}
                    >
                      {result.relevanceScore}%
                    </Typography>
                  </Box>
                </Box>

                {/* 效果评分 */}
                {result.effectivenessScore !== undefined && (
                  <Box>
                    <Typography variant="subtitle2" sx={{ color: 'text.secondary', mb: 0.5 }}>
                      效果评分
                    </Typography>
                    <Rating
                      value={result.effectivenessScore / 20}
                      readOnly
                      size="small"
                    />
                  </Box>
                )}

                {/* 使用次数 */}
                {result.usageCount !== undefined && (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <VisibilityIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      被使用 {result.usageCount} 次
                    </Typography>
                  </Box>
                )}
              </Stack>
            </Grid>
          </Grid>

          {/* 操作按钮 */}
          <Stack
            direction="row"
            spacing={1}
            sx={{
              mt: 2,
              pt: 2,
              borderTop: `1px solid ${theme.palette.divider}`,
            }}
          >
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                handleHelpful();
              }}
              title="有帮助"
              color="success"
            >
              <ThumbUpIcon fontSize="small" />
            </IconButton>
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                handleUnhelpful();
              }}
              title="无帮助"
              color="error"
            >
              <ThumbDownIcon fontSize="small" />
            </IconButton>
            <IconButton
              size="small"
              title="分享"
              onClick={(e) => {
                e.stopPropagation();
              }}
            >
              <ShareIcon fontSize="small" />
            </IconButton>
          </Stack>
        </CardContent>
      </Card>

      {/* 反馈对话框 */}
      <Dialog open={feedbackDialogOpen} onClose={() => setFeedbackDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>反馈搜索结果质量</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Stack spacing={2}>
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                评分
              </Typography>
              <Rating
                value={rating}
                onChange={(_, value) => setRating(value)}
                size="large"
              />
            </Box>
            <TextField
              label="额外评论（可选）"
              multiline
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFeedbackDialogOpen(false)}>取消</Button>
          <Button
            onClick={handleSubmitFeedback}
            variant="contained"
            disabled={submittingFeedback}
          >
            提交反馈
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export const SearchResultsList: React.FC<SearchResultsListProps> = ({
  results,
  isLoading = false,
  total,
  executionTimeMs,
  onFeedback,
  onResultClick,
}) => {
  const theme = useTheme();
  const quietSurface = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.07 : 0.035);

  if (results.length === 0 && !isLoading) {
    return (
      <Paper
        data-testid="search-results-empty-surface"
        sx={{ p: 3, textAlign: 'center', backgroundColor: quietSurface, border: `1px solid ${theme.palette.divider}` }}
      >
        <Typography color="textSecondary">暂无搜索结果</Typography>
      </Paper>
    );
  }

  return (
    <Box>
      {/* 搜索统计 */}
      {(total !== undefined || executionTimeMs !== undefined) && (
        <Paper
          data-testid="search-results-summary-surface"
          sx={{ p: 2, mb: 2, backgroundColor: quietSurface, border: `1px solid ${theme.palette.divider}` }}
        >
          <Stack direction="row" spacing={3}>
            {total !== undefined && (
              <Box>
                <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
                  找到 {total} 个结果
                </Typography>
              </Box>
            )}
            {executionTimeMs !== undefined && (
              <Box>
                <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
                  耗时 {executionTimeMs}ms
                </Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      )}

      {/* 结果列表 */}
      <Box>
        {results.map((result) => (
          <SearchResultCard
            key={result.id}
            result={result}
            onFeedback={onFeedback}
            onResultClick={onResultClick}
          />
        ))}
      </Box>

      {/* 加载中 */}
      {isLoading && (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography color="textSecondary">加载中...</Typography>
        </Paper>
      )}
    </Box>
  );
};
