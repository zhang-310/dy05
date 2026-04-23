import { useState } from 'react'
import {
  Box, Typography, Stack, Tabs, Tab, TextField, Button, CircularProgress, Alert,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip, FormControl, InputLabel, Select, MenuItem,
  List, ListItem,
} from '@mui/material'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  brainApi,
  type BrainTrendPrediction,
  type BrainHostPersona,
  type BrainRiskItem,
  type BrainStrategicPlan,
  type BrainGrowthPathResult,
  type BrainRelationSuggestion,
  type BrainTrendSignal,
  type BrainCounterfactualResult,
  type BrainRiskStats,
} from '@/api/brain'
import { useToast } from '@/contexts/ToastContext'

interface CausalPayload {
  [key: string]: unknown
}

const SOURCE_OPTS = [
  { value: '', label: '全部来源' },
  { value: 'douyin', label: '抖音' },
  { value: 'weibo', label: '微博' },
  { value: 'network', label: '全网' },
]

function LifecyclePanel() {
  const [source, setSource] = useState('')
  const { data = [], isLoading, refetch } = useQuery({
    queryKey: ['brain-trends-lifecycle', source],
    queryFn: () => brainApi.trendsWithLifecycle({
      category: source || undefined,
      limit: 25,
    }),
  })
  return (
    <Box sx={{ mt: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center" mb={2}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>榜单来源</InputLabel>
          <Select value={source} label="榜单来源" onChange={e => setSource(e.target.value)}>
            {SOURCE_OPTS.map(o => <MenuItem key={o.value || 'all'} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
        <Button size="small" variant="outlined" onClick={() => refetch()} disabled={isLoading}>刷新</Button>
        {isLoading && <CircularProgress size={20} />}
      </Stack>
      {!isLoading && data.length === 0 && <Alert severity="info">暂无趋势数据（需启用 TianAPI 等数据源）</Alert>}
      {data.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>关键词</TableCell>
                <TableCell>来源</TableCell>
                <TableCell>生命周期</TableCell>
                <TableCell>窗口</TableCell>
                <TableCell>建议</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data as BrainTrendPrediction[]).map((row, i) => (
                <TableRow key={i}>
                  <TableCell>{row.signal?.title}</TableCell>
                  <TableCell><Chip size="small" label={row.signal?.category} variant="outlined" /></TableCell>
                  <TableCell>{row.lifecycle?.phase ?? '--'} · 动量 {(row.lifecycle?.momentum ?? 0).toFixed(2)}</TableCell>
                  <TableCell>{row.window?.windowType ?? '--'} ({row.window?.remainingHours ?? 0}h)</TableCell>
                  <TableCell sx={{ maxWidth: 280 }}>{row.window?.advice ?? '--'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

function HostTrendsPanel() {
  const { data: personas = [] } = useQuery({
    queryKey: ['brain-host-personas'],
    queryFn: () => brainApi.hostPersonas(),
  })
  const [hostCode, setHostCode] = useState('')
  const { data: hostTrends = [], isLoading, refetch } = useQuery({
    queryKey: ['brain-trends-host', hostCode],
    queryFn: () => brainApi.trendsForHost({ hostCode: hostCode || undefined, limit: 20 }),
    enabled: true,
  })
  return (
    <Box sx={{ mt: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center" mb={2}>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>主播人设</InputLabel>
          <Select
            value={hostCode}
            label="主播人设"
            onChange={e => setHostCode(e.target.value)}
          >
            <MenuItem value="">未指定（通用排序）</MenuItem>
            {(personas as BrainHostPersona[]).map(p => (
              <MenuItem key={p.id} value={p.hostCode}>{p.hostName} ({p.hostCode})</MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button size="small" variant="outlined" onClick={() => refetch()} disabled={isLoading}>刷新</Button>
        {isLoading && <CircularProgress size={20} />}
      </Stack>
      {hostTrends.length === 0 && !isLoading && <Alert severity="info">暂无趋势</Alert>}
      <Stack spacing={1}>
        {hostTrends.map(t => (
          <Paper key={t.id} variant="outlined" sx={{ p: 1.5 }}>
            <Typography variant="body2" fontWeight={600}>{t.title}</Typography>
            <Typography variant="caption" color="text.secondary">{t.category} · {t.source} · 热度 {t.heatScore.toFixed(2)}</Typography>
          </Paper>
        ))}
      </Stack>
    </Box>
  )
}

function RiskPanel() {
  const toast = useToast()
  const [text, setText] = useState('')
  const mut = useMutation({
    mutationFn: () => brainApi.riskWarn(text),
    onError: (e: Error) => toast(e.message, 'error'),
  })
  return (
    <Box sx={{ mt: 2 }}>
      <TextField
        fullWidth multiline minRows={4}
        label="待检测话术/文案"
        value={text}
        onChange={e => setText(e.target.value)}
        sx={{ mb: 2 }}
      />
      <Button variant="contained" disabled={!text.trim() || mut.isPending} onClick={() => mut.mutate()}>
        {mut.isPending ? <CircularProgress size={20} /> : '检测风险'}
      </Button>
      {mut.data && mut.data.length === 0 && <Alert sx={{ mt: 2 }} severity="success">未检出明显风险</Alert>}
      {mut.data && mut.data.length > 0 && (
        <Stack spacing={1} sx={{ mt: 2 }}>
          {mut.data.map((r: BrainRiskItem, i: number) => (
            <Alert key={i} severity={r.level >= 3 ? 'error' : r.level >= 2 ? 'warning' : 'info'}>
              [{r.type}] {r.message}{r.suggestion ? ` — ${r.suggestion}` : ''}
            </Alert>
          ))}
        </Stack>
      )}
    </Box>
  )
}

function StrategyGrowthPanel() {
  const toast = useToast()
  const [accountId, setAccountId] = useState('')
  const [goals, setGoals] = useState('万粉突破, 5万粉')
  const [targetFans, setTargetFans] = useState('100000')
  const [plan, setPlan] = useState<BrainStrategicPlan | null>(null)
  const [growth, setGrowth] = useState<BrainGrowthPathResult | null>(null)
  const planMut = useMutation({
    mutationFn: () => brainApi.strategicPlan({
      accountId: accountId ? parseInt(accountId, 10) : null,
      goals: goals.split(/[,，]/).map(s => s.trim()).filter(Boolean),
    }),
    onSuccess: (d) => { setPlan(d); toast('战略报告已生成', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const growthMut = useMutation({
    mutationFn: () => brainApi.growthPath({
      accountId: accountId ? parseInt(accountId, 10) : null,
      targetFans: parseInt(targetFans, 10) || 100000,
      currentState: {},
    }),
    onSuccess: (d) => { setGrowth(d); toast('增长路径已生成', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  return (
    <Box sx={{ mt: 2 }}>
      <Stack spacing={2}>
        <TextField size="small" label="抖音账号 ID（可选）" value={accountId} onChange={e => setAccountId(e.target.value)} sx={{ maxWidth: 320 }} />
        <TextField size="small" label="战略目标（逗号分隔）" value={goals} onChange={e => setGoals(e.target.value)} fullWidth />
        <Stack direction="row" spacing={2}>
          <Button variant="contained" onClick={() => planMut.mutate()} disabled={planMut.isPending}>
            {planMut.isPending ? <CircularProgress size={20} /> : '生成战略规划'}
          </Button>
          <TextField size="small" label="目标粉丝数" value={targetFans} onChange={e => setTargetFans(e.target.value)} sx={{ width: 160 }} />
          <Button variant="outlined" onClick={() => growthMut.mutate()} disabled={growthMut.isPending}>
            {growthMut.isPending ? <CircularProgress size={20} /> : '生成增长路径'}
          </Button>
        </Stack>
        {plan && (
          <Stack spacing={2}>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>行业分析</Typography>
              <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>{plan.industryAnalysis}</Typography>
              <Typography variant="subtitle2" gutterBottom>竞品分析</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{plan.competitorAnalysis}</Typography>
            </Paper>
            {(plan.opportunityPoints?.length ?? 0) > 0 && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom>机会点</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5}>
                  {(plan.opportunityPoints ?? []).map((p, i) => <Chip key={i} label={p} size="small" color="primary" variant="outlined" />)}
                </Stack>
              </Paper>
            )}
            {plan.swotScores && Object.keys(plan.swotScores).length > 0 && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom>SWOT 得分</Typography>
                <Table size="small">
                  <TableBody>
                    {Object.entries(plan.swotScores).map(([k, v]) => (
                      <TableRow key={k}>
                        <TableCell>{k}</TableCell>
                        <TableCell align="right">{v.toFixed(2)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            )}
            {(plan.contentMatrix?.length ?? 0) > 0 && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom>内容矩阵</Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>类型</TableCell>
                      <TableCell>策略</TableCell>
                      <TableCell align="right">优先级</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(plan.contentMatrix ?? []).map((c, i) => (
                      <TableRow key={i}>
                        <TableCell>{c.type}</TableCell>
                        <TableCell>{c.strategy}</TableCell>
                        <TableCell align="right">{c.priority}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            )}
            {(plan.growthPhases?.length ?? 0) > 0 && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom>增长阶段策略</Typography>
                {(plan.growthPhases ?? []).map((g, i) => (
                  <Box key={i} sx={{ mb: 1.5 }}>
                    <Typography variant="body2" fontWeight={600}>{g.phase} — {g.goal}</Typography>
                    <ul style={{ margin: '4px 0', paddingLeft: 20 }}>
                      {(g.strategies ?? []).map((s, j) => <li key={j}><Typography variant="caption">{s}</Typography></li>)}
                    </ul>
                  </Box>
                ))}
              </Paper>
            )}
            {(plan.diagnoses?.length ?? 0) > 0 && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom>诊断摘要</Typography>
                {(plan.diagnoses ?? []).map((d, i) => (
                  <Box key={i} sx={{ mb: 1 }}>
                    <Typography variant="body2" fontWeight={600}>{d.title}</Typography>
                    <Typography variant="caption" color="text.secondary">{d.detail}</Typography>
                  </Box>
                ))}
              </Paper>
            )}
          </Stack>
        )}
        {growth && (
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" gutterBottom>摘要</Typography>
            <Typography variant="body2" sx={{ mb: 1, whiteSpace: 'pre-wrap' }}>{growth.summary}</Typography>
            {(growth.phases ?? []).map((ph, i) => (
              <Box key={i} sx={{ mb: 1 }}>
                <Typography variant="body2" fontWeight={600}>{ph.phaseName} → {ph.targetFans} 粉</Typography>
                <Typography variant="caption" component="div" color="text.secondary">{ph.estimatedDuration}</Typography>
                <ul style={{ margin: '4px 0', paddingLeft: 20 }}>
                  {(ph.strategies ?? []).map((s, j) => <li key={j}><Typography variant="caption">{s}</Typography></li>)}
                </ul>
              </Box>
            ))}
          </Paper>
        )}
      </Stack>
    </Box>
  )
}

function GraphOpsPanel() {
  const toast = useToast()
  const qc = useQueryClient()
  const [src, setSrc] = useState('')
  const [tgt, setTgt] = useState('')
  const [rel, setRel] = useState('co_mentioned')
  const { data = [], isLoading, refetch } = useQuery({
    queryKey: ['brain-relation-suggestions'],
    queryFn: () => brainApi.relationSuggestionsList({ status: 'pending', limit: 30 }),
  })
  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'approved' | 'rejected' }) =>
      brainApi.relationSuggestionsUpdateStatus(id, status),
    onSuccess: () => {
      toast('审核状态已更新', 'success')
      qc.invalidateQueries({ queryKey: ['brain-relation-suggestions'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const materializeMut = useMutation({
    mutationFn: () => brainApi.relationSuggestionsMaterialize([{
      sourceEntityKey: src.trim(),
      targetEntityKey: tgt.trim(),
      relationType: rel.trim() || 'co_mentioned',
    }]),
    onSuccess: (r) => {
      toast(`已写入建议队列 ${r.inserted} 条`, 'success')
      qc.invalidateQueries({ queryKey: ['brain-relation-suggestions'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  return (
    <Box sx={{ mt: 2 }}>
      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>手动写入待审关系（G-2 materialize）</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <TextField size="small" label="源实体 key" value={src} onChange={e => setSrc(e.target.value)} sx={{ width: 200 }} />
          <TextField size="small" label="目标实体 key" value={tgt} onChange={e => setTgt(e.target.value)} sx={{ width: 200 }} />
          <TextField size="small" label="关系类型" value={rel} onChange={e => setRel(e.target.value)} sx={{ width: 160 }} />
          <Button
            size="small"
            variant="contained"
            disabled={!src.trim() || !tgt.trim() || materializeMut.isPending}
            onClick={() => materializeMut.mutate()}
          >
            {materializeMut.isPending ? <CircularProgress size={18} /> : '写入队列'}
          </Button>
        </Stack>
      </Paper>
      <Stack direction="row" spacing={1} mb={2}>
        <Button size="small" variant="outlined" onClick={() => refetch()} disabled={isLoading}>刷新待审核关系</Button>
        {isLoading && <CircularProgress size={20} />}
      </Stack>
      {data.length === 0 && !isLoading && (
        <Alert severity="info">暂无待审核关系建议（或 Neo4j / 行业大脑图谱未启用）</Alert>
      )}
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>源</TableCell>
              <TableCell>目标</TableCell>
              <TableCell>关系</TableCell>
              <TableCell align="right">置信度</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(data as BrainRelationSuggestion[]).map((row) => (
              <TableRow key={row.id}>
                <TableCell>{row.id}</TableCell>
                <TableCell sx={{ maxWidth: 140 }}><Typography variant="caption" noWrap title={row.sourceEntityKey}>{row.sourceEntityKey}</Typography></TableCell>
                <TableCell sx={{ maxWidth: 140 }}><Typography variant="caption" noWrap title={row.targetEntityKey}>{row.targetEntityKey}</Typography></TableCell>
                <TableCell>{row.relationType}</TableCell>
                <TableCell align="right">{row.confidence != null ? row.confidence.toFixed(2) : '--'}</TableCell>
                <TableCell>
                  <Stack direction="row" spacing={0.5}>
                    <Button
                      size="small"
                      color="success"
                      disabled={statusMut.isPending}
                      onClick={() => statusMut.mutate({ id: row.id, status: 'approved' })}
                    >通过</Button>
                    <Button
                      size="small"
                      color="error"
                      disabled={statusMut.isPending}
                      onClick={() => statusMut.mutate({ id: row.id, status: 'rejected' })}
                    >驳回</Button>
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}

function ExtensionsPanel() {
  const toast = useToast()
  const [cfCur, setCfCur] = useState('{}')
  const [cfInt, setCfInt] = useState('{}')
  const [cfRes, setCfRes] = useState<BrainCounterfactualResult | null>(null)
  const [stratId, setStratId] = useState('combo_A')
  const [stratExplain, setStratExplain] = useState('')
  const [detected, setDetected] = useState<BrainTrendSignal[]>([])
  const [riskBatchText, setRiskBatchText] = useState('')
  const [riskBatchRes, setRiskBatchRes] = useState<BrainRiskItem[]>([])
  const [riskStats, setRiskStats] = useState<BrainRiskStats | null>(null)
  const [synergyJson, setSynergyJson] = useState('')
  const [styleHost, setStyleHost] = useState('')
  const [styleContent, setStyleContent] = useState('')
  const [styleScore, setStyleScore] = useState<string>('')
  const [ipType, setIpType] = useState('phenomenal')
  const [followers, setFollowers] = useState('10000')
  const [months, setMonths] = useState('6')
  const [ipStageJson, setIpStageJson] = useState('')
  const [ipBaseJson, setIpBaseJson] = useState('')

  const cfMut = useMutation({
    mutationFn: () => {
      const currentState = JSON.parse(cfCur || '{}') as CausalPayload
      const intervention = JSON.parse(cfInt || '{}') as CausalPayload
      return brainApi.causalCounterfactual({ currentState, intervention })
    },
    onSuccess: (d) => { setCfRes(d); toast('反事实推理完成', 'success') },
    onError: (e: Error) => toast(e.message || 'JSON 或接口错误', 'error'),
  })
  const explainMut = useMutation({
    mutationFn: () => brainApi.causalExplainStrategy({ strategyId: stratId, context: {} }),
    onSuccess: (d) => { setStratExplain(d.explanation); toast('策略解释已生成', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const detectMut = useMutation({
    mutationFn: () => brainApi.trendsDetectNew(),
    onSuccess: (d) => { setDetected(d); toast(`检测到 ${d.length} 条趋势`, 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const batchRiskMut = useMutation({
    mutationFn: () => {
      const lines = riskBatchText.split(/\r?\n/).map(s => s.trim()).filter(Boolean)
      return brainApi.riskWarnBatch(lines)
    },
    onSuccess: (d) => { setRiskBatchRes(d); toast('批量检测完成', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const statsMut = useMutation({
    mutationFn: () => brainApi.riskStats(),
    onSuccess: (d) => { setRiskStats(d); toast('已拉取风险统计', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const synergyMut = useMutation({
    mutationFn: () => brainApi.synergy(),
    onSuccess: (d) => { setSynergyJson(JSON.stringify(d, null, 2)); toast('协同摘要已更新', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const styleMut = useMutation({
    mutationFn: () => brainApi.styleConsistency({ hostCode: styleHost || undefined, content: styleContent || undefined }),
    onSuccess: (d) => { setStyleScore(`score=${d.score}`); toast('风格一致性已计算', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const ipStageMut = useMutation({
    mutationFn: () => brainApi.ipGrowthStage({
      ipType,
      followerCount: parseInt(followers, 10) || 0,
      operatingMonths: parseInt(months, 10) || 0,
    }),
    onSuccess: (d) => { setIpStageJson(JSON.stringify(d, null, 2)); toast('IP 阶段已计算', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const ipBaseMut = useMutation({
    mutationFn: () => brainApi.ipMetricsBaseline({ ipType }),
    onSuccess: (d) => { setIpBaseJson(JSON.stringify(d, null, 2)); toast('基线已拉取', 'success') },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  return (
    <Box sx={{ mt: 2 }}>
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>因果反事实</Typography>
          <TextField fullWidth multiline minRows={2} label="currentState JSON" value={cfCur} onChange={e => setCfCur(e.target.value)} sx={{ mb: 1 }} />
          <TextField fullWidth multiline minRows={2} label="intervention JSON" value={cfInt} onChange={e => setCfInt(e.target.value)} sx={{ mb: 1 }} />
          <Button size="small" variant="contained" onClick={() => cfMut.mutate()} disabled={cfMut.isPending}>执行</Button>
          {cfRes && (
            <Typography variant="caption" component="pre" sx={{ display: 'block', mt: 1, whiteSpace: 'pre-wrap' }}>
              {JSON.stringify(cfRes, null, 2)}
            </Typography>
          )}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>策略解释</Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <TextField size="small" label="strategyId" value={stratId} onChange={e => setStratId(e.target.value)} />
            <Button size="small" variant="outlined" onClick={() => explainMut.mutate()} disabled={explainMut.isPending}>解释</Button>
          </Stack>
          {stratExplain ? <Typography variant="body2" sx={{ mt: 1 }}>{stratExplain}</Typography> : null}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>新趋势检测</Typography>
          <Button size="small" onClick={() => detectMut.mutate()} disabled={detectMut.isPending}>拉取 detect-new</Button>
          {detected.length > 0 && (
            <List dense sx={{ maxHeight: 160, overflow: 'auto' }}>
              {detected.map(t => <ListItem key={t.id} disableGutters><Typography variant="caption">{t.title}</Typography></ListItem>)}
            </List>
          )}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>批量风险检测</Typography>
          <TextField fullWidth multiline minRows={3} label="每行一条话术" value={riskBatchText} onChange={e => setRiskBatchText(e.target.value)} />
          <Button size="small" sx={{ mt: 1 }} onClick={() => batchRiskMut.mutate()} disabled={batchRiskMut.isPending}>检测</Button>
          {riskBatchRes.length > 0 && (
            <Stack spacing={0.5} sx={{ mt: 1 }}>
              {riskBatchRes.map((r, i) => (
                <Alert key={i} severity={r.level >= 3 ? 'error' : 'warning'}>{r.message}</Alert>
              ))}
            </Stack>
          )}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>风险统计</Typography>
          <Button size="small" onClick={() => statsMut.mutate()} disabled={statsMut.isPending}>拉取 risk/stats</Button>
          {riskStats && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              总检 {riskStats.totalChecks} · 违规 {riskStats.violationCount} · 准确率估计 {riskStats.accuracyEstimate?.toFixed?.(2) ?? riskStats.accuracyEstimate}
            </Typography>
          )}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>五主播协同</Typography>
          <Button size="small" onClick={() => synergyMut.mutate()} disabled={synergyMut.isPending}>拉取 synergy</Button>
          {synergyJson ? <Typography variant="caption" component="pre" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>{synergyJson}</Typography> : null}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>风格一致性</Typography>
          <TextField size="small" label="hostCode" value={styleHost} onChange={e => setStyleHost(e.target.value)} sx={{ mr: 1 }} />
          <TextField size="small" label="content" value={styleContent} onChange={e => setStyleContent(e.target.value)} fullWidth sx={{ mt: 1 }} />
          <Button size="small" sx={{ mt: 1 }} onClick={() => styleMut.mutate()} disabled={styleMut.isPending}>计算</Button>
          {styleScore ? <Typography variant="caption">{styleScore}</Typography> : null}
        </Paper>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>IP 增长阶段 / 基线</Typography>
          <FormControl size="small" sx={{ minWidth: 160, mr: 1 }}>
            <InputLabel>ipType</InputLabel>
            <Select value={ipType} label="ipType" onChange={e => setIpType(e.target.value)}>
              <MenuItem value="phenomenal">phenomenal</MenuItem>
              <MenuItem value="top">top</MenuItem>
            </Select>
          </FormControl>
          <TextField size="small" label="粉丝数" value={followers} onChange={e => setFollowers(e.target.value)} sx={{ width: 120, mr: 1 }} />
          <TextField size="small" label="运营月数" value={months} onChange={e => setMonths(e.target.value)} sx={{ width: 120 }} />
          <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
            <Button size="small" onClick={() => ipStageMut.mutate()} disabled={ipStageMut.isPending}>增长阶段</Button>
            <Button size="small" onClick={() => ipBaseMut.mutate()} disabled={ipBaseMut.isPending}>指标基线</Button>
          </Stack>
          {ipStageJson ? <Typography variant="caption" component="pre" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>{ipStageJson}</Typography> : null}
          {ipBaseJson ? <Typography variant="caption" component="pre" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>{ipBaseJson}</Typography> : null}
        </Paper>
      </Stack>
    </Box>
  )
}

const SUB_TABS = ['趋势窗口', '主播趋势', '风险检测', '战略与增长', '图谱运维', '扩展 API']

export function IndustryBrainAdvancedTab() {
  const [sub, setSub] = useState(0)
  return (
    <Box sx={{ mt: 1 }}>
      <Tabs value={sub} onChange={(_, v) => setSub(v)} variant="scrollable" scrollButtons="auto" sx={{ mb: 1 }}>
        {SUB_TABS.map((label, i) => <Tab key={i} label={label} />)}
      </Tabs>
      {sub === 0 && <LifecyclePanel />}
      {sub === 1 && <HostTrendsPanel />}
      {sub === 2 && <RiskPanel />}
      {sub === 3 && <StrategyGrowthPanel />}
      {sub === 4 && <GraphOpsPanel />}
      {sub === 5 && <ExtensionsPanel />}
    </Box>
  )
}
