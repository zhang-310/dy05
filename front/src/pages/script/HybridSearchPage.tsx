import { useState } from 'react'
import {
  Box, Button, Card, CardContent, Chip,
  InputAdornment, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'
import { hybridSearch, SearchResultItem } from '@/api/search'

const MODULE_TABS = [
  { label: '全部', value: '' },
  { label: '话术', value: 'script' },
  { label: '知识库', value: 'live' },
  { label: '商品', value: 'product' },
]

export default function HybridSearchPage() {
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResultItem[]>([])
  const [loading, setLoading] = useState(false)
  const [tab, setTab] = useState('')
  const [searched, setSearched] = useState(false)

  const handleSearch = async () => {
    if (!query.trim()) { toast('请输入搜索关键词', 'warning'); return }
    setLoading(true)
    setSearched(true)
    try {
      const res = await hybridSearch({
        query: query.trim(),
        searchType: 'hybrid',
        topK: 20,
        filters: tab ? { moduleType: tab } : undefined,
      })
      setResults(Array.isArray(res.results) ? res.results : [])
    } catch {
      toast('搜索失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filtered = tab ? results.filter((r) => r.moduleType === tab) : results

  const scoreColor = (score: number) => {
    if (score >= 0.8) return 'success'
    if (score >= 0.5) return 'warning'
    return 'default'
  }

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="混合搜索" subtitle="向量语义 + 关键词 BM25 融合检索" />

      <Box sx={{ display: 'flex', gap: 1, mt: 2, mb: 3 }}>
        <TextField
          fullWidth
          placeholder="输入搜索内容…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          InputProps={{
            startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment>,
          }}
        />
        <Button variant="contained" onClick={handleSearch} disabled={loading} sx={{ minWidth: 100 }}>
          {loading ? '搜索中…' : '搜索'}
        </Button>
      </Box>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        {MODULE_TABS.map((t) => <Tab key={t.value} label={t.label} value={t.value} />)}
      </Tabs>

      {searched && filtered.length === 0 && !loading && (
        <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>未找到相关结果</Typography>
      )}

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
        {filtered.map((item) => (
          <Card key={item.id} variant="outlined">
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 0.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>{item.title}</Typography>
                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  <Chip label={item.moduleType} size="small" variant="outlined" />
                  <Chip
                    label={`${(item.finalScore * 100).toFixed(0)}%`}
                    size="small"
                    color={scoreColor(item.finalScore) as 'success' | 'warning' | 'default'}
                  />
                </Box>
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{
                overflow: 'hidden', display: '-webkit-box',
                WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
              }}>
                {item.content}
              </Typography>
            </CardContent>
          </Card>
        ))}
      </Box>
    </Box>
  )
}
