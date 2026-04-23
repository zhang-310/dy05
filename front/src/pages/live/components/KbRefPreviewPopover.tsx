import { memo, useState, useCallback, useEffect } from 'react'
import {
  Box,
  Button,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
  Popover,
  Switch,
  Typography,
} from '@mui/material'
import PreviewIcon from '@mui/icons-material/Preview'
import { aiApi } from '@/api/ai'
import type { KbSearchHit } from '@/api/ai'

interface KbRefItem {
  title?: string
  snippet?: string
  score?: number
  excluded?: boolean
}

export interface KbRefPreviewPopoverProps {
  /** 知识库 ID（默认使用第一个可用的） */
  kbId?: number
  /** 搜索 query — 场次标题 + 商品名组合 */
  query: string
  disabled?: boolean
}

export const KbRefPreviewPopover = memo(function KbRefPreviewPopover({
  kbId = 1,
  query,
  disabled,
}: KbRefPreviewPopoverProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null)
  const [loading, setLoading] = useState(false)
  const [items, setItems] = useState<KbRefItem[]>([])

  const handleOpen = useCallback((event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget)
  }, [])

  const handleClose = useCallback(() => {
    setAnchorEl(null)
  }, [])

  const open = Boolean(anchorEl)

  // Fetch when opened
  useEffect(() => {
    if (!open || !query.trim()) return
    setLoading(true)
    aiApi.kbSearch(kbId, { query, limit: 10 })
      .then((res) => {
        const data = res
        const docs: KbSearchHit[] = Array.isArray(data) ? data : Array.isArray((data as { results?: unknown })?.results) ? (data as { results: KbSearchHit[] }).results : []
        setItems(docs.map((d) => ({
          title: d.title ?? '未知文档',
          snippet: (d.content ?? '').slice(0, 120),
          score: d.score,
          excluded: false,
        })))
      })
      .catch(() => setItems([]))
      .finally(() => setLoading(false))
  }, [open, kbId, query])

  const toggleExclude = useCallback((index: number) => {
    setItems((prev) => prev.map((item, i) => i === index ? { ...item, excluded: !item.excluded } : item))
  }, [])

  return (
    <>
      <Button
        size="small"
        startIcon={<PreviewIcon />}
        onClick={handleOpen}
        disabled={disabled}
        variant="text"
      >
        预览引用
      </Button>
      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
        PaperProps={{ sx: { width: 400, maxHeight: 480 } }}
      >
        <Box sx={{ p: 2 }}>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            相关文档 ({items.filter((i) => !i.excluded).length})
          </Typography>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={24} />
            </Box>
          ) : items.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
              未找到相关文档
            </Typography>
          ) : (
            <List dense sx={{ maxHeight: 360, overflow: 'auto' }}>
              {items.map((item, idx) => (
                <ListItem
                  key={idx}
                  sx={{
                    opacity: item.excluded ? 0.5 : 1,
                    bgcolor: item.excluded ? 'action.disabledBackground' : 'transparent',
                    borderRadius: 1,
                    mb: 0.5,
                  }}
                  secondaryAction={
                    <Switch
                      size="small"
                      checked={!item.excluded}
                      onChange={() => toggleExclude(idx)}
                      title={item.excluded ? '已排除' : '包含'}
                    />
                  }
                >
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <Typography variant="body2" noWrap sx={{ flex: 1 }}>{item.title}</Typography>
                        {item.score != null && (
                          <Typography variant="caption" color="text.secondary">
                            {Math.round(item.score * 100)}%
                          </Typography>
                        )}
                      </Box>
                    }
                    secondary={item.snippet}
                    secondaryTypographyProps={{ variant: 'caption', noWrap: true }}
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Popover>
    </>
  )
})
