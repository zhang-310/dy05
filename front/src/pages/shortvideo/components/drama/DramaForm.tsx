/**
 * 短剧表单（新建/编辑）对话框
 */
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import type { DramaFormState } from '@/hooks/useDramaEditor'

const GENRES = ['都市', '古装', '悬疑', '甜宠', '搞笑']

export interface DramaFormProps {
  open: boolean
  onClose: () => void
  form: DramaFormState & { set: (p: Partial<DramaFormState>) => void; reset: () => void; load?: (p: DramaFormState) => void }
  loading: boolean
  onSubmit: () => void
  mode: 'create' | 'edit'
}

export function DramaForm({ open, onClose, form, loading, onSubmit, mode }: DramaFormProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{mode === 'create' ? '新建短剧' : '编辑短剧'}</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          label="标题"
          value={form.title}
          onChange={(e) => form.set({ title: e.target.value })}
          margin="dense"
          required
        />
        <TextField
          fullWidth
          label="简介"
          value={form.desc}
          onChange={(e) => form.set({ desc: e.target.value })}
          margin="dense"
          multiline
          rows={2}
        />
        <FormControl fullWidth margin="dense">
          <InputLabel>类型</InputLabel>
          <Select value={form.genre} label="类型" onChange={(e) => form.set({ genre: e.target.value })}>
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
          value={form.episodes}
          onChange={(e) => form.set({ episodes: parseInt(e.target.value, 10) || 1 })}
          margin="dense"
          inputProps={{ min: 1 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={onSubmit} disabled={loading || !form.title.trim()}>
          {mode === 'create' ? '创建' : '保存'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
