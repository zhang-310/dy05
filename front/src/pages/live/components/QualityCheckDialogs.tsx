import {
  Box,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  List,
  ListItemButton,
  ListItemText,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  CircularProgress,
} from '@mui/material'
import { EMOTIONAL_CATEGORIES, REFINE_SUGGESTIONS } from './constants'

export interface SimilarityItem {
  scriptId1: number
  scriptId2: number
  type1: string
  type2: string
  similarityLevel: string
  suggestion: string
}

export interface SkeletonItem {
  scriptId: number
  scriptType: string
  summary: string
  suggestedDurationSec: number
}

export interface QualityCheckDialogsProps {
  deleteConfirm: Record<string, unknown> | null
  onDeleteConfirmClose: () => void
  onDeleteConfirm: () => void

  similarityOpen: boolean
  similarityList: SimilarityItem[]
  similarityLoading: boolean
  onSimilarityClose: () => void

  skeletonOpen: boolean
  skeletonList: SkeletonItem[]
  skeletonLoading: boolean
  onSkeletonClose: () => void
  onSkeletonExpand: (scriptId: number, summary: string, duration: number) => void

  batchOpen: boolean
  batchMessage: string
  batchLoading: boolean
  selectedCount: number
  onBatchClose: () => void
  onBatchMessageChange: (v: string) => void
  onBatchApply: () => void

  refineOpen: { scriptId: number } | null
  refineQuestion: string
  refineLoading: boolean
  onRefineClose: () => void
  onRefineQuestionChange: (v: string) => void
  onRefineApply: () => void

  emotionalOpen: boolean
  emotionalCategory: string
  emotionalSubCategory: string
  emotionalLoading: boolean
  onEmotionalClose: () => void
  onEmotionalCategoryChange: (v: string) => void
  onEmotionalSubCategoryChange: (v: string) => void
  onEmotionalGenerate: () => void

  productGenOpen: boolean
  sortedProducts: Record<string, unknown>[]
  onProductGenClose: () => void
  onProductGenSelect: (productId: number) => void

  scriptTypeLabel: Record<string, string>
}

export function QualityCheckDialogs({
  deleteConfirm,
  onDeleteConfirmClose,
  onDeleteConfirm,
  similarityOpen,
  similarityList,
  similarityLoading,
  onSimilarityClose,
  skeletonOpen,
  skeletonList,
  skeletonLoading,
  onSkeletonClose,
  onSkeletonExpand,
  batchOpen,
  batchMessage,
  batchLoading,
  selectedCount,
  onBatchClose,
  onBatchMessageChange,
  onBatchApply,
  refineOpen,
  refineQuestion,
  refineLoading,
  onRefineClose,
  onRefineQuestionChange,
  onRefineApply,
  emotionalOpen,
  emotionalCategory,
  emotionalSubCategory,
  emotionalLoading,
  onEmotionalClose,
  onEmotionalCategoryChange,
  onEmotionalSubCategoryChange,
  onEmotionalGenerate,
  productGenOpen,
  sortedProducts,
  onProductGenClose,
  onProductGenSelect,
  scriptTypeLabel,
}: QualityCheckDialogsProps) {
  return (
    <>
      {deleteConfirm && (
        <Dialog open onClose={onDeleteConfirmClose}>
          <DialogTitle>确认删除</DialogTitle>
          <DialogContent>确定删除该条话术？</DialogContent>
          <DialogActions>
            <Button onClick={onDeleteConfirmClose}>取消</Button>
            <Button color="error" variant="contained" onClick={onDeleteConfirm}>
              删除
            </Button>
          </DialogActions>
        </Dialog>
      )}

      {similarityOpen && (
        <Dialog open onClose={onSimilarityClose} maxWidth="sm" fullWidth>
          <DialogTitle>相似度检测</DialogTitle>
          <DialogContent>
            {similarityLoading ? (
              <Box sx={{ py: 4, textAlign: 'center' }}><CircularProgress /></Box>
            ) : similarityList.length === 0 ? (
              <Typography color="text.secondary">未发现明显相似段落</Typography>
            ) : (
              <List dense>
                {similarityList.map((item, i) => (
                  <ListItemButton key={i} sx={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                    <Typography variant="body2">
                      #{item.scriptId1} ({item.type1}) 与 #{item.scriptId2} ({item.type2}) 相似度：{item.similarityLevel}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">{item.suggestion}</Typography>
                  </ListItemButton>
                ))}
              </List>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={onSimilarityClose}>关闭</Button>
          </DialogActions>
        </Dialog>
      )}

      {skeletonOpen && (
        <Dialog open onClose={onSkeletonClose} maxWidth="sm" fullWidth>
          <DialogTitle>话术骨架</DialogTitle>
          <DialogContent>
            {skeletonLoading ? (
              <Box sx={{ py: 4, textAlign: 'center' }}><CircularProgress /></Box>
            ) : skeletonList.length === 0 ? (
              <Typography color="text.secondary">暂无骨架</Typography>
            ) : (
              <List dense>
                {skeletonList.map((item) => (
                  <ListItemButton
                    key={item.scriptId}
                    onClick={() => onSkeletonExpand(item.scriptId, item.summary, item.suggestedDurationSec)}
                  >
                    <ListItemText
                      primary={`#${item.scriptId} ${scriptTypeLabel[item.scriptType] ?? item.scriptType} · ${item.suggestedDurationSec}s`}
                      secondary={item.summary}
                    />
                    <Typography variant="caption" color="primary">展开</Typography>
                  </ListItemButton>
                ))}
              </List>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={onSkeletonClose}>关闭</Button>
          </DialogActions>
        </Dialog>
      )}

      {batchOpen && (
        <Dialog open onClose={() => { onBatchClose(); onBatchMessageChange('') }} maxWidth="sm" fullWidth>
          <DialogTitle>批量应用 AI 指令</DialogTitle>
          <DialogContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              已选 {selectedCount} 段话术，输入统一指令（如：改成促销风格、缩短到30秒内）将应用到所有选中段落
            </Typography>
            <TextField
              fullWidth
              multiline
              minRows={3}
              label="指令"
              placeholder="如：全场统一改成促销风格、所有转场缩短到20秒"
              value={batchMessage}
              onChange={(e) => onBatchMessageChange(e.target.value)}
              size="small"
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { onBatchClose(); onBatchMessageChange('') }}>取消</Button>
            <Button variant="contained" onClick={onBatchApply} disabled={batchLoading || !batchMessage.trim()}>
              {batchLoading ? '处理中...' : '批量生成并保存'}
            </Button>
          </DialogActions>
        </Dialog>
      )}

      {refineOpen && (
        <Dialog open onClose={() => { onRefineClose(); onRefineQuestionChange('') }} maxWidth="sm" fullWidth>
          <DialogTitle>AI 修改话术</DialogTitle>
          <DialogContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              输入修改要求，AI 将根据您的要求修改话术，例如：
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }}>
              {REFINE_SUGGESTIONS.map((s) => (
                <Chip key={s} label={s} size="small" variant="outlined" onClick={() => onRefineQuestionChange(s)} sx={{ cursor: 'pointer' }} />
              ))}
            </Box>
            <TextField
              fullWidth
              multiline
              minRows={2}
              label="修改要求"
              placeholder="如：改成30秒以内、换成促销风格、改成促单话术"
              value={refineQuestion}
              onChange={(e) => onRefineQuestionChange(e.target.value)}
              size="small"
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { onRefineClose(); onRefineQuestionChange('') }}>取消</Button>
            <Button variant="contained" onClick={onRefineApply} disabled={refineLoading || !refineQuestion.trim()}>
              {refineLoading ? '修改中...' : 'AI 修改'}
            </Button>
          </DialogActions>
        </Dialog>
      )}

      <Dialog open={emotionalOpen} onClose={onEmotionalClose} maxWidth="xs" fullWidth>
        <DialogTitle>插入情绪价值话术</DialogTitle>
        <DialogContent>
          <FormControl fullWidth size="small" sx={{ mt: 1, mb: 2 }}>
            <InputLabel>话术类型</InputLabel>
            <Select
              value={emotionalCategory}
              label="话术类型"
              onChange={(e) => {
                onEmotionalCategoryChange(e.target.value)
                onEmotionalSubCategoryChange('')
              }}
            >
              {EMOTIONAL_CATEGORIES.map((c) => (
                <MenuItem key={c.value} value={c.value}>{c.label}</MenuItem>
              ))}
            </Select>
          </FormControl>
          {EMOTIONAL_CATEGORIES.find((c) => c.value === emotionalCategory)?.subOptions && (
            <FormControl fullWidth size="small" sx={{ mb: 2 }}>
              <InputLabel>子类别（可选）</InputLabel>
              <Select
                value={emotionalSubCategory}
                label="子类别（可选）"
                onChange={(e) => onEmotionalSubCategoryChange(e.target.value)}
              >
                <MenuItem value="">不指定</MenuItem>
                {EMOTIONAL_CATEGORIES.find((c) => c.value === emotionalCategory)?.subOptions?.map((o) => (
                  <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          <Typography variant="caption" color="text.secondary">
            情绪话术穿插在产品话术间，可提升停留时长和互动率
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={onEmotionalClose}>取消</Button>
          <Button variant="contained" onClick={onEmotionalGenerate} disabled={emotionalLoading}>
            {emotionalLoading ? '生成中...' : '生成并插入'}
          </Button>
        </DialogActions>
      </Dialog>

      {productGenOpen && (
        <Dialog open onClose={onProductGenClose} maxWidth="xs" fullWidth>
          <DialogTitle>选择产品</DialogTitle>
          <DialogContent>
            <List>
              {sortedProducts.map((p) => (
                <ListItemButton key={String(p.id)} onClick={() => onProductGenSelect(p.productId as number)}>
                  <ListItemText primary={String(p.productName ?? p.id)} />
                </ListItemButton>
              ))}
            </List>
          </DialogContent>
          <DialogActions>
            <Button onClick={onProductGenClose}>取消</Button>
          </DialogActions>
        </Dialog>
      )}
    </>
  )
}
