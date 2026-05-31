import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  LinearProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ArticleIcon from '@mui/icons-material/Article'
import MovieCreationIcon from '@mui/icons-material/MovieCreation'
import PublishIcon from '@mui/icons-material/Publish'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import InsightsIcon from '@mui/icons-material/Insights'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useLocation, useNavigate } from 'react-router-dom'
import { shortvideoApi, type SvProject } from '@/api/shortvideo'
import type { PublishReviewResult } from '@/types/shortvideo'

const USER_SHORTVIDEO_SCOPE = 'user-shortvideo-creator-line'
const READY_ROUTE_PREFIX = '/user/shortvideo'
const REAL_PROJECT_ENDPOINTS = [
  '/short-video/project/list',
  '/short-video/project/save',
  '/short-video/publish/ai-review',
] as const

const workflowItems = [
  {
    key: 'create',
    title: 'AI 创作',
    desc: '从产品、场景、人群和目标创建个人短视频项目草稿。',
    icon: <AutoAwesomeIcon fontSize="small" />,
    path: '/user/shortvideo/create',
  },
  {
    key: 'planning',
    title: '脚本策划',
    desc: '围绕项目沉淀开头钩子、卖点结构、镜头节奏和口播框架。',
    icon: <ArticleIcon fontSize="small" />,
    path: '/user/shortvideo/planning',
  },
  {
    key: 'materials',
    title: '素材准备',
    desc: '跟进项目所需口播、产品细节、场景补拍和数字人素材清单。',
    icon: <MovieCreationIcon fontSize="small" />,
    path: '/user/shortvideo/materials',
  },
  {
    key: 'publish',
    title: '发布检查',
    desc: '发布前检查标题、封面、标签、违规风险和官方规则引用。',
    icon: <PublishIcon fontSize="small" />,
    path: '/user/shortvideo/publish',
  },
] as const

const statusLabels: Record<string, string> = {
  draft: '草稿',
  processing: '制作中',
  completed: '已完成',
  failed: '失败',
}

const projectTypeLabels: Record<string, string> = {
  daily: '日常内容',
  viral_clone: '爆款复刻',
  soft_ad: '软广种草',
}

function projectProgress(project?: SvProject): number {
  if (!project) return 0
  if (project.status === 'completed' || project.finalVideoUrl) return 100
  if (project.shotListId) return 70
  if (project.scriptId) return 50
  if (project.status === 'processing') return 40
  if (project.status === 'failed') return 20
  return 25
}

function formatShortDate(value?: string): string {
  return value ? value.slice(0, 10) : '未排期'
}

function projectStatusLabel(project: SvProject): string {
  return statusLabels[String(project.status ?? '')] ?? project.status ?? '未设置'
}

function projectTypeLabel(project: SvProject): string {
  return projectTypeLabels[String(project.projectType ?? '')] ?? project.projectType
}

export default function UserShortvideoPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null)
  const [publishReview, setPublishReview] = useState<PublishReviewResult | null>(null)

  const activeItem = useMemo(
    () => workflowItems.find(item => location.pathname === item.path) ?? workflowItems[0],
    [location.pathname],
  )

  const projectsQuery = useQuery({
    queryKey: ['user-shortvideo-projects'],
    queryFn: () => shortvideoApi.list({ page: 0, rows: 8, sortName: 'createTime', sortOrder: 'desc' }),
  })

  const projects = projectsQuery.data?.list ?? []
  const latestProject = projects[0]
  const latestProgress = projectProgress(latestProject)

  const createProjectMutation = useMutation({
    mutationFn: () => shortvideoApi.save({
      title: `个人短视频草稿 ${new Date().toLocaleString('zh-CN', { hour12: false })}`,
      projectType: 'daily',
      status: 'draft',
      shootStatus: 'not_started',
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['user-shortvideo-projects'] })
    },
  })

  const publishReviewMutation = useMutation({
    mutationFn: (project: SvProject) => shortvideoApi.publishAiReview({
      projectId: project.id,
      title: project.publishTitle || project.title,
      videoUrl: project.finalVideoUrl,
    }),
    onSuccess: (result) => {
      setPublishReview(result)
    },
  })

  const handlePublishReview = (project: SvProject) => {
    setSelectedProjectId(project.id)
    setPublishReview(null)
    publishReviewMutation.mutate(project)
  }

  return (
    <Box
      data-testid="user-shortvideo-page"
      data-contract-scope={USER_SHORTVIDEO_SCOPE}
      data-route-prefix={READY_ROUTE_PREFIX}
      data-no-talent-shell-redirect="true"
      data-real-project-endpoints={REAL_PROJECT_ENDPOINTS.join(',')}
      sx={{ width: '100%', maxWidth: 1180, mx: 'auto' }}
    >
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ xs: 'flex-start', md: 'center' }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h6" fontWeight={700}>我的短视频</Typography>
              <Typography variant="body2" color="text.secondary">
                普通用户独立创作工作台，已接入个人项目库和发布前 AI 审核，不再跳转达人后台。
              </Typography>
            </Box>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Button
                size="small"
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => projectsQuery.refetch()}
                disabled={projectsQuery.isFetching}
              >
                刷新
              </Button>
              <Button
                size="small"
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => createProjectMutation.mutate()}
                disabled={createProjectMutation.isPending}
              >
                创建草稿
              </Button>
            </Stack>
          </Stack>
        </Paper>

        {createProjectMutation.isError ? (
          <Alert severity="error" variant="outlined" data-testid="user-shortvideo-create-error">
            创建项目失败：{createProjectMutation.error instanceof Error ? createProjectMutation.error.message : '未知错误'}
          </Alert>
        ) : null}

        {projectsQuery.isError ? (
          <Alert severity="error" variant="outlined" data-testid="user-shortvideo-load-error">
            个人短视频项目读取失败：{projectsQuery.error instanceof Error ? projectsQuery.error.message : '未知错误'}
          </Alert>
        ) : null}

        <Grid container spacing={2}>
          {workflowItems.map(item => {
            const active = activeItem.key === item.key
            return (
              <Grid key={item.key} item xs={12} sm={6} lg={3}>
                <Paper
                  variant="outlined"
                  data-testid="user-shortvideo-workflow-card"
                  data-active={active ? 'true' : 'false'}
                  sx={(theme) => ({
                    p: 1.5,
                    borderRadius: 1,
                    height: '100%',
                    borderColor: active ? 'primary.main' : 'divider',
                    bgcolor: active ? theme.palette.action.selected : 'background.paper',
                  })}
                >
                  <Stack spacing={1.25} sx={{ height: '100%' }}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Box sx={{ display: 'flex', color: active ? 'primary.main' : 'text.secondary' }}>{item.icon}</Box>
                      <Typography variant="subtitle2" fontWeight={700}>{item.title}</Typography>
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ minHeight: 60 }}>
                      {item.desc}
                    </Typography>
                    <Box sx={{ mt: 'auto' }}>
                      <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">最近项目进度</Typography>
                        <Typography variant="caption" color="text.secondary">{latestProgress}%</Typography>
                      </Stack>
                      <LinearProgress variant="determinate" value={latestProgress} sx={{ height: 6, borderRadius: 1 }} />
                    </Box>
                    <Button
                      size="small"
                      variant={active ? 'contained' : 'outlined'}
                      onClick={() => navigate(item.path)}
                      startIcon={item.icon}
                    >
                      进入
                    </Button>
                  </Stack>
                </Paper>
              </Grid>
            )
          })}
        </Grid>

        <Grid container spacing={2}>
          <Grid item xs={12} lg={8}>
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 1, height: '100%' }}>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                  <Stack direction="row" spacing={1} alignItems="center">
                    <InsightsIcon fontSize="small" color="primary" />
                    <Typography variant="subtitle1" fontWeight={700}>我的项目库</Typography>
                  </Stack>
                  <Chip size="small" variant="outlined" label={`共 ${projectsQuery.data?.total ?? projects.length} 个`} />
                </Stack>
                <Divider />

                {projectsQuery.isLoading ? (
                  <Stack data-testid="user-shortvideo-loading" direction="row" spacing={1} alignItems="center">
                    <CircularProgress size={18} />
                    <Typography variant="body2" color="text.secondary">正在读取个人短视频项目...</Typography>
                  </Stack>
                ) : null}

                {!projectsQuery.isLoading && projects.length === 0 && !projectsQuery.isError ? (
                  <Alert severity="info" variant="outlined" data-testid="user-shortvideo-empty">
                    当前账号还没有短视频项目。点击“创建草稿”会写入后端个人项目库，并按当前登录用户隔离。
                  </Alert>
                ) : null}

                <Stack spacing={1}>
                  {projects.map(project => (
                    <Paper key={project.id} variant="outlined" sx={{ p: 1.5, borderRadius: 1 }}>
                      <Stack spacing={1}>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} justifyContent="space-between">
                          <Box sx={{ minWidth: 0 }}>
                            <Typography variant="subtitle2" fontWeight={700} noWrap>{project.title}</Typography>
                            <Typography variant="caption" color="text.secondary">
                              #{project.id} · {projectTypeLabel(project)} · {formatShortDate(project.scheduleDate)}
                            </Typography>
                          </Box>
                          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                            <Chip size="small" label={projectStatusLabel(project)} />
                            <Chip size="small" variant="outlined" label={project.reviewStatus || '待审核'} />
                          </Stack>
                        </Stack>
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'stretch', sm: 'center' }}>
                          <Box sx={{ flex: 1 }}>
                            <LinearProgress variant="determinate" value={projectProgress(project)} sx={{ height: 6, borderRadius: 1 }} />
                          </Box>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<FactCheckIcon />}
                            onClick={() => handlePublishReview(project)}
                            disabled={publishReviewMutation.isPending && selectedProjectId === project.id}
                          >
                            发布检查
                          </Button>
                        </Stack>
                      </Stack>
                    </Paper>
                  ))}
                </Stack>
              </Stack>
            </Paper>
          </Grid>

          <Grid item xs={12} lg={4}>
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 1, height: '100%' }}>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <FactCheckIcon fontSize="small" color="primary" />
                  <Typography variant="subtitle1" fontWeight={700}>{activeItem.title} · AI 审核</Typography>
                </Stack>
                <Divider />
                <Typography variant="body2" color="text.secondary">
                  发布检查调用统一短视频发布审核接口，返回违规问题、修改建议和官方规则引用。
                </Typography>

                {publishReviewMutation.isPending ? (
                  <Stack data-testid="user-shortvideo-review-loading" direction="row" spacing={1} alignItems="center">
                    <CircularProgress size={18} />
                    <Typography variant="body2" color="text.secondary">正在执行发布检查...</Typography>
                  </Stack>
                ) : null}

                {publishReviewMutation.isError ? (
                  <Alert severity="error" variant="outlined" data-testid="user-shortvideo-review-error">
                    发布检查失败：{publishReviewMutation.error instanceof Error ? publishReviewMutation.error.message : '未知错误'}
                  </Alert>
                ) : null}

                {publishReview ? (
                  <Stack spacing={1} data-testid="user-shortvideo-review-result">
                    <Alert severity={publishReview.passed ? 'success' : 'warning'} variant="outlined">
                      {publishReview.passed ? 'AI 审核通过' : 'AI 审核发现风险'}
                    </Alert>
                    <Typography variant="body2" fontWeight={700}>问题</Typography>
                    {(publishReview.issues ?? []).length > 0 ? (
                      publishReview.issues.map((issue, index) => (
                        <Typography key={index} variant="body2" color="text.secondary">· {issue}</Typography>
                      ))
                    ) : (
                      <Typography variant="body2" color="text.secondary">未返回风险问题。</Typography>
                    )}
                    <Typography variant="body2" fontWeight={700}>官方引用</Typography>
                    {(publishReview.officialReferences ?? []).length > 0 ? (
                      publishReview.officialReferences?.map((ref, index) => (
                        <Paper key={`${ref.docId ?? 'ref'}-${index}`} variant="outlined" sx={{ p: 1, borderRadius: 1 }}>
                          <Typography variant="caption" fontWeight={700}>{ref.title || ref.kbName || '官方规则'}</Typography>
                          <Typography variant="caption" color="text.secondary" display="block">
                            {ref.contentPreview || `chunk ${ref.chunkId ?? '-'} · score ${ref.score ?? '-'}`}
                          </Typography>
                        </Paper>
                      ))
                    ) : (
                      <Alert severity="warning" variant="outlined">
                        审核接口没有返回官方规则引用，当前结果不能作为最终放行依据。
                      </Alert>
                    )}
                  </Stack>
                ) : null}
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      </Stack>
    </Box>
  )
}
