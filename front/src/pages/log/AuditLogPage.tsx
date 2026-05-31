import { useState, useCallback, type ReactNode } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Drawer, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { alpha } from '@mui/material/styles'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { logApi, type AuditLog, type AuditLogQuery } from '@/api/log'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

type AuditLogSearch = Required<Pick<AuditLogQuery, 'page' | 'rows'>> &
  Pick<AuditLogQuery, 'keyword' | 'username' | 'entity' | 'action' | 'status' | 'startTime' | 'endTime'>

const initialSearch: AuditLogSearch = { page: 0, rows: 20, keyword: '', username: '', entity: '', action: '', startTime: '', endTime: '' }

const statusOptions = [
  { value: '', label: '全部结果' },
  { value: '1', label: '成功' },
  { value: '0', label: '失败' },
]
const AUDIT_LOG_ENDPOINTS = ['/log/audit/search'].join('|')
const AUDIT_LOG_UNSUPPORTED_ACTIONS = [
  'local-audit-log-fallback',
  'synthetic-detail-fetch',
  'client-side-value-diff',
  'local-before-after-fallback',
].join('|')

export default function AuditLogPage() {
  const [search, setSearch] = useState<AuditLogSearch>(initialSearch)
  const [draft, setDraft] = useState<AuditLogSearch>(initialSearch)
  const [selected, setSelected] = useState<AuditLog | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['audit-logs', search],
    queryFn: () => logApi.auditLogPage(toRequest(search)),
  })

  const handleSearch = useCallback(() => setSearch({ ...draft, page: 0 }), [draft])
  const handleReset = useCallback(() => {
    setDraft(initialSearch)
    setSearch(initialSearch)
  }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'username', headerName: '操作人', width: 120, valueGetter: (_value, row) => row.username ?? '-' },
    { field: 'entity', headerName: '实体', width: 130, valueGetter: (_value, row) => row.entity ?? row.targetType ?? '-' },
    { field: 'entityId', headerName: '对象ID', width: 90, valueGetter: (_value, row) => row.entityId ?? row.targetId ?? '-' },
    { field: 'action', headerName: '操作', width: 120 },
    {
      field: 'status',
      headerName: '结果',
      width: 90,
      renderCell: ({ value }) => <Chip size="small" color={value === 0 ? 'error' : 'success'} label={value === 0 ? '失败' : '成功'} />,
    },
    { field: 'ip', headerName: 'IP', width: 130, valueGetter: (_value, row) => row.ip ?? '-' },
    {
      field: 'createTime', headerName: '时间', width: 170,
      renderCell: ({ value }) => formatDate(value),
    },
    {
      field: '_actions', headerName: '操作', width: 80,
      renderCell: ({ row }) => (
        <Button
          size="small"
          variant="text"
          data-testid="audit-log-detail-action"
          aria-label={`查看审计日志 ${String((row as AuditLog).id ?? '')} 详情`}
          data-log-id={(row as AuditLog).id}
          onClick={() => setSelected(row as AuditLog)}
        >详情</Button>
      ),
    },
  ]

  const rows = normalizeRows<AuditLog>(data)
  const total = readTotal(data, rows.length)
  const failedCount = rows.filter(row => row.status === 0).length
  const changedCount = rows.filter(row => Boolean(row.oldValue ?? row.beforeValue ?? row.newValue ?? row.afterValue)).length

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
      <TextField size="small" label="关键词" value={draft.keyword ?? ''}
        onChange={e => setDraft(d => ({ ...d, keyword: e.target.value }))} sx={{ width: 150 }} />
      <TextField size="small" label="操作人" name="username" value={draft.username ?? ''}
        onChange={e => setDraft(d => ({ ...d, username: e.target.value }))} sx={{ width: 140 }} />
      <TextField size="small" label="实体" value={draft.entity ?? ''}
        onChange={e => setDraft(d => ({ ...d, entity: e.target.value }))} sx={{ width: 120 }} />
      <TextField size="small" label="操作类型" value={draft.action ?? ''}
        onChange={e => setDraft(d => ({ ...d, action: e.target.value }))} sx={{ width: 120 }} />
      <TextField
        select
        size="small"
        label="结果"
        value={draft.status ?? ''}
        onChange={e => setDraft(d => ({ ...d, status: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 120 }}
      >
        {statusOptions.map(option => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
      </TextField>
      <TextField size="small" type="date" label="开始时间" name="startDate" InputLabelProps={{ shrink: true }}
        value={draft.startTime ?? ''}
        onChange={e => setDraft(d => ({ ...d, startTime: e.target.value }))} sx={{ width: 160 }} />
      <TextField size="small" type="date" label="结束时间" name="endDate" InputLabelProps={{ shrink: true }}
        value={draft.endTime ?? ''}
        onChange={e => setDraft(d => ({ ...d, endTime: e.target.value }))} sx={{ width: 160 }} />
      <Button size="small" variant="contained" onClick={handleSearch}>查询</Button>
      <Button size="small" onClick={handleReset}>重置</Button>
    </Stack>
  )

  const listErrorMessage = error instanceof Error ? error.message : '审计日志加载失败，请检查 /log/audit/search。'

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="audit-log-page-workbench"
      data-contract-scope="log-audit-readonly-detail"
      data-ready-endpoints={AUDIT_LOG_ENDPOINTS}
      data-unsupported-actions={AUDIT_LOG_UNSUPPORTED_ACTIONS}
      data-row-count={rows.length}
      data-total-count={total}
      data-failed-count={failedCount}
      data-changed-count={changedCount}
      data-query-keyword={search.keyword || ''}
      data-query-entity={search.entity || ''}
      data-detail-open={String(Boolean(selected))}
      data-selected-id={selected?.id ?? ''}
      data-list-error={String(isError)}
      data-no-local-audit-log-fallback="true"
      data-no-synthetic-detail-fetch="true"
      data-no-client-side-value-diff="true"
    >
      <PageHeader
        title="审计日志"
        subtitle="记录高风险对象修改前后值，便于追踪配置、商品、话术等关键数据变更。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <MetricCard label="当前页审计" value={rows.length} hint={`总数 ${total}`} />
        <MetricCard label="失败审计" value={failedCount} hint="status=0" color={failedCount > 0 ? 'error.main' : 'success.main'} />
        <MetricCard label="包含变更值" value={changedCount} hint="oldValue/newValue 已落库" />
      </Stack>

      <Alert
        severity="info"
        data-testid="audit-log-source-contract"
        data-contract-source="/log/audit/search"
        data-query-fields="keyword|username|entity|action|status|startTime|endTime"
        data-no-local-audit-log-fallback="true"
        data-no-synthetic-detail-fetch="true"
      >
        审计日志来自 `/log/audit/search`，读取 common 模块 `audit_log` 表；详情抽屉展示实体、IP、User-Agent、修改前后值和失败原因。
      </Alert>
      {isError && (
        <Box
          data-testid="audit-log-list-error"
          data-contract-source="/log/audit/search"
          data-no-local-audit-log-fallback="true"
        >
          <ErrorAlert title="审计日志加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}

      <Box
        data-testid="audit-log-grid-contract"
        data-contract-source="/log/audit/search"
        data-row-count={rows.length}
        data-total-count={total}
        data-no-local-audit-log-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={total}
          loading={isFetching} paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={(
            <Box
              data-testid="audit-log-search-contract"
              data-contract-source="/log/audit/search"
              data-draft-keyword={draft.keyword || ''}
              data-applied-keyword={search.keyword || ''}
              data-applied-entity={search.entity || ''}
              data-applied-action={search.action || ''}
              data-applied-status={search.status ?? ''}
              data-no-client-side-filter-only="true"
              sx={{ display: 'contents' }}
            >
              {searchSlot}
            </Box>
          )}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>
      <Drawer anchor="right" open={!!selected} onClose={() => setSelected(null)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 560 }, p: 3 } }}>
        {selected && (
          <Stack
            spacing={2}
            className="log-detail"
            data-testid="audit-log-detail-contract"
            data-contract-source="/log/audit/search"
            data-selected-id={selected.id}
            data-has-before={String(Boolean(readBeforeValue(selected)))}
            data-has-after={String(Boolean(readAfterValue(selected)))}
            data-no-synthetic-detail-fetch="true"
            data-no-client-side-value-diff="true"
          >
            <Typography variant="h6">审计日志详情</Typography>
            <Divider />
            <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
              <Chip label={`实体: ${selected.entity ?? selected.targetType ?? '-'}`} size="small" />
              <Chip label={`操作: ${selected.action}`} size="small" color="primary" />
              <Chip label={selected.status === 0 ? '失败' : '成功'} size="small" color={selected.status === 0 ? 'error' : 'success'} />
              <Chip label={`IP: ${selected.ip ?? '-'}`} size="small" variant="outlined" />
            </Stack>
            <Box>
              <Typography variant="caption" color="text.secondary">操作人</Typography>
              <Typography>{selected.username ?? '-'} (ID: {selected.userId ?? '-'})</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">操作对象</Typography>
              <Typography>{selected.entity ?? selected.targetType ?? '-'} #{selected.entityId ?? selected.targetId ?? '-'}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">User-Agent</Typography>
              <Typography sx={{ wordBreak: 'break-all' }}>{selected.userAgent ?? '-'}</Typography>
            </Box>
            {selected.errorMsg && (
              <Alert severity="error">{selected.errorMsg}</Alert>
            )}
            <Divider />
            {readBeforeValue(selected) ? (
              <Box>
                <Typography variant="caption" color="text.secondary">修改前</Typography>
                <Box
                  component="pre"
                  data-testid="audit-log-before-value-surface"
                  sx={(theme) => ({
                    fontSize: 12,
                    bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.default : alpha(theme.palette.common.black, 0.025),
                    border: '1px solid',
                    borderColor: 'divider',
                    p: 1,
                    borderRadius: 1,
                    overflow: 'auto',
                    maxHeight: 200,
                  })}
                >
                  {readBeforeValue(selected)}
                </Box>
              </Box>
            ) : (
              <Alert
                severity="warning"
                data-testid="audit-log-before-missing"
                data-no-local-before-after-fallback="true"
              >
                该审计记录没有写入修改前内容，通常表示当前操作为创建、登录、删除或切面未捕获旧值。
              </Alert>
            )}
            {readAfterValue(selected) ? (
              <Box>
                <Typography variant="caption" color="text.secondary">修改后</Typography>
                <Box
                  component="pre"
                  data-testid="audit-log-after-value-surface"
                  sx={(theme) => ({
                    fontSize: 12,
                    bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
                    border: '1px solid',
                    borderColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.42 : 0.24),
                    p: 1,
                    borderRadius: 1,
                    overflow: 'auto',
                    maxHeight: 200,
                  })}
                >
                  {readAfterValue(selected)}
                </Box>
              </Box>
            ) : (
              <Alert
                severity="warning"
                data-testid="audit-log-after-missing"
                data-no-local-before-after-fallback="true"
              >
                该审计记录没有写入修改后内容，详情需结合业务操作日志或后端切面配置排查。
              </Alert>
            )}
            <Typography variant="caption" color="text.secondary">{formatDate(selected.createTime)}</Typography>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}

function toRequest(search: AuditLogSearch): AuditLogQuery {
  return {
    page: search.page,
    rows: search.rows,
    keyword: search.keyword || undefined,
    username: search.username || undefined,
    entity: search.entity || undefined,
    action: search.action || undefined,
    status: search.status,
    startTime: search.startTime || undefined,
    endTime: search.endTime || undefined,
  }
}

function readBeforeValue(log: AuditLog) {
  return log.oldValue ?? log.beforeValue ?? ''
}

function readAfterValue(log: AuditLog) {
  return log.newValue ?? log.afterValue ?? ''
}

function MetricCard({ label, value, hint, color = 'text.primary' }: { label: string; value: ReactNode; hint: string; color?: string }) {
  return (
    <Card variant="outlined" sx={{ flex: 1 }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" sx={{ color }}>{value}</Typography>
        <Typography variant="caption" color="text.secondary">{hint}</Typography>
      </CardContent>
    </Card>
  )
}
