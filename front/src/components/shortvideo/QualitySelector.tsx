import { Box, Button, Typography } from '@mui/material'

export const QUALITY_LEVELS = [
  { code: 'fast-sd', label: 'SD 480p', desc: '快速预览', cost: '$' },
  { code: 'standard-hd', label: 'HD 720p', desc: '标准质量', cost: '$$' },
  { code: 'premium-fhd', label: 'FHD 1080p', desc: '高清 (推荐)', cost: '$$$' },
  { code: 'cinema-4k', label: '4K 2160p', desc: '电影级', cost: '$$$$' },
] as const

export type QualityLevelCode = (typeof QUALITY_LEVELS)[number]['code']

interface QualitySelectorProps {
  value: string
  onChange: (code: string) => void
}

export default function QualitySelector({ value, onChange }: QualitySelectorProps) {
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
      {QUALITY_LEVELS.map((q) => (
        <Button
          key={q.code}
          variant={value === q.code ? 'contained' : 'outlined'}
          size="small"
          onClick={() => onChange(q.code)}
          sx={{
            flex: { xs: '1 1 100%', sm: '1 1 45%', md: 1 },
            minWidth: 120,
            flexDirection: 'column',
            py: 1.5,
            textTransform: 'none',
          }}
        >
          <Typography variant="body2" fontWeight="medium">
            {q.label}
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.9 }}>
            {q.desc}
          </Typography>
        </Button>
      ))}
    </Box>
  )
}
