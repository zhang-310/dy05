import { useCallback, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DownloadIcon from '@mui/icons-material/Download'
import type { GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ConfirmDialog, DataGridEmptyOverlay, ErrorAlert, FormDialog, PageHeader, StandardDataGrid } from '@/components/base'
import { scriptApi, type ViolationWord, type ViolationWordSave } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

const LEVELS = [
  { value: 1, label: '低', color: 'info' as const },
  { value: 2, label: '中', color: 'warning' as const },
  { value: 3, label: '高', color: 'error' as const },
]

const SCOPES = [
  { value: 'all', label: '全场景' },
  { value: 'live_only', label: '仅直播' },
  { value: 'video_only', label: '仅短视频' },
]

const VIOLATION_ROUTE = '/admin/script/violation-words'
const VIOLATION_READY_ENDPOINTS = [
  '/script/admin/violation/list',
  '/script/admin/violation/save',
  '/script/admin/violation/delete',
] as const
const VIOLATION_FILE_ENDPOINTS = [
  '/script/admin/violation/import',
  '/script/admin/violation/export',
] as const
const VIOLATION_CONTEXT_ENDPOINTS = [
  '/script/admin/violation/active',
  '/script/violation/check',
  '/script/violation/check-batch',
  '/script/violation/suggest-replacement',
] as const
const VIOLATION_UNSUPPORTED_ACTIONS = [
  'server-file-import',
  'server-file-export',
  'inline-active-list',
  'inline-suggest-replacement',
] as const

const contractList = (items: readonly string[]) => items.join('|')

const defaultForm: Partial<ViolationWordSave> = {
  word: '',
  level: 2,
  reason: '',
  scope: 'all',
  replacement: '',
  status: 1,
}

function levelValue(row: ViolationWord) {
  return Number(row.level ?? row.severity ?? 2)
}

function reasonValue(row: ViolationWord) {
  return row.reason ?? row.category ?? ''
}

function scopeLabel(value?: string) {
  return SCOPES.find((item) => item.value === value)?.label ?? value ?? '全场景'
}

export default function ViolationWordPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, keyword: '', reason: '', level: '' as number | '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ViolationWordSave>>(defaultForm)
  const [deleteTarget, setDeleteTarget] = useState<ViolationWord | null>(null)
  const [formError, setFormError] = useState('')
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['violation-words', search],
    queryFn: () => scriptApi.violationList({
      page: search.page,
      rows: search.rows,
      keyword: search.keyword || undefined,
      reason: search.reason || undefined,
      level: search.level === '' ? undefined : Number(search.level),
    }),
  })

  const saveMut = useMutation({
    mutationFn: scriptApi.violationSave,
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormError('')
      setFormOpen(false)
      qc.invalidateQueries({ queryKey: ['violation-words'] })
    },
    onError: (e, payload) => {
      const message = `违规词保存失败（POST /script/admin/violation/save）：${getErrorMessage(e)}。接口来源：/script/admin/violation/save（route=${VIOLATION_ROUTE}; wordId=${payload.id ?? '新增'}; word=${payload.word ?? '空'}; level=${payload.level ?? payload.severity ?? 2}; reason=${payload.reason ?? payload.category ?? '空'}; scope=${payload.scope ?? 'all'}）`
      setFormError(message)
      toast(message, 'error')
    },
  })

  const delMut = useMutation({
    mutationFn: (row: ViolationWord) => scriptApi.violationDelete(row.id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setActionError('')
      setDeleteTarget(null)
      qc.invalidateQueries({ queryKey: ['violation-words'] })
    },
    onError: (e, row) => {
      const message = `违规词删除失败（POST /script/admin/violation/delete）：${getErrorMessage(e)}。接口来源：/script/admin/violation/delete（route=${VIOLATION_ROUTE}; wordId=${row.id}; word=${row.word}; level=${levelValue(row)}; reason=${reasonValue(row) || '空'}; scope=${row.scope ?? 'all'}）`
      setActionError(message)
      toast(message, 'error')
    },
  })

  const rows = data?.list ?? []
  const diagnostics = useMemo(() => {
    const enabled = rows.filter((row) => row.status === 1).length
    const highRisk = rows.filter((row) => levelValue(row) >= 3).length
    const scoped = rows.filter((row) => row.scope && row.scope !== 'all').length
    return { enabled, disabled: rows.length - enabled, highRisk, scoped }
  }, [rows])

  const openAdd = useCallback(() => {
    setFormError('')
    setForm(defaultForm)
    setFormOpen(true)
  }, [])

  const openEdit = useCallback((row: ViolationWord) => {
    setFormError('')
    setForm({
      id: row.id,
      word: row.word,
      level: levelValue(row),
      reason: reasonValue(row),
      scope: row.scope ?? 'all',
      replacement: row.replacement,
      status: row.status,
    })
    setFormOpen(true)
  }, [])

  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])

  const handleExport = useCallback(() => {
    const csv = [
      '违规词,等级,原因,范围,替换词,状态',
      ...rows.map((row) => [
        row.word,
        levelValue(row),
        reasonValue(row),
        row.scope ?? 'all',
        row.replacement ?? '',
        row.status === 1 ? '启用' : '禁用',
      ].join(',')),
    ].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'violation-words.csv'
    anchor.click()
    URL.revokeObjectURL(url)
  }, [rows])

  const handleSave = () => {
    setFormError('')
    const word = String(form.word ?? '').trim()
    if (!word) {
      toast('请填写违规词', 'warning')
      return
    }
    saveMut.mutate({
      id: form.id,
      word,
      level: Number(form.level ?? 2),
      reason: String(form.reason ?? '').trim() || undefined,
      scope: String(form.scope ?? 'all'),
      replacement: String(form.replacement ?? '').trim() || undefined,
      status: Number(form.status ?? 1),
    })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'word', headerName: '违规词', flex: 1, minWidth: 130 },
    {
      field: 'level',
      headerName: '等级',
      width: 90,
      renderCell: ({ row }) => {
        const level = levelValue(row as ViolationWord)
        const opt = LEVELS.find((item) => item.value === level)
        return <Chip label={opt?.label ?? level} color={opt?.color ?? 'default'} size="small" />
      },
    },
    { field: 'reason', headerName: '原因', flex: 1, minWidth: 140, valueGetter: (_, row) => reasonValue(row as ViolationWord) },
    { field: 'scope', headerName: '范围', width: 100, valueFormatter: (v: string) => scopeLabel(v) },
    { field: 'replacement', headerName: '替换词', flex: 1, minWidth: 130 },
    {
      field: 'status',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions',
      headerName: '操作',
      width: 150,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={0.5}>
          <Button size="small" onClick={() => openEdit(row as ViolationWord)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteTarget(row as ViolationWord)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField
        size="small"
        label="违规词"
        value={query.keyword}
        onChange={(e) => setQuery((q) => ({ ...q, keyword: e.target.value }))}
        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        sx={{ width: 150 }}
      />
      <TextField
        size="small"
        label="原因"
        value={query.reason}
        onChange={(e) => setQuery((q) => ({ ...q, reason: e.target.value }))}
        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        sx={{ width: 150 }}
      />
      <TextField
        select
        size="small"
        label="等级"
        value={query.level}
        onChange={(e) => setQuery((q) => ({ ...q, level: e.target.value === '' ? '' : Number(e.target.value) }))}
        sx={{ width: 110 }}
      >
        <MenuItem value="">全部</MenuItem>
        {LEVELS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </>
  )

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, minHeight: 'calc(100vh - 48px)' }}
      data-testid="violation-word-workbench"
      data-contract-scope="script-violation-word-admin"
      data-ready-endpoints={contractList(VIOLATION_READY_ENDPOINTS)}
      data-file-endpoints={contractList(VIOLATION_FILE_ENDPOINTS)}
      data-context-endpoints={contractList(VIOLATION_CONTEXT_ENDPOINTS)}
      data-unsupported-actions={contractList(VIOLATION_UNSUPPORTED_ACTIONS)}
      data-keyword={search.keyword}
      data-reason={search.reason}
      data-level={search.level}
      data-page={search.page}
      data-rows={search.rows}
      data-row-count={rows.length}
      data-total={data?.total ?? 0}
      data-enabled-count={diagnostics.enabled}
      data-high-risk-count={diagnostics.highRisk}
    >
      <PageHeader
        title="违规词库"
        subtitle="对齐 /script/admin/violation：字段以 level、reason、scope 为准，旧 severity/category 只作为兼容读取。"
        breadcrumbs={[{ label: '话术' }, { label: '违规词库' }]}
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={handleExport}
              data-testid="violation-word-local-export-button"
              data-contract-action="local-current-page-csv"
              data-file-endpoint={VIOLATION_FILE_ENDPOINTS[1]}
            >
              导出当前页
            </Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增违规词</Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="violation-word-contract-alert"
        data-contract-gap="file-stream-import-export"
        data-local-export="current-page-csv"
        data-no-server-import-request="true"
        data-no-server-export-request="true"
        data-no-active-list-request="true"
        data-no-suggest-replacement-request="true"
      >
        批量 CSV 导入/全量导出后端已提供 <code>/script/admin/violation/import|export</code>，但当前 request 工具只解包 JSON；
        本页先提供当前页 CSV 导出，服务端文件流导入导出需要后续单独接入 axios blob/form-data。
      </Alert>

      <Grid container spacing={2}>
        {[
          { key: 'current-page', label: '当前页词条', value: rows.length, source: '/script/admin/violation/list list.length' },
          { key: 'enabled', label: '启用', value: diagnostics.enabled, source: 'local-derived status=1' },
          { key: 'high-risk', label: '高风险', value: diagnostics.highRisk, source: 'local-derived level>=3' },
          { key: 'scoped', label: '限定范围', value: diagnostics.scoped, source: 'local-derived scope!=all' },
        ].map((item) => (
          <Grid item xs={6} md={3} key={item.key}>
            <Card
              variant="outlined"
              data-testid={`violation-word-kpi-${item.key}`}
              data-contract-source={item.source}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <ErrorAlert
          title="违规词加载失败"
          message={`${getErrorMessage(error)}。接口来源：/script/admin/violation/list，请检查管理员权限和登录态。`}
          onRetry={() => refetch()}
        />
      )}
      {saveMut.isError && (
        <Alert severity="error">
          {formError || `保存失败：${getErrorMessage(saveMut.error)}。接口来源：/script/admin/violation/save`}
        </Alert>
      )}
      {delMut.isError && (
        <Alert severity="error">
          {actionError || `删除失败：${getErrorMessage(delMut.error)}。接口来源：/script/admin/violation/delete`}；列表不会本地移除该违规词。
        </Alert>
      )}

      <Box sx={{ flex: 1, minHeight: 460 }}>
        <StandardDataGrid
          rows={rows}
          columns={columns}
          rowCount={data?.total ?? 0}
          loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={(m) => setSearch((s) => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          showExport={false}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      <FormDialog
        open={formOpen}
        title={form.id ? '编辑违规词' : '新增违规词'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave}
        loading={saveMut.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          {formError ? (
            <Alert severity="error">
              {formError}。保存失败会保留当前违规词表单输入。
            </Alert>
          ) : null}
          <TextField label="违规词" required value={form.word ?? ''} onChange={(e) => setForm((f) => ({ ...f, word: e.target.value }))} fullWidth size="small" />
          <TextField select label="等级" value={form.level ?? 2} onChange={(e) => setForm((f) => ({ ...f, level: Number(e.target.value) }))} size="small" fullWidth>
            {LEVELS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
          </TextField>
          <TextField label="原因" value={form.reason ?? ''} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} fullWidth size="small" />
          <TextField select label="范围" value={form.scope ?? 'all'} onChange={(e) => setForm((f) => ({ ...f, scope: e.target.value }))} size="small" fullWidth>
            {SCOPES.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
          </TextField>
          <TextField label="替换词" value={form.replacement ?? ''} onChange={(e) => setForm((f) => ({ ...f, replacement: e.target.value }))} fullWidth size="small" />
          <TextField select label="状态" value={form.status ?? 1} onChange={(e) => setForm((f) => ({ ...f, status: Number(e.target.value) }))} size="small" fullWidth>
            <MenuItem value={1}>启用</MenuItem>
            <MenuItem value={0}>禁用</MenuItem>
          </TextField>
        </Stack>
      </FormDialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        content={`确定要删除该违规词吗？endpoint=/script/admin/violation/delete; wordId=${deleteTarget?.id ?? '-'}; word=${deleteTarget?.word ?? '未知词'}; level=${deleteTarget ? levelValue(deleteTarget) : '-'}。删除失败会保留违规词行和当前筛选。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending}
      />
    </Box>
  )
}
