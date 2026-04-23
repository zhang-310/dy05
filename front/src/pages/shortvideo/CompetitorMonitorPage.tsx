import { useState, useEffect, useCallback } from 'react'
import {
  Box, TextField, Button, Dialog, DialogTitle,
  DialogContent, DialogActions, FormControl, InputLabel, Select,
  MenuItem, CircularProgress, Chip, Typography, Stack,
} from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import {
  Add as AddIcon, Analytics as AnalyticsIcon,
  Summarize as ReportIcon,
} from '@mui/icons-material'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { addCompetitor, listCompetitors, analyzeCompetitor, generateWeeklyReport } from '@/api/competitor'

type Row = Record<string, unknown> & { id: number }

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
  const [form, setForm] = useState({ accountId: '', accountName: '', platform: 'douyin' })

  const fetchList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await listCompetitors()
      setRows((res ?? []).map((r, i) => ({ ...r, id: (r.id as number) ?? i })))
    } catch { /* ignore */ } finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchList() }, [fetchList])

  const handleAdd = async () => {
    if (!form.accountId || !form.accountName) { toast('请填写账号信息', 'warning'); return }
    try {
      await addCompetitor(form)
      toast('添加成功', 'success')
      setAddOpen(false)
      setForm({ accountId: '', accountName: '', platform: 'douyin' })
      fetchList()
    } catch { toast('添加失败', 'error') }
  }

  const handleAnalyze = async (id: number) => {
    setAnalyzing(true)
    setAnalysisResult(null)
    setAnalysisOpen(true)
    try {
      const res = await analyzeCompetitor(id)
      setAnalysisResult(res)
    } catch { setAnalysisResult({ error: '分析失败' }) } finally { setAnalyzing(false) }
  }

  const handleReport = async () => {
    setReportOpen(true)
    setReportResult('')
    try {
      const res = await generateWeeklyReport()
      setReportResult(String(res ?? ''))
    } catch { setReportResult('生成失败') }
  }

  const columns: GridColDef[] = [
    { field: 'accountName', headerName: '账号名称', flex: 1 },
    { field: 'accountId', headerName: '账号ID', width: 160 },
    { field: 'platform', headerName: '平台', width: 100, renderCell: ({ value }) => <Chip size="small" label={String(value ?? 'douyin')} /> },
    { field: 'followerCount', headerName: '粉丝数', width: 120, type: 'number' },
    { field: 'lastAnalyzedAt', headerName: '最后分析', width: 160, renderCell: ({ value }) => String(value ?? '').slice(0, 16) },
    {
      field: '_actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" startIcon={<AnalyticsIcon />} onClick={() => handleAnalyze(row.id as number)}>分析</Button>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title="竞品监控"
        breadcrumbs={[{ label: '短视频' }, { label: '竞品监控' }]}
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<ReportIcon />} onClick={handleReport}>周报</Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)}>添加竞品</Button>
          </Stack>
        }
      />

      <StandardDataGrid
        rows={rows} columns={columns} loading={loading}
        getRowId={(r) => r.id}
      />

      {/* 添加竞品弹窗 */}
      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>添加竞品账号</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="账号ID" required value={form.accountId} onChange={(e) => setForm((f) => ({ ...f, accountId: e.target.value }))} />
            <TextField label="账号名称" required value={form.accountName} onChange={(e) => setForm((f) => ({ ...f, accountName: e.target.value }))} />
            <FormControl><InputLabel>平台</InputLabel>
              <Select value={form.platform} label="平台" onChange={(e) => setForm((f) => ({ ...f, platform: e.target.value }))}>
                <MenuItem value="douyin">抖音</MenuItem>
                <MenuItem value="kuaishou">快手</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleAdd}>确认添加</Button>
        </DialogActions>
      </Dialog>

      {/* 分析结果弹窗 */}
      <Dialog open={analysisOpen} onClose={() => setAnalysisOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>竞品分析</DialogTitle>
        <DialogContent dividers>
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
        <DialogContent dividers>
          <Typography component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', lineHeight: 1.8 }}>
            {reportResult || '生成中...'}
          </Typography>
        </DialogContent>
        <DialogActions><Button onClick={() => setReportOpen(false)}>关闭</Button></DialogActions>
      </Dialog>
    </Box>
  )
}
