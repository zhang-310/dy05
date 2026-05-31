import { useMemo, useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent,
  DialogTitle, Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import { orgApi, type OrgMember, type TalentCandidate } from '@/api/org'

const ROLE_LABELS: Record<string, string> = {
  OWNER: '机构负责人',
  MANAGER: '运营管理',
  TALENT: '达人',
  operator: '运营',
  viewer: '只读',
  talent: '达人',
}
const ORG_MEMBER_READY_ENDPOINTS = [
  '/organization/my',
  '/organization/members',
  '/organization/search-talents',
  '/organization/invite',
  '/organization/remove',
] as const
const ORG_MEMBER_UNSUPPORTED_ACTIONS = ['update-role', 'server-export', 'bulk-invite'] as const
const ORG_MEMBER_UNSUPPORTED_ENDPOINTS = [
  '/organization/member/update-role',
  '/organization/member/export',
  '/organization/invite/batch',
] as const
const ORG_MEMBER_READY_ENDPOINTS_ATTR = ORG_MEMBER_READY_ENDPOINTS.join(',')
const ORG_MEMBER_UNSUPPORTED_ACTIONS_ATTR = ORG_MEMBER_UNSUPPORTED_ACTIONS.join(',')
const ORG_MEMBER_UNSUPPORTED_ENDPOINTS_ATTR = ORG_MEMBER_UNSUPPORTED_ENDPOINTS.join(',')

function statusLabel(status: number) {
  if (status === 1) return { label: '已加入', color: 'success' as const }
  if (status === 0) return { label: '待确认', color: 'warning' as const }
  return { label: '已停用', color: 'default' as const }
}

export default function MembersPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [inviteOpen, setInviteOpen] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<OrgMember | null>(null)
  const [removeError, setRemoveError] = useState('')
  const [inviteError, setInviteError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [selectedTalentId, setSelectedTalentId] = useState<number | ''>('')

  const { data: org, isError: orgError, error: orgLoadError, refetch: refetchOrg } = useQuery({
    queryKey: ['org-my'],
    queryFn: orgApi.my,
  })
  const { data: membersData, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['org-members'],
    queryFn: orgApi.members,
  })
  const { data: talentsData, isFetching: talentsFetching, isError: talentsError, error: talentsLoadError, refetch: refetchTalents } = useQuery({
    queryKey: ['org-talent-search', keyword],
    queryFn: () => orgApi.searchTalents(keyword.trim()),
    enabled: inviteOpen && keyword.trim().length > 0,
  })

  const members = membersData ?? []
  const talents = talentsData ?? []
  const rows = useMemo(() => members.slice(page * 20, page * 20 + 20), [members, page])
  const selectedTalent = selectedTalentId === '' ? undefined : talents.find(t => t.id === Number(selectedTalentId))
  const activeCount = members.filter(m => m.status === 1).length
  const pendingCount = members.filter(m => m.status === 0).length
  const managerCount = members.filter(m => ['OWNER', 'MANAGER', 'admin', 'operator'].includes(m.roleInOrg)).length
  const kpiCards = [
    { label: '机构', value: org?.orgName ?? '未创建', hint: org?.orgCode ?? '依赖 /organization/my', endpoint: '/organization/my', metric: 'org-profile' },
    { label: '成员总数', value: members.length, hint: '来自 /organization/members', endpoint: '/organization/members', metric: 'member-count' },
    { label: '已加入', value: activeCount, hint: `待确认 ${pendingCount}`, endpoint: '/organization/members', metric: 'active-count' },
    { label: '管理角色', value: managerCount, hint: 'OWNER / MANAGER / operator', endpoint: '/organization/members', metric: 'manager-count' },
  ]

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['org-members'] })
    qc.invalidateQueries({ queryKey: ['org-my'] })
  }

  const inviteMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      if (selectedTalentId === '') throw new Error('请先搜索并选择达人')
      await orgApi.inviteTalent(Number(selectedTalentId))
    },
    onSuccess: () => {
      toast('邀请已发送', 'success')
      setInviteOpen(false)
      setKeyword('')
      setSelectedTalentId('')
      setInviteError('')
      invalidate()
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      const talentLabel = selectedTalent
        ? `${selectedTalent.nickname || selectedTalent.username} / userId=${selectedTalent.id}`
        : `userId=${selectedTalentId || '-'}`
      setInviteError(`${message}。endpoint=/organization/invite，${talentLabel}，keyword=${keyword.trim() || '-'}`)
      toast(`邀请失败：${message}`, 'error')
    },
  })
  const removeMutation = useMutation({
    mutationFn: (userId: number) => orgApi.removeMember(userId),
    onSuccess: () => {
      toast('已移除成员', 'success')
      setRemoveTarget(null)
      setRemoveError('')
      invalidate()
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      const targetLabel = removeTarget
        ? `${removeTarget.nickname || removeTarget.username || '成员'} / userId=${removeTarget.userId}`
        : 'userId=-'
      setRemoveError(`${message}。endpoint=/organization/remove，${targetLabel}`)
      toast(`移除失败：${message}`, 'error')
    },
  })

  const columns: GridColDef[] = [
    { field: 'username', headerName: '用户名', flex: 1, minWidth: 140 },
    { field: 'nickname', headerName: '昵称', flex: 1, minWidth: 120, renderCell: ({ value }) => value || '-' },
    {
      field: 'roleInOrg',
      headerName: '组织角色',
      width: 130,
      renderCell: ({ value }) => <Chip label={ROLE_LABELS[String(value)] ?? String(value ?? '-')} size="small" color="primary" variant="outlined" />,
    },
    {
      field: 'status',
      headerName: '状态',
      width: 100,
      renderCell: ({ value }) => {
        const s = statusLabel(Number(value))
        return <Chip label={s.label} size="small" color={s.color} />
      },
    },
    { field: 'joinedAt', headerName: '加入时间', width: 170, renderCell: ({ row }) => (row as OrgMember).joinedAt || (row as OrgMember).invitedAt || '-' },
    {
      field: '_actions',
      headerName: '操作',
      width: 120,
      sortable: false,
      renderCell: ({ row }) => (
        <Button
          size="small"
          color="error"
          data-contract-action="remove-member"
          data-contract-endpoint="/organization/remove"
          data-user-id={(row as OrgMember).userId}
          disabled={removeMutation.isPending}
          onClick={() => {
            setRemoveTarget(row as OrgMember)
            setRemoveError('')
          }}
        >
          移除
        </Button>
      ),
    },
  ]

  return (
    <Box
      data-testid="org-members-workbench"
      data-contract-scope="org-members"
      data-ready-endpoints={ORG_MEMBER_READY_ENDPOINTS_ATTR}
      data-unsupported-actions={ORG_MEMBER_UNSUPPORTED_ACTIONS_ATTR}
      data-unsupported-endpoints={ORG_MEMBER_UNSUPPORTED_ENDPOINTS_ATTR}
      data-org-state={org ? 'ready' : 'missing'}
      data-page-index={page}
      data-row-count={rows.length}
      data-total-count={members.length}
      data-active-count={activeCount}
      data-pending-count={pendingCount}
      data-manager-count={managerCount}
      data-no-local-member-fallback="true"
      data-no-local-talent-candidates="true"
      data-row-retained-on-action-error="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 96px)' }}
    >
      <PageHeader
        title="机构成员"
        subtitle="成员管理已对齐 `/organization/members|search-talents|invite|remove`；邀请必须先搜索达人并提交后端真实 userId。"
        breadcrumbs={[{ label: '机构' }, { label: '成员管理' }]}
        actions={
          <>
            <Button startIcon={<RefreshIcon />} onClick={() => { refetchOrg(); refetch() }}>刷新</Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                setInviteError('')
                setInviteOpen(true)
              }}
            >
              邀请达人
            </Button>
          </>
        }
      />

      {(orgError || isError) && (
        <Alert
          severity="error"
          data-testid="org-members-source-error"
          data-source-endpoint={orgError ? '/organization/my' : '/organization/members'}
          data-no-fake-org={orgError ? 'true' : undefined}
          data-no-local-member-fallback={isError ? 'true' : undefined}
          action={<Button color="inherit" size="small" onClick={() => { refetchOrg(); refetch() }}>重试</Button>}
        >
          {orgError ? `机构信息加载失败：${getErrorMessage(orgLoadError)}` : `成员列表加载失败：${getErrorMessage(error)}`}
        </Alert>
      )}
      {!org && !orgError && (
        <Alert
          severity="warning"
          data-testid="org-members-missing-org-downgrade"
          data-contract-status="degraded"
          data-source-endpoint="/organization/my"
          data-no-fake-org="true"
        >
          当前账号未创建机构，后端会返回空成员列表；请先在机构资料链路创建机构后再邀请达人。
        </Alert>
      )}

      <Grid container spacing={1.5}>
        {kpiCards.map(card => (
          <Grid item xs={12} sm={6} md={3} key={card.label}>
            <Card
              variant="outlined"
              data-testid="org-member-kpi-card"
              data-contract-status={card.value === '未创建' ? 'degraded' : 'ready'}
              data-source-endpoint={card.endpoint}
              data-kpi-label={card.label}
              data-kpi-metric={card.metric}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{card.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{card.value}</Typography>
                <Typography variant="caption" color="text.secondary">{card.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Alert
        severity="info"
        data-testid="org-member-role-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="unsupported"
        data-contract-action="update-role"
        data-contract-endpoint="/organization/member/update-role"
        data-source-endpoint="/organization/members"
      >
        后端当前没有组织角色修改接口；页面不再展示“修改角色”伪动作，角色变更需要补 `/organization/member/update-role` 后再启用。
      </Alert>

      <Box
        data-testid="org-member-list-surface"
        data-contract-status="ready"
        data-source-endpoint="/organization/members"
        data-row-count={rows.length}
        data-total-count={members.length}
        data-server-export="unsupported"
        data-no-local-member-fallback="true"
        sx={{ flex: 1, minHeight: 360 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={members.length}
          getRowId={(r) => (r as OrgMember).id}
          paginationMode="server"
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={m => setPage(m.page)}
          pageSizeOptions={[20]}
          showExport={false}
        />
      </Box>

      <Dialog open={inviteOpen} onClose={() => setInviteOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>邀请达人加入机构</DialogTitle>
        <DialogContent
          data-testid="org-member-invite-dialog"
          data-contract-status="ready"
          data-contract-action="search-and-invite"
          data-search-endpoint="/organization/search-talents"
          data-invite-endpoint="/organization/invite"
          data-selected-user-id={selectedTalentId}
          data-keyword={keyword.trim()}
          data-bulk-invite-endpoint="/organization/invite/batch"
          data-bulk-invite-status="unsupported"
        >
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">后端邀请接口只接收 `userId`，请先按用户名、昵称或手机号搜索达人。</Alert>
            <TextField
              label="达人关键词"
              value={keyword}
              onChange={e => { setKeyword(e.target.value); setSelectedTalentId('') }}
              size="small"
              fullWidth
              placeholder="用户名 / 昵称 / 手机号"
            />
            {talentsError && (
              <Alert
                severity="error"
                data-testid="org-member-talent-search-error"
                data-source-endpoint="/organization/search-talents"
                data-no-local-candidates="true"
                action={<Button color="inherit" size="small" onClick={() => refetchTalents()}>重试</Button>}
              >
                达人搜索失败：{getErrorMessage(talentsLoadError)}。请检查 `/organization/search-talents`；当前不会构造本地达人候选。
              </Alert>
            )}
            {inviteError && (
              <Alert
                severity="error"
                data-testid="org-member-invite-error"
                data-source-endpoint="/organization/invite"
                data-selected-user-id={selectedTalentId}
                data-keyword={keyword.trim()}
                data-input-retained="true"
              >
                邀请失败：{inviteError}。已选择的达人和关键词会保留。
              </Alert>
            )}
            <TextField
              select
              label="选择达人"
              value={selectedTalentId}
              onChange={e => setSelectedTalentId(e.target.value === '' ? '' : Number(e.target.value))}
              size="small"
              fullWidth
              disabled={keyword.trim().length === 0 || talentsFetching || talents.length === 0}
              helperText={keyword.trim().length === 0 ? '输入关键词后搜索达人' : talents.length === 0 ? '暂无匹配达人' : '将提交所选达人 userId'}
            >
              {talents.map((t: TalentCandidate) => (
                <MenuItem key={t.id} value={t.id}>
                  {t.nickname || t.username}（{t.username} / ID {t.id}{t.mobile ? ` / ${t.mobile}` : ''}）
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInviteOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => inviteMutation.mutate()} disabled={selectedTalentId === '' || inviteMutation.isPending}>发送邀请</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={removeTarget !== null} onClose={() => !removeMutation.isPending && setRemoveTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>移除机构成员</DialogTitle>
        <DialogContent
          data-testid="org-member-remove-dialog"
          data-contract-status="ready"
          data-contract-action="remove-member"
          data-contract-endpoint="/organization/remove"
          data-user-id={removeTarget?.userId ?? ''}
          data-member-id={removeTarget?.id ?? ''}
        >
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="warning">
              确定移除 {removeTarget?.nickname || removeTarget?.username || `用户 ${removeTarget?.userId}`}？
              该操作会调用 `/organization/remove`，userId={removeTarget?.userId ?? '-'}，后端按 userId 移除机构关系。
            </Alert>
            {removeError && (
              <Alert
                severity="error"
                data-testid="org-member-remove-error"
                data-source-endpoint="/organization/remove"
                data-user-id={removeTarget?.userId ?? ''}
                data-row-retained-on-action-error="true"
              >
                移除失败：{removeError}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRemoveTarget(null)} disabled={removeMutation.isPending}>取消</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => removeTarget && removeMutation.mutate(removeTarget.userId)}
            disabled={removeTarget === null || removeMutation.isPending}
          >
            {removeMutation.isPending ? '移除中...' : '确认移除'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
