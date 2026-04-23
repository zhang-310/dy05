/**
 * 可拖拽分镜卡片：配合 @dnd-kit 实现拖拽排序，支持批量选择
 */
import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Box, Checkbox } from '@mui/material'
import { DragIndicator as DragIcon } from '@mui/icons-material'
import { ShotCard, type ShotCardData } from './ShotCard'
import type { CameraTypeCode } from './CameraControlPanel'

interface SortableShotCardProps {
  id: string
  shot: ShotCardData
  onMoveUp?: () => void
  onMoveDown?: () => void
  onCameraTypeChange?: (code: CameraTypeCode) => void
  canMoveUp?: boolean
  canMoveDown?: boolean
  selected?: boolean
  onSelectChange?: (checked: boolean) => void
}

export function SortableShotCard({
  id,
  shot,
  onMoveUp,
  onMoveDown,
  onCameraTypeChange,
  canMoveUp = true,
  canMoveDown = true,
  selected,
  onSelectChange,
}: SortableShotCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <Box ref={setNodeRef} style={style} sx={{ display: 'flex', alignItems: 'flex-start', gap: 0.5 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        {onSelectChange != null && (
          <Checkbox
            size="small"
            checked={!!selected}
            onChange={(e) => onSelectChange(e.target.checked)}
            onClick={(e) => e.stopPropagation()}
          />
        )}
        <Box
          {...attributes}
          {...listeners}
          sx={{
            cursor: 'grab',
            display: 'flex',
            alignItems: 'center',
            py: 2,
            color: 'text.secondary',
            '&:active': { cursor: 'grabbing' },
          }}
        >
          <DragIcon fontSize="small" />
        </Box>
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <ShotCard
          shot={shot}
          onMoveUp={onMoveUp}
          onMoveDown={onMoveDown}
          onCameraTypeChange={onCameraTypeChange}
          canMoveUp={canMoveUp}
          canMoveDown={canMoveDown}
        />
      </Box>
    </Box>
  )
}
