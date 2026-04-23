import { useState } from 'react'
import { Box, TextField, Button, Chip, MenuItem, Select, FormControl, InputLabel, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Typography } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { paymentApi } from '@/api/payment'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' | 'info' }> = {
  0: { label: '待支付', color: 'warning' },
  1: { label: '已支付', color: 'info' },
  2: { label: '已发货', color: 'info' },
  3: { label: '已完成', color: 'success' },
  4: { label: '已取消', color: 'default' },
  5: { label: '已退款', color: 'error' },
}

const PAY_TYPES = ['alipay', 'wechat', 'bank']

export default function OrdersPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, orderNo: '', status: -1, payType: '' })
  const [query, setQuery] = useState(search)
  const [shipDialog, setShipDialog] = useState<{ open: boolean; id: number | null }>({ open: false, id: null })
  const [trackingNo, setTrackingNo] = useState('')

  const { data, isFetching } = useQuery({
    queryKey: ['payment-orders', search],
    queryFn: () => paymentApi.list({ ...search, status: search.status === -1 ? undefined : search.status, payType: search.payType || undefined }),
  })

  const confirmMut = useMutation({
    mutationFn: (id: number) => paymentApi.confirmPayment(id),
    onSuccess: () => { toast('已确认支付', 'success'); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const shipMut = useMutation({
    mutationFn: ({ id, trackingNo: tn }: { id: number; trackingNo: string }) => paymentApi.ship(id, tn),
    onSuccess: () => { toast('发货成功', 'success'); setShipDialog({ open: false, id: null }); setTrackingNo(''); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const completeMut = useMutation({
    mutationFn: (id: number) => paymentApi.complete(id),
    onSuccess: () => { toast('已完成', 'success'); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const cancelMut = useMutation({
    mutationFn: (id: number) => paymentApi.cancel(id),
    onSuccess: () => { toast('已取消', 'success'); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'orderNo', headerName: '订单号', width: 200 },
    { field: 'amount', headerName: '金额', width: 100, valueFormatter: (value: number) => `¥${(value ?? 0).toFixed(2)}` },
    { field: 'payType', headerName: '支付方式', width: 110 },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const s = STATUS_MAP[value as number] ?? { label: '未知', color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    { field: 'payTime', headerName: '支付时间', width: 160 },
    { field: 'trackingNo', headerName: '物流单号', width: 150 },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 220, sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {row.status === 0 && <Button size="small" onClick={() => confirmMut.mutate(row.id)} disabled={confirmMut.isPending}>确认</Button>}
          {row.status === 1 && <Button size="small" color="primary" onClick={() => { setShipDialog({ open: true, id: row.id }); setTrackingNo('') }} disabled={shipMut.isPending}>发货</Button>}
          {row.status === 2 && <Button size="small" color="success" onClick={() => completeMut.mutate(row.id)} disabled={completeMut.isPending}>完成</Button>}
          {(row.status === 0 || row.status === 1) && <Button size="small" color="error" onClick={() => cancelMut.mutate(row.id)} disabled={cancelMut.isPending}>取消</Button>}
        </Box>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField label="订单号" size="small" value={query.orderNo} onChange={e => setQuery(q => ({ ...q, orderNo: e.target.value }))} sx={{ width: 180 }} />
      <FormControl size="small" sx={{ minWidth: 110 }}>
        <InputLabel>状态</InputLabel>
        <Select label="状态" value={query.status} onChange={e => setQuery(q => ({ ...q, status: e.target.value as number }))}>
          <MenuItem value={-1}>全部</MenuItem>
          {Object.entries(STATUS_MAP).map(([k, v]) => <MenuItem key={k} value={Number(k)}>{v.label}</MenuItem>)}
        </Select>
      </FormControl>
      <FormControl size="small" sx={{ minWidth: 120 }}>
        <InputLabel>支付方式</InputLabel>
        <Select label="支付方式" value={query.payType} onChange={e => setQuery(q => ({ ...q, payType: e.target.value }))}>
          <MenuItem value="">全部</MenuItem>
          {PAY_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
        </Select>
      </FormControl>
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { const d = { page: 0, rows: 20, orderNo: '', status: -1, payType: '' }; setQuery(d); setSearch(d) }}>重置</Button>
    </>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>订单管理</Typography>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} sx={{ flex: 1 }}
      />

      {/* 发货对话框 */}
      <Dialog open={shipDialog.open} onClose={() => setShipDialog({ open: false, id: null })} maxWidth="xs" fullWidth>
        <DialogTitle>填写物流单号</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>请输入快递物流单号：</DialogContentText>
          <TextField autoFocus label="物流单号" fullWidth value={trackingNo} onChange={e => setTrackingNo(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShipDialog({ open: false, id: null })}>取消</Button>
          <Button variant="contained" disabled={!trackingNo || shipMut.isPending} onClick={() => shipDialog.id !== null && shipMut.mutate({ id: shipDialog.id, trackingNo })}>确认发货</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
