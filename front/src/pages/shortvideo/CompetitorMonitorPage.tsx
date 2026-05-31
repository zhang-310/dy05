import { useState, useEffect, useCallback } from 'react'
import {
  Box, TextField, Button, Dialog, DialogTitle,
  DialogContent, DialogActions, FormControl, InputLabel, Select,
  MenuItem, CircularProgress, Chip, Typography, Stack, Alert,
} from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import {
  Add as AddIcon, Analytics as AnalyticsIcon,
  Summarize as ReportIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { ConfirmDialog, DataGridEmptyOverlay, PageHeader, StandardDataGrid } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { addCompetitor, listCompetitors, analyzeCompetitor, generateWeeklyReport, removeCompetitor } from '@/api/competitor'
import { getErrorMessage } from '@/utils/errorHandler'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

type Row = Record<string, unknown> & { id: number }
const COMPETITOR_ADD_ENDPOINT = '/short-video/competitor/add'
const COMPETITOR_LIST_ENDPOINT = '/short-video/competitor/list'
const COMPETITOR_ANALYZE_ENDPOINT = '/short-video/competitor/analyze'
const COMPETITOR_WEEKLY_REPORT_ENDPOINT = '/short-video/competitor/weekly-report'
const COMPETITOR_REMOVE_ENDPOINT = '/short-video/competitor/remove'
const COMPETITOR_READY_ENDPOINTS = [
  COMPETITOR_ADD_ENDPOINT,
  COMPETITOR_LIST_ENDPOINT,
  COMPETITOR_ANALYZE_ENDPOINT,
  COMPETITOR_WEEKLY_REPORT_ENDPOINT,
  COMPETITOR_REMOVE_ENDPOINT,
].join('|')
const COMPETITOR_READY_ROUTES = [
  shortvideoRoutes.competitorMonitor,
  shortvideoRoutes.dashboard,
].join('|')
const COMPETITOR_SUPPORTED_ACTIONS = [
  'refresh-competitor-list',
  'add-competitor',
  'analyze-competitor',
  'generate-weekly-report',
  'remove-competitor',
].join('|')
const COMPETITOR_UNSUPPORTED_ENDPOINTS = [
  '/short-video/competitor/mock',
  '/short-video/competitor/local-list',
  '/short-video/competitor/local-add',
  '/short-video/competitor/local-remove',
  '/short-video/competitor/local-analysis',
  '/short-video/competitor/template-report',
  '/short-video/competitor/export',
  '/short-video/douyin/direct-scrape',
].join('|')

export default function CompetitorMonitorPage() {
  const toast = useToast()
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(false)
  const [addOpen, setAddOpen] = useState(false)
  const [analysisOpen, setAnalysisOpen] = useState(false)
  const [reportOpen, setReportOpen] = useState(false)
  const [analysisResult, setAnalysisResult] = useState<Record<string, unknown> | null>(null)
  const [reportResult, setReportResult] = useState('')
  const [analyzing, setAnalyzing] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [form, setForm] = useState({ accountId: '', accountName: '', platform: 'douyin' })

  const fetchList = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await listCompetitors()
      setRows((res ?? []).map((r, i) => ({ ...r, id: (r.id as number) ?? i })))
    } catch (e) {
      setError(`竞品列表加载失败（POST ${COMPETITOR_LIST_ENDPOINT}）：${getErrorMessage(e)}。页面不会填充本地竞品账号。`)
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchList() }, [fetchList])

  const handleAdd = async () => {
    if (!form.accountId || !form.accountName) { toast('请填写账号信息', 'warning'); return }
    setActionError('')
    try {
      await addCompetitor(form)
      toast('添加成功', 'success')
      setAddOpen(false)
      setForm({ accountId: '', accountName: '', platform: 'douyin' })
      fetchList()
    } catch (e) {
      const message = getErrorMessage(e)
      setActionError(`添加竞品失败（POST ${COMPETITOR_ADD_ENDPOINT}）：${message}。弹窗输入会保留。`)
      toast(`添加失败：${message}`, 'error')
    }
  }

  const handleAnalyze = async (id: number) => {
    setAnalyzing(true)
    setAnalysisResult(null)
    setAnalysisOpen(true)
    try {
      const res = await analyzeCompetitor(id)
      setAnalysisResult(res)
    } catch (e) {
      setAnalysisResult({
        error: `竞品分析失败（POST ${COMPETITOR_ANALYZE_ENDPOINT}）：${getErrorMessage(e)}。竞品行会保留，不生成本地分析结论。`,
      })
    } finally { setAnalyzing(false) }
  }

  const handleReport = async () => {
    setReportOpen(true)
    setReportResult('')
    try {
      const res = await generateWeeklyReport()
      setReportResult(String(res ?? ''))
    } catch (e) {
      setReportResult(`竞品周报生成失败（POST ${COMPETITOR_WEEKLY_REPORT_ENDPOINT}）：${getErrorMessage(e)}。不会展示模板周报。`)
    }
  }

  const handleRemove = async () => {
    if (deleteId == null) return
    setDeleting(true)
    try {
      await removeCompetitor(deleteId)
      toast('已删除竞品', 'success')
      setActionError('')
      setDeleteId(null)
      fetchList()
    } catch (e) {
      const message = getErrorMessage(e)
      setActionError(`删除竞品失败（POST ${COMPETITOR_REMOVE_ENDPOINT}）：${message}。竞品行会保留，不做前端本地删除。`)
      toast(`删除失败：${message}`, 'error')
    } finally {
      setDeleting(false)
    }
  }

  const columns: GridColDef[] = [
    { field: 'accountName', headerName: '账号名称', flex: 1 },
    { field: 'accountId', headerName: '账号ID', width: 160 },
    { field: 'platform', headerName: '平台', width: 100, renderCell: ({ value }) => <Chip size="small" label={String(value ?? 'douyin')} /> },
    { field: 'followerCount', headerName: '粉丝数', width: 120, type: 'number' },
    { field: 'lastAnalyzedAt', headerName: '最后分析', width: 160, renderCell: ({ value }) => String(value ?? '').slice(0, 16) },
    {
      field: '_actions', headerName: '操作', width: 220, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" startIcon={<AnalyticsIcon />} onClick={() => handleAnalyze(row.id as number)} data-testid="competitor-analyze-button" data-source-endpoint={COMPETITOR_ANALYZE_ENDPOINT}>分析</Button>
          <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteId(row.id as number)} data-testid="competitor-delete-button" data-source-endpoint={COMPETITOR_REMOVE_ENDPOINT}>删除</Button>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="competitor-monitor-page"
      data-ready-endpoints={COMPETITOR_READY_ENDPOINTS}
      data-ready-routes={COMPETITOR_READY_ROUTES}
      data-supported-actions={COMPETITOR_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={COMPETITOR_UNSUPPORTED_ENDPOINTS}
      data-no-local-competitor-fallback="true"
      data-no-local-analysis-fallback="true"
      data-no-template-report-fallback="true"
    >
      <PageHeader
        title="竞品监控"
        breadcrumbs={[{ label: '短视频' }, { label: '竞品监控' }]}
        subtitle={`竞品列表、分析、周报和删除均走 POST ${COMPETITOR_LIST_ENDPOINT}|${COMPETITOR_ANALYZE_ENDPOINT}|${COMPETITOR_WEEKLY_REPORT_ENDPOINT}|${COMPETITOR_REMOVE_ENDPOINT}；分析依赖可用 AI 模型。`}
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={fetchList} disabled={loading} data-testid="competitor-refresh-button" data-source-endpoint={COMPETITOR_LIST_ENDPOINT}>刷新</Button>
            <Button variant="outlined" startIcon={<ReportIcon />} onClick={handleReport} data-testid="competitor-report-button" data-source-endpoint={COMPETITOR_WEEKLY_REPORT_ENDPOINT}>周报</Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)} data-testid="competitor-open-add-button">添加竞品</Button>
          </Stack>
        }
      />
      <Alert
        severity="info"
        variant="outlined"
        data-testid="competitor-monitor-boundary-contract"
        data-no-local-competitor-fallback="true"
        data-no-local-analysis-fallback="true"
        data-no-browser-direct-scrape="true"
        data-supported-actions={COMPETITOR_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        当前竞品监控只持久化账号和分析快照；粉丝数、近热视频等指标若未入库会为空，需后续接抖音采集任务补全。
      </Alert>
      {error && (
        <Alert
          severity="error"
          data-testid="competitor-list-error"
          data-no-local-competitor-fallback="true"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={fetchList}>重试</Button>}
        >
          {error}
        </Alert>
      )}
      {actionError && !(addOpen && actionError.startsWith('添加竞品失败')) && (
        <Alert
          severity="error"
          data-testid="competitor-action-error"
          data-no-local-delete-mutation="true"
          data-input-retained="true"
          sx={{ mb: 2 }}
          onClose={() => setActionError('')}
        >
          {actionError}
        </Alert>
      )}

      <Box data-testid="competitor-grid-contract" data-no-local-competitor-fallback="true" data-no-local-delete-mutation="true">
        <StandardDataGrid
        rows={rows} columns={columns} loading={loading}
        getRowId={(r) => r.id}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      {/* 添加竞品弹窗 */}
      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>添加竞品账号</DialogTitle>
        <DialogContent data-testid="competitor-add-dialog" data-input-retained="true" data-no-local-add-mutation="true">
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="账号ID" required value={form.accountId} onChange={(e) => setForm((f) => ({ ...f, accountId: e.target.value }))} />
            <TextField label="账号名称" required value={form.accountName} onChange={(e) => setForm((f) => ({ ...f, accountName: e.target.value }))} />
            <FormControl><InputLabel>平台</InputLabel>
              <Select value={form.platform} label="平台" onChange={(e) => setForm((f) => ({ ...f, platform: e.target.value }))}>
                <MenuItem value="douyin">抖音</MenuItem>
                <MenuItem value="kuaishou">快手</MenuItem>
              </Select>
            </FormControl>
            {actionError.startsWith('添加竞品失败') && (
              <Alert severity="error" data-testid="competitor-add-error" data-input-retained="true" data-no-local-add-mutation="true">
                {actionError}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleAdd} data-testid="competitor-add-confirm-button" data-source-endpoint={COMPETITOR_ADD_ENDPOINT}>确认添加</Button>
        </DialogActions>
      </Dialog>

      {/* 分析结果弹窗 */}
      <Dialog open={analysisOpen} onClose={() => setAnalysisOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>竞品分析</DialogTitle>
        <DialogContent dividers data-testid="competitor-analysis-dialog" data-no-local-analysis-fallback="true">
          {analyzing ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 3 }} /> : (
            <Typography component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13 }}>
              {analysisResult ? JSON.stringify(analysisResult, null, 2) : ''}
            </Typography>
          )}
        </DialogContent>
        <DialogActions><Button onClick={() => setAnalysisOpen(false)}>关闭</Button></DialogActions>
      </Dialog>

      {/* 周报弹窗 */}
      <Dialog open={reportOpen} onClose={() => setReportOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>竞品周报</DialogTitle>
        <DialogContent dividers data-testid="competitor-report-dialog" data-no-template-report-fallback="true">
          <Typography component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', lineHeight: 1.8 }}>
            {reportResult || '生成中...'}
          </Typography>
        </DialogContent>
        <DialogActions><Button onClick={() => setReportOpen(false)}>关闭</Button></DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleteId !== null}
        content="确定要删除该竞品账号吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={handleRemove}
        loading={deleting}
      />
    </Box>
  )
}
