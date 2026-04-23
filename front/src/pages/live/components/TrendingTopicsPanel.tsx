import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Chip,
  CircularProgress,
  IconButton,
  Popover,
  Tooltip,
  Typography,
} from '@mui/material'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import RefreshIcon from '@mui/icons-material/Refresh'
import AddIcon from '@mui/icons-material/Add'
import { brainTrendsCurrent } from '@/api/brain'

interface TrendItem {
  id: string
  title: string
  category: string
  heatScore: number
  source: string
}

interface TrendingTopicsPanelProps {
  /** 一键注入关键词回调 */
  onInjectKeyword: (keyword: string) => void
  /** 已选中的热词 */
  selectedKeywords?: string[]
}

export function TrendingTopicsPanel({ onInjectKeyword, selectedKeywords = [] }: TrendingTopicsPanelProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const [trends, setTrends] = useState<TrendItem[]>([])
  const [loading, setLoading] = useState(false)

  const open = Boolean(anchorEl)

  const loadTrends = useCallback(async () => {
    setLoading(true)
    try {
      const data = await brainTrendsCurrent({ limit: 15 })
      setTrends(
        (data ?? []).map((b) => ({
          id: b.id,
          title: b.title,
          category: b.category,
          heatScore: b.heatScore,
          source: b.source,
        }))
      )
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (open && trends.length === 0) loadTrends()
  }, [open, trends.length, loadTrends])

  return (
    <>
      <Tooltip title="热点趋势 — 一键注入到话术">
        <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)} color="warning">
          <TrendingUpIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        slotProps={{ paper: { sx: { width: 280, maxHeight: 400 } } }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 1.5, py: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <TrendingUpIcon fontSize="small" color="warning" />
            <Typography variant="subtitle2">热点趋势</Typography>
          </Box>
          <IconButton size="small" onClick={loadTrends} disabled={loading}>
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Box>
        <Box sx={{ maxHeight: 320, overflow: 'auto', px: 1, pb: 1 }}>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={20} />
            </Box>
          ) : trends.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
              暂无热点数据
            </Typography>
          ) : (
            trends.map((t) => {
              const isSelected = selectedKeywords.includes(t.title)
              return (
                <Box
                  key={t.id}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    py: 0.5,
                    px: 0.5,
                    borderRadius: 1,
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                >
                  <Box sx={{ minWidth: 0, flex: 1 }}>
                    <Typography variant="body2" noWrap sx={{ fontSize: '0.8rem' }}>
                      {t.title}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      <Chip label={t.category} size="small" sx={{ height: 16, fontSize: '0.65rem' }} />
                      <Typography variant="caption" color="text.disabled">
                        {t.heatScore >= 1000
                          ? t.heatScore > 10000
                            ? `${(t.heatScore / 10000).toFixed(1)}万`
                            : Math.round(t.heatScore)
                          : t.heatScore.toFixed(2)}
                      </Typography>
                    </Box>
                  </Box>
                  <Tooltip title={isSelected ? '已添加' : '注入到热搜词'}>
                    <IconButton
                      size="small"
                      onClick={() => onInjectKeyword(t.title)}
                      disabled={isSelected}
                      color={isSelected ? 'success' : 'primary'}
                    >
                      <AddIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </Box>
              )
            })
          )}
        </Box>
      </Popover>
    </>
  )
}
