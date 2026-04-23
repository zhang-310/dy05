import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Button, TextField,
  FormControl, InputLabel, Select, MenuItem, Stack,
  CircularProgress, Dialog, DialogTitle, DialogContent,
  DialogActions, LinearProgress,
} from '@mui/material'
import {
  AutoAwesome as AiIcon, NavigateNext as NextIcon, Check as DoneIcon,
} from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const STYLES = [
  { id: '温馨', label: '温馨治愈' },
  { id: '搞笑', label: '搞笑幽默' },
  { id: '高端', label: '高端大气' },
  { id: '专业', label: '专业干货' },
]

const STAGES = ['脚本生成', '分镜设计', '素材准备', '视频合成', '发布评估']

export default function QuickGeneratePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [theme, setTheme] = useState('')
  const [keywords, setKeywords] = useState('')
  const [style, setStyle] = useState('温馨')
  const [generating, setGenerating] = useState(false)
  const [progress, setProgress] = useState(0)
  const [currentStage, setCurrentStage] = useState(0)
  const [progressOpen, setProgressOpen] = useState(false)
  const [resultProjectId, setResultProjectId] = useState<number | null>(null)

  const handleGenerate = async () => {
    if (!theme) { toast('请输入主题', 'warning'); return }
    setGenerating(true)
    setProgress(0)
    setCurrentStage(0)
    setProgressOpen(true)

    try {
      // 创建项目
      const projectId = await shortvideoApi.save({
        title: theme,
        projectType: 'viral_clone',
        status: 'draft',
        publishTitle: [keywords, `风格:${style}`].filter(Boolean).join(' · ').slice(0, 255) || undefined,
      })

      // 模拟进度：每步展示当前进行中；全部结束后 currentStage 需 >最后一项索引，否则最后一项会永远停在转圈
      for (let i = 0; i < STAGES.length; i++) {
        setCurrentStage(i)
        setProgress(((i + 1) / STAGES.length) * 100)
        await new Promise((r) => setTimeout(r, 800))
      }
      setCurrentStage(STAGES.length)
      setProgress(100)

      setResultProjectId(typeof projectId === 'number' ? projectId : Number(projectId))
      toast('生成成功！', 'success')
    } catch {
      toast('生成失败', 'error')
      setProgressOpen(false)
    } finally {
      setGenerating(false)
    }
  }

  return (
    <Box>
      <PageHeader
        title="一键快速生成"
        breadcrumbs={[{ label: '短视频' }, { label: '快速生成' }]}
      />

      <Card sx={{ maxWidth: 680, mx: 'auto' }}>
        <CardContent>
          <Stack spacing={3}>
            <TextField
              label="视频主题" required
              value={theme} onChange={(e) => setTheme(e.target.value)}
              placeholder="例如：护肤品开箱测评、美妆新手入门..."
            />
            <TextField
              label="关键词（用逗号分隔）"
              value={keywords} onChange={(e) => setKeywords(e.target.value)}
              placeholder="例如：美白、保湿、平价好物..."
            />
            <FormControl>
              <InputLabel>视频风格</InputLabel>
              <Select value={style} label="视频风格" onChange={(e) => setStyle(e.target.value)}>
                {STYLES.map((s) => <MenuItem key={s.id} value={s.id}>{s.label}</MenuItem>)}
              </Select>
            </FormControl>
            <Button
              variant="contained" size="large"
              startIcon={<AiIcon />}
              onClick={handleGenerate}
              disabled={generating}
              fullWidth
            >
              一键生成视频
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {/* 进度弹窗 */}
      <Dialog open={progressOpen} maxWidth="sm" fullWidth disableEscapeKeyDown>
        <DialogTitle>正在生成视频...</DialogTitle>
        <DialogContent>
          <LinearProgress variant="determinate" value={progress} sx={{ mb: 2, height: 8, borderRadius: 4 }} />
          <Stack spacing={1}>
            {STAGES.map((s, i) => (
              <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {i < currentStage ? (
                  <DoneIcon fontSize="small" color="success" />
                ) : i === currentStage ? (
                  <CircularProgress size={16} />
                ) : (
                  <Box sx={{ width: 16, height: 16, border: '1px solid', borderColor: 'divider', borderRadius: '50%' }} />
                )}
                <Typography variant="body2" color={i === currentStage ? 'primary.main' : i < currentStage ? 'text.secondary' : 'text.disabled'}>
                  {s}
                </Typography>
              </Box>
            ))}
          </Stack>
        </DialogContent>
        {!generating && (
          <DialogActions>
            <Button onClick={() => setProgressOpen(false)}>关闭</Button>
            {resultProjectId && (
              <Button variant="contained" startIcon={<NextIcon />} onClick={() => navigate(shortvideoRoutes.projects)}>
                查看项目
              </Button>
            )}
          </DialogActions>
        )}
      </Dialog>
    </Box>
  )
}
