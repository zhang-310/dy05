import {
  Box,
  Typography,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  InputAdornment,
} from '@mui/material'
import QualitySelector from '@/components/shortvideo/QualitySelector'
import { CAMERA_TYPES } from '@/components/shortvideo/CameraControlPanel'

export const NODE_LABELS: Record<string, string> = {
  script: '脚本',
  shotList: '分镜',
  keyframe: '关键帧',
  videoGen: '视频',
  postProcess: '后期',
  musicGen: 'BGM',
  sfxGen: '音效',
  digitalHuman: '数字人',
  compose: '合成',
  publish: '发布',
}

export interface WorkflowParamsMap {
  script?: { theme?: string; style?: string; duration?: number }
  shotList?: { style?: string; shotCount?: number }
  keyframe?: { style?: string }
  videoGen?: {
    duration?: number
    motion?: string
    quality?: string
    aspectRatio?: string
    defaultCamera?: string
  }
  musicGen?: { style?: string; duration?: number }
  sfxGen?: { sceneDesc?: string }
  digitalHuman?: { avatarId?: string }
  compose?: { bgmUrl?: string; sfxUrls?: string[] }
}

const STYLE_OPTIONS = ['温馨', '搞笑', '励志', '悬疑', '治愈', '热血', '文艺', '科技感']
const ASPECT_OPTIONS = [
  { value: '9:16', label: '9:16 竖屏' },
  { value: '16:9', label: '16:9 横屏' },
]

export type WorkflowParamsValue = WorkflowParamsMap[keyof WorkflowParamsMap]

interface Props {
  nodeId: string
  params: WorkflowParamsValue
  onChange: (params: WorkflowParamsValue) => void
}

export default function WorkflowParamsPanel({ nodeId, params, onChange }: Props) {
  const get = <T,>(key: string, fallback: T): T =>
    ((params ?? {}) as Record<string, unknown>)[key] as T ?? fallback
  const set = (key: string, value: unknown) => onChange({ ...(params ?? {}), [key]: value } as WorkflowParamsValue)

  switch (nodeId) {
    case 'script':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            size="small"
            label="主题"
            value={get<string>('theme', '')}
            onChange={(e) => set('theme', e.target.value)}
            placeholder="如：家常菜、旅行vlog"
          />
          <FormControl size="small" fullWidth>
            <InputLabel>风格</InputLabel>
            <Select
              value={get<string>('style', '温馨')}
              label="风格"
              onChange={(e) => set('style', e.target.value)}
            >
              {STYLE_OPTIONS.map((s) => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            size="small"
            type="number"
            label="时长"
            value={get<number>('duration', 30)}
            onChange={(e) => set('duration', parseInt(e.target.value, 10) || 30)}
            InputProps={{ endAdornment: <InputAdornment position="end">秒</InputAdornment> }}
            inputProps={{ min: 15, max: 120 }}
          />
        </Box>
      )

    case 'shotList':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <FormControl size="small" fullWidth>
            <InputLabel>风格</InputLabel>
            <Select
              value={get<string>('style', '温馨')}
              label="风格"
              onChange={(e) => set('style', e.target.value)}
            >
              {STYLE_OPTIONS.map((s) => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            size="small"
            type="number"
            label="分镜数量"
            value={get<number>('shotCount', 6)}
            onChange={(e) => set('shotCount', parseInt(e.target.value, 10) || 6)}
            inputProps={{ min: 3, max: 20 }}
          />
        </Box>
      )

    case 'keyframe':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <FormControl size="small" fullWidth>
            <InputLabel>画面风格</InputLabel>
            <Select
              value={get<string>('style', '温馨')}
              label="画面风格"
              onChange={(e) => set('style', e.target.value)}
            >
              {STYLE_OPTIONS.map((s) => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      )

    case 'videoGen':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            size="small"
            type="number"
            label="单镜时长"
            value={get<number>('duration', 5)}
            onChange={(e) => set('duration', parseInt(e.target.value, 10) || 5)}
            InputProps={{ endAdornment: <InputAdornment position="end">秒</InputAdornment> }}
            inputProps={{ min: 3, max: 10 }}
          />
          <FormControl size="small" fullWidth>
            <InputLabel>默认运镜</InputLabel>
            <Select
              value={get<string>('defaultCamera', 'zoom-in')}
              label="默认运镜"
              onChange={(e) => set('defaultCamera', e.target.value)}
            >
              {CAMERA_TYPES.map((t) => (
                <MenuItem key={t.code} value={t.code}>{t.icon} {t.label}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Box>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>画质</Typography>
            <QualitySelector
              value={String(get<string>('quality', 'premium-fhd'))}
              onChange={(v) => set('quality', v)}
            />
          </Box>
          <FormControl size="small" fullWidth>
            <InputLabel>画面比例</InputLabel>
            <Select
              value={get<string>('aspectRatio', '9:16')}
              label="画面比例"
              onChange={(e) => set('aspectRatio', e.target.value)}
            >
              {ASPECT_OPTIONS.map((a) => (
                <MenuItem key={a.value} value={a.value}>{a.label}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      )

    case 'musicGen':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            size="small"
            label="风格描述"
            value={get<string>('style', '')}
            onChange={(e) => set('style', e.target.value)}
            placeholder="如：温馨、悬疑"
          />
          <TextField
            size="small"
            type="number"
            label="时长(秒)"
            value={get<number>('duration', 30)}
            onChange={(e) => set('duration', parseInt(e.target.value, 10) || 30)}
          />
        </Box>
      )

    case 'sfxGen':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            size="small"
            label="场景描述"
            value={get<string>('sceneDesc', '')}
            onChange={(e) => set('sceneDesc', e.target.value)}
            placeholder="如：雨声、脚步声"
          />
        </Box>
      )

    case 'digitalHuman':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            size="small"
            label="Avatar ID"
            value={get<string>('avatarId', '')}
            onChange={(e) => set('avatarId', e.target.value)}
            placeholder="HeyGen Avatar ID"
          />
        </Box>
      )

    case 'compose':
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            size="small"
            label="BGM 地址"
            value={get<string>('bgmUrl', '')}
            onChange={(e) => set('bgmUrl', e.target.value)}
            placeholder="可选，留空则无 BGM"
          />
        </Box>
      )

    case 'postProcess':
    case 'publish':
      return <Typography variant="body2" color="text.secondary">此节点无可调参数</Typography>

    default:
      return null
  }
}

export const DEFAULT_WORKFLOW_PARAMS: WorkflowParamsMap = {
  script: { theme: '', style: '温馨', duration: 30 },
  shotList: { style: '温馨', shotCount: 6 },
  keyframe: { style: '温馨' },
  videoGen: { duration: 5, defaultCamera: 'zoom-in', quality: 'premium-fhd', aspectRatio: '9:16' },
  musicGen: { style: '温馨', duration: 30 },
  sfxGen: { sceneDesc: '' },
  digitalHuman: { avatarId: '' },
  compose: { bgmUrl: '' },
}
