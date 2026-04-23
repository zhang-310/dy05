import { useNavigate, useSearchParams } from 'react-router-dom'
import { Box, Typography, List, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import {
  Description as ScriptIcon,
  ViewModule as ShotIcon,
  PhotoLibrary as PrepareIcon,
  Movie as MaterialIcon,
  Edit as EditIcon,
  Publish as PublishIcon,
  CheckCircle as DoneIcon,
} from '@mui/icons-material'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const STEPS = [
  { id: 'script', label: '脚本策划', path: shortvideoRoutes.scriptPlanning, icon: <ScriptIcon /> },
  { id: 'shot-list', label: '分镜设计', path: shortvideoRoutes.shotList, icon: <ShotIcon /> },
  { id: 'prepare', label: '素材准备', path: shortvideoRoutes.materialPrepare, icon: <PrepareIcon /> },
  { id: 'material', label: '素材生产', path: shortvideoRoutes.materialProduction, icon: <MaterialIcon /> },
  { id: 'edit', label: '视频剪辑', path: shortvideoRoutes.editing, icon: <EditIcon /> },
  { id: 'publish', label: '审核发布', path: shortvideoRoutes.publish, icon: <PublishIcon /> },
]

interface ProjectFlowSidebarProps {
  projectId: number
  projectTitle?: string
  currentStep?: string
  progress?: number
}

export function ProjectFlowSidebar({ projectId, projectTitle, currentStep, progress = 0 }: ProjectFlowSidebarProps) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const buildUrl = (path: string) => {
    const params = new URLSearchParams(searchParams)
    params.set('projectId', String(projectId))
    return `${path}?${params.toString()}`
  }

  return (
    <Box
      sx={{
        width: 200,
        flexShrink: 0,
        borderRight: 1,
        borderColor: 'divider',
        p: 2,
        bgcolor: 'action.hover',
      }}
    >
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        创作流程
      </Typography>
      {projectTitle && (
        <Typography variant="body2" sx={{ mb: 1, fontWeight: 500 }} noWrap title={projectTitle}>
          {projectTitle}
        </Typography>
      )}
      {progress > 0 && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="caption" color="text.secondary">
            进度 {progress}%
          </Typography>
        </Box>
      )}
      <List dense disablePadding>
        {STEPS.map((step, idx) => {
          const isActive = currentStep === step.id
          const stepProgress = [16, 33, 50, 66, 83, 100][idx]
          const isDone = progress >= stepProgress
          return (
            <ListItemButton
              key={step.id}
              selected={isActive}
              onClick={() => navigate(buildUrl(step.path))}
              sx={{ borderRadius: 1, mb: 0.5 }}
            >
              <ListItemIcon sx={{ minWidth: 36, color: isDone ? 'success.main' : isActive ? 'primary.main' : 'action.active' }}>
                {isDone && !isActive ? <DoneIcon fontSize="small" /> : step.icon}
              </ListItemIcon>
              <ListItemText primary={step.label} primaryTypographyProps={{ variant: 'body2' }} />
            </ListItemButton>
          )
        })}
      </List>
    </Box>
  )
}
