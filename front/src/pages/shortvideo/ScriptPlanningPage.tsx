import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, TextField, Button, Typography, Stack,
  FormControl, InputLabel, Select, MenuItem, CircularProgress,
  Chip, Tab, Tabs, Divider,
} from '@mui/material'
import { AutoAwesome as AiIcon, Save as SaveIcon, Delete as DeleteIcon } from '@mui/icons-material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import type { GridColDef } from '@mui/x-data-grid'

interface SvScriptRow {
  id?: number
  title?: string
  createTime?: string
  scriptType?: string
  [key: string]: unknown
}

export default function ScriptPlanningPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : undefined
  const [tab, setTab] = useState(0)
  const [type, setType] = useState('daily')
  const [theme, setTheme] = useState('')
  const [viralUrl, setViralUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [generated, setGenerated] = useState('')

  const { data: scripts = [], isLoading } = useQuery({
    queryKey: ['sv-scripts', projectId],
    queryFn: () => shortvideoApi.svScriptList({ projectId: projectId ?? 0, page: 0, rows: 50 }).then((r) => r.list ?? []),
    enabled: tab === 1,
  })

  const handleGenerate = async () => {
    if (!theme && type !== 'viral-clone') { toast('请输入主题', 'warning'); return }
    setLoading(true)
    try {
      const res = await shortvideoApi.svScriptList({ projectId: projectId ?? 0, page: 0, rows: 1 })
      // 简单生成示例 — 实际调 generate API
      setGenerated(JSON.stringify(res, null, 2))
      toast('脚本已生成', 'success')
    } catch {
      toast('生成失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    if (!generated) return
    try {
      await shortvideoApi.svScriptSave({ projectId: projectId ?? 0, content: generated })
      toast('保存成功', 'success')
      qc.invalidateQueries({ queryKey: ['sv-scripts', projectId] })
      setTab(1)
    } catch {
      toast('保存失败', 'error')
    }
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('确认删除？')) return
    try {
      await shortvideoApi.svScriptDelete(id)
      toast('已删除', 'success')
      qc.invalidateQueries({ queryKey: ['sv-scripts', projectId] })
    } catch {
      toast('删除失败', 'error')
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'title', headerName: '标题', flex: 1 },
    { field: 'scriptType', headerName: '类型', width: 120 },
    { field: 'status', headerName: '状态', width: 100, renderCell: ({ value }) => <Chip size="small" label={String(value ?? '')} /> },
    { field: 'createTime', headerName: '创建时间', width: 160, renderCell: ({ value }) => String(value ?? '').slice(0, 16) },
    {
      field: '_del', headerName: '操作', width: 100, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDelete(row.id as number)}>删除</Button>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title="脚本策划"
        breadcrumbs={[{ label: '短视频' }, { label: '脚本策划' }]}
      />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="AI 生成" />
        <Tab label="脚本列表" />
      </Tabs>

      {tab === 0 && (
        <Card sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
          <CardContent sx={{ p: 'var(--spacing-lg)' }}>
            <Stack spacing={2}>
              <FormControl size="small" sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 'var(--border-radius-lg)',
                  bgcolor: 'var(--color-surface-dark)',
                  color: 'var(--color-text-primary)',
                  '& fieldset': { borderColor: 'var(--color-surface-light)' },
                  '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                },
                '& .MuiInputLabel-root': { color: 'var(--color-text-secondary)' }
              }}>
                <InputLabel>生成模式</InputLabel>
                <Select value={type} label="生成模式" onChange={(e) => setType(e.target.value)}>
                  <MenuItem value="daily">日常内容</MenuItem>
                  <MenuItem value="viral-clone">爆款复刻</MenuItem>
                  <MenuItem value="soft-ad">软广植入</MenuItem>
                </Select>
              </FormControl>
              {type === 'viral-clone' ? (
                <TextField
                  label="爆款视频链接" value={viralUrl}
                  onChange={(e) => setViralUrl(e.target.value)}
                  placeholder="粘贴抖音/快手爆款视频链接"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'var(--color-surface-dark)',
                      color: 'var(--color-text-primary)',
                      '& fieldset': { borderColor: 'var(--color-surface-light)' },
                      '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                    }
                  }}
                />
              ) : (
                <TextField
                  label="视频主题" required value={theme}
                  onChange={(e) => setTheme(e.target.value)}
                  placeholder="例如：护肤品开箱测评"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'var(--color-surface-dark)',
                      color: 'var(--color-text-primary)',
                      '& fieldset': { borderColor: 'var(--color-surface-light)' },
                      '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                    }
                  }}
                />
              )}
              <Stack direction="row" spacing={1}>
                <Button
                  variant="contained"
                  startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <AiIcon />}
                  onClick={handleGenerate} disabled={loading}
                  sx={{
                    height: '44px',
                    borderRadius: 'var(--border-radius-lg)',
                    bgcolor: 'var(--color-primary)',
                    color: '#000',
                    fontWeight: 600,
                    '&:hover': { bgcolor: 'var(--color-primary-dark)' }
                  }}
                >
                  {loading ? 'AI 生成中...' : 'AI 生成脚本'}
                </Button>
                {!!generated && (
                  <Button variant="outlined" startIcon={<SaveIcon />} onClick={handleSave}
                    sx={{
                      height: '44px',
                      borderRadius: 'var(--border-radius-lg)',
                      borderColor: 'var(--color-primary)',
                      color: 'var(--color-primary)',
                      '&:hover': { bgcolor: 'rgba(0, 208, 132, 0.1)' }
                    }}>保存</Button>
                )}
              </Stack>
              {!!generated && (
                <Box>
                  <Divider sx={{ my: 'var(--spacing-md)', borderColor: 'var(--color-surface-light)' }} />
                  <Typography variant="body2" sx={{ color: 'var(--color-text-secondary)', mb: 'var(--spacing-md)' }}>生成结果</Typography>
                  <Typography component="pre" sx={{
                    whiteSpace: 'pre-wrap',
                    fontFamily: 'monospace',
                    fontSize: 12,
                    p: 'var(--spacing-md)',
                    bgcolor: 'var(--color-surface-dark)',
                    borderRadius: 'var(--border-radius-lg)',
                    border: '1px solid var(--color-surface-light)',
                    color: 'var(--color-text-primary)',
                    overflow: 'auto'
                  }}>
                    {generated}
                  </Typography>
                </Box>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 1 && (
        <StandardDataGrid
          rows={(scripts as SvScriptRow[]).map((r, index) => ({
            ...r,
            id: r.id ?? `${String(r.title ?? 'script')}-${String(r.createTime ?? '')}-${String(r.scriptType ?? '')}-${index}`,
          }))}
          columns={columns}
          loading={isLoading}
        />
      )}
    </Box>
  )
}
