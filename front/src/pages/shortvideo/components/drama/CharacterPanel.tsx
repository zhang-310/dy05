/**
 * 角色面板组件
 */
import { Box, Typography, List, ListItem, ListItemText, Button } from '@mui/material'
import type { SvDramaCharacter } from '@/api/shortvideo'

export interface CharacterPanelProps {
  characters: SvDramaCharacter[]
  onEdit: (c: SvDramaCharacter) => void
  onDelete: (c: SvDramaCharacter) => void
  deletingCharId: number | null
}

export function CharacterPanel({
  characters,
  onEdit,
  onDelete,
  deletingCharId,
}: CharacterPanelProps) {
  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>
        角色 ({characters.length})
      </Typography>
      <List dense>
        {characters.map((c) => (
          <ListItem
            key={c.id}
            secondaryAction={
              <Box sx={{ display: 'flex', gap: 0.5 }}>
                <Button size="small" onClick={() => onEdit(c)}>
                  编辑
                </Button>
                <Button
                  size="small"
                  color="error"
                  onClick={() => onDelete(c)}
                  disabled={!!deletingCharId}
                >
                  {deletingCharId === c.id ? '删除中…' : '删除'}
                </Button>
              </Box>
            }
          >
            <ListItemText
              primary={c.characterName}
              secondary={
                c.description
                  ? c.description.slice(0, 40) + (c.description.length > 40 ? '...' : '')
                  : null
              }
            />
          </ListItem>
        ))}
        {characters.length === 0 && (
          <ListItem>
            <ListItemText primary="暂无角色" />
          </ListItem>
        )}
      </List>
    </Box>
  )
}
