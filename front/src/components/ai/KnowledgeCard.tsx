import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
} from '@mui/material'
import {
  Inventory as BoxIcon,
  Description as DocIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
  Settings as SettingsIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material'
import type { ReactNode } from 'react'

export interface KnowledgeCardKb {
  id?: number | string
  kbName?: string
  status?: number
  totalDocuments?: number
  updateTime?: string | number
  [key: string]: unknown
}

export interface KnowledgeCardProps {
  kb: KnowledgeCardKb
  onSearch?: () => void
  onImport?: () => void
  onSettings?: () => void
  onDelete?: () => void
  onDocuments?: () => void
  extraActions?: ReactNode
  statsText?: ReactNode
}

export function KnowledgeCard({
  kb,
  onSearch,
  onImport,
  onSettings,
  onDelete,
  onDocuments,
  extraActions,
  statsText,
}: KnowledgeCardProps) {
  const docCount = Number(kb.totalDocuments ?? 0)
  const isReady = kb.status === 1

  return (
    <Card variant="outlined" sx={{ overflow: 'hidden' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <BoxIcon sx={{ color: 'primary.main', fontSize: 24 }} />
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              {String(kb.kbName ?? '-')}
            </Typography>
            <Chip
              label={isReady ? '就绪' : '构建中'}
              size="small"
              color={isReady ? 'success' : 'default'}
            />
          </Box>
          {extraActions}
        </Box>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          包含 {docCount.toLocaleString()} 个文档
        </Typography>

        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: statsText ? 2 : 0 }}>
          {onDocuments != null && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<DocIcon fontSize="small" />}
              onClick={onDocuments}
            >
              {docCount} 文档
            </Button>
          )}
          {onSearch != null && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<SearchIcon fontSize="small" />}
              onClick={onSearch}
            >
              搜索
            </Button>
          )}
          {onImport != null && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<UploadIcon fontSize="small" />}
              onClick={onImport}
            >
              导入
            </Button>
          )}
          {onSettings != null && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<SettingsIcon fontSize="small" />}
              onClick={onSettings}
            >
              设置
            </Button>
          )}
          {onDelete != null && (
            <Button
              size="small"
              color="error"
              variant="outlined"
              startIcon={<DeleteIcon fontSize="small" />}
              onClick={onDelete}
            >
              删除
            </Button>
          )}
        </Box>

        {statsText != null && (
          <Box sx={{ bgcolor: 'action.hover', borderRadius: 1, px: 1.5, py: 1 }}>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              📊 使用统计：{statsText}
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}
