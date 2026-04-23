/**
 * 左侧短剧列表侧边栏
 */
import {
  Card,
  CardContent,
  Typography,
  Button,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
} from '@mui/material'
import { Add as AddIcon } from '@mui/icons-material'
import type { SvDrama } from '@/api/shortvideo'

export interface DramaSidebarProps {
  dramaList: SvDrama[]
  selectedDramaId: number | null
  onSelectDrama: (id: number | null) => void
  onCreateClick: () => void
}

export function DramaSidebar({
  dramaList,
  selectedDramaId,
  onSelectDrama,
  onCreateClick,
}: DramaSidebarProps) {
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle2" gutterBottom>
          我的短剧
        </Typography>
        <Button
          fullWidth
          variant="outlined"
          startIcon={<AddIcon />}
          onClick={onCreateClick}
          sx={{ mb: 2 }}
        >
          新建短剧
        </Button>
        <List dense>
          {dramaList.map((d) => (
            <ListItem key={d.id} disablePadding>
              <ListItemButton
                selected={selectedDramaId === d.id}
                onClick={() => onSelectDrama(d.id ?? null)}
              >
                <ListItemText
                  primary={d.title}
                  secondary={
                    (d.episodesWithSynopsis ?? 0) > 0 ||
                    (d.episodesWithProject ?? 0) > 0
                      ? `${d.genre ?? '-'} · ${d.totalEpisodes ?? 0}集 · ${d.episodesWithSynopsis ?? 0}有剧情 ${d.episodesWithProject ?? 0}已制作`
                      : `${d.genre ?? '-'} · ${d.totalEpisodes ?? 0}集`
                  }
                />
              </ListItemButton>
            </ListItem>
          ))}
          {dramaList.length === 0 && (
            <ListItem>
              <ListItemText primary="暂无短剧，点击上方新建" />
            </ListItem>
          )}
        </List>
      </CardContent>
    </Card>
  )
}
