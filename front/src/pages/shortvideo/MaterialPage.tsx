import { useState, useRef } from 'react'
import { Box, Button, Stack, Card, CardContent, Typography, Chip, IconButton, TextField, LinearProgress } from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import UploadIcon from '@mui/icons-material/Upload'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

interface Material {
  id: number
  projectId?: number
  fileName: string
  fileType: string
  fileSize?: number
  fileUrl: string
  tags?: string
  duration?: number
  createTime: string
}

export default function MaterialPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [materialType, setMaterialType] = useState('')
  const [projectId, setProjectId] = useState('')
  const [uploading, setUploading] = useState(false)
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['sv-materials', materialType, projectId, page],
    queryFn: () => shortvideoApi.materialList({
      materialType: materialType || undefined,
      projectId: projectId.trim() ? Number(projectId) : undefined,
      page,
      rows: 20,
    }),
  })
  const materials: Material[] = (data?.list ?? []).map(m => {
    const url = String(m.url ?? m.fileUrl ?? '')
    const mt = String(m.materialType ?? m.fileType ?? '')
    return {
      id: Number(m.id ?? 0),
      projectId: m.projectId != null ? Number(m.projectId) : undefined,
      fileName: url ? url.split('/').pop() ?? `素材-${m.id}` : `素材-${m.id}`,
      fileType: mt || 'unknown',
      fileUrl: url,
      createTime: String(m.createTime ?? ''),
      duration: m.duration != null ? Number(m.duration) : undefined,
    }
  })
  const total: number = data?.total ?? materials.length

  const deleteMutation = useMutation({
    mutationFn: async (id: number): Promise<void> => { await shortvideoApi.materialDelete(id) },
    onSuccess: () => { toast('删除成功', 'success'); qc.invalidateQueries({ queryKey: ['sv-materials'] }) },
    onError: () => toast('删除失败', 'error'),
  })

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const pid = projectId.trim() ? Number(projectId) : NaN
    if (!Number.isFinite(pid) || pid <= 0) {
      toast('请先填写项目 ID（成片上传到该项目目录）', 'warning')
      return
    }
    setUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const token = localStorage.getItem('token') ?? sessionStorage.getItem('token') ?? ''
      const url = `${shortvideoApi.materialUploadBase}?projectId=${encodeURIComponent(String(pid))}`
      const resp = await fetch(url, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      })
      if (resp.ok) {
        toast('上传成功', 'success')
        qc.invalidateQueries({ queryKey: ['sv-materials'] })
      } else {
        toast('上传失败', 'error')
      }
    } catch {
      toast('上传失败', 'error')
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  const formatSize = (bytes?: number) => {
    if (!bytes) return '—'
    if (bytes < 1024) return `${bytes}B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
    return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  }

  const getFileIcon = (fileType: string) => {
    if (fileType?.startsWith('video')) return '🎬'
    if (fileType?.startsWith('image')) return '🖼️'
    if (fileType?.startsWith('audio')) return '🎵'
    return '📄'
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
        <TextField
          size="small"
          label="素材类型（可选）"
          placeholder="如 image / video / audio"
          value={materialType}
          onChange={e => { setMaterialType(e.target.value); setPage(0) }}
          sx={{ width: 200 }}
        />
        <TextField
          size="small"
          label="项目 ID（筛选 / 上传必填）"
          value={projectId}
          onChange={e => { setProjectId(e.target.value); setPage(0) }}
          sx={{ width: 220 }}
        />
        <Box sx={{ flex: 1 }} />
        <input ref={fileRef} type="file" accept="video/*,image/*,audio/*" style={{ display: 'none' }} onChange={handleUpload} />
        <Button
          variant="contained"
          startIcon={uploading ? undefined : <UploadIcon />}
          onClick={() => fileRef.current?.click()}
          disabled={uploading}
        >
          {uploading ? '上传中...' : '上传素材'}
        </Button>
      </Stack>

      {uploading && <LinearProgress />}
      {isLoading && <LinearProgress />}

      <Typography variant="caption" color="text.secondary">共 {total} 个素材</Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 2 }}>
        {materials.map((m) => (
          <Card key={m.id} variant="outlined" sx={{ position: 'relative' }}>
            <Box sx={{ height: 120, bgcolor: 'grey.100', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '4px 4px 0 0', overflow: 'hidden' }}>
              {m.fileType?.startsWith('image') ? (
                <Box
                  component="img"
                  src={m.fileUrl + '@!200X120'}
                  alt={m.fileName}
                  sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                />
              ) : (
                <Typography variant="h3" sx={{ lineHeight: 1 }}>{getFileIcon(m.fileType)}</Typography>
              )}
            </Box>
            <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
              <Typography variant="caption" fontWeight={600} noWrap display="block" title={m.fileName}>
                {m.fileName}
              </Typography>
              <Stack direction="row" spacing={0.5} flexWrap="wrap" mt={0.5}>
                <Chip label={m.fileType?.split('/')[0] ?? 'file'} size="small" />
                <Chip label={formatSize(m.fileSize)} size="small" variant="outlined" />
                {m.duration && <Chip label={`${m.duration}s`} size="small" color="secondary" variant="outlined" />}
              </Stack>
              {m.tags && (
                <Typography variant="caption" color="text.secondary" display="block" mt={0.5} noWrap>
                  {m.tags}
                </Typography>
              )}
            </CardContent>
            <IconButton
              size="small"
              color="error"
              sx={{ position: 'absolute', top: 4, right: 4, bgcolor: 'rgba(255,255,255,0.85)', '&:hover': { bgcolor: 'white' } }}
              onClick={() => deleteMutation.mutate(m.id)}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Card>
        ))}
      </Box>

      {materials.length === 0 && !isLoading && (
        <Typography color="text.secondary" textAlign="center" sx={{ py: 4 }}>暂无素材，点击「上传素材」添加</Typography>
      )}

      {total > 20 && (
        <Stack direction="row" spacing={1} justifyContent="center">
          <Button size="small" disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</Button>
          <Typography variant="body2" sx={{ lineHeight: '30px' }}>第 {page + 1} 页</Typography>
          <Button size="small" disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>下一页</Button>
        </Stack>
      )}
    </Box>
  )
}
