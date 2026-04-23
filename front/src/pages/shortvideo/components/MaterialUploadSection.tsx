import {
  Box,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import type { SvProject, SvScript } from '@/types/shortvideo'

interface RefImage {
  id: number
  url: string
}

export interface MaterialUploadSectionProps {
  projects: SvProject[]
  scripts: SvScript[]
  shotLists: Array<{ id: number; scriptId?: number; shotCount?: number }>
  selectedProjectId: number | ''
  selectedScriptId: number | ''
  selectedShotListId: number | ''
  characterRefUrl: string
  sceneRefUrl: string
  refImages: RefImage[]
  shotsCount: number
  onProjectChange: (projectId: number | '') => void
  onScriptChange: (scriptId: number | '') => void
  onShotListChange: (shotListId: number | '') => void
  onCharacterRefUrlChange: (url: string) => void
  onSceneRefUrlChange: (url: string) => void
}

export function MaterialUploadSection({
  projects,
  scripts,
  shotLists,
  selectedProjectId,
  selectedScriptId,
  selectedShotListId,
  characterRefUrl,
  sceneRefUrl,
  refImages,
  shotsCount,
  onProjectChange,
  onScriptChange,
  onShotListChange,
  onCharacterRefUrlChange,
  onSceneRefUrlChange,
}: MaterialUploadSectionProps) {
  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>
        选择项目或脚本（加载分镜）
      </Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 3 }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>项目</InputLabel>
          <Select
            value={projects.some((p) => p.id === selectedProjectId) ? selectedProjectId : ''}
            label="项目"
            onChange={(e) => onProjectChange(e.target.value as number | '')}
          >
            <MenuItem value="">无</MenuItem>
            {projects.map((p) => (
              <MenuItem key={String(p.id)} value={p.id}>
                {p.title ?? ''}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>或按脚本加载分镜</InputLabel>
          <Select
            value={scripts.some((s) => s.id === selectedScriptId) ? selectedScriptId : ''}
            label="或按脚本加载分镜"
            onChange={(e) => onScriptChange(e.target.value as number | '')}
          >
            <MenuItem value="">无</MenuItem>
            {scripts.map((s) => (
              <MenuItem key={String(s.id)} value={s.id}>
                {s.title ?? ''}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>或直接选分镜列表</InputLabel>
          <Select
            value={shotLists.some((sl) => sl.id === selectedShotListId) ? selectedShotListId : ''}
            label="或直接选分镜列表"
            onChange={(e) => onShotListChange(e.target.value as number | '')}
          >
            <MenuItem value="">无</MenuItem>
            {shotLists.map((sl) => (
              <MenuItem key={String(sl.id)} value={sl.id}>
                分镜 #{sl.id}（{sl.shotCount ?? 0} 镜）
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {shotsCount > 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          已加载 {shotsCount} 个分镜
        </Typography>
      )}

      {selectedProjectId && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2, alignItems: 'flex-start' }}>
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel>人物参考图</InputLabel>
            <Select
              value={characterRefUrl || ''}
              label="人物参考图"
              onChange={(e) => onCharacterRefUrlChange(e.target.value)}
            >
              <MenuItem value="">无</MenuItem>
              {characterRefUrl && !refImages.some((m) => m.url === characterRefUrl) && (
                <MenuItem value={characterRefUrl}>已上传（素材准备页）</MenuItem>
              )}
              {refImages.map((m) => (
                <MenuItem key={m.id} value={m.url}>
                  素材图 #{m.id}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel>场景参考图</InputLabel>
            <Select
              value={sceneRefUrl || ''}
              label="场景参考图"
              onChange={(e) => onSceneRefUrlChange(e.target.value)}
            >
              <MenuItem value="">无</MenuItem>
              {sceneRefUrl && !refImages.some((m) => m.url === sceneRefUrl) && (
                <MenuItem value={sceneRefUrl}>已上传（素材准备页）</MenuItem>
              )}
              {refImages.map((m) => (
                <MenuItem key={m.id} value={m.url}>
                  素材图 #{m.id}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center', pt: 1 }}>
            保持人物/场景一致，无 ComfyUI 时用可灵图生图
          </Typography>
        </Box>
      )}
    </Box>
  )
}
