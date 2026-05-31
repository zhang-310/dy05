import { memo, useState, useEffect, useCallback } from 'react'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
  Alert,
} from '@mui/material'
import StarIcon from '@mui/icons-material/Star'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { searchTemplates, applyTemplate, type ScriptTemplate } from '@/api/live-script'
import { SCRIPT_TYPE_LABEL } from './constants'

export interface TemplateLibraryDialogProps {
  open: boolean
  onClose: () => void
  onApply: (content: string, scriptType: string) => void
}

const TEMPLATE_LIBRARY_READY_ENDPOINTS = [
  '/live/template/search',
  '/live/template/apply',
]

const TEMPLATE_LIBRARY_UNSUPPORTED_ACTIONS = [
  'local-template-fallback',
  'direct-script-save',
  'template-mutation',
  'product-mutation',
  'shortvideo-mutation',
]

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error || '未知错误')
}

export const TemplateLibraryDialog = memo(function TemplateLibraryDialog({
  open,
  onClose,
  onApply,
}: TemplateLibraryDialogProps) {
  const [templates, setTemplates] = useState<ScriptTemplate[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [applyingId, setApplyingId] = useState<number | null>(null)
  const [scriptType, setScriptType] = useState('')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [applyError, setApplyError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const res = await searchTemplates({
        scriptType: scriptType || undefined,
        keyword: keyword || undefined,
        page,
        rows: 20,
      })
      setTemplates(res.list ?? [])
      setTotal(res.total ?? 0)
    } catch (error) {
      setTemplates([])
      setTotal(0)
      setLoadError(`/live/template/search 模板加载失败：${getErrorMessage(error)}`)
    } finally {
      setLoading(false)
    }
  }, [scriptType, keyword, page])

  useEffect(() => {
    if (open) load()
  }, [open, load])

  const handleApply = async (tpl: ScriptTemplate) => {
    setApplyingId(tpl.id)
    setApplyError(null)
    try {
      const res = await applyTemplate(tpl.id)
      onApply(res.content, res.scriptType)
      onClose()
    } catch (error) {
      setApplyError(`/live/template/apply 模板应用失败：${getErrorMessage(error)}`)
    } finally {
      setApplyingId(null)
    }
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      data-testid="template-library-dialog"
      data-contract-scope="live-template-library-dialog"
      data-ready-endpoints={TEMPLATE_LIBRARY_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={TEMPLATE_LIBRARY_UNSUPPORTED_ACTIONS.join('|')}
      data-template-count={templates.length}
      data-total={total}
      data-page={page}
      data-script-type={scriptType}
      data-keyword={keyword}
      data-loading={loading ? 'true' : 'false'}
      data-applying-id={applyingId ?? ''}
      data-no-local-template-fallback="true"
    >
      <DialogTitle data-testid="template-library-title" data-contract-source="/live/template/search">话术模板库</DialogTitle>
      <DialogContent>
        <Box
          data-testid="template-library-toolbar"
          data-contract-source="/live/template/search"
          sx={{ display: 'flex', gap: 1, mb: 2, mt: 0.5, flexWrap: 'wrap' }}
        >
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>话术类型</InputLabel>
            <Select
              value={scriptType}
              label="话术类型"
              onChange={(e) => { setScriptType(e.target.value); setPage(0) }}
              data-testid="template-library-script-type-select"
              data-contract-source="/live/template/search"
            >
              <MenuItem value="">全部</MenuItem>
              {Object.entries(SCRIPT_TYPE_LABEL).map(([k, v]) => (
                <MenuItem key={k} value={k}>{v}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            size="small"
            label="搜索关键词"
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0) }}
            inputProps={{
              'data-testid': 'template-library-keyword-input',
              'data-contract-source': '/live/template/search',
            }}
            sx={{ flex: 1 }}
          />
          <Button
            size="small"
            variant="outlined"
            onClick={load}
            disabled={loading}
            data-testid="template-library-search-button"
            data-contract-source="/live/template/search"
            data-disabled-reason={loading ? 'loading' : 'ready'}
          >
            搜索
          </Button>
        </Box>

        {loadError && (
          <Alert
            severity="error"
            data-testid="template-library-load-error"
            data-contract-source="/live/template/search"
            data-no-local-template-fallback="true"
            sx={{ mb: 1.5 }}
          >
            {loadError}
          </Alert>
        )}

        {applyError && (
          <Alert
            severity="error"
            data-testid="template-library-apply-error"
            data-contract-source="/live/template/apply"
            data-no-local-template-fallback="true"
            sx={{ mb: 1.5 }}
          >
            {applyError}
          </Alert>
        )}

        {loading && (
          <Typography
            color="text.secondary"
            data-testid="template-library-loading"
            data-contract-source="/live/template/search"
            sx={{ py: 2, textAlign: 'center' }}
          >
            加载中...
          </Typography>
        )}

        {!loading && templates.length === 0 && (
          <Typography
            color="text.secondary"
            data-testid="template-library-empty-state"
            data-contract-source="/live/template/search"
            data-no-local-template-fallback="true"
            sx={{ py: 4, textAlign: 'center' }}
          >
            暂无模板
          </Typography>
        )}

        <Box
          data-testid="template-library-list"
          data-contract-source="/live/template/search"
          data-no-local-template-fallback="true"
          sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}
        >
          {templates.map((tpl) => (
            <Box
              key={tpl.id}
              data-testid="template-library-item"
              data-contract-source="/live/template/search"
              data-template-id={tpl.id}
              data-script-type={tpl.scriptType}
              data-auto-collected={tpl.autoCollected === 1 ? 'true' : 'false'}
              sx={{
                p: 1.5,
                border: 1,
                borderColor: 'divider',
                borderRadius: 1,
                '&:hover': { bgcolor: 'action.hover' },
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                <Typography variant="body2" sx={{ fontWeight: 600, flex: 1 }}>
                  {tpl.templateName}
                </Typography>
                <Chip
                  data-testid="template-library-type-chip"
                  data-contract-source="/live/template/search"
                  label={SCRIPT_TYPE_LABEL[tpl.scriptType] ?? tpl.scriptType}
                  size="small"
                  variant="outlined"
                  sx={{ height: 20, fontSize: '0.7rem' }}
                />
                {tpl.autoCollected === 1 && (
                  <Chip
                    label="自动沉淀"
                    size="small"
                    color="info"
                    icon={<AutoAwesomeIcon />}
                    data-testid="template-library-auto-chip"
                    data-contract-source="/live/template/search"
                    sx={{ height: 20, fontSize: '0.7rem' }}
                  />
                )}
                {tpl.effectivenessScore > 0 && (
                  <Chip
                    data-testid="template-library-score-chip"
                    data-contract-source="/live/template/search"
                    label={`${tpl.effectivenessScore}分`}
                    size="small"
                    color="success"
                    icon={<StarIcon />}
                    sx={{ height: 20, fontSize: '0.7rem' }}
                  />
                )}
              </Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                  display: '-webkit-box',
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: 'vertical',
                  overflow: 'hidden',
                  mb: 0.5,
                }}
              >
                {tpl.content}
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {tpl.category && <Chip label={tpl.category} size="small" variant="outlined" sx={{ height: 18, fontSize: '0.65rem' }} />}
                <Typography variant="caption" color="text.disabled">
                  使用 {tpl.usageCount ?? 0} 次
                </Typography>
                <Box sx={{ flex: 1 }} />
                <Button
                  size="small"
                  variant="contained"
                  onClick={() => handleApply(tpl)}
                  disabled={applyingId === tpl.id}
                  data-testid="template-library-apply-button"
                  data-contract-source="/live/template/apply"
                  data-template-id={tpl.id}
                  data-disabled-reason={applyingId === tpl.id ? 'applying' : 'ready'}
                >
                  应用
                </Button>
              </Box>
            </Box>
          ))}
        </Box>

        {total > 20 && (
          <Box
            data-testid="template-library-pagination"
            data-contract-source="/live/template/search"
            data-page={page}
            data-total={total}
            sx={{ display: 'flex', justifyContent: 'center', gap: 1, mt: 2 }}
          >
            <Button
              size="small"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              data-testid="template-library-prev-page-button"
              data-contract-source="/live/template/search"
            >
              上一页
            </Button>
            <Typography variant="caption" sx={{ alignSelf: 'center' }}>
              {page + 1} / {Math.ceil(total / 20)}
            </Typography>
            <Button
              size="small"
              disabled={(page + 1) * 20 >= total}
              onClick={() => setPage((p) => p + 1)}
              data-testid="template-library-next-page-button"
              data-contract-source="/live/template/search"
            >
              下一页
            </Button>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} data-testid="template-library-close-button" data-contract-source="onClose-prop">关闭</Button>
      </DialogActions>
    </Dialog>
  )
})
