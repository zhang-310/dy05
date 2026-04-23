/**
 * 剧集列表组件
 */
import {
  Box,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Button,
  Chip,
  Collapse,
} from '@mui/material'
import { Videocam as ProduceIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material'
import type { SvDramaEpisode } from '@/api/shortvideo'

export interface EpisodeListProps {
  episodes: SvDramaEpisode[]
  episodesWithSynopsis: number
  expandedEpisodeId: number | null
  onExpandToggle: (id: number | null) => void
  onEdit: (ep: SvDramaEpisode) => void
  onDelete: (ep: SvDramaEpisode) => void
  onStartProduce: (ep: SvDramaEpisode) => void
  producingEpisodeId: number | null
  deletingEpId: number | null
  navigateToShotList: (projectId: number) => void
}

export function EpisodeList({
  episodes,
  episodesWithSynopsis,
  expandedEpisodeId,
  onExpandToggle,
  onEdit,
  onDelete,
  onStartProduce,
  producingEpisodeId,
  deletingEpId,
  navigateToShotList,
}: EpisodeListProps) {
  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="subtitle2">剧集 ({episodes.length})</Typography>
        {episodes.length > 0 && episodesWithSynopsis === 0 && (
          <Typography variant="caption" color="text.secondary">
            请先生成剧本并应用到剧集
          </Typography>
        )}
      </Box>
      <List dense>
        {episodes.map((ep) => (
          <ListItem key={ep.id} disablePadding sx={{ flexDirection: 'column', alignItems: 'stretch' }}>
            <ListItemButton onClick={() => onExpandToggle(expandedEpisodeId === ep.id ? null : ep.id ?? null)}>
              <ListItemText
                primary={`第${ep.episodeNumber}集 ${ep.title || '(无标题)'}`}
                secondary={
                  ep.synopsis ? ep.synopsis.slice(0, 80) + (ep.synopsis.length > 80 ? '...' : '') : '暂无剧情'
                }
              />
              {ep.projectId ? (
                <Chip
                  size="small"
                  label="已关联项目"
                  color="success"
                  sx={{ ml: 1, cursor: 'pointer' }}
                  onClick={(e) => {
                    e.stopPropagation()
                    if (ep.projectId) navigateToShotList(ep.projectId)
                  }}
                />
              ) : ep.synopsis?.trim() ? (
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<ProduceIcon />}
                  onClick={(e) => {
                    e.stopPropagation()
                    onStartProduce(ep)
                  }}
                  disabled={!!producingEpisodeId}
                  sx={{ ml: 1 }}
                >
                  {producingEpisodeId === ep.id ? '正在生成分镜…' : '开始制作'}
                </Button>
              ) : null}
            </ListItemButton>
            <Collapse in={expandedEpisodeId === ep.id}>
              <Box
                sx={{
                  pl: 2,
                  pr: 2,
                  pb: 1,
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                }}
              >
                <Typography
                  variant="body2"
                  component="pre"
                  sx={{ whiteSpace: 'pre-wrap', fontSize: 12, flex: 1 }}
                >
                  {ep.synopsis || '—'}
                </Typography>
                {!ep.projectId && (
                  <Box sx={{ display: 'flex', gap: 0.5, ml: 1 }}>
                    <Button
                      size="small"
                      startIcon={<EditIcon />}
                      onClick={(e) => {
                        e.stopPropagation()
                        onEdit(ep)
                      }}
                    >
                      编辑
                    </Button>
                    <Button
                      size="small"
                      color="error"
                      startIcon={<DeleteIcon />}
                      onClick={(e) => {
                        e.stopPropagation()
                        onDelete(ep)
                      }}
                      disabled={!!deletingEpId}
                    >
                      {deletingEpId === ep.id ? '删除中…' : '删除'}
                    </Button>
                  </Box>
                )}
              </Box>
            </Collapse>
          </ListItem>
        ))}
        {episodes.length === 0 && (
          <ListItem>
            <ListItemText primary="暂无剧集" />
          </ListItem>
        )}
      </List>
    </Box>
  )
}
