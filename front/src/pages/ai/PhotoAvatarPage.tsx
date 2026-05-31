import { useState, useCallback } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Checkbox,
  Grid,
  IconButton,
  InputLabel,
  LinearProgress,
  MenuItem,
  Select,
  Stack,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import DeleteIcon from '@mui/icons-material/Delete'
import ReplayIcon from '@mui/icons-material/Replay'
import VisibilityIcon from '@mui/icons-material/Visibility'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getPhotoAvatarOverview,
  searchPhotoAvatarTasks,
  createPhotoAvatarTask,
  deletePhotoAvatarTask,
  retryPhotoAvatarTask,
  type PhotoAvatarTaskVO,
  type PhotoAvatarSearchVO,
} from '@/api/photo-avatar'
import { PageHeader, KpiCard } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { commercialDenialMessage, isCommercialDenial, CREDITS_GOVERNANCE_PATH } from '@/utils/commercialError'
import { Link } from 'react-router-dom'

const STATUS_LABELS: Record<string, string> = {
  pending: '待处理',
  processing: '处理中',
  completed: '已完成',
  failed: '失败',
}

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
  pending: 'default',
  processing: 'info',
  completed: 'success',
  failed: 'error',
}

const OUTFIT_STYLES = ['casual', 'formal', 'fantasy', 'business']
const BACKGROUNDS = ['solid', 'studio', 'outdoor']

interface CreateFormData {
  photoUrl: string
  outfitStyle: string
  background: string
  portraitConsentConfirmed: boolean
}

const EMPTY_FORM: CreateFormData = {
  photoUrl: '',
  outfitStyle: 'casual',
  background: 'studio',
  portraitConsentConfirmed: false,
}

export default function PhotoAvatarPage() {
  const toast = useToast()
  const qc = useQueryClient()

  // Search/filter state
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)
  const [filterStatus, setFilterStatus] = useState('')
  const [filterOutfit, setFilterOutfit] = useState('')
  const [filterBackground, setFilterBackground] = useState('')

  // Create form state
  const [form, setForm] = useState<CreateFormData>(EMPTY_FORM)

  // Delete/retry dialog
  const [deleteTarget, setDeleteTarget] = useState<PhotoAvatarTaskVO | null>(null)
  const [retryTarget, setRetryTarget] = useState<PhotoAvatarTaskVO | null>(null)

  // Preview dialog
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)

  // Queries
  const overviewQuery = useQuery({
    queryKey: ['photo-avatar-overview'],
    queryFn: getPhotoAvatarOverview,
    refetchInterval: 15000,
  })

  const searchParams: PhotoAvatarSearchVO = {
    page,
    rows: rowsPerPage,
    sortName: 'createTime',
    sortOrder: 'desc',
    ...(filterStatus && { status: filterStatus }),
    ...(filterOutfit && { outfitStyle: filterOutfit }),
    ...(filterBackground && { background: filterBackground }),
  }

  const tasksQuery = useQuery({
    queryKey: ['photo-avatar-tasks', searchParams],
    queryFn: () => searchPhotoAvatarTasks(searchParams),
  })

  // Mutations
  const createMutation = useMutation({
    mutationFn: createPhotoAvatarTask,
    onSuccess: (id) => {
      toast(`任务创建成功 (ID: ${id})`, 'success')
      setForm(EMPTY_FORM)
      void qc.invalidateQueries({ queryKey: ['photo-avatar-overview'] })
      void qc.invalidateQueries({ queryKey: ['photo-avatar-tasks'] })
    },
    onError: (e: unknown) => {
      const msg = isCommercialDenial(e) ? commercialDenialMessage(e) : (e instanceof Error ? e.message : '创建失败')
      toast(msg, 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deletePhotoAvatarTask,
    onSuccess: () => {
      toast('任务已删除', 'success')
      setDeleteTarget(null)
      void qc.invalidateQueries({ queryKey: ['photo-avatar-overview'] })
      void qc.invalidateQueries({ queryKey: ['photo-avatar-tasks'] })
    },
    onError: (e: Error) => toast(e.message || '删除失败', 'error'),
  })

  const retryMutation = useMutation({
    mutationFn: retryPhotoAvatarTask,
    onSuccess: (id) => {
      toast(`重试任务已提交 (ID: ${id})`, 'success')
      setRetryTarget(null)
      void qc.invalidateQueries({ queryKey: ['photo-avatar-overview'] })
      void qc.invalidateQueries({ queryKey: ['photo-avatar-tasks'] })
    },
    onError: (e: Error) => toast(e.message || '重试失败', 'error'),
  })

  const handleCreate = useCallback(() => {
    if (!form.photoUrl.trim()) {
      toast('请输入照片 URL', 'warning')
      return
    }
    if (!form.portraitConsentConfirmed) {
      toast('请先勾选肖像权授权确认', 'warning')
      return
    }
    createMutation.mutate({
      photoUrl: form.photoUrl.trim(),
      outfitStyle: form.outfitStyle,
      background: form.background,
      portraitConsentConfirmed: true,
    })
  }, [form, createMutation, toast])

  const overview = overviewQuery.data
  const tasks = tasksQuery.data?.list ?? []
  const total = tasksQuery.data?.total ?? 0

  const statusCounts = overview?.byStatus ?? {}

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2 }}>
      <PageHeader
        title="照片转视频"
        subtitle="产品 photo-avatar-video · 创建任务消耗权益与积分（需肖像授权）"
        actions={
          <IconButton onClick={() => {
            void qc.invalidateQueries({ queryKey: ['photo-avatar-overview'] })
            void qc.invalidateQueries({ queryKey: ['photo-avatar-tasks'] })
          }}>
            <RefreshIcon />
          </IconButton>
        }
      />

      <Alert severity="warning" variant="outlined" sx={{ mb: 1 }}>
        无权益或积分不足时返回 HTTP 402。请前往 <Link to={CREDITS_GOVERNANCE_PATH}>积分治理</Link> 充值或购买套餐。
      </Alert>

      {(overviewQuery.isLoading || tasksQuery.isLoading) && <LinearProgress />}

      {/* Overview KPI Cards */}
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard title="总任务数" value={overview?.taskCount ?? 0} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard title="已完成" value={statusCounts.completed ?? 0} color="#2e7d32" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard title="处理中" value={statusCounts.processing ?? 0} color="#0288d1" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard title="失败" value={statusCounts.failed ?? 0} color="#d32f2f" />
        </Grid>
      </Grid>

      {/* Create Form */}
      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" mb={2}>
            创建视频生成任务
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="照片 URL"
              value={form.photoUrl}
              onChange={(e) => setForm({ ...form, photoUrl: e.target.value })}
              size="small"
              fullWidth
              placeholder="https://example.com/photo.jpg"
              helperText="输入人物照片的公开可访问 URL"
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>穿搭风格</InputLabel>
                <Select
                  value={form.outfitStyle}
                  label="穿搭风格"
                  onChange={(e) => setForm({ ...form, outfitStyle: e.target.value })}
                >
                  {OUTFIT_STYLES.map((s) => (
                    <MenuItem key={s} value={s}>{s}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>背景</InputLabel>
                <Select
                  value={form.background}
                  label="背景"
                  onChange={(e) => setForm({ ...form, background: e.target.value })}
                >
                  {BACKGROUNDS.map((b) => (
                    <MenuItem key={b} value={b}>{b}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
            <FormControlLabel
              control={
                <Checkbox
                  checked={form.portraitConsentConfirmed}
                  onChange={(e) => setForm({ ...form, portraitConsentConfirmed: e.target.checked })}
                />
              }
              label="我已获得照片中人物的肖像权授权，可用于 AI 口播生成"
            />
            <Box>
              <Button
                variant="contained"
                onClick={handleCreate}
                disabled={createMutation.isPending || !form.photoUrl.trim() || !form.portraitConsentConfirmed}
                startIcon={createMutation.isPending ? <CircularProgress size={18} /> : undefined}
              >
                生成头像视频
              </Button>
              <Typography variant="caption" color="text.secondary" sx={{ ml: 2 }}>
                消耗 80 积分/次
              </Typography>
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* Task History */}
      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" mb={1}>
            任务历史
          </Typography>

          {/* Filters */}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2}>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>状态</InputLabel>
              <Select
                value={filterStatus}
                label="状态"
                onChange={(e) => { setFilterStatus(e.target.value); setPage(0) }}
              >
                <MenuItem value="">全部</MenuItem>
                <MenuItem value="pending">待处理</MenuItem>
                <MenuItem value="processing">处理中</MenuItem>
                <MenuItem value="completed">已完成</MenuItem>
                <MenuItem value="failed">失败</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>风格</InputLabel>
              <Select
                value={filterOutfit}
                label="风格"
                onChange={(e) => { setFilterOutfit(e.target.value); setPage(0) }}
              >
                <MenuItem value="">全部</MenuItem>
                {OUTFIT_STYLES.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>背景</InputLabel>
              <Select
                value={filterBackground}
                label="背景"
                onChange={(e) => { setFilterBackground(e.target.value); setPage(0) }}
              >
                <MenuItem value="">全部</MenuItem>
                {BACKGROUNDS.map((b) => (
                  <MenuItem key={b} value={b}>{b}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>

          {/* Table */}
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>照片</TableCell>
                  <TableCell>风格</TableCell>
                  <TableCell>背景</TableCell>
                  <TableCell>状态</TableCell>
                  <TableCell>进度</TableCell>
                  <TableCell>创建时间</TableCell>
                  <TableCell align="right">操作</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tasks.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                      <Typography color="text.secondary">暂无任务记录</Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  tasks.map((task) => (
                    <TableRow key={task.id} hover>
                      <TableCell>{task.id}</TableCell>
                      <TableCell>
                        {task.photoUrl ? (
                          <Box
                            component="img"
                            src={task.photoUrl}
                            alt={`task-${task.id}`}
                            sx={{ width: 48, height: 48, borderRadius: 1, objectFit: 'cover' }}
                          />
                        ) : (
                          <Box sx={{ width: 48, height: 48, bgcolor: 'grey.200', borderRadius: 1 }} />
                        )}
                      </TableCell>
                      <TableCell>{task.outfitStyle}</TableCell>
                      <TableCell>{task.background}</TableCell>
                      <TableCell>
                        <Chip
                          label={STATUS_LABELS[task.status] ?? task.status}
                          size="small"
                          color={STATUS_COLORS[task.status] ?? 'default'}
                        />
                      </TableCell>
                      <TableCell sx={{ minWidth: 100 }}>
                        {task.status === 'processing' ? (
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <LinearProgress
                              variant="determinate"
                              value={task.progress ?? 0}
                              sx={{ flex: 1, minWidth: 60 }}
                            />
                            <Typography variant="caption">{task.progress ?? 0}%</Typography>
                          </Box>
                        ) : task.status === 'completed' ? (
                          <Typography variant="caption" color="success.main">100%</Typography>
                        ) : (
                          <Typography variant="caption">{task.progress ?? 0}%</Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {task.createTime ? new Date(task.createTime).toLocaleString('zh-CN') : '—'}
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                          {task.outputUrl && (
                            <Tooltip title="预览结果">
                              <IconButton size="small" onClick={() => setPreviewUrl(task.outputUrl)}>
                                <VisibilityIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {task.status === 'failed' && (
                            <Tooltip title="重试">
                              <IconButton
                                size="small"
                                color="warning"
                                onClick={() => setRetryTarget(task)}
                              >
                                <ReplayIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          <Tooltip title="删除">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => setDeleteTarget(task)}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          <TablePagination
            component="div"
            count={total}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => { setRowsPerPage(Number(e.target.value)); setPage(0) }}
            rowsPerPageOptions={[5, 10, 25, 50]}
            labelRowsPerPage="每页:"
          />
        </CardContent>
      </Card>

      {/* Delete Confirm Dialog */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>确认删除</DialogTitle>
        <DialogContent>
          <DialogContentText>
            确定要删除任务 #{deleteTarget?.id} 吗？此操作不可撤销。
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>取消</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
            disabled={deleteMutation.isPending}
          >
            删除
          </Button>
        </DialogActions>
      </Dialog>

      {/* Retry Confirm Dialog */}
      <Dialog open={!!retryTarget} onClose={() => setRetryTarget(null)}>
        <DialogTitle>确认重试</DialogTitle>
        <DialogContent>
          <DialogContentText>
            确定要重试任务 #{retryTarget?.id} 吗？将消耗 80 积分。
            {retryTarget?.errorMessage && (
              <Box component="pre" sx={{ mt: 1, fontSize: 12, whiteSpace: 'pre-wrap', color: 'error.main' }}>
                上次错误: {retryTarget.errorMessage}
              </Box>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRetryTarget(null)}>取消</Button>
          <Button
            color="warning"
            variant="contained"
            onClick={() => retryTarget && retryMutation.mutate(retryTarget.id)}
            disabled={retryMutation.isPending}
          >
            重试
          </Button>
        </DialogActions>
      </Dialog>

      {/* Preview Dialog */}
      <Dialog open={!!previewUrl} onClose={() => setPreviewUrl(null)} maxWidth="md" fullWidth>
        <DialogTitle>生成结果预览</DialogTitle>
        <DialogContent>
          {previewUrl && (
            <Box
              component="img"
              src={previewUrl}
              alt="preview"
              sx={{ width: '100%', maxHeight: 500, objectFit: 'contain', borderRadius: 1 }}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreviewUrl(null)}>关闭</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
