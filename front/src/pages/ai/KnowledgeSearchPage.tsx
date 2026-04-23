import { useState } from 'react'
import {
  Box, Typography, TextField, Button, Card, CardContent, CardActions,
  Chip, CircularProgress, InputAdornment, FormControl, InputLabel,
  Select, MenuItem, Stack, IconButton, Tooltip,
} from '@mui/material'
import {
  Search as SearchIcon, ThumbUp as ThumbUpIcon, ThumbDown as ThumbDownIcon,
  ContentCopy as CopyIcon, LocalFireDepartment as FireIcon,
} from '@mui/icons-material'
import { useQuery, useMutation } from '@tanstack/react-query'
import { aiApi, type KbSearchHit } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'

const HOT_SEARCHES = ['直播话术', '短视频脚本', '选品策略', '数据分析', '美妆技巧']

interface SearchResult {
  id: number; title: string; content: string; score?: number
  sourceType?: string; labels?: string[]; kbId?: number; text?: string
}

export default function KnowledgeSearchPage() {
  const toast = useToast()
  const [selectedKbId, setSelectedKbId] = useState<number | ''>(``)
  const [query, setQuery] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  const { data: kbList = [] } = useQuery({
    queryKey: ['kb-list-for-search'],
    queryFn: () => aiApi.kbList({ rows: 100 }),
  })

  const { data: results = [], isLoading } = useQuery({
    queryKey: ['kb-search', selectedKbId, searchQuery],
    queryFn: async () => {
      const res = await aiApi.kbSearch(
        selectedKbId as number,
        { query: searchQuery, topK: 10 }
      )
      const list: KbSearchHit[] = Array.isArray(res) ? res : []
      return list.map((item): SearchResult => ({
        id: Number(item.docId ?? 0),
        title: (item.title && item.title.trim() !== '')
          ? String(item.title).slice(0, 50)
          : String(item.content ?? '').slice(0, 50),
        content: String(item.content ?? ''),
        score: item.score != null ? Number(item.score) : undefined,
        sourceType: item.source != null ? String(item.source) : undefined,
        labels: Array.isArray(item.labels) ? item.labels.map((l: string) => String(l)) : undefined,
        kbId: selectedKbId as number,
      }))
    },
    enabled: !!selectedKbId && searchQuery.length > 0,
  })

  const feedbackMut = useMutation({
    mutationFn: ({ docId, rating }: { docId: number; rating: number }) =>
      aiApi.kbSearch(selectedKbId as number, { docId, rating, type: 'feedback' }),
    onSuccess: () => toast('反馈已提交', 'success'),
  })

  const handleSearch = () => { if (query.trim()) setSearchQuery(query.trim()) }
  const handleHot = (term: string) => { setQuery(term); setSearchQuery(term) }
  const handleCopy = (text: string) => { navigator.clipboard.writeText(text); toast('已复制', 'success') }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-lg)', p: 'var(--spacing-lg)', maxWidth: 900, bgcolor: 'var(--color-surface-dark)' }}>
      <Typography variant="h5" sx={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>知识库检索</Typography>

      {/* 搜索区 */}
      <Card sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
        <CardContent sx={{ bgcolor: 'var(--color-surface)' }}>
          <Stack spacing={2}>
            <FormControl size="small" fullWidth sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: 'var(--border-radius-lg)',
                bgcolor: 'var(--color-surface-dark)',
                color: 'var(--color-text-primary)',
                '& fieldset': { borderColor: 'var(--color-surface-light)' },
                '&:hover fieldset': { borderColor: 'var(--color-primary)' }
              },
              '& .MuiInputLabel-root': { color: 'var(--color-text-secondary)' }
            }}>
              <InputLabel>选择知识库</InputLabel>
              <Select value={selectedKbId} label="选择知识库"
                onChange={e => setSelectedKbId(e.target.value as number | '')}>
                <MenuItem value="">全部</MenuItem>
                {(Array.isArray(kbList) ? kbList : []).map((kb) => (
                  <MenuItem key={kb.id} value={kb.id}>{kb.kbName}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              fullWidth size="small" placeholder="输入搜索关键词..."
              value={query} onChange={e => setQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: '44px',
                  borderRadius: 'var(--border-radius-lg)',
                  bgcolor: 'var(--color-surface-dark)',
                  color: 'var(--color-text-primary)',
                  '& fieldset': { borderColor: 'var(--color-surface-light)' },
                  '&:hover fieldset': { borderColor: 'var(--color-primary)' },
                  '&.Mui-focused fieldset': { borderColor: 'var(--color-primary)' }
                },
                '& .MuiOutlinedInput-input::placeholder': { color: 'var(--color-text-secondary)' }
              }}
              InputProps={{
                startAdornment: <InputAdornment position="start"><SearchIcon sx={{ color: 'var(--color-text-secondary)' }} /></InputAdornment>,
                endAdornment: <InputAdornment position="end">
                  <Button variant="contained" size="small" onClick={handleSearch} disabled={!query.trim() || !selectedKbId}
                    sx={{
                      height: '32px',
                      borderRadius: 'var(--border-radius-lg)',
                      bgcolor: 'var(--color-primary)',
                      color: '#000',
                      fontWeight: 600,
                      '&:hover': { bgcolor: 'var(--color-primary-dark)' }
                    }}>搜索</Button>
                </InputAdornment>,
              }} />
            <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
              <FireIcon sx={{ color: '#f57c00', fontSize: 18 }} />
              <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>热门：</Typography>
              {HOT_SEARCHES.map(h => (
                <Chip key={h} label={h} size="small" clickable onClick={() => handleHot(h)} variant="outlined" />
              ))}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {isLoading && <CircularProgress sx={{ display: 'block', mx: 'auto' }} />}

      {!isLoading && searchQuery && results.length === 0 && (
        <Typography sx={{ py: 4, textAlign: 'center', color: 'var(--color-text-secondary)' }}>未找到相关内容</Typography>
      )}

      {results.map((r: SearchResult, i: number) => (
        <Card key={r.id ?? i} variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)', borderRadius: 'var(--border-radius-xl)', boxShadow: 'var(--shadow-elevation-1)', transition: 'all var(--transition-base)', '&:hover': { boxShadow: 'var(--shadow-elevation-2)', borderColor: 'var(--color-primary)' } }}>
          <CardContent sx={{ pb: 1, bgcolor: 'var(--color-surface)' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={1}>
              <Typography variant="subtitle2" fontWeight={600}>{r.title || `结果 ${i + 1}`}</Typography>
              {r.score !== undefined && (
                <Chip label={`相关度 ${(r.score * 100).toFixed(0)}%`} size="small" variant="filled"
                  sx={{
                    bgcolor: r.score > 0.8 ? 'var(--color-success)' : r.score > 0.5 ? 'var(--color-warning)' : 'var(--color-text-secondary)',
                    color: '#fff',
                    fontWeight: 600
                  }} />
              )}
            </Stack>
            {r.labels && r.labels.length > 0 && (
              <Stack direction="row" spacing={0.5} mb={1} flexWrap="wrap">
                {r.labels.map(l => <Chip key={l} label={l} size="small" variant="outlined" sx={{ borderColor: 'var(--color-surface-light)', color: 'var(--color-text-secondary)' }} />)}
              </Stack>
            )}
            <Typography variant="body2" sx={{ lineHeight: 1.5, color: 'var(--color-text-secondary)' }}>
              {String(r.content ?? r.text ?? '').slice(0, 300)}
              {String(r.content ?? r.text ?? '').length > 300 ? '...' : ''}
            </Typography>
          </CardContent>
          <CardActions sx={{ px: 'var(--spacing-md)', pb: 'var(--spacing-md)', bgcolor: 'var(--color-surface)' }}>
            <Tooltip title="复制内容">
              <IconButton size="small" onClick={() => handleCopy(String(r.content ?? r.text ?? ''))}><CopyIcon fontSize="small" /></IconButton>
            </Tooltip>
            <Tooltip title="有用">
              <IconButton size="small" onClick={() => feedbackMut.mutate({ docId: r.id, rating: 1 })}><ThumbUpIcon fontSize="small" /></IconButton>
            </Tooltip>
            <Tooltip title="无用">
              <IconButton size="small" onClick={() => feedbackMut.mutate({ docId: r.id, rating: -1 })}><ThumbDownIcon fontSize="small" /></IconButton>
            </Tooltip>
          </CardActions>
        </Card>
      ))}
    </Box>
  )
}
