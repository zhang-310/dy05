import { useState, useRef } from 'react'
import { Box, TextField, Button, Chip, Tooltip, IconButton, Alert } from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { storageApi, type StorageFile } from '@/api/storage'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'

const STORAGE_READY_ENDPOINTS = '/storage/configured|/storage/list|/storage/upload|/storage/delete'
const STORAGE_UNSUPPORTED_ACTIONS = 'local-file-fallback|local-delete-mutation|client-prefix-bypass|server-export|directory-delete'

function fmtSize(bytes: number) {
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

function getFileName(key: string) {
  const clean = key.endsWith('/') ? key.slice(0, -1) : key
  return clean.split('/').pop() || clean
}

function getFileType(row: StorageFile) {
  if (row.directory) return 'directory'
  const ext = getFileName(row.key).split('.').pop()?.toLowerCase()
  if (!ext) return 'file'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext)) return 'image'
  if (['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) return 'video'
  if (['mp3', 'wav', 'aac', 'm4a'].includes(ext)) return 'audio'
  if (['pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'md'].includes(ext)) return 'document'
  return ext
}

export default function StoragePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [search, setSearch] = useState({ page: 0, rows: 20, prefixSuffix: '' })
  const [query, setQuery] = useState(search)
  const [deleteKey, setDeleteKey] = useState<string | null>(null)
  const {
    data: configured,
    isFetching: checkingConfig,
    isError: configIsError,
    error: configError,
    refetch: refetchConfigured,
  } = useQuery({
    queryKey: ['storage-configured'],
    queryFn: () => storageApi.configured(),
  })
  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['files', search],
    queryFn: () => storageApi.list(search.prefixSuffix ? { prefixSuffix: search.prefixSuffix } : {}),
  })

  const uploadMut = useMutation({
    mutationFn: (file: File) => storageApi.upload(file, search.prefixSuffix || undefined),
    onSuccess: () => { toast('上传成功', 'success'); qc.invalidateQueries({ queryKey: ['files'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: storageApi.delete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteKey(null); qc.invalidateQueries({ queryKey: ['files'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) uploadMut.mutate(file)
    e.target.value = ''
  }

  const columns: GridColDef[] = [
    { field: 'key', headerName: 'BOS Key', flex: 1.5, minWidth: 260 },
    { field: 'name', headerName: '文件名', flex: 1, minWidth: 180, valueGetter: (_, row) => getFileName((row as StorageFile).key) },
    { field: 'fileType', headerName: '类型', width: 110, renderCell: ({ row }) => <Chip label={getFileType(row as StorageFile)} size="small" /> },
    { field: 'size', headerName: '大小', width: 110, valueFormatter: (v: number | null) => v == null ? '-' : fmtSize(v) },
    {
      field: 'url', headerName: '预览/链接', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Tooltip title={(row as StorageFile).url ?? '目录或无公开 URL'}>
          <span>
            <IconButton
              size="small"
              disabled={!(row as StorageFile).url}
              onClick={() => (row as StorageFile).url && window.open((row as StorageFile).url ?? '', '_blank')}
            >
              <OpenInNewIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      ),
    },
    { field: 'lastModified', headerName: '最后修改', width: 170, valueFormatter: (v: string | null) => v ? formatDate(v) : '-' },
    {
      field: 'actions', headerName: '操作', width: 90, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" color="error" disabled={(row as StorageFile).directory} onClick={() => setDeleteKey((row as StorageFile).key)}>
          删除
        </Button>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField
        label="路径后缀"
        size="small"
        placeholder="如 2026-03-01/5001/keyframes"
        value={query.prefixSuffix}
        onChange={e => setQuery(q => ({ ...q, prefixSuffix: e.target.value }))}
        sx={{ width: 260 }}
      />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
      <Button onClick={() => { const empty = { page: 0, rows: 20, prefixSuffix: '' }; setQuery(empty); setSearch(empty) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <>
      <input ref={fileInputRef} type="file" hidden onChange={handleFileChange} />
      <Button variant="contained" startIcon={<UploadFileIcon />} onClick={() => fileInputRef.current?.click()} disabled={uploadMut.isPending}>
        上传文件
      </Button>
    </>
  )

  const rows = normalizeRows<StorageFile>(data)
  const listErrorMessage = error instanceof Error ? error.message : '存储列表加载失败，请检查 /storage/list 与 BOS 配置。'
  const uploadErrorMessage = uploadMut.isError ? `/storage/upload 上传失败：${getErrorMessage(uploadMut.error)}` : ''
  const deleteErrorMessage = deleteMut.isError ? `/storage/delete 删除失败：${getErrorMessage(deleteMut.error)}` : ''

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="storage-workbench"
      data-contract-scope="asset-storage-bos-browser"
      data-ready-endpoints={STORAGE_READY_ENDPOINTS}
      data-unsupported-actions={STORAGE_UNSUPPORTED_ACTIONS}
      data-prefix-suffix={search.prefixSuffix || ''}
      data-row-count={rows.length}
      data-configured={String(Boolean(configured))}
      data-config-check-error={String(configIsError)}
      data-list-error={String(isError)}
      data-upload-error={String(uploadMut.isError)}
      data-delete-error={String(deleteMut.isError)}
      data-delete-key={deleteKey ?? ''}
      data-no-local-file-fallback="true"
      data-no-local-delete-mutation="true"
      data-no-client-prefix-bypass="true"
    >
      <PageHeader
        title="文件存储"
        subtitle="按当前用户 BOS 路径列出对象，删除与预览均使用真实 key。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => { void refetchConfigured(); void refetch() }} disabled={isFetching || checkingConfig}>
            刷新
          </Button>
        )}
      />

      <Alert
        data-testid="storage-config-contract"
        data-contract-source="/storage/configured|/storage/list"
        data-configured={String(Boolean(configured))}
        data-config-error={String(configIsError)}
        data-prefix-mode="server-user-prefix"
        data-no-client-prefix-bypass="true"
        severity={configIsError || configured === false ? 'warning' : 'info'}
      >
        BOS 配置状态：
        {configIsError ? `检查失败（${configError instanceof Error ? configError.message : '请检查 /storage/configured 和管理员权限'}）` : checkingConfig ? '检查中' : configured ? '已配置' : '未配置'}。
        存储列表来自 <code>/storage/list</code>，后端只接受 <code>prefixSuffix</code> 并强制拼接当前用户 ID 前缀；BOS 未配置或没有对象时会返回空列表。
      </Alert>

      {isError && (
        <Box data-testid="storage-list-error" data-contract-source="/storage/list" data-prefix-suffix={search.prefixSuffix || ''} data-no-local-file-fallback="true">
          <ErrorAlert title="存储列表加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {configIsError && (
        <Box data-testid="storage-config-error" data-contract-source="/storage/configured" data-no-local-config-fallback="true">
          <ErrorAlert severity="warning" title="BOS 配置检查失败" message={configError instanceof Error ? configError.message : '请检查 /storage/configured 与管理员权限。'} onRetry={() => void refetchConfigured()} />
        </Box>
      )}
      {uploadErrorMessage && (
        <Alert data-testid="storage-upload-error" data-contract-source="/storage/upload" data-prefix-suffix={search.prefixSuffix || ''} data-no-local-file-fallback="true" severity="error">
          {uploadErrorMessage}；当前 prefixSuffix={search.prefixSuffix || '根路径'}，页面不会补造本地文件行。
        </Alert>
      )}
      {deleteErrorMessage && (
        <Alert data-testid="storage-delete-error" data-contract-source="/storage/delete" data-delete-key={deleteKey ?? ''} data-row-retained="true" data-no-local-delete-mutation="true" severity="error">
          {deleteErrorMessage}；文件 key={deleteKey ?? '-'} 已保留在列表和确认框中。
        </Alert>
      )}

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isFetching}
        rowCount={rows.length}
        getRowId={(row) => (row as StorageFile).key}
        paginationMode="client"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={(
          <Box data-testid="storage-search-contract" data-contract-source="/storage/list" data-prefix-suffix={query.prefixSuffix || ''} data-applied-prefix-suffix={search.prefixSuffix || ''} data-no-client-prefix-bypass="true" sx={{ display: 'contents' }}>
            {searchSlot}
          </Box>
        )}
        actionSlot={(
          <Box data-testid="storage-upload-contract" data-contract-source="/storage/upload" data-prefix-suffix={search.prefixSuffix || ''} data-no-local-file-fallback="true" sx={{ display: 'contents' }}>
            {actionSlot}
          </Box>
        )}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
      {deleteKey !== null && (
        <Box
          data-testid="storage-delete-contract"
          data-contract-source="/storage/delete"
          data-delete-key={deleteKey}
          data-row-retained={deleteMut.isError ? 'true' : 'false'}
          data-no-local-delete-mutation="true"
          sx={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}
        />
      )}
      <ConfirmDialog
        open={deleteKey !== null}
        title="确认删除"
        content={`${deleteErrorMessage ? `${deleteErrorMessage}\n` : ''}删除后文件「${deleteKey ?? '-'}」将从 BOS 存储中移除，无法恢复。确认删除？`}
        onConfirm={() => deleteKey !== null && deleteMut.mutate(deleteKey)}
        onClose={() => setDeleteKey(null)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
