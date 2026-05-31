import { useState, useRef } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, IconButton, LinearProgress, Stack, TextField, Typography } from '@mui/material'
import { alpha } from '@mui/material/styles'
import DeleteIcon from '@mui/icons-material/Delete'
import UploadIcon from '@mui/icons-material/Upload'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { EmptyState, PageHeader } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

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
const MATERIAL_LIST_ENDPOINT = '/short-video/library/list'
const MATERIAL_DELETE_ENDPOINT = '/short-video/library/delete'
const FINAL_VIDEO_UPLOAD_ENDPOINT = '/short-video/upload/final-video'
const MATERIAL_READY_ENDPOINTS = [
  MATERIAL_LIST_ENDPOINT,
  MATERIAL_DELETE_ENDPOINT,
  FINAL_VIDEO_UPLOAD_ENDPOINT,
] as const
const MATERIAL_READY_ROUTES = [
  shortvideoRoutes.material,
  `${shortvideoRoutes.material}?projectId=:id`,
  shortvideoRoutes.materialProduction,
].join('|')
const MATERIAL_SUPPORTED_ACTIONS = [
  'refresh-material-library',
  'filter-materials',
  'upload-final-video',
  'delete-material',
  'paginate-materials',
].join('|')
const MATERIAL_UNSUPPORTED_ENDPOINTS = [
  '/short-video/library/mock',
  '/short-video/library/local-list',
  '/short-video/library/local-delete',
  '/short-video/library/static-materials',
  '/short-video/upload/local-final-video',
  '/short-video/upload/mock-final-video',
] as const

export default function MaterialPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [materialType, setMaterialType] = useState('')
  const [projectId, setProjectId] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading, isError, error, refetch } = useQuery({
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
      fileSize: m.fileSize != null ? Number(m.fileSize) : undefined,
      duration: m.duration != null ? Number(m.duration) : undefined,
    }
  })
  const total: number = data?.total ?? materials.length

  const deleteMutation = useMutation({
    mutationFn: async (id: number): Promise<void> => { await shortvideoApi.materialDelete(id) },
    onSuccess: () => {
      toast('删除成功', 'success')
      setDeleteError('')
      qc.invalidateQueries({ queryKey: ['sv-materials'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setDeleteError(`素材删除失败（POST ${MATERIAL_DELETE_ENDPOINT}）：${message}。素材行会保留，不做前端本地删除。`)
      toast(`删除失败：${message}`, 'error')
    },
  })

  const parseUploadError = async (resp: Response) => {
    const contentType = resp.headers.get('content-type') ?? ''
    if (contentType.includes('application/json')) {
      const json = await resp.json().catch(() => null) as { message?: unknown; error?: unknown; data?: unknown } | null
      const message = json?.message ?? json?.error ?? json?.data
      if (message != null && String(message).trim()) return String(message)
    }
    const text = await resp.text().catch(() => '')
    return text || `${resp.status} ${resp.statusText}`
  }

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const pid = projectId.trim() ? Number(projectId) : NaN
    if (!Number.isFinite(pid) || pid <= 0) {
      toast('请先填写项目 ID（成片上传到该项目目录）', 'warning')
      setUploadError('成片上传接口要求 query 参数 projectId，请先填写有效项目 ID。')
      return
    }
    setUploading(true)
    setUploadError('')
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
      const contentType = resp.headers.get('content-type') ?? ''
      const json = contentType.includes('application/json') ? await resp.clone().json().catch(() => null) as { status?: unknown; message?: unknown; data?: unknown } | null : null
      if (resp.ok && (!json || json.status === 200)) {
        toast('上传成功', 'success')
        setUploadError('')
        qc.invalidateQueries({ queryKey: ['sv-materials'] })
      } else {
        const detail = json?.message != null ? String(json.message) : await parseUploadError(resp)
        setUploadError(`成片上传失败（POST ${FINAL_VIDEO_UPLOAD_ENDPOINT}）：${detail}。项目 ID 与已加载素材列表会保留。`)
        toast('上传失败', 'error')
      }
    } catch (e) {
      const message = getErrorMessage(e)
      setUploadError(`成片上传失败（POST ${FINAL_VIDEO_UPLOAD_ENDPOINT}）：${message}。项目 ID 与已加载素材列表会保留。`)
      toast(`上传失败：${message}`, 'error')
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
    if (fileType?.startsWith('video')) return 'Video'
    if (fileType?.startsWith('image')) return 'Image'
    if (fileType?.startsWith('audio')) return 'Audio'
    return 'File'
  }

  return (
    <Box
      data-testid="shortvideo-material-page"
      data-contract-scope="shortvideo-material-library"
      data-ready-endpoints={MATERIAL_READY_ENDPOINTS.join('|')}
      data-ready-routes={MATERIAL_READY_ROUTES}
      data-supported-actions={MATERIAL_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={MATERIAL_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-material-fallback="true"
      data-server-pagination="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="素材库"
        breadcrumbs={[{ label: '短视频' }, { label: '素材库' }]}
        subtitle={`查看 POST ${MATERIAL_LIST_ENDPOINT} 素材；成片上传走 POST ${FINAL_VIDEO_UPLOAD_ENDPOINT}?projectId=，项目 ID 必填。`}
        actions={
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetch()} disabled={isLoading} data-testid="shortvideo-material-refresh-button" data-source-endpoint={MATERIAL_LIST_ENDPOINT}>
            刷新
          </Button>
        }
      />
      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-material-boundary-contract"
        data-source-endpoints={MATERIAL_READY_ENDPOINTS.join('|')}
        data-no-local-material-fallback="true"
        data-no-local-upload-fallback="true"
        data-supported-actions={MATERIAL_SUPPORTED_ACTIONS}
      >
        本页只管理已入库素材。关键帧、图生视频和自动成片分别在“素材生产”和“视频剪辑”页面执行；这里的上传入口只对接 POST {FINAL_VIDEO_UPLOAD_ENDPOINT}，仅接受成片视频并绑定到指定项目目录。
      </Alert>
      <Stack
        direction="row"
        spacing={2}
        alignItems="center"
        flexWrap="wrap"
        data-testid="shortvideo-material-filter"
        data-source-endpoint={MATERIAL_LIST_ENDPOINT}
        data-upload-endpoint={FINAL_VIDEO_UPLOAD_ENDPOINT}
        data-input-retained="true"
      >
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
        <input ref={fileRef} type="file" accept="video/*" style={{ display: 'none' }} onChange={handleUpload} />
        <Button
          variant="contained"
          startIcon={uploading ? undefined : <UploadIcon />}
          onClick={() => fileRef.current?.click()}
          disabled={uploading}
          data-testid="shortvideo-material-upload-button"
          data-source-endpoint={FINAL_VIDEO_UPLOAD_ENDPOINT}
        >
          {uploading ? '上传中...' : '上传素材'}
        </Button>
      </Stack>

      {uploading && <LinearProgress />}
      {isLoading && <LinearProgress />}
      {isError && (
        <Alert
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          data-testid="shortvideo-material-list-error"
          data-source-endpoint={MATERIAL_LIST_ENDPOINT}
          data-no-local-material-fallback="true"
          data-input-retained="true"
        >
          素材列表加载失败（POST {MATERIAL_LIST_ENDPOINT}）：{getErrorMessage(error)}。请检查项目筛选参数；页面不会展示本地素材。
        </Alert>
      )}
      {uploadError && (
        <Alert
          severity="error"
          data-testid="shortvideo-material-upload-error"
          data-source-endpoint={FINAL_VIDEO_UPLOAD_ENDPOINT}
          data-no-local-upload-fallback="true"
          data-input-retained="true"
        >
          {uploadError}
        </Alert>
      )}
      {deleteError && (
        <Alert
          severity="error"
          onClose={() => setDeleteError('')}
          data-testid="shortvideo-material-delete-error"
          data-source-endpoint={MATERIAL_DELETE_ENDPOINT}
          data-no-local-material-mutation="true"
        >
          {deleteError}
        </Alert>
      )}
      {projectId.trim() && (
        <Alert
          severity="info"
          variant="outlined"
          data-testid="shortvideo-material-project-context"
          data-source-endpoint={MATERIAL_LIST_ENDPOINT}
          data-upload-endpoint={FINAL_VIDEO_UPLOAD_ENDPOINT}
          data-input-retained="true"
        >
          当前筛选/上传项目 ID：{projectId.trim()}。成片上传会写入该项目目录，列表筛选则依赖 POST {MATERIAL_LIST_ENDPOINT} 对 projectId 的支持。
        </Alert>
      )}

      <Typography variant="caption" color="text.secondary">共 {total} 个素材</Typography>

      <Box
        sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 2 }}
        data-testid="shortvideo-material-grid"
        data-source-endpoint={MATERIAL_LIST_ENDPOINT}
        data-no-local-material-fallback="true"
      >
        {materials.map((m) => (
          <Card
            key={m.id}
            variant="outlined"
            sx={{ position: 'relative' }}
            data-testid="shortvideo-material-card"
            data-source-endpoint={MATERIAL_LIST_ENDPOINT}
            data-material-id={m.id}
          >
            <Box
              data-testid="material-preview-surface"
              sx={(theme) => ({
                height: 120,
                bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.default : theme.palette.grey[100],
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '4px 4px 0 0',
                overflow: 'hidden',
              })}
            >
              {m.fileType?.startsWith('image') ? (
                <Box
                  component="img"
                  src={m.fileUrl + '@!200X120'}
                  alt={m.fileName}
                  sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                />
              ) : (
                <Typography variant="subtitle2" color="text.secondary" sx={{ lineHeight: 1 }}>{getFileIcon(m.fileType)}</Typography>
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
              data-testid="material-delete-button"
              data-source-endpoint={MATERIAL_DELETE_ENDPOINT}
              sx={(theme) => ({
                position: 'absolute',
                top: 4,
                right: 4,
                bgcolor: theme.palette.mode === 'dark'
                  ? alpha(theme.palette.background.paper, 0.88)
                  : alpha(theme.palette.common.white, 0.85),
                '&:hover': {
                  bgcolor: theme.palette.mode === 'dark'
                    ? alpha(theme.palette.error.main, 0.18)
                    : alpha(theme.palette.common.white, 0.96),
                },
              })}
              onClick={() => deleteMutation.mutate(m.id)}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Card>
        ))}
      </Box>

      {materials.length === 0 && !isLoading && (
        <Box
          data-testid="shortvideo-material-empty"
          data-source-endpoint={MATERIAL_LIST_ENDPOINT}
          data-no-local-material-fallback="true"
        >
          <EmptyState
            title="暂无素材"
            description={projectId.trim() ? `当前项目还没有素材。可以上传成片，或先到素材生产页生成关键帧/视频片段。若列表一直为空，检查 POST ${MATERIAL_LIST_ENDPOINT} 是否返回该项目的素材。` : '可以先填写项目 ID 筛选素材；上传成片前项目 ID 必填。'}
            action={{ text: '上传素材', onClick: () => fileRef.current?.click() }}
          />
        </Box>
      )}

      {total > 20 && (
        <Stack direction="row" spacing={1} justifyContent="center" data-testid="shortvideo-material-pagination" data-supported-actions="paginate-materials">
          <Button size="small" disabled={page === 0} onClick={() => setPage(p => p - 1)} data-testid="shortvideo-material-prev-page-button">上一页</Button>
          <Typography variant="body2" sx={{ lineHeight: '30px' }}>第 {page + 1} 页</Typography>
          <Button size="small" disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)} data-testid="shortvideo-material-next-page-button">下一页</Button>
        </Stack>
      )}
    </Box>
  )
}
