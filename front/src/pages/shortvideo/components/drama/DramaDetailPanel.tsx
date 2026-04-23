/**
 * 短剧详情面板 — 标题、流程进度条、操作按钮区
 */
import { Box, Typography, Button, Chip, Card, CardContent } from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material'
import type { SvDrama } from '@/api/shortvideo'

export interface FlowStep {
  id: number
  label: string
  done: boolean
}

export interface DramaDetailPanelProps {
  selectedDrama: SvDrama | undefined
  flowSteps: FlowStep[]
  episodesWithSynopsis: number
  episodesWithProject: number
  onEditDrama: () => void
  onDeleteDrama: () => void
  onAddEpisode: () => void
  onAddCharacter: () => void
  /** Children slots for sub-sections (script generator, episode list, etc.) */
  children: React.ReactNode
}

export function DramaDetailPanel({
  selectedDrama,
  flowSteps,
  episodesWithSynopsis,
  episodesWithProject,
  onEditDrama,
  onDeleteDrama,
  onAddEpisode,
  onAddCharacter,
  children,
}: DramaDetailPanelProps) {
  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            flexWrap: 'wrap',
            gap: 1,
          }}
        >
          <Box>
            <Typography variant="h6" gutterBottom>
              {selectedDrama?.title ?? '短剧详情'}
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {selectedDrama?.genre ?? '-'} · 共{' '}
              {selectedDrama?.totalEpisodes ?? 0} 集 ·{' '}
              {selectedDrama?.status ?? 'draft'}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<EditIcon />}
              onClick={onEditDrama}
            >
              编辑短剧
            </Button>
            <Button
              size="small"
              color="error"
              variant="outlined"
              startIcon={<DeleteIcon />}
              onClick={onDeleteDrama}
            >
              删除短剧
            </Button>
          </Box>
        </Box>
        {selectedDrama?.description && (
          <Typography variant="body2" sx={{ mb: 2 }}>
            {selectedDrama.description}
          </Typography>
        )}
        <Box
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 0.5,
            mb: 2,
            alignItems: 'center',
          }}
        >
          {flowSteps.map((s) => (
            <Chip
              key={s.id}
              size="small"
              label={`${s.id}. ${s.label}`}
              color={s.done ? 'success' : 'default'}
              variant={s.done ? 'filled' : 'outlined'}
              sx={{ fontSize: 11 }}
            />
          ))}
          {episodesWithSynopsis > 0 && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ ml: 1 }}
            >
              {episodesWithSynopsis} 集有剧情 · {episodesWithProject} 集已制作
            </Typography>
          )}
        </Box>
        <Box
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 1,
            mb: 2,
            alignItems: 'center',
          }}
        >
          <Button
            size="small"
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={onAddEpisode}
          >
            添加剧集
          </Button>
          <Button
            size="small"
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={onAddCharacter}
          >
            添加角色
          </Button>
        </Box>
        {children}
      </CardContent>
    </Card>
  )
}

/** Empty state shown when no drama is selected */
export function DramaEmptyState() {
  return (
    <Card>
      <CardContent>
        <Typography color="text.secondary">
          请从左侧选择或新建一个短剧
        </Typography>
      </CardContent>
    </Card>
  )
}
