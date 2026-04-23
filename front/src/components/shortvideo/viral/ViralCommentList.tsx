import { Box, Chip, CircularProgress, Pagination, Typography } from '@mui/material'
import type { ViralComment } from '@/api/viral-analysis'

export interface ViralCommentListProps {
  loading: boolean
  comments: ViralComment[]
  commentsTotal: number
  commentsPage: number
  detailId: number | null
  onPageChange: (zeroBasedPage: number) => void
}

export function ViralCommentList({
  loading,
  comments,
  commentsTotal,
  commentsPage,
  detailId,
  onPageChange,
}: ViralCommentListProps) {
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
        <CircularProgress size={24} />
      </Box>
    )
  }
  if (comments.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        暂无评论数据（需先执行深度分析且评论抓取已启用）
      </Typography>
    )
  }
  return (
    <>
      {comments.map((c) => (
        <Box key={c.id} sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="subtitle2">{c.authorName || '匿名'}</Typography>
            {c.sentiment && (
              <Chip
                size="small"
                label={c.sentiment}
                color={c.sentiment === 'positive' ? 'success' : c.sentiment === 'negative' ? 'error' : 'default'}
                variant="outlined"
              />
            )}
            {c.likeCount != null && c.likeCount > 0 && (
              <Typography variant="caption" color="text.secondary">{c.likeCount} 赞</Typography>
            )}
          </Box>
          <Typography variant="body2" sx={{ mt: 0.5 }}>{c.content}</Typography>
        </Box>
      ))}
      {commentsTotal > 20 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
          <Pagination
            count={Math.ceil(commentsTotal / 20)}
            page={commentsPage + 1}
            onChange={(_, p) => detailId != null && onPageChange(p - 1)}
            size="small"
          />
        </Box>
      )}
    </>
  )
}
