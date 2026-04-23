import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { Box, Button, Stack, Card, CardContent, Typography, TextField, MenuItem, Chip, Dialog, DialogTitle, DialogContent, DialogActions, IconButton } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import MovieFilterIcon from '@mui/icons-material/MovieFilter'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

interface ShotScript {
  id: number
  projectId: number
  shotNo: number
  shotType: string
  duration: number
  description: string
  dialogue: string
  cameraAngle: string
  createTime: string
}

export default function ShotListPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const urlProjectId = searchParams.get('projectId')
  const [projectId, setProjectId] = useState(urlProjectId ?? '')
  const [loadedProjectId, setLoadedProjectId] = useState(() => {
    const n = urlProjectId ? Number(urlProjectId) : 0
    return n > 0 ? n : 0
  })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Partial<ShotScript>>({})

  useEffect(() => {
    if (!urlProjectId) return
    const n = Number(urlProjectId)
    if (n > 0) {
      setProjectId(String(n))
      setLoadedProjectId(n)
    }
  }, [urlProjectId])

  const { data, isLoading } = useQuery({
    queryKey: ['sv-scripts', loadedProjectId],
    queryFn: () => shortvideoApi.svScriptList({ projectId: loadedProjectId, rows: 100 }),
    enabled: loadedProjectId > 0,
  })
  const shots: ShotScript[] = (data?.list ?? []).map(s => ({
    id: Number(s.id ?? 0),
    projectId: Number(s.projectId ?? 0),
    shotNo: Number(s.shotNo ?? 0),
    shotType: String(s.shotType ?? 'normal'),
    duration: Number(s.duration ?? 5),
    description: String(s.description ?? ''),
    dialogue: String(s.dialogue ?? ''),
    cameraAngle: String(s.cameraAngle ?? '中景'),
    createTime: String(s.createTime ?? ''),
  }))

  const saveMutation = useMutation({
    mutationFn: async (p: Partial<ShotScript>): Promise<void> => {
      await shortvideoApi.svScriptSave({ ...p, projectId: loadedProjectId })
    },
    onSuccess: () => { toast('保存成功', 'success'); qc.invalidateQueries({ queryKey: ['sv-scripts'] }); setOpen(false) },
    onError: () => toast('保存失败', 'error'),
  })
  const deleteMutation = useMutation({
    mutationFn: async (id: number): Promise<void> => { await shortvideoApi.svScriptDelete(id) },
    onSuccess: () => { toast('删除成功', 'success'); qc.invalidateQueries({ queryKey: ['sv-scripts'] }) },
    onError: () => toast('删除失败', 'error'),
  })

  const handleLoad = () => {
    const id = Number(projectId)
    if (id > 0) setLoadedProjectId(id)
  }

  const handleOpen = (shot?: ShotScript) => {
    setEditing(shot ? { ...shot } : { shotNo: (shots.length + 1), shotType: 'normal', duration: 5, cameraAngle: '中景' })
    setOpen(true)
  }

  const SHOT_TYPES = ['opening', 'product', 'demo', 'testimonial', 'transition', 'closing', 'normal']
  const CAMERA_ANGLES = ['特写', '近景', '中景', '全景', '俯拍', '仰拍', '侧拍']

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField size="small" label="项目 ID" value={projectId}
          onChange={e => setProjectId(e.target.value)} sx={{ width: 160 }} />
        <Button variant="outlined" size="small" onClick={handleLoad}>加载分镜</Button>
        <Box sx={{ flex: 1 }} />
               {loadedProjectId > 0 && (
          <>
            <Button
              variant="outlined"
              size="small"
              startIcon={<MovieFilterIcon />}
              onClick={() => navigate(`${shortvideoRoutes.materialProduction}?projectId=${loadedProjectId}`)}
            >
              去素材生产一键生成
            </Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpen()}>新增分镜</Button>
          </>
        )}
      </Stack>

      {isLoading && <Typography color="text.secondary">加载中...</Typography>}

      {shots.length > 0 && (
        <Stack spacing={1.5}>
          {shots.map((shot, idx) => (
            <Card key={shot.id} variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" alignItems="flex-start" spacing={2}>
                  <Box sx={{ width: 36, height: 36, bgcolor: 'primary.main', color: 'white', borderRadius: '50%',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 700, flexShrink: 0 }}>
                    {idx + 1}
                  </Box>
                  <Box sx={{ flex: 1 }}>
                    <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                      <Chip label={shot.shotType} size="small" color="primary" variant="outlined" />
                      <Chip label={shot.cameraAngle} size="small" />
                      <Chip label={`${shot.duration}s`} size="small" color="secondary" />
                    </Stack>
                    <Typography variant="body2" fontWeight={500} mb={0.25}>{shot.description || '（无描述）'}</Typography>
                    {shot.dialogue && (
                      <Typography variant="caption" color="text.secondary" sx={{ fontStyle: 'italic' }}>台词：{shot.dialogue}</Typography>
                    )}
                  </Box>
                  <Stack direction="row" spacing={0.5}>
                    <Button size="small" onClick={() => handleOpen(shot)}>编辑</Button>
                    <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(shot.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}

      {loadedProjectId > 0 && shots.length === 0 && !isLoading && (
        <Typography color="text.secondary" textAlign="center" sx={{ py: 4 }}>暂无分镜，点击「新增分镜」开始创作</Typography>
      )}

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing.id ? '编辑分镜' : '新增分镜'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Stack direction="row" spacing={2}>
              <TextField label="分镜编号" type="number" value={editing.shotNo ?? ''}
                onChange={e => setEditing(p => ({ ...p, shotNo: Number(e.target.value) }))}
                size="small" sx={{ width: 120 }} />
              <TextField select label="镜头类型" value={editing.shotType ?? 'normal'}
                onChange={e => setEditing(p => ({ ...p, shotType: e.target.value }))}
                size="small" sx={{ flex: 1 }}>
                {SHOT_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </TextField>
              <TextField select label="景别" value={editing.cameraAngle ?? '中景'}
                onChange={e => setEditing(p => ({ ...p, cameraAngle: e.target.value }))}
                size="small" sx={{ width: 120 }}>
                {CAMERA_ANGLES.map(a => <MenuItem key={a} value={a}>{a}</MenuItem>)}
              </TextField>
              <TextField label="时长(s)" type="number" value={editing.duration ?? 5}
                onChange={e => setEditing(p => ({ ...p, duration: Number(e.target.value) }))}
                size="small" sx={{ width: 100 }} />
            </Stack>
            <TextField label="画面描述" value={editing.description ?? ''}
              onChange={e => setEditing(p => ({ ...p, description: e.target.value }))}
              multiline minRows={2} fullWidth size="small" />
            <TextField label="台词/旁白" value={editing.dialogue ?? ''}
              onChange={e => setEditing(p => ({ ...p, dialogue: e.target.value }))}
              multiline minRows={2} fullWidth size="small" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => saveMutation.mutate(editing)} disabled={saveMutation.isPending}>保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
