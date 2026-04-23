import { Box, Button, Typography } from '@mui/material'

/** 运镜类型定义 - 与后端 CameraType 枚举严格对齐 (24 种) */
export const CAMERA_TYPES = [
  // 基础运镜 (7)
  { code: 'static', label: '固定', icon: '📌', category: 'basic' },
  { code: 'zoom-in', label: '推镜头', icon: '🔍', category: 'basic' },
  { code: 'zoom-out', label: '拉镜头', icon: '🔭', category: 'basic' },
  { code: 'pan-left', label: '左摇', icon: '⬅️', category: 'basic' },
  { code: 'pan-right', label: '右摇', icon: '➡️', category: 'basic' },
  { code: 'tilt-up', label: '上仰', icon: '⬆️', category: 'basic' },
  { code: 'tilt-down', label: '下俯', icon: '⬇️', category: 'basic' },
  // 专业运镜 (8)
  { code: 'dolly-in', label: '推轨推进', icon: '🎥', category: 'pro' },
  { code: 'dolly-out', label: '推轨拉远', icon: '🎬', category: 'pro' },
  { code: 'crane-up', label: '摇臂上升', icon: '🏗️', category: 'pro' },
  { code: 'crane-down', label: '摇臂下降', icon: '⬇️', category: 'pro' },
  { code: 'orbit', label: '环绕', icon: '🔄', category: 'pro' },
  { code: 'tracking', label: '跟踪', icon: '🏃', category: 'pro' },
  { code: 'steadicam', label: '斯坦尼康', icon: '🎯', category: 'pro' },
  { code: 'handheld', label: '手持', icon: '✋', category: 'pro' },
  { code: 'whip-pan', label: '快速横摇', icon: '💨', category: 'pro' },
  { code: 'dutch-angle', label: '荷兰角', icon: '📐', category: 'pro' },
  // 电影级运镜 (9)
  { code: 'dolly-zoom', label: '希区柯克', icon: '😵', category: 'cinema' },
  { code: 'drone-aerial', label: '航拍', icon: '🚁', category: 'cinema' },
  { code: 'pov', label: '第一人称', icon: '👁️', category: 'cinema' },
  { code: 'over-shoulder', label: '过肩', icon: '🎭', category: 'cinema' },
  { code: 'rack-focus', label: '焦点转移', icon: '🔬', category: 'cinema' },
  { code: 'push-in', label: '缓慢靠近', icon: '🔎', category: 'cinema' },
  { code: 'pull-out', label: '缓慢远离', icon: '🌐', category: 'cinema' },
] as const

export type CameraTypeCode = (typeof CAMERA_TYPES)[number]['code']

interface CameraControlPanelProps {
  value: CameraTypeCode
  onChange: (code: CameraTypeCode) => void
}

const CATEGORIES = [
  { key: 'basic', label: '基础运镜' },
  { key: 'pro', label: '专业运镜' },
  { key: 'cinema', label: '电影运镜' },
] as const

export default function CameraControlPanel({ value, onChange }: CameraControlPanelProps) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {CATEGORIES.map((cat) => (
        <Box key={cat.key}>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
            {cat.label}
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {CAMERA_TYPES.filter((t) => t.category === cat.key).map((t) => (
              <Button
                key={t.code}
                variant={value === t.code ? 'contained' : 'outlined'}
                size="small"
                onClick={() => onChange(t.code)}
                sx={{ minWidth: 'auto', textTransform: 'none' }}
              >
                {t.icon} {t.label}
              </Button>
            ))}
          </Box>
        </Box>
      ))}
    </Box>
  )
}
