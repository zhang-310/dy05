import { useState } from 'react'
import { Box, Button, Stack, Chip, TextField, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { StandardDataGrid } from '@/components/base/StandardDataGrid'
import { useToast } from '@/contexts/ToastContext'
import request from '@/utils/request'

interface OrgMember {
  id: number
  userId: number
  username: string
  email: string
  role: string
  status: number
  joinTime: string
}

const memberApi = {
  list: (params: Record<string, unknown>) => request.post<{ list: OrgMember[]; total: number }>('/org/member/list', params),
  invite: (params: { email: string; role: string }) => request.post<void>('/org/member/invite', params),
  remove: (userId: number) => request.post<void>('/org/member/remove', { userId }),
  updateRole: (userId: number, role: string) => request.post<void>('/org/member/update-role', { userId, role }),
}

const ROLES = ['admin', 'operator', 'viewer', 'talent']

export default function MembersPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [inviteOpen, setInviteOpen] = useState(false)
  const [inviteForm, setInviteForm] = useState({ email: '', role: 'operator' })

  const { data, isLoading } = useQuery({
    queryKey: ['org-members', page],
    queryFn: () => memberApi.list({ page, rows: 20 }),
  })
  const rows: OrgMember[] = data?.list ?? []
  const total = data?.total ?? 0

  const inviteMutation = useMutation({
    mutationFn: async (): Promise<void> => { await memberApi.invite(inviteForm) },
    onSuccess: () => { toast('邀请已发送', 'success'); qc.invalidateQueries({ queryKey: ['org-members'] }); setInviteOpen(false) },
    onError: () => toast('邀请失败', 'error'),
  })
  const removeMutation = useMutation({
    mutationFn: async (userId: number): Promise<void> => { await memberApi.remove(userId) },
    onSuccess: () => { toast('已移除成员', 'success'); qc.invalidateQueries({ queryKey: ['org-members'] }) },
    onError: () => toast('操作失败', 'error'),
  })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'username', headerName: '用户名', flex: 1 },
    { field: 'email', headerName: '邮箱', flex: 1.5 },
    { field: 'role', headerName: '角色', width: 120,
      renderCell: ({ value }) => <Chip label={String(value ?? '')} size="small" color="primary" variant="outlined" /> },
    { field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => <Chip label={value ? '正常' : '停用'} size="small" color={value ? 'success' : 'default'} /> },
    { field: 'joinTime', headerName: '加入时间', width: 160 },
    { field: '_actions', headerName: '操作', width: 120, sortable: false,
      renderCell: ({ row: r }) => (
        <Button size="small" color="error" onClick={() => removeMutation.mutate((r as OrgMember).userId)}>移除</Button>
      ) },
  ]

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 120px)' }}>
      <Stack direction="row" justifyContent="flex-end">
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setInviteOpen(true)}>邀请成员</Button>
      </Stack>

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={m => setPage(m.page)}
        pageSizeOptions={[20]}
      />

      <Dialog open={inviteOpen} onClose={() => setInviteOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>邀请成员</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="邮箱" value={inviteForm.email}
              onChange={e => setInviteForm(f => ({ ...f, email: e.target.value }))}
              size="small" fullWidth type="email" />
            <TextField select label="角色" value={inviteForm.role}
              onChange={e => setInviteForm(f => ({ ...f, role: e.target.value }))}
              size="small" fullWidth>
              {ROLES.map(r => <MenuItem key={r} value={r}>{r}</MenuItem>)}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInviteOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => inviteMutation.mutate()} disabled={!inviteForm.email || inviteMutation.isPending}>发送邀请</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
