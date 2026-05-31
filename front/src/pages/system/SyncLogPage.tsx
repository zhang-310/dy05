import { useState } from 'react'
import { Alert, Button } from '@mui/material'
import {
  Box, Chip, Drawer, IconButton, Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ErrorAlert, DataGridEmptyOverlay, EmptyState } from '@/components/base'
import { systemApi, type SyncLog } from '@/api/system'
import { useQuery } from '@tanstack/react-query'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const SYNC_LOG_ENDPOINTS = ['/system/sync-log/list'].join('|')
const SYNC_LOG_UNSUPPORTED_ACTIONS = [
  'local-sync-log-fallback',
  'status-inference-from-counts',
  'synthetic-detail-fetch',
  'client-side-sync-retry',
  'local-export-csv',
].join('|')
const SYNC_LOG_UNSUPPORTED_ENDPOINTS = [
  '/system/sync-log/get',
  '/system/sync-log/retry',
  '/system/sync-log/export',
].join('|')

function StatusChip({ status }: { status: string }) {
  const color = status === 'SUCCESS' ? 'success' : status === 'FAILED' ? 'error' : 'default'
  const label = status === 'SUCCESS' ? '成功' : status === 'FAILED' ? '失败' : status
  return <Chip label={label} color={color} size="small" />
}

export default function SyncLogPage() {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [selected, setSelected] = useState<SyncLog | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sync-log', page, pageSize],
    queryFn: () => systemApi.syncLogList({ page, rows: pageSize }),
  })

  const rows: SyncLog[] = normalizeRows<SyncLog>(data)
  const total = readTotal(data, rows.length)
  const mutationError = isError ? error : null

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'syncType', headerName: '同步类型', width: 150 },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: (p) => <StatusChip status={String(p.value ?? '')} />,
    },
    { field: 'totalCount', headerName: '总数', width: 90 },
    { field: 'successCount', headerName: '成功', width: 90 },
    { field: 'failCount', headerName: '失败', width: 90 },
    { field: 'startTime', headerName: '开始时间', width: 180 },
    { field: 'endTime', headerName: '结束时间', width: 180 },
    { field: 'createTime', headerName: '创建时间', width: 180 },
  ]

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="sync-log-page-workbench"
      data-contract-scope="system-sync-log-readonly-ledger"
      data-ready-endpoints={SYNC_LOG_ENDPOINTS}
      data-unsupported-actions={SYNC_LOG_UNSUPPORTED_ACTIONS}
      data-unsupported-endpoints={SYNC_LOG_UNSUPPORTED_ENDPOINTS}
      data-row-count={rows.length}
      data-total-count={total}
      data-page={page}
      data-page-size={pageSize}
      data-load-error={String(isError)}
      data-detail-open={String(Boolean(selected))}
      data-selected-id={selected?.id ?? ''}
      data-no-local-sync-log-fallback="true"
      data-no-status-inference="true"
      data-no-client-side-sync-retry="true"
      data-no-local-export-csv-fallback="true"
    >
      <PageHeader
        title="同步日志"
        subtitle="数据同步执行记录，展示同步类型、成功/失败计数与执行时间。"
        actions={(
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        data-testid="sync-log-source-contract"
        data-contract-source="/system/sync-log/list"
        data-unsupported-endpoints={SYNC_LOG_UNSUPPORTED_ENDPOINTS}
        data-no-local-sync-log-fallback="true"
        data-no-synthetic-detail-fetch="true"
        data-no-client-side-sync-retry="true"
      >
        列表来自 `/system/sync-log/list`，后端只按分页返回同步记录；详情抽屉展示的是落库字段，不额外补推断状态。
      </Alert>

      {mutationError && (
        <Box
          data-testid="sync-log-load-error"
          data-contract-source="/system/sync-log/list"
          data-no-local-sync-log-fallback="true"
        >
          <ErrorAlert
            title="同步日志加载失败"
            message={mutationError instanceof Error ? mutationError.message : '同步日志接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      <Box
        data-testid="sync-log-grid-contract"
        data-contract-source="/system/sync-log/list"
        data-row-count={rows.length}
        data-total-count={total}
        data-no-local-sync-log-fallback="true"
        data-no-client-side-sync-retry="true"
        data-no-local-export-csv-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={total}
          paginationMode="server"
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
          pageSizeOptions={[10, 20, 50]}
          disableRowSelectionOnClick
          getRowId={(r) => (r as SyncLog).id}
          onRowClick={(p) => setSelected(p.row as SyncLog)}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1, cursor: 'pointer' }}
        />
      </Box>

      <Drawer anchor="right" open={!!selected} onClose={() => setSelected(null)}
        PaperProps={{ sx: { width: 480, p: 3 } }}>
        <Box
          sx={{ display: 'flex', alignItems: 'center', mb: 2 }}
          data-testid="sync-log-detail-contract"
          data-contract-source="/system/sync-log/list"
          data-selected-id={selected?.id ?? ''}
          data-no-synthetic-detail-fetch="true"
          data-no-status-inference="true"
          data-no-client-side-sync-retry="true"
        >
          <Typography variant="h6" sx={{ flex: 1 }}>同步详情</Typography>
          <IconButton onClick={() => setSelected(null)}><CloseIcon /></IconButton>
        </Box>
        {selected && [
          { label: 'ID', value: String(selected.id) },
          { label: '同步类型', value: selected.syncType },
          { label: '状态', value: <StatusChip status={selected.status} /> },
          { label: '总数', value: String(selected.totalCount) },
          { label: '成功数', value: String(selected.successCount) },
          { label: '失败数', value: String(selected.failCount) },
          { label: '开始时间', value: selected.startTime },
          { label: '结束时间', value: selected.endTime },
          { label: '错误信息', value: selected.errorMessage ?? '-' },
          { label: '用户 ID', value: String(selected.userId ?? '-') },
          { label: '账号 ID', value: String(selected.accountId ?? '-') },
        ].map((row) => (
          <Box key={row.label} sx={{ display: 'flex', py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography variant="body2" color="text.secondary" sx={{ width: 100, flexShrink: 0 }}>{row.label}</Typography>
            <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
              {typeof row.value === 'string' ? row.value : row.value}
            </Typography>
          </Box>
        ))}
        {!selected && <EmptyState title="请选择一条同步日志" />}
      </Drawer>
    </Box>
  )
}
