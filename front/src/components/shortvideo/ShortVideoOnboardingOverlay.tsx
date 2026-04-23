import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Dialog,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
} from '@mui/material'
import { RocketLaunch as RocketIcon } from '@mui/icons-material'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const STORAGE_KEY = 'shortvideo-onboarding-seen'

export function ShortVideoOnboardingOverlay() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  useEffect(() => {
    try {
      const seen = localStorage.getItem(STORAGE_KEY)
      if (!seen) setOpen(true)
    } catch {
      // localStorage 不可用时静默跳过
    }
  }, [])

  const dismiss = () => {
    try {
      localStorage.setItem(STORAGE_KEY, '1')
    } catch {
      // ignore
    }
    setOpen(false)
  }

  const handleStart = () => {
    dismiss()
    navigate(shortvideoRoutes.quickGenerate)
  }

  const handleSkip = () => {
    dismiss()
  }

  if (!open) return null

  return (
    <Dialog
      open={open}
      onClose={handleSkip}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: 2,
          p: 1,
        },
      }}
    >
      <DialogContent sx={{ textAlign: 'center', py: 4 }}>
        <Box sx={{ color: 'primary.main', mb: 2 }}>
          <RocketIcon sx={{ fontSize: 56 }} />
        </Box>
        <Typography variant="h5" gutterBottom fontWeight={600}>
          3 步完成你的第一条 AI 视频
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 360, mx: 'auto' }}>
          选择主题 → 输入关键词 → 选择风格，AI 自动生成脚本、分镜、素材并合成成片，约 3 分钟即可完成
        </Typography>
      </DialogContent>
      <DialogActions sx={{ justifyContent: 'center', gap: 2, pb: 3 }}>
        <Button variant="outlined" onClick={handleSkip} color="inherit">
          跳过
        </Button>
        <Button variant="contained" onClick={handleStart} startIcon={<RocketIcon />}>
          开始创作
        </Button>
      </DialogActions>
    </Dialog>
  )
}
