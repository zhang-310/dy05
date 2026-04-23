/**
 * 短剧编辑器所有对话框（新建短剧、编辑短剧、添加/编辑剧集、添加/编辑角色、删除确认）
 */
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import type { SvDramaEpisode, SvDramaCharacter } from '@/api/shortvideo'

const GENRES = ['都市', '古装', '悬疑', '甜宠', '搞笑']

// ─── Create Drama ────────────────────────────────────────────────────────────

export interface CreateDramaDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onSubmit: () => void
  formTitle: string
  onFormTitleChange: (v: string) => void
  formDesc: string
  onFormDescChange: (v: string) => void
  formGenre: string
  onFormGenreChange: (v: string) => void
  formEpisodes: number
  onFormEpisodesChange: (v: number) => void
}

export function CreateDramaDialog({
  open,
  onClose,
  loading,
  onSubmit,
  formTitle,
  onFormTitleChange,
  formDesc,
  onFormDescChange,
  formGenre,
  onFormGenreChange,
  formEpisodes,
  onFormEpisodesChange,
}: CreateDramaDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>新建短剧</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="标题"
          value={formTitle}
          onChange={(e) => onFormTitleChange(e.target.value)}
          margin="dense"
          required
        />
        <TextField
          fullWidth
          label="简介"
          value={formDesc}
          onChange={(e) => onFormDescChange(e.target.value)}
          margin="dense"
          multiline
          rows={2}
        />
        <FormControl fullWidth margin="dense">
          <InputLabel>类型</InputLabel>
          <Select
            value={formGenre}
            label="类型"
            onChange={(e) => onFormGenreChange(e.target.value)}
          >
            {GENRES.map((g) => (
              <MenuItem key={g} value={g}>
                {g}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <TextField
          fullWidth
          type="number"
          label="计划集数"
          value={formEpisodes}
          onChange={(e) =>
            onFormEpisodesChange(parseInt(e.target.value, 10) || 1)
          }
          margin="dense"
          inputProps={{ min: 1 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          onClick={onSubmit}
          disabled={loading || !formTitle.trim()}
        >
          创建
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Edit Drama ──────────────────────────────────────────────────────────────

export interface EditDramaDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onSubmit: () => void
  title: string
  onTitleChange: (v: string) => void
  desc: string
  onDescChange: (v: string) => void
  genre: string
  onGenreChange: (v: string) => void
  episodes: number
  onEpisodesChange: (v: number) => void
}

export function EditDramaDialog({
  open,
  onClose,
  loading,
  onSubmit,
  title,
  onTitleChange,
  desc,
  onDescChange,
  genre,
  onGenreChange,
  episodes,
  onEpisodesChange,
}: EditDramaDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>编辑短剧</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="标题"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          margin="dense"
          required
        />
        <TextField
          fullWidth
          label="简介"
          value={desc}
          onChange={(e) => onDescChange(e.target.value)}
          margin="dense"
          multiline
          rows={2}
        />
        <FormControl fullWidth margin="dense">
          <InputLabel>类型</InputLabel>
          <Select
            value={genre}
            label="类型"
            onChange={(e) => onGenreChange(e.target.value)}
          >
            {GENRES.map((g) => (
              <MenuItem key={g} value={g}>
                {g}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <TextField
          fullWidth
          type="number"
          label="计划集数"
          value={episodes}
          onChange={(e) =>
            onEpisodesChange(parseInt(e.target.value, 10) || 1)
          }
          margin="dense"
          inputProps={{ min: 1 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          onClick={onSubmit}
          disabled={loading || !title.trim()}
        >
          保存
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Add Episode ─────────────────────────────────────────────────────────────

export interface AddEpisodeDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onSubmit: () => void
  title: string
  onTitleChange: (v: string) => void
  synopsis: string
  onSynopsisChange: (v: string) => void
  cliffhanger: string
  onCliffhangerChange: (v: string) => void
}

export function AddEpisodeDialog({
  open,
  onClose,
  loading,
  onSubmit,
  title,
  onTitleChange,
  synopsis,
  onSynopsisChange,
  cliffhanger,
  onCliffhangerChange,
}: AddEpisodeDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>添加剧集</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="标题"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          margin="dense"
          placeholder="第X集"
        />
        <TextField
          fullWidth
          label="剧情概要"
          value={synopsis}
          onChange={(e) => onSynopsisChange(e.target.value)}
          margin="dense"
          multiline
          rows={3}
        />
        <TextField
          fullWidth
          label="悬念/钩子"
          value={cliffhanger}
          onChange={(e) => onCliffhangerChange(e.target.value)}
          margin="dense"
          multiline
          rows={2}
          placeholder="留住观众看下一集"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={onSubmit} disabled={loading}>
          添加
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Edit Episode ────────────────────────────────────────────────────────────

export interface EditEpisodeDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onSubmit: () => void
  editingEpisode: SvDramaEpisode | null
  title: string
  onTitleChange: (v: string) => void
  synopsis: string
  onSynopsisChange: (v: string) => void
  cliffhanger: string
  onCliffhangerChange: (v: string) => void
}

export function EditEpisodeDialog({
  open,
  onClose,
  loading,
  onSubmit,
  editingEpisode,
  title,
  onTitleChange,
  synopsis,
  onSynopsisChange,
  cliffhanger,
  onCliffhangerChange,
}: EditEpisodeDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        编辑剧集{' '}
        {editingEpisode ? `第${editingEpisode.episodeNumber}集` : ''}
      </DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="标题"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          margin="dense"
          placeholder="第X集"
        />
        <TextField
          fullWidth
          label="剧情/剧本"
          value={synopsis}
          onChange={(e) => onSynopsisChange(e.target.value)}
          margin="dense"
          multiline
          rows={8}
        />
        <TextField
          fullWidth
          label="悬念/钩子"
          value={cliffhanger}
          onChange={(e) => onCliffhangerChange(e.target.value)}
          margin="dense"
          multiline
          rows={2}
          placeholder="留住观众看下一集"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={onSubmit} disabled={loading}>
          保存
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Add Character ───────────────────────────────────────────────────────────

export interface AddCharacterDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onSubmit: () => void
  name: string
  onNameChange: (v: string) => void
  desc: string
  onDescChange: (v: string) => void
}

export function AddCharacterDialog({
  open,
  onClose,
  loading,
  onSubmit,
  name,
  onNameChange,
  desc,
  onDescChange,
}: AddCharacterDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>添加角色</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="角色名"
          value={name}
          onChange={(e) => onNameChange(e.target.value)}
          margin="dense"
          required
        />
        <TextField
          fullWidth
          label="角色描述"
          value={desc}
          onChange={(e) => onDescChange(e.target.value)}
          margin="dense"
          multiline
          rows={2}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          onClick={onSubmit}
          disabled={loading || !name.trim()}
        >
          添加
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Edit Character ──────────────────────────────────────────────────────────

export interface EditCharacterDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onSubmit: () => void
  name: string
  onNameChange: (v: string) => void
  desc: string
  onDescChange: (v: string) => void
}

export function EditCharacterDialog({
  open,
  onClose,
  loading,
  onSubmit,
  name,
  onNameChange,
  desc,
  onDescChange,
}: EditCharacterDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>编辑角色</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="角色名"
          value={name}
          onChange={(e) => onNameChange(e.target.value)}
          margin="dense"
          required
        />
        <TextField
          fullWidth
          label="角色描述"
          value={desc}
          onChange={(e) => onDescChange(e.target.value)}
          margin="dense"
          multiline
          rows={2}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          onClick={onSubmit}
          disabled={loading || !name.trim()}
        >
          保存
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Delete Confirm Dialogs ──────────────────────────────────────────────────

export interface DeleteDramaDialogProps {
  open: boolean
  onClose: () => void
  loading: boolean
  onConfirm: () => void
  dramaTitle: string | undefined
}

export function DeleteDramaDialog({
  open,
  onClose,
  loading,
  onConfirm,
  dramaTitle,
}: DeleteDramaDialogProps) {
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>删除短剧</DialogTitle>
      <DialogContent>
        <Typography>
          确定要删除短剧「{dramaTitle}」吗？剧集与角色将一并删除，已关联的项目不受影响。
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={loading}
        >
          删除
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export interface DeleteEpisodeDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  deletingEpId: number | null
  epToDelete: SvDramaEpisode | null
}

export function DeleteEpisodeDialog({
  open,
  onClose,
  onConfirm,
  deletingEpId,
  epToDelete,
}: DeleteEpisodeDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={() => {
        if (!deletingEpId) onClose()
      }}
    >
      <DialogTitle>删除剧集</DialogTitle>
      <DialogContent>
        <Typography>
          确定要删除「第{epToDelete?.episodeNumber}集{' '}
          {epToDelete?.title || ''}」吗？
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={!!deletingEpId}>
          取消
        </Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={!!deletingEpId}
        >
          {deletingEpId ? '删除中…' : '删除'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export interface DeleteCharacterDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  deletingCharId: number | null
  charToDelete: SvDramaCharacter | null
}

export function DeleteCharacterDialog({
  open,
  onClose,
  onConfirm,
  deletingCharId,
  charToDelete,
}: DeleteCharacterDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={() => {
        if (!deletingCharId) onClose()
      }}
    >
      <DialogTitle>删除角色</DialogTitle>
      <DialogContent>
        <Typography>
          确定要删除角色「{charToDelete?.characterName}」吗？
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={!!deletingCharId}>
          取消
        </Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={!!deletingCharId}
        >
          {deletingCharId ? '删除中…' : '删除'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
