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

const QUALITY_DIALOG_READY_ENDPOINTS = [
  '/live/script/delete',
  '/live/ai/check-similarity',
  '/live/ai/generate-skeleton-sse',
  '/live/ai/batch-chat-for-script',
  '/live/script/save',
  '/live/ai/refine-script',
  '/live/ai/generate-emotional',
  '/live/ai/generate-product',
]

const QUALITY_DIALOG_UNSUPPORTED_ACTIONS = [
  'direct-api-request',
  'local-similarity-fallback',
  'local-skeleton-fallback',
  'local-ai-refine-fallback',
  'local-emotional-script-fallback',
  'local-product-fallback',
]

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
      <Box
        data-testid="quality-check-dialogs-contract-root"
        data-contract-scope="live-quality-check-dialogs-props-suite"
        data-ready-endpoints={QUALITY_DIALOG_READY_ENDPOINTS.join('|')}
        data-unsupported-actions={QUALITY_DIALOG_UNSUPPORTED_ACTIONS.join('|')}
        data-no-direct-api-request="true"
        data-no-local-fallback="true"
        data-dialog-count={[
          deleteConfirm ? 'delete' : '',
          similarityOpen ? 'similarity' : '',
          skeletonOpen ? 'skeleton' : '',
          batchOpen ? 'batch' : '',
          refineOpen ? 'refine' : '',
          emotionalOpen ? 'emotional' : '',
          productGenOpen ? 'product' : '',
        ].filter(Boolean).length}
        sx={{ display: 'none' }}
      />
      {deleteConfirm && (
        <Dialog
          open
          onClose={onDeleteConfirmClose}
          data-testid="quality-delete-dialog"
          data-contract-scope="live-quality-delete-confirm-props"
          data-contract-source="/live/script/delete|onDeleteConfirm-prop"
          data-no-direct-api-request="true"
        >
          <DialogTitle data-testid="quality-delete-title" data-contract-source="/live/script/delete">确认删除</DialogTitle>
          <DialogContent data-testid="quality-delete-content" data-contract-source="/live/script/delete">
            确定删除该条话术？
          </DialogContent>
          <DialogActions>
            <Button onClick={onDeleteConfirmClose} data-testid="quality-delete-cancel-button" data-contract-source="onDeleteConfirmClose-prop">取消</Button>
            <Button
              color="error"
              variant="contained"
              onClick={onDeleteConfirm}
              data-testid="quality-delete-confirm-button"
              data-contract-source="/live/script/delete|onDeleteConfirm-prop"
            >
              删除
            </Button>
          </DialogActions>
        </Dialog>
      )}

      {similarityOpen && (
        <Dialog
          open
          onClose={onSimilarityClose}
          maxWidth="sm"
          fullWidth
          data-testid="quality-similarity-dialog"
          data-contract-scope="live-quality-similarity-result-props"
          data-contract-source="/live/ai/check-similarity"
          data-no-direct-api-request="true"
          data-no-local-similarity-fallback="true"
          data-similarity-count={similarityList.length}
          data-loading={similarityLoading ? 'true' : 'false'}
        >
          <DialogTitle data-testid="quality-similarity-title" data-contract-source="/live/ai/check-similarity">相似度检测</DialogTitle>
          <DialogContent data-testid="quality-similarity-content" data-contract-source="/live/ai/check-similarity">
            {similarityLoading ? (
              <Box
                sx={{ py: 4, textAlign: 'center' }}
                data-testid="quality-similarity-loading"
                data-contract-source="/live/ai/check-similarity"
              >
                <CircularProgress />
              </Box>
            ) : similarityList.length === 0 ? (
              <Typography
                color="text.secondary"
                data-testid="quality-similarity-empty-state"
                data-contract-source="/live/ai/check-similarity"
                data-no-local-similarity-fallback="true"
              >
                未发现明显相似段落
              </Typography>
            ) : (
              <List dense data-testid="quality-similarity-list" data-contract-source="/live/ai/check-similarity">
                {similarityList.map((item, i) => (
                  <ListItemButton
                    key={i}
                    sx={{ flexDirection: 'column', alignItems: 'flex-start' }}
                    data-testid="quality-similarity-item"
                    data-contract-source="/live/ai/check-similarity"
                    data-script-id-1={item.scriptId1}
                    data-script-id-2={item.scriptId2}
                    data-similarity-level={item.similarityLevel}
                  >
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
            <Button onClick={onSimilarityClose} data-testid="quality-similarity-close-button" data-contract-source="onSimilarityClose-prop">关闭</Button>
          </DialogActions>
        </Dialog>
      )}

      {skeletonOpen && (
        <Dialog
          open
          onClose={onSkeletonClose}
          maxWidth="sm"
          fullWidth
          data-testid="quality-skeleton-dialog"
          data-contract-scope="live-quality-skeleton-result-props"
          data-contract-source="/live/ai/generate-skeleton-sse"
          data-no-direct-api-request="true"
          data-no-local-skeleton-fallback="true"
          data-skeleton-count={skeletonList.length}
          data-loading={skeletonLoading ? 'true' : 'false'}
        >
          <DialogTitle data-testid="quality-skeleton-title" data-contract-source="/live/ai/generate-skeleton-sse">话术骨架</DialogTitle>
          <DialogContent data-testid="quality-skeleton-content" data-contract-source="/live/ai/generate-skeleton-sse">
            {skeletonLoading ? (
              <Box
                sx={{ py: 4, textAlign: 'center' }}
                data-testid="quality-skeleton-loading"
                data-contract-source="/live/ai/generate-skeleton-sse"
              >
                <CircularProgress />
              </Box>
            ) : skeletonList.length === 0 ? (
              <Typography
                color="text.secondary"
                data-testid="quality-skeleton-empty-state"
                data-contract-source="/live/ai/generate-skeleton-sse"
                data-no-local-skeleton-fallback="true"
              >
                暂无骨架
              </Typography>
            ) : (
              <List dense data-testid="quality-skeleton-list" data-contract-source="/live/ai/generate-skeleton-sse">
                {skeletonList.map((item) => (
                  <ListItemButton
                    key={item.scriptId}
                    onClick={() => onSkeletonExpand(item.scriptId, item.summary, item.suggestedDurationSec)}
                    data-testid="quality-skeleton-item"
                    data-contract-source="/live/ai/generate-skeleton-sse|onSkeletonExpand-prop"
                    data-script-id={item.scriptId}
                    data-script-type={item.scriptType}
                    data-duration={item.suggestedDurationSec}
                  >
                    <ListItemText
                      primary={`#${item.scriptId} ${scriptTypeLabel[item.scriptType] ?? item.scriptType} · ${item.suggestedDurationSec}s`}
                      secondary={item.summary}
                    />
                    <Typography
                      variant="caption"
                      color="primary"
                      data-testid="quality-skeleton-expand-label"
                      data-contract-source="onSkeletonExpand-prop"
                    >
                      展开
                    </Typography>
                  </ListItemButton>
                ))}
              </List>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={onSkeletonClose} data-testid="quality-skeleton-close-button" data-contract-source="onSkeletonClose-prop">关闭</Button>
          </DialogActions>
        </Dialog>
      )}

      {batchOpen && (
        <Dialog
          open
          onClose={() => { onBatchClose(); onBatchMessageChange('') }}
          maxWidth="sm"
          fullWidth
          data-testid="quality-batch-dialog"
          data-contract-scope="live-quality-batch-ai-command-props"
          data-contract-source="/live/ai/batch-chat-for-script|/live/script/save"
          data-no-direct-api-request="true"
          data-selected-count={selectedCount}
          data-message-state={batchMessage.trim() ? 'ready' : 'empty'}
          data-loading={batchLoading ? 'true' : 'false'}
        >
          <DialogTitle data-testid="quality-batch-title" data-contract-source="/live/ai/batch-chat-for-script">批量应用 AI 指令</DialogTitle>
          <DialogContent data-testid="quality-batch-content" data-contract-source="/live/ai/batch-chat-for-script|/live/script/save">
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
              inputProps={{
                'data-testid': 'quality-batch-message-input',
                'data-contract-source': 'onBatchMessageChange-prop',
              }}
            />
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => { onBatchClose(); onBatchMessageChange('') }}
              data-testid="quality-batch-cancel-button"
              data-contract-source="onBatchClose-prop|onBatchMessageChange-prop"
            >
              取消
            </Button>
            <Button
              variant="contained"
              onClick={onBatchApply}
              disabled={batchLoading || !batchMessage.trim()}
              data-testid="quality-batch-apply-button"
              data-contract-source="/live/ai/batch-chat-for-script|/live/script/save|onBatchApply-prop"
              data-disabled-reason={batchLoading ? 'batch-loading' : (!batchMessage.trim() ? 'empty-message' : 'ready')}
            >
              {batchLoading ? '处理中...' : '批量生成并保存'}
            </Button>
          </DialogActions>
        </Dialog>
      )}

      {refineOpen && (
        <Dialog
          open
          onClose={() => { onRefineClose(); onRefineQuestionChange('') }}
          maxWidth="sm"
          fullWidth
          data-testid="quality-refine-dialog"
          data-contract-scope="live-quality-refine-ai-command-props"
          data-contract-source="/live/ai/refine-script|/live/script/save"
          data-no-direct-api-request="true"
          data-script-id={refineOpen.scriptId}
          data-question-state={refineQuestion.trim() ? 'ready' : 'empty'}
          data-loading={refineLoading ? 'true' : 'false'}
        >
          <DialogTitle data-testid="quality-refine-title" data-contract-source="/live/ai/refine-script">AI 修改话术</DialogTitle>
          <DialogContent data-testid="quality-refine-content" data-contract-source="/live/ai/refine-script|/live/script/save">
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              输入修改要求，AI 将根据您的要求修改话术，例如：
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }} data-testid="quality-refine-suggestion-list" data-contract-source="onRefineQuestionChange-prop">
              {REFINE_SUGGESTIONS.map((s) => (
                <Chip
                  key={s}
                  label={s}
                  size="small"
                  variant="outlined"
                  onClick={() => onRefineQuestionChange(s)}
                  sx={{ cursor: 'pointer' }}
                  data-testid="quality-refine-suggestion-chip"
                  data-contract-source="onRefineQuestionChange-prop"
                />
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
              inputProps={{
                'data-testid': 'quality-refine-question-input',
                'data-contract-source': 'onRefineQuestionChange-prop',
              }}
            />
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => { onRefineClose(); onRefineQuestionChange('') }}
              data-testid="quality-refine-cancel-button"
              data-contract-source="onRefineClose-prop|onRefineQuestionChange-prop"
            >
              取消
            </Button>
            <Button
              variant="contained"
              onClick={onRefineApply}
              disabled={refineLoading || !refineQuestion.trim()}
              data-testid="quality-refine-apply-button"
              data-contract-source="/live/ai/refine-script|/live/script/save|onRefineApply-prop"
              data-disabled-reason={refineLoading ? 'refine-loading' : (!refineQuestion.trim() ? 'empty-question' : 'ready')}
            >
              {refineLoading ? '修改中...' : 'AI 修改'}
            </Button>
          </DialogActions>
        </Dialog>
      )}

      <Dialog
        open={emotionalOpen}
        onClose={onEmotionalClose}
        maxWidth="xs"
        fullWidth
        data-testid="quality-emotional-dialog"
        data-contract-scope="live-quality-emotional-generator-props"
        data-contract-source="/live/ai/generate-emotional"
        data-no-direct-api-request="true"
        data-no-local-emotional-fallback="true"
        data-category={emotionalCategory}
        data-sub-category={emotionalSubCategory}
        data-loading={emotionalLoading ? 'true' : 'false'}
      >
        <DialogTitle data-testid="quality-emotional-title" data-contract-source="/live/ai/generate-emotional">插入情绪价值话术</DialogTitle>
        <DialogContent data-testid="quality-emotional-content" data-contract-source="/live/ai/generate-emotional">
          <FormControl fullWidth size="small" sx={{ mt: 1, mb: 2 }}>
            <InputLabel>话术类型</InputLabel>
            <Select
              value={emotionalCategory}
              label="话术类型"
              onChange={(e) => {
                onEmotionalCategoryChange(e.target.value)
                onEmotionalSubCategoryChange('')
              }}
              data-testid="quality-emotional-category-select"
              data-contract-source="onEmotionalCategoryChange-prop|onEmotionalSubCategoryChange-prop"
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
                data-testid="quality-emotional-sub-category-select"
                data-contract-source="onEmotionalSubCategoryChange-prop"
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
          <Button onClick={onEmotionalClose} data-testid="quality-emotional-cancel-button" data-contract-source="onEmotionalClose-prop">取消</Button>
          <Button
            variant="contained"
            onClick={onEmotionalGenerate}
            disabled={emotionalLoading}
            data-testid="quality-emotional-generate-button"
            data-contract-source="/live/ai/generate-emotional|onEmotionalGenerate-prop"
            data-disabled-reason={emotionalLoading ? 'emotional-loading' : 'ready'}
          >
            {emotionalLoading ? '生成中...' : '生成并插入'}
          </Button>
        </DialogActions>
      </Dialog>

      {productGenOpen && (
        <Dialog
          open
          onClose={onProductGenClose}
          maxWidth="xs"
          fullWidth
          data-testid="quality-product-dialog"
          data-contract-scope="live-quality-product-generator-props"
          data-contract-source="/live/ai/generate-product|/live/product/by-session"
          data-no-direct-api-request="true"
          data-no-local-product-fallback="true"
          data-product-count={sortedProducts.length}
        >
          <DialogTitle data-testid="quality-product-title" data-contract-source="/live/ai/generate-product">选择产品</DialogTitle>
          <DialogContent data-testid="quality-product-content" data-contract-source="/live/ai/generate-product|/live/product/by-session">
            {sortedProducts.length === 0 ? (
              <Typography
                color="text.secondary"
                data-testid="quality-product-empty-state"
                data-contract-source="/live/product/by-session"
                data-no-local-product-fallback="true"
              >
                暂无可生成产品
              </Typography>
            ) : (
              <List data-testid="quality-product-list" data-contract-source="/live/product/by-session">
                {sortedProducts.map((p) => (
                  <ListItemButton
                    key={String(p.id)}
                    onClick={() => onProductGenSelect(p.productId as number)}
                    data-testid="quality-product-item"
                    data-contract-source="/live/product/by-session|onProductGenSelect-prop"
                    data-product-id={String(p.productId ?? '')}
                  >
                    <ListItemText primary={String(p.productName ?? p.id)} />
                  </ListItemButton>
                ))}
              </List>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={onProductGenClose} data-testid="quality-product-cancel-button" data-contract-source="onProductGenClose-prop">取消</Button>
          </DialogActions>
        </Dialog>
      )}
    </>
  )
}
