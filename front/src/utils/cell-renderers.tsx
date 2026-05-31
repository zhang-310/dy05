import type { ReactNode } from 'react'
import { Box, Chip, LinearProgress, Typography } from '@mui/material'
import BrokenImageOutlinedIcon from '@mui/icons-material/BrokenImageOutlined'
import type { GridRenderCellParams } from '@mui/x-data-grid'
import { formatMoney } from '@/utils/format'
import type { StatusChipDef } from '@/utils/status-chips'

function cellString(value: unknown): string {
  if (value == null) return '-'
  if (typeof value === 'object' && value !== null && 'createTime' in value) {
    interface CellTimeObj { createTime?: unknown; [key: string]: unknown }
    const t = (value as CellTimeObj).createTime
    if (typeof t === 'string') return t.length > 19 ? t.slice(0, 19) : t
  }
  const s = String(value)
  return s.length > 80 ? `${s.slice(0, 80)}…` : s
}

/** 状态 Chip（value 为 statusMap 的 key） */
export function renderStatusChip(
  statusMap: Record<string, StatusChipDef>,
): (params: GridRenderCellParams) => ReactNode {
  return function StatusChipCell(params: GridRenderCellParams) {
    const key = params.value == null ? '' : String(params.value)
    const def = statusMap[key]
    if (!def) return <Chip size="small" label={key || '-'} variant="outlined" />
    return <Chip size="small" label={def.label} color={def.color} variant="outlined" />
  }
}

/** 金额（¥ + 千分位） */
export function renderCurrency(params: GridRenderCellParams): ReactNode {
  return <Typography variant="body2">{formatMoney(params.value)}</Typography>
}

/** 日期时间展示（截断 ISO 字符串到分钟） */
export function renderDateTime(params: GridRenderCellParams): ReactNode {
  const v = params.value
  if (v == null) return '-'
  const s = String(v)
  if (s.length >= 16) return s.slice(0, 16).replace('T', ' ')
  return s
}

/** 布尔值 Chip */
export function renderBooleanChip(
  trueLabel = '是',
  falseLabel = '否',
): (params: GridRenderCellParams) => ReactNode {
  return function BooleanChipCell(params: GridRenderCellParams) {
    const on = params.value === true || params.value === 1 || params.value === '1'
    return (
      <Chip
        size="small"
        label={on ? trueLabel : falseLabel}
        color={on ? 'success' : 'default'}
        variant="outlined"
      />
    )
  }
}

/** CDN 缩略图 */
export function renderThumbnail(params: GridRenderCellParams): ReactNode {
  const url = params.value
  if (!url || typeof url !== 'string') {
    return (
      <Box sx={{ width: 40, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'action.hover', borderRadius: 0.5 }}>
        <BrokenImageOutlinedIcon fontSize="small" color="disabled" />
      </Box>
    )
  }
  return (
    <Box
      component="img"
      src={url + '@!80X80'}
      alt=""
      sx={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 0.5 }}
    />
  )
}

/** 进度条（value 为 0~1 或 0~100） */
export function renderProgress(params: GridRenderCellParams): ReactNode {
  const raw = Number(params.value)
  if (!Number.isFinite(raw)) return '-'
  const pct = raw <= 1 ? Math.round(raw * 100) : Math.min(100, Math.round(raw))
  return (
    <Box sx={{ width: '100%', minWidth: 80 }}>
      <LinearProgress variant="determinate" value={pct} sx={{ height: 6, borderRadius: 1 }} />
      <Typography variant="caption" color="text.secondary">{pct}%</Typography>
    </Box>
  )
}

/** 纯文本单元格（截断 + 空值处理） */
export function renderPlainCell(params: GridRenderCellParams): ReactNode {
  return cellString(params.value)
}
