import { useParams, Navigate, useSearchParams } from 'react-router-dom'
import {
  Box, Chip, IconButton, Typography, Tooltip,
  Skeleton, Breadcrumbs, Link, Button, LinearProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Stack, FormControlLabel, Checkbox, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import VideocamIcon from '@mui/icons-material/Videocam'
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord'
import MovieIcon from '@mui/icons-material/Movie'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { SessionWorkspacePage } from './SessionWorkspacePage'
import { ShoppingCartFloat } from '@/components/ShoppingCartFloat'
import {
  STEP_TO_TAB, STEP_LABELS, STEP_COLORS,
  parseStepFromSearch, buildStepParams,
  type WorkspaceTab,
} from './sessionWorkbenchNav'
import { liveApi } from '@/api/live'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { useState, useMemo } from 'react'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

interface ReadinessData {
  score?: number
  [key: string]: unknown
}

export default function LiveWorkbenchPage() {
  const { sessionId: rawId } = useParams<{ sessionId: string }>()
  const sessionId = Number(rawId)
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const toast = useToast()
  const step = parseStepFromSearch(searchParams)
  const [cloneOpen, setCloneOpen] = useState(false)
  const [cloneName, setCloneName] = useState('')
  const [exportOpen, setExportOpen] = useState(false)
  const [exportStyle, setExportStyle] = useState('professional')

  const { data: session, isLoading } = useQuery({
    queryKey: ['wb-session-header', sessionId],
    queryFn: () => liveApi.sessionGet(sessionId),
    enabled: !isNaN(sessionId),
  })

  const { data: readiness } = useQuery({
    queryKey: ['wb-readiness', sessionId],
    queryFn: () => liveApi.sessionReadiness(sessionId),
    enabled: !isNaN(sessionId),
    refetchInterval: 60000,
  })

  const cloneMut = useMutation({
    mutationFn: () => liveApi.sessionClone(sessionId),
    onSuccess: (newId) => {
      toast('场次克隆成功', 'success')
      setCloneOpen(false)
      navigate(`/admin/live/sessions/${newId}`)
    },
    onError: () => toast('克隆失败', 'error'),
  })

  const exportMut = useMutation({
    mutationFn: () => liveApi.sessionExportToShortVideo(sessionId),
    onSuccess: () => {
      toast('已导出至短视频项目', 'success')
      setExportOpen(false)
      navigate(shortvideoRoutes.projects)
    },
    onError: () => toast('导出失败', 'error'),
  })

  const { data: overviewRaw } = useQuery({
    queryKey: ['wb-session-overview', sessionId],
    queryFn: () => liveApi.sessionOverview(sessionId),
    enabled: !isNaN(sessionId),
    staleTime: 60000,
  })
  const overview = overviewRaw as { gmv?: number; orderCount?: number; scriptCount?: number; productCount?: number; readiness?: number } | undefined

  if (isNaN(sessionId)) return <Navigate to="/admin/live/sessions" replace />

  const readinessPct = Number((readiness as ReadinessData)?.score ?? 0)
  const statusLabel = session?.status === 2 ? '直播中' : session?.status === 1 ? '准备中' : session?.status === 3 ? '已结束' : '草稿'
  const statusColor = session?.status === 2 ? '#f44336' : session?.status === 1 ? '#ff9800' : '#9e9e9e'

  const handleStepChange = (newStep: number) => {
    setSearchParams(buildStepParams(newStep, searchParams), { replace: true })
  }

  // 模拟购物车数据（实际应从状态管理获取）
  const cartItems = useMemo(() => {
    return [
      // 示例数据，实际应连接到真实购物车状态
    ]
  }, [])

  return (
    <Box sx={{ height: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {/* 购物车浮窗 */}
      <ShoppingCartFloat
        items={cartItems}
        onCheckout={() => {
          toast('前往支付', 'info')
        }}
      />

      {/* 顶部导航栏 */}
      <Box sx={{
        px: 1.5, py: 0.75,
        borderBottom: '1px solid',
        borderColor: 'divider',
        bgcolor: 'background.paper',
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        flexShrink: 0,
        flexWrap: 'wrap',
      }}>
        {/* 返回按钮 */}
        <Tooltip title="返回场次列表">
          <IconButton size="small" onClick={() => navigate('/admin/live/sessions')}>
            <ArrowBackIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        {/* 面包屑 */}
        <Breadcrumbs separator={<NavigateNextIcon sx={{ fontSize: 14 }} />} sx={{ flex: 1, minWidth: 0 }}>
          <Link component={RouterLink} to="/admin/live/sessions" underline="hover" color="text.secondary" sx={{ fontSize: 13 }}>
            直播场次
          </Link>
          <Tooltip
            title={overview ? (
              <Box sx={{ fontSize: 12 }}>
                <div>GMV: ¥{(overview.gmv ?? 0).toLocaleString()}</div>
                <div>订单: {overview.orderCount ?? 0}</div>
                <div>话术: {overview.scriptCount ?? 0} 条</div>
                <div>商品: {overview.productCount ?? 0} 件</div>
                <div>就绪度: {overview.readiness ?? 0}%</div>
              </Box>
            ) : ''}
            arrow
          >
            <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.primary', cursor: overview ? 'help' : 'default' }} noWrap>
              {isLoading ? <Skeleton width={120} /> : (session?.liveTitle || `场次 #${sessionId}`)}
            </Typography>
          </Tooltip>
        </Breadcrumbs>

        {/* 状态 Chip */}
        {session && (
          <Chip
            icon={<FiberManualRecordIcon sx={{ fontSize: '10px !important', color: `${statusColor} !important` }} />}
            label={statusLabel}
            size="small"
            variant="outlined"
            sx={{ borderColor: statusColor, color: statusColor, fontWeight: 600 }}
          />
        )}

        {/* 就绪度进度条 */}
        {readinessPct > 0 && (
          <Tooltip title={`就绪度 ${readinessPct}%`}>
            <Stack direction="row" alignItems="center" spacing={0.5} sx={{ minWidth: 100 }}>
              <LinearProgress
                variant="determinate"
                value={readinessPct}
                sx={{ flex: 1, height: 6, borderRadius: 3,
                  '& .MuiLinearProgress-bar': { bgcolor: readinessPct >= 85 ? '#4caf50' : readinessPct >= 60 ? '#ff9800' : '#f44336' } }}
              />
              <Typography variant="caption" sx={{ fontSize: 11 }}>{readinessPct}%</Typography>
            </Stack>
          </Tooltip>
        )}

        {/* 操作按钮 */}
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="克隆场次">
            <IconButton size="small" onClick={() => { setCloneName(`${session?.liveTitle ?? ''}（副本）`); setCloneOpen(true) }}>
              <ContentCopyIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="导出为短视频项目">
            <IconButton size="small" onClick={() => setExportOpen(true)} disabled={exportMut.isPending}>
              <MovieIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Button size="small" variant="outlined" startIcon={<VideocamIcon />}
            onClick={() => navigate(`/admin/live/sessions/${sessionId}/realtime`)}
            sx={{ fontSize: 12, py: 0.3 }}>
            实时面板
          </Button>
        </Stack>

        {/* 5步骤 Chip 导航 */}
        <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
          {STEP_TO_TAB.map((t: WorkspaceTab, i: number) => {
            const colors = STEP_COLORS[t]
            const active = step === i
            return (
              <Chip
                key={t}
                label={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Box sx={{
                      width: 16, height: 16, borderRadius: '50%',
                      bgcolor: active ? '#fff' : colors.active,
                      color: active ? colors.active : '#fff',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 10, fontWeight: 700,
                    }}>
                      {i + 1}
                    </Box>
                    {STEP_LABELS[t]}
                  </Box>
                }
                onClick={() => handleStepChange(i)}
                size="small"
                sx={{
                  bgcolor: active ? colors.active : colors.bg,
                  color: active ? '#fff' : colors.active,
                  fontWeight: active ? 700 : 400,
                  border: '1px solid',
                  borderColor: active ? colors.active : 'transparent',
                  cursor: 'pointer',
                  '&:hover': { bgcolor: active ? colors.active : colors.bg, opacity: 0.85 },
                  '& .MuiChip-label': { px: 1 },
                }}
              />
            )
          })}
        </Box>
      </Box>

      {/* 工作区主体 */}
      <Box sx={{ flex: 1, overflow: 'hidden', minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        <SessionWorkspacePage
          sessionId={sessionId}
          step={step}
          onStepChange={handleStepChange}
        />
      </Box>

      {/* 导出短视频弹窗 */}
      <Dialog open={exportOpen} onClose={() => setExportOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>导出为短视频项目</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary" mb={2}>
            将「{session?.liveTitle}」的话术导出为短视频脚本，自动创建新项目。
          </Typography>
          <FormControl fullWidth size="small">
            <InputLabel>脚本风格</InputLabel>
            <Select value={exportStyle} label="脚本风格" onChange={e => setExportStyle(e.target.value)}>
              <MenuItem value="professional">专业科普</MenuItem>
              <MenuItem value="viral">爆款风格</MenuItem>
              <MenuItem value="storytelling">故事叙述</MenuItem>
              <MenuItem value="interactive">强互动</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExportOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => exportMut.mutate()} disabled={exportMut.isPending}>
            确认导出
          </Button>
        </DialogActions>
      </Dialog>

      {/* 克隆场次弹窗 */}
      <Dialog open={cloneOpen} onClose={() => setCloneOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>克隆场次</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary" mb={2}>
            将基于「{session?.liveTitle}」创建副本，包含商品选品、话术内容、节奏配置和生成参数。
          </Typography>
          <TextField
            fullWidth size="small" label="新场次名称"
            value={cloneName}
            onChange={e => setCloneName(e.target.value)}
            sx={{ mb: 1.5 }}
          />
          <FormControlLabel
            control={<Checkbox defaultChecked />}
            label="克隆商品选品与排序"
          />
          <br />
          <FormControlLabel
            control={<Checkbox defaultChecked />}
            label="克隆话术内容（当前激活版本）"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCloneOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => cloneMut.mutate()} disabled={cloneMut.isPending}>
            确认克隆
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
