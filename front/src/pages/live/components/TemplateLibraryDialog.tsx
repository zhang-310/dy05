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

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await searchTemplates({
        scriptType: scriptType || undefined,
        keyword: keyword || undefined,
        page,
        rows: 20,
      })
      setTemplates(res.list ?? [])
      setTotal(res.total ?? 0)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [scriptType, keyword, page])

  useEffect(() => {
    if (open) load()
  }, [open, load])

  const handleApply = async (tpl: ScriptTemplate) => {
    setApplyingId(tpl.id)
    try {
      const res = await applyTemplate(tpl.id)
      onApply(res.content, res.scriptType)
      onClose()
    } catch {
      // ignore
    } finally {
      setApplyingId(null)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>话术模板库</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', gap: 1, mb: 2, mt: 0.5 }}>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>话术类型</InputLabel>
            <Select value={scriptType} label="话术类型" onChange={(e) => { setScriptType(e.target.value); setPage(0) }}>
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
            sx={{ flex: 1 }}
          />
          <Button size="small" variant="outlined" onClick={load} disabled={loading}>
            搜索
          </Button>
        </Box>

        {loading && <Typography color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>加载中...</Typography>}

        {!loading && templates.length === 0 && (
          <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            暂无模板
          </Typography>
        )}

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {templates.map((tpl) => (
            <Box
              key={tpl.id}
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
                  label={SCRIPT_TYPE_LABEL[tpl.scriptType] ?? tpl.scriptType}
                  size="small"
                  variant="outlined"
                  sx={{ height: 20, fontSize: '0.7rem' }}
                />
                {tpl.autoCollected === 1 && (
                  <Chip label="自动沉淀" size="small" color="info" icon={<AutoAwesomeIcon />} sx={{ height: 20, fontSize: '0.7rem' }} />
                )}
                {tpl.effectivenessScore > 0 && (
                  <Chip
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
                >
                  应用
                </Button>
              </Box>
            </Box>
          ))}
        </Box>

        {total > 20 && (
          <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mt: 2 }}>
            <Button size="small" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>上一页</Button>
            <Typography variant="caption" sx={{ alignSelf: 'center' }}>
              {page + 1} / {Math.ceil(total / 20)}
            </Typography>
            <Button size="small" disabled={(page + 1) * 20 >= total} onClick={() => setPage((p) => p + 1)}>下一页</Button>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>关闭</Button>
      </DialogActions>
    </Dialog>
  )
})
