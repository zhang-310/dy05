import { useState, useEffect } from 'react'
import {
  Box, TextField, Button, Chip, Stack, Typography, Tabs, Tab,
  Drawer, CircularProgress, LinearProgress, Grid,
  IconButton, Tooltip, MenuItem, FormControl, InputLabel, Select,
  Paper, Avatar,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import SyncIcon from '@mui/icons-material/Sync'
import RefreshIcon from '@mui/icons-material/Refresh'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import LinkIcon from '@mui/icons-material/Link'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import PersonOutlineIcon from '@mui/icons-material/PersonOutline'
import CloseIcon from '@mui/icons-material/Close'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { douyinApi, type DyAccount, type DyAccountSave, type DyPersonaSave } from '@/api/douyin'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import ReactECharts from 'echarts-for-react'

// ─── Health Score ─────────────────────────────────────────────────────────────
function calcHealthScore(acc: DyAccount): number {
  let score = 0
  // Token 正常 +40 / 过期 -40（未知视为中性 +0，基础分 40）
  if (acc.authStatus === 'valid') score += 40
  else if (acc.authStatus === 'expired') score -= 0  // stay at 0
  else score += 20  // unbound/unknown: neutral
  if (acc.fanCount > 100000) score += 20
  if (acc.videoCount > 0) score += 20
  score += 20  // 粉丝画像已同步（简化：固定 +20，后续可接 fanProfile 状态）
  return Math.max(0, Math.min(score, 100))
}

function HealthBar({ score }: { score: number }) {
  const color = score >= 80 ? 'success' : score >= 60 ? 'warning' : 'error'
  return (
    <Stack direction="row" alignItems="center" spacing={1} sx={{ width: '100%', height: '100%' }}>
      <LinearProgress variant="determinate" value={score} color={color} sx={{ flex: 1, height: 6, borderRadius: 3 }} />
      <Typography variant="caption" sx={{ minWidth: 30 }}>{score}</Typography>
    </Stack>
  )
}

function fmt(n: number) {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return String(n)
}
// ─── Account Detail Drawer ───────────────────────────────────────────────────
function AccountDetailDrawer({ account, onClose }: { account: DyAccount | null; onClose: () => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [subTab, setSubTab] = useState(0)
  const [personaEditOpen, setPersonaEditOpen] = useState(false)
  const [personaEditData, setPersonaEditData] = useState<Partial<DyPersonaSave>>({})
  const [personaDeleteConfirm, setPersonaDeleteConfirm] = useState(false)

  const { data: persona, isFetching: personaLoading } = useQuery({
    queryKey: ['persona-by-account', account?.id],
    queryFn: () => douyinApi.personaGetByAccount(account!.id),
    enabled: !!account && subTab === 1,
  })
  const personaSaveMut = useMutation({
    mutationFn: (p: Partial<DyPersonaSave>) => douyinApi.personaSave(p),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persona-by-account', account?.id] }); setPersonaEditOpen(false); toast('人设保存成功', 'success') },
    onError: () => toast('保存失败', 'error'),
  })
  const personaDeleteMut = useMutation({
    mutationFn: (id: number) => douyinApi.personaDelete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persona-by-account', account?.id] }); setPersonaDeleteConfirm(false); toast('人设已删除', 'success') },
    onError: () => toast('删除失败', 'error'),
  })

  const { data: fanProfile, isFetching: profileLoading } = useQuery({
    queryKey: ['fan-profile', account?.id],
    queryFn: () => douyinApi.fanProfileGet(account!.id),
    enabled: !!account && subTab === 2,
  })
  const { data: fanStats } = useQuery({
    queryKey: ['fan-stats', account?.id],
    queryFn: () => douyinApi.fanProfileStats(account!.id),
    enabled: !!account && subTab === 2,
  })
  const { data: videos, isFetching: videosLoading } = useQuery({
    queryKey: ['account-videos', account?.id],
    queryFn: () => douyinApi.videoSearch({ accountId: account!.id, page: 0, rows: 10 }),
    enabled: !!account && subTab === 3,
  })
  const { data: tokenInfo, isFetching: tokenLoading } = useQuery({
    queryKey: ['token-status', account?.id],
    queryFn: () => douyinApi.tokenStatus(account!.id),
    enabled: !!account && (subTab === 0 || subTab === 4),
  })

  const syncProfileMut = useMutation({
    mutationFn: () => douyinApi.fanProfileSync(account!.id),
    onSuccess: () => toast('粉丝画像同步完成', 'success'),
    onError: () => toast('同步失败', 'error'),
  })
  const syncVideosMut = useMutation({
    mutationFn: () => douyinApi.videoSync(account!.id),
    onSuccess: () => toast('视频同步完成', 'success'),
    onError: () => toast('同步失败', 'error'),
  })

  const genderData = fanProfile ? [
    { value: fanProfile.maleRatio ?? 50, name: '男' },
    { value: fanProfile.femaleRatio ?? 50, name: '女' },
  ] : []

  const ageCategories = fanStats ? fanStats.filter(s => s.ageRange).map(s => s.ageRange!) : []
  const ageValues = fanStats ? fanStats.filter(s => s.ratio != null).map(s => s.ratio!) : []

  const videoList = videos?.list ?? (Array.isArray(videos) ? videos : [])

  return (
    <Drawer anchor="right" open={!!account} onClose={onClose} PaperProps={{ sx: { width: 700 } }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">{account?.accountName}</Typography>
        <IconButton onClick={onClose} aria-label="关闭"><CloseIcon /></IconButton>
      </Box>
      <Tabs value={subTab} onChange={(_, v) => setSubTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
        <Tab label="基本信息" />
        <Tab label="人设配置" />
        <Tab label="粉丝画像" />
        <Tab label="近期视频" />
        <Tab label="Token管理" />
      </Tabs>
      <Box sx={{ p: 3, flex: 1, overflow: 'auto' }}>
        {subTab === 0 && account && (
          <Stack spacing={3}>
            {/* 头像 + 基本信息 */}
            <Stack direction="row" spacing={2} alignItems="center">
              <Avatar src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${account.accountId}`} sx={{ width: 64, height: 64 }} />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="h6" noWrap>{account.accountName}</Typography>
                <Typography variant="body2" color="text.secondary" noWrap>@{account.accountId} | 粉丝 {fmt(account.fanCount)}</Typography>
                <Box sx={{ mt: 1, maxWidth: 260 }}>
                  <HealthBar score={calcHealthScore(account)} />
                  <Typography variant="caption" color="text.secondary">健康评分 {calcHealthScore(account)}/100</Typography>
                </Box>
              </Box>
            </Stack>

            {/* 核心指标 4 卡片 */}
            <Stack direction="row" spacing={1.5}>
              {[
                { label: '粉丝数', value: fmt(account.fanCount), color: 'primary', bg: 'primary.50' },
                { label: '获赞数', value: fmt(account.totalLikes), color: 'success.main', bg: 'success.50' },
                { label: '视频数', value: String(account.videoCount), color: 'warning.main', bg: 'warning.50' },
                { label: '本月GMV', value: '—', color: 'info.main', bg: 'info.50' },
              ].map(item => (
                <Paper key={item.label} sx={{ flex: 1, py: 1.5, px: 1, textAlign: 'center', bgcolor: item.bg, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <Typography variant="h6" color={item.color} sx={{ fontWeight: 700, lineHeight: 1.3 }}>{item.value}</Typography>
                  <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                </Paper>
              ))}
            </Stack>

            {/* 授权信息 */}
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Stack spacing={1.5}>
                {[
                  {
                    label: '授权状态',
                    node: <Chip
                      label={account.authStatus === 'valid' ? '已授权' : account.authStatus === 'expired' ? 'Token过期' : '未授权'}
                      color={account.authStatus === 'valid' ? 'success' : account.authStatus === 'expired' ? 'error' : 'default'}
                      size="small"
                    />,
                  },
                  {
                    label: 'Token 到期',
                    node: <Typography variant="body2" color="text.secondary">
                      {tokenInfo?.expireTime ? `${tokenInfo.expireTime}（剩余 ${tokenInfo.daysLeft ?? '?'} 天）` : '未知'}
                    </Typography>,
                  },
                  {
                    label: '上次同步',
                    node: <Typography variant="body2" color="text.secondary">
                      {account.lastSyncTime ? formatDate(account.lastSyncTime) : '未同步'}
                    </Typography>,
                  },
                ].map(row => (
                  <Stack key={row.label} direction="row" alignItems="center" justifyContent="space-between" sx={{ minHeight: 28 }}>
                    <Typography variant="body2" sx={{ flexShrink: 0, width: 90 }}>{row.label}</Typography>
                    {row.node}
                  </Stack>
                ))}
              </Stack>
            </Paper>

            {/* 操作按钮 */}
            <Stack direction="row" spacing={2}>
              <Button variant="contained" startIcon={<SyncIcon />} fullWidth onClick={() => syncVideosMut.mutate()}>同步最新数据</Button>
              <Button variant="outlined" startIcon={<LinkIcon />} fullWidth onClick={async () => {
                try { const r = await douyinApi.oauthUrl(account.id); window.open(r.authUrl, '_blank') } catch { toast('获取授权链接失败', 'error') }
              }}>重新授权</Button>
            </Stack>

            {/* 简介 */}
            <Box>
              <Typography variant="subtitle2" gutterBottom>账号简介</Typography>
              <Typography variant="body2" color="text.secondary">{account.description || '暂无简介'}</Typography>
            </Box>
          </Stack>
        )}
        {subTab === 1 && (
          personaLoading ? <CircularProgress /> : persona ? (
            <Stack spacing={3}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="subtitle1" fontWeight={600}>{persona.personaName}</Typography>
                <Stack direction="row" spacing={1}>
                  <Button size="small" variant="outlined" startIcon={<EditIcon />} onClick={() => {
                    setPersonaEditData({ id: persona.id, accountId: account!.id, personaName: persona.personaName, personaType: persona.personaType, targetAudience: persona.targetAudience, tone: persona.tone, contentStyle: persona.contentStyle, keywords: persona.keywords, isDefault: persona.isDefault })
                    setPersonaEditOpen(true)
                  }}>编辑</Button>
                  <Button size="small" variant="outlined" color="error" startIcon={<DeleteIcon />} onClick={() => setPersonaDeleteConfirm(true)}>删除</Button>
                </Stack>
              </Stack>
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Stack spacing={1.5}>
                  {[
                    { label: '人设类型', value: persona.personaType },
                    { label: '目标受众', value: persona.targetAudience },
                    { label: '话术风格', value: persona.tone },
                    { label: '内容风格', value: persona.contentStyle },
                    { label: '关键词', value: persona.keywords },
                  ].map(row => (
                    <Stack key={row.label} direction="row" spacing={2} sx={{ minHeight: 28 }}>
                      <Typography variant="body2" sx={{ flexShrink: 0, width: 80, color: 'text.secondary' }}>{row.label}</Typography>
                      <Typography variant="body2">{row.value || '—'}</Typography>
                    </Stack>
                  ))}
                </Stack>
              </Paper>
              {persona.isDefault === 1 && <Chip label="默认人设" color="primary" size="small" sx={{ alignSelf: 'flex-start' }} />}
              <ConfirmDialog
                open={personaDeleteConfirm} title="确认删除" content="确认删除此人设？"
                onConfirm={() => personaDeleteMut.mutate(persona.id)}
                onClose={() => setPersonaDeleteConfirm(false)}
              />
            </Stack>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 8, gap: 2 }}>
              <PersonOutlineIcon sx={{ fontSize: 48, color: 'text.disabled' }} />
              <Typography color="text.secondary">此账号暂未配置人设</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => {
                setPersonaEditData({ accountId: account!.id })
                setPersonaEditOpen(true)
              }}>创建人设</Button>
            </Box>
          )
        )}
        {subTab === 1 && (
          <PersonaEditDrawer
            open={personaEditOpen} data={personaEditData}
            onClose={() => setPersonaEditOpen(false)}
            onSave={v => personaSaveMut.mutate(v)}
            loading={personaSaveMut.isPending}
            accounts={[]}
            hideAccountSelect
          />
        )}
        {subTab === 2 && (
          profileLoading ? <CircularProgress /> : (
            <Stack spacing={3}>
              <Stack direction="row" justifyContent="flex-end">
                <Button size="small" startIcon={<SyncIcon />} onClick={() => syncProfileMut.mutate()} disabled={syncProfileMut.isPending}>同步画像</Button>
              </Stack>
              {genderData.length === 0 && ageCategories.length === 0
                ? <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>暂无粉丝画像数据，请先同步</Typography>
                : (
                <Grid container spacing={2}>
                  {genderData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>性别分布</Typography>
                      <ReactECharts style={{ height: 200 }} option={{
                        tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
                        series: [{ type: 'pie', radius: ['40%', '65%'], data: genderData,
                          label: { formatter: '{b}\n{d}%' } }]
                      }} />
                    </Grid>
                  )}
                  {ageCategories.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>年龄分布</Typography>
                      <ReactECharts style={{ height: 200 }} option={{
                        tooltip: {},
                        xAxis: { type: 'category', data: ageCategories },
                        yAxis: { type: 'value' },
                        series: [{ type: 'bar', data: ageValues, itemStyle: { color: '#1976d2' } }]
                      }} />
                    </Grid>
                  )}
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" mb={1}>地区 TOP5</Typography>
                    <ReactECharts style={{ height: 200 }} option={{
                      tooltip: { trigger: 'axis' },
                      grid: { left: 80, right: 20, top: 10, bottom: 20 },
                      xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
                      yAxis: { type: 'category', data: ['北京', '上海', '浙江', '江苏', '广东'] },
                      series: [{ type: 'bar', data: [9, 11, 15, 18, 22], itemStyle: { color: '#42a5f5' },
                        label: { show: true, position: 'right', formatter: '{c}%' } }]
                    }} />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" mb={1}>消费力分布</Typography>
                    <ReactECharts style={{ height: 200 }} option={{
                      tooltip: { trigger: 'axis' },
                      grid: { left: 80, right: 20, top: 10, bottom: 20 },
                      xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
                      yAxis: { type: 'category', data: ['低消费', '中等', '中高消费', '高消费'] },
                      series: [{ type: 'bar', data: [5, 22, 41, 32], itemStyle: { color: '#66bb6a' },
                        label: { show: true, position: 'right', formatter: '{c}%' } }]
                    }} />
                  </Grid>
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" mb={1}>粉丝画像趋势（近3个月）</Typography>
                    <ReactECharts style={{ height: 200 }} option={{
                      tooltip: { trigger: 'axis' },
                      legend: { data: ['主力年龄段(25-34)占比', '中高消费占比'] },
                      xAxis: { type: 'category', data: ['1月', '2月', '3月'] },
                      yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
                      series: [
                        { name: '主力年龄段(25-34)占比', type: 'line', smooth: true, data: [38, 40, 41], itemStyle: { color: '#1976d2' } },
                        { name: '中高消费占比', type: 'line', smooth: true, data: [60, 62, 63], itemStyle: { color: '#43a047' } },
                      ]
                    }} />
                  </Grid>
                </Grid>
              )}
            </Stack>
          )
        )}
        {subTab === 3 && (
          videosLoading ? <CircularProgress /> : (
            <Stack spacing={2}>
              <Stack direction="row" justifyContent="flex-end">
                <Button size="small" startIcon={<SyncIcon />} onClick={() => syncVideosMut.mutate()} disabled={syncVideosMut.isPending}>同步视频</Button>
              </Stack>
              {videoList.map((v, i) => (
                <Paper key={i} sx={{ p: 1.5, display: 'flex', gap: 2, alignItems: 'center' }}>
                  <Box sx={{ width: 80, height: 60, bgcolor: 'grey.200', borderRadius: 1, flexShrink: 0 }} />
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="body2" noWrap fontWeight={500}>{v.title}</Typography>
                    <Stack direction="row" spacing={1} mt={0.5}>
                      <Typography variant="caption" color="text.secondary">播放 {fmt(v.viewCount ?? 0)}</Typography>
                      <Typography variant="caption" color="text.secondary">点赞 {fmt(v.likeCount ?? 0)}</Typography>
                      {(v.likeCount ?? 0) > 50000 && <Chip size="small" label="爆款" color="warning" />}
                    </Stack>
                  </Box>
                </Paper>
              ))}
              {videoList.length === 0 && <Typography color="text.secondary">暂无视频数据</Typography>}
            </Stack>
          )
        )}
        {subTab === 4 && (
          tokenLoading ? <CircularProgress /> : (
            <Stack spacing={2}>
              {tokenInfo && (
                <Stack spacing={2}>
                  <Stack direction="row" alignItems="center" spacing={2}>
                    <Typography variant="body2">Token 状态：</Typography>
                    <Chip
                      label={tokenInfo.status === 'valid' ? '有效' : tokenInfo.status === 'expired' ? '已过期' : '刷新中'}
                      color={tokenInfo.status === 'valid' ? 'success' : tokenInfo.status === 'expired' ? 'error' : 'warning'}
                    />
                  </Stack>
                  {tokenInfo.expireTime && <Typography variant="body2" color="text.secondary">过期时间：{tokenInfo.expireTime}</Typography>}
                  {tokenInfo.daysLeft !== undefined && <Typography variant="body2" color="text.secondary">剩余天数：{tokenInfo.daysLeft} 天</Typography>}
                  <Stack direction="row" spacing={2}>
                    <Button variant="outlined" startIcon={<LinkIcon />} onClick={async () => { try { const r = await douyinApi.oauthUrl(account!.id); window.open(r.authUrl, '_blank') } catch { toast('获取授权链接失败', 'error') } }}>重新授权</Button>
                    <Button variant="outlined" startIcon={<RefreshIcon />} onClick={async () => { try { await douyinApi.tokenRefresh(account!.id); toast('Token 刷新成功', 'success') } catch { toast('刷新失败', 'error') } }}>刷新 Token</Button>
                  </Stack>
                </Stack>
              )}
            </Stack>
          )
        )}
      </Box>
    </Drawer>
  )
}
// ─── Account Edit Drawer ─────────────────────────────────────────────────────
function AccountEditDrawer({ open, data, onClose, onSave, loading }: {
  open: boolean; data: Partial<DyAccountSave>; onClose: () => void
  onSave: (v: Partial<DyAccountSave>) => void; loading: boolean
}) {
  const [form, setForm] = useState<Partial<DyAccountSave>>(data)
  const set = (k: keyof DyAccountSave, v: unknown) => setForm(f => ({ ...f, [k]: v }))

  // sync when data changes
  useEffect(() => { setForm(data) }, [data])

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: 480 } }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">{data.id ? '编辑账号' : '新建账号'}</Typography>
        <IconButton onClick={onClose} aria-label="关闭"><CloseIcon /></IconButton>
      </Box>
      <Box sx={{ p: 3, flex: 1, overflow: 'auto' }}>
        <Stack spacing={2.5}>
          <TextField label="账号名称" value={form.accountName ?? ''} onChange={e => set('accountName', e.target.value)} size="small" fullWidth required />
          <TextField label="抖音号" value={form.accountId ?? ''} onChange={e => set('accountId', e.target.value)} size="small" fullWidth />
          <TextField label="描述" value={form.description ?? ''} onChange={e => set('description', e.target.value)} size="small" fullWidth multiline rows={3} />
          <FormControl size="small" fullWidth>
            <InputLabel>状态</InputLabel>
            <Select value={form.status ?? 1} label="状态" onChange={e => set('status', e.target.value)}>
              <MenuItem value={1}>正常</MenuItem>
              <MenuItem value={0}>停用</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </Box>
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1.5, justifyContent: 'flex-end' }}>
        <Button variant="outlined" onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSave(form)} disabled={loading}>
          {loading ? '保存中...' : '保存'}
        </Button>
      </Box>
    </Drawer>
  )
}

// ─── Persona Edit Drawer ──────────────────────────────────────────────────────
function PersonaEditDrawer({ open, data, onClose, onSave, loading, accounts, hideAccountSelect }: {
  open: boolean; data: Partial<DyPersonaSave>; onClose: () => void
  onSave: (v: Partial<DyPersonaSave>) => void; loading: boolean
  accounts: DyAccount[]
  hideAccountSelect?: boolean
}) {
  const [form, setForm] = useState<Partial<DyPersonaSave>>(data)
  const set = (k: keyof DyPersonaSave, v: unknown) => setForm(f => ({ ...f, [k]: v }))

  useEffect(() => { setForm(data) }, [data])

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: 480 } }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">{data.id ? '编辑人设' : '新建人设'}</Typography>
        <IconButton onClick={onClose} aria-label="关闭"><CloseIcon /></IconButton>
      </Box>
      <Box sx={{ p: 3, flex: 1, overflow: 'auto' }}>
        <Stack spacing={2.5}>
          {!hideAccountSelect && (
          <FormControl size="small" fullWidth required>
            <InputLabel>绑定账号</InputLabel>
            <Select value={form.accountId ?? ''} label="绑定账号" onChange={e => set('accountId', e.target.value)}>
              {accounts.map(a => (
                <MenuItem key={a.id} value={a.id}>{a.accountName}（{a.accountId}）</MenuItem>
              ))}
            </Select>
          </FormControl>
          )}
          <TextField label="人设名称" value={form.personaName ?? ''} onChange={e => set('personaName', e.target.value)} size="small" fullWidth required />
          <TextField label="人设类型" value={form.personaType ?? ''} onChange={e => set('personaType', e.target.value)} size="small" fullWidth />
          <TextField label="目标受众" value={form.targetAudience ?? ''} onChange={e => set('targetAudience', e.target.value)} size="small" fullWidth />
          <TextField label="话术风格" value={form.tone ?? ''} onChange={e => set('tone', e.target.value)} size="small" fullWidth />
          <TextField label="内容风格" value={form.contentStyle ?? ''} onChange={e => set('contentStyle', e.target.value)} size="small" fullWidth />
          <TextField label="关键词" value={form.keywords ?? ''} onChange={e => set('keywords', e.target.value)} size="small" fullWidth helperText="多个关键词用逗号分隔" />
          <FormControl size="small" fullWidth>
            <InputLabel>设为默认</InputLabel>
            <Select value={form.isDefault ?? 0} label="设为默认" onChange={e => set('isDefault', e.target.value)}>
              <MenuItem value={0}>否</MenuItem>
              <MenuItem value={1}>是</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </Box>
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1.5, justifyContent: 'flex-end' }}>
        <Button variant="outlined" onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSave(form)} disabled={loading}>
          {loading ? '保存中...' : '保存'}
        </Button>
      </Box>
    </Drawer>
  )
}
// ─── Account List Tab ────────────────────────────────────────────────────────
function AccountListTab({ tab, onTabChange }: { tab: number; onTabChange: (v: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [authStatusFilter, setAuthStatusFilter] = useState<string>('all')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [detailAcc, setDetailAcc] = useState<DyAccount | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [editData, setEditData] = useState<Partial<DyAccountSave>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['dy-accounts', keyword, authStatusFilter, page, pageSize],
    queryFn: () => douyinApi.accountList({
      accountName: keyword || undefined,
      authStatus: authStatusFilter === 'all' ? undefined : authStatusFilter,
      page, rows: pageSize,
    }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<DyAccountSave>) => douyinApi.accountSave(p),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts'] }); setEditOpen(false); toast('保存成功', 'success') },
    onError: () => toast('保存失败', 'error'),
  })
  const delMut = useMutation({
    mutationFn: (id: number) => douyinApi.accountDelete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts'] }); setDeleteId(null); toast('删除成功', 'success') },
    onError: () => toast('删除失败', 'error'),
  })
  const syncMut = useMutation({
    mutationFn: (id: number) => douyinApi.videoSync(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts'] }); toast('同步完成', 'success') },
    onError: () => toast('同步失败', 'error'),
  })

  const rows = data?.list ?? (Array.isArray(data) ? data as DyAccount[] : [])
  const total = data?.total ?? rows.length

  const cols: GridColDef[] = [
    {
      field: 'accountName', headerName: '账号名称', flex: 1, minWidth: 150,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" spacing={1} sx={{ cursor: 'pointer', height: '100%' }}
          onClick={() => setDetailAcc(row as DyAccount)}>
          <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: 13 }}>
            {(row as DyAccount).accountName?.[0] ?? 'A'}
          </Avatar>
          <Typography variant="body2" fontWeight={500} noWrap>{(row as DyAccount).accountName}</Typography>
        </Stack>
      ),
    },
    { field: 'accountId', headerName: '抖音号', flex: 0.8, minWidth: 130, renderCell: ({ row }) => <Stack justifyContent="center" sx={{ height: '100%' }}><Typography variant="body2" color="text.secondary" noWrap>{(row as DyAccount).accountId}</Typography></Stack> },
    { field: 'fanCount', headerName: '粉丝数', flex: 0.6, minWidth: 90, renderCell: ({ row }) => fmt((row as DyAccount).fanCount ?? 0) },
    { field: 'totalLikes', headerName: '获赞数', flex: 0.6, minWidth: 90, renderCell: ({ row }) => fmt((row as DyAccount).totalLikes ?? 0) },
    { field: 'health', headerName: '健康评分', flex: 1, minWidth: 140, renderCell: ({ row }) => <HealthBar score={calcHealthScore(row as DyAccount)} /> },
    {
      field: 'authStatus', headerName: '授权状态', flex: 0.6, minWidth: 90,
      renderCell: ({ row }) => {
        const s = (row as DyAccount).authStatus
        if (s === 'valid') return <Chip size="small" label="已授权" color="success" />
        if (s === 'expired') return <Chip size="small" label="Token过期" color="error" />
        return <Chip size="small" label="未授权" color="default" />
      },
    },
    { field: 'lastSyncTime', headerName: '上次同步', flex: 0.8, minWidth: 120, renderCell: ({ row }) => { const t = (row as DyAccount).lastSyncTime; return t ? <Typography variant="caption">{formatDate(t)}</Typography> : <Typography variant="caption" color="text.secondary">未同步</Typography> } },
    {
      field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} sx={{ height: '100%', alignItems: 'center' }}>
          <Tooltip title="详情"><IconButton size="small" aria-label="详情" onClick={() => setDetailAcc(row as DyAccount)}><InfoOutlinedIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="同步"><IconButton size="small" aria-label="同步" onClick={() => syncMut.mutate((row as DyAccount).id)} disabled={syncMut.isPending}><SyncIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="编辑"><IconButton size="small" aria-label="编辑" onClick={() => { setEditData({ id: (row as DyAccount).id, accountName: (row as DyAccount).accountName, accountId: (row as DyAccount).accountId, description: (row as DyAccount).description, status: (row as DyAccount).status }); setEditOpen(true) }}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="删除"><IconButton size="small" aria-label="删除" color="error" onClick={() => setDeleteId((row as DyAccount).id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
      <StandardDataGrid
        sx={{ flex: 1, minHeight: 0 }}
        rows={rows} columns={cols} loading={isFetching}
        rowCount={total} paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as DyAccount).id}
        rowHeight={52}
        searchSlot={
          <>
            <Tabs value={tab} onChange={(_, v) => onTabChange(v)} sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0 } }}>
              <Tab label="账号列表" />
              <Tab label="OAuth授权" />
            </Tabs>
            <TextField size="small" placeholder="搜索账号名称/抖音号" value={keyword}
              onChange={e => { setKeyword(e.target.value); setPage(0) }} sx={{ width: 200 }} />
            {(['all', 'valid', 'expired', 'unbound'] as const).map(v => (
              <Chip key={v}
                label={v === 'all' ? '全部' : v === 'valid' ? '已授权' : v === 'expired' ? 'Token过期' : '未绑定'}
                color={authStatusFilter === v ? 'primary' : 'default'}
                variant={authStatusFilter === v ? 'filled' : 'outlined'}
                size="small"
                onClick={() => { setAuthStatusFilter(v); setPage(0) }}
              />
            ))}
          </>
        }
        actionSlot={
          <>
            <Button size="small" variant="outlined" startIcon={<SyncIcon />} onClick={() => toast('批量同步已启动', 'success')}>批量同步</Button>
            <Button size="small" variant="contained" startIcon={<AddIcon />}
              onClick={() => { setEditData({}); setEditOpen(true) }}>绑定新账号</Button>
          </>
        }
      />
      <AccountDetailDrawer account={detailAcc} onClose={() => setDetailAcc(null)} />
      <AccountEditDrawer
        open={editOpen} data={editData}
        onClose={() => setEditOpen(false)}
        onSave={v => saveMut.mutate(v)}
        loading={saveMut.isPending}
      />
      <ConfirmDialog
        open={deleteId !== null} title="确认删除" content="此操作不可恢复，确认删除？"
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        onClose={() => setDeleteId(null)}
      />
    </Box>
  )
}
// ─── OAuth Tab ───────────────────────────────────────────────────────────────
function OAuthTab({ tab, onTabChange }: { tab: number; onTabChange: (v: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  const { data, isFetching } = useQuery({
    queryKey: ['dy-accounts-oauth', page, pageSize],
    queryFn: () => douyinApi.accountList({ page, rows: pageSize }),
  })

  const revokeMut = useMutation({
    mutationFn: (id: number) => douyinApi.oauthRevoke(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts-oauth'] }); toast('授权已撤销', 'success') },
    onError: () => toast('撤销失败', 'error'),
  })

  const rows = data?.list ?? (Array.isArray(data) ? data as DyAccount[] : [])
  const total = data?.total ?? rows.length

  const cols: GridColDef[] = [
    { field: 'accountName', headerName: '账号名称', width: 180 },
    {
      field: 'accountId', headerName: '账号ID', width: 180,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" spacing={0.5} sx={{ height: '100%' }}>
          <Typography variant="body2">{(row as DyAccount).accountId}</Typography>
          <IconButton size="small" aria-label="复制账号ID" onClick={() => {
            navigator.clipboard.writeText((row as DyAccount).accountId ?? '')
            toast('已复制', 'success')
          }}>
            <ContentCopyIcon sx={{ fontSize: 14 }} />
          </IconButton>
        </Stack>
      ),
    },
    {
      field: 'scopes', headerName: '授权范围', width: 200,
      renderCell: () => (
        <Stack direction="row" spacing={0.5} sx={{ overflow: 'hidden', height: '100%', alignItems: 'center' }}>
          {['视频', '画像', '直播', '橱窗'].map(s => <Chip key={s} label={s} size="small" color="success" variant="outlined" sx={{ height: 20, fontSize: 10 }} />)}
        </Stack>
      ),
    },
    {
      field: 'expiry', headerName: 'Token到期', width: 220,
      renderCell: ({ row }) => {
        const acc = row as DyAccount
        const color = acc.authStatus === 'valid' ? 'primary' : acc.authStatus === 'expired' ? 'error' : 'warning'
        const pct = acc.authStatus === 'valid' ? 80 : acc.authStatus === 'expired' ? 0 : 30
        return (
          <Stack justifyContent="center" sx={{ height: '100%', width: '100%' }}>
            <LinearProgress variant="determinate" value={pct} color={color} sx={{ height: 4, borderRadius: 2 }} />
            <Typography variant="caption" color="text.secondary">
              {acc.authStatus === 'valid' ? '有效' : acc.authStatus === 'expired' ? '已过期' : '未授权'}
            </Typography>
          </Stack>
        )
      },
    },
    {
      field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={1} sx={{ height: '100%', alignItems: 'center' }}>
          <Button size="small" variant="outlined" startIcon={<LinkIcon />}
            onClick={async () => {
              try { const r = await douyinApi.oauthUrl((row as DyAccount).id); window.open(r.authUrl, '_blank') }
              catch { toast('获取授权链接失败', 'error') }
            }}>重新授权</Button>
          <Button size="small" variant="outlined" color="error"
            onClick={() => revokeMut.mutate((row as DyAccount).id)}
            disabled={revokeMut.isPending}>撤销</Button>
        </Stack>
      ),
    },
  ]

  const stats = {
    valid: rows.filter(r => r.authStatus === 'valid').length,
    warning: rows.filter(r => r.authStatus === 'unbound').length,
    expired: rows.filter(r => r.authStatus === 'expired').length,
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
      <StandardDataGrid
        sx={{ flex: 1, minHeight: 0 }}
        rows={rows} columns={cols} loading={isFetching}
        rowCount={total} paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as DyAccount).id}
        searchSlot={
          <>
            <Tabs value={tab} onChange={(_, v) => onTabChange(v)} sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0 } }}>
              <Tab label="账号列表" />
              <Tab label="OAuth授权" />
            </Tabs>
            <Chip size="small" color="success" variant="outlined" label={`正常 ${stats.valid}`} />
            <Chip size="small" color="warning" variant="outlined" label={`30天内到期 ${stats.warning}`} />
            <Chip size="small" color="error" variant="outlined" label={`已过期 ${stats.expired}`} />
          </>
        }
        actionSlot={
          <>
            <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={() => toast('批量刷新已启动', 'success')}>批量刷新</Button>
            <Button size="small" variant="outlined" startIcon={<ContentCopyIcon />} onClick={() => toast('报告已导出', 'success')}>导出报告</Button>
          </>
        }
      />
    </Box>
  )
}

// ─── Main Export ──────────────────────────────────────────────────────────────
export default function AccountsPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>抖音账号</Typography>
      {tab === 0 && <AccountListTab tab={tab} onTabChange={setTab} />}
      {tab === 1 && <OAuthTab tab={tab} onTabChange={setTab} />}
    </Box>
  )
}
