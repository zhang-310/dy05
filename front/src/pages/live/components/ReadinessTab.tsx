import {
  Box, Typography, Chip, CircularProgress, Alert, List, ListItem,
  ListItemIcon, ListItemText, Divider, Button, Stack, Card, CardContent, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningIcon from '@mui/icons-material/Warning'
import ErrorIcon from '@mui/icons-material/Error'
import RefreshIcon from '@mui/icons-material/Refresh'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import BookmarkAddIcon from '@mui/icons-material/BookmarkAdd'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import AssignmentIcon from '@mui/icons-material/Assignment'
import { useState } from 'react'
import { useCoreData } from '../contexts'
import { liveApi } from '@/api/live'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { useNavigate } from 'react-router-dom'
interface ReadinessItem {
  key: string
  label: string
  status: 'ok' | 'warn' | 'error'
  detail?: string
}

function ReadinessIcon({ status }: { status: ReadinessItem['status'] }) {
  if (status === 'ok') return <CheckCircleIcon color="success" />
  if (status === 'warn') return <WarningIcon color="warning" />
  return <ErrorIcon color="error" />
}

function SaveAsTemplateDialog({ sessionId, open, onClose }: { sessionId: number; open: boolean; onClose: () => void }) {
  const toast = useToast()
  const [name, setName] = useState('')
  const [desc, setDesc] = useState('')
  const mut = useMutation({
    mutationFn: () => liveApi.templateSaveAsFromSession({ sessionId, name, description: desc }),
    onSuccess: () => { toast('已保存为模板', 'success'); onClose() },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>保存为模板</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        <Stack spacing={2}>
          <TextField label="模板名称" value={name} onChange={e => setName(e.target.value)} size="small" fullWidth required />
          <TextField label="描述（可选）" value={desc} onChange={e => setDesc(e.target.value)} size="small" fullWidth multiline rows={2} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} size="small">取消</Button>
        <Button onClick={() => mut.mutate()} disabled={!name.trim() || mut.isPending} variant="contained" size="small">
          {mut.isPending ? <CircularProgress size={14} /> : '保存'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
export function ReadinessTab() {
  const { session, products, scripts } = useCoreData()
  const toast = useToast()
  const navigate = useNavigate()
  const [showSaveTemplate, setShowSaveTemplate] = useState(false)

  const { data: readinessData, isFetching, refetch } = useQuery({
    queryKey: ['wb-readiness-detail', session?.id],
    queryFn: () => liveApi.sessionReadiness(session!.id),
    enabled: !!session?.id,
  })

  const cloneMut = useMutation({
    mutationFn: () => liveApi.sessionClone(session!.id),
    onSuccess: (newId) => {
      toast('场次已克隆，即将跳转', 'success')
      setTimeout(() => navigate(`/live/workspace/${newId}`), 1200)
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const exportMut = useMutation({
    mutationFn: () => liveApi.scriptExport({ sessionId: session!.id, format: 'txt' }),
    onSuccess: (content) => {
      const blob = new Blob([String(content)], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${session?.liveTitle ?? '话术'}.txt`
      a.click()
      URL.revokeObjectURL(url)
      toast('话术已导出', 'success')
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const filledScripts = scripts.filter(s => (s.scriptContent ?? '').trim().length > 0)
  const activatedScripts = scripts.filter(s => s.status === 1)

  const localItems: ReadinessItem[] = [
    {
      key: 'products',
      label: '商品已配置',
      status: products.length > 0 ? 'ok' : 'error',
      detail: products.length > 0 ? `已选 ${products.length} 个商品` : '请先在「选品排品」步骤添加商品',
    },
    {
      key: 'scripts',
      label: '话术已生成',
      status: scripts.length > 0 ? 'ok' : 'warn',
      detail: scripts.length > 0 ? `已有 ${scripts.length} 条话术` : '建议先生成话术再开播',
    },
    {
      key: 'title',
      label: '直播标题已填写',
      status: session?.liveTitle ? 'ok' : 'warn',
      detail: session?.liveTitle || '未填写直播标题',
    },
    {
      key: 'scheduled',
      label: '已设置开播时间',
      status: session?.scheduledTime ? 'ok' : 'warn',
      detail: session?.scheduledTime || '未设置开播时间',
    },
    {
      key: 'script_content',
      label: '话术内容已编辑',
      status: filledScripts.length === scripts.length && scripts.length > 0
        ? 'ok'
        : scripts.length > 0 ? 'warn' : 'error',
      detail: scripts.length === 0
        ? '暂无话术'
        : `${filledScripts.length}/${scripts.length} 条已填写内容`,
    },
    {
      key: 'activated',
      label: '话术已激活',
      status: activatedScripts.length > 0 ? 'ok' : 'warn',
      detail: activatedScripts.length > 0
        ? `已激活 ${activatedScripts.length}/${scripts.length} 条`
        : '建议激活话术后开播',
    },
  ]

  const errorCount = localItems.filter(i => i.status === 'error').length
  const warnCount = localItems.filter(i => i.status === 'warn').length
  const allOk = errorCount === 0 && warnCount === 0
  const completionPct = scripts.length === 0 ? 0 : Math.round((filledScripts.length / scripts.length) * 100)

  return (
    <Box sx={{ height: '100%', overflow: 'auto', p: 2 }}>
      <Box sx={{ maxWidth: 640, mx: 'auto' }}>

        {/* 操作栏 */}
        <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap">
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetch()} disabled={isFetching} variant="outlined">
            {isFetching ? <CircularProgress size={12} /> : '刷新检查'}
          </Button>
          <Button size="small" startIcon={<FileDownloadIcon />} onClick={() => exportMut.mutate()} disabled={exportMut.isPending} variant="outlined">
            {exportMut.isPending ? <CircularProgress size={12} /> : '导出话术'}
          </Button>
          <Button size="small" startIcon={<BookmarkAddIcon />} onClick={() => setShowSaveTemplate(true)} variant="outlined">
            保存为模板
          </Button>
          <Button size="small" startIcon={<ContentCopyIcon />} onClick={() => cloneMut.mutate()} disabled={cloneMut.isPending} variant="outlined">
            {cloneMut.isPending ? <CircularProgress size={12} /> : '克隆场次'}
          </Button>
        </Stack>

        {/* 统计概览 */}
        <Grid container spacing={1.5} sx={{ mb: 2 }}>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">商品数</Typography>
              <Typography variant="h5" fontWeight={700}>{products.length}</Typography>
            </CardContent></Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">话术数</Typography>
              <Typography variant="h5" fontWeight={700}>{scripts.length}</Typography>
            </CardContent></Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">内容完成率</Typography>
              <Typography variant="h5" fontWeight={700}>{completionPct}%</Typography>
            </CardContent></Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">已激活</Typography>
              <Typography variant="h5" fontWeight={700}>{activatedScripts.length}</Typography>
            </CardContent></Card>
          </Grid>
        </Grid>

        {/* 总体状态 */}
        <Box sx={{
          p: 2, mb: 2, borderRadius: 2,
          bgcolor: allOk ? 'success.50' : errorCount > 0 ? 'error.50' : 'warning.50',
          border: '1px solid',
          borderColor: allOk ? 'success.200' : errorCount > 0 ? 'error.200' : 'warning.200',
          display: 'flex', alignItems: 'center', gap: 2,
        }}>
          {allOk
            ? <CheckCircleIcon color="success" sx={{ fontSize: 40 }} />
            : errorCount > 0
            ? <ErrorIcon color="error" sx={{ fontSize: 40 }} />
            : <WarningIcon color="warning" sx={{ fontSize: 40 }} />}
          <Box sx={{ flex: 1 }}>
            <Typography variant="subtitle1" fontWeight={700}>
              {allOk ? '准备就绪，可以开播！' : errorCount > 0 ? '有问题需要修复' : '有警告项，建议处理'}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
              {errorCount > 0 && <Chip label={`${errorCount} 个错误`} color="error" size="small" />}
              {warnCount > 0 && <Chip label={`${warnCount} 个警告`} color="warning" size="small" />}
              {allOk && <Chip label="全部通过" color="success" size="small" />}
            </Box>
          </Box>
          <AssignmentIcon sx={{ fontSize: 32, opacity: 0.15 }} />
        </Box>

        {/* 检查项列表 */}
        <Typography variant="subtitle2" sx={{ mb: 1 }}>准备检查项</Typography>
        <List dense sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, overflow: 'hidden' }}>
          {localItems.map((item, idx) => (
            <Box key={item.key}>
              <ListItem sx={{ py: 1 }}>
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <ReadinessIcon status={item.status} />
                </ListItemIcon>
                <ListItemText
                  primary={<Typography variant="body2" fontWeight={600}>{item.label}</Typography>}
                  secondary={item.detail}
                />
              </ListItem>
              {idx < localItems.length - 1 && <Divider />}
            </Box>
          ))}
        </List>

        {/* 后端就绪数据 */}
        {isFetching && <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}><CircularProgress size={24} /></Box>}
        {readinessData && !isFetching && (
          <Alert severity="success" sx={{ mt: 2, fontSize: 12 }}>后端就绪检测已完成</Alert>
        )}
      </Box>

      {session && (
        <SaveAsTemplateDialog sessionId={session.id} open={showSaveTemplate} onClose={() => setShowSaveTemplate(false)} />
      )}
    </Box>
  )
}



